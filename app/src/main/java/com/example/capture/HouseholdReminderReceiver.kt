package com.example.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.AppContainer
import com.example.GharApplication
import com.example.data.preferences.DailyReminderRoutine
import com.example.data.preferences.RoutineKind
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.Calendar
import kotlinx.coroutines.launch

class HouseholdReminderReceiver : BroadcastReceiver() {

  private fun resolveActiveContainer(app: GharApplication): AppContainer {
    val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
    return if (uid != null) app.rootContainer.forUser(uid) else app.rootContainer.guestContainer
  }

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context == null || intent == null) return
    val action = intent.action ?: return

    when (action) {
      Intent.ACTION_BOOT_COMPLETED,
      Intent.ACTION_MY_PACKAGE_REPLACED -> {
        val app = context.applicationContext as? GharApplication ?: return
        val pendingResult = goAsync()
        app.rootContainer.appScope.launch {
          try {
            val prefs = app.rootContainer.preferencesRepository.prefsSnapshot.value
            HouseholdNotificationHelper.syncAllScheduledNotifications(
              context = context,
              routines = prefs.dailyRoutines,
              smartNudgesEnabled = prefs.smartRandomNudgesEnabled
            )
          } finally {
            pendingResult.finish()
          }
        }
      }

      HouseholdNotificationHelper.ACTION_CUSTOM_REMINDER -> {
        val title = intent.getStringExtra(HouseholdNotificationHelper.EXTRA_CUSTOM_TITLE)
          ?: "Budget Reminder"
        val message = intent.getStringExtra(HouseholdNotificationHelper.EXTRA_CUSTOM_MESSAGE)
          ?: "Check today's spending and safe-to-spend balance."
        HouseholdNotificationHelper.postCustomNotification(
          context = context,
          title = title,
          message = message,
          channelId = HouseholdNotificationHelper.CHANNEL_CUSTOM
        )
      }

      HouseholdNotificationHelper.ACTION_ROUTINE_REMINDER,
      HouseholdNotificationHelper.ACTION_DAILY_LOG_REMINDER -> {
        val app = context.applicationContext as? GharApplication ?: return
        val container = resolveActiveContainer(app)
        val prefs = container.preferencesRepository.prefsSnapshot.value

        val routineId = intent.getStringExtra(HouseholdNotificationHelper.EXTRA_ROUTINE_ID)
          ?: "routine_evening"
        val storedRoutine = prefs.dailyRoutines.firstOrNull { it.id == routineId }
        val routine = storedRoutine ?: DailyReminderRoutine(
          id = routineId,
          title = intent.getStringExtra(HouseholdNotificationHelper.EXTRA_ROUTINE_TITLE)
            ?: "Nightly Khata & Gullak Routine",
          hour = intent.getIntExtra(HouseholdNotificationHelper.EXTRA_ROUTINE_HOUR, prefs.dailyReminderHour),
          minute = intent.getIntExtra(HouseholdNotificationHelper.EXTRA_ROUTINE_MINUTE, 0),
          kind = RoutineKind.fromName(intent.getStringExtra(HouseholdNotificationHelper.EXTRA_ROUTINE_KIND)),
          enabled = true
        )

        if (storedRoutine == null || storedRoutine.enabled) {
          // Schedule tomorrow's exact occurrence immediately
          HouseholdNotificationHelper.scheduleRoutineAlarm(
            context = context,
            routine = routine.copy(enabled = true),
            forceTomorrow = true
          )

          val (title, body) = HouseholdNotificationHelper.buildRoutineNotificationContent(
            routine = routine,
            profile = container.budgetRepository.profileState.value,
            transactions = container.transactionRepository.transactionsState.value,
            commitments = container.commitmentRepository.commitmentsState.value,
            plainEnglish = prefs.plainEnglishMode
          )

          HouseholdNotificationHelper.postCustomNotification(
            context = context,
            title = title,
            message = body,
            channelId = HouseholdNotificationHelper.CHANNEL_REMINDERS
          )
        }
      }

      HouseholdNotificationHelper.ACTION_SMART_CONTEXTUAL_NUDGE -> {
        val app = context.applicationContext as? GharApplication ?: return
        val container = resolveActiveContainer(app)
        val prefs = container.preferencesRepository.prefsSnapshot.value

        if (!prefs.smartRandomNudgesEnabled) return

        // Always queue the next randomized daytime slot
        HouseholdNotificationHelper.scheduleNextSmartContextualNudge(
          context = context,
          enabled = true
        )

        val nowMs = System.currentTimeMillis()
        val hour = Calendar.getInstance().apply { timeInMillis = nowMs }.get(Calendar.HOUR_OF_DAY)
        val minSpacingMs = 3L * 3600_000L // Never nudge more than once every 3 hours
        if (!prefs.onboardingCompleted || hour !in 9..20 || (nowMs - prefs.lastSmartNudgeAtMs) < minSpacingMs) {
          return
        }

        val nudge = HouseholdNotificationHelper.buildSmartContextualNudge(
          profile = container.budgetRepository.profileState.value,
          transactions = container.transactionRepository.transactionsState.value,
          commitments = container.commitmentRepository.commitmentsState.value,
          goals = container.goalRepository.allGoalsState.value,
          plainEnglish = prefs.plainEnglishMode,
          lastNudgeKey = prefs.lastSmartNudgeKey,
          nowMs = nowMs
        )

        val posted = HouseholdNotificationHelper.postCustomNotification(
          context = context,
          title = nudge.title,
          message = nudge.body,
          channelId = HouseholdNotificationHelper.CHANNEL_SMART_NUDGES
        )
        if (posted) {
          app.rootContainer.appScope.launch {
            container.preferencesRepository.recordSmartNudgeSent(nudge.key, nowMs)
          }
        }
      }
    }
  }
}
