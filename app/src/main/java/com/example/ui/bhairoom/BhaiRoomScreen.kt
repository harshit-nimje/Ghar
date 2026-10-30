package com.example.ui.bhairoom

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppContainer
import com.example.R
import com.example.core.audio.GharSoundEffects
import com.example.core.engine.ChitthiWisdomBank
import com.example.core.engine.WisdomChitthiSeed
import com.example.core.engine.HouseholdAnalyticsEngine
import com.example.core.engine.SafeToSpendCalculator
import com.example.core.localization.GharTerms
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.model.BudgetProfile
import com.example.core.model.CaptureSource
import com.example.core.model.Category
import com.example.core.model.Direction
import com.example.core.model.InstrumentType
import com.example.core.model.SavingsGoal
import com.example.core.model.Transaction
import com.example.core.util.CurrencyFormatter
import com.example.core.util.DateUtils
import com.example.ui.common.BotanicalCompoundingVine
import com.example.ui.common.ChitthiDabbaDialog
import com.example.ui.common.ChitthiDetailDialog
import com.example.ui.common.FlyingCoinOverlay
import com.example.ui.common.GharConfirmDialog
import com.example.ui.common.GharUndoSnackbarHost
import com.example.ui.common.GullakCanvas
import com.example.ui.common.GullakSparkleOverlay
import com.example.ui.common.HisaabCalculatorDialog
import com.example.ui.common.IllustratedRoomBanner
import com.example.ui.common.InteractiveChitthiStickyCard
import com.example.ui.common.JharokhaCardShape
import com.example.ui.common.LocalGharUndoController
import com.example.ui.common.MutableOffsetRef
import com.example.ui.common.PapaAvatar
import com.example.ui.common.PebbleCardShape
import com.example.ui.common.TactileCoinMedallion
import com.example.ui.common.jharokhaCornerFiligree
import com.example.ui.common.pastelHomeBackdrop
import com.example.ui.common.rememberGharUndoController
import com.example.ui.kitchen.EditTransactionDialog
import com.example.ui.kitchen.MaaGuidanceDialog
import com.example.ui.theme.ClayGullakColor
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.contrastingContentColor
import java.util.UUID
import kotlin.math.pow
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PaceHorizonOption(val label: String, val days: Int) {
  DAYS_30("30d", 30),
  DAYS_90("90d", 90),
  MONTHS_6("6mo", 180),
  YEAR_1("1yr", 365)
}

@Composable
fun BhaiRoomScreen(
  container: AppContainer,
  listState: LazyListState = rememberLazyListState(),
  onShowRoomGuide: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val plainEnglish = LocalPlainEnglishMode.current
  val coroutineScope = rememberCoroutineScope()
  val hasParentUndo = LocalGharUndoController.current != null
  val fallbackUndo = rememberGharUndoController()
  val undoController = LocalGharUndoController.current ?: fallbackUndo

  val prefs by container.preferencesRepository.prefsSnapshot.collectAsStateWithLifecycle()
  val goal by container.goalRepository.primaryGoalState.collectAsStateWithLifecycle()
  val allGoals by container.goalRepository.allGoalsState.collectAsStateWithLifecycle()
  val transactions by container.transactionRepository.transactionsState.collectAsStateWithLifecycle()
  val commitments by container.commitmentRepository.commitmentsState.collectAsStateWithLifecycle()
  val profile by container.budgetRepository.profileState.collectAsStateWithLifecycle()
  val stashedChitthis by container.chitthiRepository.chitthisState.collectAsStateWithLifecycle()
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

  val spentSoFarPaise = remember(transactions) {
    transactions.filter { it.isExpense }.sumOf { it.amountPaise }
  }
  val creditIncomePaise = remember(transactions) {
    transactions.filter { !it.isExpense }.sumOf { it.amountPaise }
  }
  val safeToSpend = remember(profile, spentSoFarPaise, creditIncomePaise) {
    SafeToSpendCalculator.calculate(profile, spentSoFarPaise, creditIncomePaise)
  }
  val spentTodayRupees = remember(transactions) {
    HouseholdAnalyticsEngine.calculateTodaySpendRupees(transactions)
  }
  val unspentTodayRupees = remember(safeToSpend.dailyAllowanceRupees, spentTodayRupees) {
    (safeToSpend.dailyAllowanceRupees - spentTodayRupees).coerceAtLeast(0L)
  }

  var showAddCoinsSuccess by remember { mutableStateOf(false) }
  var clinkBannerMessage by remember { mutableStateOf("Saved safely in your pot.") }
  var showCustomDepositDialog by remember { mutableStateOf(false) }
  var showDipDialog by remember { mutableStateOf(false) }
  var showSetBalanceDialog by remember { mutableStateOf(false) }
  var showCreateGullakDialog by remember { mutableStateOf(false) }
  var showCalculatorDialog by remember { mutableStateOf(false) }
  var showChitthiDabbaDialog by remember { mutableStateOf(false) }
  var showChitthiDetailDialog by remember { mutableStateOf<WisdomChitthiSeed?>(null) }
  var showPapaGuidanceDialog by remember { mutableStateOf(false) }
  var showMaaGuidanceDialog by remember { mutableStateOf(false) }
  var pendingCrossChatPrompt by remember { mutableStateOf<String?>(null) }
  var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

  var papaNoteIndex by remember {
    mutableIntStateOf(Random.nextInt(ChitthiWisdomBank.papaSeeds.size))
  }
  val activePapaSeed = remember(papaNoteIndex, plainEnglish) {
    ChitthiWisdomBank.pickPapaSeed(papaNoteIndex, plainEnglish)
  }
  val isPapaSeedStashed = remember(stashedChitthis, activePapaSeed.id) {
    stashedChitthis.any { it.id == activePapaSeed.id }
  }

  var isCoinFlying by remember { mutableStateOf(false) }
  var coinStartPos by remember { mutableStateOf(Offset(300f, 900f)) }
  var coinTargetPos by remember { mutableStateOf(Offset(300f, 320f)) }
  // Zero-scroll-overhead coordinate holder: captures LayoutCoordinates via .onPlaced
  val gullakSlotRef = remember { MutableOffsetRef(Offset(300f, 320f)) }
  var lastAddedAmountText by remember { mutableStateOf("+ ₹100") }
  var lastAddedRupees by remember { mutableLongStateOf(100L) }

  var gullakSquished by remember { mutableStateOf(false) }

  val gullakDrops = remember(transactions) {
    transactions.filter {
      it.isGullakTransfer() ||
        it.merchant.contains("Gullak", ignoreCase = true) ||
        it.merchant.contains("Savings Pot", ignoreCase = true)
    }.sortedByDescending { it.timestamp }
  }

  val totalSavedAcrossPotsRupees = remember(allGoals) {
    allGoals.sumOf { it.currentAmountRupees }
  }

  val potTerm = GharTerms.savingsPotName(plainEnglish)
  val walletTerm = GharTerms.walletName(plainEnglish)

  fun triggerCoinDrop(amountRupees: Long, fromPos: Offset, noteLabel: String? = null) {
    if (amountRupees <= 0L) return
    val targetGoal = container.goalRepository.primaryGoalState.value
    val prevBalancePaise = targetGoal.currentAmountPaise
    val depositTx = Transaction(
      amountPaise = amountRupees * 100L,
      direction = Direction.DEBIT,
      merchant = noteLabel ?: "$potTerm: ${targetGoal.name}",
      category = Category.TRANSFERS,
      source = CaptureSource.MANUAL
    )
    var undoneEarly = false

    coinStartPos = if (fromPos == Offset.Zero) Offset(300f, 880f) else fromPos
    val slotCoords = gullakSlotRef.coords
    coinTargetPos = if (slotCoords != null && slotCoords.isAttached) {
      val b = slotCoords.boundsInRoot()
      Offset(b.left + b.width / 2f, b.top + b.height * 0.28f)
    } else {
      gullakSlotRef.value
    }
    lastAddedRupees = amountRupees
    lastAddedAmountText = "+ ${CurrencyFormatter.formatRupees(amountRupees)}"
    isCoinFlying = true

    undoController.recordAction(
      message = "Saved ${CurrencyFormatter.formatRupees(amountRupees)} to ${targetGoal.name}"
    ) {
      undoneEarly = true
      isCoinFlying = false
      showAddCoinsSuccess = false
      container.transactionRepository.delete(depositTx.id)
      container.goalRepository.setGullakBalance(prevBalancePaise)
    }

    coroutineScope.launch {
      delay(440)
      if (undoneEarly) return@launch
      GharSoundEffects.playGullakCoinClink(context, prefs.soundEnabled)
      gullakSquished = true
      container.goalRepository.addToGullak(amountRupees * 100L)
      container.transactionRepository.insert(depositTx)
      clinkBannerMessage = "Added ${CurrencyFormatter.formatRupees(amountRupees)} to ${targetGoal.name}."
      showAddCoinsSuccess = true
      delay(220)
      gullakSquished = false
    }
  }

  fun triggerGullakDip(amountRupees: Long, reason: String) {
    if (amountRupees <= 0L) return
    val targetGoal = container.goalRepository.primaryGoalState.value
    val prevBalancePaise = targetGoal.currentAmountPaise
    val actualDipPaise = (amountRupees * 100L).coerceAtMost(prevBalancePaise.coerceAtLeast(0L))
    val dipPrefix = if (plainEnglish) "Savings Withdrawal" else "Gullak Dip"
    val dipTx = Transaction(
      amountPaise = amountRupees * 100L,
      direction = Direction.CREDIT,
      merchant = if (reason.isBlank()) "$dipPrefix: ${targetGoal.name}" else "$dipPrefix: $reason",
      category = Category.TRANSFERS,
      source = CaptureSource.MANUAL
    )
    if (actualDipPaise > 0L) {
      container.goalRepository.adjustGullakDelta(-actualDipPaise)
    }
    container.transactionRepository.insert(dipTx)
    clinkBannerMessage = "Moved ${CurrencyFormatter.formatRupees(amountRupees)} from ${targetGoal.name} back to $walletTerm."
    showAddCoinsSuccess = true

    undoController.recordAction(
      message = "Withdrew ${CurrencyFormatter.formatRupees(amountRupees)} to $walletTerm"
    ) {
      container.transactionRepository.delete(dipTx.id)
      container.goalRepository.setGullakBalance(prevBalancePaise)
      showAddCoinsSuccess = false
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .pastelHomeBackdrop(
        primaryWash = PastelSage,
        secondaryWash = PastelHoney,
        tertiaryWash = PastelTerracotta
      )
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item(key = "gullak_banner", contentType = "banner") {
        val highlight = if (goal.currentAmountRupees == 0L) {
          "Starts at ₹0 • Target ${CurrencyFormatter.formatCompactRupees(goal.targetAmountRupees)}"
        } else {
          "${goal.progressPercent.toInt()}% of ${CurrencyFormatter.formatCompactRupees(goal.targetAmountRupees)} saved"
        }
        IllustratedRoomBanner(
          artworkRes = R.drawable.img_gullak_sill_1792931330238,
          roomBadge = if (plainEnglish) "Savings • ${goal.name}" else "Papa's Sill • ${goal.name}",
          title = if (plainEnglish) "Savings Pot" else "Papa's Gullak",
          keyHighlight = "$highlight • Ask ${GharTerms.papaName(plainEnglish)} for FD/SIP guidance",
          accentColor = PastelSage,
          heroHeight = 276.dp,
          scrollOffsetProvider = {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 600
          },
          onHelpClick = onShowRoomGuide,
          trailingBadge = {
            Surface(
              onClick = {
                pendingCrossChatPrompt = null
                showPapaGuidanceDialog = true
              },
              shape = CircleShape,
              color = Color(0xFF1E1510).copy(alpha = 0.70f),
              border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.46f)),
              modifier = Modifier
                .defaultMinSize(minHeight = 44.dp)
                .testTag("open_papa_guidance_button")
            ) {
              Row(
                modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                PapaAvatar(size = 32.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = GharTerms.chatWithPapaLabel(plainEnglish),
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFFFFBF4),
                  fontWeight = FontWeight.Bold,
                  maxLines = 1
                )
              }
            }
          }
        )
      }

      // Multi-Pot Shelf
      item(key = "multi_gullak_shelf", contentType = "shelf") {
        MultiGullakShelfRow(
          goals = allGoals,
          activeGoalId = goal.id,
          totalSavedRupees = totalSavedAcrossPotsRupees,
          plainEnglish = plainEnglish,
          onSelectGoal = { selectedId ->
            container.goalRepository.selectGoal(selectedId)
          },
          onAddNewGullak = { showCreateGullakDialog = true },
          onDeleteGoal = { toDelete ->
            val prevActiveId = goal.id
            container.goalRepository.deleteGoal(toDelete.id)
            undoController.recordAction("Removed \"${toDelete.name}\"") {
              container.goalRepository.createGoal(toDelete)
              container.goalRepository.selectGoal(prevActiveId)
            }
          }
        )
      }

      // Utility Strip: Calculator • Withdraw / Dip • Saved Notes
      item(key = "bhai_utility_strip", contentType = "utility") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          BhaiSillUtilityPill(
            icon = Icons.Default.Calculate,
            label = "Calculator",
            accent = PastelHoney,
            onClick = { showCalculatorDialog = true },
            modifier = Modifier
              .weight(1f)
              .testTag("open_bhai_calculator_button")
          )
          BhaiSillUtilityPill(
            icon = Icons.AutoMirrored.Filled.Undo,
            label = if (plainEnglish) "Withdraw" else "Dip to Batua",
            accent = PastelTerracotta,
            onClick = { showDipDialog = true },
            modifier = Modifier
              .weight(1f)
              .testTag("open_gullak_dip_button")
          )
          BhaiSillUtilityPill(
            icon = Icons.Default.BookmarkAdded,
            label = GharTerms.notesBadgeLabel(plainEnglish, stashedChitthis.size),
            accent = PastelSage,
            onClick = { showChitthiDabbaDialog = true },
            modifier = Modifier
              .weight(1f)
              .testTag("open_bhai_chitthis_button")
          )
        }
      }

      // Savings Pot Hero Card
      item(key = "gullak_hero", contentType = "hero_pot") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          GullakHeroCard(
            goal = goal,
            profile = profile,
            dailySafeAllowanceRupees = safeToSpend.dailyAllowanceRupees,
            unspentTodayRupees = unspentTodayRupees,
            plainEnglish = plainEnglish,
            isGullakSquished = gullakSquished,
            gullakSlotRef = gullakSlotRef,
            onDropCoin = { amountRupees, pos ->
              triggerCoinDrop(amountRupees, pos)
            },
            onOpenCustomDeposit = { showCustomDepositDialog = true },
            onOpenSetBalance = { showSetBalanceDialog = true }
          )
        }
      }

      item(key = "gullak_clink_banner", contentType = "clink_banner") {
        AnimatedVisibility(visible = showAddCoinsSuccess) {
          Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Surface(
              shape = PebbleCardShape,
              color = PastelSage.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = PastelSage,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = clinkBannerMessage,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }
      }

      // Unspent Daily Allowance Sweep Card
      if (unspentTodayRupees >= 20L) {
        item(key = "unspent_sweep_card", contentType = "sweep_card") {
          Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            UnspentBatuaSweepCard(
              unspentTodayRupees = unspentTodayRupees,
              goalName = goal.name,
              onSweepAmount = { sweepRupees, pos ->
                triggerCoinDrop(
                  amountRupees = sweepRupees,
                  fromPos = pos,
                  noteLabel = "$potTerm Sweep: ${goal.name}"
                )
              }
            )
          }
        }
      }

      // Target Pace Planner Card
      item(key = "bhai_pace_planner", contentType = "pace_planner") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          BhaiTargetPacePlannerCard(
            goal = goal,
            plainEnglish = plainEnglish,
            dailySafeBatuaRupees = safeToSpend.dailyAllowanceRupees,
            onDropDailyShare = { shareRupees, pos ->
              triggerCoinDrop(shareRupees, pos)
            },
            onOpenCalculator = { showCalculatorDialog = true }
          )
        }
      }

      // Compounding Growth Card
      item(key = "gullak_vine", contentType = "vine") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          ReinvestmentCard(goal = goal)
        }
      }

      // Papa / Dad Interactive Sticky Note
      item(key = "bhai_quote", contentType = "chitthi") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          InteractiveChitthiStickyCard(
            seed = activePapaSeed,
            isStashed = isPapaSeedStashed,
            stashedCount = stashedChitthis.size,
            onCycleNext = {
              papaNoteIndex = (papaNoteIndex + 1) % ChitthiWisdomBank.papaSeeds.size
            },
            onToggleStash = {
              if (isPapaSeedStashed) {
                container.chitthiRepository.remove(activePapaSeed.id)
              } else {
                val note = activePapaSeed.toChitthiNote()
                container.chitthiRepository.stash(note)
              }
            },
            onOpenDabba = { showChitthiDabbaDialog = true },
            onQuickGullakAction = { suggestedRupees ->
              triggerCoinDrop(suggestedRupees, Offset(300f, 880f))
            },
            onCardBodyClick = {
              showChitthiDetailDialog = activePapaSeed
            }
          )
        }
      }

      // Editable & Undoable Recent Savings Transfers
      item(key = "gullak_activity", contentType = "activity") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          GullakActivityCard(
            drops = gullakDrops,
            plainEnglish = plainEnglish,
            onTransactionClick = { tx -> editingTransaction = tx }
          )
        }
      }
    }

    if (isCoinFlying) {
      FlyingCoinOverlay(
        startOffset = coinStartPos,
        targetOffset = coinTargetPos,
        amountText = lastAddedAmountText,
        amountRupees = lastAddedRupees,
        onAnimationFinished = { isCoinFlying = false }
      )
    }

    if (showCustomDepositDialog) {
      CustomGullakDepositDialog(
        plainEnglish = plainEnglish,
        onDismiss = { showCustomDepositDialog = false },
        onConfirm = { amountRupees ->
          showCustomDepositDialog = false
          triggerCoinDrop(amountRupees, Offset(300f, 920f))
        }
      )
    }

    if (showSetBalanceDialog) {
      SetGullakBalanceDialog(
        goal = goal,
        plainEnglish = plainEnglish,
        onDismiss = { showSetBalanceDialog = false },
        onConfirmBalanceRupees = { newRupees ->
          val prevPaise = goal.currentAmountPaise
          showSetBalanceDialog = false
          container.goalRepository.setGullakBalance(newRupees * 100L)
          clinkBannerMessage = if (newRupees == 0L) {
            "Reset ${goal.name} balance to ₹0."
          } else {
            "Set ${goal.name} balance to ${CurrencyFormatter.formatRupees(newRupees)}."
          }
          showAddCoinsSuccess = true
          undoController.recordAction(
            message = if (newRupees == 0L) {
              "Reset ${goal.name} to ₹0"
            } else {
              "Set ${goal.name} to ${CurrencyFormatter.formatRupees(newRupees)}"
            }
          ) {
            container.goalRepository.setGullakBalance(prevPaise)
            showAddCoinsSuccess = false
          }
        }
      )
    }

    if (showCreateGullakDialog) {
      CreateNewGullakDialog(
        plainEnglish = plainEnglish,
        onDismiss = { showCreateGullakDialog = false },
        onCreate = { newGoal ->
          val prevActiveId = goal.id
          showCreateGullakDialog = false
          container.goalRepository.createGoal(newGoal)
          clinkBannerMessage = "Created \"${newGoal.name}\" starting at ${CurrencyFormatter.formatRupees(newGoal.currentAmountRupees)}."
          showAddCoinsSuccess = true
          undoController.recordAction("Created \"${newGoal.name}\"") {
            container.goalRepository.deleteGoal(newGoal.id)
            container.goalRepository.selectGoal(prevActiveId)
            showAddCoinsSuccess = false
          }
        }
      )
    }

    if (showDipDialog) {
      DipFromGullakDialog(
        currentGullakRupees = goal.currentAmountRupees,
        plainEnglish = plainEnglish,
        onDismiss = { showDipDialog = false },
        onConfirm = { amountRupees, reason ->
          showDipDialog = false
          triggerGullakDip(amountRupees, reason)
        }
      )
    }

    if (showCalculatorDialog) {
      HisaabCalculatorDialog(
        onDismiss = { showCalculatorDialog = false },
        onJotExpense = { spendRupees ->
          showCalculatorDialog = false
          val spendTx = Transaction(
            amountPaise = spendRupees * 100L,
            direction = Direction.DEBIT,
            merchant = if (plainEnglish) "Calculated Expense" else "Calculated Khata Note",
            category = Category.OTHER,
            source = CaptureSource.MANUAL
          )
          container.transactionRepository.insert(spendTx)
          undoController.recordAction(
            message = "Logged ${CurrencyFormatter.formatRupees(spendRupees)}"
          ) {
            container.transactionRepository.delete(spendTx.id)
          }
        },
        onDropInGullak = { gullakRupees ->
          showCalculatorDialog = false
          triggerCoinDrop(gullakRupees, Offset(300f, 880f))
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
        onAskAuthor = {
          showChitthiDetailDialog = null
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

    if (showPapaGuidanceDialog) {
      PapaGuidanceDialog(
        container = container,
        userName = displayUserName,
        safeToSpend = safeToSpend,
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

    if (showMaaGuidanceDialog) {
      MaaGuidanceDialog(
        container = container,
        userName = displayUserName,
        safeToSpend = safeToSpend,
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

    editingTransaction?.let { oldTx ->
      EditTransactionDialog(
        transaction = oldTx,
        onDismiss = { editingTransaction = null },
        onSave = { updatedTx ->
          val prevGoalBalance = container.goalRepository.primaryGoalState.value.currentAmountPaise
          val delta = updatedTx.gullakImpactPaise() - oldTx.gullakImpactPaise()
          container.transactionRepository.update(updatedTx)
          if (delta != 0L) {
            container.goalRepository.adjustGullakDelta(delta)
          }
          editingTransaction = null
          undoController.recordAction("Updated \"${updatedTx.displayMerchant(plainEnglish)}\"") {
            container.transactionRepository.update(oldTx)
            if (delta != 0L) {
              container.goalRepository.setGullakBalance(prevGoalBalance)
            }
          }
        },
        onDelete = { txToDelete ->
          val prevGoalBalance = container.goalRepository.primaryGoalState.value.currentAmountPaise
          val delta = -txToDelete.gullakImpactPaise()
          container.transactionRepository.delete(txToDelete.id)
          if (delta != 0L) {
            container.goalRepository.adjustGullakDelta(delta)
          }
          editingTransaction = null
          undoController.recordAction("Deleted \"${txToDelete.displayMerchant(plainEnglish)}\"") {
            container.transactionRepository.insert(txToDelete)
            if (delta != 0L) {
              container.goalRepository.setGullakBalance(prevGoalBalance)
            }
          }
        }
      )
    }

    if (!hasParentUndo) {
      GharUndoSnackbarHost(
        controller = undoController,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 16.dp)
      )
    }
  }
}

@Composable
private fun MultiGullakShelfRow(
  goals: List<SavingsGoal>,
  activeGoalId: String,
  totalSavedRupees: Long,
  plainEnglish: Boolean,
  onSelectGoal: (String) -> Unit,
  onAddNewGullak: () -> Unit,
  onDeleteGoal: (SavingsGoal) -> Unit
) {
  var pendingDeleteGoal by remember { mutableStateOf<SavingsGoal?>(null) }
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (goals.size > 1) {
          "${GharTerms.savingsPotsPlural(plainEnglish)} (${goals.size} • ${CurrencyFormatter.formatCompactRupees(totalSavedRupees)} total)"
        } else {
          GharTerms.savingsPotsPlural(plainEnglish)
        },
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Text(
        text = if (plainEnglish) "+ New Pot" else "+ New Gullak",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = PastelSage,
        modifier = Modifier
          .clip(CircleShape)
          .clickable { onAddNewGullak() }
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .testTag("add_new_gullak_button")
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      goals.forEach { item ->
        val isSelected = item.id == activeGoalId
        Surface(
          onClick = { onSelectGoal(item.id) },
          shape = RoundedCornerShape(18.dp),
          color = if (isSelected) PastelSage.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surface,
          border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) PastelSage else MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
          ),
          modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag("gullak_shelf_chip_${item.id}")
        ) {
          Row(
            modifier = Modifier.padding(
              start = 14.dp,
              end = if (goals.size > 1 && isSelected) 6.dp else 14.dp,
              top = 8.dp,
              bottom = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Savings,
              contentDescription = null,
              tint = if (isSelected) PastelSage else ClayGullakColor,
              modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = item.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
              )
              Text(
                text = "${CurrencyFormatter.formatCompactRupees(item.currentAmountRupees)} / ${CurrencyFormatter.formatCompactRupees(item.targetAmountRupees)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
              )
            }
            if (goals.size > 1 && isSelected) {
              Spacer(modifier = Modifier.width(4.dp))
              IconButton(
                onClick = { pendingDeleteGoal = item },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("delete_gullak_button_${item.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Remove ${item.name}",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }
      }
    }
  }

  pendingDeleteGoal?.let { target ->
    val potLabel = GharTerms.savingsPotName(plainEnglish)
    GharConfirmDialog(
      title = "Delete \"${target.name}\"?",
      message = "This removes the \"${target.name}\" $potLabel (${CurrencyFormatter.formatRupees(target.currentAmountRupees)} saved). You can still undo right after confirming.",
      confirmLabel = "Delete Pot",
      onConfirm = {
        pendingDeleteGoal = null
        onDeleteGoal(target)
      },
      onDismiss = { pendingDeleteGoal = null },
      dialogTestTag = "confirm_delete_gullak_dialog",
      confirmTestTag = "confirm_delete_gullak_button"
    )
  }
}

@Composable
private fun UnspentBatuaSweepCard(
  unspentTodayRupees: Long,
  goalName: String,
  onSweepAmount: (Long, Offset) -> Unit
) {
  val sweepBtnRef = remember { MutableOffsetRef(Offset(300f, 760f)) }
  val gentleHalfRupees = ((unspentTodayRupees / 2L) / 10L * 10L).coerceAtLeast(20L).coerceAtMost(unspentTodayRupees)

  Surface(
    shape = PebbleCardShape,
    color = PastelSage.copy(alpha = 0.13f),
    border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.38f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("unspent_batua_sweep_card")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = PastelSage,
          modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Unspent Daily Allowance: ${CurrencyFormatter.formatRupees(unspentTodayRupees)}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f)
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Move part or all of today's unspent daily budget into $goalName:",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (gentleHalfRupees < unspentTodayRupees) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.45f)),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .clip(CircleShape)
              .clickable { onSweepAmount(gentleHalfRupees, sweepBtnRef.value) }
              .testTag("sweep_half_unspent_button")
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
              Text(
                text = "Save Half (${CurrencyFormatter.formatRupees(gentleHalfRupees)})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PastelSage,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
        val sageSweepContent = PastelSage.contrastingContentColor()
        Button(
          onClick = { onSweepAmount(unspentTodayRupees, sweepBtnRef.value) },
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = PastelSage,
            contentColor = sageSweepContent
          ),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .onPlaced { sweepBtnRef.coords = it }
            .testTag("sweep_full_unspent_button")
        ) {
          Text(
            text = "Sweep ${CurrencyFormatter.formatRupees(unspentTodayRupees)}",
            color = sageSweepContent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
private fun BhaiSillUtilityPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.dp, accent.copy(alpha = 0.36f)),
    modifier = modifier
      .defaultMinSize(minHeight = 48.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accent,
        modifier = Modifier.size(17.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun BhaiTargetPacePlannerCard(
  goal: SavingsGoal,
  plainEnglish: Boolean,
  dailySafeBatuaRupees: Long,
  onDropDailyShare: (Long, Offset) -> Unit,
  onOpenCalculator: () -> Unit
) {
  var selectedHorizon by remember { mutableStateOf(PaceHorizonOption.DAYS_90) }
  val dropButtonRef = remember { MutableOffsetRef(Offset(300f, 820f)) }

  val remainingRupees = (goal.targetAmountRupees - goal.currentAmountRupees).coerceAtLeast(0L)
  val dailyNeededRupees = remember(remainingRupees, selectedHorizon) {
    if (remainingRupees == 0L) 0L
    else ((remainingRupees + selectedHorizon.days - 1) / selectedHorizon.days).coerceAtLeast(10L)
  }
  val weeklyNeededRupees = dailyNeededRupees * 7L
  val fitsBatuaComfortably = dailySafeBatuaRupees <= 0L || dailyNeededRupees <= (dailySafeBatuaRupees * 0.45f).toLong().coerceAtLeast(100L)

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("bhai_pace_planner_card"),
    shape = PebbleCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, PastelHoney.copy(alpha = 0.36f))
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.Timeline,
            contentDescription = null,
            tint = PastelHoney,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (plainEnglish) "Savings Target Pace" else "Papa's Target Pace",
              style = MaterialTheme.typography.titleLarge,
              fontSize = 18.sp,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Daily & weekly amount required for ${goal.name}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        IconButton(
          onClick = onOpenCalculator,
          modifier = Modifier.size(48.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Calculate,
            contentDescription = "Open Calculator",
            tint = PastelHoney
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        PaceHorizonOption.entries.forEach { option ->
          val selected = option == selectedHorizon
          Surface(
            shape = CircleShape,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 42.dp)
              .clip(CircleShape)
              .clickable { selectedHorizon = option }
              .testTag("pace_horizon_${option.label}")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = option.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (remainingRupees == 0L) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = PastelSage.copy(alpha = 0.16f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "${goal.name} has reached 100% of its target. Add a new savings pot above or update your goal in Plan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(14.dp)
          )
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = PastelSage.copy(alpha = 0.14f),
            modifier = Modifier.weight(1f)
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "Daily Required",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "${CurrencyFormatter.formatRupees(dailyNeededRupees)}/day",
                style = GharHandStyles.moneyMedium,
                color = PastelSage,
                maxLines = 1
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(18.dp),
            color = PastelHoney.copy(alpha = 0.16f),
            modifier = Modifier.weight(1f)
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "Weekly Required",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "${CurrencyFormatter.formatCompactRupees(weeklyNeededRupees)}/wk",
                style = GharHandStyles.moneyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = if (fitsBatuaComfortably) {
            "Fits comfortably within your ${CurrencyFormatter.formatRupees(dailySafeBatuaRupees)}/day safe-to-spend allowance."
          } else {
            "Exceeds 45% of your ${CurrencyFormatter.formatRupees(dailySafeBatuaRupees)}/day allowance — consider 6mo or 1yr for a lighter daily amount."
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        val sageContent = PastelSage.contrastingContentColor()
        Button(
          onClick = { onDropDailyShare(dailyNeededRupees, dropButtonRef.value) },
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = PastelSage,
            contentColor = sageContent
          ),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .onPlaced { dropButtonRef.coords = it }
            .testTag("drop_daily_share_button")
        ) {
          Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = null,
            tint = sageContent,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Save Today's Share (${CurrencyFormatter.formatRupees(dailyNeededRupees)})",
            color = sageContent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
private fun GullakHeroCard(
  goal: SavingsGoal,
  profile: BudgetProfile,
  dailySafeAllowanceRupees: Long,
  unspentTodayRupees: Long,
  plainEnglish: Boolean,
  isGullakSquished: Boolean,
  gullakSlotRef: MutableOffsetRef,
  onDropCoin: (Long, Offset) -> Unit,
  onOpenCustomDeposit: () -> Unit,
  onOpenSetBalance: () -> Unit
) {
  val gullakScaleXState = animateFloatAsState(
    targetValue = if (isGullakSquished) 1.14f else 1.0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
    label = "gullakScaleX"
  )
  val gullakScaleYState = animateFloatAsState(
    targetValue = if (isGullakSquished) 0.88f else 1.0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
    label = "gullakScaleY"
  )
  val btnRef1 = remember { MutableOffsetRef() }
  val btnRef2 = remember { MutableOffsetRef() }
  val btnRef3 = remember { MutableOffsetRef() }
  val btnRef4 = remember { MutableOffsetRef() }
  val btnRefs = remember { listOf(btnRef1, btnRef2, btnRef3, btnRef4) }

  val adaptiveCoins = remember(profile, goal, dailySafeAllowanceRupees, unspentTodayRupees, plainEnglish) {
    computeAdaptiveGullakCoins(profile, goal, dailySafeAllowanceRupees, unspentTodayRupees, plainEnglish)
  }

  val cardBgBrush = remember {
    Brush.verticalGradient(
      colors = listOf(
        PastelHoney.copy(alpha = 0.08f),
        Color.Transparent,
        PastelSage.copy(alpha = 0.08f)
      )
    )
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("gullak_hero_card"),
    shape = JharokhaCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, PastelTerracotta.copy(alpha = 0.25f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(cardBgBrush)
        .jharokhaCornerFiligree(accentColor = PastelSage)
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          modifier = Modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(CircleShape)
            .clickable { onOpenSetBalance() }
            .testTag("adjust_gullak_balance_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Set or reset balance",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Set / Zero Balance",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      Box(
        modifier = Modifier
          .graphicsLayer {
            scaleX = gullakScaleXState.value
            scaleY = gullakScaleYState.value
          }
          .onPlaced { gullakSlotRef.coords = it },
        contentAlignment = Alignment.Center
      ) {
        GullakSparkleOverlay(size = 162.dp)
        GullakCanvas(size = 146.dp)
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = if (goal.currentAmountRupees == 0L) {
          "Current balance for ${goal.name}"
        } else {
          "Saved for ${goal.name}"
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      AnimatedContent(
        targetState = goal.currentAmountRupees,
        transitionSpec = {
          if (targetState > initialState) {
            slideInVertically { height -> height } + fadeIn() togetherWith
              slideOutVertically { height -> -height } + fadeOut()
          } else {
            slideInVertically { height -> -height } + fadeIn() togetherWith
              slideOutVertically { height -> height } + fadeOut()
          }
        },
        label = "gullakAmountRoll"
      ) { amount ->
        Text(
          text = CurrencyFormatter.formatRupees(amount),
          style = GharHandStyles.moneyHero,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          modifier = Modifier.testTag("gullak_current_balance_text")
        )
      }

      if (goal.currentAmountRupees == 0L) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Starts at ₹0 — every rupee shown here is money you explicitly saved.",
          style = MaterialTheme.typography.labelSmall,
          color = PastelSage,
          textAlign = TextAlign.Center
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      SteppingStoneGoalTrail(goal = goal)

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Tap an amount below to add to savings (scaled to your budget):",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(12.dp))

      val coinRings = remember { listOf(PastelHoney, ClayGullakColor, PastelTerracotta, PastelSage) }
      val coinFills = remember { listOf(PastelHoney, PastelTerracotta, GoldCoin, PastelSage) }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        adaptiveCoins.forEachIndexed { i, coin ->
          val posRef = btnRefs.getOrElse(i) { btnRef1 }
          TactileCoinMedallion(
            amountLabel = coin.amountLabel,
            caption = coin.caption,
            ringColor = coinRings.getOrElse(i) { PastelHoney },
            fillColor = coinFills.getOrElse(i) { GoldCoin },
            denominationRupees = coin.amountRupees,
            positionRef = posRef,
            onClick = { onDropCoin(coin.amountRupees, posRef.value) },
            modifier = Modifier.testTag(coin.testTag)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Surface(
        shape = CircleShape,
        color = PastelHoney.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, PastelHoney.copy(alpha = 0.45f)),
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .clip(CircleShape)
          .clickable { onOpenCustomDeposit() }
          .testTag("open_custom_gullak_deposit_button")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (plainEnglish) "Add custom amount" else "Slip in a custom envelope",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
          )
        }
      }
    }
  }
}

@Composable
private fun SteppingStoneGoalTrail(goal: SavingsGoal) {
  val totalStones = 10
  val activeStones = if (goal.currentAmountPaise <= 0L) {
    0
  } else {
    ((goal.progressPercent / 100f) * totalStones).toInt().coerceIn(1, totalStones)
  }
  val leftRupees = (goal.targetAmountRupees - goal.currentAmountRupees).coerceAtLeast(0L)

  val milestoneLabel = when {
    goal.currentAmountPaise <= 0L -> "0% Saved • Ready to Start"
    goal.progressPercent >= 100 -> "100% Target Reached!"
    goal.progressPercent >= 75 -> "75% Milestone Passed"
    goal.progressPercent >= 50 -> "50% Halfway Mark Passed"
    goal.progressPercent >= 25 -> "25% Milestone Passed"
    else -> "${goal.progressPercent.toInt()}% of Target Saved"
  }

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = PastelSage.copy(alpha = 0.11f),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = milestoneLabel,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (leftRupees == 0L) "Target Reached!" else "${CurrencyFormatter.formatCompactRupees(leftRupees)} remaining",
          style = GharHandStyles.moneySmall,
          color = PastelSage,
          maxLines = 1
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(14.dp)
      ) {
        val gap = 6.dp.toPx()
        val stoneWidth = (size.width - gap * (totalStones - 1)) / totalStones
        for (i in 0 until totalStones) {
          val isFilled = i < activeStones
          val color = when {
            !isFilled -> PastelSage.copy(alpha = 0.18f)
            i == activeStones - 1 -> GoldCoin
            else -> PastelSage
          }
          val h = if (i % 2 == 0) size.height else size.height * 0.80f
          val yOffset = (size.height - h) / 2f
          drawRoundRect(
            color = color,
            topLeft = Offset(i * (stoneWidth + gap), yOffset),
            size = Size(stoneWidth, h),
            cornerRadius = CornerRadius(h / 2f, h / 2f)
          )
        }
      }
    }
  }
}

@Composable
private fun ReinvestmentCard(goal: SavingsGoal) {
  val instrument = goal.instrumentType
  val basisRupees = if (goal.currentAmountRupees > 0L) goal.currentAmountRupees else goal.targetAmountRupees.coerceAtLeast(1000L)
  val (milestone1Yr, milestone3Yr, milestone5Yr) = remember(basisRupees, instrument) {
    val r = instrument.typicalAnnualReturnPercent / 100.0
    val f1 = CurrencyFormatter.formatCompactRupees((basisRupees * (1.0 + r).pow(1)).toLong())
    val f3 = CurrencyFormatter.formatCompactRupees((basisRupees * (1.0 + r).pow(3)).toLong())
    val f5 = CurrencyFormatter.formatCompactRupees((basisRupees * (1.0 + r).pow(5)).toLong())
    Triple(f1, f3, f5)
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("reinvestment_card"),
    shape = PebbleCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.28f))
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.Spa,
            contentDescription = null,
            tint = PastelSage,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Compounding Growth",
              style = MaterialTheme.typography.titleLarge,
              fontSize = 18.sp,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (goal.currentAmountRupees > 0L) {
                "Projected growth of saved ${CurrencyFormatter.formatCompactRupees(goal.currentAmountRupees)} (${instrument.typicalAnnualReturnPercent}% p.a.)"
              } else {
                "Projected growth of ${CurrencyFormatter.formatCompactRupees(basisRupees)} target (${instrument.typicalAnnualReturnPercent}% p.a.)"
              },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          shape = CircleShape,
          color = PastelSage.copy(alpha = 0.16f)
        ) {
          Text(
            text = instrument.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = PastelSage,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      BotanicalCompoundingVine(
        milestone1Yr = milestone1Yr,
        milestone3Yr = milestone3Yr,
        milestone5Yr = milestone5Yr
      )
    }
  }
}

@Composable
private fun GullakActivityCard(
  drops: List<Transaction>,
  plainEnglish: Boolean,
  onTransactionClick: (Transaction) -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("gullak_activity_card"),
    shape = PebbleCardShape,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (plainEnglish) "Recent Savings Transfers" else "Recent Gullak Activity",
            style = MaterialTheme.typography.titleLarge,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Tap any entry to edit or delete",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Text(
          text = "${drops.size} entries",
          style = GharHandStyles.stickyNoteMeta,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (drops.isEmpty()) {
        Text(
          text = "No savings transfers yet. Tap an amount above to make your first deposit.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      } else {
        val recentDrops = remember(drops) { drops.take(10) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          recentDrops.forEach { drop ->
            androidx.compose.runtime.key(drop.id) {
            val isDeposit = drop.isExpense
            Surface(
              onClick = { onTransactionClick(drop) },
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
              modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .testTag("gullak_tx_item_${drop.id}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Box(
                    modifier = Modifier
                      .size(34.dp)
                      .clip(CircleShape)
                      .background(
                        if (isDeposit) GoldCoin.copy(alpha = 0.22f)
                        else PastelTerracotta.copy(alpha = 0.20f)
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "₹",
                      style = GharHandStyles.moneySmall,
                      color = if (isDeposit) ClayGullakColor else PastelTerracotta
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = drop.displayMerchant(plainEnglish),
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = DateUtils.formatRelativeDate(drop.timestamp),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = if (isDeposit) {
                      "+${CurrencyFormatter.formatRupees(drop.amountRupees)}"
                    } else {
                      "−${CurrencyFormatter.formatRupees(drop.amountRupees)}"
                    },
                    style = GharHandStyles.moneyMedium,
                    color = if (isDeposit) PastelSage else PastelTerracotta,
                    maxLines = 1
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(15.dp)
                  )
                }
              }
            }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SetGullakBalanceDialog(
  goal: SavingsGoal,
  plainEnglish: Boolean,
  onDismiss: () -> Unit,
  onConfirmBalanceRupees: (Long) -> Unit
) {
  var balanceText by remember(goal.id) { mutableStateOf(goal.currentAmountRupees.toString()) }
  var showZeroConfirm by remember { mutableStateOf(false) }
  val potTerm = GharTerms.savingsPotName(plainEnglish)

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = JharokhaCardShape,
    title = {
      Text("Adjust ${goal.name} Balance", style = MaterialTheme.typography.titleLarge)
    },
    text = {
      Column {
        Text(
          text = "Set the exact amount you currently have saved in this $potTerm, or reset it to ₹0.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = balanceText,
          onValueChange = { balanceText = it.filter { c -> c.isDigit() } },
          label = { Text("Actual Saved Balance (₹)") },
          prefix = {
            Text("₹ ", style = GharHandStyles.moneyMedium, color = PastelSage)
          },
          textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("set_gullak_balance_input")
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
          onClick = {
            if (goal.currentAmountPaise > 0L) {
              showZeroConfirm = true
            } else {
              onConfirmBalanceRupees(0L)
            }
          },
          modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag("zero_gullak_balance_button")
        ) {
          Text(
            text = "Reset Balance to ₹0",
            color = PastelTerracotta,
            fontWeight = FontWeight.Bold
          )
        }
      }
    },
    confirmButton = {
      val sageContent = PastelSage.contrastingContentColor()
      Button(
        onClick = {
          val amt = balanceText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
          onConfirmBalanceRupees(amt)
        },
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag("confirm_set_gullak_balance_button"),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = PastelSage,
          contentColor = sageContent
        )
      ) {
        Text("Save Balance", color = sageContent, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
      ) {
        Text("Cancel")
      }
    }
  )

  if (showZeroConfirm) {
    GharConfirmDialog(
      title = "Reset ${goal.name} to ₹0?",
      message = "This sets your \"${goal.name}\" $potTerm balance from ${CurrencyFormatter.formatRupees(goal.currentAmountRupees)} to ₹0.",
      confirmLabel = "Reset to ₹0",
      onConfirm = {
        showZeroConfirm = false
        onConfirmBalanceRupees(0L)
      },
      onDismiss = { showZeroConfirm = false },
      dialogTestTag = "confirm_zero_gullak_dialog",
      confirmTestTag = "confirm_zero_gullak_button"
    )
  }
}

@Composable
private fun CreateNewGullakDialog(
  plainEnglish: Boolean,
  onDismiss: () -> Unit,
  onCreate: (SavingsGoal) -> Unit
) {
  var nameText by remember { mutableStateOf("") }
  var targetText by remember { mutableStateOf("25000") }
  var openingText by remember { mutableStateOf("") }
  val potTerm = GharTerms.savingsPotName(plainEnglish)

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = JharokhaCardShape,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Savings,
          contentDescription = null,
          tint = PastelSage,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create New $potTerm", style = MaterialTheme.typography.titleLarge)
      }
    },
    text = {
      Column {
        Text(
          text = "Keep separate pots for trips, gifts, or an emergency fund. Starts at ₹0 by default.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = nameText,
          onValueChange = { nameText = it.take(40) },
          label = {
            Text(
              if (plainEnglish) "Goal Name (e.g. Emergency Fund)" else "Goal Name (e.g. Diwali Gifts)"
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_gullak_name_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = targetText,
          onValueChange = { targetText = it.filter { c -> c.isDigit() } },
          label = { Text("Target Amount (₹)") },
          prefix = { Text("₹ ", style = GharHandStyles.moneyMedium, color = MaterialTheme.colorScheme.primary) },
          textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_gullak_target_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = openingText,
          onValueChange = { openingText = it.filter { c -> c.isDigit() } },
          label = { Text("Opening Balance (Optional — defaults to ₹0)") },
          placeholder = { Text("0") },
          prefix = { Text("₹ ", style = GharHandStyles.moneyMedium, color = PastelSage) },
          textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_gullak_opening_input")
        )
      }
    },
    confirmButton = {
      val sageContent = PastelSage.contrastingContentColor()
      Button(
        onClick = {
          val cleanName = nameText.trim().ifBlank { "New $potTerm" }
          val targetRupees = targetText.toLongOrNull()?.coerceAtLeast(500L) ?: 10000L
          val openingRupees = openingText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
          val goal = SavingsGoal(
            id = "goal_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            targetAmountPaise = targetRupees * 100L,
            currentAmountPaise = openingRupees * 100L,
            instrumentType = InstrumentType.SIP
          )
          onCreate(goal)
        },
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag("confirm_create_gullak_button"),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = PastelSage,
          contentColor = sageContent
        )
      ) {
        Text("Create Pot", color = sageContent, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
      ) {
        Text("Cancel")
      }
    }
  )
}

@Composable
private fun CustomGullakDepositDialog(
  plainEnglish: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (Long) -> Unit
) {
  var amountText by remember { mutableStateOf("250") }
  var showCalc by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = JharokhaCardShape,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Savings,
          contentDescription = null,
          tint = PastelSage,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (plainEnglish) "Add Custom Savings" else "Slip an Envelope In",
          style = MaterialTheme.typography.titleLarge
        )
      }
    },
    text = {
      Column {
        Text(
          text = if (plainEnglish) {
            "Enter the amount you want to transfer into your savings pot."
          } else {
            "Add spare cash or bonus money into your Gullak."
          },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { c -> c.isDigit() } },
          label = { Text("Amount (₹)") },
          trailingIcon = {
            IconButton(onClick = { showCalc = true }) {
              Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = "Calculate amount",
                tint = PastelHoney
              )
            }
          },
          textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountText.toLongOrNull()?.coerceAtLeast(10L) ?: 100L
          onConfirm(amt)
        },
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) {
        Text("Add to Savings", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
      ) {
        Text("Cancel")
      }
    }
  )

  if (showCalc) {
    HisaabCalculatorDialog(
      initialAmountRupees = amountText.toLongOrNull(),
      onDismiss = { showCalc = false },
      onUseAmount = { calculated ->
        amountText = calculated.toString()
        showCalc = false
      }
    )
  }
}

@Composable
private fun DipFromGullakDialog(
  currentGullakRupees: Long,
  plainEnglish: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (Long, String) -> Unit
) {
  var amountText by remember { mutableStateOf("200") }
  var reasonText by remember { mutableStateOf("") }
  val walletTerm = GharTerms.walletName(plainEnglish)

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = JharokhaCardShape,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Undo,
          contentDescription = null,
          tint = PastelTerracotta,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (plainEnglish) "Withdraw from Savings" else "Dip Into Gullak",
          style = MaterialTheme.typography.titleLarge
        )
      }
    },
    text = {
      Column {
        Text(
          text = "Move funds back to your $walletTerm. Current saved balance: ${CurrencyFormatter.formatRupees(currentGullakRupees)}.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { c -> c.isDigit() } },
          label = { Text("Amount to move to $walletTerm (₹)") },
          textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("gullak_dip_amount_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = reasonText,
          onValueChange = { reasonText = it.take(50) },
          label = { Text("Note (optional, e.g. Train ticket)") },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      val withdrawContent = PastelTerracotta.contrastingContentColor()
      Button(
        onClick = {
          val amt = amountText.toLongOrNull()?.coerceAtLeast(1L) ?: 0L
          if (amt > 0L) onConfirm(amt, reasonText.trim())
        },
        enabled = (amountText.toLongOrNull() ?: 0L) > 0L,
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag("confirm_gullak_dip_button"),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = PastelTerracotta,
          contentColor = withdrawContent
        )
      ) {
        Text("Withdraw to $walletTerm", color = withdrawContent, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
      ) {
        Text("Cancel")
      }
    }
  )
}

@Composable
private fun SitWithPapaRoomCard(
  plainEnglish: Boolean,
  onOpenPapaChat: () -> Unit
) {
  Surface(
    onClick = onOpenPapaChat,
    shape = JharokhaCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.2.dp, PastelSage.copy(alpha = 0.45f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("open_papa_guidance_button")
  ) {
    Row(
      modifier = Modifier
        .jharokhaCornerFiligree(accentColor = PastelSage)
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      PapaAvatar(size = 46.dp)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = GharTerms.chatWithPapaLabel(plainEnglish),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (plainEnglish) {
            "Investments, live FD/PPF rates, inflation insights & allocating big amounts"
          } else {
            "Investments, live FD/PPF rates, real-world insights & saving big amounts"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      val sageCtaContent = PastelSage.contrastingContentColor()
      Surface(
        shape = CircleShape,
        color = PastelSage
      ) {
        Text(
          text = if (plainEnglish) "Ask Dad" else "Chat",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = sageCtaContent,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
      }
    }
  }
}

internal data class AdaptiveCoinPreset(
  val amountRupees: Long,
  val amountLabel: String,
  val caption: String,
  val testTag: String
)

internal fun computeAdaptiveGullakCoins(
  profile: BudgetProfile,
  goal: SavingsGoal,
  dailySafeAllowanceRupees: Long,
  unspentTodayRupees: Long,
  plainEnglish: Boolean
): List<AdaptiveCoinPreset> {
  val monthlyIncome = profile.monthlyIncomeRupees.coerceAtLeast(3_000L)
  val fixedCommitments = profile.fixedCommitmentsRupees.coerceAtLeast(0L)
  val disposableMonthly = (monthlyIncome - fixedCommitments).coerceAtLeast(monthlyIncome / 4L).coerceAtLeast(1_200L)

  val effectiveDaily = when {
    unspentTodayRupees > 0L && dailySafeAllowanceRupees > 0L -> ((dailySafeAllowanceRupees * 2L + unspentTodayRupees) / 3L)
    dailySafeAllowanceRupees > 0L -> dailySafeAllowanceRupees
    else -> (disposableMonthly / 30L).coerceAtLeast(40L)
  }

  val remainingGoal = (goal.targetAmountRupees - goal.currentAmountRupees).coerceAtLeast(goal.targetAmountRupees / 5L).coerceAtLeast(500L)
  val goalDailyPace = (remainingGoal / 90L).coerceAtLeast(10L)

  val maxAffordableCoin = (disposableMonthly / 3L)
    .coerceAtMost(profile.savingsTargetRupees.coerceAtLeast(2_000L) / 2L)
    .coerceAtLeast(100L)

  val ladder = longArrayOf(10L, 20L, 50L, 100L, 200L, 250L, 500L, 1_000L, 2_000L, 2_500L, 5_000L, 10_000L, 25_000L)

  val target1 = (effectiveDaily * 0.22).toLong().coerceIn(10L, maxAffordableCoin / 4L)
  val target2 = (effectiveDaily * 0.55).toLong().coerceIn(target1 * 2L, maxAffordableCoin / 2L)
  val target3 = (effectiveDaily * 1.30 + goalDailyPace * 0.35).toLong().coerceIn(target2 + 10L, maxAffordableCoin)
  val target4 = (effectiveDaily * 3.50 + goalDailyPace * 1.20).toLong().coerceIn(target3 + 20L, maxAffordableCoin)

  fun snapNearestLadder(target: Long, floor: Long): Long {
    var best = ladder.first()
    var bestDiff = Long.MAX_VALUE
    for (v in ladder) {
      if (v <= floor) continue
      val diff = Math.abs(v - target)
      if (diff < bestDiff) {
        bestDiff = diff
        best = v
      }
    }
    return best.coerceAtLeast(floor + 5L)
  }

  val c1 = snapNearestLadder(target1, 0L)
  val c2 = snapNearestLadder(target2, c1)
  val c3 = snapNearestLadder(target3, c2)
  val c4 = snapNearestLadder(target4, c3)

  val rawCoins = listOf(c1, c2, c3, c4).distinct()
  val coins = if (rawCoins.size == 4) {
    rawCoins
  } else {
    val result = mutableListOf<Long>()
    for (v in ladder) {
      if (v <= maxAffordableCoin) {
        result.add(v)
      }
    }
    if (result.size < 4) {
      listOf(10L, 20L, 50L, 100L)
    } else {
      result.take(4)
    }
  }

  val captions = if (plainEnglish) {
    listOf("Micro", "Spare", "Goal Pace", "Big Drop")
  } else {
    listOf("Chai Coin", "Spare Coin", "Goal Pace", "Shagun Drop")
  }

  val testTags = listOf("feed_gullak_50", "feed_gullak_100", "feed_gullak_500", "feed_gullak_1000")

  return coins.mapIndexed { idx, valRupees ->
    AdaptiveCoinPreset(
      amountRupees = valRupees,
      amountLabel = CurrencyFormatter.formatCompactRupees(valRupees),
      caption = captions.getOrElse(idx) { "Savings" },
      testTag = testTags.getOrElse(idx) { "feed_gullak_coin_$idx" }
    )
  }
}

