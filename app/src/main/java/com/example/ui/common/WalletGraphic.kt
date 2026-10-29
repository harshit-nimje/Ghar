package com.example.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.WarmWood

private val LeatherWalletShape = RoundedCornerShape(24.dp)
private val InnerStitchShape = RoundedCornerShape(20.dp)

/**
 * Tactile Saddle-Stitched Cognac Leather Batua (Wallet) Asset.
 * Uses pre-decoded RGB_565 bitmap cache for zero UI-thread decode jank.
 */
@Composable
fun WalletGraphic(
  thicknessRatio: Float,
  size: Dp = 92.dp,
  modifier: Modifier = Modifier
) {
  val animatedFullness by animateFloatAsState(
    targetValue = thicknessRatio.coerceIn(0.12f, 1.0f),
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "leatherBatuaFullness"
  )

  val plumpScale = 0.94f + (animatedFullness * 0.08f)
  val vignetteBrush = remember {
    Brush.radialGradient(
      colorStops = arrayOf(
        0.0f to Color.Transparent,
        0.78f to Color.Transparent,
        1.0f to Color(0xFF2B180A).copy(alpha = 0.35f)
      )
    )
  }

  Surface(
    modifier = modifier
      .size(size)
      .graphicsLayer {
        scaleX = plumpScale
        scaleY = plumpScale
      }
      .testTag("wallet_graphic"),
    shape = LeatherWalletShape,
    color = WarmWood,
    shadowElevation = 4.dp,
    border = BorderStroke(1.8.dp, Color(0xFFC89158).copy(alpha = 0.65f))
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Image(
        painter = rememberCachedGharPainter(R.drawable.img_leather_batua_1792933032717),
        contentDescription = "Handcrafted Stitched Leather Batua Wallet",
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
          .padding(3.5.dp)
          .border(0.8.dp, GoldCoin.copy(alpha = 0.32f), InnerStitchShape)
      )
    }
  }
}
