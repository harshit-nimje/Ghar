package com.example.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.localization.GharTerms
import com.example.core.localization.LocalPlainEnglishMode
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.PlusJakartaFontFamily
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lazy coordinate holder used to track anchor positions inside scrollable lists.
 * Stores LayoutCoordinates on placement and computes boundsInRoot() ONLY when tapped,
 * eliminating per-frame coordinate math during scrolling.
 */
@Stable
class MutableOffsetRef(initial: Offset = Offset.Zero) {
  var coords: LayoutCoordinates? = null
  private var explicitOffset: Offset? = if (initial != Offset.Zero) initial else null

  var value: Offset
    get() = coords?.takeIf { it.isAttached }?.boundsInRoot()?.center
      ?: explicitOffset
      ?: Offset.Zero
    set(newValue) {
      explicitOffset = newValue
    }
}

val JharokhaCardShape = RoundedCornerShape(
  topStart = 28.dp,
  topEnd = 28.dp,
  bottomStart = 22.dp,
  bottomEnd = 22.dp
)

val PebbleCardShape = RoundedCornerShape(
  topStart = 24.dp,
  topEnd = 18.dp,
  bottomEnd = 24.dp,
  bottomStart = 18.dp
)

/**
 * Single-pass top header wash modifier with subtle sunlit warmth — zero fullscreen overdraw.
 */
fun Modifier.pastelHomeBackdrop(
  primaryWash: Color,
  secondaryWash: Color,
  tertiaryWash: Color = PastelHoney
): Modifier = this.drawWithCache {
  val washHeight = 260.dp.toPx().coerceAtMost(size.height)
  val topBrush = Brush.verticalGradient(
    colors = listOf(
      primaryWash.copy(alpha = 0.13f),
      secondaryWash.copy(alpha = 0.06f),
      tertiaryWash.copy(alpha = 0.02f),
      Color.Transparent
    ),
    startY = 0f,
    endY = washHeight.coerceAtLeast(1f)
  )

  onDrawBehind {
    drawRect(brush = topBrush, size = Size(size.width, washHeight))
  }
}

/**
 * Handcrafted Rajasthani brass & terracotta concentric rounded corner filigree for hero cards.
 * Uses smooth circular corner arcs that mirror the rounded card silhouette rather than sharp 90° angles.
 * Pure cached Path instructions — zero extra LayoutNodes or allocations during scroll.
 */
fun Modifier.jharokhaCornerFiligree(
  accentColor: Color,
  inset: Dp = 9.dp,
  topCornerRadius: Dp = 26.dp,
  bottomCornerRadius: Dp = 22.dp
): Modifier = this.drawWithCache {
  val pad = inset.toPx()
  val rTop = (topCornerRadius.toPx() - pad).coerceAtLeast(8.dp.toPx())
  val rBot = (bottomCornerRadius.toPx() - pad).coerceAtLeast(7.dp.toPx())
  val leg = 8.dp.toPx()
  val strokeStyle = Stroke(
    width = 1.15.dp.toPx(),
    cap = StrokeCap.Round,
    join = StrokeJoin.Round
  )
  val dotRadius = 1.8.dp.toPx()
  val w = size.width
  val h = size.height
  val lineTint = accentColor.copy(alpha = 0.30f)
  val dotTint = GoldCoin.copy(alpha = 0.62f)

  val cornerPath = Path().apply {
    if (w > (rTop + leg + pad) * 2.2f && h > (rTop + rBot + leg * 2f)) {
      // Top-left concentric rounded arc
      moveTo(pad, pad + rTop + leg)
      lineTo(pad, pad + rTop)
      arcTo(
        rect = Rect(pad, pad, pad + rTop * 2f, pad + rTop * 2f),
        startAngleDegrees = 180f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
      )
      lineTo(pad + rTop + leg, pad)

      // Top-right concentric rounded arc
      moveTo(w - pad - rTop - leg, pad)
      lineTo(w - pad - rTop, pad)
      arcTo(
        rect = Rect(w - pad - rTop * 2f, pad, w - pad, pad + rTop * 2f),
        startAngleDegrees = 270f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
      )
      lineTo(w - pad, pad + rTop + leg)

      // Bottom-right concentric rounded arc
      moveTo(w - pad, h - pad - rBot - leg)
      lineTo(w - pad, h - pad - rBot)
      arcTo(
        rect = Rect(w - pad - rBot * 2f, h - pad - rBot * 2f, w - pad, h - pad),
        startAngleDegrees = 0f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
      )
      lineTo(w - pad - rBot - leg, h - pad)

      // Bottom-left concentric rounded arc
      moveTo(pad + rBot + leg, h - pad)
      lineTo(pad + rBot, h - pad)
      arcTo(
        rect = Rect(pad, h - pad - rBot * 2f, pad + rBot * 2f, h - pad),
        startAngleDegrees = 90f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
      )
      lineTo(pad, h - pad - rBot - leg)
    }
  }
  val binduOffset = pad + rTop * 0.42f

  onDrawWithContent {
    drawContent()
    if (!cornerPath.isEmpty) {
      drawPath(path = cornerPath, color = lineTint, style = strokeStyle)
      drawCircle(dotTint, dotRadius, Offset(binduOffset, binduOffset))
      drawCircle(dotTint, dotRadius, Offset(w - binduOffset, binduOffset))
    }
  }
}

/**
 * Delicate Indian brass-and-kolam diamond divider flourish.
 * Uses pure drawWithCache vector geometry for 0-allocation visual richness.
 */
@Composable
fun RangoliBrassOrnament(
  accentColor: Color = PastelTerracotta,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(14.dp)
      .drawWithCache {
        val cy = size.height / 2f
        val cx = size.width / 2f
        val wingSpan = (size.width * 0.34f).coerceAtMost(132.dp.toPx())
        val diamondR = 4.2.dp.toPx()
        val sideDotR = 2.0.dp.toPx()
        val strokePx = 1.dp.toPx()

        val leftBrush = Brush.horizontalGradient(
          colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.45f)),
          startX = cx - wingSpan,
          endX = cx - diamondR * 2.5f
        )
        val rightBrush = Brush.horizontalGradient(
          colors = listOf(accentColor.copy(alpha = 0.45f), Color.Transparent),
          startX = cx + diamondR * 2.5f,
          endX = cx + wingSpan
        )
        val diamondPath = Path().apply {
          moveTo(cx, cy - diamondR)
          lineTo(cx + diamondR, cy)
          lineTo(cx, cy + diamondR)
          lineTo(cx - diamondR, cy)
          close()
        }

        onDrawBehind {
          drawLine(
            brush = leftBrush,
            start = Offset(cx - wingSpan, cy),
            end = Offset(cx - diamondR * 2.6f, cy),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
          )
          drawLine(
            brush = rightBrush,
            start = Offset(cx + diamondR * 2.6f, cy),
            end = Offset(cx + wingSpan, cy),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
          )
          drawPath(path = diamondPath, color = accentColor.copy(alpha = 0.78f))
          drawCircle(
            color = GoldCoin.copy(alpha = 0.85f),
            radius = sideDotR,
            center = Offset(cx - diamondR * 1.9f, cy)
          )
          drawCircle(
            color = GoldCoin.copy(alpha = 0.85f),
            radius = sideDotR,
            center = Offset(cx + diamondR * 1.9f, cy)
          )
        }
      }
  )
}

/**
 * Full-Bleed Top-Half Room Illustration with Smooth Bottom Fade-Out,
 * GPU-only scroll parallax, sunlit golden-hour dust motes, and brass kolam flourish.
 */
@Composable
fun IllustratedRoomBanner(
  @DrawableRes artworkRes: Int,
  roomBadge: String,
  title: String,
  accentColor: Color,
  keyHighlight: String? = null,
  heroHeight: Dp = 304.dp,
  scrollOffsetProvider: () -> Int = { 0 },
  onHelpClick: (() -> Unit)? = null,
  trailingBadge: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val bgColor = MaterialTheme.colorScheme.background

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(heroHeight)
      .clip(RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp))
  ) {
    Image(
      painter = rememberCachedGharPainter(artworkRes),
      contentDescription = title,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          val rawScroll = scrollOffsetProvider().coerceAtLeast(0).toFloat()
          translationY = rawScroll * 0.26f
          val zoom = 1f + (rawScroll / 3200f).coerceIn(0f, 0.045f)
          scaleX = zoom
          scaleY = zoom
        }
        .drawWithCache {
          val w = size.width
          val h = size.height
          val sunbeamBrush = Brush.linearGradient(
            colors = listOf(
              Color(0xFFFFF4D2).copy(alpha = 0.20f),
              accentColor.copy(alpha = 0.08f),
              Color.Transparent
            ),
            start = Offset(w * 0.12f, 0f),
            end = Offset(w * 0.72f, h * 0.68f)
          )
          val scrimBrush = Brush.verticalGradient(
            colorStops = arrayOf(
              0.0f to Color.Black.copy(alpha = 0.30f),
              0.24f to Color.Transparent,
              0.55f to bgColor.copy(alpha = 0.20f),
              0.82f to bgColor.copy(alpha = 0.90f),
              1.0f to bgColor
            )
          )
          val motes = arrayOf(
            Offset(w * 0.20f, h * 0.28f) to 2.2.dp.toPx(),
            Offset(w * 0.34f, h * 0.22f) to 1.6.dp.toPx(),
            Offset(w * 0.68f, h * 0.30f) to 2.4.dp.toPx(),
            Offset(w * 0.80f, h * 0.20f) to 1.8.dp.toPx()
          )
          val moteColor = Color(0xFFFFF6D6).copy(alpha = 0.50f)

          onDrawWithContent {
            drawContent()
            drawRect(sunbeamBrush)
            motes.forEach { (pt, r) ->
              drawCircle(color = moteColor, radius = r, center = pt)
            }
            drawRect(scrimBrush)
          }
        }
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 18.dp, vertical = 10.dp)
        .align(Alignment.TopCenter),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = Color(0xFF1E1510).copy(alpha = 0.66f),
        border = BorderStroke(0.9.dp, GoldCoin.copy(alpha = 0.42f)),
        modifier = Modifier
          .defaultMinSize(minHeight = 44.dp)
          .weight(1f, fill = false)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(accentColor)
          )
          Spacer(modifier = Modifier.width(7.dp))
          Text(
            text = roomBadge,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFFBF4),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (onHelpClick != null || trailingBadge != null) {
        Spacer(modifier = Modifier.width(8.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (onHelpClick != null) {
            RoomGuideHelpPill(
              roomName = title,
              onClick = onHelpClick
            )
          }
          if (trailingBadge != null) {
            trailingBadge()
          }
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomStart)
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      if (!keyHighlight.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = keyHighlight,
          style = GharHandStyles.stickyNoteMeta,
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.primary
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      RangoliBrassOrnament(accentColor = accentColor)
    }
  }
}

/**
 * Full-Bleed Sunlit Rasoi Top Hero for Maa's Kitchen.
 * Displays "Welcome back, <Name>" cleanly directly on the sunlit bottom fade-out without a boxed card.
 */
@Composable
fun KitchenGreetingHero(
  userName: String = "Friend",
  streakDays: Int = 1,
  scrollOffsetProvider: () -> Int = { 0 },
  onAvatarClick: () -> Unit,
  onHelpClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val plainEnglish = LocalPlainEnglishMode.current
  val cleanName = userName.trim().ifBlank { "Friend" }
  val (timeWhisper, accentTint) = remember(plainEnglish) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    if (plainEnglish) {
      when (hour) {
        in 5..11 -> Pair("Good morning • Daily budget overview", PastelHoney)
        in 12..16 -> Pair("Good afternoon • Daily budget overview", PastelSage)
        in 17..20 -> Pair("Good evening • Review today's expenses", PastelTerracotta)
        else -> Pair("Evening summary • Ready for tomorrow", PastelSage)
      }
    } else {
      when (hour) {
        in 5..11 -> Pair("Suprabhat • Ready for today's check-in", PastelHoney)
        in 12..16 -> Pair("Afternoon check-in in Maa's rasoi", PastelSage)
        in 17..20 -> Pair("Evening check-in at home", PastelTerracotta)
        else -> Pair("Night summary in Maa's rasoi", PastelSage)
      }
    }
  }

  val bgColor = MaterialTheme.colorScheme.background

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(306.dp)
      .clip(RoundedCornerShape(0.dp))
      .testTag("kitchen_greeting_hero")
  ) {
    Image(
      painter = rememberCachedGharPainter(R.drawable.img_kitchen_hearth_1792931327219),
      contentDescription = if (plainEnglish) "Home Budget Header" else "Maa's Sunlit Rasoi",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          val rawScroll = scrollOffsetProvider().coerceAtLeast(0).toFloat()
          translationY = rawScroll * 0.26f
          val zoom = 1f + (rawScroll / 3200f).coerceIn(0f, 0.045f)
          scaleX = zoom
          scaleY = zoom
        }
        .drawWithCache {
          val w = size.width
          val h = size.height
          val sunbeamBrush = Brush.linearGradient(
            colors = listOf(
              Color(0xFFFFF3CC).copy(alpha = 0.22f),
              PastelTerracotta.copy(alpha = 0.08f),
              Color.Transparent
            ),
            start = Offset(w * 0.08f, 0f),
            end = Offset(w * 0.70f, h * 0.65f)
          )
          val scrimBrush = Brush.verticalGradient(
            colorStops = arrayOf(
              0.0f to Color.Black.copy(alpha = 0.28f),
              0.24f to Color.Transparent,
              0.54f to bgColor.copy(alpha = 0.22f),
              0.81f to bgColor.copy(alpha = 0.91f),
              1.0f to bgColor
            )
          )
          val motes = arrayOf(
            Offset(w * 0.18f, h * 0.26f) to 2.4.dp.toPx(),
            Offset(w * 0.36f, h * 0.19f) to 1.7.dp.toPx(),
            Offset(w * 0.66f, h * 0.28f) to 2.2.dp.toPx(),
            Offset(w * 0.82f, h * 0.22f) to 1.8.dp.toPx()
          )
          val moteColor = Color(0xFFFFF6D6).copy(alpha = 0.52f)

          onDrawWithContent {
            drawContent()
            drawRect(sunbeamBrush)
            motes.forEach { (pt, r) ->
              drawCircle(color = moteColor, radius = r, center = pt)
            }
            drawRect(scrimBrush)
          }
        }
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 18.dp, vertical = 10.dp)
        .align(Alignment.TopCenter),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = Color(0xFF1E1510).copy(alpha = 0.66f),
        border = BorderStroke(0.9.dp, GoldCoin.copy(alpha = 0.42f)),
        modifier = Modifier
          .defaultMinSize(minHeight = 44.dp)
          .weight(1f, fill = false)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(accentTint)
          )
          Spacer(modifier = Modifier.width(7.dp))
          val dayWord = if (streakDays == 1) "Day Under Budget" else "Days Under Budget"
          val roomPrefix = if (plainEnglish) "Home" else "Maa's Rasoi"
          Text(
            text = "$roomPrefix • $streakDays $dayWord",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFFBF4),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (onHelpClick != null) {
          RoomGuideHelpPill(
            roomName = if (plainEnglish) "Home" else "Maa's Kitchen",
            onClick = onHelpClick
          )
        }

        Surface(
          onClick = onAvatarClick,
          shape = CircleShape,
          color = Color(0xFF1E1510).copy(alpha = 0.70f),
          border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.46f)),
          modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .testTag("maa_header_avatar")
        ) {
          Row(
            modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            MaaAvatar(size = 32.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = GharTerms.chatWithMaaLabel(plainEnglish),
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFFFFBF4),
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
          }
        }
      }
    }

    // Clean, background-free greeting resting seamlessly in the sunlit bottom fade-out
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomStart)
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      Text(
        text = "Welcome back, $cleanName",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag("home_welcome_greeting")
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = timeWhisper,
        style = GharHandStyles.stickyNoteMeta,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(6.dp))
      RangoliBrassOrnament(accentColor = PastelTerracotta)
    }
  }
}

/**
 * Tactile Stitched Leather Wallet Gauge with direct, factual status label.
 */
@Composable
fun SafeGrowthGauge(
  percentageSafe: Float,
  isPaceHot: Boolean,
  modifier: Modifier = Modifier
) {
  val statusTint = if (isPaceHot) PastelTerracotta else PastelSage

  Column(
    modifier = modifier.testTag("safe_growth_gauge"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    WalletGraphic(
      thicknessRatio = percentageSafe,
      size = 90.dp
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = if (isPaceHot) "Over Pace" else "On Track",
      style = GharHandStyles.stickyNoteMeta,
      color = statusTint,
      fontWeight = FontWeight.Bold,
      maxLines = 1
    )
  }
}

/**
 * Tactile Artisanal Rubber-Ink Stamp on Khadi Linen — single RenderNode clip + inner registration ticks.
 */
@Composable
fun TactileMomentStamp(
  label: String,
  sublabel: String,
  accentColor: Color,
  containerWash: Color,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.92f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "stampPress"
  )

  val stampShape = remember {
    RoundedCornerShape(topStart = 20.dp, topEnd = 14.dp, bottomEnd = 20.dp, bottomStart = 14.dp)
  }

  Surface(
    onClick = onClick,
    interactionSource = interactionSource,
    shape = stampShape,
    color = containerWash,
    border = BorderStroke(1.1.dp, accentColor.copy(alpha = 0.42f)),
    modifier = modifier
      .defaultMinSize(minHeight = 94.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
  ) {
    Column(
      modifier = Modifier
        .drawWithCache {
          val inset = 4.5.dp.toPx()
          val cornerR = 14.dp.toPx()
          val leg = 4.dp.toPx()
          val strokeStyle = Stroke(
            width = 0.95.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
          )
          val ink = accentColor.copy(alpha = 0.24f)
          val w = size.width
          val h = size.height
          val arcPath = Path().apply {
            if (w > (cornerR + inset) * 2f && h > (cornerR + inset) * 2f) {
              // Top-left concentric rounded corner
              moveTo(inset, inset + cornerR + leg)
              lineTo(inset, inset + cornerR)
              arcTo(
                rect = Rect(inset, inset, inset + cornerR * 2f, inset + cornerR * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
              )
              lineTo(inset + cornerR + leg, inset)

              // Bottom-right concentric rounded corner
              moveTo(w - inset, h - inset - cornerR - leg)
              lineTo(w - inset, h - inset - cornerR)
              arcTo(
                rect = Rect(w - inset - cornerR * 2f, h - inset - cornerR * 2f, w - inset, h - inset),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
              )
              lineTo(w - inset - cornerR - leg, h - inset)
            }
          }
          onDrawBehind {
            if (!arcPath.isEmpty) {
              drawPath(path = arcPath, color = ink, style = strokeStyle)
            }
          }
        }
        .padding(horizontal = 6.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.20f)),
        contentAlignment = Alignment.Center
      ) {
        icon()
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = sublabel,
        style = GharHandStyles.moneySmall,
        color = accentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

private data class HeritageCoinPalette(
  val faceTop: Color,
  val faceBottom: Color,
  val outerRim: Color,
  val innerLine: Color,
  val binduTint: Color,
  val inkColor: Color
)

private fun resolveHeritageCoinPalette(denominationRupees: Long): HeritageCoinPalette = when {
  denominationRupees >= 1000L -> HeritageCoinPalette(
    // Swarna-Kansa: Muted antique warm brass-ochre
    faceTop = Color(0xFFF3E6CA),
    faceBottom = Color(0xFFE4CFA3),
    outerRim = Color(0xFFA68446),
    innerLine = Color(0xFF96753A),
    binduTint = Color(0xFF8A692F),
    inkColor = Color(0xFF2B1F0E)
  )
  denominationRupees >= 500L -> HeritageCoinPalette(
    // Chandi: Brushed heirloom silver-pewter
    faceTop = Color(0xFFECEEF0),
    faceBottom = Color(0xFFDADFE4),
    outerRim = Color(0xFF8A949E),
    innerLine = Color(0xFF7B8590),
    binduTint = Color(0xFF6B7580),
    inkColor = Color(0xFF1D232A)
  )
  denominationRupees >= 100L -> HeritageCoinPalette(
    // Tamba: Soft matte terracotta-copper
    faceTop = Color(0xFFF3DFD3),
    faceBottom = Color(0xFFE4C5B3),
    outerRim = Color(0xFFA86D51),
    innerLine = Color(0xFF995F44),
    binduTint = Color(0xFF8C5339),
    inkColor = Color(0xFF2E1910)
  )
  else -> HeritageCoinPalette(
    // Kansa: Traditional muted bell-metal sand-bronze (₹50)
    faceTop = Color(0xFFF2E8D5),
    faceBottom = Color(0xFFE2D3B5),
    outerRim = Color(0xFF9E845B),
    innerLine = Color(0xFF8E744C),
    binduTint = Color(0xFF7F6640),
    inkColor = Color(0xFF291F12)
  )
}

/**
 * Understated, minimal yet traditional Indian heritage metal coin (*Kansa*, *Tamba*, *Chandi*, *Swarna-Kansa*)
 * for feeding Bhai's Gullak. Uses a calm matte 2-stop wash, concentric double-hairline *mandala* ring,
 * and 4 subtle cardinal *bindu* dots — pre-cached in drawWithCache for 120fps fluidity and crisp legibility.
 */
@Composable
fun TactileCoinMedallion(
  amountLabel: String,
  caption: String,
  ringColor: Color,
  fillColor: Color,
  denominationRupees: Long = 100L,
  positionRef: MutableOffsetRef? = null,
  onPositionedCenter: ((Offset) -> Unit)? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.93f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
    label = "coinPress"
  )
  val palette = remember(denominationRupees) { resolveHeritageCoinPalette(denominationRupees) }
  val numeralFontSize = when {
    amountLabel.length >= 6 -> 13.5.sp
    amountLabel.length == 5 -> 14.5.sp
    else -> 16.sp
  }

  Column(
    modifier = modifier
      .defaultMinSize(minWidth = 72.dp, minHeight = 86.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .let { base ->
        if (positionRef != null && onPositionedCenter == null) {
          base.onPlaced { coords ->
            positionRef.coords = coords
          }
        } else if (positionRef != null || onPositionedCenter != null) {
          base.onPlaced { coords ->
            positionRef?.coords = coords
            if (onPositionedCenter != null && coords.isAttached) {
              onPositionedCenter(coords.boundsInRoot().center)
            }
          }
        } else {
          base
        }
      }
      .clip(RoundedCornerShape(16.dp))
      .clickable(interactionSource = interactionSource, indication = null) { onClick() },
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(68.dp)
        .drawWithCache {
          val w = size.width
          val h = size.height
          val center = Offset(w / 2f, h / 2f)
          val shadowCenter = Offset(w / 2f, h / 2f + 1.5.dp.toPx())
          val outerR = (size.minDimension / 2f) - 2.dp.toPx()
          val outerHairlineR = outerR * 0.82f
          val innerHairlineR = outerR * 0.74f
          val binduOrbitR = (outerHairlineR + innerHairlineR) * 0.5f
          val binduRadius = 1.25.dp.toPx()

          val faceBrush = Brush.verticalGradient(
            colors = listOf(palette.faceTop, palette.faceBottom),
            startY = center.y - outerR,
            endY = center.y + outerR
          )
          val outerRimStroke = Stroke(width = 1.15.dp.toPx())
          val outerHairlineStroke = Stroke(width = 0.85.dp.toPx())
          val innerHairlineStroke = Stroke(width = 0.65.dp.toPx())

          val cardinalBindus = arrayOf(
            Offset(center.x, center.y - binduOrbitR),
            Offset(center.x + binduOrbitR, center.y),
            Offset(center.x, center.y + binduOrbitR),
            Offset(center.x - binduOrbitR, center.y)
          )

          onDrawBehind {
            // Soft grounded shadow
            drawCircle(
              color = Color(0x1E1A1108),
              radius = outerR,
              center = shadowCenter
            )
            // Calm matte heirloom metal face
            drawCircle(
              brush = faceBrush,
              radius = outerR,
              center = center
            )
            // Subtle outer turned rim
            drawCircle(
              color = palette.outerRim.copy(alpha = 0.78f),
              radius = outerR,
              center = center,
              style = outerRimStroke
            )
            // Minimal concentric traditional mandala hairlines
            drawCircle(
              color = palette.innerLine.copy(alpha = 0.42f),
              radius = outerHairlineR,
              center = center,
              style = outerHairlineStroke
            )
            drawCircle(
              color = palette.innerLine.copy(alpha = 0.24f),
              radius = innerHairlineR,
              center = center,
              style = innerHairlineStroke
            )
            // 4 delicate cardinal bindu points (N / E / S / W)
            for (pt in cardinalBindus) {
              drawCircle(
                color = palette.binduTint.copy(alpha = 0.55f),
                radius = binduRadius,
                center = pt
              )
            }
          }
        },
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = amountLabel,
        style = TextStyle(
          fontFamily = PlusJakartaFontFamily,
          fontWeight = FontWeight.Bold,
          fontSynthesis = FontSynthesis.All,
          fontSize = numeralFontSize,
          letterSpacing = (-0.3).sp
        ),
        color = palette.inkColor,
        textAlign = TextAlign.Center,
        maxLines = 1
      )
    }
    Spacer(modifier = Modifier.height(5.dp))
    Text(
      text = caption,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 11.5.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/**
 * Pastel Botanical Money-Plant Vine with scribbled milestone tags.
 * Drawn statically via drawWithCache so scrolling into view never triggers continuous path animation jank.
 */
@Composable
fun BotanicalCompoundingVine(
  milestone1Yr: String,
  milestone3Yr: String,
  milestone5Yr: String,
  year1Label: String = "Year 1",
  year2Label: String = "Year 3",
  year3Label: String = "Year 5",
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(154.dp)
      .clip(RoundedCornerShape(22.dp))
      .drawWithCache {
        val w = size.width
        val h = size.height
        val bgBrush = Brush.horizontalGradient(
          colors = listOf(
            PastelSage.copy(alpha = 0.11f),
            PastelHoney.copy(alpha = 0.15f),
            PastelSage.copy(alpha = 0.19f)
          )
        )
        val vinePath = Path().apply {
          moveTo(w * 0.06f, h * 0.82f)
          cubicTo(
            w * 0.30f, h * 0.78f,
            w * 0.45f, h * 0.46f,
            w * 0.88f, h * 0.20f
          )
        }
        val stroke = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
        val dotRadius = 3.5.dp.toPx()
        val nodes = arrayOf(
          Triple(w * 0.18f, h * 0.72f, 10.dp.toPx()),
          Triple(w * 0.50f, h * 0.50f, 13.dp.toPx()),
          Triple(w * 0.84f, h * 0.24f, 16.dp.toPx())
        )

        onDrawBehind {
          drawRect(brush = bgBrush)
          drawPath(path = vinePath, color = PastelSage, style = stroke)
          nodes.forEachIndexed { idx, (nx, ny, leafSize) ->
            drawOval(
              color = if (idx == 2) GoldCoin.copy(alpha = 0.88f) else PastelSage.copy(alpha = 0.76f),
              topLeft = Offset(nx - leafSize, ny - leafSize * 0.7f),
              size = Size(leafSize * 2f, leafSize * 1.4f)
            )
            drawCircle(color = Color.White, radius = dotRadius, center = Offset(nx, ny))
          }
        }
      }
      .padding(horizontal = 12.dp, vertical = 14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.Bottom
    ) {
      VineMilestoneTag(
        yearLabel = year1Label,
        amount = milestone1Yr,
        tint = PastelSage,
        modifier = Modifier
          .weight(1f)
          .align(Alignment.Bottom)
      )
      VineMilestoneTag(
        yearLabel = year2Label,
        amount = milestone3Yr,
        tint = PastelSage,
        modifier = Modifier
          .weight(1f)
          .align(Alignment.CenterVertically)
      )
      VineMilestoneTag(
        yearLabel = year3Label,
        amount = milestone5Yr,
        tint = PastelHoney,
        isHighlighted = true,
        modifier = Modifier
          .weight(1f)
          .align(Alignment.Top)
      )
    }
  }
}

@Composable
private fun VineMilestoneTag(
  yearLabel: String,
  amount: String,
  tint: Color,
  isHighlighted: Boolean = false,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isHighlighted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    border = BorderStroke(1.dp, tint.copy(alpha = if (isHighlighted) 0.68f else 0.32f)),
    tonalElevation = if (isHighlighted) 2.dp else 0.dp,
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = yearLabel,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = amount,
        style = GharHandStyles.moneySmall,
        color = if (isHighlighted) PastelTerracotta else tint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun GullakSparkleOverlay(
  size: Dp = 156.dp,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(size)
      .drawWithCache {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.width * 0.46f
        val haloBrush = Brush.radialGradient(
          colors = listOf(
            GoldCoin.copy(alpha = 0.18f),
            PastelHoney.copy(alpha = 0.06f),
            Color.Transparent
          ),
          center = Offset(cx, cy),
          radius = r * 1.05f
        )
        val sparkles = arrayOf(
          Offset(cx - r * 0.78f, cy - r * 0.65f) to 3.4f,
          Offset(cx + r * 0.80f, cy - r * 0.58f) to 4.0f,
          Offset(cx - r * 0.84f, cy + r * 0.45f) to 3.0f,
          Offset(cx + r * 0.82f, cy + r * 0.42f) to 3.4f
        )
        val dotTint = GoldCoin.copy(alpha = 0.80f)
        val rayTint = Color(0xFFFFECC0).copy(alpha = 0.68f)

        onDrawBehind {
          drawCircle(brush = haloBrush, radius = r * 1.05f, center = Offset(cx, cy))
          sparkles.forEach { (pos, radius) ->
            drawCircle(color = dotTint, radius = radius, center = pos)
            drawLine(
              color = rayTint,
              start = Offset(pos.x - radius * 1.9f, pos.y),
              end = Offset(pos.x + radius * 1.9f, pos.y),
              strokeWidth = 1.5f
            )
            drawLine(
              color = rayTint,
              start = Offset(pos.x, pos.y - radius * 1.9f),
              end = Offset(pos.x, pos.y + radius * 1.9f),
              strokeWidth = 1.5f
            )
          }
        }
      }
  )
}

@Composable
fun BalconyCalmIllustration(
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(44.dp)
      .drawWithCache {
        val w = size.width
        val h = size.height
        val glowCenter = Offset(w * 0.5f, h * 0.62f)
        val glowBrush = Brush.radialGradient(
          colors = listOf(PastelHoney.copy(alpha = 0.44f), Color.Transparent),
          center = glowCenter,
          radius = 44f
        )
        val wavePath = Path().apply {
          moveTo(w * 0.1f, h * 0.52f)
          cubicTo(w * 0.32f, h * 0.36f, w * 0.58f, h * 0.68f, w * 0.9f, h * 0.48f)
        }
        val waveStroke = Stroke(width = 2.2f, cap = StrokeCap.Round)
        val waveColor = Color(0xFF4C7A5B).copy(alpha = 0.68f)

        onDrawBehind {
          drawCircle(brush = glowBrush, radius = 44f, center = glowCenter)
          drawPath(path = wavePath, color = waveColor, style = waveStroke)
        }
      }
  )
}

/**
 * Realistic Fridge Sticky-Note Colors — always stays real tactile paper even in Dark/Moonlit mode!
 */
object StickyNotePaperColors {
  val InkDark = Color(0xFF251A12)
  val InkMuted = Color(0xFF65503C)
  val InkAccentRed = Color(0xFFAC3718)
  val InkAccentSage = Color(0xFF285F3C)

  val ButterTop = Color(0xFFFFF8CA)
  val ButterBottom = Color(0xFFFBE692)
  val SagePaperTop = Color(0xFFEEF7E6)
  val SagePaperBottom = Color(0xFFDCECD0)
}

/**
 * Realistic Tilted Fridge Sticky Note — GPU-rotated paper tilt, striped kraft washi tape,
 * specular brass magnet pin, notebook red margin & dog-ear fold drawn via drawWithCache.
 */
@Composable
fun HandwrittenHomeNote(
  isMintPaper: Boolean = false,
  tiltDegrees: Float = -0.8f,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val paperTop = if (isMintPaper) StickyNotePaperColors.SagePaperTop else StickyNotePaperColors.ButterTop
  val paperBottom = if (isMintPaper) StickyNotePaperColors.SagePaperBottom else StickyNotePaperColors.ButterBottom
  val ruleLineColor = if (isMintPaper) Color(0xFF8FB899).copy(alpha = 0.25f) else Color(0xFFD6A84F).copy(alpha = 0.26f)
  val magnetColor = if (isMintPaper) Color(0xFF3E7C56) else Color(0xFFC84B28)

  val noteShape = remember {
    RoundedCornerShape(
      topStart = 5.dp,
      topEnd = 5.dp,
      bottomStart = 7.dp,
      bottomEnd = 24.dp
    )
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 3.dp, vertical = 5.dp)
      .graphicsLayer {
        rotationZ = tiltDegrees
      }
  ) {
    Surface(
      shape = noteShape,
      color = paperTop,
      tonalElevation = 2.dp,
      border = BorderStroke(0.9.dp, Color(0xFFD6B96E).copy(alpha = 0.52f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 10.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .drawWithCache {
            val w = size.width
            val h = size.height
            val paperBrush = Brush.verticalGradient(colors = listOf(paperTop, paperBottom))
            val lineSpacing = 26.dp.toPx()
            val startY = 46.dp.toPx()
            val endMargin = 14.dp.toPx()
            val strokePx = 1.dp.toPx()
            val redLineX = 22.dp.toPx()
            val fold = 22.dp.toPx()
            val foldPath = Path().apply {
              moveTo(w - fold, h)
              lineTo(w, h - fold)
              lineTo(w - fold, h - fold)
              close()
            }
            val redMarginTint = Color(0xFFD96B59).copy(alpha = 0.25f)
            val foldTint = if (isMintPaper) {
              Color(0xFFB7D4BC).copy(alpha = 0.72f)
            } else {
              Color(0xFFD9BF6E).copy(alpha = 0.68f)
            }

            onDrawBehind {
              drawRect(brush = paperBrush)
              var y = startY
              while (y < h - endMargin) {
                drawLine(
                  color = ruleLineColor,
                  start = Offset(endMargin, y),
                  end = Offset(w - endMargin, y),
                  strokeWidth = strokePx
                )
                y += lineSpacing
              }
              drawLine(
                color = redMarginTint,
                start = Offset(redLineX, 0f),
                end = Offset(redLineX, h),
                strokeWidth = strokePx
              )
              drawPath(path = foldPath, color = foldTint)
            }
          }
          .padding(start = 28.dp, end = 18.dp, top = 20.dp, bottom = 18.dp)
      ) {
        content()
      }
    }

    // Translucent striped kraft washi tape + specular brass tack magnet at top center
    Box(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .width(76.dp)
        .height(21.dp)
        .clip(RoundedCornerShape(4.dp))
        .drawWithCache {
          val w = size.width
          val h = size.height
          val tapeBg = if (isMintPaper) {
            Color(0xFFCDE5D2).copy(alpha = 0.86f)
          } else {
            Color(0xFFE9CE84).copy(alpha = 0.86f)
          }
          val stripeTint = Color.White.copy(alpha = 0.34f)
          val center = Offset(w / 2f, h / 2f)
          val pinR = 6.5.dp.toPx()
          val magnetBrush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF8E5), magnetColor),
            center = Offset(center.x - 1.5f, center.y - 1.5f),
            radius = pinR * 1.2f
          )
          val brassRimStroke = Stroke(width = 1.dp.toPx())

          onDrawBehind {
            drawRect(color = tapeBg)
            var sx = -h
            val step = 7.dp.toPx()
            while (sx < w + h) {
              drawLine(
                color = stripeTint,
                start = Offset(sx, h),
                end = Offset(sx + h * 0.7f, 0f),
                strokeWidth = 1.5.dp.toPx()
              )
              sx += step
            }
            drawCircle(brush = magnetBrush, radius = pinR, center = center)
            drawCircle(
              color = GoldCoin.copy(alpha = 0.75f),
              radius = pinR,
              center = center,
              style = brassRimStroke
            )
          }
        }
    )
  }
}
