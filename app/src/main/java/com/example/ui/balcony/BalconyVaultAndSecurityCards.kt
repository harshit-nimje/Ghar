package com.example.ui.balcony

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.AppContainer
import com.example.R
import com.example.core.audio.GharSoundEffects
import com.example.core.model.Transaction
import com.example.data.backup.ParsedVaultBackup
import com.example.data.backup.VaultBackupManager
import com.example.ui.common.GharConfirmDialog
import com.example.ui.common.PebbleCardShape
import com.example.ui.security.GharBiometricLockHelper
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelPlum
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelSky
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.contrastingContentColor
import com.example.ui.theme.readableAccentOn
import com.example.ui.widget.GharQuickStampWidgetUpdater
import com.example.ui.widget.PinWidgetResult
import kotlinx.coroutines.launch

@Composable
fun BalconyVaultBackupAndCsvCard(
  container: AppContainer,
  transactions: List<Transaction>,
  plainEnglish: Boolean,
  soundEnabled: Boolean,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var isWorking by remember { mutableStateOf(false) }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }
  var isFeedbackError by remember { mutableStateOf(false) }
  var pendingRestoreBackup by remember { mutableStateOf<ParsedVaultBackup?>(null) }

  val createJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    isWorking = true
    feedbackMessage = null
    scope.launch {
      val result = VaultBackupManager.writeBackupJsonToUri(context, uri, container)
      isWorking = false
      result.fold(
        onSuccess = { summary ->
          isFeedbackError = false
          val ledgerWord = if (plainEnglish) "transactions" else "Khata entries"
          feedbackMessage = "✓ Saved .json backup (${summary.transactionCount} $ledgerWord, ${summary.goalCount} goals, ${summary.stampCount} stamps)."
          if (soundEnabled) {
            GharSoundEffects.playGullakCoinClink(context, soundEnabled = true)
          }
        },
        onFailure = { err ->
          isFeedbackError = true
          feedbackMessage = err.localizedMessage ?: "Could not save .json backup file."
        }
      )
    }
  }

  val openJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    isWorking = true
    feedbackMessage = null
    scope.launch {
      val parsedRes = VaultBackupManager.readAndValidateBackupFromUri(context, uri, container)
      isWorking = false
      parsedRes.fold(
        onSuccess = { parsed ->
          pendingRestoreBackup = parsed
        },
        onFailure = { err ->
          isFeedbackError = true
          feedbackMessage = err.localizedMessage ?: "Selected file is not a valid Ghar .json backup."
        }
      )
    }
  }

  val createCsvLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/csv")
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    isWorking = true
    feedbackMessage = null
    scope.launch {
      val result = VaultBackupManager.exportTransactionsCsvToUri(
        context = context,
        uri = uri,
        transactions = transactions,
        plainEnglish = plainEnglish
      )
      isWorking = false
      result.fold(
        onSuccess = { count ->
          isFeedbackError = false
          feedbackMessage = "✓ Exported $count rows to spreadsheet (.csv)."
        },
        onFailure = { err ->
          isFeedbackError = true
          feedbackMessage = err.localizedMessage ?: "Could not export .csv spreadsheet."
        }
      )
    }
  }

  Surface(
    shape = PebbleCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.2.dp, PastelHoney.copy(alpha = 0.48f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("balcony_vault_backup_card")
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(PastelHoney.copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.SaveAlt,
            contentDescription = null,
            tint = PastelHoney.readableAccentOn(MaterialTheme.colorScheme.surface),
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (plainEnglish) {
              "Vault Backup, Restore & CSV"
            } else {
              stringResource(R.string.vault_backup_card_title)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = if (plainEnglish) {
              "Switch phones offline with 1-tap .json backup/restore or export a .csv spreadsheet"
            } else {
              stringResource(R.string.vault_backup_card_subtitle)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = {
            if (!isWorking) {
              createJsonLauncher.launch(VaultBackupManager.suggestedJsonBackupFileName())
            }
          },
          enabled = !isWorking,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("backup_json_button")
        ) {
          Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(17.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.vault_backup_json_action),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        OutlinedButton(
          onClick = {
            if (!isWorking) {
              openJsonLauncher.launch(
                arrayOf("application/json", "text/*", "application/octet-stream")
              )
            }
          },
          enabled = !isWorking,
          shape = CircleShape,
          border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("restore_json_button")
        ) {
          Icon(
            imageVector = Icons.Default.UploadFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.vault_restore_json_action),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedButton(
        onClick = {
          if (!isWorking) {
            createCsvLauncher.launch(VaultBackupManager.suggestedCsvExportFileName())
          }
        },
        enabled = !isWorking,
        shape = CircleShape,
        border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.65f)),
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 48.dp)
          .testTag("export_csv_button")
      ) {
        if (isWorking) {
          CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
        } else {
          Icon(
            imageVector = Icons.Default.TableView,
            contentDescription = null,
            tint = PastelSage.readableAccentOn(MaterialTheme.colorScheme.surface),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = "${stringResource(R.string.vault_export_csv_action)} (${transactions.size})",
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      feedbackMessage?.let { msg ->
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isFeedbackError) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
          } else {
            PastelSage.copy(alpha = 0.16f)
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = msg,
            style = MaterialTheme.typography.bodySmall,
            color = if (isFeedbackError) {
              MaterialTheme.colorScheme.onErrorContainer
            } else {
              MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
          )
        }
      }
    }
  }

  val candidate = pendingRestoreBackup
  if (candidate != null) {
    val entriesWord = if (plainEnglish) "transactions" else "Khata entries"
    GharConfirmDialog(
      title = "Restore Vault Backup?",
      message = "Found backup for \"${candidate.userNameDisplay}\" containing ${candidate.transactionCount} $entriesWord, ${candidate.goalCount} savings ${if (candidate.goalCount == 1) "goal" else "goals"}, and ${candidate.stampCount} Quick Stamps.\n\nThis will replace the current ledger on this phone with the backup.",
      confirmLabel = "Restore Backup",
      isDestructive = false,
      onConfirm = {
        val toApply = candidate
        pendingRestoreBackup = null
        isWorking = true
        scope.launch {
          val res = VaultBackupManager.applyValidatedBackup(context, toApply, container)
          isWorking = false
          res.fold(
            onSuccess = { applied ->
              isFeedbackError = false
              feedbackMessage = "✓ Restored ${applied.transactionCount} $entriesWord & ${applied.goalCount} goals for ${applied.userNameDisplay}."
              if (soundEnabled) {
                GharSoundEffects.playGullakCoinClink(context, soundEnabled = true)
              }
            },
            onFailure = { err ->
              isFeedbackError = true
              feedbackMessage = err.localizedMessage ?: "Failed to restore backup."
            }
          )
        }
      },
      onDismiss = { pendingRestoreBackup = null },
      dialogTestTag = "confirm_restore_backup_dialog",
      confirmTestTag = "confirm_restore_backup_button"
    )
  }
}

@Composable
fun BalconySecurityAndWidgetCard(
  container: AppContainer,
  appLockEnabled: Boolean,
  plainEnglish: Boolean,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var securityFeedback by remember { mutableStateOf<String?>(null) }
  var widgetFeedback by remember { mutableStateOf<String?>(null) }

  fun handleToggleAppLock(targetEnabled: Boolean) {
    securityFeedback = null
    val activity = GharBiometricLockHelper.findFragmentActivity(context)
    val capability = GharBiometricLockHelper.queryCapability(context)
    if (!capability.isSupportedAndEnrolled) {
      securityFeedback = capability.statusMessage
      return
    }
    if (activity == null) {
      scope.launch { container.preferencesRepository.setAppLockEnabled(targetEnabled) }
      return
    }

    GharBiometricLockHelper.authenticate(
      activity = activity,
      title = if (targetEnabled) "Enable Ghar App Lock" else "Turn Off Ghar App Lock",
      subtitle = if (targetEnabled) {
        "Confirm your fingerprint or screen lock to protect your household balances"
      } else {
        "Confirm your fingerprint or screen lock to disable App Lock"
      },
      onAuthenticated = {
        scope.launch {
          container.preferencesRepository.setAppLockEnabled(targetEnabled)
          securityFeedback = if (targetEnabled) {
            "✓ App Lock active. Balances are also shielded in Recent Apps."
          } else {
            "App Lock turned off."
          }
        }
      },
      onErrorOrCancel = { errMsg ->
        if (!errMsg.isNullOrBlank()) {
          securityFeedback = errMsg
        }
      }
    )
  }

  Surface(
    shape = PebbleCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.2.dp, PastelPlum.copy(alpha = 0.42f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("balcony_security_widget_card")
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      // Section 1: Optional Biometric / PIN App Lock
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(PastelPlum.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Fingerprint,
              contentDescription = null,
              tint = PastelPlum.readableAccentOn(MaterialTheme.colorScheme.surface),
              modifier = Modifier.size(21.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (plainEnglish) {
                stringResource(R.string.biometric_lock_title)
              } else {
                "Ghar Kundi • Biometric Lock"
              },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = stringResource(R.string.biometric_lock_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
          checked = appLockEnabled,
          onCheckedChange = { wantEnabled -> handleToggleAppLock(wantEnabled) },
          colors = SwitchDefaults.colors(
            checkedThumbColor = PastelPlum.contrastingContentColor(),
            checkedTrackColor = PastelPlum
          ),
          modifier = Modifier.testTag("biometric_app_lock_switch")
        )
      }

      securityFeedback?.let { msg ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = msg,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      Spacer(modifier = Modifier.height(16.dp))

      // Section 2: 2x2 Home Screen Quick Stamp Widget (Jetpack Glance)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(PastelTerracotta.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Widgets,
            contentDescription = null,
            tint = PastelTerracotta.readableAccentOn(MaterialTheme.colorScheme.surface),
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (plainEnglish) {
              "2×2 Home Screen Quick Widget"
            } else {
              "2×2 Quick Stamp Home Widget"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = if (plainEnglish) {
              "Live Safe-to-Spend balance + 1-tap Quick Stamps & Expense entry on your launcher"
            } else {
              "Live Safe-to-Spend balance + 1-tap Quick Stamps & Kharcha entry on your launcher"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Button(
        onClick = {
          GharQuickStampWidgetUpdater.enqueueUpdate(context)
          val pinResult = GharQuickStampWidgetUpdater.requestPinToHomeScreen(context)
          widgetFeedback = when (pinResult) {
            PinWidgetResult.PIN_PROMPT_SHOWN ->
              "✓ Confirm in your launcher prompt to place the 2×2 Ghar Quick Stamp widget."
            PinWidgetResult.UNSUPPORTED_BY_LAUNCHER ->
              "Long-press an empty spot on your Home Screen → Widgets → Ghar Quick Stamp (2×2)."
          }
        },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = PastelSky.copy(alpha = 0.22f),
          contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 48.dp)
          .testTag("pin_quick_stamp_widget_button")
      ) {
        Icon(
          imageVector = Icons.Default.Widgets,
          contentDescription = null,
          tint = PastelSky.readableAccentOn(MaterialTheme.colorScheme.surface),
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.widget_pin_action),
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      widgetFeedback?.let { msg ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = msg,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
