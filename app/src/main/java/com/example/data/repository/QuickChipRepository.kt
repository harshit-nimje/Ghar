package com.example.data.repository

import com.example.core.model.Category
import com.example.core.model.Direction
import com.example.core.model.QuickChip
import com.example.core.model.StampColorKey
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

private fun sanitizeChipId(rawId: String): String {
  val cleaned = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
  return cleaned.ifBlank { "chip_${UUID.randomUUID()}" }
}

class QuickChipRepository(
  private val db: FirebaseFirestore,
  private val userId: String
) {
  private val collectionRef
    get() = db.collection("users").document(userId).collection("quick_chips")

  private val collectionPath: String
    get() = "users/$userId/quick_chips"

  /**
   * Real-time Flow of user's custom quick chips aligned with security rules (.whereEqualTo("userId", userId)).
   */
  fun observeQuickChips(): Flow<List<QuickChip>> = callbackFlow {
    val registration = collectionRef
      .whereEqualTo("userId", userId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.LIST, collectionPath)
          close(error)
          return@addSnapshotListener
        }
        val items = snapshot?.documents
          ?.mapNotNull { it.toQuickChipOrNull() }
          ?.sortedWith(compareBy<QuickChip> { it.sortOrder }.thenBy { it.label })
          ?: emptyList()
        trySend(items)
      }
    awaitClose { registration.remove() }
  }

  val allQuickChips: Flow<List<QuickChip>> = observeQuickChips()
    .catch { emit(emptyList()) }

  suspend fun getChipsSync(): List<QuickChip> {
    return try {
      val snapshot = collectionRef
        .whereEqualTo("userId", userId)
        .get()
        .await()
      snapshot.documents
        .mapNotNull { it.toQuickChipOrNull() }
        .sortedWith(compareBy<QuickChip> { it.sortOrder }.thenBy { it.label })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, collectionPath)
      throw e
    }
  }

  suspend fun upsert(chip: QuickChip): String {
    val docId = sanitizeChipId(chip.id)
    val docRef = collectionRef.document(docId)
    val cleanLabel = chip.label.trim().ifBlank { "Stamp" }.take(24)
    val cleanMerchant = chip.merchant.trim().ifBlank { cleanLabel }.take(120)
    val boundedSortOrder = chip.sortOrder.coerceIn(0, 200)
    val boundedAmount = chip.amountPaise.coerceIn(1L, 100_000_000_000L)

    val updatePayload = mapOf(
      "label" to cleanLabel,
      "merchant" to cleanMerchant,
      "amountPaise" to boundedAmount,
      "category" to chip.category.name,
      "direction" to chip.direction.name,
      "colorKey" to chip.colorKey.name,
      "sortOrder" to boundedSortOrder,
      "updatedAt" to FieldValue.serverTimestamp()
    )

    try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(updatePayload).await()
      } else {
        val createPayload = updatePayload + mapOf(
          "userId" to userId,
          "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(createPayload).await()
      }
      return docId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun insertDefaultsIfEmpty() {
    val existing = getChipsSync()
    if (existing.isNotEmpty()) return
    saveBatchAsNew(QuickChip.defaultChips())
  }

  suspend fun resetToDefaults() {
    try {
      val snapshot = collectionRef.whereEqualTo("userId", userId).get().await()
      val batch = db.batch()
      snapshot.documents.forEach { batch.delete(it.reference) }
      QuickChip.defaultChips().forEachIndexed { index, chip ->
        val docId = sanitizeChipId(chip.id)
        val docRef = collectionRef.document(docId)
        batch.set(
          docRef,
          mapOf(
            "userId" to userId,
            "label" to chip.label.take(24),
            "merchant" to chip.merchant.take(120),
            "amountPaise" to chip.amountPaise.coerceAtLeast(1L),
            "category" to chip.category.name,
            "direction" to chip.direction.name,
            "colorKey" to chip.colorKey.name,
            "sortOrder" to index.coerceIn(0, 200),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, collectionPath)
      throw e
    }
  }

  suspend fun updateSortOrders(orderedChips: List<QuickChip>) {
    if (orderedChips.isEmpty()) return
    try {
      val batch = db.batch()
      orderedChips.forEachIndexed { index, chip ->
        val docId = sanitizeChipId(chip.id)
        val docRef = collectionRef.document(docId)
        batch.update(
          docRef,
          mapOf(
            "sortOrder" to index.coerceIn(0, 200),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, collectionPath)
      throw e
    }
  }

  suspend fun delete(chipId: String) {
    val docId = sanitizeChipId(chipId)
    val docRef = collectionRef.document(docId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  private suspend fun saveBatchAsNew(chips: List<QuickChip>) {
    if (chips.isEmpty()) return
    try {
      val batch = db.batch()
      chips.forEachIndexed { index, chip ->
        val docId = sanitizeChipId(chip.id)
        val docRef = collectionRef.document(docId)
        batch.set(
          docRef,
          mapOf(
            "userId" to userId,
            "label" to chip.label.trim().ifBlank { "Stamp" }.take(24),
            "merchant" to chip.merchant.trim().ifBlank { "Household Note" }.take(120),
            "amountPaise" to chip.amountPaise.coerceIn(1L, 100_000_000_000L),
            "category" to chip.category.name,
            "direction" to chip.direction.name,
            "colorKey" to chip.colorKey.name,
            "sortOrder" to index.coerceIn(0, 200),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, collectionPath)
      throw e
    }
  }

  private fun DocumentSnapshot.toQuickChipOrNull(): QuickChip? {
    val labelStr = getString("label") ?: return null
    val merchantStr = getString("merchant") ?: labelStr
    val amount = getLong("amountPaise") ?: return null
    val catStr = getString("category") ?: Category.OTHER.name
    val dirStr = getString("direction") ?: Direction.DEBIT.name
    val colorStr = getString("colorKey") ?: StampColorKey.HONEY.name
    val order = (getLong("sortOrder") ?: 0L).toInt()
    return QuickChip(
      id = id,
      label = labelStr,
      merchant = merchantStr,
      amountPaise = amount,
      category = Category.fromName(catStr),
      direction = runCatching { Direction.valueOf(dirStr) }.getOrDefault(Direction.DEBIT),
      colorKey = StampColorKey.fromName(colorStr),
      sortOrder = order
    )
  }
}
