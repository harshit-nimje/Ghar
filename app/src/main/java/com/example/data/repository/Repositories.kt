package com.example.data.repository

import com.example.core.model.BudgetProfile
import com.example.core.model.CaptureSource
import com.example.core.model.Category
import com.example.core.model.ChatSender
import com.example.core.model.Direction
import com.example.core.model.GroundingSourceChip
import com.example.core.model.GuiltSensitivity
import com.example.core.model.HabitType
import com.example.core.model.InstrumentType
import com.example.core.model.MaaChatMessage
import com.example.core.model.PainDial
import com.example.core.model.PapaChatMessage
import com.example.core.model.SavingsGoal
import com.example.core.model.Transaction
import com.example.core.model.UserHousehold
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private fun sanitizeDocId(rawId: String): String {
  val cleaned = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
  return cleaned.ifBlank { UUID.randomUUID().toString() }
}

class TransactionRepository(
  private val db: FirebaseFirestore,
  private val userId: String
) {
  private val collectionRef
    get() = db.collection("users").document(userId).collection("transactions")

  private val collectionPath: String
    get() = "users/$userId/transactions"

  /**
   * Raw real-time Flow of user's transactions aligned with security rules (.whereEqualTo("userId", userId)).
   * Logs structured errors via handleFirestoreError and closes with the root exception for callers/tests.
   */
  fun observeTransactions(): Flow<List<Transaction>> = callbackFlow {
    val registration = collectionRef
      .whereEqualTo("userId", userId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.LIST, collectionPath)
          close(error)
          return@addSnapshotListener
        }
        val items = snapshot?.documents
          ?.mapNotNull { it.toTransactionOrNull() }
          ?.sortedByDescending { it.timestamp }
          ?: emptyList()
        trySend(items)
      }
    awaitClose { registration.remove() }
  }

  val allTransactions: Flow<List<Transaction>> = observeTransactions()
    .catch { emit(emptyList()) }

  val recentTransactions: Flow<List<Transaction>> = allTransactions
    .map { it.take(50) }

  suspend fun insert(transaction: Transaction): String {
    val docId = sanitizeDocId(transaction.id)
    val docRef = collectionRef.document(docId)
    val payload = buildMap<String, Any> {
      put("userId", userId)
      put("amountPaise", transaction.amountPaise.coerceAtLeast(1L))
      put("direction", transaction.direction.name)
      put("merchant", transaction.merchant.trim().ifBlank { "Household Note" }.take(120))
      put("category", transaction.category.name)
      put("timestamp", transaction.timestamp.coerceAtLeast(1L))
      put("source", transaction.source.name)
      transaction.rawText?.takeIf { it.isNotBlank() }?.let { put("rawText", it.take(500)) }
      put("isRecurring", transaction.isRecurring)
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }
    try {
      docRef.set(payload).await()
      return docId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun insertAll(transactions: List<Transaction>) {
    if (transactions.isEmpty()) return
    try {
      val batch = db.batch()
      transactions.forEach { tx ->
        val docId = sanitizeDocId(tx.id)
        val docRef = collectionRef.document(docId)
        val payload = buildMap<String, Any> {
          put("userId", userId)
          put("amountPaise", tx.amountPaise.coerceAtLeast(1L))
          put("direction", tx.direction.name)
          put("merchant", tx.merchant.trim().ifBlank { "Household Note" }.take(120))
          put("category", tx.category.name)
          put("timestamp", tx.timestamp.coerceAtLeast(1L))
          put("source", tx.source.name)
          tx.rawText?.takeIf { it.isNotBlank() }?.let { put("rawText", it.take(500)) }
          put("isRecurring", tx.isRecurring)
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
        batch.set(docRef, payload)
      }
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, collectionPath)
      throw e
    }
  }

  suspend fun update(transaction: Transaction) {
    val docId = sanitizeDocId(transaction.id)
    val docRef = collectionRef.document(docId)
    val updateMap = buildMap<String, Any> {
      put("amountPaise", transaction.amountPaise.coerceAtLeast(1L))
      put("direction", transaction.direction.name)
      put("merchant", transaction.merchant.trim().ifBlank { "Household Note" }.take(120))
      put("category", transaction.category.name)
      put("timestamp", transaction.timestamp.coerceAtLeast(1L))
      transaction.rawText?.takeIf { it.isNotBlank() }?.let { put("rawText", it.take(500)) }
      put("isRecurring", transaction.isRecurring)
      put("updatedAt", FieldValue.serverTimestamp())
    }
    try {
      docRef.update(updateMap).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun getById(id: String): Transaction? {
    val docId = sanitizeDocId(id)
    val docRef = collectionRef.document(docId)
    return try {
      val snap = docRef.get().await()
      if (snap.exists()) snap.toTransactionOrNull() else null
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun getByDateRange(start: Long, end: Long): List<Transaction> {
    return try {
      val snapshot = collectionRef
        .whereEqualTo("userId", userId)
        .get()
        .await()
      snapshot.documents
        .mapNotNull { it.toTransactionOrNull() }
        .filter { it.timestamp in start..end }
        .sortedByDescending { it.timestamp }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, collectionPath)
      throw e
    }
  }

  suspend fun delete(id: String) {
    val docId = sanitizeDocId(id)
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

  suspend fun clearAll() {
    try {
      val snapshot = collectionRef.whereEqualTo("userId", userId).get().await()
      if (snapshot.isEmpty) return
      val batch = db.batch()
      snapshot.documents.forEach { batch.delete(it.reference) }
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, collectionPath)
      throw e
    }
  }

  private fun DocumentSnapshot.toTransactionOrNull(): Transaction? {
    val amount = getLong("amountPaise") ?: return null
    val merchantName = getString("merchant") ?: return null
    val dirStr = getString("direction") ?: Direction.DEBIT.name
    val catStr = getString("category") ?: Category.OTHER.name
    val srcStr = getString("source") ?: CaptureSource.MANUAL.name
    return Transaction(
      id = id,
      amountPaise = amount,
      direction = runCatching { Direction.valueOf(dirStr) }.getOrDefault(Direction.DEBIT),
      merchant = merchantName,
      category = Category.fromName(catStr),
      timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
      source = runCatching { CaptureSource.valueOf(srcStr) }.getOrDefault(CaptureSource.MANUAL),
      rawText = getString("rawText"),
      isRecurring = getBoolean("isRecurring") ?: false
    )
  }
}

class BudgetRepository(
  private val db: FirebaseFirestore,
  private val userId: String,
  private val defaultDisplayName: String? = null
) {
  private val userDocRef
    get() = db.collection("users").document(userId)

  private val fallbackFirstName: String
    get() = defaultDisplayName
      ?.trim()
      ?.substringBefore(" ")
      ?.take(60)
      ?.takeIf { it.isNotBlank() }
      ?: "Friend"

  fun observeHousehold(): Flow<UserHousehold?> = callbackFlow {
    val registration = userDocRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.GET, userDocRef.path)
        close(error)
        return@addSnapshotListener
      }
      if (snapshot != null && snapshot.exists()) {
        trySend(snapshot.toUserHousehold())
      } else {
        trySend(null)
      }
    }
    awaitClose { registration.remove() }
  }

  val household: Flow<UserHousehold?> = observeHousehold()
    .catch { emit(null) }

  val profile: Flow<BudgetProfile> = household.map { it?.profile ?: BudgetProfile() }

  val userName: Flow<String> = household.map { doc ->
    doc?.userName?.takeIf { it.isNotBlank() } ?: fallbackFirstName
  }

  suspend fun getHouseholdSync(): UserHousehold? {
    return try {
      val snap = userDocRef.get().await()
      if (snap.exists()) snap.toUserHousehold() else null
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, userDocRef.path)
      throw e
    }
  }

  suspend fun getProfileSync(): BudgetProfile {
    return getHouseholdSync()?.profile ?: BudgetProfile()
  }

  suspend fun saveHouseholdCalibration(
    userName: String,
    onboardingCompleted: Boolean,
    profile: BudgetProfile,
    habitLeaks: Set<String>
  ) {
    val cleanName = userName.trim().ifBlank { fallbackFirstName }.take(60)
    val boundedLeaks = habitLeaks.map { it.take(40) }.take(10)
    val updatePayload = mapOf(
      "userName" to cleanName,
      "onboardingCompleted" to onboardingCompleted,
      "monthlyIncomePaise" to profile.monthlyIncomePaise.coerceAtLeast(0L),
      "fixedCommitmentsPaise" to profile.fixedCommitmentsPaise.coerceAtLeast(0L),
      "savingsTargetPaise" to profile.savingsTargetPaise.coerceAtLeast(0L),
      "monthStartDay" to profile.monthStartDay.coerceIn(1, 31),
      "guiltSensitivity" to profile.guiltSensitivity.name,
      "habitType" to profile.habitType.name,
      "habitLeaks" to boundedLeaks,
      "painDial" to profile.painDial.name,
      "ageBand" to profile.ageBand.ifBlank { "25-34" }.take(20),
      "lifeStage" to profile.lifeStage.ifBlank { "Salaried" }.take(50),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      val existing = userDocRef.get().await()
      if (existing.exists()) {
        userDocRef.update(updatePayload).await()
      } else {
        val createPayload = updatePayload + mapOf(
          "userId" to userId,
          "createdAt" to FieldValue.serverTimestamp()
        )
        userDocRef.set(createPayload).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userDocRef.path)
      throw e
    }
  }

  suspend fun saveProfile(profile: BudgetProfile) {
    val updatePayload = mapOf(
      "monthlyIncomePaise" to profile.monthlyIncomePaise.coerceAtLeast(0L),
      "fixedCommitmentsPaise" to profile.fixedCommitmentsPaise.coerceAtLeast(0L),
      "savingsTargetPaise" to profile.savingsTargetPaise.coerceAtLeast(0L),
      "monthStartDay" to profile.monthStartDay.coerceIn(1, 31),
      "guiltSensitivity" to profile.guiltSensitivity.name,
      "habitType" to profile.habitType.name,
      "painDial" to profile.painDial.name,
      "ageBand" to profile.ageBand.ifBlank { "25-34" }.take(20),
      "lifeStage" to profile.lifeStage.ifBlank { "Salaried" }.take(50),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      val existing = userDocRef.get().await()
      if (existing.exists()) {
        userDocRef.update(updatePayload).await()
      } else {
        saveHouseholdCalibration(
          userName = fallbackFirstName,
          onboardingCompleted = true,
          profile = profile,
          habitLeaks = setOf(profile.habitType.name)
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
      throw e
    }
  }

  suspend fun updateUserName(newName: String) {
    val cleanName = newName.trim().ifBlank { fallbackFirstName }.take(60)
    try {
      val existing = userDocRef.get().await()
      if (existing.exists()) {
        userDocRef.update(
          mapOf(
            "userName" to cleanName,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        saveHouseholdCalibration(
          userName = cleanName,
          onboardingCompleted = true,
          profile = BudgetProfile(),
          habitLeaks = setOf("CHAI_SNACKS")
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
      throw e
    }
  }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    try {
      val existing = userDocRef.get().await()
      if (existing.exists()) {
        userDocRef.update(
          mapOf(
            "onboardingCompleted" to completed,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
      throw e
    }
  }

  private fun DocumentSnapshot.toUserHousehold(): UserHousehold {
    val leaksRaw = (get("habitLeaks") as? List<*>)?.filterIsInstance<String>()
      ?: listOf("CHAI_SNACKS")
    val parsedProfile = BudgetProfile(
      monthlyIncomePaise = getLong("monthlyIncomePaise") ?: 60_000_00L,
      fixedCommitmentsPaise = getLong("fixedCommitmentsPaise") ?: 24_000_00L,
      savingsTargetPaise = getLong("savingsTargetPaise") ?: 12_000_00L,
      monthStartDay = (getLong("monthStartDay") ?: 1L).toInt(),
      guiltSensitivity = GuiltSensitivity.fromName(getString("guiltSensitivity")),
      habitType = HabitType.fromName(getString("habitType")),
      painDial = PainDial.fromName(getString("painDial")),
      ageBand = getString("ageBand") ?: "25-34",
      lifeStage = getString("lifeStage") ?: "Salaried"
    )
    return UserHousehold(
      userId = getString("userId") ?: id,
      userName = getString("userName") ?: fallbackFirstName,
      onboardingCompleted = getBoolean("onboardingCompleted") ?: false,
      profile = parsedProfile,
      habitLeaks = leaksRaw
    )
  }
}

class GoalRepository(
  private val db: FirebaseFirestore,
  private val userId: String
) {
  private val goalsCollection
    get() = db.collection("users").document(userId).collection("goals")

  private val collectionPath: String
    get() = "users/$userId/goals"

  private val primaryGoalDocRef
    get() = goalsCollection.document("primary_goal")

  fun observePrimaryGoal(): Flow<SavingsGoal> = callbackFlow {
    val registration = primaryGoalDocRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.GET, primaryGoalDocRef.path)
        close(error)
        return@addSnapshotListener
      }
      if (snapshot != null && snapshot.exists()) {
        trySend(snapshot.toSavingsGoal())
      } else {
        trySend(SavingsGoal())
      }
    }
    awaitClose { registration.remove() }
  }

  fun observeAllGoals(): Flow<List<SavingsGoal>> = callbackFlow {
    val registration = goalsCollection.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, collectionPath)
        close(error)
        return@addSnapshotListener
      }
      val items = snapshot?.documents?.map { it.toSavingsGoal() }.orEmpty()
      trySend(items)
    }
    awaitClose { registration.remove() }
  }

  val primaryGoal: Flow<SavingsGoal> = observePrimaryGoal()
    .catch { emit(SavingsGoal()) }

  suspend fun getGoalSync(): SavingsGoal {
    return try {
      val snap = primaryGoalDocRef.get().await()
      if (snap.exists()) snap.toSavingsGoal() else SavingsGoal(targetAmountPaise = 0L, currentAmountPaise = 0L)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, primaryGoalDocRef.path)
      throw e
    }
  }

  suspend fun getAllGoalsSync(): List<SavingsGoal> {
    return try {
      val snap = goalsCollection.get().await()
      snap.documents.map { it.toSavingsGoal() }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, collectionPath)
      throw e
    }
  }

  suspend fun saveGoal(goal: SavingsGoal) {
    val docId = sanitizeDocId(goal.id.ifBlank { "primary_goal" })
    val docRef = goalsCollection.document(docId)
    val updatePayload = buildMap<String, Any> {
      put("name", goal.name.trim().ifBlank { "Himachal Trip" }.take(80))
      put("targetAmountPaise", goal.targetAmountPaise.coerceAtLeast(0L))
      put("currentAmountPaise", goal.currentAmountPaise.coerceAtLeast(0L))
      goal.targetDateEpochMillis?.takeIf { it > 0L }?.let { put("targetDateEpochMillis", it) }
      put("instrumentType", goal.instrumentType.name)
      put("updatedAt", FieldValue.serverTimestamp())
    }
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
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun deleteGoal(goalId: String) {
    val docId = sanitizeDocId(goalId)
    if (docId == "primary_goal") return
    val docRef = goalsCollection.document(docId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun addToGullak(amountPaise: Long, goalId: String = "primary_goal") {
    if (amountPaise <= 0L) return
    val docId = sanitizeDocId(goalId.ifBlank { "primary_goal" })
    val docRef = goalsCollection.document(docId)
    try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(
          mapOf(
            "currentAmountPaise" to FieldValue.increment(amountPaise),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        val defaultGoal = SavingsGoal(id = docId)
        saveGoal(defaultGoal.copy(currentAmountPaise = defaultGoal.currentAmountPaise + amountPaise))
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun setGullakBalance(newBalancePaise: Long, goalId: String = "primary_goal") {
    val bounded = newBalancePaise.coerceIn(0L, 100_000_000_000L)
    val docId = sanitizeDocId(goalId.ifBlank { "primary_goal" })
    val docRef = goalsCollection.document(docId)
    try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(
          mapOf(
            "currentAmountPaise" to bounded,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        val defaultGoal = SavingsGoal(id = docId)
        saveGoal(defaultGoal.copy(currentAmountPaise = bounded))
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  private fun DocumentSnapshot.toSavingsGoal(): SavingsGoal {
    return SavingsGoal(
      id = id,
      name = getString("name") ?: "Himachal Trip",
      targetAmountPaise = getLong("targetAmountPaise") ?: 40_000_00L,
      currentAmountPaise = getLong("currentAmountPaise") ?: 0L,
      targetDateEpochMillis = getLong("targetDateEpochMillis"),
      instrumentType = InstrumentType.fromName(getString("instrumentType"))
    )
  }
}

class MaaChatRepository(
  private val db: FirebaseFirestore,
  private val userId: String
) {
  private val chatsCollection
    get() = db.collection("users").document(userId).collection("maa_chats")

  private val collectionPath: String
    get() = "users/$userId/maa_chats"

  fun observeMessages(): Flow<List<MaaChatMessage>> = callbackFlow {
    val registration = chatsCollection
      .whereEqualTo("userId", userId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.LIST, collectionPath)
          close(error)
          return@addSnapshotListener
        }
        val messages = snapshot?.documents
          ?.mapNotNull { it.toMaaChatMessageOrNull() }
          ?.sortedBy { it.timestamp }
          ?: emptyList()
        trySend(messages)
      }
    awaitClose { registration.remove() }
  }

  suspend fun appendMessage(sender: ChatSender, text: String, timestamp: Long = System.currentTimeMillis()): String {
    val cleanText = text.trim().take(4000)
    if (cleanText.isEmpty()) return ""
    val docId = UUID.randomUUID().toString()
    val docRef = chatsCollection.document(docId)
    val payload = mapOf(
      "userId" to userId,
      "sender" to sender.name,
      "text" to cleanText,
      "timestamp" to timestamp.coerceAtLeast(1L),
      "createdAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(payload).await()
      return docId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun clearHistory() {
    try {
      val snapshot = chatsCollection.whereEqualTo("userId", userId).get().await()
      if (snapshot.isEmpty) return
      val batch = db.batch()
      snapshot.documents.forEach { batch.delete(it.reference) }
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, collectionPath)
      throw e
    }
  }

  private fun DocumentSnapshot.toMaaChatMessageOrNull(): MaaChatMessage? {
    val body = getString("text") ?: return null
    val senderStr = getString("sender") ?: ChatSender.MAA.name
    return MaaChatMessage(
      id = id,
      sender = runCatching { ChatSender.valueOf(senderStr) }.getOrDefault(ChatSender.MAA),
      text = body,
      timestamp = getLong("timestamp") ?: System.currentTimeMillis()
    )
  }
}

class PapaChatRepository(
  private val db: FirebaseFirestore,
  private val userId: String
) {
  private val chatsCollection
    get() = db.collection("users").document(userId).collection("papa_chats")

  private val collectionPath: String
    get() = "users/$userId/papa_chats"

  fun observeMessages(): Flow<List<PapaChatMessage>> = callbackFlow {
    val registration = chatsCollection
      .whereEqualTo("userId", userId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.LIST, collectionPath)
          close(error)
          return@addSnapshotListener
        }
        val messages = snapshot?.documents
          ?.mapNotNull { it.toPapaChatMessageOrNull() }
          ?.sortedBy { it.timestamp }
          ?: emptyList()
        trySend(messages)
      }
    awaitClose { registration.remove() }
  }

  suspend fun appendMessage(
    sender: ChatSender,
    text: String,
    sources: List<GroundingSourceChip> = emptyList(),
    timestamp: Long = System.currentTimeMillis()
  ): String {
    val cleanText = text.trim().take(4000)
    if (cleanText.isEmpty()) return ""
    val normalizedSender = if (sender == ChatSender.USER) ChatSender.USER else ChatSender.PAPA
    val docId = UUID.randomUUID().toString()
    val docRef = chatsCollection.document(docId)
    val tempMsg = PapaChatMessage(
      id = docId,
      sender = normalizedSender,
      text = cleanText,
      timestamp = timestamp.coerceAtLeast(1L),
      sources = sources
    )
    val sourcesJson = tempMsg.sourcesToJsonString()
    val payload = buildMap<String, Any> {
      put("userId", userId)
      put("sender", normalizedSender.name)
      put("text", cleanText)
      put("timestamp", timestamp.coerceAtLeast(1L))
      if (sourcesJson.isNotEmpty()) {
        put("sourcesJson", sourcesJson)
      }
      put("createdAt", FieldValue.serverTimestamp())
    }
    try {
      docRef.set(payload).await()
      return docId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun clearHistory() {
    try {
      val snapshot = chatsCollection.whereEqualTo("userId", userId).get().await()
      if (snapshot.isEmpty) return
      val batch = db.batch()
      snapshot.documents.forEach { batch.delete(it.reference) }
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, collectionPath)
      throw e
    }
  }

  private fun DocumentSnapshot.toPapaChatMessageOrNull(): PapaChatMessage? {
    val body = getString("text") ?: return null
    val senderStr = getString("sender") ?: ChatSender.PAPA.name
    val parsedSender = runCatching { ChatSender.valueOf(senderStr) }.getOrDefault(ChatSender.PAPA)
    return PapaChatMessage(
      id = id,
      sender = if (parsedSender == ChatSender.USER) ChatSender.USER else ChatSender.PAPA,
      text = body,
      timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
      sources = PapaChatMessage.parseSourcesJson(getString("sourcesJson"))
    )
  }
}
