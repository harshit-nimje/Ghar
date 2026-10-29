package com.example.capture

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.AppContainer
import com.example.MainActivity
import com.example.R
import com.example.core.engine.HouseholdAnalyticsEngine
import com.example.core.engine.SafeToSpendCalculator
import com.example.core.model.BudgetProfile
import com.example.core.model.Category
import com.example.core.model.PakkaCommitment
import com.example.core.model.SavingsGoal
import com.example.core.model.Transaction
import com.example.core.util.CurrencyFormatter
import com.example.core.util.DateUtils
import com.example.data.preferences.DailyReminderRoutine
import com.example.data.preferences.RoutineKind
import java.util.Calendar
import kotlin.math.abs
import kotlin.random.Random

data class SmartContextualNudge(
  val key: String,
  val title: String,
  val body: String
)

/**
 * Handles Timed Daily Reminder Routines, Smart Randomized Contextual Household Nudges,
 * Auto-Captured SMS/UPI Balance Updates, and notification channel lifecycle.
 */
object HouseholdNotificationHelper {

  const val CHANNEL_BALANCE = "ghar_balance_updates"
  const val CHANNEL_REMINDERS = "ghar_daily_reminders"
  const val CHANNEL_SMART_NUDGES = "ghar_smart_contextual_nudges"
  const val CHANNEL_CUSTOM = "ghar_custom_alerts"

  const val ACTION_DAILY_LOG_REMINDER = "com.example.action.DAILY_LOG_REMINDER"
  const val ACTION_ROUTINE_REMINDER = "com.example.action.ROUTINE_REMINDER"
  const val ACTION_SMART_CONTEXTUAL_NUDGE = "com.example.action.SMART_CONTEXTUAL_NUDGE"
  const val ACTION_CUSTOM_REMINDER = "com.example.action.CUSTOM_REMINDER"

  const val EXTRA_CUSTOM_TITLE = "extra_custom_title"
  const val EXTRA_CUSTOM_MESSAGE = "extra_custom_message"
  const val EXTRA_ROUTINE_ID = "extra_routine_id"
  const val EXTRA_ROUTINE_TITLE = "extra_routine_title"
  const val EXTRA_ROUTINE_KIND = "extra_routine_kind"
  const val EXTRA_ROUTINE_HOUR = "extra_routine_hour"
  const val EXTRA_ROUTINE_MINUTE = "extra_routine_minute"

  private const val DAILY_REMINDER_REQUEST_CODE = 4101
  private const val SMART_NUDGE_REQUEST_CODE = 4901
  private const val BALANCE_NOTIFICATION_ID = 5101

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val balanceChannel = NotificationChannel(
      CHANNEL_BALANCE,
      "Quick Balance Updates",
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = "Automatic summary of Safe to Spend Today and cycle balance after SMS or UPI capture."
    }

    val reminderChannel = NotificationChannel(
      CHANNEL_REMINDERS,
      "Daily Reminder Routines",
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = "Scheduled daily household routines for morning briefs, midday checks, and nightly expense logs."
    }

    val smartNudgeChannel = NotificationChannel(
      CHANNEL_SMART_NUDGES,
      "Smart Household Nudges",
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = "Timely, non-intrusive reminders for upcoming bills, unlogged days, and savings opportunities."
    }

    val customChannel = NotificationChannel(
      CHANNEL_CUSTOM,
      "Household Alerts",
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      description = "Important budget alerts and reminders from Ghar."
    }

    manager.createNotificationChannel(balanceChannel)
    manager.createNotificationChannel(reminderChannel)
    manager.createNotificationChannel(smartNudgeChannel)
    manager.createNotificationChannel(customChannel)
  }

  fun postQuickBalanceNotification(
    context: Context,
    container: AppContainer,
    plainEnglish: Boolean = container.preferencesRepository.prefsSnapshot.value.plainEnglishMode,
    triggerNote: String? = null
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    ensureChannels(context)

    val profile = container.budgetRepository.profileState.value
    val transactions = container.transactionRepository.transactionsState.value
    val goals = container.goalRepository.allGoalsState.value
    val prefs = container.preferencesRepository.prefsSnapshot.value

    val cycleStartMs = DateUtils.startOfBudgetCycleMillis(profile.monthStartDay)
    var spentCyclePaise = 0L
    var extraIncomePaise = 0L
    for (tx in transactions) {
      if (tx.timestamp >= cycleStartMs) {
        if (tx.isExpense) spentCyclePaise += tx.amountPaise else extraIncomePaise += tx.amountPaise
      }
    }

    val safeResult = SafeToSpendCalculator.calculate(profile, spentCyclePaise, extraIncomePaise)
    val todaySpentRupees = HouseholdAnalyticsEngine.calculateTodaySpendRupees(transactions)
    val totalSavedRupees = goals.sumOf { it.currentAmountRupees }

    val safeTodayStr = CurrencyFormatter.formatRupees(safeResult.dailyAllowanceRupees)
    val spentTodayStr = CurrencyFormatter.formatRupees(todaySpentRupees)
    val cycleLeftStr = CurrencyFormatter.formatRupees(safeResult.totalRemainingRupees.coerceAtLeast(0L))
    val savedStr = CurrencyFormatter.formatRupees(totalSavedRupees)

    val title = if (triggerNote != null) {
      "$triggerNote • Safe Today: $safeTodayStr"
    } else {
      "Balance Update • Safe Today: $safeTodayStr"
    }

    val bankLine = prefs.lastDetectedBankBalancePaise?.let {
      "\nLast SMS Bank Balance: ${CurrencyFormatter.formatRupees(it / 100L)}"
    }.orEmpty()

    val savingsWord = if (plainEnglish) "Total Saved" else "Total Gullak Saved"
    val summaryBody = buildString {
      append("Spent Today: $spentTodayStr • Remaining (${safeResult.daysRemaining}d): $cycleLeftStr\n")
      append("$savingsWord: $savedStr")
      append(bankLine)
    }

    val launchIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_BALANCE)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText("Spent Today: $spentTodayStr • Cycle Left: $cycleLeftStr")
      .setStyle(NotificationCompat.BigTextStyle().bigText(summaryBody))
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .build()

    return runCatching {
      NotificationManagerCompat.from(context).notify(BALANCE_NOTIFICATION_ID, notification)
      true
    }.getOrDefault(false)
  }

  fun postCustomNotification(
    context: Context,
    title: String,
    message: String,
    channelId: String = CHANNEL_CUSTOM
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    ensureChannels(context)

    val cleanTitle = title.trim().ifBlank { "Budget Reminder" }
    val cleanMessage = message.trim().ifBlank { "Take 10 seconds to review today's transactions." }

    val launchIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      cleanTitle.hashCode(),
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(cleanTitle)
      .setContentText(cleanMessage)
      .setStyle(NotificationCompat.BigTextStyle().bigText(cleanMessage))
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setPriority(
        if (channelId == CHANNEL_CUSTOM) NotificationCompat.PRIORITY_HIGH
        else NotificationCompat.PRIORITY_DEFAULT
      )
      .build()

    val notifId = 5200 + abs((cleanTitle.hashCode() + System.currentTimeMillis().toInt()) % 4000)
    return runCatching {
      NotificationManagerCompat.from(context).notify(notifId, notification)
      true
    }.getOrDefault(false)
  }

  private fun routineRequestCode(routineId: String): Int =
    4200 + abs(routineId.hashCode() % 600)

  /**
   * Synchronizes all user-configured Daily Reminder Routines and the randomized Smart Contextual Nudge
   * schedule with Android's AlarmManager.
   */
  fun syncAllScheduledNotifications(
    context: Context,
    routines: List<DailyReminderRoutine>,
    smartNudgesEnabled: Boolean
  ) {
    ensureChannels(context)
    // Cancel legacy single alarm so there are never duplicate triggers
    cancelLegacyDailyReminder(context)

    routines.forEach { routine ->
      scheduleRoutineAlarm(context, routine)
    }
    scheduleNextSmartContextualNudge(context, enabled = smartNudgesEnabled)
  }

  private fun cancelLegacyDailyReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, HouseholdReminderReceiver::class.java).apply {
      action = ACTION_DAILY_LOG_REMINDER
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      DAILY_REMINDER_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingIntent != null) {
      alarmManager.cancel(pendingIntent)
    }
  }

  fun scheduleRoutineAlarm(
    context: Context,
    routine: DailyReminderRoutine,
    forceTomorrow: Boolean = false
  ) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, HouseholdReminderReceiver::class.java).apply {
      action = ACTION_ROUTINE_REMINDER
      putExtra(EXTRA_ROUTINE_ID, routine.id)
      putExtra(EXTRA_ROUTINE_TITLE, routine.title)
      putExtra(EXTRA_ROUTINE_KIND, routine.kind.name)
      putExtra(EXTRA_ROUTINE_HOUR, routine.safeHour)
      putExtra(EXTRA_ROUTINE_MINUTE, routine.safeMinute)
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      routineRequestCode(routine.id),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (!routine.enabled) {
      alarmManager.cancel(pendingIntent)
      return
    }

    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance().apply {
      timeInMillis = now
      set(Calendar.HOUR_OF_DAY, routine.safeHour)
      set(Calendar.MINUTE, routine.safeMinute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (forceTomorrow || timeInMillis <= now + 5_000L) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    alarmManager.setAndAllowWhileIdle(
      AlarmManager.RTC_WAKEUP,
      calendar.timeInMillis,
      pendingIntent
    )
  }

  fun cancelRoutineAlarm(context: Context, routineId: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, HouseholdReminderReceiver::class.java).apply {
      action = ACTION_ROUTINE_REMINDER
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      routineRequestCode(routineId),
      intent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingIntent != null) {
      alarmManager.cancel(pendingIntent)
    }
  }

  /**
   * Schedules the next randomized Smart Contextual Nudge within polite daytime hours (10:15 AM – 8:15 PM),
   * spaced 4.5 to 9 hours apart so notifications feel thoughtful, organic, and never spammy.
   */
  fun scheduleNextSmartContextualNudge(
    context: Context,
    enabled: Boolean,
    nowMs: Long = System.currentTimeMillis()
  ) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, HouseholdReminderReceiver::class.java).apply {
      action = ACTION_SMART_CONTEXTUAL_NUDGE
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      SMART_NUDGE_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (!enabled) {
      alarmManager.cancel(pendingIntent)
      return
    }

    val randomDelayMinutes = Random.nextInt(270, 540) // 4.5h to 9h
    val targetCal = Calendar.getInstance().apply {
      timeInMillis = nowMs + randomDelayMinutes * 60_000L
    }
    val hour = targetCal.get(Calendar.HOUR_OF_DAY)
    if (hour >= 20) {
      // Shift to next morning between 10:15 AM and 12:30 PM
      targetCal.add(Calendar.DAY_OF_YEAR, 1)
      targetCal.set(Calendar.HOUR_OF_DAY, Random.nextInt(10, 12))
      targetCal.set(Calendar.MINUTE, Random.nextInt(15, 55))
      targetCal.set(Calendar.SECOND, 0)
    } else if (hour < 10) {
      targetCal.set(Calendar.HOUR_OF_DAY, Random.nextInt(10, 13))
      targetCal.set(Calendar.MINUTE, Random.nextInt(10, 55))
      targetCal.set(Calendar.SECOND, 0)
    }

    alarmManager.setAndAllowWhileIdle(
      AlarmManager.RTC_WAKEUP,
      targetCal.timeInMillis,
      pendingIntent
    )
  }

  fun buildRoutineNotificationContent(
    routine: DailyReminderRoutine,
    profile: BudgetProfile,
    transactions: List<Transaction>,
    commitments: List<PakkaCommitment>,
    plainEnglish: Boolean
  ): Pair<String, String> {
    val cycleStartMs = DateUtils.startOfBudgetCycleMillis(profile.monthStartDay)
    val cycleSpentPaise = transactions.filter { it.timestamp >= cycleStartMs && it.isExpense }.sumOf { it.amountPaise }
    val cycleIncomePaise = transactions.filter { it.timestamp >= cycleStartMs && !it.isExpense }.sumOf { it.amountPaise }
    val safeCalc = SafeToSpendCalculator.calculate(profile, cycleSpentPaise, cycleIncomePaise)
    val safeTodayStr = CurrencyFormatter.formatRupees(safeCalc.dailyAllowanceRupees)
    val todaySpent = HouseholdAnalyticsEngine.calculateTodaySpendRupees(transactions)
    val todaySpentStr = CurrencyFormatter.formatRupees(todaySpent)

    val cal = Calendar.getInstance()
    val todayDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
    val dueTodayCount = commitments.count { c ->
      c.isActive && c.safeDueDay == todayDayOfMonth &&
        transactions.none { tx -> tx.timestamp >= cycleStartMs && tx.rawText?.contains(c.ledgerMarkerTag()) == true }
    }

    val title = routine.displayTitle(plainEnglish)
    val body = when (routine.kind) {
      RoutineKind.MORNING_BRIEF -> buildString {
        append("Today's safe daily allowance is $safeTodayStr (${safeCalc.daysRemaining} days left in cycle).")
        if (dueTodayCount > 0) {
          append(" $dueTodayCount scheduled ${if (dueTodayCount == 1) "bill/payment is" else "items are"} due today.")
        }
      }
      RoutineKind.MIDDAY_CHECK -> {
        val leftToday = (safeCalc.dailyAllowanceRupees - todaySpent).coerceAtLeast(0L)
        "Logged so far today: $todaySpentStr • Comfortable room left today: ${CurrencyFormatter.formatRupees(leftToday)}."
      }
      RoutineKind.EVENING_KHATA -> {
        val savingsTerm = if (plainEnglish) "Savings Pot" else "Papa's Gullak"
        if (todaySpent == 0L) {
          "No expenses logged yet today (safe allowance: $safeTodayStr). Tap to jot today's spend or move leftovers into $savingsTerm."
        } else {
          val unspent = safeCalc.dailyAllowanceRupees - todaySpent
          if (unspent >= 50L) {
            "Spent today: $todaySpentStr — you saved ${CurrencyFormatter.formatRupees(unspent)} under today's $safeTodayStr target! Tap to log final items or drop coins into $savingsTerm."
          } else {
            "Spent today: $todaySpentStr against your $safeTodayStr daily target. Tap to wrap up today's ledger."
          }
        }
      }
      RoutineKind.CUSTOM -> {
        "Spent today: $todaySpentStr • Safe daily allowance: $safeTodayStr (${safeCalc.daysRemaining}d left in cycle)."
      }
    }
    return title to body
  }

  /**
   * Selects a genuinely relevant, non-repetitive contextual notification grounded in real local
   * ledger state (due bills/receivables, unlogged days, habit pace, Gullak sweep opportunity, or streak).
   */
  fun buildSmartContextualNudge(
    profile: BudgetProfile,
    transactions: List<Transaction>,
    commitments: List<PakkaCommitment>,
    goals: List<SavingsGoal>,
    plainEnglish: Boolean,
    lastNudgeKey: String = "",
    nowMs: Long = System.currentTimeMillis()
  ): SmartContextualNudge {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)
    val maxDayInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cycleStartMs = DateUtils.startOfBudgetCycleMillis(profile.monthStartDay, nowMs)
    val cycleTxs = transactions.filter { it.timestamp >= cycleStartMs }
    val spentCyclePaise = cycleTxs.filter { it.isExpense }.sumOf { it.amountPaise }
    val incomeCyclePaise = cycleTxs.filter { !it.isExpense }.sumOf { it.amountPaise }
    val safeCalc = SafeToSpendCalculator.calculate(profile, spentCyclePaise, incomeCyclePaise)
    val todaySpentRupees = HouseholdAnalyticsEngine.calculateTodaySpendRupees(transactions, nowMs)

    val candidates = mutableListOf<SmartContextualNudge>()

    // 1. Upcoming or due-today unsettled Pakke bills / subscriptions / receivables (within 0..3 days)
    val unsettledActive = commitments.filter { c ->
      c.isActive && cycleTxs.none { tx -> tx.rawText?.contains(c.ledgerMarkerTag()) == true }
    }
    for (item in unsettledActive) {
      val effectiveDue = item.safeDueDay.coerceAtMost(maxDayInMonth)
      val daysUntil = effectiveDue - todayDay
      if (daysUntil in -2..3) {
        val amtStr = CurrencyFormatter.formatRupees(item.amountRupees)
        val itemName = item.displayTitle(plainEnglish)
        if (item.isReceivable) {
          val whenLabel = when {
            daysUntil < 0 -> "was expected ${abs(daysUntil)}d ago"
            daysUntil == 0 -> "is expected today"
            daysUntil == 1 -> "is expected tomorrow"
            else -> "is expected in $daysUntil days"
          }
          candidates += SmartContextualNudge(
            key = "receivable_${item.id}_$todayDay",
            title = "Incoming Payment Reminder • $amtStr",
            body = "$itemName ($amtStr) $whenLabel. Once received, mark it settled in Ghar to update your balance."
          )
        } else {
          val whenLabel = when {
            daysUntil < 0 -> "was due ${abs(daysUntil)}d ago"
            daysUntil == 0 -> "is due today"
            daysUntil == 1 -> "is due tomorrow"
            else -> "is due in $daysUntil days (${item.dueDayOrdinal})"
          }
          val prefix = if (plainEnglish) "Upcoming Bill" else "Pakka Kharcha Check"
          candidates += SmartContextualNudge(
            key = "bill_${item.id}_$todayDay",
            title = "$prefix • $itemName",
            body = "$itemName ($amtStr) $whenLabel. Safe to spend today remains ${CurrencyFormatter.formatRupees(safeCalc.dailyAllowanceRupees)}."
          )
        }
      }
    }

    // 2. Afternoon / evening unlogged day nudge
    if (todaySpentRupees == 0L && currentHour >= 15) {
      val ledgerTerm = if (plainEnglish) "Ledger" else "Khata"
      candidates += SmartContextualNudge(
        key = "unlogged_day_$todayDay",
        title = if (plainEnglish) "Quick Expense Check-In" else "Quiet Day in Your Khata?",
        body = "Rs.0 logged so far today. Your safe daily allowance is ${CurrencyFormatter.formatRupees(safeCalc.dailyAllowanceRupees)} — tap to jot any small cash/UPI spends in $ledgerTerm."
      )
    }

    // 3. Unspent daily allowance -> Gullak coin opportunity (evening)
    val leftoverToday = safeCalc.dailyAllowanceRupees - todaySpentRupees
    val primaryGoal = goals.firstOrNull()
    if (todaySpentRupees > 0L && leftoverToday >= 100L && currentHour >= 17) {
      val potName = if (plainEnglish) {
        primaryGoal?.name ?: "Savings Pot"
      } else {
        "Papa's Gullak (${primaryGoal?.name ?: "Goal"})"
      }
      candidates += SmartContextualNudge(
        key = "leftover_sweep_$todayDay",
        title = "${CurrencyFormatter.formatRupees(leftoverToday)} Still Safe Today",
        body = "You've spent ${CurrencyFormatter.formatRupees(todaySpentRupees)} against your ${CurrencyFormatter.formatRupees(safeCalc.dailyAllowanceRupees)} daily pace. Consider dropping a coin into $potName."
      )
    }

    // 4. Category habit spike notice (Chai/Coffee or Food Delivery in last 7 days)
    val sevenDaysAgo = nowMs - 7L * 24L * 3600_000L
    val weekFoodAndChai = transactions
      .filter {
        it.timestamp >= sevenDaysAgo && it.isExpense &&
          (it.category == Category.CHAI_SNACKS || it.category == Category.FOOD_DELIVERY)
      }
    val weekHabitRupees = weekFoodAndChai.sumOf { it.amountRupees }
    if (weekFoodAndChai.size >= 3 && weekHabitRupees >= 250L) {
      val label = if (plainEnglish) "Snacks & Food Orders" else "Chai, Snacks & Tiffin Orders"
      candidates += SmartContextualNudge(
        key = "habit_pulse_${weekFoodAndChai.size}",
        title = "7-Day $label: ${CurrencyFormatter.formatRupees(weekHabitRupees)}",
        body = "${weekFoodAndChai.size} entries this week totaling ${CurrencyFormatter.formatRupees(weekHabitRupees)}. Trimming just one order keeps tomorrow's ${CurrencyFormatter.formatRupees(safeCalc.dailyAllowanceRupees)} target effortless."
      )
    }

    // 5. Calm cycle-pace status fallback (always grounded in real SafeToSpend math)
    val cycleLeftStr = CurrencyFormatter.formatRupees(safeCalc.totalRemainingRupees.coerceAtLeast(0L))
    candidates += SmartContextualNudge(
      key = "pace_brief_$todayDay",
      title = "Safe Today: ${CurrencyFormatter.formatRupees(safeCalc.dailyAllowanceRupees)}",
      body = "$cycleLeftStr remaining across ${safeCalc.daysRemaining} days in this cycle • Spent today: ${CurrencyFormatter.formatRupees(todaySpentRupees)}."
    )

    val nonRepeating = candidates.filter { it.key != lastNudgeKey }
    val pool = nonRepeating.ifEmpty { candidates }
    return pool[Random.nextInt(pool.size)]
  }

  fun scheduleDailyLogReminder(
    context: Context,
    enabled: Boolean,
    hourOfDay: Int = 21,
    minute: Int = 0
  ) {
    scheduleRoutineAlarm(
      context = context,
      routine = DailyReminderRoutine(
        id = "routine_evening",
        title = "Nightly Khata & Gullak Routine",
        hour = hourOfDay.coerceIn(0, 23),
        minute = minute.coerceIn(0, 59),
        kind = RoutineKind.EVENING_KHATA,
        enabled = enabled
      )
    )
  }

  fun scheduleCustomReminder(
    context: Context,
    title: String,
    message: String,
    delayMinutes: Int
  ) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val reqCode = 6000 + (System.currentTimeMillis() % 3000).toInt()
    val intent = Intent(context, HouseholdReminderReceiver::class.java).apply {
      action = ACTION_CUSTOM_REMINDER
      putExtra(EXTRA_CUSTOM_TITLE, title.trim().ifBlank { "Budget Reminder" })
      putExtra(EXTRA_CUSTOM_MESSAGE, message.trim().ifBlank { "Check your daily budget." })
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      reqCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val triggerAtMs = System.currentTimeMillis() + delayMinutes.coerceAtLeast(1) * 60_000L
    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
  }
}
