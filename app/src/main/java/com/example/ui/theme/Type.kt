package com.example.ui.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * High-contrast editorial display serif for room hero titles and major splashes.
 * Registered at its true static Normal weight so FontSynthesis.All adds genuine weight contrast.
 */
val DisplaySerifFontFamily = FontFamily(
  Font(R.font.dm_serif_display, FontWeight.Normal)
)

/**
 * Warm literary serif for section titles, card headers, and dialog headers.
 */
val FrauncesFontFamily = FontFamily(
  Font(R.font.fraunces, FontWeight.Normal)
)

/**
 * Clean humanist sans-serif for crisp everyday descriptions, inputs, and controls.
 * Registered solely at FontWeight.Normal so Skia synthesizes true SemiBold & Bold weights.
 */
val PlusJakartaFontFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

/**
 * Natural Indian fountain-pen & felt-tip handwriting for Maa's fridge sticky notes,
 * Bhai's desk notes, and room greeting whispers.
 */
val HandwritingFontFamily = FontFamily(
  Font(R.font.kalam, FontWeight.Normal),
  Font(R.font.caveat, FontWeight.Bold)
)

/**
 * Bold felt-marker scribble / bahi-khata ledger ink font for primary rupee amounts.
 * Gives every rupee figure a tactile, handwritten, non-generic character.
 */
val ScribbleMoneyFontFamily = FontFamily(
  Font(R.font.permanent_marker, FontWeight.Normal)
)

/**
 * Devanagari-inspired Indian brush calligraphy font for compact money stamps and badges.
 */
val TillanaFontFamily = FontFamily(
  Font(R.font.tillana, FontWeight.Normal)
)

object GharHandStyles {
  val stickyNoteTitle = TextStyle(
    fontFamily = HandwritingFontFamily,
    fontWeight = FontWeight.Bold,
    fontSynthesis = FontSynthesis.All,
    fontSize = 22.sp,
    lineHeight = 26.sp
  )

  val stickyNoteBody = TextStyle(
    fontFamily = HandwritingFontFamily,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.All,
    fontSize = 18.sp,
    lineHeight = 25.sp,
    letterSpacing = 0.1.sp
  )

  val stickyNoteMeta = TextStyle(
    fontFamily = HandwritingFontFamily,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.All,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.1.sp
  )

  val ornamentalBadge = TextStyle(
    fontFamily = TillanaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSynthesis = FontSynthesis.All,
    fontSize = 13.sp,
    lineHeight = 17.sp,
    letterSpacing = 0.2.sp
  )

  val moneyHero = TextStyle(
    fontFamily = ScribbleMoneyFontFamily,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.All,
    fontSize = 38.sp,
    lineHeight = 44.sp,
    letterSpacing = 0.4.sp
  )

  val moneyLarge = TextStyle(
    fontFamily = ScribbleMoneyFontFamily,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.All,
    fontSize = 25.sp,
    lineHeight = 31.sp,
    letterSpacing = 0.2.sp
  )

  val moneyMedium = TextStyle(
    fontFamily = ScribbleMoneyFontFamily,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.All,
    fontSize = 18.sp,
    lineHeight = 23.sp,
    letterSpacing = 0.2.sp
  )

  val moneySmall = TextStyle(
    fontFamily = TillanaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSynthesis = FontSynthesis.All,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.1.sp
  )
}

val Typography = Typography(
  displayLarge = TextStyle(
    fontFamily = DisplaySerifFontFamily,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.All,
    fontSize = 40.sp,
    lineHeight = 46.sp,
    letterSpacing = (-0.3).sp
  ),
  displayMedium = TextStyle(
    fontFamily = DisplaySerifFontFamily,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.All,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.2).sp
  ),
  displaySmall = TextStyle(
    fontFamily = DisplaySerifFontFamily,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.All,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.15).sp
  ),
  headlineLarge = TextStyle(
    fontFamily = DisplaySerifFontFamily,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.All,
    fontSize = 29.sp,
    lineHeight = 35.sp,
    letterSpacing = (-0.15).sp
  ),
  headlineMedium = TextStyle(
    fontFamily = DisplaySerifFontFamily,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.All,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.1).sp
  ),
  headlineSmall = TextStyle(
    fontFamily = FrauncesFontFamily,
    fontWeight = FontWeight.Bold,
    fontSynthesis = FontSynthesis.All,
    fontSize = 21.sp,
    lineHeight = 27.sp
  ),
  titleLarge = TextStyle(
    fontFamily = FrauncesFontFamily,
    fontWeight = FontWeight.Bold,
    fontSynthesis = FontSynthesis.All,
    fontSize = 20.sp,
    lineHeight = 26.sp
  ),
  titleMedium = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSynthesis = FontSynthesis.All,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.1.sp
  ),
  titleSmall = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSynthesis = FontSynthesis.All,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.1.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.All,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.15.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.All,
    fontSize = 14.sp,
    lineHeight = 21.sp,
    letterSpacing = 0.15.sp
  ),
  bodySmall = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSynthesis = FontSynthesis.All,
    fontSize = 12.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.2.sp
  ),
  labelLarge = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSynthesis = FontSynthesis.All,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.2.sp
  ),
  labelMedium = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSynthesis = FontSynthesis.All,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.25.sp
  ),
  labelSmall = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.Medium,
    fontSynthesis = FontSynthesis.All,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.3.sp
  )
)

/**
 * Pre-warms both Android's native Typeface cache and Compose's FontFamily.Resolver cache
 * so first-time scrolling and room switching never block the UI thread decompressing .ttf files.
 */
object GharFontWarmup {
  private val warmupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val nativeStarted = AtomicBoolean(false)
  private val composeResolved = AtomicBoolean(false)

  val allBundledFontResIds = intArrayOf(
    R.font.dm_serif_display,
    R.font.fraunces,
    R.font.plus_jakarta_sans,
    R.font.kalam,
    R.font.caveat,
    R.font.permanent_marker,
    R.font.tillana
  )

  fun preloadNativeTypefaces(context: Context) {
    if (!nativeStarted.compareAndSet(false, true)) return
    val appCtx = context.applicationContext
    for (fontRes in allBundledFontResIds) {
      warmupScope.launch {
        runCatching { ResourcesCompat.getFont(appCtx, fontRes) }
      }
    }
  }

  @Composable
  fun PrewarmComposeFontResolver() {
    val resolver = LocalFontFamilyResolver.current
    LaunchedEffect(resolver) {
      if (!composeResolved.compareAndSet(false, true)) return@LaunchedEffect
      withContext(Dispatchers.Default) {
        val families = listOf(
          DisplaySerifFontFamily,
          FrauncesFontFamily,
          PlusJakartaFontFamily,
          HandwritingFontFamily,
          ScribbleMoneyFontFamily,
          TillanaFontFamily
        )
        val weights = listOf(
          FontWeight.Normal,
          FontWeight.Medium,
          FontWeight.SemiBold,
          FontWeight.Bold
        )
        for (family in families) {
          for (weight in weights) {
            runCatching {
              resolver.resolve(
                fontFamily = family,
                fontWeight = weight,
                fontSynthesis = FontSynthesis.All
              )
            }
          }
        }
      }
    }
  }
}
