package com.example.ui.livingroom

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.core.engine.HouseholdAnalyticsEngine
import com.example.core.engine.SafeToSpendCalculator
import com.example.core.engine.SafeToSpendResult
import com.example.core.localization.GharTerms
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.model.BudgetProfile
import com.example.core.model.CaptureSource
import com.example.core.model.Category
import com.example.core.model.Direction
import com.example.core.model.PakkaCommitment
import com.example.core.model.Transaction
import com.example.core.util.CurrencyFormatter
import com.example.core.util.DateUtils
import com.example.ui.common.CategoryIcon
import com.example.ui.common.GharUndoSnackbarHost
import com.example.ui.common.IllustratedRoomBanner
import com.example.ui.common.JharokhaCardShape
import com.example.ui.common.LocalGharUndoController
import com.example.ui.common.PakkaCommitmentEditorDialog
import com.example.ui.common.PakkeCommitmentsCard
import com.example.ui.common.PakkeCommitmentsDialog
import com.example.ui.common.PebbleCardShape
import com.example.ui.common.RangoliBrassOrnament
import com.example.ui.common.jharokhaCornerFiligree
import com.example.ui.common.pastelHomeBackdrop
import com.example.ui.common.rememberGharUndoController
import com.example.ui.kitchen.EditTransactionDialog
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelPlum
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelSky
import com.example.ui.theme.PastelTerracotta

private enum class CourtyardPeriod(val label: String) {
  THIS_CYCLE("This Cycle"),
  DAYS_7("Last 7d"),
  ALL_TIME("All Time")
}

@Composable
fun LivingRoomScreen(
  container: AppContainer,
  listState: LazyListState = rememberLazyListState(),
  onShowRoomGuide: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val plainEnglish = LocalPlainEnglishMode.current
  val context = LocalContext.current
  val hasParentUndo = LocalGharUndoController.current != null
  val fallbackUndo = rememberGharUndoController()
  val undoController = LocalGharUndoController.current ?: fallbackUndo

  val transactions by container.transactionRepository.transactionsState.collectAsStateWithLifecycle()
  val profile by container.budgetRepository.profileState.collectAsStateWithLifecycle()
  val goal by container.goalRepository.primaryGoalState.collectAsStateWithLifecycle()
  val allGoals by container.goalRepository.allGoalsState.collectAsStateWithLifecycle()
  val commitments by container.commitmentRepository.commitmentsState.collectAsStateWithLifecycle()
  val userName by container.budgetRepository.userNameState.collectAsStateWithLifecycle()

  var selectedPeriod by remember { mutableStateOf(CourtyardPeriod.THIS_CYCLE) }
  var expandedCategory by remember { mutableStateOf<Category?>(null) }
  var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
  var showAddCommitmentEditor by remember { mutableStateOf(false) }
  var editingCommitment by remember { mutableStateOf<PakkaCommitment?>(null) }
  var showFullCommitmentsDialog by remember { mutableStateOf(false) }

  val spentSoFarPaise = remember(transactions) {
    transactions.filter { it.isExpense }.sumOf { it.amountPaise }
  }
  val creditIncomePaise = remember(transactions) {
    transactions.filter { !it.isExpense }.sumOf { it.amountPaise }
  }
  val safeToSpend = remember(profile, spentSoFarPaise, creditIncomePaise) {
    SafeToSpendCalculator.calculate(profile, spentSoFarPaise, creditIncomePaise)
  }

  val periodTransactions = remember(transactions, selectedPeriod, profile.monthStartDay) {
    val cutoff = when (selectedPeriod) {
      CourtyardPeriod.THIS_CYCLE -> DateUtils.startOfBudgetCycleMillis(profile.monthStartDay)
      CourtyardPeriod.DAYS_7 -> DateUtils.daysAgoMillis(6)
      CourtyardPeriod.ALL_TIME -> 0L
    }
    transactions.filter { it.timestamp >= cutoff }
  }

  val periodSpentPaise = remember(periodTransactions) {
    periodTransactions.filter { it.isExpense && !it.isGullakTransfer() }.sumOf { it.amountPaise }
  }

  val sevenDayBars = remember(transactions, safeToSpend.dailyAllowanceRupees) {
    HouseholdAnalyticsEngine.buildSevenDayRhythm(
      transactions = transactions,
      dailySafeRupees = safeToSpend.dailyAllowanceRupees
    )
  }

  val commitmentsSummary = remember(commitments, transactions, profile.monthStartDay) {
    HouseholdAnalyticsEngine.evaluateCommitments(
      commitments = commitments,
      transactions = transactions,
      monthStartDay = profile.monthStartDay
    )
  }

  val categoryTotals = remember(periodTransactions) {
    val map = mutableMapOf<Category, Long>()
    periodTransactions.filter { it.isExpense }.forEach { tx ->
      map[tx.category] = (map[tx.category] ?: 0L) + tx.amountPaise
    }
    map.toList().sortedByDescending { it.second }
  }

  fun shareMonthlySummary() {
    val summaryText = HouseholdAnalyticsEngine.buildShareableMonthlySummary(
      userName = userName,
      profile = profile,
      goal = goal,
      goals = allGoals,
      safeToSpend = safeToSpend,
      transactions = transactions,
      plainEnglish = plainEnglish
    )
    val subject = if (plainEnglish) "Monthly Budget Summary" else "Ghar Monthly Summary"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, subject)
      putExtra(Intent.EXTRA_TEXT, summaryText)
    }
    runCatching {
      context.startActivity(
        Intent.createChooser(sendIntent, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .pastelHomeBackdrop(
        primaryWash = PastelSky,
        secondaryWash = PastelTerracotta,
        tertiaryWash = PastelHoney
      )
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item(key = "courtyard_banner", contentType = "banner") {
        val monthYearLabel = remember { DateUtils.formatMonthYear() }
        IllustratedRoomBanner(
          artworkRes = R.drawable.img_living_courtyard_1792931331737,
          roomBadge = if (plainEnglish) "Insights • $monthYearLabel" else "Courtyard • $monthYearLabel",
          title = "Monthly Overview",
          keyHighlight = "${CurrencyFormatter.formatRupees(periodSpentPaise / 100L)} spent • ${safeToSpend.daysRemaining} days left in cycle",
          accentColor = PastelSky,
          heroHeight = 292.dp,
          scrollOffsetProvider = {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 600
          },
          onHelpClick = onShowRoomGuide
        )
      }

      // Period Filter + 1-Tap Share Monthly Summary Strip
      item(key = "courtyard_controls", contentType = "controls") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          CourtyardPeriod.entries.forEach { period ->
            val selected = period == selectedPeriod
            Surface(
              shape = CircleShape,
              color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
              border = BorderStroke(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
              ),
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 42.dp)
                .clip(CircleShape)
                .clickable { selectedPeriod = period }
                .testTag("courtyard_period_${period.name.lowercase()}")
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                Text(
                  text = period.label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1
                )
              }
            }
          }

          Surface(
            shape = CircleShape,
            color = PastelSky.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.45f)),
            modifier = Modifier
              .defaultMinSize(minHeight = 42.dp)
              .clip(CircleShape)
              .clickable { shareMonthlySummary() }
              .testTag("share_monthly_hisaab_button")
            ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = GharTerms.monthlyReportButton(plainEnglish),
                tint = PastelSky,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Share",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PastelSky
              )
            }
          }
        }
      }

      // Monthly Cycle Progress & Month-End Projection Card
      item(key = "sun_arc_card", contentType = "sun_arc") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          SunArcRhythmCard(safeToSpend = safeToSpend)
        }
      }

      // 7-Day Spend vs Daily Limit Bar Chart
      item(key = "seven_day_chart", contentType = "chart") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          SevenDayRhythmCard(
            bars = sevenDayBars,
            dailySafeRupees = safeToSpend.dailyAllowanceRupees
          )
        }
      }

      // Customizable Monthly Bills, Payments, Subscriptions & Receivables Card
      item(key = "pakke_bills_card", contentType = "bills") {
        Box(
          modifier = Modifier
            .padding(horizontal = 16.dp)
            .testTag("pakke_bills_card")
        ) {
          PakkeCommitmentsCard(
            summary = commitmentsSummary,
            plainEnglish = plainEnglish,
            onAddCommitment = { showAddCommitmentEditor = true },
            onEditCommitment = { editingCommitment = it },
            onSettleCommitment = { item ->
              val tx = HouseholdAnalyticsEngine.buildCommitmentSettlementTransaction(item)
              container.transactionRepository.insert(tx)
              val verb = if (item.isReceivable) "Received" else "Paid"
              undoController.recordAction(
                message = "$verb \"${item.displayTitle(plainEnglish)}\" (${CurrencyFormatter.formatRupees(item.amountRupees)})"
              ) {
                container.transactionRepository.delete(tx.id)
              }
            },
            onUnsettleCommitment = { item, matchedTx ->
              container.transactionRepository.delete(matchedTx.id)
              undoController.recordAction(
                message = "Unmarked \"${item.displayTitle(plainEnglish)}\" for this cycle"
              ) {
                container.transactionRepository.insert(matchedTx)
              }
            },
            onOpenFullManager = { showFullCommitmentsDialog = true }
          )
        }
      }

      // Monthly Budget Split Card
      item(key = "tapestry_card", contentType = "tapestry") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          HandloomTapestryCard(
            profile = profile,
            plainEnglish = plainEnglish
          )
        }
      }

      item(key = "petal_header", contentType = "header") {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          RangoliBrassOrnament(
            accentColor = PastelSky,
            modifier = Modifier.padding(bottom = 10.dp)
          )
          Text(
            text = "Spending by Category",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Tap any category row to inspect, edit, or delete entries",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (categoryTotals.isEmpty()) {
        item(key = "empty_petals", contentType = "empty") {
          Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Surface(
              shape = PebbleCardShape,
              color = PastelSky.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "No expenses recorded for ${selectedPeriod.label.lowercase()} yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp)
              )
            }
          }
        }
      } else {
        item(key = "category_breakdown", contentType = "petals") {
          Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Surface(
              shape = JharokhaCardShape,
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 1.dp,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("category_breakdown_card")
            ) {
              val totalAllCategoriesRupees = remember(categoryTotals) {
                (categoryTotals.sumOf { it.second } / 100L).coerceAtLeast(1L)
              }
              Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                categoryTotals.forEach { (cat, amountPaise) ->
                  androidx.compose.runtime.key(cat) {
                    val amountRupees = amountPaise / 100L
                    val percent = (amountRupees.toFloat() / totalAllCategoriesRupees.toFloat()).coerceIn(0.08f, 1f)
                    val isExpanded = expandedCategory == cat
                    val matchingTransactions = remember(periodTransactions, cat) {
                      periodTransactions.filter { it.category == cat }
                        .sortedByDescending { it.timestamp }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      WatercolorCategoryPetal(
                        category = cat,
                        plainEnglish = plainEnglish,
                        amountRupees = amountRupees,
                        shareFraction = percent,
                        isExpanded = isExpanded,
                        onClick = {
                          expandedCategory = if (isExpanded) null else cat
                        }
                      )

                    AnimatedVisibility(visible = isExpanded) {
                      Column(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(start = 8.dp, end = 4.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        matchingTransactions.forEach { tx ->
                          androidx.compose.runtime.key(tx.id) {
                            val shownMerchant = tx.displayMerchant(plainEnglish)
                            Surface(
                              onClick = { editingTransaction = tx },
                              shape = RoundedCornerShape(14.dp),
                              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                              modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("courtyard_tx_item_${tx.id}")
                            ) {
                              Row(
                                modifier = Modifier
                                  .fillMaxWidth()
                                  .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                              ) {
                                Column(modifier = Modifier.weight(1f)) {
                                  Text(
                                    text = shownMerchant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                  )
                                  Text(
                                    text = DateUtils.formatRelativeDate(tx.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                  )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                  Text(
                                    text = if (tx.isExpense) {
                                      CurrencyFormatter.formatRupees(tx.amountRupees)
                                    } else {
                                      "+${CurrencyFormatter.formatRupees(tx.amountRupees)}"
                                    },
                                    style = GharHandStyles.moneySmall,
                                    color = if (tx.isExpense) MaterialTheme.colorScheme.onSurface else PastelSage
                                  )
                                  Spacer(modifier = Modifier.width(6.dp))
                                  Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit $shownMerchant",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
              }
            }
          }
        }
      }
    }

    if (showAddCommitmentEditor || editingCommitment != null) {
      val currentTarget = editingCommitment
      PakkaCommitmentEditorDialog(
        existing = currentTarget,
        plainEnglish = plainEnglish,
        onDismiss = {
          showAddCommitmentEditor = false
          editingCommitment = null
        },
        onSave = { savedItem, markSettledNow ->
          val prevExisting = commitments.firstOrNull { it.id == savedItem.id }
          showAddCommitmentEditor = false
          editingCommitment = null
          container.commitmentRepository.upsert(savedItem)
          var settledTx: Transaction? = null
          if (markSettledNow) {
            val tx = HouseholdAnalyticsEngine.buildCommitmentSettlementTransaction(savedItem)
            settledTx = tx
            container.transactionRepository.insert(tx)
          }
          undoController.recordAction(
            message = "Saved \"${savedItem.displayTitle(plainEnglish)}\" (${CurrencyFormatter.formatRupees(savedItem.amountRupees)})"
          ) {
            settledTx?.let { container.transactionRepository.delete(it.id) }
            if (prevExisting != null) {
              container.commitmentRepository.upsert(prevExisting)
            } else {
              container.commitmentRepository.delete(savedItem.id)
            }
          }
        },
        onDelete = currentTarget?.let { target ->
          {
            editingCommitment = null
            container.commitmentRepository.delete(target.id)
            undoController.recordAction("Removed \"${target.displayTitle(plainEnglish)}\"") {
              container.commitmentRepository.upsert(target)
            }
          }
        }
      )
    }

    if (showFullCommitmentsDialog) {
      PakkeCommitmentsDialog(
        summary = commitmentsSummary,
        plainEnglish = plainEnglish,
        onDismiss = { showFullCommitmentsDialog = false },
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
private fun SevenDayRhythmCard(
  bars: List<HouseholdAnalyticsEngine.DailySpendBar>,
  dailySafeRupees: Long
) {
  val safeCap = dailySafeRupees.coerceAtLeast(100L)
  val maxBarValue = remember(bars, safeCap) {
    maxOf(safeCap, bars.maxOfOrNull { it.spentRupees } ?: 0L).coerceAtLeast(100L)
  }
  val weekTotal = remember(bars) { bars.sumOf { it.spentRupees } }
  val calmDaysCount = remember(bars) { bars.count { !it.isOverSafe } }

  Surface(
    shape = PebbleCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.30f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("seven_day_rhythm_card")
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
            imageVector = Icons.Default.BarChart,
            contentDescription = null,
            tint = PastelSky,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Last 7 Days Spending",
              style = MaterialTheme.typography.titleLarge,
              fontSize = 18.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "$calmDaysCount of 7 days under ${CurrencyFormatter.formatRupees(dailySafeRupees)} daily limit",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Text(
          text = CurrencyFormatter.formatCompactRupees(weekTotal),
          style = GharHandStyles.moneyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(112.dp)
          .drawWithCache {
            val w = size.width
            val h = size.height
            val safeRatio = (safeCap.toFloat() / maxBarValue.toFloat()).coerceIn(0.12f, 0.95f)
            val safeY = h * (1f - safeRatio)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            val slotW = w / bars.size.coerceAtLeast(1)
            val barW = (slotW * 0.48f).coerceAtMost(28.dp.toPx())
            val corner = CornerRadius(barW / 2f, barW / 2f)

            onDrawBehind {
              drawLine(
                color = PastelSage.copy(alpha = 0.65f),
                start = Offset(0f, safeY),
                end = Offset(w, safeY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dashEffect
              )

              bars.forEachIndexed { idx, bar ->
                val centerX = slotW * idx + slotW / 2f
                val ratio = if (bar.spentRupees <= 0L) 0.05f else {
                  (bar.spentRupees.toFloat() / maxBarValue.toFloat()).coerceIn(0.08f, 1f)
                }
                val barH = h * ratio
                val color = when {
                  bar.spentRupees == 0L -> PastelSage.copy(alpha = 0.25f)
                  bar.isOverSafe -> PastelTerracotta.copy(alpha = 0.85f)
                  else -> PastelSage.copy(alpha = 0.85f)
                }
                drawRoundRect(
                  color = color,
                  topLeft = Offset(centerX - barW / 2f, h - barH),
                  size = Size(barW, barH),
                  cornerRadius = corner
                )
              }
            }
          }
      )

      Spacer(modifier = Modifier.height(6.dp))

      Row(modifier = Modifier.fillMaxWidth()) {
        bars.forEachIndexed { idx, bar ->
          val isToday = idx == bars.lastIndex
          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (isToday) "Today" else bar.dayLabel,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
              color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 10.sp,
              maxLines = 1
            )
            Text(
              text = if (bar.spentRupees == 0L) "₹0" else CurrencyFormatter.formatCompactRupees(bar.spentRupees),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 9.sp,
              maxLines = 1
            )
          }
        }
      }
    }
  }
}



@Composable
private fun SunArcRhythmCard(safeToSpend: SafeToSpendResult) {
  val accent = if (safeToSpend.isPaceHot) PastelTerracotta else PastelSage
  val dayProgress = ((30 - safeToSpend.daysRemaining).coerceIn(1, 29)) / 30f

  Surface(
    shape = PebbleCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("pace_card")
  ) {
    Column(
      modifier = Modifier
        .jharokhaCornerFiligree(accentColor = PastelSky)
        .padding(20.dp)
    ) {
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
            imageVector = Icons.Default.WbSunny,
            contentDescription = null,
            tint = PastelHoney,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Monthly Cycle Progress",
            style = MaterialTheme.typography.titleLarge,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          shape = CircleShape,
          color = accent.copy(alpha = 0.16f)
        ) {
          Text(
            text = if (safeToSpend.isPaceHot) "Over Pace" else "On Track",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(64.dp)
          .drawWithCache {
            val w = size.width
            val h = size.height
            val arcTopLeft = Offset(w * 0.08f, h * 0.16f)
            val arcSize = Size(w * 0.84f, h * 1.55f)
            val baseStroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            val activeStroke = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)

            val angleRad = Math.toRadians((195f + 150f * dayProgress).toDouble())
            val rx = w * 0.42f
            val ry = h * 0.775f
            val cx = w * 0.5f + (rx * kotlin.math.cos(angleRad)).toFloat()
            val cy = h * 0.935f + (ry * kotlin.math.sin(angleRad)).toFloat()
            val sunCenter = Offset(cx, cy)
            val glowRadius = 22.dp.toPx()
            val sunRadius = 7.dp.toPx()
            val sunGlowBrush = Brush.radialGradient(
              colors = listOf(PastelHoney.copy(alpha = 0.45f), Color.Transparent),
              center = sunCenter,
              radius = glowRadius
            )
            val trackTint = PastelHoney.copy(alpha = 0.28f)

            onDrawBehind {
              drawArc(
                color = trackTint,
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = baseStroke
              )
              drawArc(
                color = accent,
                startAngle = 195f,
                sweepAngle = 150f * dayProgress,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = activeStroke
              )
              drawCircle(brush = sunGlowBrush, radius = glowRadius, center = sunCenter)
              drawCircle(color = PastelHoney, radius = sunRadius, center = sunCenter)
            }
          }
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Projected Month-End Spend",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = CurrencyFormatter.formatRupees(safeToSpend.projectedMonthEndSpendRupees),
          style = GharHandStyles.moneyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
private fun HandloomTapestryCard(
  profile: BudgetProfile,
  plainEnglish: Boolean
) {
  val total = (profile.fixedCommitmentsRupees + profile.savingsTargetRupees + profile.discretionaryPoolRupees)
    .coerceAtLeast(1L).toFloat()

  val fixedShare = (profile.fixedCommitmentsRupees / total).coerceIn(0.12f, 0.75f)
  val savingsShare = (profile.savingsTargetRupees / total).coerceIn(0.12f, 0.75f)
  val lifeShare = (1f - fixedShare - savingsShare).coerceAtLeast(0.12f)

  Surface(
    shape = PebbleCardShape,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Monthly Budget Split",
          style = MaterialTheme.typography.titleLarge,
          fontSize = 18.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = CurrencyFormatter.formatRupees(profile.monthlyIncomeRupees),
          style = GharHandStyles.moneyMedium,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(26.dp)
      ) {
        val gap = 5.dp.toPx()
        val usableW = size.width - gap * 2f
        val w1 = usableW * fixedShare
        val w2 = usableW * savingsShare
        val w3 = (usableW * lifeShare).coerceAtLeast(16f)

        drawRoundRect(
          color = PastelSky.copy(alpha = 0.75f),
          topLeft = Offset(0f, 0f),
          size = Size(w1, size.height),
          cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx())
        )
        drawRoundRect(
          color = PastelSage.copy(alpha = 0.82f),
          topLeft = Offset(w1 + gap, 0f),
          size = Size(w2, size.height),
          cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx())
        )
        drawRoundRect(
          color = PastelTerracotta.copy(alpha = 0.78f),
          topLeft = Offset(w1 + w2 + gap * 2f, 0f),
          size = Size(w3, size.height),
          cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx())
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        WeaveThreadTag(
          title = "Fixed Bills",
          amount = CurrencyFormatter.formatCompactRupees(profile.fixedCommitmentsRupees),
          color = PastelSky,
          modifier = Modifier.weight(1f)
        )
        WeaveThreadTag(
          title = if (plainEnglish) "Savings Goal" else "Gullak",
          amount = CurrencyFormatter.formatCompactRupees(profile.savingsTargetRupees),
          color = PastelSage,
          modifier = Modifier.weight(1f)
        )
        WeaveThreadTag(
          title = if (plainEnglish) "Daily Budget" else "Daily Batua",
          amount = CurrencyFormatter.formatCompactRupees(profile.discretionaryPoolRupees),
          color = PastelTerracotta,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun WeaveThreadTag(
  title: String,
  amount: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = color.copy(alpha = 0.13f),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = amount,
        style = GharHandStyles.moneySmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun WatercolorCategoryPetal(
  category: Category,
  plainEnglish: Boolean,
  amountRupees: Long,
  shareFraction: Float,
  isExpanded: Boolean,
  onClick: () -> Unit
) {
  val tint = when {
    category.isEssential -> PastelSage
    category == Category.CHAI_SNACKS -> PastelHoney
    category == Category.COMMUTE -> PastelSky
    category == Category.SHOPPING -> PastelPlum
    else -> PastelTerracotta
  }
  val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
  val clampedFraction = shareFraction.coerceIn(0.08f, 1f)
  val categoryLabel = category.displayLabel(plainEnglish)

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    color = Color.Transparent,
    modifier = Modifier
      .fillMaxWidth()
      .height(54.dp)
      .drawWithCache {
        val corner = CornerRadius(18.dp.toPx(), 18.dp.toPx())
        val fillW = size.width * clampedFraction
        val brush = Brush.horizontalGradient(
          colors = listOf(tint.copy(alpha = 0.28f), tint.copy(alpha = 0.14f)),
          startX = 0f,
          endX = fillW.coerceAtLeast(1f)
        )
        onDrawBehind {
          drawRoundRect(color = trackColor, cornerRadius = corner)
          drawRoundRect(brush = brush, size = Size(fillW, size.height), cornerRadius = corner)
        }
      }
      .testTag("category_petal_${category.name.lowercase()}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 14.dp),
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
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
          contentAlignment = Alignment.Center
        ) {
          CategoryIcon(category = category, size = 18.dp, tint = tint)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = categoryLabel,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = CurrencyFormatter.formatRupees(amountRupees),
          style = GharHandStyles.moneyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = if (isExpanded) "Collapse $categoryLabel" else "Inspect $categoryLabel",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
