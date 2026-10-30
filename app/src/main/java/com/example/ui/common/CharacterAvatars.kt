package com.example.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ClayGullakColor
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelTerracotta

@Composable
fun MaaAvatar(
  size: Dp = 48.dp,
  isSpeaking: Boolean = false,
  modifier: Modifier = Modifier
) {
  val breathScaleState = if (isSpeaking) {
    val infiniteTransition = rememberInfiniteTransition(label = "maaSpeakingPulse")
    infiniteTransition.animateFloat(
      initialValue = 0.97f,
      targetValue = 1.05f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "maaPulse"
    )
  } else {
    null
  }

  val bgBrush = remember {
    Brush.radialGradient(
      colors = listOf(
        Color(0xFFFFFAF3),
        Color(0xFFFDE3CE),
        PastelTerracotta.copy(alpha = 0.78f)
      )
    )
  }

  Box(
    modifier = modifier
      .size(size)
      .let { base ->
        if (breathScaleState != null) {
          base.graphicsLayer {
            val s = breathScaleState.value
            scaleX = s
            scaleY = s
          }
        } else {
          base
        }
      }
      .clip(CircleShape)
      .background(bgBrush)
      .border(1.5.dp, Color(0xFFB44626).copy(alpha = 0.62f), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .drawWithCache {
          val w = this.size.width
          val h = this.size.height

          // Inner zari gold accent ring
          val innerRingStroke = Stroke(width = (w * 0.025f).coerceAtLeast(0.9f))

          // Hair crown & side bun silhouette
          val hairColor = Color(0xFF2B1B17)
          val hairHighlight = Color(0xFF472F29)
          val skinWarm = Color(0xFFE9B691)
          val cheekWarmth = Color(0xFFD97D64).copy(alpha = 0.28f)
          val sareeCrimson = Color(0xFFB83B24)
          val sareeZariGold = Color(0xFFE7B649)
          val bindiCrimson = Color(0xFFA81D1D)
          val featureInk = Color(0xFF3A2119)

          // Saree shoulders & diagonal pallu drape at bottom of medallion
          val shoulderPath = Path().apply {
            moveTo(w * 0.14f, h * 0.98f)
            quadraticTo(w * 0.24f, h * 0.72f, w * 0.50f, h * 0.73f)
            quadraticTo(w * 0.76f, h * 0.72f, w * 0.86f, h * 0.98f)
            close()
          }
          val palluStripePath = Path().apply {
            moveTo(w * 0.24f, h * 0.95f)
            quadraticTo(w * 0.48f, h * 0.78f, w * 0.77f, h * 0.76f)
          }

          // Center-parted flowing hair framing the face
          val hairCrownPath = Path().apply {
            moveTo(w * 0.23f, h * 0.50f)
            quadraticTo(w * 0.20f, h * 0.17f, w * 0.50f, h * 0.16f)
            quadraticTo(w * 0.80f, h * 0.17f, w * 0.77f, h * 0.50f)
            quadraticTo(w * 0.74f, h * 0.62f, w * 0.67f, h * 0.64f)
            lineTo(w * 0.33f, h * 0.64f)
            quadraticTo(w * 0.26f, h * 0.62f, w * 0.23f, h * 0.50f)
            close()
          }

          // Curtain bangs parted gently in the middle
          val leftHairWave = Path().apply {
            moveTo(w * 0.50f, h * 0.22f)
            quadraticTo(w * 0.34f, h * 0.25f, w * 0.28f, h * 0.44f)
            quadraticTo(w * 0.25f, h * 0.30f, w * 0.36f, h * 0.20f)
            close()
          }
          val rightHairWave = Path().apply {
            moveTo(w * 0.50f, h * 0.22f)
            quadraticTo(w * 0.66f, h * 0.25f, w * 0.72f, h * 0.44f)
            quadraticTo(w * 0.75f, h * 0.30f, w * 0.64f, h * 0.20f)
            close()
          }

          val leftEyeArc = Path().apply {
            moveTo(w * 0.36f, h * 0.46f)
            quadraticTo(w * 0.41f, h * 0.43f, w * 0.45f, h * 0.46f)
          }
          val rightEyeArc = Path().apply {
            moveTo(w * 0.55f, h * 0.46f)
            quadraticTo(w * 0.59f, h * 0.43f, w * 0.64f, h * 0.46f)
          }
          val smilePath = Path().apply {
            moveTo(w * 0.42f, h * 0.585f)
            quadraticTo(w * 0.50f, h * 0.645f, w * 0.58f, h * 0.585f)
          }

          val lineStroke = Stroke(width = (w * 0.036f).coerceAtLeast(1.2f), cap = StrokeCap.Round)
          val zariStroke = Stroke(width = (w * 0.042f).coerceAtLeast(1.3f), cap = StrokeCap.Round)

          onDrawBehind {
            // Subtle inner gold zari ring
            drawCircle(
              color = sareeZariGold.copy(alpha = 0.55f),
              radius = w * 0.455f,
              center = Offset(w * 0.50f, h * 0.50f),
              style = innerRingStroke
            )

            // Side low-bun silhouette behind right ear
            drawCircle(
              color = hairColor,
              radius = w * 0.11f,
              center = Offset(w * 0.74f, h * 0.55f)
            )
            // Hair crown
            drawPath(path = hairCrownPath, color = hairColor)

            // Saree blouse & pallu drape
            drawPath(path = shoulderPath, color = sareeCrimson)
            drawPath(path = palluStripePath, color = sareeZariGold, style = zariStroke)

            // Neck
            drawOval(
              color = skinWarm,
              topLeft = Offset(w * 0.43f, h * 0.62f),
              size = Size(w * 0.14f, h * 0.14f)
            )

            // Gold jhumka earrings
            val jhumkaTopL = Offset(w * 0.275f, h * 0.525f)
            val jhumkaTopR = Offset(w * 0.725f, h * 0.525f)
            drawCircle(color = sareeZariGold, radius = w * 0.033f, center = jhumkaTopL)
            drawCircle(color = sareeZariGold, radius = w * 0.033f, center = jhumkaTopR)
            drawCircle(color = bindiCrimson, radius = w * 0.016f, center = jhumkaTopL)
            drawCircle(color = bindiCrimson, radius = w * 0.016f, center = jhumkaTopR)

            // Warm oval face
            drawOval(
              color = skinWarm,
              topLeft = Offset(w * 0.29f, h * 0.24f),
              size = Size(w * 0.42f, h * 0.44f)
            )

            // Parted front hair waves + center sindoor/parting glint
            drawPath(path = leftHairWave, color = hairColor)
            drawPath(path = rightHairWave, color = hairColor)
            drawLine(
              color = hairHighlight,
              start = Offset(w * 0.50f, h * 0.17f),
              end = Offset(w * 0.50f, h * 0.245f),
              strokeWidth = (w * 0.022f).coerceAtLeast(0.8f),
              cap = StrokeCap.Round
            )

            // Soft warm cheeks
            drawCircle(color = cheekWarmth, radius = w * 0.055f, center = Offset(w * 0.37f, h * 0.53f))
            drawCircle(color = cheekWarmth, radius = w * 0.055f, center = Offset(w * 0.63f, h * 0.53f))

            // Signature crimson Bindi at brow center
            drawCircle(
              color = bindiCrimson,
              radius = (w * 0.038f).coerceAtLeast(1.4f),
              center = Offset(w * 0.50f, h * 0.375f)
            )

            // Warm smiling eyes + pupils
            drawPath(path = leftEyeArc, color = featureInk, style = lineStroke)
            drawPath(path = rightEyeArc, color = featureInk, style = lineStroke)
            drawCircle(color = featureInk, radius = w * 0.022f, center = Offset(w * 0.408f, h * 0.468f))
            drawCircle(color = featureInk, radius = w * 0.022f, center = Offset(w * 0.592f, h * 0.468f))

            // Tiny gold nose stud (laung)
            drawCircle(
              color = sareeZariGold,
              radius = (w * 0.020f).coerceAtLeast(0.9f),
              center = Offset(w * 0.465f, h * 0.525f)
            )

            // Affectionate smile
            drawPath(path = smilePath, color = Color(0xFF9C3626), style = lineStroke)
          }
        }
    )
  }
}

@Composable
fun PapaAvatar(
  size: Dp = 48.dp,
  isSpeaking: Boolean = false,
  modifier: Modifier = Modifier
) {
  val breathScaleState = if (isSpeaking) {
    val infiniteTransition = rememberInfiniteTransition(label = "papaSpeakingPulse")
    infiniteTransition.animateFloat(
      initialValue = 0.97f,
      targetValue = 1.05f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "papaPulse"
    )
  } else {
    null
  }

  val bgBrush = remember {
    Brush.radialGradient(
      colors = listOf(
        Color(0xFFF6FAF4),
        Color(0xFFD6ECDD),
        PastelSage.copy(alpha = 0.86f)
      )
    )
  }

  Box(
    modifier = modifier
      .size(size)
      .let { base ->
        if (breathScaleState != null) {
          base.graphicsLayer {
            val s = breathScaleState.value
            scaleX = s
            scaleY = s
          }
        } else {
          base
        }
      }
      .clip(CircleShape)
      .background(bgBrush)
      .border(1.5.dp, Color(0xFF355F40).copy(alpha = 0.62f), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .drawWithCache {
          val w = this.size.width
          val h = this.size.height

          val hairDark = Color(0xFF2B2D2F)
          val silverTemple = Color(0xFF9DA7A2)
          val skinWarm = Color(0xFFE2AE88)
          val kurtaSage = Color(0xFF3E684A)
          val kurtaCollarCream = Color(0xFFF6F1E3)
          val frameBrass = Color(0xFF284D34)
          val moustacheDark = Color(0xFF332A27)

          val shoulderPath = Path().apply {
            moveTo(w * 0.13f, h * 0.98f)
            quadraticTo(w * 0.24f, h * 0.72f, w * 0.50f, h * 0.73f)
            quadraticTo(w * 0.76f, h * 0.72f, w * 0.87f, h * 0.98f)
            close()
          }
          val collarVPath = Path().apply {
            moveTo(w * 0.41f, h * 0.73f)
            lineTo(w * 0.50f, h * 0.87f)
            lineTo(w * 0.59f, h * 0.73f)
          }
          val hairCrownPath = Path().apply {
            moveTo(w * 0.26f, h * 0.44f)
            quadraticTo(w * 0.24f, h * 0.17f, w * 0.50f, h * 0.17f)
            quadraticTo(w * 0.76f, h * 0.17f, w * 0.74f, h * 0.44f)
            quadraticTo(w * 0.66f, h * 0.27f, w * 0.45f, h * 0.26f)
            quadraticTo(w * 0.32f, h * 0.28f, w * 0.26f, h * 0.44f)
            close()
          }
          val moustachePath = Path().apply {
            moveTo(w * 0.38f, h * 0.575f)
            quadraticTo(w * 0.44f, h * 0.535f, w * 0.50f, h * 0.558f)
            quadraticTo(w * 0.56f, h * 0.535f, w * 0.62f, h * 0.575f)
            quadraticTo(w * 0.55f, h * 0.592f, w * 0.50f, h * 0.576f)
            quadraticTo(w * 0.45f, h * 0.592f, w * 0.38f, h * 0.575f)
            close()
          }
          val smilePath = Path().apply {
            moveTo(w * 0.435f, h * 0.612f)
            quadraticTo(w * 0.50f, h * 0.650f, w * 0.565f, h * 0.612f)
          }

          val lensRadius = w * 0.095f
          val leftLens = Offset(w * 0.395f, h * 0.455f)
          val rightLens = Offset(w * 0.605f, h * 0.455f)
          val specStroke = Stroke(width = (w * 0.034f).coerceAtLeast(1.2f), cap = StrokeCap.Round)
          val collarStroke = Stroke(width = (w * 0.036f).coerceAtLeast(1.2f), cap = StrokeCap.Round)

          onDrawBehind {
            drawCircle(
              color = GoldCoin.copy(alpha = 0.42f),
              radius = w * 0.455f,
              center = Offset(w * 0.50f, h * 0.50f),
              style = Stroke(width = (w * 0.024f).coerceAtLeast(0.9f))
            )

            // Kurta shoulders & collar
            drawPath(path = shoulderPath, color = kurtaSage)
            drawPath(path = collarVPath, color = kurtaCollarCream, style = collarStroke)

            // Neck & ears
            drawOval(
              color = skinWarm,
              topLeft = Offset(w * 0.425f, h * 0.62f),
              size = Size(w * 0.15f, h * 0.14f)
            )
            drawCircle(color = skinWarm, radius = w * 0.042f, center = Offset(w * 0.28f, h * 0.47f))
            drawCircle(color = skinWarm, radius = w * 0.042f, center = Offset(w * 0.72f, h * 0.47f))

            // Warm oval face
            drawOval(
              color = skinWarm,
              topLeft = Offset(w * 0.29f, h * 0.24f),
              size = Size(w * 0.42f, h * 0.44f)
            )

            // Hair crown + distinguished silver temples
            drawPath(path = hairCrownPath, color = hairDark)
            drawOval(
              color = silverTemple,
              topLeft = Offset(w * 0.268f, h * 0.35f),
              size = Size(w * 0.042f, h * 0.10f)
            )
            drawOval(
              color = silverTemple,
              topLeft = Offset(w * 0.690f, h * 0.35f),
              size = Size(w * 0.042f, h * 0.10f)
            )

            // Spectacle lenses, pupils & bridge
            drawCircle(color = Color.White.copy(alpha = 0.45f), radius = lensRadius, center = leftLens)
            drawCircle(color = Color.White.copy(alpha = 0.45f), radius = lensRadius, center = rightLens)
            drawCircle(color = hairDark, radius = w * 0.021f, center = leftLens)
            drawCircle(color = hairDark, radius = w * 0.021f, center = rightLens)
            drawCircle(color = frameBrass, radius = lensRadius, center = leftLens, style = specStroke)
            drawCircle(color = frameBrass, radius = lensRadius, center = rightLens, style = specStroke)
            drawLine(
              color = frameBrass,
              start = Offset(leftLens.x + lensRadius, leftLens.y - lensRadius * 0.1f),
              end = Offset(rightLens.x - lensRadius, rightLens.y - lensRadius * 0.1f),
              strokeWidth = specStroke.width,
              cap = StrokeCap.Round
            )

            // Fatherly moustache & warm reassuring smile
            drawPath(path = moustachePath, color = moustacheDark)
            drawPath(path = smilePath, color = Color(0xFF7D3E2D), style = specStroke)
          }
        }
    )
  }
}

@Composable
fun BhaiAvatar(
  size: Dp = 48.dp,
  modifier: Modifier = Modifier
) {
  PapaAvatar(size = size, modifier = modifier)
}

/**
 * Tactile Handcrafted Red-Terracotta Clay Gullak (Piggy Bank) Asset.
 * Uses pre-decoded cached BitmapPainter for 0ms main-thread draw cost.
 */
@Composable
fun GullakCanvas(
  size: Dp = 148.dp,
  modifier: Modifier = Modifier
) {
  val potShape = remember(size) {
    RoundedCornerShape(
      topStart = size * 0.44f,
      topEnd = size * 0.44f,
      bottomStart = size * 0.34f,
      bottomEnd = size * 0.34f
    )
  }
  val vignetteBrush = remember {
    Brush.radialGradient(
      colorStops = arrayOf(
        0.0f to Color.Transparent,
        0.76f to Color.Transparent,
        1.0f to Color(0xFF2A160D).copy(alpha = 0.38f)
      )
    )
  }

  Surface(
    modifier = modifier.size(size),
    shape = potShape,
    color = Color(0xFF3D2418),
    tonalElevation = 3.dp,
    border = BorderStroke(2.dp, ClayGullakColor.copy(alpha = 0.55f))
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Image(
        painter = rememberCachedGharPainter(R.drawable.img_earthen_gullak_1792933032830),
        contentDescription = "Handcrafted Earthen Clay Gullak",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )

      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(vignetteBrush)
      )

      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(3.dp)
          .border(0.8.dp, GoldCoin.copy(alpha = 0.35f), potShape)
      )
    }
  }
}
