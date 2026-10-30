package com.example.core.perf

import android.content.Context
import android.os.Looper
import android.os.Process
import com.example.RootAppContainer
import com.example.core.audio.GharSoundEffects
import com.example.core.engine.ChitthiWisdomBank
import com.example.core.engine.SmartKhataParser
import com.example.ui.common.GharImageCache
import com.example.ui.theme.GharFontWarmup
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Staged, idle-aware startup resource prewarmer.
 *
 * Startup discipline:
 * 1. Immediately decodes ONLY the drawables required for the user's current initial screen
 *    (Welcome Onboarding artwork OR Rasoi Kitchen hearth + Batua/Gullak medallions).
 *    Secondary room banners and sitting-pose chat scenes are NOT eagerly decoded or retained.
 * 2. Does NOT initialize `MaaMiniModelManager` or synthesize offline AI neural tensors during
 *    startup — AI engine creation is strictly lazy upon first chat or AI settings interaction.
 * 3. Defers lightweight typeface lookup, regex/seed warmup, and foley WAV verification until
 *    the main thread reports an idle window (or a post-settle fallback), running at background
 *    thread priority so initial scrolling and gestures have zero CPU/IO contention.
 */
object GharResourcePrewarmer {
  private val prewarmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val started = AtomicBoolean(false)

  private val _warmupProgress = MutableStateFlow(0f)
  val warmupProgress: StateFlow<Float> = _warmupProgress.asStateFlow()

  private val _isFullyWarmed = MutableStateFlow(false)
  val isFullyWarmed: StateFlow<Boolean> = _isFullyWarmed.asStateFlow()

  fun startWarmup(
    context: Context,
    @Suppress("UNUSED_PARAMETER") rootContainer: RootAppContainer? = null,
    isOnboardingCompleted: Boolean = true
  ) {
    val appCtx = context.applicationContext
    if (!started.compareAndSet(false, true)) {
      GharImageCache.preloadInitialScreenEssentials(appCtx, isOnboardingCompleted)
      return
    }

    prewarmScope.launch(Dispatchers.IO) {
      runCatching {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
      }

      // Stage 1: Prepare ONLY the assets needed for the active initial screen
      runCatching {
        GharImageCache.preloadInitialScreenEssentialsAwait(appCtx, isOnboardingCompleted)
      }
      _warmupProgress.value = 0.45f

      // Yield until the main UI Looper reaches an idle slot (bounded so headless/unit tests complete promptly)
      awaitMainThreadIdleOrFallback(maxWaitMs = 350L)

      // Stage 2: Preload bundled font file descriptors without redundant 98-pass Canvas rasterization
      runCatching {
        GharFontWarmup.preloadNativeTypefaces(appCtx)
      }
      _warmupProgress.value = 0.70f

      // Stage 3: Pre-compile lightweight Khata parser regexes & wisdom seed lists
      runCatching {
        ChitthiWisdomBank.getMaaSeeds(plainEnglish = false)
        ChitthiWisdomBank.getMaaSeeds(plainEnglish = true)
        ChitthiWisdomBank.getBhaiSeeds(plainEnglish = false)
        ChitthiWisdomBank.getBhaiSeeds(plainEnglish = true)
        SmartKhataParser.parse("chai 20")
      }
      _warmupProgress.value = 0.85f

      // Stage 4: Verify cached foley WAVs on low-priority IO (no-op file check after first install)
      runCatching {
        GharSoundEffects.prewarm(appCtx)
      }

      _warmupProgress.value = 1.0f
      _isFullyWarmed.value = true
    }
  }

  private suspend fun awaitMainThreadIdleOrFallback(maxWaitMs: Long) {
    val mainLooper = Looper.getMainLooper()
    if (mainLooper == null) {
      delay(40L.coerceAtMost(maxWaitMs))
      return
    }
    withTimeoutOrNull(maxWaitMs) {
      suspendCancellableCoroutine { cont ->
        val idleHandler = android.os.MessageQueue.IdleHandler {
          if (cont.isActive) cont.resume(Unit)
          false
        }
        val registered = runCatching {
          mainLooper.queue.addIdleHandler(idleHandler)
          true
        }.getOrDefault(false)
        if (!registered && cont.isActive) {
          cont.resume(Unit)
        } else {
          cont.invokeOnCancellation {
            runCatching { mainLooper.queue.removeIdleHandler(idleHandler) }
          }
        }
      }
    }
  }
}
