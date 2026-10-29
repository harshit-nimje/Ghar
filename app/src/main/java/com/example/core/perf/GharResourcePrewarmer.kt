package com.example.core.perf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Process
import androidx.core.content.res.ResourcesCompat
import com.example.RootAppContainer
import com.example.core.audio.GharSoundEffects
import com.example.core.engine.ChitthiWisdomBank
import com.example.core.engine.SmartKhataParser
import com.example.ui.common.GharImageCache
import com.example.ui.theme.GharFontWarmup
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Background resource compiler & asset pre-warmer that runs silently on low-priority worker
 * threads as soon as [com.example.MainActivity] opens (including while the user steps through
 * Onboarding) so there is zero frame hitch or lag when entering Rasoi or swiping rooms.
 *
 * Pre-compiles:
 * 1. Downsampled room & Batua/Gullak `ImageBitmap`s + `Bitmap.prepareToDraw()` GPU texture staging.
 * 2. All 7 bundled `.ttf` typefaces + Skia/FreeType glyph strike rasterization across multiple sizes & weights.
 * 3. Household foley WAV synthesis & `SoundPool` preloading (`GharSoundEffects`).
 * 4. Instant offline 0.5B quantized neural engine (`MaaMiniModelManager.getOrCreateInstantOfflineEngine`).
 * 5. Wisdom seeds & `SmartKhataParser` regex tables.
 */
object GharResourcePrewarmer {
  private val prewarmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val started = AtomicBoolean(false)

  private val _warmupProgress = MutableStateFlow(0f)
  val warmupProgress: StateFlow<Float> = _warmupProgress.asStateFlow()

  private val _isFullyWarmed = MutableStateFlow(false)
  val isFullyWarmed: StateFlow<Boolean> = _isFullyWarmed.asStateFlow()

  private const val GLYPH_WARMUP_TEXT =
    "₹0123456789.,+-/% Safe to Spend Rasoi Gullak Courtyard Khata Maa Papa Batua Pakke Shagun"

  fun startWarmup(context: Context, rootContainer: RootAppContainer? = null) {
    val appCtx = context.applicationContext
    if (!started.compareAndSet(false, true)) {
      // Ensure any uncached drawables still finish if called again
      GharImageCache.preloadAll(appCtx)
      return
    }

    // 1. Kick off parallel IO image decoding + SoundPool foley staging immediately
    GharImageCache.preloadAll(appCtx)
    GharSoundEffects.prewarm(appCtx)

    // 2. Run structured background compilation pipeline at background thread priority
    prewarmScope.launch(Dispatchers.IO) {
      runCatching {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
      }

      // Stage A: Decode & rasterize all 7 bundled fonts into Skia's native glyph Strike cache
      runCatching {
        GharFontWarmup.preloadNativeTypefaces(appCtx)
        warmNativeSkiaGlyphCache(appCtx)
      }
      _warmupProgress.value = 0.35f

      // Stage B: Ensure every room banner & Batua/Gullak texture is decoded and staged via prepareToDraw()
      runCatching {
        GharImageCache.preloadAllAwait(appCtx)
      }
      _warmupProgress.value = 0.70f

      // Stage C: Pre-compile regexes, wisdom seed tables, and instant offline 0.5B LLM weights
      runCatching {
        ChitthiWisdomBank.getMaaSeeds(plainEnglish = false)
        ChitthiWisdomBank.getMaaSeeds(plainEnglish = true)
        ChitthiWisdomBank.getBhaiSeeds(plainEnglish = false)
        ChitthiWisdomBank.getBhaiSeeds(plainEnglish = true)
        SmartKhataParser.parse("chai 20")
      }
      _warmupProgress.value = 0.85f

      runCatching {
        rootContainer?.maaMiniModelManager?.getOrCreateInstantOfflineEngine()
      }

      _warmupProgress.value = 1.0f
      _isFullyWarmed.value = true
    }
  }

  private fun warmNativeSkiaGlyphCache(context: Context) {
    val scratchBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    try {
      val canvas = Canvas(scratchBitmap)
      val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
      val density = context.resources.displayMetrics.scaledDensity.coerceAtLeast(1f)
      val sizesSp = floatArrayOf(12f, 14f, 16f, 20f, 25f, 32f, 38f)

      for (fontRes in GharFontWarmup.allBundledFontResIds) {
        val baseTf = runCatching { ResourcesCompat.getFont(context, fontRes) }.getOrNull() ?: continue
        val variants = arrayOf(
          baseTf,
          Typeface.create(baseTf, Typeface.BOLD)
        )
        for (tf in variants) {
          paint.typeface = tf
          for (sp in sizesSp) {
            paint.textSize = sp * density
            paint.measureText(GLYPH_WARMUP_TEXT)
            canvas.drawText(GLYPH_WARMUP_TEXT, 0f, 32f, paint)
          }
        }
      }
    } finally {
      scratchBitmap.recycle()
    }
  }
}
