package com.example

import android.app.Application
import android.content.ComponentCallbacks2
import com.example.capture.HouseholdNotificationHelper
import com.example.ui.common.GharImageCache
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
      // Hydrate local guest vault on IO ahead of first UI read; AI model manager stays strictly lazy
      runCatching { rootContainer.guestContainer }
      val prefs = rootContainer.preferencesRepository.prefsSnapshot.first { it.isLoaded }
      HouseholdNotificationHelper.syncAllScheduledNotifications(
        context = this@GharApplication,
        routines = prefs.dailyRoutines,
        smartNudgesEnabled = prefs.smartRandomNudgesEnabled
      )
    }
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN ||
      level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
    ) {
      GharImageCache.trimMemory(clearAll = level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
    }
  }

  override fun onLowMemory() {
    super.onLowMemory()
    GharImageCache.trimMemory(clearAll = true)
  }
}
