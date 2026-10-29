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

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enforcePeak120Hz()

    // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
    configureAppCheck(this, intent)

    enableEdgeToEdge()

    val app = application as GharApplication
    val rootContainer = app.rootContainer

    // Start low-priority background compilation of room banners, Skia glyph caches,
    // household foley WAVs, and instant offline 0.5B LLM weights immediately on UI launch.
    GharResourcePrewarmer.startWarmup(this, rootContainer)

    setContent {
      GharFontWarmup.PrewarmComposeFontResolver()
      val prefs by rootContainer.preferencesRepository.prefsSnapshot.collectAsStateWithLifecycle()

      GharTheme(themeMode = prefs.themeMode, dynamicColor = prefs.useDynamicColor) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          GharNavHost(rootContainer = rootContainer)
        }
      }
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      enforcePeak120Hz()
    }
  }

  private fun enforcePeak120Hz() {
    val window = window ?: return
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
      val display = window.decorView.display ?: windowManager.defaultDisplay
      val modes = display?.supportedModes.orEmpty()
      val maxMode = modes.maxByOrNull { it.refreshRate }
      val maxRate = maxMode?.refreshRate ?: 120f
      val lp = window.attributes
      lp.preferredRefreshRate = maxRate.coerceAtLeast(120f)
      if (maxMode != null) {
        lp.preferredDisplayModeId = maxMode.modeId
      }
      window.attributes = lp
    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
      val lp = window.attributes
      lp.preferredRefreshRate = 120f
      window.attributes = lp
    }
  }
}
