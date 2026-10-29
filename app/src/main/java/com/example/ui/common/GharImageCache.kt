package com.example.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
 * Application-wide preloaded ImageBitmap & Painter cache.
 * Decodes concurrently across background threads, calls [Bitmap.prepareToDraw] so Android's
 * RenderThread uploads GPU textures ahead of time, and caches both ImageBitmap and BitmapPainter
 * so room banners and tactile wallet/gullak graphics mount in 0ms on first visit.
 */
object GharImageCache {
  private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val bitmapCache = ConcurrentHashMap<Int, ImageBitmap>()
  private val painterCache = ConcurrentHashMap<Int, BitmapPainter>()
  private val placeholderPainter = ColorPainter(Color.Transparent)

  // Ordered by visual priority (Kitchen & Batua first so home screen has 0ms wait)
  private val allPreloadableDrawables = intArrayOf(
    R.drawable.img_kitchen_hearth_1792931327219,
    R.drawable.img_leather_batua_1792933032717,
    R.drawable.img_earthen_gullak_1792933032830,
    R.drawable.img_gullak_sill_1792931330238,
    R.drawable.img_living_courtyard_1792931331737,
    R.drawable.img_study_desk_1792931360510,
    R.drawable.img_balcony_dusk_1792931332537,
    R.drawable.img_house_entry_1792907129736,
    R.drawable.img_maa_room_lean_1793191524432,
    R.drawable.img_maa_room_cozy_1793191528081,
    R.drawable.img_maa_room_cross_1793191529210,
    R.drawable.img_papa_room_mature_lean_1793199208784,
    R.drawable.img_papa_room_mature_cross_1793199209084,
    R.drawable.ic_ghar_gullak_1792907113972
  )

  fun preloadAll(context: Context) {
    val appCtx = context.applicationContext
    for (resId in allPreloadableDrawables) {
      if (!bitmapCache.containsKey(resId)) {
        preloadScope.launch {
          decodeAndCache(appCtx, resId)
        }
      }
    }
  }

  suspend fun preloadAllAwait(context: Context) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    for (resId in allPreloadableDrawables) {
      if (!bitmapCache.containsKey(resId)) {
        decodeAndCache(appCtx, resId)
      }
    }
  }

  fun getCached(@DrawableRes resId: Int): ImageBitmap? = bitmapCache[resId]

  fun getCachedPainter(@DrawableRes resId: Int): BitmapPainter? = painterCache[resId]

  suspend fun getOrDecode(context: Context, @DrawableRes resId: Int): ImageBitmap? {
    bitmapCache[resId]?.let { return it }
    return withContext(Dispatchers.IO) {
      bitmapCache[resId] ?: decodeAndCache(context.applicationContext, resId)
    }
  }

  private fun decodeAndCache(context: Context, @DrawableRes resId: Int): ImageBitmap? {
    bitmapCache[resId]?.let { return it }
    return try {
      val boundsOpts = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        inScaled = false
      }
      BitmapFactory.decodeResource(context.resources, resId, boundsOpts)

      val screenWidth = context.resources.displayMetrics.widthPixels.coerceIn(720, 1440)
      val maxTargetDim = screenWidth.coerceAtLeast(1080)
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
      val imgBmp = bmp.asImageBitmap()
      val painter = BitmapPainter(imgBmp)
      bitmapCache[resId] = imgBmp
      painterCache[resId] = painter
      imgBmp
    } catch (_: Throwable) {
      null
    }
  }

  internal fun placeholder(): Painter = placeholderPainter
}

/**
 * Returns a pre-cached [BitmapPainter] in 0ms when warmed, or decodes asynchronously on IO
 * without stalling Compose frame rendering.
 */
@Composable
fun rememberCachedGharPainter(@DrawableRes resId: Int): Painter {
  val existingPainter = GharImageCache.getCachedPainter(resId)
  if (existingPainter != null) {
    return existingPainter
  }

  val context = LocalContext.current
  var decodedBitmap by remember(resId) { mutableStateOf(GharImageCache.getCached(resId)) }

  LaunchedEffect(resId) {
    if (decodedBitmap == null) {
      decodedBitmap = GharImageCache.getOrDecode(context, resId)
    }
  }

  val bmp = decodedBitmap
  return if (bmp != null) {
    GharImageCache.getCachedPainter(resId) ?: remember(bmp) { BitmapPainter(bmp) }
  } else {
    GharImageCache.placeholder()
  }
}
