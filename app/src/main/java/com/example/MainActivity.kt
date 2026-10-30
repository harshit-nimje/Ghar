package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.firebase.configureAppCheck
import com.example.core.perf.GharResourcePrewarmer
import com.example.ui.navigation.GharNavHost
import com.example.ui.theme.GharFontWarmup
import com.example.ui.theme.GharTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
    configureAppCheck(this, intent)

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

      val themeSliceFlow = androidx.compose.runtime.remember(rootContainer) {
        rootContainer.preferencesRepository.prefsSnapshot
          .map { it.themeMode to it.useDynamicColor }
          .distinctUntilChanged()
      }
      val initialPrefs = rootContainer.preferencesRepository.prefsSnapshot.value
      val themeSlice by themeSliceFlow.collectAsStateWithLifecycle(
        initialValue = initialPrefs.themeMode to initialPrefs.useDynamicColor
      )
      val (themeMode, useDynamicColor) = themeSlice

      GharTheme(themeMode = themeMode, dynamicColor = useDynamicColor) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          GharNavHost(rootContainer = rootContainer)
        }
      }
    }
  }
}
