package com.example.data.repository

import android.util.Log
import com.example.core.model.BudgetProfile
import com.example.core.model.CaptureSource
import com.example.core.model.ChatSender
import com.example.core.model.ChitthiNote
import com.example.core.model.GroundingSourceChip
import com.example.core.model.MaaChatMessage
import com.example.core.model.PakkaCommitment
import com.example.core.model.PapaChatMessage
import com.example.core.model.QuickChip
import com.example.core.model.SavingsGoal
import com.example.core.model.Transaction
import com.example.core.model.UserHousehold
import com.example.data.local.LocalHouseholdVault
import com.example.data.preferences.PreferencesRepository
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import androidx.compose.runtime.Stable

private const val TAG = "HouseholdStores"

@Stable
interface TransactionStore {
  val transactionsState: StateFlow<List<Transaction>>
  val allTransactions: Flow<List<Transaction>>
  fun insert(transaction: Transaction)
  fun insertAll(transactions: List<Transaction>, markStarterSeeded: Boolean = false)
  fun update(transaction: Transaction)
  fun delete(id: String)
  fun clearAll()
}

@Stable
interface BudgetStore {
  val householdState: StateFlow<UserHousehold>
  val profileState: StateFlow<BudgetProfile>
  val userNameState: StateFlow<String>
  val profile: Flow<BudgetProfile>
  val userName: Flow<String>
  fun saveHouseholdCalibration(
    userName: String,
    onboardingCompleted: Boolean,
    profile: BudgetProfile,
    habitLeaks: Set<String>
  )
  fun saveProfile(profile: BudgetProfile)
  fun updateUserName(newName: String)
  fun setOnboardingCompleted(completed: Boolean)
}

@Stable
interface GoalStore {
  val primaryGoalState: StateFlow<SavingsGoal>
  val allGoalsState: StateFlow<List<SavingsGoal>>
  val activeGoalIdState: StateFlow<String>
  val primaryGoal: Flow<SavingsGoal>
  fun saveGoal(goal: SavingsGoal)
  fun createGoal(goal: SavingsGoal, makeActive: Boolean = true)
  fun selectGoal(goalId: String)
  fun deleteGoal(goalId: String)
  fun addToGullak(amountPaise: Long)
  fun adjustGullakDelta(deltaPaise: Long)
  fun setGullakBalance(newBalancePaise: Long)
}

@Stable
interface QuickChipStore {
  val quickChipsState: StateFlow<List<QuickChip>>
  val allQuickChips: Flow<List<QuickChip>>
  fun upsert(chip: QuickChip)
  fun updateSortOrders(orderedChips: List<QuickChip>)
  fun delete(chipId: String)
  fun resetToDefaults()
  fun insertDefaultsIfEmpty()
}

@Stable
interface MaaChatStore {
  val messagesState: StateFlow<List<MaaChatMessage>>
  fun observeMessages(): Flow<List<MaaChatMessage>>
  suspend fun appendMessage(sender: ChatSender, text: String): String
  suspend fun clearHistory()
}

@Stable
interface PapaChatStore {
  val messagesState: StateFlow<List<PapaChatMessage>>
  fun observeMessages(): Flow<List<PapaChatMessage>>
  suspend fun appendMessage(
    sender: ChatSender,
    text: String,
    sources: List<GroundingSourceChip> = emptyList()
  ): String
  suspend fun clearHistory()
}

@Stable
interface ChitthiStore {
  val chitthisState: StateFlow<List<ChitthiNote>>
  val allChitthis: Flow<List<ChitthiNote>>
  fun stash(note: ChitthiNote)
  fun remove(id: String)
}

@Stable
interface CommitmentStore {
  val commitmentsState: StateFlow<List<PakkaCommitment>>
  val allCommitments: Flow<List<PakkaCommitment>>
  fun upsert(item: PakkaCommitment)
  fun delete(id: String)
  fun resetToDefaults()
}

// ============================================================================
// LOCAL-ONLY STORES (Guest Mode — Zero Firestore Calls, 100% On-Device Persistent)
// ============================================================================

class LocalTransactionStore(
  private val vault: LocalHouseholdVault
) : TransactionStore {
  override val transactionsState: StateFlow<List<Transaction>> = vault.transactions
  override val allTransactions: Flow<List<Transaction>> = vault.transactions

  override fun insert(transaction: Transaction) {
    vault.upsertTransactionFromLocal(transaction)
  }

  override fun insertAll(transactions: List<Transaction>, markStarterSeeded: Boolean) {
    vault.insertTransactionsBatchFromLocal(transactions, markStarterSeeded = markStarterSeeded)
  }

  override fun update(transaction: Transaction) {
    vault.upsertTransactionFromLocal(transaction)
  }

  override fun delete(id: String) {
    vault.deleteTransactionFromLocal(id)
  }

  override fun clearAll() {
    vault.clearTransactionsFromLocal()
  }
}

class LocalBudgetStore(
  private val vault: LocalHouseholdVault,
  appScope: CoroutineScope,
  private val defaultName: String = "Friend"
) : BudgetStore {
  override val householdState: StateFlow<UserHousehold> = vault.household

  override val profileState: StateFlow<BudgetProfile> = vault.household
    .map { it.profile }
    .stateIn(appScope, SharingStarted.Eagerly, vault.household.value.profile)

  override val userNameState: StateFlow<String> = vault.household
    .map { it.userName.takeIf { n -> n.isNotBlank() } ?: defaultName }
    .stateIn(
      appScope,
      SharingStarted.Eagerly,
      vault.household.value.userName.takeIf { it.isNotBlank() } ?: defaultName
    )

  override val profile: Flow<BudgetProfile> = profileState
  override val userName: Flow<String> = userNameState

  override fun saveHouseholdCalibration(
    userName: String,
    onboardingCompleted: Boolean,
    profile: BudgetProfile,
    habitLeaks: Set<String>
  ) {
    val cleanName = userName.trim().ifBlank { defaultName }.take(60)
    vault.updateHouseholdFromLocal(
      isCalibration = true,
      transform = { cur ->
        cur.copy(
          userName = cleanName,
          onboardingCompleted = onboardingCompleted,
          profile = profile,
          habitLeaks = habitLeaks.toList().ifEmpty { listOf("CHAI_SNACKS") }
        )
      }
    )
  }

  override fun saveProfile(profile: BudgetProfile) {
    vault.updateHouseholdFromLocal(
      isCalibration = true,
      transform = { cur -> cur.copy(profile = profile) }
    )
  }

  override fun updateUserName(newName: String) {
    val cleanName = newName.trim().ifBlank { defaultName }.take(60)
    vault.updateHouseholdFromLocal(
      isCalibration = true,
      transform = { cur -> cur.copy(userName = cleanName) }
    )
  }

  override fun setOnboardingCompleted(completed: Boolean) {
    if (!completed) {
      vault.explicitResetOnboarding()
    } else {
      vault.updateHouseholdFromLocal(transform = { cur -> cur.copy(onboardingCompleted = true) })
    }
  }
}

class LocalGoalStore(
  private val vault: LocalHouseholdVault
) : GoalStore {
  override val primaryGoalState: StateFlow<SavingsGoal> = vault.goal
  override val allGoalsState: StateFlow<List<SavingsGoal>> = vault.goals
  override val activeGoalIdState: StateFlow<String> = vault.activeGoalId
  override val primaryGoal: Flow<SavingsGoal> = vault.goal

  override fun saveGoal(goal: SavingsGoal) {
    vault.upsertGoalByIdFromLocal(goal.copy(id = goal.id.ifBlank { vault.activeGoalId.value }), makeActive = false)
  }

  override fun createGoal(goal: SavingsGoal, makeActive: Boolean) {
    vault.upsertGoalByIdFromLocal(goal, makeActive = makeActive)
  }

  override fun selectGoal(goalId: String) {
    vault.selectActiveGoalFromLocal(goalId)
  }

  override fun deleteGoal(goalId: String) {
    vault.deleteGoalFromLocal(goalId)
  }

  override fun addToGullak(amountPaise: Long) {
    if (amountPaise <= 0L) return
    adjustGullakDelta(amountPaise)
  }

  override fun adjustGullakDelta(deltaPaise: Long) {
    if (deltaPaise == 0L) return
    vault.updateGoalFromLocal { cur ->
      cur.copy(currentAmountPaise = (cur.currentAmountPaise + deltaPaise).coerceAtLeast(0L))
    }
  }

  override fun setGullakBalance(newBalancePaise: Long) {
    vault.updateGoalFromLocal { cur ->
      cur.copy(currentAmountPaise = newBalancePaise.coerceAtLeast(0L))
    }
  }
}

class LocalQuickChipStore(
  private val vault: LocalHouseholdVault
) : QuickChipStore {
  override val quickChipsState: StateFlow<List<QuickChip>> = vault.quickChips
  override val allQuickChips: Flow<List<QuickChip>> = vault.quickChips

  override fun upsert(chip: QuickChip) {
    vault.upsertQuickChipFromLocal(chip)
  }

  override fun updateSortOrders(orderedChips: List<QuickChip>) {
    vault.replaceQuickChipsFromLocal(orderedChips)
  }

  override fun delete(chipId: String) {
    vault.deleteQuickChipFromLocal(chipId)
  }

  override fun resetToDefaults() {
    vault.replaceQuickChipsFromLocal(QuickChip.defaultChips())
  }

  override fun insertDefaultsIfEmpty() {
    if (vault.quickChips.value.isEmpty()) {
      vault.replaceQuickChipsFromLocal(QuickChip.defaultChips())
    }
  }
}

class LocalMaaChatStore(
  private val vault: LocalHouseholdVault
) : MaaChatStore {
  override val messagesState: StateFlow<List<MaaChatMessage>> = vault.maaChats

  override fun observeMessages(): Flow<List<MaaChatMessage>> = vault.maaChats

  override suspend fun appendMessage(sender: ChatSender, text: String): String {
    val clean = text.trim().take(4000)
    if (clean.isEmpty()) return ""
    val id = UUID.randomUUID().toString()
    vault.appendMaaChatFromLocal(
      MaaChatMessage(
        id = id,
        sender = sender,
        text = clean,
        timestamp = System.currentTimeMillis()
      )
    )
    return id
  }

  override suspend fun clearHistory() {
    vault.clearMaaChatFromLocal()
  }
}

class LocalPapaChatStore(
  private val vault: LocalHouseholdVault
) : PapaChatStore {
  override val messagesState: StateFlow<List<PapaChatMessage>> = vault.papaChats

  override fun observeMessages(): Flow<List<PapaChatMessage>> = vault.papaChats

  override suspend fun appendMessage(
    sender: ChatSender,
    text: String,
    sources: List<GroundingSourceChip>
  ): String {
    val clean = text.trim().take(4000)
    if (clean.isEmpty()) return ""
    val normalizedSender = if (sender == ChatSender.USER) ChatSender.USER else ChatSender.PAPA
    val id = UUID.randomUUID().toString()
    vault.appendPapaChatFromLocal(
      PapaChatMessage(
        id = id,
        sender = normalizedSender,
        text = clean,
        timestamp = System.currentTimeMillis(),
        sources = sources
      )
    )
    return id
  }

  override suspend fun clearHistory() {
    vault.clearPapaChatFromLocal()
  }
}

class LocalChitthiStore(
  private val vault: LocalHouseholdVault
) : ChitthiStore {
  override val chitthisState: StateFlow<List<ChitthiNote>> = vault.chitthis
  override val allChitthis: Flow<List<ChitthiNote>> = vault.chitthis

  override fun stash(note: ChitthiNote) {
    vault.upsertChitthiFromLocal(note)
  }

  override fun remove(id: String) {
    vault.deleteChitthiFromLocal(id)
  }
}

class LocalCommitmentStore(
  private val vault: LocalHouseholdVault
) : CommitmentStore {
  override val commitmentsState: StateFlow<List<PakkaCommitment>> = vault.commitments
  override val allCommitments: Flow<List<PakkaCommitment>> = vault.commitments

  override fun upsert(item: PakkaCommitment) {
    vault.upsertCommitmentFromLocal(item)
  }

  override fun delete(id: String) {
    vault.deleteCommitmentFromLocal(id)
  }

  override fun resetToDefaults() {
    vault.replaceCommitmentsFromLocal(PakkaCommitment.defaultCommitments())
  }
}

// ============================================================================
// CLOUD-SYNCED STORES (Authenticated User — 0ms Optimistic Vault + AppScope Cloud Sync)
// ============================================================================

class CloudSyncedTransactionStore(
  private val vault: LocalHouseholdVault,
  private val remote: TransactionRepository,
  private val appScope: CoroutineScope
) : TransactionStore {
  override val transactionsState: StateFlow<List<Transaction>> = vault.transactions
  override val allTransactions: Flow<List<Transaction>> = vault.transactions

  override fun insert(transaction: Transaction) {
    vault.upsertTransactionFromLocal(transaction)
    appScope.launch {
      runCatching { remote.insert(transaction) }
    }
  }

  override fun insertAll(transactions: List<Transaction>, markStarterSeeded: Boolean) {
    vault.insertTransactionsBatchFromLocal(transactions, markStarterSeeded = markStarterSeeded)
    if (transactions.isNotEmpty()) {
      appScope.launch {
        runCatching { remote.insertAll(transactions) }
      }
    }
  }

  override fun update(transaction: Transaction) {
    vault.upsertTransactionFromLocal(transaction)
    appScope.launch {
      runCatching { remote.update(transaction) }
    }
  }

  override fun delete(id: String) {
    vault.deleteTransactionFromLocal(id)
    appScope.launch {
      runCatching { remote.delete(id) }
    }
  }

  override fun clearAll() {
    vault.clearTransactionsFromLocal()
    appScope.launch {
      runCatching { remote.clearAll() }
    }
  }
}

class CloudSyncedBudgetStore(
  private val vault: LocalHouseholdVault,
  private val remote: BudgetRepository,
  private val appScope: CoroutineScope,
  private val defaultName: String
) : BudgetStore {
  override val householdState: StateFlow<UserHousehold> = vault.household

  override val profileState: StateFlow<BudgetProfile> = vault.household
    .map { it.profile }
    .stateIn(appScope, SharingStarted.Eagerly, vault.household.value.profile)

  override val userNameState: StateFlow<String> = vault.household
    .map { it.userName.takeIf { n -> n.isNotBlank() } ?: defaultName }
    .stateIn(
      appScope,
      SharingStarted.Eagerly,
      vault.household.value.userName.takeIf { it.isNotBlank() } ?: defaultName
    )

  override val profile: Flow<BudgetProfile> = profileState
  override val userName: Flow<String> = userNameState

  override fun saveHouseholdCalibration(
    userName: String,
    onboardingCompleted: Boolean,
    profile: BudgetProfile,
    habitLeaks: Set<String>
  ) {
    val cleanName = userName.trim().ifBlank { defaultName }.take(60)
    vault.updateHouseholdFromLocal(
      isCalibration = true,
      transform = { cur ->
        cur.copy(
          userName = cleanName,
          onboardingCompleted = onboardingCompleted,
          profile = profile,
          habitLeaks = habitLeaks.toList().ifEmpty { listOf("CHAI_SNACKS") }
        )
      }
    )
    appScope.launch {
      runCatching {
        remote.saveHouseholdCalibration(
          userName = cleanName,
          onboardingCompleted = onboardingCompleted,
          profile = profile,
          habitLeaks = habitLeaks
        )
      }
    }
  }

  override fun saveProfile(profile: BudgetProfile) {
    vault.updateHouseholdFromLocal(
      isCalibration = true,
      transform = { cur -> cur.copy(profile = profile) }
    )
    appScope.launch {
      runCatching { remote.saveProfile(profile) }
    }
  }

  override fun updateUserName(newName: String) {
    val cleanName = newName.trim().ifBlank { defaultName }.take(60)
    vault.updateHouseholdFromLocal(
      isCalibration = true,
      transform = { cur -> cur.copy(userName = cleanName) }
    )
    appScope.launch {
      runCatching { remote.updateUserName(cleanName) }
    }
  }

  override fun setOnboardingCompleted(completed: Boolean) {
    if (!completed) {
      vault.explicitResetOnboarding()
    } else {
      vault.updateHouseholdFromLocal(transform = { cur -> cur.copy(onboardingCompleted = true) })
    }
    appScope.launch {
      runCatching { remote.setOnboardingCompleted(completed) }
    }
  }
}

class CloudSyncedGoalStore(
  private val vault: LocalHouseholdVault,
  private val remote: GoalRepository,
  private val appScope: CoroutineScope
) : GoalStore {
  override val primaryGoalState: StateFlow<SavingsGoal> = vault.goal
  override val allGoalsState: StateFlow<List<SavingsGoal>> = vault.goals
  override val activeGoalIdState: StateFlow<String> = vault.activeGoalId
  override val primaryGoal: Flow<SavingsGoal> = vault.goal

  override fun saveGoal(goal: SavingsGoal) {
    val cleanId = goal.id.ifBlank { vault.activeGoalId.value.ifBlank { "primary_goal" } }
    val normalized = goal.copy(id = cleanId)
    vault.upsertGoalByIdFromLocal(normalized, makeActive = false)
    appScope.launch {
      runCatching { remote.saveGoal(normalized) }
    }
  }

  override fun createGoal(goal: SavingsGoal, makeActive: Boolean) {
    val cleanId = goal.id.ifBlank { "gullak_${UUID.randomUUID().toString().take(8)}" }
    val normalized = goal.copy(id = cleanId)
    vault.upsertGoalByIdFromLocal(normalized, makeActive = makeActive)
    appScope.launch {
      runCatching { remote.saveGoal(normalized) }
    }
  }

  override fun selectGoal(goalId: String) {
    vault.selectActiveGoalFromLocal(goalId)
  }

  override fun deleteGoal(goalId: String) {
    if (goalId == "primary_goal") return
    val deleted = vault.deleteGoalFromLocal(goalId)
    if (deleted) {
      appScope.launch {
        runCatching { remote.deleteGoal(goalId) }
      }
    }
  }

  override fun addToGullak(amountPaise: Long) {
    if (amountPaise <= 0L) return
    val targetGoalId = vault.activeGoalId.value.ifBlank { "primary_goal" }
    vault.updateGoalFromLocal { cur ->
      cur.copy(currentAmountPaise = cur.currentAmountPaise + amountPaise)
    }
    appScope.launch {
      runCatching { remote.addToGullak(amountPaise, goalId = targetGoalId) }
    }
  }

  override fun adjustGullakDelta(deltaPaise: Long) {
    if (deltaPaise == 0L) return
    val updated = vault.updateGoalFromLocal { cur ->
      cur.copy(currentAmountPaise = (cur.currentAmountPaise + deltaPaise).coerceAtLeast(0L))
    }
    appScope.launch {
      runCatching { remote.setGullakBalance(updated.currentAmountPaise, goalId = updated.id) }
    }
  }

  override fun setGullakBalance(newBalancePaise: Long) {
    val bounded = newBalancePaise.coerceAtLeast(0L)
    val updated = vault.updateGoalFromLocal { cur ->
      cur.copy(currentAmountPaise = bounded)
    }
    appScope.launch {
      runCatching { remote.setGullakBalance(bounded, goalId = updated.id) }
    }
  }
}

class CloudSyncedQuickChipStore(
  private val vault: LocalHouseholdVault,
  private val remote: QuickChipRepository,
  private val appScope: CoroutineScope
) : QuickChipStore {
  override val quickChipsState: StateFlow<List<QuickChip>> = vault.quickChips
  override val allQuickChips: Flow<List<QuickChip>> = vault.quickChips

  override fun upsert(chip: QuickChip) {
    vault.upsertQuickChipFromLocal(chip)
    appScope.launch {
      runCatching { remote.upsert(chip) }
    }
  }

  override fun updateSortOrders(orderedChips: List<QuickChip>) {
    vault.replaceQuickChipsFromLocal(orderedChips)
    appScope.launch {
      runCatching { remote.updateSortOrders(orderedChips) }
    }
  }

  override fun delete(chipId: String) {
    vault.deleteQuickChipFromLocal(chipId)
    appScope.launch {
      runCatching { remote.delete(chipId) }
    }
  }

  override fun resetToDefaults() {
    vault.replaceQuickChipsFromLocal(QuickChip.defaultChips())
    appScope.launch {
      runCatching { remote.resetToDefaults() }
    }
  }

  override fun insertDefaultsIfEmpty() {
    if (vault.quickChips.value.isEmpty()) {
      vault.replaceQuickChipsFromLocal(QuickChip.defaultChips())
    }
    appScope.launch {
      runCatching { remote.insertDefaultsIfEmpty() }
    }
  }
}

class CloudSyncedMaaChatStore(
  private val vault: LocalHouseholdVault,
  private val remote: MaaChatRepository,
  private val appScope: CoroutineScope
) : MaaChatStore {
  override val messagesState: StateFlow<List<MaaChatMessage>> = vault.maaChats

  override fun observeMessages(): Flow<List<MaaChatMessage>> = vault.maaChats

  override suspend fun appendMessage(sender: ChatSender, text: String): String {
    val clean = text.trim().take(4000)
    if (clean.isEmpty()) return ""
    val localId = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()
    vault.appendMaaChatFromLocal(
      MaaChatMessage(
        id = localId,
        sender = sender,
        text = clean,
        timestamp = now
      )
    )
    appScope.launch {
      runCatching { remote.appendMessage(sender, clean, now) }
    }
    return localId
  }

  override suspend fun clearHistory() {
    vault.clearMaaChatFromLocal()
    appScope.launch {
      runCatching { remote.clearHistory() }
    }
  }
}

class CloudSyncedPapaChatStore(
  private val vault: LocalHouseholdVault,
  private val remote: PapaChatRepository,
  private val appScope: CoroutineScope
) : PapaChatStore {
  override val messagesState: StateFlow<List<PapaChatMessage>> = vault.papaChats

  override fun observeMessages(): Flow<List<PapaChatMessage>> = vault.papaChats

  override suspend fun appendMessage(
    sender: ChatSender,
    text: String,
    sources: List<GroundingSourceChip>
  ): String {
    val clean = text.trim().take(4000)
    if (clean.isEmpty()) return ""
    val normalizedSender = if (sender == ChatSender.USER) ChatSender.USER else ChatSender.PAPA
    val localId = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()
    vault.appendPapaChatFromLocal(
      PapaChatMessage(
        id = localId,
        sender = normalizedSender,
        text = clean,
        timestamp = now,
        sources = sources
      )
    )
    appScope.launch {
      runCatching { remote.appendMessage(normalizedSender, clean, sources, now) }
    }
    return localId
  }

  override suspend fun clearHistory() {
    vault.clearPapaChatFromLocal()
    appScope.launch {
      runCatching { remote.clearHistory() }
    }
  }
}

class CloudSyncedChitthiStore(
  private val vault: LocalHouseholdVault,
  private val remote: ChitthiRepository,
  private val appScope: CoroutineScope
) : ChitthiStore {
  override val chitthisState: StateFlow<List<ChitthiNote>> = vault.chitthis
  override val allChitthis: Flow<List<ChitthiNote>> = vault.chitthis

  override fun stash(note: ChitthiNote) {
    vault.upsertChitthiFromLocal(note)
    appScope.launch {
      runCatching { remote.upsert(note) }
    }
  }

  override fun remove(id: String) {
    vault.deleteChitthiFromLocal(id)
    appScope.launch {
      runCatching { remote.delete(id) }
    }
  }
}

class CloudSyncedCommitmentStore(
  private val vault: LocalHouseholdVault,
  private val remote: PakkaCommitmentRepository,
  private val appScope: CoroutineScope
) : CommitmentStore {
  override val commitmentsState: StateFlow<List<PakkaCommitment>> = vault.commitments
  override val allCommitments: Flow<List<PakkaCommitment>> = vault.commitments

  override fun upsert(item: PakkaCommitment) {
    vault.upsertCommitmentFromLocal(item)
    appScope.launch {
      runCatching { remote.upsert(item) }
    }
  }

  override fun delete(id: String) {
    vault.deleteCommitmentFromLocal(id)
    appScope.launch {
      runCatching { remote.delete(id) }
    }
  }

  override fun resetToDefaults() {
    vault.replaceCommitmentsFromLocal(PakkaCommitment.defaultCommitments())
    appScope.launch {
      runCatching { remote.resetToDefaults() }
    }
  }
}

/**
 * Coordinates one-time Guest->Cloud migration and resilient background Firestore snapshot sync
 * for an authenticated user session.
 */
class CloudSyncCoordinator(
  private val uid: String,
  private val userVault: LocalHouseholdVault,
  private val guestVault: LocalHouseholdVault,
  private val preferencesRepository: PreferencesRepository,
  private val budgetRepository: BudgetRepository,
  private val transactionRepository: TransactionRepository,
  private val goalRepository: GoalRepository,
  private val quickChipRepository: QuickChipRepository,
  private val maaChatRepository: MaaChatRepository,
  private val papaChatRepository: PapaChatRepository,
  private val chitthiRepository: ChitthiRepository,
  private val pakkaCommitmentRepository: PakkaCommitmentRepository,
  private val appScope: CoroutineScope
) {
  private val _initialCloudCheckDone = kotlinx.coroutines.flow.MutableStateFlow(userVault.isOnboardingCompleted)
  val initialCloudCheckDone: StateFlow<Boolean> = _initialCloudCheckDone

  fun start() {
    appScope.launch {
      reconcileGuestAndInitialCloud()
      _initialCloudCheckDone.value = true
      launchResilientListeners()
    }
  }

  private suspend fun reconcileGuestAndInitialCloud() {
    try {
      val hasGuestChanges = guestVault.hasUnmigratedGuestDataFor(uid)
      val cloudHousehold = runCatching { budgetRepository.getHouseholdSync() }.getOrNull()

      if (hasGuestChanges) {
        val guestSnap = guestVault.snapshot.value
        val preferGuestCalibration = cloudHousehold == null ||
          !cloudHousehold.onboardingCompleted ||
          guestSnap.hasCustomCalibration

        if (preferGuestCalibration) {
          val resolvedName = guestSnap.household.userName.ifBlank {
            cloudHousehold?.userName.orEmpty()
          }.ifBlank { "Friend" }
          val onboarded = guestSnap.household.onboardingCompleted || (cloudHousehold?.onboardingCompleted == true)

          userVault.updateHouseholdFromLocal(isCalibration = true) {
            guestSnap.household.copy(
              userId = uid,
              userName = resolvedName,
              onboardingCompleted = onboarded
            )
          }
          guestSnap.goals.forEach { g -> userVault.upsertGoalByIdFromLocal(g, makeActive = g.id == guestSnap.activeGoalId) }
          if (guestSnap.quickChips.isNotEmpty()) {
            userVault.replaceQuickChipsFromLocal(guestSnap.quickChips)
          }
          if (guestSnap.transactions.isNotEmpty()) {
            userVault.insertTransactionsBatchFromLocal(
              guestSnap.transactions,
              markStarterSeeded = guestSnap.starterSeeded
            )
          }
          guestSnap.chitthis.forEach { userVault.upsertChitthiFromLocal(it) }
          if (guestSnap.commitmentsInitialized) {
            userVault.replaceCommitmentsFromLocal(guestSnap.commitments)
          }

          preferencesRepository.setUserName(resolvedName)
          preferencesRepository.setGuiltSensitivity(guestSnap.household.profile.guiltSensitivity)
          preferencesRepository.setPainDial(guestSnap.household.profile.painDial)
          preferencesRepository.setHabitLeaks(guestSnap.household.habitLeaks.toSet())
          if (onboarded) {
            preferencesRepository.setOnboardingCompleted(true)
          }

          // Push migrated state to Cloud Firestore asynchronously
          runCatching {
            budgetRepository.saveHouseholdCalibration(
              userName = resolvedName,
              onboardingCompleted = onboarded,
              profile = guestSnap.household.profile,
              habitLeaks = guestSnap.household.habitLeaks.toSet()
            )
            guestSnap.goals.forEach { g -> goalRepository.saveGoal(g) }
            guestSnap.quickChips.forEach { chip -> quickChipRepository.upsert(chip) }
            if (guestSnap.transactions.isNotEmpty()) {
              transactionRepository.insertAll(guestSnap.transactions)
            }
            guestSnap.chitthis.forEach { note -> chitthiRepository.upsert(note) }
            if (guestSnap.commitmentsInitialized) {
              guestSnap.commitments.forEach { item -> pakkaCommitmentRepository.upsert(item) }
            }
          }
        } else if (cloudHousehold != null) {
          // Cloud already had a full profile; keep cloud profile and merge any manual guest transactions & chitthis
          userVault.applyCloudHouseholdIfFresher(cloudHousehold, allowDuringGraceWindow = true)
          preferencesRepository.setUserName(cloudHousehold.userName)
          preferencesRepository.setGuiltSensitivity(cloudHousehold.profile.guiltSensitivity)
          preferencesRepository.setPainDial(cloudHousehold.profile.painDial)
          preferencesRepository.setHabitLeaks(cloudHousehold.habitLeaks.toSet())
          preferencesRepository.setOnboardingCompleted(true)

          val manualGuestTxs = guestSnap.transactions.filter { it.source != CaptureSource.SEED }
          if (manualGuestTxs.isNotEmpty()) {
            userVault.insertTransactionsBatchFromLocal(manualGuestTxs)
            runCatching { transactionRepository.insertAll(manualGuestTxs) }
          }
          if (guestSnap.chitthis.isNotEmpty()) {
            guestSnap.chitthis.forEach { note ->
              userVault.upsertChitthiFromLocal(note)
              runCatching { chitthiRepository.upsert(note) }
            }
          }
        }
        guestVault.markMigratedToCloud(uid)
      } else if (cloudHousehold != null && cloudHousehold.onboardingCompleted) {
        userVault.applyCloudHouseholdIfFresher(cloudHousehold, allowDuringGraceWindow = true)
        preferencesRepository.setUserName(cloudHousehold.userName)
        preferencesRepository.setGuiltSensitivity(cloudHousehold.profile.guiltSensitivity)
        preferencesRepository.setPainDial(cloudHousehold.profile.painDial)
        preferencesRepository.setHabitLeaks(cloudHousehold.habitLeaks.toSet())
        preferencesRepository.setOnboardingCompleted(true)

        runCatching {
          val cloudGoals = goalRepository.getAllGoalsSync()
          if (cloudGoals.isNotEmpty()) {
            userVault.applyCloudGoalsIfFresher(cloudGoals, allowDuringGraceWindow = true)
          }
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Initial cloud reconciliation skipped (offline or transient)", e)
    }
  }

  private fun launchResilientListeners() {
    appScope.launch {
      observeWithRetry("household") {
        budgetRepository.observeHousehold().collect { doc ->
          if (doc != null) {
            userVault.applyCloudHouseholdIfFresher(doc)
          }
        }
      }
    }

    appScope.launch {
      observeWithRetry("transactions") {
        transactionRepository.observeTransactions().collect { list ->
          userVault.applyCloudTransactionsIfFresher(list)
        }
      }
    }

    appScope.launch {
      observeWithRetry("goals") {
        goalRepository.observeAllGoals().collect { goals ->
          if (goals.isNotEmpty()) {
            userVault.applyCloudGoalsIfFresher(goals)
          }
        }
      }
    }

    appScope.launch {
      observeWithRetry("quick_chips") {
        quickChipRepository.observeQuickChips().collect { chips ->
          userVault.applyCloudQuickChipsIfFresher(chips)
        }
      }
    }

    appScope.launch {
      observeWithRetry("maa_chats") {
        maaChatRepository.observeMessages().collect { msgs ->
          userVault.applyCloudMaaChats(msgs)
        }
      }
    }

    appScope.launch {
      observeWithRetry("papa_chats") {
        papaChatRepository.observeMessages().collect { msgs ->
          userVault.applyCloudPapaChats(msgs)
        }
      }
    }

    appScope.launch {
      observeWithRetry("chitthis") {
        chitthiRepository.observeChitthis().collect { notes ->
          userVault.applyCloudChitthisIfFresher(notes)
        }
      }
    }

    appScope.launch {
      observeWithRetry("pakke_commitments") {
        pakkaCommitmentRepository.observeCommitments().collect { items ->
          userVault.applyCloudCommitmentsIfFresher(items)
        }
      }
    }
  }

  private suspend fun observeWithRetry(streamName: String, block: suspend () -> Unit) {
    var attempt = 0
    while (attempt < 5) {
      try {
        block()
        return
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        attempt++
        Log.w(TAG, "Stream $streamName disconnected (attempt $attempt), retrying gently", e)
        delay(2_000L * attempt)
      }
    }
  }
}
