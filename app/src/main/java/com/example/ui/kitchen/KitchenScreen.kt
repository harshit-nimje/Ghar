package com.example.ui.kitchen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppContainer
import com.example.capture.CaptureEventBus
import com.example.capture.HouseholdNotificationHelper
import com.example.core.audio.GharSoundEffects
import com.example.data.preferences.ActiveHabitSankalp
import com.example.data.preferences.DailyReminderRoutine
import com.example.data.preferences.RoutineKind
import com.example.core.engine.ChitthiWisdomBank
import com.example.core.engine.WisdomChitthiSeed
import com.example.core.engine.CooldownState
import com.example.core.engine.HouseholdAnalyticsEngine
import com.example.core.engine.InsightEngine
import com.example.core.engine.SafeToSpendCalculator
import com.example.core.engine.SafeToSpendResult
import com.example.core.engine.SmartKhataParser
import com.example.core.localization.GharTerms
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.model.BudgetProfile
import com.example.core.model.CaptureSource
import com.example.core.model.Category
import com.example.core.model.ChitthiAuthor
import com.example.core.model.ChitthiNote
import com.example.core.model.Direction
import com.example.core.model.Insight
import com.example.core.model.PakkaCommitment
import com.example.core.model.QuickChip
import com.example.core.model.Transaction
import com.example.core.util.CurrencyFormatter
import com.example.core.util.DateUtils
import com.example.ui.bhairoom.PapaGuidanceDialog
import com.example.ui.common.CategoryIcon
import com.example.ui.common.ChitthiDabbaDialog
import com.example.ui.common.ChitthiDetailDialog
import com.example.ui.common.GharConfirmDialog
import com.example.ui.common.GharUndoSnackbarHost
import com.example.ui.common.HandwrittenHomeNote
import com.example.ui.common.HisaabCalculatorDialog
import com.example.ui.common.InteractiveChitthiStickyCard
import com.example.ui.common.JharokhaCardShape
import com.example.ui.common.KitchenGreetingHero
import com.example.ui.common.LocalGharUndoController
import com.example.ui.common.MaaAvatar
import com.example.ui.common.PakkeCommitmentsDialog
import com.example.ui.common.PebbleCardShape
import com.example.ui.common.PhysicalCashAnimationOverlay
import com.example.ui.common.RangoliBrassOrnament
import com.example.ui.common.SafeGrowthGauge
import com.example.ui.common.SmartAlertsAndSmsDialog
import com.example.ui.common.StickyNotePaperColors
import com.example.ui.common.jharokhaCornerFiligree
import com.example.ui.common.pastelHomeBackdrop
import com.example.ui.common.rememberGharUndoController
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelPlum
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelSky
import com.example.ui.theme.PastelTerracotta
import kotlin.random.Random
import kotlinx.coroutines.launch

@Immutable
private data class CashAnimRequest(
  val amountPaise: Long,
  val isIncoming: Boolean
)

private enum class KhataFilter {
  ALL,
  TODAY,
  SPENDS,
  INCOME,
  GULLAK;

  fun displayLabel(plainEnglish: Boolean): String = when (this) {
    ALL -> "All"
    TODAY -> "Today"
    SPENDS -> if (plainEnglish) "Expenses" else "Spends"
    INCOME -> if (plainEnglish) "Income" else "Came Home"
    GULLAK -> if (plainEnglish) "Savings" else "Gullak"
  }
}

@Composable
fun KitchenScreen(
  container: AppContainer,
  listState: LazyListState = rememberLazyListState(),
  onShowRoomGuide: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val plainEnglish = LocalPlainEnglishMode.current
  val hasParentUndo = LocalGharUndoController.current != null
  val fallbackUndo = rememberGharUndoController()
  val undoController = LocalGharUndoController.current ?: fallbackUndo
  val focusManager = LocalFocusManager.current
  val scope = rememberCoroutineScope()

  val transactions by container.transactionRepository.transactionsState.collectAsStateWithLifecycle()
  val profile by container.budgetRepository.profileState.collectAsStateWithLifecycle()
  val goal by container.goalRepository.primaryGoalState.collectAsStateWithLifecycle()
  val allGoals by container.goalRepository.allGoalsState.collectAsStateWithLifecycle()
  val commitments by container.commitmentRepository.commitmentsState.collectAsStateWithLifecycle()
  val stashedChitthis by container.chitthiRepository.chitthisState.collectAsStateWithLifecycle()
  val storedQuickChips by container.quickChipRepository.quickChipsState.collectAsStateWithLifecycle()
  val prefs by container.preferencesRepository.prefsSnapshot.collectAsStateWithLifecycle()
  val quickChipsSeededFlow = remember(container.userId) {
    container.preferencesRepository.observeQuickChipsSeeded(container.userId)
  }
  val quickChipsSeeded by quickChipsSeededFlow.collectAsStateWithLifecycle(initialValue = false)

  val commitmentsSummary = remember(commitments, transactions, profile.monthStartDay) {
    HouseholdAnalyticsEngine.evaluateCommitments(
      commitments = commitments,
      transactions = transactions,
      monthStartDay = profile.monthStartDay
    )
  }

  val activeQuickChips = remember(storedQuickChips, quickChipsSeeded, plainEnglish) {
    if (storedQuickChips.isEmpty() && !quickChipsSeeded) {
      QuickChip.defaultChips(plainEnglish)
    } else {
      storedQuickChips
    }
  }
  val painDial = profile.painDial
  val hapticsEnabled = prefs.isHapticsEnabled
  val cloudUserName by container.budgetRepository.userNameState.collectAsStateWithLifecycle()
  val displayUserName = remember(cloudUserName, prefs.userName) {
    val cleanCloud = cloudUserName.trim()
    val cleanPref = prefs.userName.trim()
    when {
      cleanCloud.isNotBlank() && cleanCloud != "Friend" -> cleanCloud
      cleanPref.isNotBlank() -> cleanPref
      cleanCloud.isNotBlank() -> cleanCloud
      else -> "Friend"
    }
  }

  var showQuickAddDialog by remember { mutableStateOf(false) }
  var prefilledQuickAddRupees by remember { mutableStateOf<Long?>(null) }
  var showCalculatorDialog by remember { mutableStateOf(false) }
  var showChitthiDabbaDialog by remember { mutableStateOf(false) }
  var showChitthiDetailDialog by remember { mutableStateOf<WisdomChitthiSeed?>(null) }
  var showSmartAlertsDialog by remember { mutableStateOf(false) }
  var showCommitmentsDialog by remember { mutableStateOf(false) }
  var showClearSamplesConfirm by remember { mutableStateOf(false) }
  var showMaaGuidanceDialog by remember { mutableStateOf(false) }
  var showPapaGuidanceDialog by remember { mutableStateOf(false) }
  var pendingCrossChatPrompt by remember { mutableStateOf<String?>(null) }
  var showCustomizeChipsDialog by remember { mutableStateOf(false) }
  var startCustomizeInCreateMode by remember { mutableStateOf(false) }
  var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

  // Ledger Search/Filter State
  var searchQuery by remember { mutableStateOf("") }
  var showSearchBar by remember { mutableStateOf(false) }
  var selectedFilter by remember { mutableStateOf(KhataFilter.ALL) }
  var visibleLimit by remember { mutableIntStateOf(25) }

  var maaSeedIndex by remember {
    mutableIntStateOf(Random.nextInt(ChitthiWisdomBank.maaSeeds.size))
  }
  val activeMaaSeed = remember(maaSeedIndex, plainEnglish) {
    ChitthiWisdomBank.pickMaaSeed(maaSeedIndex, plainEnglish)
  }
  val isMaaSeedStashed = remember(stashedChitthis, activeMaaSeed.id) {
    stashedChitthis.any { it.id == activeMaaSeed.id }
  }

  var cashAnimation by remember { mutableStateOf<CashAnimRequest?>(null) }
  var dismissedInsightId by remember { mutableStateOf<String?>(null) }

  val spentSoFarPaise = remember(transactions) {
    transactions.filter { it.isExpense }.sumOf { it.amountPaise }
  }
  val creditIncomePaise = remember(transactions) {
    transactions.filter { !it.isExpense }.sumOf { it.amountPaise }
  }

  val safeToSpendResult = remember(profile, spentSoFarPaise, creditIncomePaise) {
    SafeToSpendCalculator.calculate(profile, spentSoFarPaise, creditIncomePaise)
  }

  val spentTodayRupees = remember(transactions) {
    HouseholdAnalyticsEngine.calculateTodaySpendRupees(transactions)
  }

  val quietStreakDays = remember(transactions, safeToSpendResult.dailyAllowanceRupees) {
    HouseholdAnalyticsEngine.calculateQuietStreakDays(
      transactions = transactions,
      dailySafeRupees = safeToSpendResult.dailyAllowanceRupees
    )
  }

  val seedTransactions = remember(transactions) {
    transactions.filter { it.source == CaptureSource.SEED }
  }

  val filteredTransactions = remember(transactions, selectedFilter, searchQuery, plainEnglish) {
    val startOfToday = DateUtils.startOfTodayMillis()
    val query = searchQuery.trim().lowercase()
    transactions.filter { tx ->
      val matchesFilter = when (selectedFilter) {
        KhataFilter.ALL -> true
        KhataFilter.TODAY -> tx.timestamp >= startOfToday
        KhataFilter.SPENDS -> tx.isExpense && !tx.isGullakTransfer()
        KhataFilter.INCOME -> !tx.isExpense
        KhataFilter.GULLAK ->
          tx.isGullakTransfer() ||
            tx.merchant.contains("Gullak", ignoreCase = true) ||
            tx.merchant.contains("Savings Pot", ignoreCase = true)
      }
      val matchesSearch = if (query.isEmpty()) {
        true
      } else {
        tx.merchant.lowercase().contains(query) ||
          tx.displayMerchant(plainEnglish).lowercase().contains(query) ||
          tx.category.displayLabel(plainEnglish).lowercase().contains(query)
      }
      matchesFilter && matchesSearch
    }
  }
  val visibleTransactions = remember(filteredTransactions, visibleLimit) {
    if (filteredTransactions.size <= visibleLimit) {
      filteredTransactions
    } else {
      filteredTransactions.subList(0, visibleLimit)
    }
  }

  val currentInsight = remember(profile, goal, transactions, safeToSpendResult, dismissedInsightId, plainEnglish) {
    val evaluated = InsightEngine.evaluate(
      profile = profile,
      goal = goal,
      transactions = transactions,
      safeToSpend = safeToSpendResult,
      cooldownState = CooldownState(),
      forcePreview = true,
      plainEnglish = plainEnglish
    )
    if (evaluated != null && evaluated.id != dismissedInsightId) evaluated else null
  }

  val potPrefix = GharTerms.savingsPotName(plainEnglish)

  fun dropIntoGullakFromKitchen(amountRupees: Long, labelMerchant: String = "$potPrefix: ${goal.name}") {
    if (amountRupees <= 0L) return
    val prevGoalBalance = container.goalRepository.primaryGoalState.value.currentAmountPaise
    val tx = Transaction(
      amountPaise = amountRupees * 100L,
      direction = Direction.DEBIT,
      merchant = labelMerchant,
      category = Category.TRANSFERS,
      source = CaptureSource.MANUAL
    )
    GharSoundEffects.playGullakCoinClink(context, prefs.soundEnabled)
    container.goalRepository.addToGullak(amountRupees * 100L)
    container.transactionRepository.insert(tx)
    undoController.recordAction(
      message = "Saved ${CurrencyFormatter.formatRupees(amountRupees)} to $potPrefix"
    ) {
      container.transactionRepository.delete(tx.id)
      container.goalRepository.setGullakBalance(prevGoalBalance)
    }
  }

  fun submitParsedQuickDraft(draft: SmartKhataParser.ParsedKhataDraft) {
    val prevGoalBalance = container.goalRepository.primaryGoalState.value.currentAmountPaise
    val tx = Transaction(
      amountPaise = draft.amountRupees * 100L,
      direction = if (draft.isExpense) Direction.DEBIT else Direction.CREDIT,
      merchant = draft.merchant,
      category = draft.category,
      source = CaptureSource.MANUAL,
      isRecurring = draft.isRecurring
    )
    val gullakDelta = tx.gullakImpactPaise()
    container.transactionRepository.insert(tx)
    if (gullakDelta != 0L) {
      if (gullakDelta > 0L) {
        GharSoundEffects.playGullakCoinClink(context, prefs.soundEnabled)
      }
      container.goalRepository.adjustGullakDelta(gullakDelta)
    }
    focusManager.clearFocus()
    cashAnimation = CashAnimRequest(
      amountPaise = tx.amountPaise,
      isIncoming = !tx.isExpense
    )
    undoController.recordAction(
      message = if (draft.isGullakDrop) {
        "Saved ${CurrencyFormatter.formatRupees(draft.amountRupees)} to $potPrefix"
      } else {
        "Logged \"${draft.merchant}\" (${CurrencyFormatter.formatRupees(draft.amountRupees)})"
      }
    ) {
      container.transactionRepository.delete(tx.id)
      if (gullakDelta != 0L) {
        container.goalRepository.setGullakBalance(prevGoalBalance)
      }
    }
  }

  fun eraseTransactionWithUndo(txToErase: Transaction) {
    val prevGoalBalance = container.goalRepository.primaryGoalState.value.currentAmountPaise
    val delta = -txToErase.gullakImpactPaise()
    container.transactionRepository.delete(txToErase.id)
    if (delta != 0L) {
      container.goalRepository.adjustGullakDelta(delta)
    }
    undoController.recordAction("Deleted \"${txToErase.displayMerchant(plainEnglish)}\"") {
      container.transactionRepository.insert(txToErase)
      if (delta != 0L) {
        container.goalRepository.setGullakBalance(prevGoalBalance)
      }
    }
  }

  LaunchedEffect(Unit) {
    CaptureEventBus.newTransactionEvent.collect { newTx ->
      cashAnimation = CashAnimRequest(
        amountPaise = newTx.amountPaise,
        isIncoming = !newTx.isExpense
      )
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .pastelHomeBackdrop(
        primaryWash = PastelTerracotta,
        secondaryWash = PastelHoney,
        tertiaryWash = PastelSage
      )
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item(key = "kitchen_hero", contentType = "hero") {
        KitchenGreetingHero(
          userName = displayUserName,
          streakDays = quietStreakDays,
          scrollOffsetProvider = {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 600
          },
          onAvatarClick = { showMaaGuidanceDialog = true },
          onHelpClick = onShowRoomGuide
        )
      }

      item(key = "safe_to_spend_card", contentType = "safe_card") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          SafeToSpendHeroCard(
            result = safeToSpendResult,
            savingsTargetRupees = profile.savingsTargetRupees,
            spentTodayRupees = spentTodayRupees,
            plainEnglish = plainEnglish,
            detectedBankBalancePaise = prefs.lastDetectedBankBalancePaise
          )
        }
      }

      // Leaf-isolated Natural-Language Quick Entry Bar
      item(key = "quick_bol_bar", contentType = "quick_bol") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          QuickBolScribbleCard(
            goalName = goal.name,
            plainEnglish = plainEnglish,
            onSubmitDraft = { draft -> submitParsedQuickDraft(draft) }
          )
        }
      }

      // Quick Shortcuts / Stamps right below Quick Entry
      item(key = "quick_khata_stamps", contentType = "stamps") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          QuickKhataStampsSection(
            chips = activeQuickChips,
            onStampClick = { chip ->
              val prevGoalBalance = container.goalRepository.primaryGoalState.value.currentAmountPaise
              val stampedTx = Transaction(
                amountPaise = chip.amountPaise,
                direction = chip.direction,
                merchant = chip.displayMerchant(plainEnglish),
                category = chip.category,
                source = CaptureSource.MANUAL
              )
              val gullakDelta = stampedTx.gullakImpactPaise()
              container.transactionRepository.insert(stampedTx)
              if (gullakDelta != 0L) {
                container.goalRepository.adjustGullakDelta(gullakDelta)
              }
              cashAnimation = CashAnimRequest(
                amountPaise = stampedTx.amountPaise,
                isIncoming = !stampedTx.isExpense
              )
              GharSoundEffects.playGullakCoinClink(context, prefs.soundEnabled)
              undoController.recordAction(
                message = if (plainEnglish) "Logged ${CurrencyFormatter.formatRupees(stampedTx.amountPaise)}" else "${CurrencyFormatter.formatRupees(stampedTx.amountPaise)} ka hisaab likha"
              ) {
                container.transactionRepository.delete(stampedTx.id)
                if (gullakDelta != 0L) {
                  container.goalRepository.setGullakBalance(prevGoalBalance)
                }
              }
            },
            onOpenCustomize = { createMode ->
              startCustomizeInCreateMode = createMode
              showCustomizeChipsDialog = true
            }
          )
        }
      }

        // Utility Grid: SMS & Smart Alerts + Plain English Toggle + Calculator + Saved Notes
        item(key = "kitchen_utility_row", contentType = "utility_row") {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              KitchenUtilityActionCard(
                icon = Icons.Default.NotificationsActive,
                title = "SMS & Alerts",
                subtitle = if (prefs.dailyReminderEnabled) {
                  "Daily reminder ON • Scan SMS"
                } else {
                  "Read bank SMS & reminders"
                },
                accent = PastelSky,
                onClick = { showSmartAlertsDialog = true },
                modifier = Modifier
                  .weight(1f)
                  .testTag("open_smart_alerts_button")
              )
              val commitmentsSubtitle = when {
                commitmentsSummary.pendingOutflowRupees > 0L ->
                  "${CurrencyFormatter.formatCompactRupees(commitmentsSummary.pendingOutflowRupees)} due • Subs & EMIs"
                commitmentsSummary.pendingReceivableRupees > 0L ->
                  "+${CurrencyFormatter.formatCompactRupees(commitmentsSummary.pendingReceivableRupees)} expected in"
                else -> "All settled • Manage bills & subs"
              }
              KitchenUtilityActionCard(
                icon = Icons.Default.EventRepeat,
                title = GharTerms.commitmentsCardShortTitle(plainEnglish),
                subtitle = commitmentsSubtitle,
                accent = PastelPlum,
                onClick = { showCommitmentsDialog = true },
                modifier = Modifier
                  .weight(1f)
                  .testTag("open_kitchen_commitments_button")
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              KitchenUtilityActionCard(
                icon = Icons.Default.Calculate,
                title = GharTerms.calculatorTitle(plainEnglish),
                subtitle = "Split bills & check math",
                accent = PastelHoney,
                onClick = { showCalculatorDialog = true },
                modifier = Modifier
                  .weight(1f)
                  .testTag("open_kitchen_calculator_button")
              )
              KitchenUtilityActionCard(
                icon = Icons.Default.BookmarkAdded,
                title = GharTerms.notesBadgeLabel(plainEnglish, stashedChitthis.size),
                subtitle = if (plainEnglish) "Mom & Dad's saved notes" else "Maa & Papa's kept notes",
                accent = PastelSage,
                onClick = { showChitthiDabbaDialog = true },
                modifier = Modifier
                  .weight(1f)
                  .testTag("open_kitchen_chitthis_button")
              )
            }
          }
        }

        // Mom/Maa Spending Insight Card
        item(key = "maa_insight_note", contentType = "maa_note") {
          val insightStashedId = currentInsight?.let { "insight_${it.id}" }
          val isInsightStashed = remember(stashedChitthis, insightStashedId) {
            insightStashedId != null && stashedChitthis.any { it.id == insightStashedId }
          }
          val activeSankalp = prefs.activeHabitSankalp

          Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            MaaInsightSection(
              insight = currentInsight,
              activeSankalp = activeSankalp,
              transactionsSincePledge = if (activeSankalp != null) transactions else emptyList(),
              isStashed = isInsightStashed,
              plainEnglish = plainEnglish,
              onCardClick = {
                val insight = currentInsight
                if (insight != null) {
                  showChitthiDetailDialog = WisdomChitthiSeed(
                    id = "insight_${insight.id}",
                    author = ChitthiAuthor.MAA,
                    title = if (plainEnglish) "Mom's Budget Note" else "Maa's Fridge Note",
                    body = insight.renderedText,
                    tag = "Budget Insight"
                  )
                }
              },
              onStashInsight = { insightToStash ->
                val noteId = "insight_${insightToStash.id}"
                if (isInsightStashed) {
                  container.chitthiRepository.remove(noteId)
                } else {
                  val note = ChitthiNote(
                    id = noteId,
                    author = ChitthiAuthor.MAA,
                    title = if (plainEnglish) "Mom's Budget Note" else "Maa's Fridge Note",
                    body = insightToStash.renderedText,
                    tag = "Budget Insight"
                  )
                  container.chitthiRepository.stash(note)
                }
              },
              onDismiss = { insightId ->
                dismissedInsightId = insightId
              },
              onAcceptAction = {
                val insight = currentInsight ?: return@MaaInsightSection
                val targetCat = when (insight.id) {
                  "repeated_chai" -> Category.CHAI_SNACKS
                  "food_delivery_creep" -> Category.FOOD_DELIVERY
                  "cab_habit" -> Category.COMMUTE
                  else -> null
                }
                val prepText = when (insight.id) {
                  "repeated_chai" -> if (plainEnglish) "Set coffee mug & powder on counter tonight • Morning reminder active" else "Raat ko hi mug aur chai/coffee dabba nikaal ke rakho • Subah reminder active"
                  "food_delivery_creep" -> if (plainEnglish) "Plan simple home dinner or meal-prep tonight • Evening reminder active" else "Shaam ko hi ghar ka khana decide karo • Evening reminder active"
                  "cab_habit" -> if (plainEnglish) "Charge metro card & set out shoes tonight • Morning reminder active" else "Metro card recharge aur shoes ready rakho • Subah reminder active"
                  else -> if (plainEnglish) "Set daily reminder & keep budget tight" else "Dhyan se kharcha karo aur bachat karo"
                }
                val rHour = if (insight.id == "food_delivery_creep") 19 else 8
                val rMin = 30
                val estimatedSaved = when (targetCat) {
                  Category.CHAI_SNACKS -> 60L
                  Category.FOOD_DELIVERY -> 180L
                  Category.COMMUTE -> 120L
                  else -> 100L
                }
                val sankalp = ActiveHabitSankalp(
                  insightId = insight.id,
                  title = insight.actionOffer ?: "Tomorrow's Habit Pledge",
                  prepChecklist = prepText,
                  targetCategoryName = targetCat?.name,
                  estimatedSavedRupees = estimatedSaved,
                  reminderHour = rHour,
                  reminderMinute = rMin,
                  createdAtMs = System.currentTimeMillis(),
                  expiresAtMs = System.currentTimeMillis() + 86_400_000L
                )
                scope.launch {
                  container.preferencesRepository.setActiveHabitSankalp(sankalp)
                  val routineId = "routine_sankalp_${insight.id}"
                  val routineTitle = "Habit Pledge: ${sankalp.title}"
                  val routine = DailyReminderRoutine(
                    id = routineId,
                    title = routineTitle,
                    hour = rHour,
                    minute = rMin,
                    kind = RoutineKind.CUSTOM,
                    enabled = true
                  )
                  container.preferencesRepository.upsertDailyRoutine(routine)
                  HouseholdNotificationHelper.scheduleRoutineAlarm(context, routine)
                  GharSoundEffects.playPaperChitthiPin(context, prefs.soundEnabled)
                }
              },
              onCompletePledge = { savedAmountRupees ->
                dropIntoGullakFromKitchen(savedAmountRupees)
                scope.launch {
                  container.preferencesRepository.setActiveHabitSankalp(null)
                }
              },
              onCancelPledge = {
                scope.launch {
                  container.preferencesRepository.setActiveHabitSankalp(null)
                }
              }
            )
          }
        }

        // Mom/Maa Advice & Quote Note
        item(key = "maa_wisdom_chitthi", contentType = "maa_wisdom") {
          Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            InteractiveChitthiStickyCard(
              seed = activeMaaSeed,
              isStashed = isMaaSeedStashed,
              stashedCount = stashedChitthis.size,
              onCycleNext = {
                maaSeedIndex = (maaSeedIndex + 1) % ChitthiWisdomBank.maaSeeds.size
              },
              onToggleStash = {
                if (isMaaSeedStashed) {
                  container.chitthiRepository.remove(activeMaaSeed.id)
                } else {
                  val note = activeMaaSeed.toChitthiNote()
                  container.chitthiRepository.stash(note)
                }
              },
              onOpenDabba = { showChitthiDabbaDialog = true },
              onQuickGullakAction = { suggestedRupees ->
                dropIntoGullakFromKitchen(suggestedRupees)
              },
              onCardBodyClick = { showChitthiDetailDialog = activeMaaSeed }
            )
          }
        }



        // Ledger Header + Search & Filter Pills + Sample Cleanup
        item(key = "khata_header", contentType = "section_header") {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
              .padding(top = 4.dp)
          ) {
            RangoliBrassOrnament(
              accentColor = PastelTerracotta,
              modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = GharTerms.ledgerName(plainEnglish),
                  style = MaterialTheme.typography.headlineSmall,
                  color = MaterialTheme.colorScheme.onBackground,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "Tap any entry to edit • tap trash icon to delete",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = {
                    showSearchBar = !showSearchBar
                    if (!showSearchBar) searchQuery = ""
                  },
                  modifier = Modifier
                    .size(48.dp)
                    .testTag("toggle_khata_search_button")
                ) {
                  Icon(
                    imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search transactions",
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
                Surface(
                  shape = RoundedCornerShape(50),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                  Text(
                    text = if (plainEnglish) "${filteredTransactions.size} entries" else "${filteredTransactions.size} notes",
                    style = GharHandStyles.stickyNoteMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                  )
                }
              }
            }

            if (showSearchBar) {
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search merchant or category...") },
                leadingIcon = {
                  Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                  if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                      Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                    }
                  }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("khata_search_input")
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              KhataFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                  onClick = { selectedFilter = filter },
                  shape = CircleShape,
                  color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.surface
                  },
                  border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                  ),
                  modifier = Modifier
                    .defaultMinSize(minHeight = 38.dp)
                    .testTag("khata_filter_${filter.name.lowercase()}")
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                  ) {
                    Text(
                      text = filter.displayLabel(plainEnglish),
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                  }
                }
              }

              if (seedTransactions.isNotEmpty()) {
                Surface(
                  onClick = { showClearSamplesConfirm = true },
                  shape = CircleShape,
                  color = PastelHoney.copy(alpha = 0.16f),
                  border = BorderStroke(1.dp, PastelHoney.copy(alpha = 0.45f)),
                  modifier = Modifier
                    .defaultMinSize(minHeight = 38.dp)
                    .testTag("clear_sample_notes_button")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.CleaningServices,
                      contentDescription = null,
                      tint = PastelHoney,
                      modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Clear ${seedTransactions.size} samples",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }
                }
              }
            }
          }
        }

        if (filteredTransactions.isEmpty()) {
          item(key = "empty_ledger", contentType = "empty") {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
              EmptyLedgerCard(
                plainEnglish = plainEnglish,
                isFiltered = transactions.isNotEmpty() && (selectedFilter != KhataFilter.ALL || searchQuery.isNotBlank()),
                onResetFilters = {
                  selectedFilter = KhataFilter.ALL
                  searchQuery = ""
                }
              )
            }
          }
        } else {
          items(
            items = visibleTransactions,
            key = { it.id },
            contentType = { "tx_item" }
          ) { tx ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
              TransactionItemCard(
                tx = tx,
                plainEnglish = plainEnglish,
                onClick = { editingTransaction = tx },
                onQuickDelete = { eraseTransactionWithUndo(tx) }
              )
            }
          }

          if (filteredTransactions.size > visibleLimit) {
            item(key = "load_more_notes", contentType = "load_more") {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
              ) {
                TextButton(
                  onClick = { visibleLimit += 25 },
                  modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                  Text(
                    text = "Show more (${filteredTransactions.size - visibleLimit} older)",
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }

      ExtendedFloatingActionButton(
        onClick = {
          prefilledQuickAddRupees = null
          showQuickAddDialog = true
        },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 16.dp, bottom = 12.dp)
          .testTag("jot_note_fab"),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
      ) {
        Text(
          text = if (plainEnglish) "Log Spend" else "Add Kharcha",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          maxLines = 1
        )
      }

      cashAnimation?.let { req ->
        if (painDial == com.example.core.model.PainDial.SUBTLE) {
          LaunchedEffect(req) {
            cashAnimation = null
          }
        } else {
          PhysicalCashAnimationOverlay(
            amountPaise = req.amountPaise,
            painDial = painDial,
            hapticsEnabled = hapticsEnabled,
            isIncoming = req.isIncoming,
            onAnimationFinished = {
              cashAnimation = null
            }
          )
        }
      }

      if (showSmartAlertsDialog) {
        SmartAlertsAndSmsDialog(
          container = container,
          onDismiss = { showSmartAlertsDialog = false }
        )
      }

      if (showCalculatorDialog) {
        HisaabCalculatorDialog(
          onDismiss = { showCalculatorDialog = false },
          onJotExpense = { calculatedRupees ->
            showCalculatorDialog = false
            prefilledQuickAddRupees = calculatedRupees
            showQuickAddDialog = true
          },
          onDropInGullak = { calculatedRupees ->
            showCalculatorDialog = false
            dropIntoGullakFromKitchen(calculatedRupees)
          }
        )
      }

      val currentSeed = showChitthiDetailDialog
      if (currentSeed != null) {
        val isStashed = stashedChitthis.any { it.id == currentSeed.id }
        ChitthiDetailDialog(
          seed = currentSeed,
          isStashed = isStashed,
          onToggleStash = {
            if (isStashed) {
              container.chitthiRepository.remove(currentSeed.id)
            } else {
              container.chitthiRepository.stash(currentSeed.toChitthiNote())
            }
          },
          onOpenDabba = {
            showChitthiDetailDialog = null
            showChitthiDabbaDialog = true
          },
          onAskAuthor = { seed ->
            showChitthiDetailDialog = null
            pendingCrossChatPrompt = "I was reading this note from you: \"${seed.body}\". Can we chat more about this?"
            if (seed.author == ChitthiAuthor.PAPA) {
              showPapaGuidanceDialog = true
            } else {
              showMaaGuidanceDialog = true
            }
          },
          onDismiss = { showChitthiDetailDialog = null }
        )
      }

      if (showChitthiDabbaDialog) {
        ChitthiDabbaDialog(
          stashedNotes = stashedChitthis,
          onStashSurpriseNote = { surprise ->
            container.chitthiRepository.stash(surprise)
            undoController.recordAction("Saved \"${surprise.title}\"") {
              container.chitthiRepository.remove(surprise.id)
            }
          },
          onRemoveNote = { removed ->
            container.chitthiRepository.remove(removed.id)
            undoController.recordAction("Removed \"${removed.title}\"") {
              container.chitthiRepository.stash(removed)
            }
          },
          onDismiss = { showChitthiDabbaDialog = false }
        )
      }

      if (showQuickAddDialog) {
        QuickAddDialog(
          quickChips = activeQuickChips,
          initialAmountRupees = prefilledQuickAddRupees,
          onOpenCustomizeChips = {
            showQuickAddDialog = false
            prefilledQuickAddRupees = null
            startCustomizeInCreateMode = false
            showCustomizeChipsDialog = true
          },
          onDismiss = {
            showQuickAddDialog = false
            prefilledQuickAddRupees = null
          },
          onSave = { amountRupees, merchant, category, isExpense, isRecurring ->
            val prevGoalBalance = container.goalRepository.primaryGoalState.value.currentAmountPaise
            val tx = Transaction(
              amountPaise = amountRupees * 100L,
              direction = if (isExpense) Direction.DEBIT else Direction.CREDIT,
              merchant = merchant,
              category = category,
              source = CaptureSource.MANUAL,
              isRecurring = isRecurring
            )
            val gullakDelta = tx.gullakImpactPaise()
            container.transactionRepository.insert(tx)
            if (gullakDelta != 0L) {
              container.goalRepository.adjustGullakDelta(gullakDelta)
            }
            showQuickAddDialog = false
            prefilledQuickAddRupees = null
            cashAnimation = CashAnimRequest(
              amountPaise = tx.amountPaise,
              isIncoming = !isExpense
            )
            undoController.recordAction(
              message = "Logged \"${tx.displayMerchant(plainEnglish)}\" (${CurrencyFormatter.formatRupees(amountRupees)})"
            ) {
              container.transactionRepository.delete(tx.id)
              if (gullakDelta != 0L) {
                container.goalRepository.setGullakBalance(prevGoalBalance)
              }
            }
          }
        )
      }

      if (showCustomizeChipsDialog) {
        QuickChipsCustomizeDialog(
          chips = activeQuickChips,
          startInCreateMode = startCustomizeInCreateMode,
          onSaveChip = { chipToSave ->
            val existedBefore = activeQuickChips.firstOrNull { it.id == chipToSave.id }
            if (!quickChipsSeeded && storedQuickChips.isEmpty()) {
              container.quickChipRepository.insertDefaultsIfEmpty()
              container.appScope.launch {
                container.preferencesRepository.markQuickChipsSeeded(container.userId)
              }
            }
            container.quickChipRepository.upsert(chipToSave)
            undoController.recordAction("Saved shortcut \"${chipToSave.label}\"") {
              if (existedBefore != null) {
                container.quickChipRepository.upsert(existedBefore)
              } else {
                container.quickChipRepository.delete(chipToSave.id)
              }
            }
          },
          onDeleteChip = { chipToDelete ->
            if (!quickChipsSeeded && storedQuickChips.isEmpty()) {
              container.quickChipRepository.insertDefaultsIfEmpty()
              container.appScope.launch {
                container.preferencesRepository.markQuickChipsSeeded(container.userId)
              }
            }
            container.quickChipRepository.delete(chipToDelete.id)
            undoController.recordAction("Removed shortcut \"${chipToDelete.label}\"") {
              container.quickChipRepository.upsert(chipToDelete)
            }
          },
          onReorderChips = { reordered ->
            val prevOrder = activeQuickChips.toList()
            if (!quickChipsSeeded && storedQuickChips.isEmpty()) {
              container.quickChipRepository.insertDefaultsIfEmpty()
              container.appScope.launch {
                container.preferencesRepository.markQuickChipsSeeded(container.userId)
              }
            }
            container.quickChipRepository.updateSortOrders(reordered)
            undoController.recordAction("Reordered shortcuts") {
              container.quickChipRepository.updateSortOrders(prevOrder)
            }
          },
          onResetDefaults = {
            val prevChips = activeQuickChips.toList()
            container.quickChipRepository.resetToDefaults()
            container.appScope.launch {
              container.preferencesRepository.markQuickChipsSeeded(container.userId)
            }
            undoController.recordAction("Restored default shortcuts") {
              val defaultIds = QuickChip.defaultChips(plainEnglish).map { it.id }.toSet()
              val prevIds = prevChips.map { it.id }.toSet()
              (defaultIds - prevIds).forEach { container.quickChipRepository.delete(it) }
              prevChips.forEach { container.quickChipRepository.upsert(it) }
            }
          },
          onDismiss = {
            showCustomizeChipsDialog = false
            startCustomizeInCreateMode = false
          }
        )
      }

      editingTransaction?.let { txToEdit ->
        EditTransactionDialog(
          transaction = txToEdit,
          onDismiss = { editingTransaction = null },
          onSave = { updatedTx ->
            val prevGoalBalance = container.goalRepository.primaryGoalState.value.currentAmountPaise
            val delta = updatedTx.gullakImpactPaise() - txToEdit.gullakImpactPaise()
            container.transactionRepository.update(updatedTx)
            if (delta != 0L) {
              container.goalRepository.adjustGullakDelta(delta)
            }
            editingTransaction = null
            undoController.recordAction("Updated \"${updatedTx.displayMerchant(plainEnglish)}\"") {
              container.transactionRepository.update(txToEdit)
              if (delta != 0L) {
                container.goalRepository.setGullakBalance(prevGoalBalance)
              }
            }
          },
          onDelete = { txToErase ->
            editingTransaction = null
            eraseTransactionWithUndo(txToErase)
          }
        )
      }

      if (showCommitmentsDialog) {
        PakkeCommitmentsDialog(
          summary = commitmentsSummary,
          plainEnglish = plainEnglish,
          onDismiss = { showCommitmentsDialog = false },
          onSaveCommitment = { savedItem, markSettledNow ->
            val prevExisting = commitments.firstOrNull { it.id == savedItem.id }
            container.commitmentRepository.upsert(savedItem)
            var settledTx: Transaction? = null
            if (markSettledNow) {
              val tx = HouseholdAnalyticsEngine.buildCommitmentSettlementTransaction(savedItem)
              settledTx = tx
              container.transactionRepository.insert(tx)
            }
            undoController.recordAction("Saved \"${savedItem.displayTitle(plainEnglish)}\"") {
              settledTx?.let { container.transactionRepository.delete(it.id) }
              if (prevExisting != null) {
                container.commitmentRepository.upsert(prevExisting)
              } else {
                container.commitmentRepository.delete(savedItem.id)
              }
            }
          },
          onDeleteCommitment = { deleted ->
            container.commitmentRepository.delete(deleted.id)
            undoController.recordAction("Removed \"${deleted.displayTitle(plainEnglish)}\"") {
              container.commitmentRepository.upsert(deleted)
            }
          },
          onSettleCommitment = { item ->
            val tx = HouseholdAnalyticsEngine.buildCommitmentSettlementTransaction(item)
            container.transactionRepository.insert(tx)
            cashAnimation = CashAnimRequest(
              amountPaise = tx.amountPaise,
              isIncoming = item.isReceivable
            )
            val verb = if (item.isReceivable) "Received" else "Paid"
            undoController.recordAction(
              "$verb \"${item.displayTitle(plainEnglish)}\" (${CurrencyFormatter.formatRupees(item.amountRupees)})"
            ) {
              container.transactionRepository.delete(tx.id)
            }
          },
          onUnsettleCommitment = { item, matchedTx ->
            container.transactionRepository.delete(matchedTx.id)
            undoController.recordAction("Unmarked \"${item.displayTitle(plainEnglish)}\" for this cycle") {
              container.transactionRepository.insert(matchedTx)
            }
          },
          onResetDefaults = {
            val prevList = commitments.toList()
            container.commitmentRepository.resetToDefaults()
            undoController.recordAction("Restored default monthly commitments") {
              val defaultIds = PakkaCommitment.defaultCommitments().map { it.id }.toSet()
              val prevIds = prevList.map { it.id }.toSet()
              (defaultIds - prevIds).forEach { container.commitmentRepository.delete(it) }
              prevList.forEach { container.commitmentRepository.upsert(it) }
            }
          }
        )
      }

      if (showClearSamplesConfirm) {
        GharConfirmDialog(
          title = "Clear Starter Sample Entries?",
          message = "This removes all ${seedTransactions.size} starter sample transactions from your ledger. Your own recorded entries will stay untouched.",
          confirmLabel = "Clear Samples",
          onConfirm = {
            showClearSamplesConfirm = false
            val removedSeeds = seedTransactions.toList()
            removedSeeds.forEach { container.transactionRepository.delete(it.id) }
            undoController.recordAction("Cleared ${removedSeeds.size} starter sample entries") {
              container.transactionRepository.insertAll(removedSeeds)
            }
          },
          onDismiss = { showClearSamplesConfirm = false },
          dialogTestTag = "confirm_clear_samples_dialog",
          confirmTestTag = "confirm_clear_samples_button"
        )
      }

      if (showMaaGuidanceDialog) {
        MaaGuidanceDialog(
          container = container,
          userName = displayUserName,
          safeToSpend = safeToSpendResult,
          profile = profile,
          goal = goal,
          transactions = transactions,
          commitments = commitments,
          allGoals = allGoals,
          initialPrompt = pendingCrossChatPrompt,
          onSwitchToPapa = { promptToForward ->
            showMaaGuidanceDialog = false
            pendingCrossChatPrompt = promptToForward
            showPapaGuidanceDialog = true
          },
          onDismiss = {
            showMaaGuidanceDialog = false
            pendingCrossChatPrompt = null
          }
        )
      }

      if (showPapaGuidanceDialog) {
        PapaGuidanceDialog(
          container = container,
          userName = displayUserName,
          safeToSpend = safeToSpendResult,
          profile = profile,
          goal = goal,
          transactions = transactions,
          commitments = commitments,
          allGoals = allGoals,
          initialPrompt = pendingCrossChatPrompt,
          onSwitchToMaa = { promptToForward ->
            showPapaGuidanceDialog = false
            pendingCrossChatPrompt = promptToForward
            showMaaGuidanceDialog = true
          },
          onDismiss = {
            showPapaGuidanceDialog = false
            pendingCrossChatPrompt = null
          }
        )
      }

      if (!hasParentUndo) {
        GharUndoSnackbarHost(
          controller = undoController,
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 72.dp)
        )
      }
    }
  }

/**
 * Leaf-isolated Quick Entry bar: keystrokes only recompose this card, never the parent LazyColumn.
 */
@Composable
private fun QuickBolScribbleCard(
  goalName: String,
  plainEnglish: Boolean,
  onSubmitDraft: (SmartKhataParser.ParsedKhataDraft) -> Unit
) {
  var text by remember { mutableStateOf("") }
  val parsedDraft = remember(text, goalName, plainEnglish) {
    SmartKhataParser.parse(text, activeGoalName = goalName, plainEnglish = plainEnglish)
  }

  fun submit() {
    val draft = parsedDraft ?: return
    text = ""
    onSubmitDraft(draft)
  }

  Surface(
    shape = PebbleCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.dp, PastelTerracotta.copy(alpha = 0.28f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("quick_bol_card")
  ) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = {
          Text(
            text = "Quick entry: ${GharTerms.quickEntryPlaceholder(plainEnglish)}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
        },
        trailingIcon = {
          IconButton(
            onClick = { submit() },
            enabled = parsedDraft != null,
            modifier = Modifier
              .size(48.dp)
              .testTag("quick_bol_submit_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Save quick entry",
              tint = if (parsedDraft != null) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
              }
            )
          }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { if (parsedDraft != null) submit() }),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("quick_bol_input")
      )

      if (parsedDraft != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
              if (parsedDraft.isGullakDrop || !parsedDraft.isExpense) {
                PastelSage.copy(alpha = 0.14f)
              } else {
                PastelHoney.copy(alpha = 0.16f)
              }
            )
            .clickable { submit() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            CategoryIcon(
              category = parsedDraft.category,
              size = 16.dp,
              tint = if (!parsedDraft.isExpense || parsedDraft.isGullakDrop) PastelSage else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = buildString {
                append(parsedDraft.merchant)
                append(" • ")
                append(parsedDraft.category.displayLabel(plainEnglish))
                if (parsedDraft.isRecurring) append(" • Monthly")
              },
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (parsedDraft.isExpense) {
              "Log ${CurrencyFormatter.formatRupees(parsedDraft.amountRupees)}"
            } else {
              "Add +${CurrencyFormatter.formatRupees(parsedDraft.amountRupees)}"
            },
            style = GharHandStyles.moneySmall,
            color = if (!parsedDraft.isExpense || parsedDraft.isGullakDrop) PastelSage else MaterialTheme.colorScheme.primary,
            maxLines = 1
          )
        }
      }
    }
  }
}

@Composable
private fun KitchenUtilityActionCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    onClick = onClick,
    shape = PebbleCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.dp, accent.copy(alpha = 0.36f)),
    modifier = modifier
      .defaultMinSize(minHeight = 62.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(accent.copy(alpha = 0.20f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun SafeToSpendHeroCard(
  result: SafeToSpendResult,
  savingsTargetRupees: Long,
  spentTodayRupees: Long,
  plainEnglish: Boolean,
  detectedBankBalancePaise: Long?
) {
  val accentColor = MaterialTheme.colorScheme.primary
  val heroBgBrush = remember {
    Brush.linearGradient(
      colors = listOf(
        PastelTerracotta.copy(alpha = 0.08f),
        Color.Transparent,
        PastelHoney.copy(alpha = 0.08f)
      )
    )
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("safe_to_spend_hero"),
    shape = JharokhaCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.24f))
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(heroBgBrush)
        .jharokhaCornerFiligree(accentColor = PastelTerracotta)
        .padding(20.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Safe to Spend Today",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedContent(
              targetState = result.dailyAllowanceRupees,
              transitionSpec = {
                if (targetState > initialState) {
                  slideInVertically { height -> height } + fadeIn() togetherWith
                    slideOutVertically { height -> -height } + fadeOut()
                } else {
                  slideInVertically { height -> -height } + fadeIn() togetherWith
                    slideOutVertically { height -> height } + fadeOut()
                }
              },
              label = "dailyAllowanceRoll"
            ) { amount ->
              Text(
                text = CurrencyFormatter.formatRupees(amount),
                style = GharHandStyles.moneyHero,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.drawBehind {
                  drawLine(
                    color = PastelHoney.copy(alpha = 0.32f),
                    start = Offset(4f, size.height - 4f),
                    end = Offset(size.width * 0.92f, size.height - 6f),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                  )
                }
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Spent today: ${CurrencyFormatter.formatRupees(spentTodayRupees)} • ${result.daysRemaining} days left in cycle",
              style = GharHandStyles.stickyNoteMeta,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (detectedBankBalancePaise != null && detectedBankBalancePaise >= 0L) {
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = "SMS Bank Balance: ${CurrencyFormatter.formatRupees(detectedBankBalancePaise / 100L)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = PastelSage,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          val safePercent = if (result.discretionaryPoolPaise > 0) {
            (result.totalRemainingPaise.toFloat() / result.discretionaryPoolPaise.toFloat()).coerceIn(0f, 1f)
          } else 0.5f

          SafeGrowthGauge(
            percentageSafe = safePercent,
            isPaceHot = result.isPaceHot
          )
        }

        val dailyCap = result.dailyAllowanceRupees.coerceAtLeast(1L)
        val todayProgress = (spentTodayRupees.toFloat() / dailyCap.toFloat()).coerceIn(0f, 1f)
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
          progress = { todayProgress },
          color = if (spentTodayRupees > result.dailyAllowanceRupees) PastelTerracotta else PastelSage,
          trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          strokeCap = StrokeCap.Round,
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          HomelyPebbleStat(
            label = if (plainEnglish) "Batua Pool" else "In Batua",
            value = CurrencyFormatter.formatCompactRupees(result.totalRemainingRupees),
            wash = PastelHoney.copy(alpha = 0.16f),
            tint = MaterialTheme.colorScheme.onSurface,
            isMoney = true,
            modifier = Modifier.weight(1f)
          )
          HomelyPebbleStat(
            label = if (plainEnglish) "Target/mo" else "Save Goal",
            value = CurrencyFormatter.formatCompactRupees(savingsTargetRupees),
            wash = PastelSage.copy(alpha = 0.16f),
            tint = PastelSage,
            isMoney = true,
            modifier = Modifier.weight(1f)
          )
          HomelyPebbleStat(
            label = "Cycle Pace",
            value = if (result.isPaceHot) "Over Pace" else "On Track",
            wash = if (result.isPaceHot) PastelTerracotta.copy(alpha = 0.16f) else PastelSky.copy(alpha = 0.16f),
            tint = if (result.isPaceHot) MaterialTheme.colorScheme.primary else PastelSky,
            isMoney = false,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

@Composable
private fun HomelyPebbleStat(
  label: String,
  value: String,
  wash: Color,
  tint: Color,
  isMoney: Boolean,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = wash,
    border = BorderStroke(1.dp, tint.copy(alpha = 0.28f)),
    modifier = modifier.defaultMinSize(minHeight = 62.dp)
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = value,
        style = if (isMoney) GharHandStyles.moneySmall else MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = tint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun MaaInsightSection(
  insight: Insight?,
  activeSankalp: ActiveHabitSankalp? = null,
  transactionsSincePledge: List<Transaction> = emptyList(),
  isStashed: Boolean,
  plainEnglish: Boolean,
  onCardClick: () -> Unit,
  onStashInsight: (Insight) -> Unit,
  onDismiss: (String) -> Unit,
  onAcceptAction: () -> Unit,
  onCompletePledge: (Long) -> Unit = {},
  onCancelPledge: () -> Unit = {}
) {
  val maaTitle = if (plainEnglish) "Mom's Budget Note" else "Maa's Fridge Note"
  val maaSub = if (plainEnglish) "Tap to ask Mom a budget question" else "Tap to chat with Maa over chai"

  if (activeSankalp != null) {
    ActiveHabitSankalpCard(
      sankalp = activeSankalp,
      transactionsSincePledge = transactionsSincePledge,
      plainEnglish = plainEnglish,
      onCompletePledge = onCompletePledge,
      onCancelPledge = onCancelPledge
    )
  } else if (insight != null) {
    HandwrittenHomeNote(
      isMintPaper = false,
      modifier = Modifier
        .clickable { onCardClick() }
        .testTag("maa_insight_card")
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            MaaAvatar(size = 34.dp, isSpeaking = false)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = maaTitle,
                style = GharHandStyles.stickyNoteTitle,
                color = StickyNotePaperColors.InkAccentRed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = maaSub,
                style = GharHandStyles.stickyNoteMeta,
                color = StickyNotePaperColors.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { onStashInsight(insight) },
              modifier = Modifier
                .size(48.dp)
                .testTag("stash_insight_button")
            ) {
              Icon(
                imageVector = if (isStashed) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                contentDescription = "Save note",
                tint = if (isStashed) StickyNotePaperColors.InkAccentSage else StickyNotePaperColors.InkAccentRed,
                modifier = Modifier.size(19.dp)
              )
            }
            IconButton(
              onClick = { onDismiss(insight.id) },
              modifier = Modifier
                .size(48.dp)
                .testTag("dismiss_insight_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss note",
                tint = StickyNotePaperColors.InkMuted,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "“${insight.renderedText}”",
          style = GharHandStyles.stickyNoteBody,
          color = StickyNotePaperColors.InkDark
        )

        insight.actionOffer?.let { offer ->
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = { onAcceptAction() },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = StickyNotePaperColors.InkAccentRed),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .testTag("accept_insight_action_button")
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = Color(0xFFFFFBF2)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = offer,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFFFBF2),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  } else {
    Spacer(modifier = Modifier.height(0.dp))
  }
}

private val IconTileShape = RoundedCornerShape(
  topStart = 18.dp,
  topEnd = 12.dp,
  bottomEnd = 18.dp,
  bottomStart = 12.dp
)

@Composable
private fun TransactionItemCard(
  tx: Transaction,
  plainEnglish: Boolean,
  onClick: () -> Unit,
  onQuickDelete: () -> Unit
) {
  val categoryAccent = when (tx.category) {
    Category.CHAI_SNACKS -> PastelHoney
    Category.FOOD_DELIVERY -> PastelTerracotta
    Category.COMMUTE -> PastelSky
    Category.ALLOWANCE_INCOME -> PastelSage
    Category.GROCERIES -> PastelSage
    Category.SHOPPING -> PastelPlum
    Category.TRANSFERS -> PastelSage
    else -> PastelTerracotta
  }
  val shownMerchant = tx.displayMerchant(plainEnglish)

  Surface(
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("tx_item_${tx.id}"),
    shape = PebbleCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(0.8.dp, categoryAccent.copy(alpha = 0.22f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(IconTileShape)
            .background(
              if (!tx.isExpense) PastelSage.copy(alpha = 0.18f)
              else categoryAccent.copy(alpha = 0.16f)
            ),
          contentAlignment = Alignment.Center
        ) {
          CategoryIcon(
            category = tx.category,
            size = 20.dp,
            tint = if (!tx.isExpense) PastelSage else categoryAccent
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = shownMerchant,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )
            if (tx.isRecurring) {
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = "Monthly recurring bill",
                tint = PastelSky,
                modifier = Modifier.size(14.dp)
              )
            }
          }
          val subtitleLine = remember(tx.category, tx.timestamp, plainEnglish) {
            "${tx.category.displayLabel(plainEnglish)} • ${DateUtils.formatRelativeDate(tx.timestamp)}"
          }
          Text(
            text = subtitleLine,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        val formattedAmount = remember(tx.amountRupees, tx.isExpense) {
          if (tx.isExpense) {
            CurrencyFormatter.formatRupees(tx.amountRupees)
          } else {
            "+${CurrencyFormatter.formatRupees(tx.amountRupees)}"
          }
        }
        Text(
          text = formattedAmount,
          style = GharHandStyles.moneyMedium,
          color = if (tx.isExpense) MaterialTheme.colorScheme.onSurface else PastelSage,
          maxLines = 1
        )
        IconButton(
          onClick = onQuickDelete,
          modifier = Modifier
            .size(48.dp)
            .testTag("delete_tx_${tx.id}")
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete $shownMerchant",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyLedgerCard(
  plainEnglish: Boolean,
  isFiltered: Boolean = false,
  onResetFilters: () -> Unit = {}
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = PebbleCardShape,
    color = PastelHoney.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, PastelHoney.copy(alpha = 0.3f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = Icons.Default.Spa,
        contentDescription = null,
        tint = PastelHoney,
        modifier = Modifier.size(30.dp)
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = when {
          isFiltered -> "No matching transactions"
          plainEnglish -> "No transactions yet"
          else -> "A fresh page in the khata"
        },
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = when {
          isFiltered -> "Try clearing your search or switching back to 'All'."
          plainEnglish -> "Type in Quick Entry above, scan Bank SMS, or tap 'Add Entry' whenever money moves."
          else -> "Type in Quick Entry above, scan Bank SMS, or tap 'Jot Note' whenever money moves."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      if (isFiltered) {
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(
          onClick = onResetFilters,
          modifier = Modifier.defaultMinSize(minHeight = 48.dp)
        ) {
          Text("Show All Entries", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun ActiveHabitSankalpCard(
  sankalp: ActiveHabitSankalp,
  transactionsSincePledge: List<Transaction>,
  plainEnglish: Boolean,
  onCompletePledge: (Long) -> Unit,
  onCancelPledge: () -> Unit
) {
  val (totalOutsideSpent, countSpent) = remember(sankalp, transactionsSincePledge) {
    val targetCat = sankalp.targetCategoryName?.let { catName ->
      runCatching { Category.valueOf(catName) }.getOrNull()
    }
    val spends = transactionsSincePledge.filter { tx ->
      tx.isExpense && (targetCat == null || tx.category == targetCat) && tx.timestamp >= sankalp.createdAtMs
    }
    spends.sumOf { it.amountRupees } to spends.size
  }

  HandwrittenHomeNote(
    isMintPaper = false,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("active_habit_sankalp_card")
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          MaaAvatar(size = 34.dp, isSpeaking = false)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = if (plainEnglish) "Active Habit Pledge • 24h" else "Active Sankalp • 24 Ghante",
              style = GharHandStyles.stickyNoteTitle,
              color = StickyNotePaperColors.InkAccentRed,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${sankalp.title} • ${sankalp.formattedTime} Reminder",
              style = GharHandStyles.stickyNoteMeta,
              color = StickyNotePaperColors.InkMuted,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
        IconButton(
          onClick = onCancelPledge,
          modifier = Modifier
            .size(48.dp)
            .testTag("cancel_habit_sankalp_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Cancel pledge",
            tint = StickyNotePaperColors.InkMuted,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "“${sankalp.prepChecklist}”",
        style = GharHandStyles.stickyNoteBody,
        color = StickyNotePaperColors.InkDark
      )

      Spacer(modifier = Modifier.height(10.dp))

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (countSpent == 0) StickyNotePaperColors.InkAccentSage.copy(alpha = 0.12f) else StickyNotePaperColors.InkAccentRed.copy(alpha = 0.12f),
        border = BorderStroke(
          1.dp,
          if (countSpent == 0) StickyNotePaperColors.InkAccentSage.copy(alpha = 0.40f) else StickyNotePaperColors.InkAccentRed.copy(alpha = 0.40f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (countSpent == 0) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = if (countSpent == 0) StickyNotePaperColors.InkAccentSage else StickyNotePaperColors.InkAccentRed,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (countSpent == 0) {
              if (plainEnglish) "0 outside spends logged so far! Keep going." else "Shabaash! 0 outside spends logged so far."
            } else {
              if (plainEnglish) "$countSpent spend (${CurrencyFormatter.formatRupees(totalOutsideSpent)}) logged since pledge." else "$countSpent spend (${CurrencyFormatter.formatRupees(totalOutsideSpent)}) logged since pledge."
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = StickyNotePaperColors.InkDark
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Button(
        onClick = { onCompletePledge(sankalp.estimatedSavedRupees) },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = StickyNotePaperColors.InkAccentSage),
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 48.dp)
          .testTag("complete_habit_sankalp_button")
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = Color(0xFFFFFBF2)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (plainEnglish) {
            "I kept it — sweep ${CurrencyFormatter.formatRupees(sankalp.estimatedSavedRupees)} saved to Savings"
          } else {
            "Pledge kept — sweep ${CurrencyFormatter.formatRupees(sankalp.estimatedSavedRupees)} saved to Gullak"
          },
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFFFFBF2),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
