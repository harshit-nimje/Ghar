package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val GharLightColorScheme = lightColorScheme(
  primary = PastelTerracotta,
  onPrimary = Color.White,
  primaryContainer = PastelTerracottaContainer,
  onPrimaryContainer = OnPastelTerracottaContainer,
  inversePrimary = PastelTerracottaDark,
  secondary = PastelSage,
  onSecondary = Color.White,
  secondaryContainer = PastelSageContainer,
  onSecondaryContainer = OnPastelSageContainer,
  tertiary = PastelHoney,
  onTertiary = Color.White,
  tertiaryContainer = PastelHoneyContainer,
  onTertiaryContainer = OnPastelHoneyContainer,
  background = WarmCreamLight,
  onBackground = HighContrastCharcoal,
  surface = SurfaceLight,
  onSurface = HighContrastCharcoal,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = HighContrastSubtext,
  surfaceTint = PastelTerracotta,
  inverseSurface = HighContrastCharcoal,
  inverseOnSurface = WarmCreamLight,
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFFAF6F0),
  surfaceContainer = Color(0xFFF5EFE6),
  surfaceContainerHigh = Color(0xFFEFE8DD),
  surfaceContainerHighest = Color(0xFFE9E1D4),
  outline = HighContrastBorder,
  outlineVariant = WarmSandLight,
  error = Color(0xFFBA1A1A),
  onError = Color.White,
  errorContainer = Color(0xFFFFDAD6),
  onErrorContainer = Color(0xFF410002),
  scrim = Color.Black
)

private val GharDarkColorScheme = darkColorScheme(
  primary = PastelTerracottaDark,
  onPrimary = Color(0xFF45170A),
  primaryContainer = PastelTerracottaDarkContainer,
  onPrimaryContainer = Color(0xFFFFDBCF),
  inversePrimary = PastelTerracotta,
  secondary = PastelSageDark,
  onSecondary = Color(0xFF00391C),
  secondaryContainer = PastelSageDarkContainer,
  onSecondaryContainer = Color(0xFFB8F3C9),
  tertiary = PastelHoneyDark,
  onTertiary = Color(0xFF432C00),
  tertiaryContainer = PastelHoneyDarkContainer,
  onTertiaryContainer = Color(0xFFFFDF9E),
  background = BackgroundDark,
  onBackground = TextPrimaryDark,
  surface = SurfaceDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = TextSecondaryDark,
  surfaceTint = PastelTerracottaDark,
  inverseSurface = TextPrimaryDark,
  inverseOnSurface = BackgroundDark,
  surfaceContainerLowest = Color(0xFF0E0C0A),
  surfaceContainerLow = Color(0xFF191512),
  surfaceContainer = Color(0xFF1E1A17),
  surfaceContainerHigh = Color(0xFF28231F),
  surfaceContainerHighest = Color(0xFF332E29),
  outline = BorderDark,
  outlineVariant = SurfaceVariantDark,
  error = Color(0xFFFFB4AB),
  onError = Color(0xFF690005),
  errorContainer = Color(0xFF93000A),
  onErrorContainer = Color(0xFFFFDAD6),
  scrim = Color.Black
)

private fun androidx.compose.material3.ColorScheme.withAdaptiveContrast(): androidx.compose.material3.ColorScheme {
  return copy(
    onPrimary = primary.contrastingContentColor(),
    onSecondary = secondary.contrastingContentColor(),
    onTertiary = tertiary.contrastingContentColor(),
    onPrimaryContainer = primaryContainer.contrastingContentColor(
      lightContent = Color(0xFFFFF4EE),
      darkContent = OnPastelTerracottaContainer
    ),
    onSecondaryContainer = secondaryContainer.contrastingContentColor(
      lightContent = Color(0xFFEFFBF3),
      darkContent = OnPastelSageContainer
    ),
    onTertiaryContainer = tertiaryContainer.contrastingContentColor(
      lightContent = Color(0xFFFFF6E4),
      darkContent = OnPastelHoneyContainer
    ),
    onError = error.contrastingContentColor(),
    onErrorContainer = errorContainer.contrastingContentColor(
      lightContent = Color(0xFFFFEDEA),
      darkContent = Color(0xFF410002)
    ),
    inverseOnSurface = inverseSurface.contrastingContentColor(
      lightContent = WarmCreamLight,
      darkContent = HighContrastCharcoal
    )
  )
}

@Composable
fun GharTheme(
  themeMode: String = "SYSTEM",
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val systemDark = isSystemInDarkTheme()
  val darkTheme = when (themeMode.uppercase()) {
    "LIGHT" -> false
    "DARK" -> true
    else -> systemDark
  }

  val context = LocalContext.current
  val rawColorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> GharDarkColorScheme
    else -> GharLightColorScheme
  }
  val colorScheme = rawColorScheme.withAdaptiveContrast()

  GharFontWarmup.PrewarmComposeFontResolver()

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = Shapes,
    content = content
  )
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  GharTheme(
    themeMode = if (darkTheme) "DARK" else "LIGHT",
    dynamicColor = dynamicColor,
    content = content
  )
}
