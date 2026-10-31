package com.example.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.AppContainer
import com.example.GharApplication
import com.example.MainActivity
import com.example.capture.CaptureEventBus
import com.example.core.audio.GharSoundEffects
import com.example.core.engine.HouseholdAnalyticsEngine
import com.example.core.engine.SafeToSpendCalculator
import com.example.core.model.CaptureSource
import com.example.core.model.QuickChip
import com.example.core.model.Transaction
import com.example.core.util.CurrencyFormatter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val KEY_WIDGET_REVISION = longPreferencesKey("ghar_widget_revision_ms")
private val PARAM_CHIP_ID = ActionParameters.Key<String>("ghar_stamp_chip_id")

private data class QuickStampWidgetModel(
  val headerTitle: String,
  val safeDailyFormatted: String,
  val statusSubtitle: String,
  val isTightOrHot: Boolean,
  val addKharchaLabel: String,
  val quickStamps: List<QuickChip>
)

class GharQuickStampWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = GharQuickStampWidget()
}

class GharQuickStampWidget : GlanceAppWidget() {

  override val stateDefinition = PreferencesGlanceStateDefinition

  override val sizeMode: SizeMode = SizeMode.Responsive(
    setOf(
      DpSize(110.dp, 110.dp),
      DpSize(180.dp, 110.dp),
      DpSize(250.dp, 120.dp)
    )
  )

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    provideContent {
      // Read widget revision key inside composition so pushUpdateNow triggers immediate recomposition
      val widgetPrefs = currentState<Preferences>()
      val revision = widgetPrefs[KEY_WIDGET_REVISION] ?: 0L
      val model = loadWidgetModel(context, revision)
      GlanceTheme {
        QuickStampWidgetContent(context = context, model = model)
      }
    }
  }
}

@Composable
private fun QuickStampWidgetContent(
  context: Context,
  model: QuickStampWidgetModel
) {
  val size = LocalSize.current
  val isWide = size.width >= 220.dp
  val showStampsRow = size.height >= 105.dp && model.quickStamps.isNotEmpty()

  val bgSurface = ColorProvider(
    day = Color(0xFFFFFBF4),
    night = Color(0xFF231B17)
  )
  val primaryTerracotta = ColorProvider(
    day = Color(0xFFB85233),
    night = Color(0xFFF09A7E)
  )
  val buttonTerracottaBg = ColorProvider(
    day = Color(0xFFB85233),
    night = Color(0xFFD66A49)
  )
  val buttonOnTerracotta = ColorProvider(
    day = Color(0xFFFFFBF6),
    night = Color(0xFFFFFBF6)
  )
  val stampPillBg = ColorProvider(
    day = Color(0xFFF4E7D5),
    night = Color(0xFF362A24)
  )
  val textPrimary = ColorProvider(
    day = Color(0xFF2D1E17),
    night = Color(0xFFF9EFE6)
  )
  val textMuted = ColorProvider(
    day = Color(0xFF755D50),
    night = Color(0xFFCBB6A8)
  )
  val statusTone = if (model.isTightOrHot) {
    ColorProvider(day = Color(0xFFB33F2B), night = Color(0xFFFF9E8A))
  } else {
    ColorProvider(day = Color(0xFF3E6E4C), night = Color(0xFF9CD4AC))
  }

  val openHomeIntent = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
  }
  val openQuickKharchaIntent = Intent(context, MainActivity::class.java).apply {
    action = CaptureEventBus.EXTRA_OPEN_QUICK_KHARCHA
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    putExtra(CaptureEventBus.EXTRA_OPEN_QUICK_KHARCHA, true)
  }

  Column(
    modifier = GlanceModifier
      .fillMaxSize()
      .cornerRadius(22.dp)
      .background(bgSurface)
      .padding(12.dp),
    verticalAlignment = Alignment.Top,
    horizontalAlignment = Alignment.Start
  ) {
    // Top Header + Balance Block (tapping opens Rasoi)
    Column(
      modifier = GlanceModifier
        .fillMaxWidth()
        .defaultWeight()
        .clickable(actionStartActivity(openHomeIntent)),
      verticalAlignment = Alignment.Top,
      horizontalAlignment = Alignment.Start
    ) {
      Text(
        text = model.headerTitle,
        maxLines = 1,
        style = TextStyle(
          color = primaryTerracotta,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      )
      Spacer(modifier = GlanceModifier.height(2.dp))
      Text(
        text = model.safeDailyFormatted,
        maxLines = 1,
        style = TextStyle(
          color = textPrimary,
          fontSize = if (isWide) 24.sp else 20.sp,
          fontWeight = FontWeight.Bold
        )
      )
      Text(
        text = model.statusSubtitle,
        maxLines = 1,
        style = TextStyle(
          color = statusTone,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      )
    }

    if (showStampsRow) {
      val visibleStamps = if (isWide) model.quickStamps.take(2) else model.quickStamps.take(1)
      Row(
        modifier = GlanceModifier
          .fillMaxWidth()
          .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        visibleStamps.forEachIndexed { index, stamp ->
          if (index > 0) {
            Spacer(modifier = GlanceModifier.width(6.dp))
          }
          Box(
            modifier = GlanceModifier
              .defaultWeight()
              .cornerRadius(12.dp)
              .background(stampPillBg)
              .clickable(
                actionRunCallback<LogQuickStampWidgetAction>(
                  actionParametersOf(PARAM_CHIP_ID to stamp.id)
                )
              )
              .padding(horizontal = 8.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${stamp.label} ₹${stamp.amountRupees}",
              maxLines = 1,
              style = TextStyle(
                color = textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              )
            )
          }
        }
      }
    } else {
      Spacer(modifier = GlanceModifier.height(4.dp))
    }

    // Primary 1-Tap Kharcha Entry Button
    Box(
      modifier = GlanceModifier
        .fillMaxWidth()
        .cornerRadius(14.dp)
        .background(buttonTerracottaBg)
        .clickable(actionStartActivity(openQuickKharchaIntent))
        .padding(vertical = 7.dp, horizontal = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = model.addKharchaLabel,
        maxLines = 1,
        style = TextStyle(
          color = buttonOnTerracotta,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      )
    }
  }
}

/**
 * Public zero-argument ActionCallback required by Jetpack Glance reflection instantiation.
 * Logs a 1-tap Quick Stamp directly from the Home Screen Widget and immediately refreshes Safe-to-Spend.
 */
class LogQuickStampWidgetAction : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters
  ) {
    val chipId = parameters[PARAM_CHIP_ID] ?: return
    val container = resolveActiveContainer(context) ?: return
    val prefs = container.preferencesRepository.prefsSnapshot.value
    val plainEnglish = prefs.plainEnglishMode

    val storedChips = container.quickChipRepository.quickChipsState.value
    val chips = storedChips.ifEmpty { QuickChip.defaultChips(plainEnglish) }
    val selected = chips.firstOrNull { it.id == chipId } ?: return

    val tx = Transaction(
      amountPaise = selected.amountPaise,
      direction = selected.direction,
      merchant = selected.merchant.ifBlank { selected.label },
      category = selected.category,
      source = CaptureSource.MANUAL
    )
    val gullakDelta = tx.gullakImpactPaise()
    container.transactionRepository.insert(tx)
    if (gullakDelta != 0L) {
      container.goalRepository.adjustGullakDelta(gullakDelta)
    }
    if (prefs.soundEnabled) {
      runCatching { GharSoundEffects.playGullakCoinClink(context, soundEnabled = true) }
    }
    CaptureEventBus.notifyNewTransaction(tx)
    GharQuickStampWidgetUpdater.pushUpdateNow(context)
  }
}

private fun resolveActiveContainer(context: Context): AppContainer? {
  val app = context.applicationContext as? GharApplication ?: return null
  val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
  return if (!uid.isNullOrBlank()) {
    app.rootContainer.forUser(uid)
  } else {
    app.rootContainer.guestContainer
  }
}

private fun loadWidgetModel(context: Context, @Suppress("UNUSED_PARAMETER") revision: Long): QuickStampWidgetModel {
  val container = resolveActiveContainer(context)
  if (container == null) {
    return QuickStampWidgetModel(
      headerTitle = "Ghar • Safe Today",
      safeDailyFormatted = "₹0",
      statusSubtitle = "Tap to open Ghar",
      isTightOrHot = false,
      addKharchaLabel = "+ Kharcha",
      quickStamps = emptyList()
    )
  }

  val prefs = container.preferencesRepository.prefsSnapshot.value
  val plainEnglish = prefs.plainEnglishMode
  val profile = container.budgetRepository.profileState.value
  val transactions = container.transactionRepository.transactionsState.value

  val spentSoFarPaise = transactions.filter { it.isExpense }.sumOf { it.amountPaise }
  val extraIncomePaise = transactions.filter { !it.isExpense }.sumOf { it.amountPaise }
  val safeToSpend = SafeToSpendCalculator.calculate(profile, spentSoFarPaise, extraIncomePaise)
  val spentTodayRupees = HouseholdAnalyticsEngine.calculateTodaySpendRupees(transactions)

  val storedChips = container.quickChipRepository.quickChipsState.value
  val stamps = (storedChips.ifEmpty { QuickChip.defaultChips(plainEnglish) })
    .filter { it.amountPaise > 0L }
    .take(2)

  val statusSubtitle = when {
    spentTodayRupees > 0L -> "${CurrencyFormatter.formatCompactRupees(spentTodayRupees)} spent today"
    safeToSpend.isTight -> if (plainEnglish) "Tight budget window" else "Tight Batua window"
    else -> if (plainEnglish) "Ready for today" else "Batua steady today"
  }

  return QuickStampWidgetModel(
    headerTitle = if (plainEnglish) "Ghar • Safe Today" else "Ghar • Aaj Safe",
    safeDailyFormatted = CurrencyFormatter.formatRupees(safeToSpend.dailyAllowanceRupees),
    statusSubtitle = statusSubtitle,
    isTightOrHot = safeToSpend.isTight || safeToSpend.isPaceHot,
    addKharchaLabel = if (plainEnglish) "+ Add Expense" else "+ Quick Kharcha",
    quickStamps = stamps
  )
}

enum class PinWidgetResult {
  PIN_PROMPT_SHOWN,
  UNSUPPORTED_BY_LAUNCHER
}

object GharQuickStampWidgetUpdater {
  private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  fun enqueueUpdate(context: Context) {
    val appCtx = context.applicationContext
    widgetScope.launch {
      pushUpdateNow(appCtx)
    }
  }

  suspend fun pushUpdateNow(context: Context) {
    runCatching {
      val appCtx = context.applicationContext
      val manager = GlanceAppWidgetManager(appCtx)
      val glanceIds = manager.getGlanceIds(GharQuickStampWidget::class.java)
      if (glanceIds.isEmpty()) return
      val now = System.currentTimeMillis()
      val widget = GharQuickStampWidget()
      for (glanceId in glanceIds) {
        updateAppWidgetState(appCtx, glanceId) { prefs ->
          prefs[KEY_WIDGET_REVISION] = now
        }
        widget.update(appCtx, glanceId)
      }
    }
  }

  fun requestPinToHomeScreen(context: Context): PinWidgetResult {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return PinWidgetResult.UNSUPPORTED_BY_LAUNCHER
    }
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (!appWidgetManager.isRequestPinAppWidgetSupported) {
      return PinWidgetResult.UNSUPPORTED_BY_LAUNCHER
    }
    val provider = ComponentName(context, GharQuickStampWidgetReceiver::class.java)
    val requested = runCatching {
      appWidgetManager.requestPinAppWidget(provider, null, null)
    }.getOrDefault(false)
    return if (requested) PinWidgetResult.PIN_PROMPT_SHOWN else PinWidgetResult.UNSUPPORTED_BY_LAUNCHER
  }
}
