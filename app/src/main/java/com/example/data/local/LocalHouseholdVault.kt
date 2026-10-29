package com.example.data.local

import android.content.Context
import com.example.core.model.BudgetProfile
import com.example.core.model.CaptureSource
import com.example.core.model.Category
import com.example.core.model.ChatSender
import com.example.core.model.ChitthiAuthor
import com.example.core.model.ChitthiNote
import com.example.core.model.CommitmentType
import com.example.core.model.Direction
import com.example.core.model.GuiltSensitivity
import com.example.core.model.HabitType
import com.example.core.model.InstrumentType
import com.example.core.model.MaaChatMessage
import com.example.core.model.PainDial
import com.example.core.model.PakkaCommitment
import com.example.core.model.PapaChatMessage
import com.example.core.model.QuickChip
import com.example.core.model.SavingsGoal
import com.example.core.model.StampColorKey
import com.example.core.model.Transaction
import com.example.core.model.UserHousehold
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

data class LocalVaultSnapshot(
  val household: UserHousehold = UserHousehold(),
  val goals: List<SavingsGoal> = listOf(SavingsGoal()),
  val activeGoalId: String = "primary_goal",
  val transactions: List<Transaction> = emptyList(),
  val quickChips: List<QuickChip> = QuickChip.defaultChips(),
  val maaChats: List<MaaChatMessage> = emptyList(),
  val papaChats: List<PapaChatMessage> = emptyList(),
  val chitthis: List<ChitthiNote> = emptyList(),
  val commitments: List<PakkaCommitment> = PakkaCommitment.defaultCommitments(),
  val commitmentsInitialized: Boolean = false,
  val starterSeeded: Boolean = false,
  val hasCustomCalibration: Boolean = false,
  val migratedToCloudUids: Set<String> = emptySet(),
  val lastLocalHouseholdEditMs: Long = 0L,
  val lastLocalGoalEditMs: Long = 0L,
  val lastLocalTxEditMs: Long = 0L,
  val lastLocalChipsEditMs: Long = 0L,
  val lastLocalChitthisEditMs: Long = 0L,
  val lastLocalCommitmentsEditMs: Long = 0L
) {
  val goal: SavingsGoal
    get() = goals.firstOrNull { it.id == activeGoalId }
      ?: goals.firstOrNull()
      ?: SavingsGoal()
}

/**
 * Local-first disk-backed state holder for both Guest Mode ("guest") and Signed-In Cloud Mode ("user_<uid>").
 * Hydrates synchronously on creation (<1ms for ~4KB JSON) so screens never flash back to default init values,
 * and persists atomically on an application-scoped IO SupervisorJob so navigation/dialog dismissal never cancels writes.
 */
class LocalHouseholdVault(
  context: Context,
  val vaultKey: String,
  defaultDisplayName: String? = null
) {
  private val appContext = context.applicationContext
  private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val writeMutex = Mutex()

  private val vaultDir: File = File(appContext.filesDir, "ghar_local_vault").apply { mkdirs() }
  private val safeFileKey: String = vaultKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(96)
  private val vaultFile: File = File(vaultDir, "vault_$safeFileKey.json")

  private val fallbackFirstName: String = defaultDisplayName
    ?.trim()
    ?.substringBefore(" ")
    ?.take(60)
    ?.takeIf { it.isNotBlank() }
    ?: ""

  private val _snapshot = MutableStateFlow(loadInitialSnapshot())
  val snapshot: StateFlow<LocalVaultSnapshot> = _snapshot.asStateFlow()

  private val _household = MutableStateFlow(_snapshot.value.household)
  val household: StateFlow<UserHousehold> = _household.asStateFlow()

  private val _goals = MutableStateFlow(_snapshot.value.goals)
  val goals: StateFlow<List<SavingsGoal>> = _goals.asStateFlow()

  private val _activeGoalId = MutableStateFlow(_snapshot.value.activeGoalId)
  val activeGoalId: StateFlow<String> = _activeGoalId.asStateFlow()

  private val _goal = MutableStateFlow(_snapshot.value.goal)
  val goal: StateFlow<SavingsGoal> = _goal.asStateFlow()

  private val _transactions = MutableStateFlow(_snapshot.value.transactions)
  val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

  private val _quickChips = MutableStateFlow(_snapshot.value.quickChips)
  val quickChips: StateFlow<List<QuickChip>> = _quickChips.asStateFlow()

  private val _maaChats = MutableStateFlow(_snapshot.value.maaChats)
  val maaChats: StateFlow<List<MaaChatMessage>> = _maaChats.asStateFlow()

  private val _papaChats = MutableStateFlow(_snapshot.value.papaChats)
  val papaChats: StateFlow<List<PapaChatMessage>> = _papaChats.asStateFlow()

  private val _chitthis = MutableStateFlow(_snapshot.value.chitthis)
  val chitthis: StateFlow<List<ChitthiNote>> = _chitthis.asStateFlow()

  private val _commitments = MutableStateFlow(_snapshot.value.commitments)
  val commitments: StateFlow<List<PakkaCommitment>> = _commitments.asStateFlow()

  val isOnboardingCompleted: Boolean
    get() = _household.value.onboardingCompleted

  val isStarterSeeded: Boolean
    get() = _snapshot.value.starterSeeded

  fun hasUnmigratedGuestDataFor(uid: String): Boolean {
    val s = _snapshot.value
    if (uid in s.migratedToCloudUids) return false
    return s.hasCustomCalibration || s.household.onboardingCompleted || s.transactions.isNotEmpty() || s.chitthis.isNotEmpty()
  }

  fun markMigratedToCloud(uid: String) {
    mutate { cur ->
      cur.copy(migratedToCloudUids = cur.migratedToCloudUids + uid)
    }
  }

  fun updateHouseholdFromLocal(
    isCalibration: Boolean = false,
    transform: (UserHousehold) -> UserHousehold
  ): UserHousehold {
    var updated = _household.value
    val now = System.currentTimeMillis()
    mutate { cur ->
      updated = transform(cur.household)
      cur.copy(
        household = updated,
        hasCustomCalibration = cur.hasCustomCalibration || isCalibration,
        lastLocalHouseholdEditMs = now
      )
    }
    return updated
  }

  fun applyCloudHouseholdIfFresher(cloud: UserHousehold, allowDuringGraceWindow: Boolean = false) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      if (!allowDuringGraceWindow && (now - cur.lastLocalHouseholdEditMs) < 4_000L && cur.lastLocalHouseholdEditMs > 0L) {
        return@mutate cur
      }
      val mergedOnboarding = cloud.onboardingCompleted || cur.household.onboardingCompleted
      cur.copy(household = cloud.copy(onboardingCompleted = mergedOnboarding))
    }
  }

  fun updateGoalFromLocal(transform: (SavingsGoal) -> SavingsGoal): SavingsGoal {
    var updated = _goal.value
    val now = System.currentTimeMillis()
    mutate { cur ->
      val currentActive = cur.goal
      updated = transform(currentActive)
      val cleanId = updated.id.ifBlank { currentActive.id.ifBlank { "primary_goal" } }
      updated = updated.copy(id = cleanId)
      val nextGoals = if (cur.goals.any { it.id == cleanId }) {
        cur.goals.map { if (it.id == cleanId) updated else it }
      } else {
        cur.goals + updated
      }
      cur.copy(
        goals = nextGoals,
        activeGoalId = cleanId,
        hasCustomCalibration = true,
        lastLocalGoalEditMs = now
      )
    }
    return updated
  }

  fun upsertGoalByIdFromLocal(goal: SavingsGoal, makeActive: Boolean = false): SavingsGoal {
    val cleanId = goal.id.ifBlank { "primary_goal" }
    val normalized = goal.copy(id = cleanId)
    val now = System.currentTimeMillis()
    mutate { cur ->
      val nextGoals = if (cur.goals.any { it.id == cleanId }) {
        cur.goals.map { if (it.id == cleanId) normalized else it }
      } else {
        cur.goals + normalized
      }
      cur.copy(
        goals = nextGoals,
        activeGoalId = if (makeActive) cleanId else cur.activeGoalId,
        hasCustomCalibration = true,
        lastLocalGoalEditMs = now
      )
    }
    return normalized
  }

  fun selectActiveGoalFromLocal(goalId: String) {
    if (goalId.isBlank()) return
    mutate { cur ->
      if (cur.goals.any { it.id == goalId }) {
        cur.copy(activeGoalId = goalId)
      } else {
        cur
      }
    }
  }

  fun deleteGoalFromLocal(goalId: String): Boolean {
    var deleted = false
    val now = System.currentTimeMillis()
    mutate { cur ->
      if (cur.goals.size <= 1 || cur.goals.none { it.id == goalId }) {
        return@mutate cur
      }
      deleted = true
      val remaining = cur.goals.filterNot { it.id == goalId }
      val nextActive = if (cur.activeGoalId == goalId) remaining.first().id else cur.activeGoalId
      cur.copy(
        goals = remaining,
        activeGoalId = nextActive,
        lastLocalGoalEditMs = now
      )
    }
    return deleted
  }

  fun applyCloudGoalIfFresher(cloudGoal: SavingsGoal, allowDuringGraceWindow: Boolean = false) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      if (!allowDuringGraceWindow && (now - cur.lastLocalGoalEditMs) < 4_000L && cur.lastLocalGoalEditMs > 0L) {
        return@mutate cur
      }
      val cid = cloudGoal.id.ifBlank { "primary_goal" }
      val normalized = cloudGoal.copy(id = cid)
      val nextGoals = if (cur.goals.any { it.id == cid }) {
        cur.goals.map { if (it.id == cid) normalized else it }
      } else {
        cur.goals + normalized
      }
      cur.copy(goals = nextGoals)
    }
  }

  fun applyCloudGoalsIfFresher(cloudGoals: List<SavingsGoal>, allowDuringGraceWindow: Boolean = false) {
    if (cloudGoals.isEmpty()) return
    val now = System.currentTimeMillis()
    mutate { cur ->
      if (!allowDuringGraceWindow && (now - cur.lastLocalGoalEditMs) < 4_000L && cur.lastLocalGoalEditMs > 0L) {
        return@mutate cur
      }
      val ordered = cloudGoals.sortedWith(
        compareBy<SavingsGoal> { if (it.id == "primary_goal") 0 else 1 }.thenBy { it.name }
      )
      val nextActive = if (ordered.any { it.id == cur.activeGoalId }) cur.activeGoalId else ordered.first().id
      cur.copy(
        goals = ordered,
        activeGoalId = nextActive
      )
    }
  }

  fun upsertTransactionFromLocal(tx: Transaction) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      val filtered = cur.transactions.filterNot { it.id == tx.id }
      val next = (listOf(tx) + filtered).sortedByDescending { it.timestamp }
      cur.copy(
        transactions = next,
        hasCustomCalibration = true,
        lastLocalTxEditMs = now
      )
    }
  }

  fun insertTransactionsBatchFromLocal(txs: List<Transaction>, markStarterSeeded: Boolean = false) {
    if (txs.isEmpty() && !markStarterSeeded) return
    val now = System.currentTimeMillis()
    mutate { cur ->
      val incomingIds = txs.map { it.id }.toSet()
      val merged = (txs + cur.transactions.filterNot { it.id in incomingIds })
        .sortedByDescending { it.timestamp }
      cur.copy(
        transactions = merged,
        starterSeeded = cur.starterSeeded || markStarterSeeded,
        lastLocalTxEditMs = now
      )
    }
  }

  fun deleteTransactionFromLocal(id: String) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      cur.copy(
        transactions = cur.transactions.filterNot { it.id == id },
        lastLocalTxEditMs = now
      )
    }
  }

  fun clearTransactionsFromLocal() {
    val now = System.currentTimeMillis()
    mutate { cur ->
      cur.copy(
        transactions = emptyList(),
        lastLocalTxEditMs = now
      )
    }
  }

  fun applyCloudTransactionsIfFresher(cloudTxs: List<Transaction>, allowDuringGraceWindow: Boolean = false) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      if (!allowDuringGraceWindow && (now - cur.lastLocalTxEditMs) < 3_500L && cur.lastLocalTxEditMs > 0L) {
        return@mutate cur
      }
      cur.copy(
        transactions = cloudTxs.sortedByDescending { it.timestamp },
        starterSeeded = cur.starterSeeded || cloudTxs.isNotEmpty()
      )
    }
  }

  fun upsertQuickChipFromLocal(chip: QuickChip) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      val next = (cur.quickChips.filterNot { it.id == chip.id } + chip)
        .sortedWith(compareBy<QuickChip> { it.sortOrder }.thenBy { it.label })
      cur.copy(quickChips = next, lastLocalChipsEditMs = now)
    }
  }

  fun replaceQuickChipsFromLocal(chips: List<QuickChip>) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      val sorted = chips.mapIndexed { idx, chip -> chip.copy(sortOrder = idx) }
      cur.copy(quickChips = sorted, lastLocalChipsEditMs = now)
    }
  }

  fun deleteQuickChipFromLocal(chipId: String) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      cur.copy(
        quickChips = cur.quickChips.filterNot { it.id == chipId },
        lastLocalChipsEditMs = now
      )
    }
  }

  fun applyCloudQuickChipsIfFresher(cloudChips: List<QuickChip>) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      if ((now - cur.lastLocalChipsEditMs) < 3_500L && cur.lastLocalChipsEditMs > 0L) {
        return@mutate cur
      }
      if (cloudChips.isEmpty() && cur.quickChips.isNotEmpty()) {
        return@mutate cur
      }
      cur.copy(
        quickChips = cloudChips.sortedWith(compareBy<QuickChip> { it.sortOrder }.thenBy { it.label })
      )
    }
  }

  fun appendMaaChatFromLocal(message: MaaChatMessage) {
    mutate { cur ->
      val next = (cur.maaChats + message).sortedBy { it.timestamp }
      cur.copy(maaChats = next)
    }
  }

  fun clearMaaChatFromLocal() {
    mutate { cur -> cur.copy(maaChats = emptyList()) }
  }

  fun applyCloudMaaChats(cloudMessages: List<MaaChatMessage>) {
    mutate { cur ->
      if (cloudMessages.isEmpty() && cur.maaChats.isNotEmpty()) {
        return@mutate cur
      }
      cur.copy(maaChats = cloudMessages.sortedBy { it.timestamp })
    }
  }

  fun appendPapaChatFromLocal(message: PapaChatMessage) {
    mutate { cur ->
      val next = (cur.papaChats + message).sortedBy { it.timestamp }
      cur.copy(papaChats = next)
    }
  }

  fun clearPapaChatFromLocal() {
    mutate { cur -> cur.copy(papaChats = emptyList()) }
  }

  fun applyCloudPapaChats(cloudMessages: List<PapaChatMessage>) {
    mutate { cur ->
      if (cloudMessages.isEmpty() && cur.papaChats.isNotEmpty()) {
        return@mutate cur
      }
      cur.copy(papaChats = cloudMessages.sortedBy { it.timestamp })
    }
  }

  fun upsertChitthiFromLocal(note: ChitthiNote) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      val next = (listOf(note) + cur.chitthis.filterNot { it.id == note.id })
        .sortedByDescending { it.savedAt }
      cur.copy(chitthis = next, lastLocalChitthisEditMs = now)
    }
  }

  fun deleteChitthiFromLocal(id: String) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      cur.copy(
        chitthis = cur.chitthis.filterNot { it.id == id },
        lastLocalChitthisEditMs = now
      )
    }
  }

  fun applyCloudChitthisIfFresher(cloudChitthis: List<ChitthiNote>) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      if ((now - cur.lastLocalChitthisEditMs) < 3_500L && cur.lastLocalChitthisEditMs > 0L) {
        return@mutate cur
      }
      cur.copy(chitthis = cloudChitthis.sortedByDescending { it.savedAt })
    }
  }

  fun upsertCommitmentFromLocal(item: PakkaCommitment) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      val normalized = item.copy(
        dueDayOfMonth = item.dueDayOfMonth.coerceIn(1, 31),
        updatedAtMs = now
      )
      val next = (cur.commitments.filterNot { it.id == normalized.id } + normalized)
        .sortedWith(compareBy<PakkaCommitment>({ !it.isActive }, { it.safeDueDay }, { it.title }))
      cur.copy(
        commitments = next,
        commitmentsInitialized = true,
        hasCustomCalibration = true,
        lastLocalCommitmentsEditMs = now
      )
    }
  }

  fun replaceCommitmentsFromLocal(items: List<PakkaCommitment>) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      val sorted = items.sortedWith(compareBy<PakkaCommitment>({ !it.isActive }, { it.safeDueDay }, { it.title }))
      cur.copy(
        commitments = sorted,
        commitmentsInitialized = true,
        lastLocalCommitmentsEditMs = now
      )
    }
  }

  fun deleteCommitmentFromLocal(commitmentId: String) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      cur.copy(
        commitments = cur.commitments.filterNot { it.id == commitmentId },
        commitmentsInitialized = true,
        lastLocalCommitmentsEditMs = now
      )
    }
  }

  fun applyCloudCommitmentsIfFresher(cloudItems: List<PakkaCommitment>) {
    val now = System.currentTimeMillis()
    mutate { cur ->
      if ((now - cur.lastLocalCommitmentsEditMs) < 3_500L && cur.lastLocalCommitmentsEditMs > 0L) {
        return@mutate cur
      }
      if (cloudItems.isEmpty() && !cur.commitmentsInitialized) {
        return@mutate cur
      }
      cur.copy(
        commitments = cloudItems.sortedWith(compareBy<PakkaCommitment>({ !it.isActive }, { it.safeDueDay }, { it.title })),
        commitmentsInitialized = cur.commitmentsInitialized || cloudItems.isNotEmpty()
      )
    }
  }

  fun markStarterSeeded() {
    mutate { cur -> cur.copy(starterSeeded = true) }
  }

  fun explicitResetOnboarding() {
    val now = System.currentTimeMillis()
    mutate { cur ->
      cur.copy(
        household = cur.household.copy(onboardingCompleted = false),
        lastLocalHouseholdEditMs = now
      )
    }
  }

  private fun mutate(block: (LocalVaultSnapshot) -> LocalVaultSnapshot) {
    synchronized(this) {
      val computed = block(_snapshot.value)
      _snapshot.value = computed
      _household.value = computed.household
      _goals.value = computed.goals
      _activeGoalId.value = computed.activeGoalId
      _goal.value = computed.goal
      _transactions.value = computed.transactions
      _quickChips.value = computed.quickChips
      _maaChats.value = computed.maaChats
      _papaChats.value = computed.papaChats
      _chitthis.value = computed.chitthis
      _commitments.value = computed.commitments
    }
    persistLatestSnapshotAsync()
  }

  private fun persistLatestSnapshotAsync() {
    ioScope.launch {
      writeMutex.withLock {
        runCatching {
          val latestSnap = synchronized(this@LocalHouseholdVault) { _snapshot.value }
          val json = serializeSnapshot(latestSnap).toString()
          val tmp = File(vaultDir, "vault_${safeFileKey}.tmp")
          tmp.writeText(json, Charsets.UTF_8)
          if (!tmp.renameTo(vaultFile)) {
            vaultFile.writeText(json, Charsets.UTF_8)
            tmp.delete()
          }
        }
      }
    }
  }

  private fun loadInitialSnapshot(): LocalVaultSnapshot {
    if (!vaultFile.exists()) {
      return LocalVaultSnapshot(
        household = UserHousehold(
          userId = vaultKey,
          userName = fallbackFirstName,
          onboardingCompleted = false
        ),
        quickChips = QuickChip.defaultChips()
      )
    }
    return try {
      val root = JSONObject(vaultFile.readText(Charsets.UTF_8))
      deserializeSnapshot(root)
    } catch (_: Throwable) {
      LocalVaultSnapshot(
        household = UserHousehold(
          userId = vaultKey,
          userName = fallbackFirstName,
          onboardingCompleted = false
        ),
        quickChips = QuickChip.defaultChips()
      )
    }
  }

  private fun serializeGoalObj(g: SavingsGoal): JSONObject = JSONObject().apply {
    put("id", g.id)
    put("name", g.name)
    put("targetAmountPaise", g.targetAmountPaise)
    put("currentAmountPaise", g.currentAmountPaise)
    if (g.targetDateEpochMillis != null) put("targetDateEpochMillis", g.targetDateEpochMillis)
    put("instrumentType", g.instrumentType.name)
  }

  private fun serializeSnapshot(s: LocalVaultSnapshot): JSONObject {
    val root = JSONObject()
    root.put("gullakHonestV2", true)
    root.put("starterSeeded", s.starterSeeded)
    root.put("hasCustomCalibration", s.hasCustomCalibration)
    root.put("activeGoalId", s.activeGoalId)
    root.put("migratedToCloudUids", JSONArray(s.migratedToCloudUids.toList()))

    val hh = s.household
    val hhObj = JSONObject().apply {
      put("userId", hh.userId)
      put("userName", hh.userName)
      put("onboardingCompleted", hh.onboardingCompleted)
      put("monthlyIncomePaise", hh.profile.monthlyIncomePaise)
      put("fixedCommitmentsPaise", hh.profile.fixedCommitmentsPaise)
      put("savingsTargetPaise", hh.profile.savingsTargetPaise)
      put("monthStartDay", hh.profile.monthStartDay)
      put("guiltSensitivity", hh.profile.guiltSensitivity.name)
      put("habitType", hh.profile.habitType.name)
      put("painDial", hh.profile.painDial.name)
      put("ageBand", hh.profile.ageBand)
      put("lifeStage", hh.profile.lifeStage)
      put("habitLeaks", JSONArray(hh.habitLeaks))
    }
    root.put("household", hhObj)

    root.put("goal", serializeGoalObj(s.goal))
    val goalsArr = JSONArray()
    s.goals.forEach { g -> goalsArr.put(serializeGoalObj(g)) }
    root.put("goals", goalsArr)

    val txArr = JSONArray()
    s.transactions.take(300).forEach { tx ->
      txArr.put(
        JSONObject().apply {
          put("id", tx.id)
          put("amountPaise", tx.amountPaise)
          put("direction", tx.direction.name)
          put("merchant", tx.merchant)
          put("category", tx.category.name)
          put("timestamp", tx.timestamp)
          put("source", tx.source.name)
          if (tx.rawText != null) put("rawText", tx.rawText)
          put("isRecurring", tx.isRecurring)
        }
      )
    }
    root.put("transactions", txArr)

    val chipArr = JSONArray()
    s.quickChips.forEach { c ->
      chipArr.put(
        JSONObject().apply {
          put("id", c.id)
          put("label", c.label)
          put("merchant", c.merchant)
          put("amountPaise", c.amountPaise)
          put("category", c.category.name)
          put("direction", c.direction.name)
          put("colorKey", c.colorKey.name)
          put("sortOrder", c.sortOrder)
        }
      )
    }
    root.put("quickChips", chipArr)

    val chatArr = JSONArray()
    s.maaChats.takeLast(100).forEach { m ->
      chatArr.put(
        JSONObject().apply {
          put("id", m.id)
          put("sender", m.sender.name)
          put("text", m.text)
          put("timestamp", m.timestamp)
        }
      )
    }
    root.put("maaChats", chatArr)

    val papaChatArr = JSONArray()
    s.papaChats.takeLast(100).forEach { m ->
      papaChatArr.put(
        JSONObject().apply {
          put("id", m.id)
          put("sender", m.sender.name)
          put("text", m.text)
          put("timestamp", m.timestamp)
          val sourcesJson = m.sourcesToJsonString()
          if (sourcesJson.isNotEmpty()) {
            put("sourcesJson", sourcesJson)
          }
        }
      )
    }
    root.put("papaChats", papaChatArr)

    val chitthiArr = JSONArray()
    s.chitthis.take(150).forEach { c ->
      chitthiArr.put(
        JSONObject().apply {
          put("id", c.id)
          put("author", c.author.name)
          put("title", c.title)
          put("body", c.body)
          put("tag", c.tag)
          put("savedAt", c.savedAt)
        }
      )
    }
    root.put("chitthis", chitthiArr)
    root.put("commitmentsInitialized", s.commitmentsInitialized)
    val commitmentArr = JSONArray()
    s.commitments.take(150).forEach { p ->
      commitmentArr.put(
        JSONObject().apply {
          put("id", p.id)
          put("title", p.title)
          put("amountPaise", p.amountPaise)
          put("type", p.type.name)
          put("category", p.category.name)
          put("dueDayOfMonth", p.safeDueDay)
          put("notes", p.notes)
          put("isActive", p.isActive)
          put("updatedAtMs", p.updatedAtMs)
        }
      )
    }
    root.put("commitments", commitmentArr)
    return root
  }

  private fun parseGoalObj(goalObj: JSONObject): SavingsGoal {
    return SavingsGoal(
      id = goalObj.optString("id", "primary_goal").ifBlank { "primary_goal" },
      name = goalObj.optString("name", "Himachal Trip"),
      targetAmountPaise = goalObj.optLong("targetAmountPaise", 40_000_00L),
      currentAmountPaise = goalObj.optLong("currentAmountPaise", 0L),
      targetDateEpochMillis = if (goalObj.has("targetDateEpochMillis")) goalObj.optLong("targetDateEpochMillis") else null,
      instrumentType = InstrumentType.fromName(goalObj.optString("instrumentType"))
    )
  }

  private fun deserializeSnapshot(root: JSONObject): LocalVaultSnapshot {
    val isHonestV2 = root.optBoolean("gullakHonestV2", false)

    val hhObj = root.optJSONObject("household")
    val leaksList = mutableListOf<String>()
    hhObj?.optJSONArray("habitLeaks")?.let { arr ->
      for (i in 0 until arr.length()) {
        val v = arr.optString(i)
        if (v.isNotBlank()) leaksList.add(v)
      }
    }
    val household = if (hhObj != null) {
      UserHousehold(
        userId = hhObj.optString("userId", vaultKey),
        userName = hhObj.optString("userName", fallbackFirstName),
        onboardingCompleted = hhObj.optBoolean("onboardingCompleted", false),
        profile = BudgetProfile(
          monthlyIncomePaise = hhObj.optLong("monthlyIncomePaise", 60_000_00L),
          fixedCommitmentsPaise = hhObj.optLong("fixedCommitmentsPaise", 24_000_00L),
          savingsTargetPaise = hhObj.optLong("savingsTargetPaise", 12_000_00L),
          monthStartDay = hhObj.optInt("monthStartDay", 1),
          guiltSensitivity = GuiltSensitivity.fromName(hhObj.optString("guiltSensitivity")),
          habitType = HabitType.fromName(hhObj.optString("habitType")),
          painDial = PainDial.fromName(hhObj.optString("painDial")),
          ageBand = hhObj.optString("ageBand", "25-34"),
          lifeStage = hhObj.optString("lifeStage", "Salaried")
        ),
        habitLeaks = leaksList.ifEmpty { listOf("CHAI_SNACKS") }
      )
    } else {
      UserHousehold(userId = vaultKey, userName = fallbackFirstName)
    }

    val txs = mutableListOf<Transaction>()
    root.optJSONArray("transactions")?.let { arr ->
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        txs.add(
          Transaction(
            id = o.optString("id"),
            amountPaise = o.optLong("amountPaise", 100L),
            direction = runCatching { Direction.valueOf(o.optString("direction", "DEBIT")) }.getOrDefault(Direction.DEBIT),
            merchant = o.optString("merchant", "Household Note"),
            category = Category.fromName(o.optString("category")),
            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
            source = runCatching { CaptureSource.valueOf(o.optString("source", "MANUAL")) }.getOrDefault(CaptureSource.MANUAL),
            rawText = if (o.has("rawText")) o.optString("rawText") else null,
            isRecurring = o.optBoolean("isRecurring", false)
          )
        )
      }
    }

    val parsedGoals = mutableListOf<SavingsGoal>()
    root.optJSONArray("goals")?.let { arr ->
      for (i in 0 until arr.length()) {
        val go = arr.optJSONObject(i) ?: continue
        parsedGoals.add(parseGoalObj(go))
      }
    }
    if (parsedGoals.isEmpty()) {
      val singleGoalObj = root.optJSONObject("goal")
      if (singleGoalObj != null) {
        parsedGoals.add(parseGoalObj(singleGoalObj))
      } else {
        parsedGoals.add(SavingsGoal())
      }
    }

    // One-time cleanup for legacy vaults created before v2 that carried unrequested phantom 35% or ₹16,500 balances
    val hasRealGullakTxs = txs.any { it.isGullakTransfer() }
    val sanitizedGoals = if (!isHonestV2 && !hasRealGullakTxs) {
      parsedGoals.map { g ->
        val wasLegacyPhantom = g.currentAmountPaise == 16_500_00L ||
          (g.targetAmountPaise > 0L && g.currentAmountPaise == ((g.targetAmountPaise / 100L) * 35L / 100L) * 100L)
        if (wasLegacyPhantom) g.copy(currentAmountPaise = 0L) else g
      }
    } else {
      parsedGoals
    }

    val savedActiveGoalId = root.optString("activeGoalId", "primary_goal").ifBlank { "primary_goal" }
    val resolvedActiveId = if (sanitizedGoals.any { it.id == savedActiveGoalId }) {
      savedActiveGoalId
    } else {
      sanitizedGoals.first().id
    }

    val chips = mutableListOf<QuickChip>()
    root.optJSONArray("quickChips")?.let { arr ->
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        chips.add(
          QuickChip(
            id = o.optString("id"),
            label = o.optString("label", "Stamp"),
            merchant = o.optString("merchant", "Household Note"),
            amountPaise = o.optLong("amountPaise", 25_00L),
            category = Category.fromName(o.optString("category")),
            direction = runCatching { Direction.valueOf(o.optString("direction", "DEBIT")) }.getOrDefault(Direction.DEBIT),
            colorKey = StampColorKey.fromName(o.optString("colorKey")),
            sortOrder = o.optInt("sortOrder", i)
          )
        )
      }
    }

    val chats = mutableListOf<MaaChatMessage>()
    root.optJSONArray("maaChats")?.let { arr ->
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        chats.add(
          MaaChatMessage(
            id = o.optString("id"),
            sender = runCatching { ChatSender.valueOf(o.optString("sender", "MAA")) }.getOrDefault(ChatSender.MAA),
            text = o.optString("text"),
            timestamp = o.optLong("timestamp", System.currentTimeMillis())
          )
        )
      }
    }

    val papaChats = mutableListOf<PapaChatMessage>()
    root.optJSONArray("papaChats")?.let { arr ->
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        papaChats.add(
          PapaChatMessage(
            id = o.optString("id"),
            sender = runCatching { ChatSender.valueOf(o.optString("sender", "PAPA")) }.getOrDefault(ChatSender.PAPA),
            text = o.optString("text"),
            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
            sources = PapaChatMessage.parseSourcesJson(o.optString("sourcesJson", ""))
          )
        )
      }
    }

    val migratedUids = mutableSetOf<String>()
    root.optJSONArray("migratedToCloudUids")?.let { arr ->
      for (i in 0 until arr.length()) {
        val v = arr.optString(i)
        if (v.isNotBlank()) migratedUids.add(v)
      }
    }

    val chitthis = mutableListOf<ChitthiNote>()
    root.optJSONArray("chitthis")?.let { arr ->
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val body = o.optString("body")
        if (body.isBlank()) continue
        chitthis.add(
          ChitthiNote(
            id = o.optString("id"),
            author = ChitthiAuthor.fromName(o.optString("author")),
            title = o.optString("title", "Maa's Chitthi"),
            body = body,
            tag = o.optString("tag", "Rasoi Wisdom"),
            savedAt = o.optLong("savedAt", System.currentTimeMillis())
          )
        )
      }
    }

    val commitmentsInitialized = root.optBoolean("commitmentsInitialized", false)
    val parsedCommitments = mutableListOf<PakkaCommitment>()
    root.optJSONArray("commitments")?.let { arr ->
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val title = o.optString("title").trim()
        if (title.isBlank()) continue
        val cType = CommitmentType.fromName(o.optString("type"))
        parsedCommitments.add(
          PakkaCommitment(
            id = o.optString("id").ifBlank { "pakka_$i" },
            title = title,
            amountPaise = o.optLong("amountPaise", 500_00L).coerceAtLeast(100L),
            type = cType,
            category = Category.fromName(o.optString("category", cType.defaultCategory.name)),
            dueDayOfMonth = o.optInt("dueDayOfMonth", 5).coerceIn(1, 31),
            notes = o.optString("notes", ""),
            isActive = o.optBoolean("isActive", true),
            updatedAtMs = o.optLong("updatedAtMs", System.currentTimeMillis())
          )
        )
      }
    }

    val resolvedCommitments = if (!commitmentsInitialized && parsedCommitments.isEmpty()) {
      val defaults = PakkaCommitment.defaultCommitments().toMutableList()
      val existingTitles = defaults.map { it.title.lowercase() }.toMutableSet()
      txs.filter { it.isRecurring }.groupBy { it.merchant.trim().lowercase() }.forEach { (normKey, group) ->
        if (normKey.isNotBlank() && normKey !in existingTitles) {
          val latest = group.maxByOrNull { it.timestamp }!!
          val inferredType = when {
            !latest.isExpense -> CommitmentType.RECEIVABLE
            latest.category == Category.ENTERTAINMENT -> CommitmentType.SUBSCRIPTION
            else -> CommitmentType.BILL
          }
          defaults.add(
            PakkaCommitment(
              id = "pakka_migrated_${normKey.replace(Regex("[^a-z0-9]"), "_").take(24)}",
              title = latest.merchant,
              amountPaise = latest.amountPaise,
              type = inferredType,
              category = latest.category,
              dueDayOfMonth = 5
            )
          )
          existingTitles.add(normKey)
        }
      }
      defaults
    } else {
      parsedCommitments.sortedWith(compareBy<PakkaCommitment>({ !it.isActive }, { it.safeDueDay }, { it.title }))
    }

    return LocalVaultSnapshot(
      household = household,
      goals = sanitizedGoals,
      activeGoalId = resolvedActiveId,
      transactions = txs.sortedByDescending { it.timestamp },
      quickChips = chips.ifEmpty { QuickChip.defaultChips() },
      maaChats = chats.sortedBy { it.timestamp },
      papaChats = papaChats.sortedBy { it.timestamp },
      chitthis = chitthis.sortedByDescending { it.savedAt },
      commitments = resolvedCommitments,
      commitmentsInitialized = commitmentsInitialized,
      starterSeeded = root.optBoolean("starterSeeded", false),
      hasCustomCalibration = root.optBoolean("hasCustomCalibration", false),
      migratedToCloudUids = migratedUids
    )
  }
}
