package com.example.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.PlusJakartaFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dramatic full-screen parabolic coin & folded paper-note drop into Bhai's Earthen Gullak slot.
 */
@Composable
fun FlyingCoinOverlay(
  startOffset: Offset,
  targetOffset: Offset,
  amountText: String = "+ ₹100",
  amountRupees: Long = 100L,
  onAnimationFinished: () -> Unit
) {
  val flightProgress = remember { Animatable(0f) }
  val coinScale = remember { Animatable(0.65f) }
  val coinRotation = remember { Animatable(-18f) }
  val burstProgress = remember { Animatable(0f) }
  val burstAlpha = remember { Animatable(0f) }

  val isPaperBill = amountRupees >= 200L

  LaunchedEffect(Unit) {
    launch {
      coinScale.animateTo(
        targetValue = 1.24f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
      )
    }
    launch {
      coinRotation.animateTo(
        targetValue = if (isPaperBill) 14f else 360f,
        animationSpec = tween(640, easing = LinearOutSlowInEasing)
      )
    }
    flightProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(640, easing = FastOutSlowInEasing)
    )

    burstAlpha.snapTo(1f)
    launch {
      burstProgress.animateTo(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
      )
    }
    burstAlpha.animateTo(
      targetValue = 0f,
      animationSpec = tween(420, easing = FastOutSlowInEasing)
    )
    delay(40)
    onAnimationFinished()
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val density = LocalDensity.current
    val isFlying by androidx.compose.runtime.remember {
      androidx.compose.runtime.derivedStateOf { flightProgress.value < 0.98f }
    }
    val isBursting by androidx.compose.runtime.remember {
      androidx.compose.runtime.derivedStateOf { burstAlpha.value > 0.01f }
    }

    if (isFlying) {
      if (isPaperBill) {
        val notePalette = if (amountRupees >= 500L) {
          RupeeDenominationPalette.NOTE_500
        } else {
          RupeeDenominationPalette.NOTE_200
        }
        val halfW = with(density) { 88.dp.toPx() }
        val halfH = with(density) { 42.dp.toPx() }
        ArtisanalRupeeNote(
          palette = notePalette,
          width = 176.dp,
          height = 84.dp,
          modifier = Modifier.graphicsLayer {
            val t = flightProgress.value
            val arcPeak = -190f * (4f * t * (1f - t))
            val currentX = (1f - t) * startOffset.x + t * targetOffset.x
            val currentY = (1f - t) * startOffset.y + t * targetOffset.y + arcPeak
            val entryScale = if (t > 0.78f) (1f - (t - 0.78f) * 3.6f).coerceIn(0.18f, 1f) else 1f
            translationX = currentX - halfW
            translationY = currentY - halfH
            scaleX = coinScale.value * entryScale
            scaleY = coinScale.value * entryScale
            rotationZ = coinRotation.value
          }
        )
      } else {
        val coinSizeDp = 66.dp
        val halfSizePx = with(density) { (coinSizeDp / 2).toPx() }
        val isCopper = amountRupees >= 100L
        Box(
          modifier = Modifier
            .size(coinSizeDp)
            .graphicsLayer {
              val t = flightProgress.value
              val arcPeak = -190f * (4f * t * (1f - t))
              val currentX = (1f - t) * startOffset.x + t * targetOffset.x
              val currentY = (1f - t) * startOffset.y + t * targetOffset.y + arcPeak
              val entryScale = if (t > 0.78f) (1f - (t - 0.78f) * 3.6f).coerceIn(0.18f, 1f) else 1f
              translationX = currentX - halfSizePx
              translationY = currentY - halfSizePx
              scaleX = coinScale.value * entryScale
              scaleY = coinScale.value * entryScale
              rotationZ = coinRotation.value
            }
            .shadow(8.dp, CircleShape)
            .drawWithCache {
              val center = Offset(size.width / 2f, size.height / 2f)
              val outerR = size.minDimension / 2f
              val outerHairlineR = outerR * 0.82f
              val innerHairlineR = outerR * 0.74f
              val binduOrbitR = (outerHairlineR + innerHairlineR) * 0.5f
              val binduRadius = 1.2.dp.toPx()

              val faceBrush = Brush.verticalGradient(
                colors = when {
                  amountRupees >= 1000L -> listOf(Color(0xFFF3E6CA), Color(0xFFE4CFA3))
                  amountRupees >= 500L -> listOf(Color(0xFFECEEF0), Color(0xFFDADFE4))
                  isCopper -> listOf(Color(0xFFF3DFD3), Color(0xFFE4C5B3))
                  else -> listOf(Color(0xFFF2E8D5), Color(0xFFE2D3B5))
                }
              )
              val rimColor = when {
                amountRupees >= 1000L -> Color(0xFFA68446)
                amountRupees >= 500L -> Color(0xFF8A949E)
                isCopper -> Color(0xFFA86D51)
                else -> Color(0xFF9E845B)
              }
              val rimStroke = Stroke(width = 1.15.dp.toPx())
              val outerHairlineStroke = Stroke(width = 0.85.dp.toPx())
              val innerHairlineStroke = Stroke(width = 0.65.dp.toPx())
              val cardinalBindus = arrayOf(
                Offset(center.x, center.y - binduOrbitR),
                Offset(center.x + binduOrbitR, center.y),
                Offset(center.x, center.y + binduOrbitR),
                Offset(center.x - binduOrbitR, center.y)
              )

              onDrawBehind {
                drawCircle(brush = faceBrush, radius = outerR, center = center)
                drawCircle(color = rimColor.copy(alpha = 0.78f), radius = outerR, center = center, style = rimStroke)
                drawCircle(color = rimColor.copy(alpha = 0.42f), radius = outerHairlineR, center = center, style = outerHairlineStroke)
                drawCircle(color = rimColor.copy(alpha = 0.24f), radius = innerHairlineR, center = center, style = innerHairlineStroke)
                for (pt in cardinalBindus) {
                  drawCircle(color = rimColor.copy(alpha = 0.55f), radius = binduRadius, center = pt)
                }
              }
            },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "₹$amountRupees",
            style = TextStyle(
              fontFamily = PlusJakartaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSynthesis = FontSynthesis.All,
              fontSize = 14.5.sp,
              letterSpacing = (-0.3).sp
            ),
            color = if (amountRupees >= 500L && amountRupees < 1000L) Color(0xFF1D232A) else Color(0xFF291F12)
          )
        }
      }
    }

    // Celebratory clink burst + floating handwritten ink tag above the Gullak slot
    if (isBursting) {
      val numParticles = 10
      Canvas(modifier = Modifier.fillMaxSize()) {
        val b = burstProgress.value
        val a = burstAlpha.value
        for (i in 0 until numParticles) {
          val angle = (i.toFloat() / numParticles) * 2 * Math.PI.toFloat()
          val radius = 28f + b * 76f
          val px = targetOffset.x + radius * cos(angle)
          val py = targetOffset.y + radius * sin(angle)
          val pSize = (1f - b) * 8f + 2.5f

          drawCircle(
            color = GoldCoin.copy(alpha = a),
            radius = pSize,
            center = Offset(px, py)
          )
        }
      }

      val tagHalfW = with(density) { 54.dp.toPx() }
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF5BA),
        border = BorderStroke(1.dp, Color(0xFF2E6B47)),
        shadowElevation = 6.dp,
        modifier = Modifier.graphicsLayer {
          translationX = targetOffset.x - tagHalfW
          translationY = targetOffset.y - 72f - (burstProgress.value * 42f)
          alpha = burstAlpha.value
        }
      ) {
        Text(
          text = amountText,
          style = GharHandStyles.moneyMedium,
          color = Color(0xFF1F5E3B),
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        )
      }
    }
  }
}
