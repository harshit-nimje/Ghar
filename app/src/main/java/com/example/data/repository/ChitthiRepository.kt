package com.example.data.repository

import com.example.core.model.ChitthiAuthor
import com.example.core.model.ChitthiNote
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

private fun sanitizeChitthiId(rawId: String): String {
  val cleaned = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
  return cleaned.ifBlank { "chitthi_${UUID.randomUUID()}" }
}

class ChitthiRepository(
  private val db: FirebaseFirestore,
  private val userId: String
) {
  private val collectionRef
    get() = db.collection("users").document(userId).collection("chitthis")

  private val collectionPath: String
    get() = "users/$userId/chitthis"

  fun observeChitthis(): Flow<List<ChitthiNote>> = callbackFlow {
    val registration = collectionRef
      .whereEqualTo("userId", userId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.LIST, collectionPath)
          close(error)
          return@addSnapshotListener
        }
        val items = snapshot?.documents
          ?.mapNotNull { it.toChitthiOrNull() }
          ?.sortedByDescending { it.savedAt }
          ?: emptyList()
        trySend(items)
      }
    awaitClose { registration.remove() }
  }

  val allChitthis: Flow<List<ChitthiNote>> = observeChitthis()
    .catch { emit(emptyList()) }

  suspend fun getChitthisSync(): List<ChitthiNote> {
    return try {
      val snapshot = collectionRef
        .whereEqualTo("userId", userId)
        .get()
        .await()
      snapshot.documents
        .mapNotNull { it.toChitthiOrNull() }
        .sortedByDescending { it.savedAt }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, collectionPath)
      throw e
    }
  }

  suspend fun upsert(note: ChitthiNote): String {
    val docId = sanitizeChitthiId(note.id)
    val docRef = collectionRef.document(docId)
    val cleanTitle = note.title.trim().ifBlank { "${note.author.displayName}'s Chitthi" }.take(80)
    val cleanBody = note.body.trim().ifBlank { "Keep calm and count every rupee gently." }.take(1000)
    val cleanTag = note.tag.trim().ifBlank { "Rasoi Wisdom" }.take(40)
    val cleanSavedAt = note.savedAt.coerceAtLeast(1L)

    val updatePayload = mapOf(
      "title" to cleanTitle,
      "body" to cleanBody,
      "tag" to cleanTag,
      "savedAt" to cleanSavedAt,
      "updatedAt" to FieldValue.serverTimestamp()
    )

    try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(updatePayload).await()
      } else {
        val createPayload = updatePayload + mapOf(
          "userId" to userId,
          "author" to note.author.name,
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

  suspend fun delete(chitthiId: String) {
    val docId = sanitizeChitthiId(chitthiId)
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

  private fun DocumentSnapshot.toChitthiOrNull(): ChitthiNote? {
    val bodyStr = getString("body") ?: return null
    val titleStr = getString("title") ?: "Household Chitthi"
    val authorStr = getString("author") ?: ChitthiAuthor.MAA.name
    val tagStr = getString("tag") ?: "Rasoi Wisdom"
    return ChitthiNote(
      id = id,
      author = ChitthiAuthor.fromName(authorStr),
      title = titleStr,
      body = bodyStr,
      tag = tagStr,
      savedAt = getLong("savedAt") ?: System.currentTimeMillis()
    )
  }
}
