package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Pastel Homely Indian Household Palette
val PastelTerracotta = Color(0xFFD46A46)
val PastelTerracottaContainer = Color(0xFFFCEBE3)
val OnPastelTerracottaContainer = Color(0xFF421608)

val PastelSage = Color(0xFF568B6D)
val PastelSageContainer = Color(0xFFE3F2E8)
val OnPastelSageContainer = Color(0xFF0E361E)

val PastelHoney = Color(0xFFD49232)
val PastelHoneyContainer = Color(0xFFFDF2DC)
val OnPastelHoneyContainer = Color(0xFF3F2602)

val PastelSky = Color(0xFF5688B2)
val PastelSkyContainer = Color(0xFFE5F0FA)
val OnPastelSkyContainer = Color(0xFF0C2B47)

val PastelPlum = Color(0xFF916E96)
val PastelPlumContainer = Color(0xFFF5EBF7)
val OnPastelPlumContainer = Color(0xFF381B3D)

// Soft Watercolor & Linen Paper Washes
val WarmCreamLight = Color(0xFFFBF7F0)
val WarmSandLight = Color(0xFFF3ECE0)
val SurfaceLight = Color(0xFFFFFDF9)
val SurfaceVariantLight = Color(0xFFF2EAE0)

val PastelPeachWash = Color(0xFFFBE7DC)
val ButtercreamSoft = Color(0xFFFCF3DC)
val SageMist = Color(0xFFE4F0E7)
val SkyMist = Color(0xFFE6EFF7)
val LavenderDusk = Color(0xFFF0E9F5)

// High-Contrast Warm Ink Typography
val HighContrastCharcoal = Color(0xFF231D18)
val HighContrastSubtext = Color(0xFF5A4E44)
val HighContrastBorder = Color(0xFFE5DBCE)

// Moonlit Home Dark Palette
val BackgroundDark = Color(0xFF15120F)
val SurfaceDark = Color(0xFF201B17)
val SurfaceVariantDark = Color(0xFF2C2520)
val BorderDark = Color(0xFF403730)

val PastelTerracottaDark = Color(0xFFF39376)
val PastelTerracottaDarkContainer = Color(0xFF471D10)
val PastelSageDark = Color(0xFF8EC8A4)
val PastelSageDarkContainer = Color(0xFF193B25)
val PastelHoneyDark = Color(0xFFF2C16D)
val PastelHoneyDarkContainer = Color(0xFF472F06)
val PastelSkyDark = Color(0xFF89B7E3)
val PastelSkyDarkContainer = Color(0xFF13324F)

// High-Contrast Dark Mode Typography
val TextPrimaryDark = Color(0xFFFAF5ED)
val TextSecondaryDark = Color(0xFFD2C6B8)
val TextMutedDark = Color(0xFFA39688)

// Physical money & coins
val RupeeGreen = Color(0xFF3B7D4F)
val RupeeNoteBg = Color(0xFFEAF5EC)
val RupeeNoteBgDark = Color(0xFF1B3620)
val ClayGullakColor = Color(0xFFC86941)
val GoldCoin = Color(0xFFE6A736)

// Compatibility aliases
val TerracottaPrimary = PastelTerracotta
val TerracottaDark = Color(0xFFA65328)
val TerracottaContainer = PastelTerracottaContainer
val WarmCream = WarmCreamLight
val WarmSand = WarmSandLight
val WarmWood = Color(0xFF8B6B47)
val WarmWoodDark = Color(0xFF5D4428)
val WarmCharcoal = HighContrastCharcoal
val WarmMutedText = HighContrastSubtext
val WarmBorder = HighContrastBorder
val DeepGreenAccent = PastelSage
val DeepGreenContainer = PastelSageContainer

// Contrast-adaptive ink tokens for filled buttons, badges, pills, and dynamic Material surfaces
val ContrastDarkInk = Color(0xFF19130F)
val ContrastLightInk = Color(0xFFFFFCF7)

/**
 * Computes exact WCAG 2.1 contrast ratio against both [lightContent] and [darkContent]
 * and returns whichever ink delivers higher legibility on [this] background color.
 * Ensures text adjusts seamlessly between dark and white/light ink across Light Mode,
 * Dark Mode, and Android 12+ Material You Dynamic Color palettes.
 */
fun Color.contrastingContentColor(
  lightContent: Color = ContrastLightInk,
  darkContent: Color = ContrastDarkInk
): Color {
  val bgLum = this.luminanceCompat()
  val lightLum = lightContent.luminanceCompat()
  val darkLum = darkContent.luminanceCompat()

  val contrastWithLight = (maxOf(bgLum, lightLum) + 0.05f) / (minOf(bgLum, lightLum) + 0.05f)
  val contrastWithDark = (maxOf(bgLum, darkLum) + 0.05f) / (minOf(bgLum, darkLum) + 0.05f)

  return if (contrastWithDark >= contrastWithLight) darkContent else lightContent
}

/**
 * Adjusts an accent color when rendered as text/icon on [surfaceColor] so it maintains
 * comfortable contrast in both bright and dark themes.
 */
fun Color.readableAccentOn(surfaceColor: Color): Color {
  val surfaceLum = surfaceColor.luminanceCompat()
  val accentLum = this.luminanceCompat()
  val ratio = (maxOf(surfaceLum, accentLum) + 0.05f) / (minOf(surfaceLum, accentLum) + 0.05f)
  if (ratio >= 3.4f) return this

  // Shift toward lighter tint on dark surfaces or deeper shade on light surfaces
  return if (surfaceLum < 0.25f) {
    Color(
      red = (red + (1f - red) * 0.38f).coerceIn(0f, 1f),
      green = (green + (1f - green) * 0.38f).coerceIn(0f, 1f),
      blue = (blue + (1f - blue) * 0.38f).coerceIn(0f, 1f),
      alpha = alpha
    )
  } else {
    Color(
      red = (red * 0.68f).coerceIn(0f, 1f),
      green = (green * 0.68f).coerceIn(0f, 1f),
      blue = (blue * 0.68f).coerceIn(0f, 1f),
      alpha = alpha
    )
  }
}

private fun Color.luminanceCompat(): Float {
  fun channel(c: Float): Float {
    val v = c.coerceIn(0f, 1f)
    return if (v <= 0.03928f) v / 12.92f else Math.pow(((v + 0.055) / 1.055), 2.4).toFloat()
  }
  return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}

