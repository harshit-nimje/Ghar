package com.example.ui.balcony

import android.content.Intent
import android.os.Build
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppContainer
import com.example.R
import com.example.core.ai.MiniModelStatus
import com.example.core.audio.GharSoundEffects
import com.example.core.engine.ChitthiWisdomBank
import com.example.core.engine.HouseholdAnalyticsEngine
import com.example.core.engine.SafeToSpendCalculator
import com.example.core.localization.GharTerms
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.model.GuiltSensitivity
import com.example.core.model.MaaAiEngineMode
import com.example.core.model.PainDial
import com.example.core.util.CurrencyFormatter
import com.example.ui.auth.GoogleEmblemCanvas
import com.example.ui.auth.performInteractiveGoogleSignIn
import com.example.ui.common.BalconyCalmIllustration
import com.example.ui.common.ChitthiDabbaDialog
import com.example.ui.common.GharConfirmDialog
import com.example.ui.common.GharUndoController
import com.example.ui.common.GharUndoSnackbarHost
import com.example.ui.common.HandwrittenHomeNote
import com.example.ui.common.IllustratedRoomBanner
import com.example.ui.common.LocalGharUndoController
import com.example.ui.common.MaaAvatar
import com.example.ui.common.PebbleCardShape
import com.example.ui.common.SmartAlertsAndSmsDialog
import com.example.ui.common.StickyNotePaperColors
import com.example.ui.common.jharokhaCornerFiligree
import com.example.ui.common.pastelHomeBackdrop
import com.example.ui.common.rememberGharUndoController
import com.example.ui.kitchen.MaaMiniInstallerInlineCard
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelPlum
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelSky
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.contrastingContentColor
import com.example.ui.theme.readableAccentOn
import kotlin.random.Random
import kotlinx.coroutines.launch

@Composable
fun BalconyScreen(
  container: AppContainer,
  onResetToOnboarding: () -> Unit,
  listState: LazyListState = rememberLazyListState(),
  onSignOut: () -> Unit = {},
  onShowRoomGuide: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val hasParentUndo = LocalGharUndoController.current != null
  val fallbackUndo = rememberGharUndoController()
  val undoController = LocalGharUndoController.current ?: fallbackUndo

  val transactions by container.transactionRepository.transactionsState.collectAsStateWithLifecycle()
  val profile by container.budgetRepository.profileState.collectAsStateWithLifecycle()
  val goal by container.goalRepository.primaryGoalState.collectAsStateWithLifecycle()
  val allGoals by container.goalRepository.allGoalsState.collectAsStateWithLifecycle()
  val stashedChitthis by container.chitthiRepository.chitthisState.collectAsStateWithLifecycle()
  val cloudUserName by container.budgetRepository.userNameState.collectAsStateWithLifecycle()
  val prefs by container.preferencesRepository.prefsSnapshot.collectAsStateWithLifecycle()
  val miniModelStatus by container.maaMiniModelManager.status.collectAsStateWithLifecycle()

  val plainEnglish = LocalPlainEnglishMode.current || prefs.plainEnglishMode
  val maaName = GharTerms.maaName(plainEnglish)
  val papaName = GharTerms.papaName(plainEnglish)
  val themeMode = prefs.themeMode
  val useDynamicColor = prefs.useDynamicColor
  val soundEnabled = prefs.soundEnabled
  val hapticsEnabled = prefs.isHapticsEnabled
  val maaEngineMode = prefs.maaAiEngineMode
  val guiltSensitivity = profile.guiltSensitivity
  val painDial = profile.painDial

  var showChitthiDabbaDialog by remember { mutableStateOf(false) }
  var showSmartAlertsDialog by remember { mutableStateOf(false) }
  var showSignOutConfirm by remember { mutableStateOf(false) }
  var showRestartOnboardingConfirm by remember { mutableStateOf(false) }
  var eveningQuoteIndex by remember {
    mutableIntStateOf(Random.nextInt(ChitthiWisdomBank.getMaaSeeds(plainEnglish).size))
  }
  val activeEveningSeed = remember(eveningQuoteIndex, plainEnglish) {
    ChitthiWisdomBank.pickMaaSeed(eveningQuoteIndex, plainEnglish)
  }
  val isEveningSeedStashed = remember(stashedChitthis, activeEveningSeed.id) {
    stashedChitthis.any { it.id == activeEveningSeed.id }
  }

  val resolvedName = remember(cloudUserName, prefs.userName) {
    val cleanCloud = cloudUserName.trim()
    val cleanPref = prefs.userName.trim()
    when {
      cleanCloud.isNotBlank() && cleanCloud != "Friend" -> cleanCloud
      cleanPref.isNotBlank() -> cleanPref
      else -> cleanCloud
    }
  }
  var isConnectingGoogle by remember { mutableStateOf(false) }
  var googleSignInMessage by remember { mutableStateOf<String?>(null) }

  val totalSavedAcrossPotsRupees = remember(allGoals) {
    allGoals.sumOf { it.currentAmountRupees }
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
  val quietStreakDays = remember(transactions, safeToSpend.dailyAllowanceRupees) {
    HouseholdAnalyticsEngine.calculateQuietStreakDays(transactions, safeToSpend.dailyAllowanceRupees)
  }
  val savingsRatePercent = remember(profile) {
    if (profile.monthlyIncomeRupees <= 0L) 0
    else ((profile.savingsTargetRupees * 100L) / profile.monthlyIncomeRupees).toInt().coerceIn(0, 100)
  }

  fun exportHouseholdSummary() {
    val summaryText = HouseholdAnalyticsEngine.buildShareableMonthlySummary(
      userName = resolvedName.ifBlank { "Friend" },
      profile = profile,
      safeToSpend = safeToSpend,
      transactions = transactions,
      goals = allGoals,
      goal = goal,
      plainEnglish = plainEnglish
    )
    val subject = if (plainEnglish) "Monthly Budget Summary" else "Ghar Household Khata Summary"
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
        primaryWash = PastelPlum,
        secondaryWash = PastelHoney,
        tertiaryWash = PastelSage
      )
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item(key = "balcony_banner", contentType = "banner") {
        IllustratedRoomBanner(
          artworkRes = R.drawable.img_balcony_dusk_1792931332537,
          roomBadge = if (plainEnglish) "Settings • Alerts & Preferences" else "Balcony • Settings & Alerts",
          title = if (plainEnglish) "Settings & Summary" else "Dusk on the Balcony",
          keyHighlight = "${CurrencyFormatter.formatCompactRupees(totalSavedAcrossPotsRupees)} saved across ${allGoals.size} ${if (allGoals.size == 1) "goal" else "goals"}",
          accentColor = PastelPlum,
          heroHeight = 280.dp,
          scrollOffsetProvider = {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 600
          },
          onHelpClick = onShowRoomGuide
        )
      }

      // Plain English / Non-Slang Mode Card (Top Priority for Clarity)
      item(key = "balcony_plain_english_mode", contentType = "language") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          Surface(
            shape = PebbleCardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.2.dp, PastelSky.copy(alpha = 0.45f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("plain_english_mode_card")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
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
                    .background(PastelSky.copy(alpha = 0.16f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = PastelSky,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Language",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = if (plainEnglish) {
                      "Plain English mode (no Hinglish)"
                    } else {
                      "Indian household terms (Gullak, Batua, Rasoi)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Switch(
                checked = plainEnglish,
                onCheckedChange = { enabled ->
                  coroutineScope.launch {
                    container.preferencesRepository.setPlainEnglishMode(enabled)
                  }
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = PastelSky.contrastingContentColor(),
                  checkedTrackColor = PastelSky
                ),
                modifier = Modifier.testTag("plain_english_mode_switch")
              )
            }
          }
        }
      }

      // Smart SMS Reader, Timed Daily Routines & Contextual Nudges Hub Card
      item(key = "balcony_smart_alerts", contentType = "smart_alerts") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          Surface(
            shape = PebbleCardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.2.dp, PastelTerracotta.copy(alpha = 0.42f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("balcony_smart_alerts_card")
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PastelTerracotta.copy(alpha = 0.14f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = PastelTerracotta,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Daily Routines, Smart Nudges & SMS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Schedule daily reminder times, smart daytime nudges, and offline bank SMS detection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                val activeRoutines = remember(prefs.dailyRoutines) {
                  prefs.dailyRoutines.filter { it.enabled }
                }
                val routineSummaryValue = when {
                  activeRoutines.isEmpty() -> "Off"
                  activeRoutines.size == 1 -> activeRoutines.first().formattedTime()
                  else -> "${activeRoutines.size} active"
                }
                PulseStatPill(
                  label = "Daily Routines",
                  value = routineSummaryValue,
                  tint = if (activeRoutines.isNotEmpty()) PastelSage else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.weight(1f)
                )
                PulseStatPill(
                  label = "Smart Nudges",
                  value = if (prefs.smartRandomNudgesEnabled) "Auto" else "Off",
                  tint = if (prefs.smartRandomNudgesEnabled) PastelSky else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.weight(1f)
                )
                val detectedBankPaise = prefs.lastDetectedBankBalancePaise
                PulseStatPill(
                  label = "SMS Bank Bal",
                  value = if (detectedBankPaise != null && detectedBankPaise > 0L) {
                    CurrencyFormatter.formatCompactRupees(detectedBankPaise / 100L)
                  } else {
                    "—"
                  },
                  tint = PastelTerracotta,
                  modifier = Modifier.weight(1f)
                )
              }

              Spacer(modifier = Modifier.height(12.dp))

              Button(
                onClick = { showSmartAlertsDialog = true },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = 48.dp)
                  .testTag("balcony_open_smart_alerts_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Sms,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Manage Daily Routines & SMS Reader",
                  color = MaterialTheme.colorScheme.onPrimary,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }

      // Direct Factual Budget Health Summary + Export
      item(key = "balcony_health_pulse", contentType = "pulse") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          Surface(
            shape = PebbleCardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.35f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("balcony_health_pulse_card")
          ) {
            Column(
              modifier = Modifier
                .jharokhaCornerFiligree(accentColor = PastelPlum)
                .padding(18.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = PastelSage,
                    modifier = Modifier.size(19.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Budget Health Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }

                Surface(
                  shape = CircleShape,
                  color = PastelSage.copy(alpha = 0.16f),
                  modifier = Modifier
                    .defaultMinSize(minHeight = 36.dp)
                    .clip(CircleShape)
                    .clickable { exportHouseholdSummary() }
                    .testTag("export_khata_summary_button")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Share,
                      contentDescription = "Export Monthly Summary",
                      tint = PastelSage,
                      modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                      text = if (plainEnglish) "Export Summary" else "Export Khata",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = PastelSage
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                PulseStatPill(
                  label = "Savings Rate",
                  value = "$savingsRatePercent%",
                  tint = PastelSage,
                  modifier = Modifier.weight(1f)
                )
                PulseStatPill(
                  label = "Days Under Budget",
                  value = "${quietStreakDays}d",
                  tint = PastelHoney,
                  modifier = Modifier.weight(1f)
                )
                PulseStatPill(
                  label = if (plainEnglish) "Total Saved" else "Gullak Total",
                  value = CurrencyFormatter.formatCompactRupees(totalSavedAcrossPotsRupees),
                  tint = PastelPlum,
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }
      }

      // Mom / Maa's Interactive Reflection & Saved Notes Access
      item(key = "balcony_chitthi", contentType = "chitthi") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          HandwrittenHomeNote(
            isMintPaper = false,
            tiltDegrees = -0.6f,
            modifier = Modifier.testTag("balcony_review_card")
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
                  MaaAvatar(size = 34.dp)
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = activeEveningSeed.title,
                      style = GharHandStyles.stickyNoteTitle,
                      color = StickyNotePaperColors.InkAccentRed,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = if (plainEnglish) {
                        "$maaName's note • ${activeEveningSeed.tag}"
                      } else {
                        "Maa's evening chitthi • ${activeEveningSeed.tag}"
                      },
                      style = GharHandStyles.stickyNoteMeta,
                      color = StickyNotePaperColors.InkMuted,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = {
                      val listSize = ChitthiWisdomBank.getMaaSeeds(plainEnglish).size
                      eveningQuoteIndex = (eveningQuoteIndex + 1) % listSize
                    },
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("cycle_balcony_chitthi_button")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Refresh,
                      contentDescription = "Next note from $maaName",
                      tint = StickyNotePaperColors.InkMuted,
                      modifier = Modifier.size(19.dp)
                    )
                  }
                  IconButton(
                    onClick = {
                      if (isEveningSeedStashed) {
                        container.chitthiRepository.remove(activeEveningSeed.id)
                      } else {
                        val note = activeEveningSeed.toChitthiNote()
                        container.chitthiRepository.stash(note)
                      }
                    },
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("stash_balcony_chitthi_button")
                  ) {
                    Icon(
                      imageVector = if (isEveningSeedStashed) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                      contentDescription = "Save note",
                      tint = if (isEveningSeedStashed) StickyNotePaperColors.InkAccentSage else StickyNotePaperColors.InkAccentRed,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
              }

              BalconyCalmIllustration()

              Text(
                text = "“${activeEveningSeed.body}”",
                style = GharHandStyles.stickyNoteBody,
                color = StickyNotePaperColors.InkDark
              )

              Spacer(modifier = Modifier.height(12.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = Color(0xFFF3DB7B).copy(alpha = 0.55f),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    Text(
                      text = if (plainEnglish) "Transactions" else "Khata Entries",
                      style = GharHandStyles.stickyNoteMeta,
                      color = StickyNotePaperColors.InkMuted,
                      maxLines = 1
                    )
                    Text(
                      text = "${transactions.size}",
                      style = GharHandStyles.moneyMedium,
                      color = StickyNotePaperColors.InkDark,
                      maxLines = 1
                    )
                  }
                }
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = Color(0xFFD6ECC8).copy(alpha = 0.72f),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    Text(
                      text = if (plainEnglish) "Total Saved" else "In Gullak",
                      style = GharHandStyles.stickyNoteMeta,
                      color = StickyNotePaperColors.InkMuted,
                      maxLines = 1
                    )
                    Text(
                      text = CurrencyFormatter.formatCompactRupees(totalSavedAcrossPotsRupees),
                      style = GharHandStyles.moneyMedium,
                      color = StickyNotePaperColors.InkAccentSage,
                      maxLines = 1
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Button(
                onClick = { showChitthiDabbaDialog = true },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = StickyNotePaperColors.InkAccentRed),
                modifier = Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = 48.dp)
                  .testTag("open_balcony_chitthis_button")
              ) {
                Icon(
                  imageVector = Icons.Default.BookmarkAdded,
                  contentDescription = null,
                  tint = Color(0xFFFFFBF2),
                  modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val collectionLabel = GharTerms.notesCollectionName(plainEnglish)
                Text(
                  text = if (stashedChitthis.isNotEmpty()) {
                    "Open $collectionLabel (${stashedChitthis.size} saved)"
                  } else {
                    "Open $collectionLabel"
                  },
                  color = Color(0xFFFFFBF2),
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }

      // Theme & Material Palette Card
      item(key = "balcony_theme", contentType = "theme") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          Surface(
            shape = PebbleCardShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("theme_selector_card")
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (themeMode == "DARK") Icons.Default.DarkMode else Icons.Default.LightMode,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = if (plainEnglish) "Theme & Colors" else "Home Lighting & Colors",
                  style = MaterialTheme.typography.titleLarge,
                  fontSize = 18.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Spacer(modifier = Modifier.height(14.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                val modes = if (plainEnglish) {
                  listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark")
                } else {
                  listOf("SYSTEM" to "Natural", "LIGHT" to "Sunlit", "DARK" to "Moonlit")
                }
                modes.forEach { (mode, label) ->
                  val isSelected = themeMode.equals(mode, ignoreCase = true)
                  Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                      .weight(1f)
                      .defaultMinSize(minHeight = 46.dp)
                      .clip(CircleShape)
                      .clickable {
                        if (!isSelected) {
                          coroutineScope.launch {
                            container.preferencesRepository.setThemeMode(mode)
                          }
                        }
                      }
                      .testTag("theme_mode_$mode")
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                      )
                    }
                  }
                }
              }

              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PastelTerracotta.copy(alpha = 0.09f))
                    .padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = "Dynamic",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = if (useDynamicColor) "Matching your phone wallpaper" else "Warm terracotta & sage palette",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  Spacer(modifier = Modifier.width(8.dp))

                  Switch(
                    checked = useDynamicColor,
                    onCheckedChange = { checked ->
                      coroutineScope.launch {
                        container.preferencesRepository.setUseDynamicColor(checked)
                      }
                    },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                      checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("dynamic_color_switch")
                  )
                }
              }
            }
          }
        }
      }

      // Guidance Tone & Cash Feedback Card
      item(key = "balcony_voices", contentType = "voices") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          Surface(
            shape = PebbleCardShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("balcony_settings_card")
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Tune,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (plainEnglish) "Guidance Tone & Feedback" else "Household Voices & Feel",
                  style = MaterialTheme.typography.titleLarge,
                  fontSize = 18.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                text = "$maaName's Guidance Tone:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                GuiltSensitivity.entries.forEach { sens ->
                  val isSelected = sens == guiltSensitivity
                  val chipText = when (sens) {
                    GuiltSensitivity.LOW -> "Firm"
                    GuiltSensitivity.MEDIUM -> "Balanced"
                    GuiltSensitivity.HIGH -> "Gentle"
                    GuiltSensitivity.NONE -> "Off"
                  }
                  Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                      .weight(1f)
                      .defaultMinSize(minHeight = 48.dp)
                      .clip(CircleShape)
                      .clickable {
                        if (!isSelected) {
                          container.budgetRepository.saveProfile(profile.copy(guiltSensitivity = sens))
                          container.appScope.launch {
                            container.preferencesRepository.setGuiltSensitivity(sens)
                          }
                        }
                      }
                  ) {
                    Box(
                      modifier = Modifier.padding(horizontal = 4.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = chipText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = when (guiltSensitivity) {
                  GuiltSensitivity.LOW -> "Direct budget nudges for every spend."
                  GuiltSensitivity.MEDIUM -> "Helpful guidance when nearing limits."
                  GuiltSensitivity.HIGH -> "Warm, gentle reminders only."
                  GuiltSensitivity.NONE -> "No proactive budget insights."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
              )

              Spacer(modifier = Modifier.height(20.dp))

              Text(
                text = "Cash Animation Intensity:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                PainDial.entries.forEach { dial ->
                  val isSelected = dial == painDial
                  val dialText = when (dial) {
                    PainDial.SUBTLE -> "Subtle"
                    PainDial.BALANCED -> "Balanced"
                    PainDial.PUNCHY -> "Punchy"
                  }
                  Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                      .weight(1f)
                      .defaultMinSize(minHeight = 48.dp)
                      .clip(CircleShape)
                      .clickable {
                        if (!isSelected) {
                          container.budgetRepository.saveProfile(profile.copy(painDial = dial))
                          container.appScope.launch {
                            container.preferencesRepository.setPainDial(dial)
                          }
                        }
                      }
                  ) {
                    Box(
                      modifier = Modifier.padding(horizontal = 4.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = dialText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = when (painDial) {
                  PainDial.SUBTLE -> "Quiet confirmation without flying cash."
                  PainDial.BALANCED -> "Satisfying paper cash visual feedback."
                  PainDial.PUNCHY -> "Tactile cash cascade and strong haptics."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
              )

              Spacer(modifier = Modifier.height(18.dp))

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
                    imageVector = if (soundEnabled) {
                      Icons.AutoMirrored.Filled.VolumeUp
                    } else {
                      Icons.AutoMirrored.Filled.VolumeOff
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Sounds",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = if (plainEnglish) {
                        "Door latch, coin clink, cash notes & chai cup"
                      } else {
                        "Rasoi door kundi, Gullak clink, Batua notes & chai cup"
                      },
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                  checked = soundEnabled,
                  onCheckedChange = { enabled ->
                    coroutineScope.launch {
                      container.preferencesRepository.setSoundEnabled(enabled)
                    }
                    if (enabled) {
                      GharSoundEffects.playGullakCoinClink(context, soundEnabled = true)
                    }
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                  ),
                  modifier = Modifier.testTag("sound_effects_switch")
                )
              }

              Spacer(modifier = Modifier.height(14.dp))

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
                    imageVector = Icons.Default.Vibration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Haptics",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Tactile vibration feedback on adding expenses or savings",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                  checked = hapticsEnabled,
                  onCheckedChange = { enabled ->
                    coroutineScope.launch {
                      container.preferencesRepository.setHapticsEnabled(enabled)
                    }
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                  )
                )
              }
            }
          }
        }
      }

      // AI Engine Card
      item(key = "balcony_ai", contentType = "ai") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          val isMiniInstalled = miniModelStatus is MiniModelStatus.Installed
          Surface(
            shape = PebbleCardShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.40f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("balcony_ai_model_card")
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Memory,
                  contentDescription = null,
                  tint = PastelSage,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (plainEnglish) {
                      "Household AI Engine ($maaName & $papaName)"
                    } else {
                      "Family AI Engine ($maaName & $papaName)"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "Cloud Gemini + one shared 492 MB offline 0.5B LLM powering both $maaName & $papaName with their own traits",
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
                val isCloudSelected = maaEngineMode == MaaAiEngineMode.CLOUD_AUTO
                Surface(
                  shape = CircleShape,
                  color = if (isCloudSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(CircleShape)
                    .clickable {
                      if (!isCloudSelected) {
                        coroutineScope.launch {
                          container.preferencesRepository.setMaaAiEngineMode(MaaAiEngineMode.CLOUD_AUTO)
                        }
                      }
                    }
                    .testTag("balcony_engine_cloud_button")
                ) {
                  Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "Cloud (Auto-Offline)",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = if (isCloudSelected) FontWeight.Bold else FontWeight.Medium,
                      color = if (isCloudSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }

                val isMiniSelected = maaEngineMode == MaaAiEngineMode.MINI_LOCAL
                val sagePillContent = PastelSage.contrastingContentColor()
                Surface(
                  shape = CircleShape,
                  color = if (isMiniSelected) PastelSage else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(CircleShape)
                    .clickable {
                      if (!isMiniSelected) {
                        coroutineScope.launch {
                          container.preferencesRepository.setMaaAiEngineMode(MaaAiEngineMode.MINI_LOCAL)
                        }
                      }
                    }
                    .testTag("balcony_engine_mini_button")
                ) {
                  Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = if (isMiniInstalled) "Local 0.5B (492 MB)" else "Local 0.5B (Offline)",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = if (isMiniSelected) FontWeight.Bold else FontWeight.Medium,
                      color = if (isMiniSelected) sagePillContent else MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              MaaMiniInstallerInlineCard(
                miniStatus = miniModelStatus,
                isActiveMode = maaEngineMode == MaaAiEngineMode.MINI_LOCAL,
                onInstallAndActivate = {
                  coroutineScope.launch {
                    val res = container.maaMiniModelManager.installModel()
                    if (res.isSuccess) {
                      container.preferencesRepository.setMaaAiEngineMode(MaaAiEngineMode.MINI_LOCAL)
                    }
                  }
                },
                onCancelInstall = {
                  container.maaMiniModelManager.cancelInstall()
                },
                onActivateInstalled = {
                  coroutineScope.launch {
                    container.preferencesRepository.setMaaAiEngineMode(MaaAiEngineMode.MINI_LOCAL)
                  }
                },
                onUninstall = {
                  coroutineScope.launch {
                    container.preferencesRepository.setMaaAiEngineMode(MaaAiEngineMode.CLOUD_AUTO)
                    container.maaMiniModelManager.uninstallModel()
                  }
                }
              )
            }
          }
        }
      }

      // Household Name & Google Cloud Backup Card (leaf-isolated text input)
      item(key = "balcony_account", contentType = "account") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          Surface(
            shape = PebbleCardShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
              1.dp,
              if (container.isCloudAuthenticated) PastelSage.copy(alpha = 0.45f)
              else PastelTerracotta.copy(alpha = 0.38f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("household_account_card")
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (container.isCloudAuthenticated) Icons.Default.CloudDone else Icons.Default.Smartphone,
                  contentDescription = null,
                  tint = if (container.isCloudAuthenticated) PastelSage else PastelTerracotta,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (container.isCloudAuthenticated) {
                      "Profile Name & Cloud Backup"
                    } else {
                      "Profile Name & Backup"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = if (container.isCloudAuthenticated) {
                      "Synced via Google (${container.userEmailOrDisplayName ?: "Connected"})"
                    } else {
                      "Guest Mode • Saved locally on this phone"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              HouseholdNameEditorSection(
                userId = container.userId,
                resolvedName = resolvedName,
                plainEnglish = plainEnglish,
                onSaveName = { trimmed ->
                  container.budgetRepository.updateUserName(trimmed)
                  container.appScope.launch {
                    container.preferencesRepository.setUserName(trimmed)
                  }
                }
              )

              Spacer(modifier = Modifier.height(14.dp))

              if (container.isCloudAuthenticated) {
                Button(
                  onClick = { showSignOutConfirm = true },
                  shape = CircleShape,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = PastelTerracotta.copy(alpha = 0.16f),
                    contentColor = MaterialTheme.colorScheme.primary
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 50.dp)
                    .testTag("sign_out_button")
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Sign Out (Switch to Guest Mode)",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              } else {
                Text(
                  text = if (plainEnglish) {
                    "Back up your daily budget, savings goals, notes, and shortcuts across devices with Google—your Guest data migrates automatically."
                  } else {
                    "Back up your Batua, Gullak, Chitthis, and shortcuts across phones with Google—your Guest Khata migrates automatically."
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                  onClick = {
                    if (isConnectingGoogle) return@Button
                    googleSignInMessage = null
                    isConnectingGoogle = true
                    coroutineScope.launch {
                      val res = performInteractiveGoogleSignIn(context)
                      isConnectingGoogle = false
                      res.fold(
                        onSuccess = {
                          container.appScope.launch {
                            container.preferencesRepository.setGuestModeChosen(false)
                            container.preferencesRepository.setOnboardingCompleted(true)
                          }
                          googleSignInMessage = "Connected! Syncing your data to Google Cloud."
                        },
                        onFailure = { err ->
                          if (err !is GetCredentialCancellationException) {
                            googleSignInMessage = err.localizedMessage
                              ?: "Could not connect Google right now. Your local Guest data stays safe on this phone."
                          }
                        }
                      )
                    }
                  },
                  enabled = !isConnectingGoogle,
                  shape = CircleShape,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .testTag("balcony_google_sign_in_button")
                ) {
                  if (isConnectingGoogle) {
                    CircularProgressIndicator(
                      color = MaterialTheme.colorScheme.onPrimary,
                      strokeWidth = 2.2.dp,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = "Connecting Google Vault...",
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  } else {
                    Surface(
                      shape = CircleShape,
                      color = Color.White,
                      modifier = Modifier.size(24.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        GoogleEmblemCanvas(modifier = Modifier.size(14.dp))
                      }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = "Sign in with Google & Back Up",
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }

                googleSignInMessage?.let { msg ->
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
        }
      }

      // Restart Setup / Onboarding
      item(key = "balcony_restart", contentType = "restart") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          Button(
            onClick = { showRestartOnboardingConfirm = true },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 50.dp)
              .testTag("restart_onboarding_button")
          ) {
            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (plainEnglish) "Run Initial Setup Again" else "Walk Back to the Front Door",
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }

    if (showSignOutConfirm) {
      GharConfirmDialog(
        title = "Sign Out of Cloud Backup?",
        message = "You will switch back to local Guest Mode on this device. Your synced household data remains safely stored in your Google account for whenever you sign back in.",
        confirmLabel = "Sign Out",
        onConfirm = {
          showSignOutConfirm = false
          onSignOut()
        },
        onDismiss = { showSignOutConfirm = false },
        dialogTestTag = "confirm_sign_out_dialog",
        confirmTestTag = "confirm_sign_out_button"
      )
    }

    if (showRestartOnboardingConfirm) {
      GharConfirmDialog(
        title = "Redo Initial Setup?",
        message = "This reopens the household setup flow (including permissions and room intro guides) so you can recalibrate your monthly income, bills, and savings target.",
        confirmLabel = "Redo Setup",
        isDestructive = false,
        onConfirm = {
          showRestartOnboardingConfirm = false
          container.budgetRepository.setOnboardingCompleted(false)
          container.appScope.launch {
            container.preferencesRepository.resetOnboardingForTesting()
            container.preferencesRepository.resetSeenRoomSplashes()
          }
          onResetToOnboarding()
        },
        onDismiss = { showRestartOnboardingConfirm = false },
        dialogTestTag = "confirm_restart_onboarding_dialog",
        confirmTestTag = "confirm_restart_onboarding_button"
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

    if (showSmartAlertsDialog) {
      SmartAlertsAndSmsDialog(
        container = container,
        onDismiss = { showSmartAlertsDialog = false }
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
private fun HouseholdNameEditorSection(
  userId: String,
  resolvedName: String,
  plainEnglish: Boolean,
  onSaveName: (String) -> Unit
) {
  val maaName = GharTerms.maaName(plainEnglish)
  var editableName by remember(userId) { mutableStateOf(resolvedName) }
  var nameSavedFeedback by remember { mutableStateOf(false) }

  LaunchedEffect(resolvedName) {
    if (editableName.isBlank() && resolvedName.isNotBlank()) {
      editableName = resolvedName
    }
  }

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = editableName,
        onValueChange = {
          editableName = it.take(60)
          nameSavedFeedback = false
        },
        label = { Text(if (plainEnglish) "Your display name" else "What Maa calls you") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        modifier = Modifier
          .weight(1f)
          .testTag("balcony_name_input")
      )
      Spacer(modifier = Modifier.width(8.dp))
      Button(
        onClick = {
          val trimmed = editableName.trim()
          if (trimmed.isNotBlank()) {
            onSaveName(trimmed)
            nameSavedFeedback = true
          }
        },
        enabled = editableName.trim().isNotBlank(),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag("save_household_name_button")
      ) {
        Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
      }
    }

    if (nameSavedFeedback) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "✓ $maaName updated your greeting name.",
        style = GharHandStyles.stickyNoteMeta,
        color = PastelSage.readableAccentOn(MaterialTheme.colorScheme.surface)
      )
    }
  }
}

@Composable
private fun PulseStatPill(
  label: String,
  value: String,
  tint: Color,
  modifier: Modifier = Modifier
) {
  val readableTint = tint.readableAccentOn(MaterialTheme.colorScheme.surface)
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = tint.copy(alpha = 0.13f),
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
        color = readableTint,
        fontWeight = FontWeight.Bold,
        maxLines = 1
      )
    }
  }
}
