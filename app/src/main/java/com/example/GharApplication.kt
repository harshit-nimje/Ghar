package com.example

import android.app.Application
import com.example.capture.HouseholdNotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GharApplication : Application() {
  lateinit var rootContainer: RootAppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    // Keep Application.onCreate lightweight so headless BroadcastReceiver wakeups
    // (SmsCaptureReceiver, HouseholdReminderReceiver) never decode UI bitmaps or fonts into RAM.
    rootContainer = DefaultRootAppContainer(this)

    rootContainer.appScope.launch {
      val prefs = rootContainer.preferencesRepository.prefsSnapshot.first { it.isLoaded }
      HouseholdNotificationHelper.syncAllScheduledNotifications(
        context = this@GharApplication,
        routines = prefs.dailyRoutines,
        smartNudgesEnabled = prefs.smartRandomNudgesEnabled
      )
    }
  }
}
