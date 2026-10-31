package com.example.ui.common

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppContainer
import com.example.capture.HouseholdNotificationHelper
import com.example.capture.ParsedSmsCandidate
import com.example.capture.SmsInboxScanner
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.util.CurrencyFormatter
import com.example.data.preferences.DailyReminderRoutine
import com.example.data.preferences.RoutineKind
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelSky
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.contrastingContentColor
import com.example.ui.theme.readableAccentOn
import java.util.UUID
import kotlinx.coroutines.launch

private enum class SmartAlertsTab(val label: String) {
  ROUTINES_AND_NUDGES("Routines & Nudges"),
  SMS_READER("Bank Alert Import")
}

@Composable
fun SmartAlertsAndSmsDialog(
  container: AppContainer,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val plainEnglish = LocalPlainEnglishMode.current
  val prefs by container.preferencesRepository.prefsSnapshot.collectAsStateWithLifecycle()
  val transactions by container.transactionRepository.transactionsState.collectAsStateWithLifecycle()
  val undoController = LocalGharUndoController.current
  val scope = rememberCoroutineScope()

  var selectedTab by remember { mutableStateOf(SmartAlertsTab.ROUTINES_AND_NUDGES) }
  var statusBanner by remember { mutableStateOf<String?>(null) }

  // Notification permission tracking (Android 13+)
  var hasNotifPerm by remember {
    mutableStateOf(HouseholdNotificationHelper.hasNotificationPermission(context))
  }
  var pendingNotifAction by remember { mutableStateOf<(() -> Unit)?>(null) }

  val notifPermLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasNotifPerm = granted
    if (granted) {
      pendingNotifAction?.invoke()
    } else {
      statusBanner = "Notification permission is off. Enable notifications in Android Settings whenever you'd like scheduled routines."
    }
    pendingNotifAction = null
  }

  fun runWithNotifPermission(action: () -> Unit) {
    if (HouseholdNotificationHelper.hasNotificationPermission(context)) {
      hasNotifPerm = true
      action()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pendingNotifAction = action
      notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      action()
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = JharokhaCardShape,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      border = BorderStroke(1.2.dp, PastelSky.copy(alpha = 0.42f)),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.88f)
        .widthIn(max = 540.dp)
        .padding(vertical = 10.dp)
        .testTag("smart_alerts_sms_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 16.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Daily Routines & Smart SMS",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Set timed daily reminder routines, smart daytime nudges, or scan bank SMS offline",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(48.dp)
              .testTag("close_smart_alerts_dialog")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Daily Routines and Smart SMS",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Switcher
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          SmartAlertsTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            val tabBg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            val tabFg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Surface(
              shape = CircleShape,
              color = tabBg,
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clip(CircleShape)
                .clickable {
                  selectedTab = tab
                  statusBanner = null
                }
                .testTag("smart_tab_${tab.name.lowercase()}")
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
              ) {
                Text(
                  text = tab.label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = tabFg,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }

        if (!statusBanner.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = PastelSage.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.42f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("smart_alerts_status_banner")
          ) {
            Text(
              text = statusBanner.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
          SmartAlertsTab.ROUTINES_AND_NUDGES -> RoutinesAndSmartNudgesTabContent(
            container = container,
            plainEnglish = plainEnglish,
            routines = prefs.dailyRoutines,
            smartNudgesEnabled = prefs.smartRandomNudgesEnabled,
            autoBalanceAlertsEnabled = prefs.autoBalanceAlertsEnabled,
            hasNotifPerm = hasNotifPerm,
            runWithNotifPermission = ::runWithNotifPermission,
            onStatusMessage = { statusBanner = it },
            scope = scope,
            modifier = Modifier.weight(1f)
          )

          SmartAlertsTab.SMS_READER -> SmsReaderTabContent(
            container = container,
            plainEnglish = plainEnglish,
            lastBankBalancePaise = prefs.lastDetectedBankBalancePaise,
            existingTransactions = transactions,
            onStatusMessage = { statusBanner = it },
            undoController = undoController,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

@Composable
private fun RoutinesAndSmartNudgesTabContent(
  container: AppContainer,
  plainEnglish: Boolean,
  routines: List<DailyReminderRoutine>,
  smartNudgesEnabled: Boolean,
  autoBalanceAlertsEnabled: Boolean,
  hasNotifPerm: Boolean,
  runWithNotifPermission: (() -> Unit) -> Unit,
  onStatusMessage: (String) -> Unit,
  scope: kotlinx.coroutines.CoroutineScope,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()
  var editingRoutine by remember { mutableStateOf<DailyReminderRoutine?>(null) }
  var isCreatingNewRoutine by remember { mutableStateOf(false) }

  val sageAccent = PastelSage.readableAccentOn(MaterialTheme.colorScheme.surface)
  val skyAccent = PastelSky.readableAccentOn(MaterialTheme.colorScheme.surface)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 1. Smart Contextual Nudges + Auto SMS Balance Update Card
    Surface(
      shape = PebbleCardShape,
      color = PastelSage.copy(alpha = 0.12f),
      border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.38f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = sageAccent,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Smart Contextual Nudges",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Automatically sends occasional daytime updates when a bill is due soon, a day goes unlogged, or extra budget can move to savings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Switch(
            checked = smartNudgesEnabled,
            onCheckedChange = { enabled ->
              if (enabled) {
                runWithNotifPermission {
                  scope.launch {
                    container.preferencesRepository.setSmartRandomNudgesEnabled(true)
                  }
                  HouseholdNotificationHelper.scheduleNextSmartContextualNudge(context, enabled = true)
                  onStatusMessage("Smart daytime nudges enabled — Ghar will send timely updates automatically.")
                }
              } else {
                scope.launch {
                  container.preferencesRepository.setSmartRandomNudgesEnabled(false)
                }
                HouseholdNotificationHelper.scheduleNextSmartContextualNudge(context, enabled = false)
                onStatusMessage("Smart daytime nudges paused.")
              }
            },
            modifier = Modifier.testTag("smart_random_nudges_switch")
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Auto-notify after SMS / UPI capture",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Updates your Safe-to-Spend balance quietly in the status bar after an automatic bank/UPI capture",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Switch(
            checked = autoBalanceAlertsEnabled,
            onCheckedChange = { enabled ->
              scope.launch {
                container.preferencesRepository.setAutoBalanceAlertsEnabled(enabled)
              }
              if (enabled && !hasNotifPerm) {
                runWithNotifPermission {}
              }
            },
            modifier = Modifier.testTag("auto_balance_alerts_switch")
          )
        }
      }
    }

    // 2. Timed Daily Reminder Routines Card
    Surface(
      shape = PebbleCardShape,
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = skyAccent,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Daily Reminder Routines",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Set specific daily times for morning budget briefs, midday checks, or nightly logging",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        routines.forEach { routine ->
          RoutineItemRow(
            routine = routine,
            plainEnglish = plainEnglish,
            onToggle = { enabled ->
              val updated = routine.copy(enabled = enabled)
              if (enabled) {
                runWithNotifPermission {
                  scope.launch {
                    container.preferencesRepository.upsertDailyRoutine(updated)
                  }
                  HouseholdNotificationHelper.scheduleRoutineAlarm(context, updated)
                  onStatusMessage("Scheduled \"${routine.displayTitle(plainEnglish)}\" daily at ${updated.formattedTime()}.")
                }
              } else {
                scope.launch {
                  container.preferencesRepository.upsertDailyRoutine(updated)
                }
                HouseholdNotificationHelper.scheduleRoutineAlarm(context, updated)
                onStatusMessage("Paused \"${routine.displayTitle(plainEnglish)}\".")
              }
            },
            onEdit = {
              isCreatingNewRoutine = false
              editingRoutine = routine
            }
          )
          Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
          onClick = {
            isCreatingNewRoutine = true
            editingRoutine = DailyReminderRoutine(
              id = "routine_${UUID.randomUUID().toString().take(8)}",
              title = if (plainEnglish) "Evening Expense Check" else "Evening Khata Routine",
              hour = 20,
              minute = 30,
              kind = RoutineKind.CUSTOM,
              enabled = true
            )
          },
          shape = CircleShape,
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("add_daily_routine_button")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Add Daily Routine Time",
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }

  val targetRoutine = editingRoutine
  if (targetRoutine != null) {
    RoutineEditorDialog(
      initialRoutine = targetRoutine,
      isNew = isCreatingNewRoutine,
      canDelete = !isCreatingNewRoutine && targetRoutine.kind == RoutineKind.CUSTOM,
      onDismiss = { editingRoutine = null },
      onSave = { saved ->
        editingRoutine = null
        if (saved.enabled) {
          runWithNotifPermission {
            scope.launch {
              container.preferencesRepository.upsertDailyRoutine(saved)
            }
            HouseholdNotificationHelper.scheduleRoutineAlarm(context, saved)
            onStatusMessage("Saved routine \"${saved.displayTitle(plainEnglish)}\" for ${saved.formattedTime()}.")
          }
        } else {
          scope.launch {
            container.preferencesRepository.upsertDailyRoutine(saved)
          }
          HouseholdNotificationHelper.scheduleRoutineAlarm(context, saved)
          onStatusMessage("Updated routine \"${saved.displayTitle(plainEnglish)}\" (${saved.formattedTime()}).")
        }
      },
      onDelete = {
        val toDelete = targetRoutine
        editingRoutine = null
        HouseholdNotificationHelper.cancelRoutineAlarm(context, toDelete.id)
        scope.launch {
          container.preferencesRepository.deleteDailyRoutine(toDelete.id)
        }
        onStatusMessage("Removed routine \"${toDelete.title}\".")
      }
    )
  }
}

@Composable
private fun RoutineItemRow(
  routine: DailyReminderRoutine,
  plainEnglish: Boolean,
  onToggle: (Boolean) -> Unit,
  onEdit: () -> Unit
) {
  val badgeBg = if (routine.enabled) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.surfaceVariant
  }
  val badgeFg = if (routine.enabled) {
    MaterialTheme.colorScheme.onPrimary
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(
      1.dp,
      if (routine.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
      else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onEdit)
      .testTag("routine_row_${routine.id}")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = badgeBg
          ) {
            Text(
              text = routine.formattedTime(),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = badgeFg,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit routine time",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = routine.displayTitle(plainEnglish),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Switch(
        checked = routine.enabled,
        onCheckedChange = onToggle,
        modifier = Modifier.testTag("routine_switch_${routine.id}")
      )
    }
  }
}

@Composable
private fun RoutineEditorDialog(
  initialRoutine: DailyReminderRoutine,
  isNew: Boolean,
  canDelete: Boolean,
  onDismiss: () -> Unit,
  onSave: (DailyReminderRoutine) -> Unit,
  onDelete: () -> Unit
) {
  var title by remember(initialRoutine.id) { mutableStateOf(initialRoutine.title) }
  var hourText by remember(initialRoutine.id) { mutableStateOf(initialRoutine.safeHour.toString()) }
  var minuteText by remember(initialRoutine.id) {
    mutableStateOf(String.format(java.util.Locale.US, "%02d", initialRoutine.safeMinute))
  }

  val parsedHour = hourText.toIntOrNull()?.coerceIn(0, 23) ?: initialRoutine.safeHour
  val parsedMinute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: initialRoutine.safeMinute

  val quickSlots = remember {
    listOf(
      Triple("8:30 AM", 8, 30),
      Triple("2:00 PM", 14, 0),
      Triple("8:00 PM", 20, 0),
      Triple("9:30 PM", 21, 30)
    )
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = JharokhaCardShape,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("routine_editor_dialog")
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = if (isNew) "New Daily Routine" else "Edit Routine Time",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Repeats every day at ${DailyReminderRoutine.formatRoutineTime(parsedHour, parsedMinute)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = title,
          onValueChange = { title = it.take(48) },
          label = { Text("Routine Name") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("routine_title_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = hourText,
            onValueChange = { raw ->
              val digits = raw.filter { it.isDigit() }.take(2)
              hourText = digits
            },
            label = { Text("Hour (0–23)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("routine_hour_input")
          )
          OutlinedTextField(
            value = minuteText,
            onValueChange = { raw ->
              val digits = raw.filter { it.isDigit() }.take(2)
              minuteText = digits
            },
            label = { Text("Minute (00–59)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("routine_minute_input")
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Quick Times:",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          quickSlots.forEach { (slotLabel, h, m) ->
            val selected = parsedHour == h && parsedMinute == m
            val chipBg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            val chipFg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Surface(
              shape = CircleShape,
              color = chipBg,
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 40.dp)
                .clip(CircleShape)
                .clickable {
                  hourText = h.toString()
                  minuteText = String.format(java.util.Locale.US, "%02d", m)
                }
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) {
                Text(
                  text = slotLabel,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = chipFg,
                  maxLines = 1
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (canDelete) {
            IconButton(
              onClick = onDelete,
              modifier = Modifier
                .size(48.dp)
                .testTag("delete_routine_button")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete routine",
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
          OutlinedButton(
            onClick = onDismiss,
            shape = CircleShape,
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
          ) {
            Text("Cancel", fontWeight = FontWeight.SemiBold)
          }
          Button(
            onClick = {
              onSave(
                initialRoutine.copy(
                  title = title.trim().ifBlank { "Daily Reminder" },
                  hour = parsedHour,
                  minute = parsedMinute,
                  enabled = true
                )
              )
            },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("save_routine_button")
          ) {
            Text(
              text = "Save Routine",
              color = MaterialTheme.colorScheme.onPrimary,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SmsReaderTabContent(
  container: AppContainer,
  plainEnglish: Boolean,
  lastBankBalancePaise: Long?,
  existingTransactions: List<com.example.core.model.Transaction>,
  onStatusMessage: (String) -> Unit,
  undoController: GharUndoController?,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var isScanning by remember { mutableStateOf(false) }
  var candidates by remember { mutableStateOf<List<ParsedSmsCandidate>>(emptyList()) }
  var scannedCount by remember { mutableStateOf<Int?>(null) }
  var pastedSms by remember { mutableStateOf("") }
  var sampleIndex by remember { mutableIntStateOf(0) }

  val skyAccent = PastelSky.readableAccentOn(MaterialTheme.colorScheme.surface)
  val sageAccent = PastelSage.readableAccentOn(MaterialTheme.colorScheme.surface)

  fun executeClipboardScan() {
    isScanning = true
    scope.launch {
      val summary = SmsInboxScanner.scanClipboardAlerts(context, existingTransactions)
      isScanning = false
      scannedCount = summary.scannedMessagesCount
      candidates = summary.candidates
      if (summary.latestAvailableBalancePaise != null) {
        container.preferencesRepository.setLastDetectedBankBalancePaise(summary.latestAvailableBalancePaise)
      }
      onStatusMessage(
        when {
          summary.scannedMessagesCount == 0 ->
            "Clipboard is empty. Copy any bank/UPI SMS (or use Share -> Ghar in your Messages app) and tap again."
          summary.candidates.isEmpty() ->
            "Checked clipboard text — no new unlogged bank/UPI debit or credit found (OTPs are ignored)."
          else ->
            "Found ${summary.candidates.size} bank/UPI alert(s) in your clipboard ready to import."
        }
      )
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(bottom = 12.dp)
  ) {
    if (lastBankBalancePaise != null) {
      item(key = "detected_bank_balance") {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = PastelSky.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.42f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("detected_sms_bank_balance_card")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = skyAccent,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Last Bank Balance",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Extracted 100% offline on-device from bank alert",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Text(
              text = CurrencyFormatter.formatRupees(lastBankBalancePaise / 100L),
              style = GharHandStyles.moneyMedium,
              color = skyAccent,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    item(key = "inbox_scanner_card") {
      Surface(
        shape = PebbleCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Sms,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Zero-Permission Clipboard & Share Import",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Ghar never asks Android for raw SMS/OTP inbox permissions. Copy one or more bank/UPI messages and tap Scan Clipboard, or use Share -> Ghar straight from your Messages app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { executeClipboardScan() },
              enabled = !isScanning,
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ),
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .testTag("scan_sms_inbox_button")
            ) {
              if (isScanning) {
                CircularProgressIndicator(
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
              }
              Text(
                text = if (isScanning) "Scanning..." else "Scan Copied Bank Alert",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            if (candidates.isNotEmpty()) {
              OutlinedButton(
                onClick = {
                  val batch = candidates.map { it.transaction }
                  container.transactionRepository.insertAll(batch)
                  candidates = emptyList()
                  onStatusMessage("Imported ${batch.size} bank/UPI transaction(s).")
                  undoController?.recordAction("Imported ${batch.size} bank alerts") {
                    batch.forEach { container.transactionRepository.delete(it.id) }
                  }
                },
                shape = CircleShape,
                modifier = Modifier
                  .defaultMinSize(minHeight = 48.dp)
                  .testTag("import_all_sms_button")
              ) {
                Text("Import All (${candidates.size})", fontWeight = FontWeight.Bold, maxLines = 1)
              }
            }
          }
          if (scannedCount != null && scannedCount!! > 0 && candidates.isEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Checked $scannedCount alert block(s) • Everything is up to date.",
              style = MaterialTheme.typography.labelSmall,
              color = sageAccent
            )
          }
        }
      }
    }

    if (candidates.isNotEmpty()) {
      items(candidates, key = { it.id }) { candidate ->
        val tx = candidate.transaction
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_candidate_row_${candidate.id}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CategoryIcon(category = tx.category, size = 18.dp, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = tx.displayMerchant(plainEnglish),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${tx.category.displayLabel(plainEnglish)} • ${candidate.sender}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = (if (tx.isExpense) "-" else "+") + CurrencyFormatter.formatRupees(tx.amountRupees),
              style = GharHandStyles.moneySmall,
              color = if (tx.isExpense) MaterialTheme.colorScheme.onSurface else RupeeGreen.readableAccentOn(MaterialTheme.colorScheme.surface),
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = CircleShape,
              color = PastelSage.copy(alpha = 0.2f),
              modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(CircleShape)
                .clickable {
                  container.transactionRepository.insert(tx)
                  if (candidate.availableBalancePaise != null) {
                    scope.launch {
                      container.preferencesRepository.setLastDetectedBankBalancePaise(candidate.availableBalancePaise)
                    }
                  }
                  candidates = candidates.filterNot { it.id == candidate.id }
                  onStatusMessage("Logged ${tx.merchant} (${CurrencyFormatter.formatRupees(tx.amountRupees)})")
                  undoController?.recordAction("Logged SMS: ${tx.merchant}") {
                    container.transactionRepository.delete(tx.id)
                  }
                }
                .testTag("import_sms_row_${candidate.id}")
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text("Add", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = sageAccent)
              }
            }
          }
        }
      }
    }

    // Zero-Permission Paste / Sample Bank SMS Parser
    item(key = "paste_sms_card") {
      val terracottaContent = PastelTerracotta.contrastingContentColor()
      Surface(
        shape = PebbleCardShape,
        color = PastelHoney.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, PastelHoney.copy(alpha = 0.38f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Paste or Test a Bank SMS",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
              shape = CircleShape,
              color = PastelHoney.copy(alpha = 0.24f),
              modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
                .clip(CircleShape)
                .clickable {
                  pastedSms = SmsInboxScanner.sampleBankAlerts[sampleIndex % SmsInboxScanner.sampleBankAlerts.size]
                  sampleIndex++
                }
                .testTag("fill_sample_sms_button")
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                  text = "Try Sample SMS",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = pastedSms,
            onValueChange = { pastedSms = it },
            label = { Text("Paste bank / UPI / card SMS text") },
            maxLines = 4,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("paste_sms_input")
          )

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = {
              val batchSummary = SmsInboxScanner.parseBatchAlertText(
                rawBatch = pastedSms,
                existingTransactions = existingTransactions
              )
              if (batchSummary.candidates.isEmpty()) {
                val singleFallback = SmsInboxScanner.parseManualSmsText(pastedSms)
                if (singleFallback == null) {
                  onStatusMessage("No valid debit/credit transaction detected in that message (OTPs are ignored).")
                } else {
                  val tx = singleFallback.transaction
                  container.transactionRepository.insert(tx)
                  if (singleFallback.availableBalancePaise != null) {
                    scope.launch {
                      container.preferencesRepository.setLastDetectedBankBalancePaise(singleFallback.availableBalancePaise)
                    }
                  }
                  pastedSms = ""
                  onStatusMessage(
                    "Logged ${if (tx.isExpense) "Expense" else "Income"}: ${tx.merchant} (${CurrencyFormatter.formatRupees(tx.amountRupees)})"
                  )
                  undoController?.recordAction("Logged SMS: ${tx.merchant}") {
                    container.transactionRepository.delete(tx.id)
                  }
                }
              } else {
                val batch = batchSummary.candidates.map { it.transaction }
                container.transactionRepository.insertAll(batch)
                if (batchSummary.latestAvailableBalancePaise != null) {
                  scope.launch {
                    container.preferencesRepository.setLastDetectedBankBalancePaise(batchSummary.latestAvailableBalancePaise)
                  }
                }
                pastedSms = ""
                val balSuffix = batchSummary.latestAvailableBalancePaise?.let {
                  " • Bank Bal: ${CurrencyFormatter.formatRupees(it / 100L)}"
                }.orEmpty()
                if (batch.size == 1) {
                  val tx = batch.first()
                  onStatusMessage(
                    "Logged ${if (tx.isExpense) "Expense" else "Income"}: ${tx.merchant} (${CurrencyFormatter.formatRupees(tx.amountRupees)})$balSuffix"
                  )
                  undoController?.recordAction("Logged SMS: ${tx.merchant}") {
                    container.transactionRepository.delete(tx.id)
                  }
                } else {
                  onStatusMessage("Logged ${batch.size} bank/UPI transactions$balSuffix")
                  undoController?.recordAction("Logged ${batch.size} bank alerts") {
                    batch.forEach { container.transactionRepository.delete(it.id) }
                  }
                }
              }
            },
            enabled = pastedSms.isNotBlank(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
              containerColor = PastelTerracotta,
              contentColor = terracottaContent
            ),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .testTag("parse_pasted_sms_button")
          ) {
            Text(
              text = "Parse & Log SMS Transaction",
              color = terracottaContent,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
