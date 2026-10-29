package com.example.data.repository

import com.example.core.model.Category
import com.example.core.model.CommitmentType
import com.example.core.model.PakkaCommitment
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

private fun sanitizeCommitmentId(rawId: String): String {
  val cleaned = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
  return cleaned.ifBlank { "pakka_${UUID.randomUUID().toString().take(8)}" }
}

class PakkaCommitmentRepository(
  private val db: FirebaseFirestore,
  private val userId: String
) {
  private val collectionRef
    get() = db.collection("users").document(userId).collection("pakke_commitments")

  private val collectionPath: String
    get() = "users/$userId/pakke_commitments"

  fun observeCommitments(): Flow<List<PakkaCommitment>> = callbackFlow {
    val registration = collectionRef
      .whereEqualTo("userId", userId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.LIST, collectionPath)
          close(error)
          return@addSnapshotListener
        }
        val items = snapshot?.documents
          ?.mapNotNull { it.toCommitmentOrNull() }
          ?.sortedWith(compareBy<PakkaCommitment>({ !it.isActive }, { it.safeDueDay }, { it.title }))
          ?: emptyList()
        trySend(items)
      }
    awaitClose { registration.remove() }
  }

  val allCommitments: Flow<List<PakkaCommitment>> = observeCommitments()
    .catch { emit(emptyList()) }

  suspend fun getCommitmentsSync(): List<PakkaCommitment> {
    return try {
      val snapshot = collectionRef
        .whereEqualTo("userId", userId)
        .get()
        .await()
      snapshot.documents
        .mapNotNull { it.toCommitmentOrNull() }
        .sortedWith(compareBy<PakkaCommitment>({ !it.isActive }, { it.safeDueDay }, { it.title }))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, collectionPath)
      throw e
    }
  }

  suspend fun upsert(item: PakkaCommitment): String {
    val docId = sanitizeCommitmentId(item.id)
    val docRef = collectionRef.document(docId)
    val cleanTitle = item.title.trim().ifBlank { item.type.displayName }.take(80)
    val cleanAmountPaise = item.amountPaise.coerceIn(100L, 100_000_000_000L)
    val cleanDueDay = item.dueDayOfMonth.coerceIn(1, 31)
    val cleanNotes = item.notes.trim().take(240)

    val updatePayload = mapOf(
      "title" to cleanTitle,
      "amountPaise" to cleanAmountPaise,
      "type" to item.type.name,
      "category" to item.category.name,
      "dueDayOfMonth" to cleanDueDay,
      "notes" to cleanNotes,
      "isActive" to item.isActive,
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

  suspend fun delete(commitmentId: String) {
    val docId = sanitizeCommitmentId(commitmentId)
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

  suspend fun resetToDefaults() {
    val existing = runCatching { getCommitmentsSync() }.getOrDefault(emptyList())
    val defaults = PakkaCommitment.defaultCommitments()
    val defaultIds = defaults.map { sanitizeCommitmentId(it.id) }.toSet()
    for (old in existing) {
      val cleanOldId = sanitizeCommitmentId(old.id)
      if (cleanOldId !in defaultIds) {
        delete(cleanOldId)
      }
    }
    for (def in defaults) {
      upsert(def)
    }
  }

  private fun DocumentSnapshot.toCommitmentOrNull(): PakkaCommitment? {
    val titleStr = getString("title")?.takeIf { it.isNotBlank() } ?: return null
    val amountPaise = getLong("amountPaise")?.takeIf { it > 0L } ?: return null
    val parsedType = CommitmentType.fromName(getString("type"))
    val parsedCategory = Category.fromName(getString("category") ?: parsedType.defaultCategory.name)
    val dueDay = (getLong("dueDayOfMonth")?.toInt() ?: 5).coerceIn(1, 31)
    val notesStr = getString("notes").orEmpty()
    val active = getBoolean("isActive") ?: true
    val updatedMs = getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
    return PakkaCommitment(
      id = id,
      title = titleStr,
      amountPaise = amountPaise,
      type = parsedType,
      category = parsedCategory,
      dueDayOfMonth = dueDay,
      notes = notesStr,
      isActive = active,
      updatedAtMs = updatedMs
    )
  }
}
