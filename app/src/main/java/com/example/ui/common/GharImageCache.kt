package com.example.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import com.example.R
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Memory-bounded, single-flight decoded [ImageBitmap] & [BitmapPainter] LRU cache.
 *
 * - Enforces a deterministic byte ceiling (`maxCacheBytes`, ~1/14th of max JVM heap bounded to
 *   8 MB – 16 MB) so offscreen room banners and large Maa/Papa sitting scenes are evicted cleanly
 *   instead of retaining every bitmap indefinitely.
 * - Prioritizes active-screen assets (`preloadInitialScreenEssentials`), loading other room banners
 *   and chat scene artwork strictly on demand.
 * - Uses per-resource single-flight locks (`decodeLocks`) so concurrent requests for the same
 *   drawable never trigger duplicate decoding.
 * - Downsamples via power-of-two `inSampleSize` matched to target viewport bounds and uses
 *   `Bitmap.Config.RGB_565` for opaque illustrations (halving pixel buffer memory vs `ARGB_8888`).
 */
object GharImageCache {
  private class CachedEntry(
    val imageBitmap: ImageBitmap,
    val painter: BitmapPainter,
    val byteCount: Int
  )

  private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(2))
  private val decodeLocks = ConcurrentHashMap<Int, Any>()
  private val placeholderPainter = ColorPainter(Color.Transparent)

  internal val maxCacheBytes: Int = run {
    val maxHeap = Runtime.getRuntime().maxMemory().coerceAtLeast(64L * 1024L * 1024L)
    (maxHeap / 14L).coerceIn(8L * 1024L * 1024L, 16L * 1024L * 1024L).toInt()
  }

  private val cacheLock = Any()
  private val lruCache = object : LruCache<Int, CachedEntry>(maxCacheBytes) {
    override fun sizeOf(key: Int, value: CachedEntry): Int = value.byteCount
  }

  // Immediate Kitchen/Rasoi home-screen essentials (Rasoi banner + Batua/Gullak medallions)
  private val homeCriticalDrawables = intArrayOf(
    R.drawable.img_kitchen_hearth_1792931327219,
    R.drawable.img_leather_batua_1792933032717,
    R.drawable.img_earthen_gullak_1792933032830
  )

  // Immediate Onboarding welcome-screen essentials
  private val onboardingCriticalDrawables = intArrayOf(
    R.drawable.img_house_entry_1792907129736,
    R.drawable.ic_ghar_gullak_1792907113972
  )

  private val secondaryRoomDrawables = intArrayOf(
    R.drawable.img_gullak_sill_1792931330238,
    R.drawable.img_living_courtyard_1792931331737,
    R.drawable.img_study_desk_1792931360510,
    R.drawable.img_balcony_dusk_1792931332537,
    R.drawable.img_house_entry_1792907129736,
    R.drawable.ic_ghar_gullak_1792907113972
  )

  private val chatDialogSceneDrawables = setOf(
    R.drawable.img_maa_room_lean_1793191524432,
    R.drawable.img_maa_room_cozy_1793191528081,
    R.drawable.img_maa_room_cross_1793191529210,
    R.drawable.img_papa_room_mature_lean_1793199208784,
    R.drawable.img_papa_room_mature_cross_1793199209084
  )

  fun preloadInitialScreenEssentials(context: Context, isOnboardingCompleted: Boolean) {
    val appCtx = context.applicationContext
    val targets = if (isOnboardingCompleted) homeCriticalDrawables else onboardingCriticalDrawables
    for (resId in targets) {
      if (peekEntry(resId) == null) {
        preloadScope.launch {
          decodeAndCache(appCtx, resId)
        }
      }
    }
  }

  fun preloadHomeEssentials(context: Context) {
    preloadInitialScreenEssentials(context, isOnboardingCompleted = true)
  }

  suspend fun preloadInitialScreenEssentialsAwait(
    context: Context,
    isOnboardingCompleted: Boolean
  ) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    val targets = if (isOnboardingCompleted) homeCriticalDrawables else onboardingCriticalDrawables
    for (resId in targets) {
      if (peekEntry(resId) == null) {
        decodeAndCache(appCtx, resId)
      }
    }
  }

  fun trimMemory(clearAll: Boolean = false) {
    synchronized(cacheLock) {
      if (clearAll) {
        lruCache.evictAll()
        return
      }
      // Evict large dialog-only scenes & non-home banners first, then trim to 50% byte budget
      for (resId in chatDialogSceneDrawables) {
        lruCache.remove(resId)
      }
      for (resId in secondaryRoomDrawables) {
        lruCache.remove(resId)
      }
      lruCache.trimToSize(maxCacheBytes / 2)
    }
  }

  private fun peekEntry(@DrawableRes resId: Int): CachedEntry? = synchronized(cacheLock) {
    lruCache.get(resId)
  }

  fun getCached(@DrawableRes resId: Int): ImageBitmap? = peekEntry(resId)?.imageBitmap

  fun getCachedPainter(@DrawableRes resId: Int): BitmapPainter? = peekEntry(resId)?.painter

  internal fun currentCachedBytes(): Int = synchronized(cacheLock) { lruCache.size() }

  suspend fun getOrDecode(context: Context, @DrawableRes resId: Int): ImageBitmap? {
    peekEntry(resId)?.imageBitmap?.let { return it }
    return withContext(Dispatchers.IO) {
      peekEntry(resId)?.imageBitmap ?: decodeAndCache(context.applicationContext, resId)
    }
  }

  private fun targetMaxDimensionPx(context: Context, @DrawableRes resId: Int): Int {
    val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
    val screenWidth = context.resources.displayMetrics.widthPixels.coerceIn(720, 1440)
    return when (resId) {
      R.drawable.img_leather_batua_1792933032717,
      R.drawable.img_earthen_gullak_1792933032830,
      R.drawable.ic_ghar_gullak_1792907113972 -> {
        // Rendered inside <=156dp medallions; 448px native cap avoids oversampling
        (160f * density).toInt().coerceIn(256, 512)
      }
      in chatDialogSceneDrawables -> {
        // Chat dialog card max width is 560dp; cap decode dimension to avoid oversized buffers
        (screenWidth * 0.90f).toInt().coerceIn(680, 960)
      }
      else -> {
        // Room hero banners (960x536 native)
        screenWidth.coerceIn(720, 1080)
      }
    }
  }

  private fun decodeAndCache(context: Context, @DrawableRes resId: Int): ImageBitmap? {
    peekEntry(resId)?.imageBitmap?.let { return it }
    val lock = decodeLocks.computeIfAbsent(resId) { Any() }
    synchronized(lock) {
      peekEntry(resId)?.imageBitmap?.let { return it }
      return try {
        val boundsOpts = BitmapFactory.Options().apply {
          inJustDecodeBounds = true
          inScaled = false
        }
        BitmapFactory.decodeResource(context.resources, resId, boundsOpts)
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        val maxTargetDim = targetMaxDimensionPx(context, resId)
        var sampleSize = 1
        val rawMax = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        while (rawMax / (sampleSize * 2) >= maxTargetDim) {
          sampleSize *= 2
        }

        val needsAlpha = resId == R.drawable.ic_ghar_gullak_1792907113972
        val opts = BitmapFactory.Options().apply {
          inPreferredConfig = if (needsAlpha) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
          inSampleSize = sampleSize
          inScaled = false
        }
        val bmp = BitmapFactory.decodeResource(context.resources, resId, opts) ?: return null
        bmp.prepareToDraw()
        val byteSize = bmp.allocationByteCount.coerceAtLeast(1024)
        val imgBmp = bmp.asImageBitmap()
        val painter = BitmapPainter(imgBmp)
        synchronized(cacheLock) {
          lruCache.put(resId, CachedEntry(imgBmp, painter, byteSize))
        }
        imgBmp
      } catch (_: Throwable) {
        null
      }
    }
  }

  internal fun placeholder(): Painter = placeholderPainter
}

/**
 * Returns a cached [BitmapPainter] immediately when present in [GharImageCache], or decodes
 * asynchronously on IO without stalling Compose frame rendering.
 *
 * Maintains a stable Compose hook call order across both cache-hit and cache-miss paths so
 * transitioning from placeholder -> decoded bitmap never shifts Compose slot-table groups.
 */
@Composable
fun rememberCachedGharPainter(@DrawableRes resId: Int): Painter {
  val context = LocalContext.current
  var painter by remember(resId) {
    mutableStateOf<BitmapPainter?>(GharImageCache.getCachedPainter(resId))
  }

  LaunchedEffect(resId) {
    if (painter == null) {
      val bmp = GharImageCache.getOrDecode(context, resId)
      if (bmp != null) {
        painter = GharImageCache.getCachedPainter(resId) ?: BitmapPainter(bmp)
      }
    }
  }

  return painter ?: GharImageCache.placeholder()
}
