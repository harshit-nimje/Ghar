package com.example.ui.study

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.core.localization.GharTerms
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.model.BudgetProfile
import com.example.core.model.InstrumentType
import com.example.core.model.SavingsGoal
import com.example.core.util.CurrencyFormatter
import com.example.ui.bhairoom.PapaGuidanceDialog
import com.example.ui.common.GharUndoController
import com.example.ui.common.GharUndoSnackbarHost
import com.example.ui.common.HisaabCalculatorDialog
import com.example.ui.common.IllustratedRoomBanner
import com.example.ui.common.JharokhaCardShape
import com.example.ui.common.LocalGharUndoController
import com.example.ui.common.PapaAvatar
import com.example.ui.common.PebbleCardShape
import com.example.ui.common.jharokhaCornerFiligree
import com.example.ui.common.pastelHomeBackdrop
import com.example.ui.common.rememberGharUndoController
import com.example.ui.kitchen.MaaGuidanceDialog
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelPlum
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelSky
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.contrastingContentColor

@Composable
fun StudyScreen(
  container: AppContainer,
  listState: LazyListState = rememberLazyListState(),
  onShowRoomGuide: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val plainEnglish = LocalPlainEnglishMode.current
  val hasParentUndo = LocalGharUndoController.current != null
  val fallbackUndo = rememberGharUndoController()
  val undoController = LocalGharUndoController.current ?: fallbackUndo

  val goal by container.goalRepository.primaryGoalState.collectAsStateWithLifecycle()
  val allGoals by container.goalRepository.allGoalsState.collectAsStateWithLifecycle()
  val profile by container.budgetRepository.profileState.collectAsStateWithLifecycle()
  val transactions by container.transactionRepository.transactionsState.collectAsStateWithLifecycle()
  val commitments by container.commitmentRepository.commitmentsState.collectAsStateWithLifecycle()
  val prefs by container.preferencesRepository.prefsSnapshot.collectAsStateWithLifecycle()
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
  val safeToSpend = remember(profile, transactions) {
    val spent = transactions.filter { it.isExpense }.sumOf { it.amountPaise }
    val income = transactions.filter { !it.isExpense }.sumOf { it.amountPaise }
    SafeToSpendCalculator.calculate(profile, spent, income)
  }

  var showPapaGuidanceDialog by remember { mutableStateOf(false) }
  var showMaaGuidanceDialog by remember { mutableStateOf(false) }
  var pendingCrossChatPrompt by remember { mutableStateOf<String?>(null) }

  var selectedInstrument by remember(goal.id, goal.instrumentType) {
    mutableStateOf(goal.instrumentType)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .pastelHomeBackdrop(
        primaryWash = PastelHoney,
        secondaryWash = PastelSage,
        tertiaryWash = PastelTerracotta
      )
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item(key = "study_banner", contentType = "banner") {
        IllustratedRoomBanner(
          artworkRes = R.drawable.img_study_desk_1792931360510,
          roomBadge = if (plainEnglish) "Planner • Goals & Budget" else "The Study • Goals & Budget Desk",
          title = if (plainEnglish) "Goals & Monthly Budget" else "Dreams & Calibration",
          keyHighlight = "${goal.name} • ${CurrencyFormatter.formatRupees(goal.targetAmountRupees)} target",
          accentColor = PastelHoney,
          heroHeight = 280.dp,
          scrollOffsetProvider = {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 600
          },
          onHelpClick = onShowRoomGuide
        )
      }

      if (allGoals.size > 1) {
        item(key = "study_gullak_selector", contentType = "selector") {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            allGoals.forEach { item ->
              val selected = item.id == goal.id
              Surface(
                onClick = { container.goalRepository.selectGoal(item.id) },
                shape = CircleShape,
                color = if (selected) PastelSage else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                  1.dp,
                  if (selected) PastelSage else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                  .defaultMinSize(minHeight = 42.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  val sageSelectedContent = PastelSage.contrastingContentColor()
                  Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    tint = if (selected) sageSelectedContent else PastelSage,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) sageSelectedContent else MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }
        }
      }

      // Consult Papa / Ask Dad — Live Grounded Investments & Big Savings Chat Card
      item(key = "study_papa_chat_card", contentType = "papa_chat") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          Surface(
            onClick = {
              pendingCrossChatPrompt = null
              showPapaGuidanceDialog = true
            },
            shape = JharokhaCardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.2.dp, PastelSage.copy(alpha = 0.45f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("study_open_papa_chat_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                    "Ask Dad about SIPs, live FD/PPF rates, inflation & allocating big amounts"
                  } else {
                    "Ask Papa about SIPs, live FD/PPF rates, inflation & investing big amounts"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              val sageBadgeContent = PastelSage.contrastingContentColor()
              Surface(
                shape = CircleShape,
                color = PastelSage
              ) {
                Text(
                  text = "Ask",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = sageBadgeContent,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
            }
          }
        }
      }

      // Leaf-isolated Goal Editor Card (typing never recomposes LazyColumn)
      item(key = "study_goal_card", contentType = "goal") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          StudyGoalEditorCard(
            goal = goal,
            selectedInstrument = selectedInstrument,
            plainEnglish = plainEnglish,
            undoController = undoController,
            onSaveGoal = { updated -> container.goalRepository.saveGoal(updated) },
            onRevertInstrument = { reverted -> selectedInstrument = reverted }
          )
        }
      }

      // Leaf-isolated Monthly Budget Calibration Card (typing never recomposes LazyColumn)
      item(key = "study_budget_calibration", contentType = "budget_desk") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          StudyBudgetCalibrationCard(
            profile = profile,
            plainEnglish = plainEnglish,
            undoController = undoController,
            onSaveProfile = { updated -> container.budgetRepository.saveProfile(updated) }
          )
        }
      }

      // Leaf-isolated Compounding Maturity Simulator Card
      item(key = "study_compounding_simulator", contentType = "simulator") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          StudyCompoundingSimulatorCard(
            currentGoalRupees = goal.currentAmountRupees,
            defaultMonthlySavingsRupees = profile.savingsTargetRupees,
            selectedInstrument = selectedInstrument
          )
        }
      }

      // Investment Option Selector Card
      item(key = "study_soil_selector", contentType = "soil") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          Surface(
            shape = PebbleCardShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Spa,
                  contentDescription = null,
                  tint = PastelSage,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Investment Option",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Spacer(modifier = Modifier.height(14.dp))

              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InstrumentType.entries.forEachIndexed { idx, instrument ->
                  val isSelected = instrument == selectedInstrument
                  val seedTint = when (idx % 4) {
                    0 -> PastelSage
                    1 -> PastelHoney
                    2 -> PastelSky
                    else -> PastelPlum
                  }

                  Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) seedTint.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                    border = BorderStroke(
                      1.2.dp,
                      if (isSelected) seedTint else Color.Transparent
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(18.dp))
                      .clickable {
                        if (selectedInstrument != instrument) {
                          selectedInstrument = instrument
                          container.goalRepository.saveGoal(goal.copy(instrumentType = instrument))
                        }
                      }
                  ) {
                    Row(
                      modifier = Modifier.padding(14.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Box(
                        modifier = Modifier
                          .size(38.dp)
                          .clip(CircleShape)
                          .background(seedTint.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(
                          imageVector = Icons.Default.Spa,
                          contentDescription = null,
                          tint = seedTint,
                          modifier = Modifier.size(18.dp)
                        )
                      }
                      Spacer(modifier = Modifier.width(12.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(
                            text = instrument.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                          )
                          Spacer(modifier = Modifier.width(8.dp))
                          Surface(
                            shape = CircleShape,
                            color = seedTint.copy(alpha = 0.2f)
                          ) {
                            Text(
                              text = "~${instrument.typicalAnnualReturnPercent}% p.a.",
                              style = GharHandStyles.moneySmall,
                              color = seedTint,
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                          }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                          text = instrument.description,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun StudyGoalEditorCard(
  goal: SavingsGoal,
  selectedInstrument: InstrumentType,
  plainEnglish: Boolean,
  undoController: GharUndoController,
  onSaveGoal: (SavingsGoal) -> Unit,
  onRevertInstrument: (InstrumentType) -> Unit
) {
  var editingName by remember(goal.id, goal.name) { mutableStateOf(goal.name) }
  var editingTargetRupees by remember(goal.id, goal.targetAmountRupees) {
    mutableStateOf(goal.targetAmountRupees.toString())
  }
  var editingCurrentRupees by remember(goal.id, goal.currentAmountRupees) {
    mutableStateOf(goal.currentAmountRupees.toString())
  }
  var isSavedNotice by remember { mutableStateOf(false) }
  var showTargetCalculator by remember { mutableStateOf(false) }

  Surface(
    shape = JharokhaCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, PastelHoney.copy(alpha = 0.32f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("study_goal_card")
  ) {
    Column(
      modifier = Modifier
        .jharokhaCornerFiligree(accentColor = PastelHoney)
        .padding(20.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = PastelHoney,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (plainEnglish) "Savings Goal Planner" else "Household Dream Journal",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = editingName,
        onValueChange = {
          editingName = it
          isSavedNotice = false
        },
        label = { Text(if (plainEnglish) "Goal name" else "Dream name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = editingTargetRupees,
          onValueChange = {
            editingTargetRupees = it.filter { c -> c.isDigit() }
            isSavedNotice = false
          },
          label = { Text("Target (₹)") },
          trailingIcon = {
            IconButton(onClick = { showTargetCalculator = true }) {
              Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = "Calculate target amount",
                tint = PastelHoney
              )
            }
          },
          textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(16.dp)
        )

        OutlinedTextField(
          value = editingCurrentRupees,
          onValueChange = {
            editingCurrentRupees = it.filter { c -> c.isDigit() }
            isSavedNotice = false
          },
          label = { Text(if (plainEnglish) "Saved So Far (₹)" else "In Pot Now (₹)") },
          textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("study_goal_current_input"),
          shape = RoundedCornerShape(16.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Button(
        onClick = {
          val previousGoal = goal
          val target = editingTargetRupees.toLongOrNull()?.coerceAtLeast(100L) ?: goal.targetAmountRupees
          val currentSaved = editingCurrentRupees.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
          val updatedGoal = goal.copy(
            name = editingName.ifBlank { "Emergency Fund" },
            targetAmountPaise = target * 100L,
            currentAmountPaise = currentSaved * 100L,
            instrumentType = selectedInstrument
          )
          onSaveGoal(updatedGoal)
          isSavedNotice = true
          undoController.recordAction("Saved \"${updatedGoal.name}\"") {
            onSaveGoal(previousGoal)
            editingName = previousGoal.name
            editingTargetRupees = previousGoal.targetAmountRupees.toString()
            editingCurrentRupees = previousGoal.currentAmountRupees.toString()
            onRevertInstrument(previousGoal.instrumentType)
            isSavedNotice = false
          }
        },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 50.dp)
          .testTag("save_study_goal_button")
      ) {
        Text(
          text = if (plainEnglish) "Save Goal Details" else "Write Into Journal",
          color = MaterialTheme.colorScheme.onPrimary,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          maxLines = 1
        )
      }

      if (isSavedNotice) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = if (plainEnglish) {
            "✓ Savings goal updated."
          } else {
            "✓ Saved in journal. ${GharTerms.papaName(false)} updated the Gullak."
          },
          style = GharHandStyles.stickyNoteMeta,
          color = PastelSage
        )
      }
    }
  }

  if (showTargetCalculator) {
    HisaabCalculatorDialog(
      initialAmountRupees = editingTargetRupees.toLongOrNull(),
      onDismiss = { showTargetCalculator = false },
      onUseAmount = { calculated ->
        editingTargetRupees = calculated.toString()
        isSavedNotice = false
        showTargetCalculator = false
      }
    )
  }
}

@Composable
private fun StudyBudgetCalibrationCard(
  profile: BudgetProfile,
  plainEnglish: Boolean,
  undoController: GharUndoController,
  onSaveProfile: (BudgetProfile) -> Unit
) {
  var incomeText by remember(profile.monthlyIncomeRupees) {
    mutableStateOf(profile.monthlyIncomeRupees.toString())
  }
  var fixedBillsText by remember(profile.fixedCommitmentsRupees) {
    mutableStateOf(profile.fixedCommitmentsRupees.toString())
  }
  var monthlySavingsText by remember(profile.savingsTargetRupees) {
    mutableStateOf(profile.savingsTargetRupees.toString())
  }
  var paydayStartText by remember(profile.monthStartDay) {
    mutableStateOf(profile.monthStartDay.toString())
  }
  var budgetSavedNotice by remember { mutableStateOf(false) }

  val previewIncome = incomeText.toLongOrNull()?.coerceAtLeast(0L) ?: profile.monthlyIncomeRupees
  val previewFixed = fixedBillsText.toLongOrNull()?.coerceAtLeast(0L) ?: profile.fixedCommitmentsRupees
  val previewSavings = monthlySavingsText.toLongOrNull()?.coerceAtLeast(0L) ?: profile.savingsTargetRupees
  val previewPool = (previewIncome - previewFixed - previewSavings).coerceAtLeast(0L)
  val previewDailyBatua = previewPool / 30L

  Surface(
    shape = JharokhaCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.35f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("study_budget_calibration_card")
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.AccountBalanceWallet,
          contentDescription = null,
          tint = PastelSky,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = if (plainEnglish) "Monthly Budget Setup" else "Monthly Household Calibration",
            style = MaterialTheme.typography.titleLarge,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Adjust monthly income, fixed bills, savings target & cycle start day",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = incomeText,
          onValueChange = {
            incomeText = it.filter { c -> c.isDigit() }
            budgetSavedNotice = false
          },
          label = { Text("Monthly Income (₹)") },
          textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("study_income_input")
        )

        OutlinedTextField(
          value = fixedBillsText,
          onValueChange = {
            fixedBillsText = it.filter { c -> c.isDigit() }
            budgetSavedNotice = false
          },
          label = { Text("Fixed Bills (₹)") },
          textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("study_fixed_bills_input")
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = monthlySavingsText,
          onValueChange = {
            monthlySavingsText = it.filter { c -> c.isDigit() }
            budgetSavedNotice = false
          },
          label = {
            Text(if (plainEnglish) "Monthly Savings Target (₹)" else "Monthly Gullak Goal (₹)")
          },
          textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1.3f)
            .testTag("study_savings_target_input")
        )

        OutlinedTextField(
          value = paydayStartText,
          onValueChange = {
            paydayStartText = it.filter { c -> c.isDigit() }.take(2)
            budgetSavedNotice = false
          },
          label = { Text("Payday (1-28)") },
          textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(0.9f)
            .testTag("study_payday_input")
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = PastelSage.copy(alpha = 0.13f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (plainEnglish) "Discretionary Monthly Budget" else "Discretionary Batua Pool",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "${CurrencyFormatter.formatRupees(previewPool)} / month",
              style = GharHandStyles.moneyMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Text(
            text = "≈ ${CurrencyFormatter.formatRupees(previewDailyBatua)}/day",
            style = GharHandStyles.moneySmall,
            color = PastelSage
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Button(
        onClick = {
          val prevProfile = profile
          val newIncome = incomeText.toLongOrNull()?.coerceAtLeast(1000L) ?: profile.monthlyIncomeRupees
          val newFixed = fixedBillsText.toLongOrNull()?.coerceAtLeast(0L) ?: profile.fixedCommitmentsRupees
          val newSavings = monthlySavingsText.toLongOrNull()?.coerceAtLeast(0L) ?: profile.savingsTargetRupees
          val newPayday = (paydayStartText.toIntOrNull() ?: profile.monthStartDay).coerceIn(1, 28)

          val updatedProfile = profile.copy(
            monthlyIncomePaise = newIncome * 100L,
            fixedCommitmentsPaise = newFixed * 100L,
            savingsTargetPaise = newSavings * 100L,
            monthStartDay = newPayday
          )
          onSaveProfile(updatedProfile)
          budgetSavedNotice = true
          undoController.recordAction("Updated monthly budget numbers") {
            onSaveProfile(prevProfile)
            incomeText = prevProfile.monthlyIncomeRupees.toString()
            fixedBillsText = prevProfile.fixedCommitmentsRupees.toString()
            monthlySavingsText = prevProfile.savingsTargetRupees.toString()
            paydayStartText = prevProfile.monthStartDay.toString()
            budgetSavedNotice = false
          }
        },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = PastelSky,
          contentColor = PastelSky.contrastingContentColor()
        ),
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 48.dp)
          .testTag("save_study_budget_button")
      ) {
        Text(
          text = if (plainEnglish) "Save Monthly Budget" else "Save Household Numbers",
          color = PastelSky.contrastingContentColor(),
          fontWeight = FontWeight.Bold
        )
      }

      if (budgetSavedNotice) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = if (plainEnglish) {
            "✓ Daily safe-to-spend budget updated immediately."
          } else {
            "✓ Daily Batua & Insights updated immediately."
          },
          style = GharHandStyles.stickyNoteMeta,
          color = PastelSage
        )
      }
    }
  }
}

@Composable
private fun StudyCompoundingSimulatorCard(
  currentGoalRupees: Long,
  defaultMonthlySavingsRupees: Long,
  selectedInstrument: InstrumentType
) {
  var simHorizonYears by remember { mutableIntStateOf(5) }
  var simMonthlySipText by remember(defaultMonthlySavingsRupees) {
    mutableStateOf(defaultMonthlySavingsRupees.coerceAtLeast(1000L).toString())
  }

  val projection = remember(currentGoalRupees, simMonthlySipText, selectedInstrument, simHorizonYears) {
    HouseholdAnalyticsEngine.projectCompounding(
      initialRupees = currentGoalRupees,
      monthlyContributionRupees = simMonthlySipText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
      annualReturnPercent = selectedInstrument.typicalAnnualReturnPercent,
      years = simHorizonYears
    )
  }

  Surface(
    shape = PebbleCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.35f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("study_compounding_simulator_card")
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.TrendingUp,
          contentDescription = null,
          tint = PastelSage,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "Compounding Calculator",
            style = MaterialTheme.typography.titleLarge,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${selectedInstrument.displayName} (~${selectedInstrument.typicalAnnualReturnPercent}% p.a.)",
            style = MaterialTheme.typography.labelSmall,
            color = PastelSage,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = simMonthlySipText,
        onValueChange = { simMonthlySipText = it.filter { c -> c.isDigit() } },
        label = { Text("Monthly Contribution (₹/mo)") },
        prefix = { Text("₹ ", style = GharHandStyles.moneyMedium, color = PastelSage) },
        textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("simulator_monthly_input")
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(1, 3, 5, 10).forEach { yrs ->
          val selected = simHorizonYears == yrs
          Surface(
            shape = CircleShape,
            color = if (selected) PastelSage else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 40.dp)
              .clip(CircleShape)
              .clickable { simHorizonYears = yrs }
              .testTag("sim_horizon_${yrs}y")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = "${yrs}Y",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) PastelSage.contrastingContentColor() else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SimulatorStatBox(
          label = "Total Principal",
          value = CurrencyFormatter.formatCompactRupees(projection.totalPrincipalRupees),
          tint = MaterialTheme.colorScheme.onSurface,
          wash = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier.weight(1f)
        )
        SimulatorStatBox(
          label = "Estimated Returns",
          value = "+${CurrencyFormatter.formatCompactRupees(projection.wealthGainRupees)}",
          tint = PastelSage,
          wash = PastelSage.copy(alpha = 0.15f),
          modifier = Modifier.weight(1f)
        )
        SimulatorStatBox(
          label = "${simHorizonYears}Y Value",
          value = CurrencyFormatter.formatCompactRupees(projection.maturityValueRupees),
          tint = PastelTerracotta,
          wash = PastelHoney.copy(alpha = 0.20f),
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun SimulatorStatBox(
  label: String,
  value: String,
  tint: Color,
  wash: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = wash,
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = GharHandStyles.moneySmall,
        color = tint,
        fontWeight = FontWeight.Bold,
        maxLines = 1
      )
    }
  }
}
