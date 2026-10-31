package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.capture.CaptureEventBus
import com.example.core.firebase.configureAppCheck
import com.example.core.perf.GharResourcePrewarmer
import com.example.ui.navigation.GharNavHost
import com.example.ui.security.GharAppLockGate
import com.example.ui.theme.GharFontWarmup
import com.example.ui.theme.GharTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Immutable
private data class ActivityShellPrefsSlice(
  val isLoaded: Boolean,
  val themeMode: String,
  val useDynamicColor: Boolean,
  val appLockEnabled: Boolean,
  val plainEnglishMode: Boolean
)

class MainActivity : FragmentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
    configureAppCheck(this, intent)
    handleWidgetQuickKharchaIntent(intent)

    enableEdgeToEdge()

    val app = application as GharApplication
    val rootContainer = app.rootContainer

    setContent {
      // Defer lightweight initial-screen asset warmup until AFTER the first Compose frame commits
      // so cold-start rendering has 100% of CPU & IO, and defer idle caches until UI settles.
      androidx.compose.runtime.LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameNanos {}
        val isOnboarded = rootContainer.guestContainer.isOnboardingCompletedLocally ||
          rootContainer.preferencesRepository.prefsSnapshot.value.onboardingCompleted
        GharResourcePrewarmer.startWarmup(
          context = this@MainActivity,
          rootContainer = rootContainer,
          isOnboardingCompleted = isOnboarded
        )
      }
      GharFontWarmup.PrewarmComposeFontResolver()

      val shellSliceFlow = androidx.compose.runtime.remember(rootContainer) {
        rootContainer.preferencesRepository.prefsSnapshot
          .map {
            ActivityShellPrefsSlice(
              isLoaded = it.isLoaded,
              themeMode = it.themeMode,
              useDynamicColor = it.useDynamicColor,
              appLockEnabled = it.appLockEnabled,
              plainEnglishMode = it.plainEnglishMode
            )
          }
          .distinctUntilChanged()
      }
      val initialPrefs = rootContainer.preferencesRepository.prefsSnapshot.value
      val shellSlice by shellSliceFlow.collectAsStateWithLifecycle(
        initialValue = ActivityShellPrefsSlice(
          isLoaded = initialPrefs.isLoaded,
          themeMode = initialPrefs.themeMode,
          useDynamicColor = initialPrefs.useDynamicColor,
          appLockEnabled = initialPrefs.appLockEnabled,
          plainEnglishMode = initialPrefs.plainEnglishMode
        )
      )

      GharTheme(themeMode = shellSlice.themeMode, dynamicColor = shellSlice.useDynamicColor) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          GharAppLockGate(
            prefsLoaded = shellSlice.isLoaded,
            appLockEnabled = shellSlice.appLockEnabled,
            plainEnglish = shellSlice.plainEnglishMode
          ) {
            GharNavHost(rootContainer = rootContainer)
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleWidgetQuickKharchaIntent(intent)
  }

  private fun handleWidgetQuickKharchaIntent(incoming: Intent?) {
    if (incoming == null) return
    val wantsQuickKharcha = incoming.getBooleanExtra(CaptureEventBus.EXTRA_OPEN_QUICK_KHARCHA, false) ||
      incoming.action == CaptureEventBus.EXTRA_OPEN_QUICK_KHARCHA
    if (wantsQuickKharcha) {
      incoming.removeExtra(CaptureEventBus.EXTRA_OPEN_QUICK_KHARCHA)
      if (incoming.action == CaptureEventBus.EXTRA_OPEN_QUICK_KHARCHA) {
        incoming.action = null
      }
      CaptureEventBus.requestOpenQuickKharcha()
      return
    }

    if (incoming.action == Intent.ACTION_SEND && incoming.type?.startsWith("text/") == true) {
      val sharedText = incoming.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
      incoming.removeExtra(Intent.EXTRA_TEXT)
      incoming.action = null
      if (sharedText.isNotEmpty()) {
        CaptureEventBus.submitSharedAlertText(sharedText)
      }
    }
  }
}
