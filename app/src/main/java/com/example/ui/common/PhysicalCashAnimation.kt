package com.example.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.core.audio.GharSoundEffects
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.PainDial
import com.example.core.util.CurrencyFormatter
import com.example.ui.theme.GharHandStyles
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Authentic Indian cotton-rag paper reserve-note color palettes (stylized, non-counterfeit),
 * covering every real Indian currency denomination from ₹500 down to ₹1 so flying cash notes
 * always sum to the exact Rupee amount spent or received.
 */
enum class RupeeDenominationPalette(
  val valueRupees: Long,
  val label: String,
  val paperLight: Color,
  val paperMid: Color,
  val paperEdge: Color,
  val inkPrimary: Color,
  val threadColor: Color,
  val serialPrefix: String
) {
  NOTE_500(
    valueRupees = 500L,
    label = "₹500",
    paperLight = Color(0xFFF1F3EC),
    paperMid = Color(0xFFD7DDD0),
    paperEdge = Color(0xFFBCC5B4),
    inkPrimary = Color(0xFF394637),
    threadColor = Color(0xFF1F7A68),
    serialPrefix = "5KM"
  ),
  NOTE_200(
    valueRupees = 200L,
    label = "₹200",
    paperLight = Color(0xFFFFF4DE),
    paperMid = Color(0xFFFAD79E),
    paperEdge = Color(0xFFEABA72),
    inkPrimary = Color(0xFF7D3E0E),
    threadColor = Color(0xFF23746B),
    serialPrefix = "2GH"
  ),
  NOTE_100(
    valueRupees = 100L,
    label = "₹100",
    paperLight = Color(0xFFF5F0FC),
    paperMid = Color(0xFFE0D4F5),
    paperEdge = Color(0xFFC8B7E6),
    inkPrimary = Color(0xFF47316B),
    threadColor = Color(0xFF237A6B),
    serialPrefix = "7LV"
  ),
  NOTE_50(
    valueRupees = 50L,
    label = "₹50",
    paperLight = Color(0xFFEAF9F9),
    paperMid = Color(0xFFC2E9EA),
    paperEdge = Color(0xFF9DD5D7),
    inkPrimary = Color(0xFF16565C),
    threadColor = Color(0xFF8C6422),
    serialPrefix = "4HP"
  ),
  NOTE_20(
    valueRupees = 20L,
    label = "₹20",
    paperLight = Color(0xFFFAF6DF),
    paperMid = Color(0xFFEBE0AE),
    paperEdge = Color(0xFFD6C78B),
    inkPrimary = Color(0xFF5D5318),
    threadColor = Color(0xFF28735C),
    serialPrefix = "9EL"
  ),
  NOTE_10(
    valueRupees = 10L,
    label = "₹10",
    paperLight = Color(0xFFF4E7DA),
    paperMid = Color(0xFFDEC0A5),
    paperEdge = Color(0xFFC59E7D),
    inkPrimary = Color(0xFF5D3418),
    threadColor = Color(0xFF296E5B),
    serialPrefix = "1KN"
  ),
  NOTE_5(
    valueRupees = 5L,
    label = "₹5",
    paperLight = Color(0xFFE6F3E8),
    paperMid = Color(0xFFBEDFC4),
    paperEdge = Color(0xFF98C6A2),
    inkPrimary = Color(0xFF1F5430),
    threadColor = Color(0xFF8C6924),
    serialPrefix = "5TR"
  ),
  NOTE_2(
    valueRupees = 2L,
    label = "₹2",
    paperLight = Color(0xFFFBE9E4),
    paperMid = Color(0xFFF0C2B6),
    paperEdge = Color(0xFFDCA192),
    inkPrimary = Color(0xFF752D1E),
    threadColor = Color(0xFF2B6B73),
    serialPrefix = "2AR"
  ),
  NOTE_1(
    valueRupees = 1L,
    label = "₹1",
    paperLight = Color(0xFFE5EFF3),
    paperMid = Color(0xFFBCD4DF),
    paperEdge = Color(0xFF97BAC9),
    inkPrimary = Color(0xFF214858),
    threadColor = Color(0xFF7D5C25),
    serialPrefix = "1GI"
  );

  companion object {
    fun forValue(rupees: Long): RupeeDenominationPalette =
      entries.firstOrNull { it.valueRupees == rupees } ?: NOTE_1
  }
}

/**
 * Represents a single visual currency bill (or strapped bundle when a spend involves many notes,
 * e.g., ₹18,000 monthly rent) whose [totalRupees] contributes to the exact transaction amount.
 */
@Immutable
data class DecomposedCashBill(
  val palette: RupeeDenominationPalette,
  val bundleCount: Int = 1,
  val extraChangeRupees: Long = 0L
) {
  val totalRupees: Long
    get() = palette.valueRupees * bundleCount + extraChangeRupees

  val strapBadgeText: String?
    get() = when {
      bundleCount > 1 && extraChangeRupees > 0L -> "×$bundleCount +₹$extraChangeRupees"
      bundleCount > 1 -> "×$bundleCount bundle"
      extraChangeRupees > 0L -> "+₹$extraChangeRupees"
      else -> null
    }

  val compactBreakdownToken: String
    get() = when {
      bundleCount > 1 && extraChangeRupees > 0L -> "${palette.label}×$bundleCount + ₹$extraChangeRupees"
      bundleCount > 1 -> "${palette.label}×$bundleCount"
      extraChangeRupees > 0L -> "${palette.label}+₹$extraChangeRupees"
      else -> palette.label
    }
}

@Immutable
data class CashFlyingNote(
  val id: Int,
  val bill: DecomposedCashBill,
  val serialSuffix: String,
  val startAngle: Float,
  val endAngle: Float,
  val spreadXFraction: Float,
  val peakHeightFraction: Float,
  val delayMs: Int
)

/**
 * Decomposes [amountRupees] into 1..6 realistic Indian Rupee bills whose [DecomposedCashBill.totalRupees]
 * ALWAYS sums to the exact Rupee amount (`amountRupees.coerceAtLeast(1L)`).
 *
 * - **SUBTLE**: Minimal exact RBI notes (e.g., ₹20 -> `[₹20]`, ₹30 -> `[₹20, ₹10]`).
 * - **BALANCED**: Splits larger notes into exact smaller denominations for tactile weight
 *   (e.g., ₹20 -> `[₹10, ₹5, ₹5]` = ₹20; ₹50 -> `[₹20, ₹20, ₹10]` = ₹50).
 * - **PUNCHY**: Splits one step further into crisp smaller bills while staying 100% mathematically exact
 *   (e.g., ₹20 -> `[₹5, ₹5, ₹5, ₹5]` = ₹20; ₹50 -> `[₹20, ₹10, ₹10, ₹10]` = ₹50).
 * - **Large amounts (e.g., ₹18,000)**: Unrolls individual ₹500 bills alongside a strapped lead bundle
 *   (`[₹500 ×33, ₹500, ₹500, ₹500]`) so at most 6 notes animate smoothly while totaling exact ₹18,000.
 */
internal fun decomposeExactRupeeBills(
  amountRupees: Long,
  painDial: PainDial
): List<DecomposedCashBill> {
  val target = amountRupees.coerceAtLeast(1L)
  val desiredCount = when (painDial) {
    PainDial.SUBTLE -> 1
    PainDial.BALANCED -> 3
    PainDial.PUNCHY -> 4
  }
  val maxAnimatedBills = when (painDial) {
    PainDial.SUBTLE -> 3
    PainDial.BALANCED -> 5
    PainDial.PUNCHY -> 6
  }

  // Step 1: Exact greedy decomposition using real Indian denominations (descending)
  val greedySingleValues = mutableListOf<Long>()
  var rem = target
  for (denom in RupeeDenominationPalette.entries) {
    if (rem <= 0L) break
    val count = rem / denom.valueRupees
    if (count > 0L) {
      // Avoid allocating huge lists for ₹10,00,000; cap raw expansion only if count <= 12
      if (count <= 12L) {
        repeat(count.toInt()) { greedySingleValues.add(denom.valueRupees) }
        rem -= count * denom.valueRupees
      } else {
        // Large spend path handled cleanly via grouped denomination bundles below
        return buildLargeBundledStack(target, desiredCount.coerceAtLeast(3), maxAnimatedBills)
      }
    }
  }

  // Step 2: If greedy count already exceeds maxAnimatedBills, bundle cleanly while preserving exact total
  if (greedySingleValues.size > maxAnimatedBills) {
    return buildLargeBundledStack(target, desiredCount.coerceAtLeast(3), maxAnimatedBills)
  }

  // Step 3: Exact denomination splitting for tactile multi-bill motion without inflating the total!
  val working = greedySingleValues.toMutableList()
  while (working.size < desiredCount) {
    val splitIndex = working.indexOfFirst { value ->
      val parts = exactSplitParts(value)
      parts != null && (working.size - 1 + parts.size) <= maxAnimatedBills
    }
    if (splitIndex == -1) break
    val original = working.removeAt(splitIndex)
    val parts = exactSplitParts(original) ?: break
    working.addAll(splitIndex, parts)
    working.sortDescending()
  }

  return working.map { value ->
    DecomposedCashBill(palette = RupeeDenominationPalette.forValue(value))
  }
}

private fun exactSplitParts(valueRupees: Long): List<Long>? = when (valueRupees) {
  500L -> listOf(200L, 200L, 100L)
  200L -> listOf(100L, 100L)
  100L -> listOf(50L, 50L)
  50L -> listOf(20L, 20L, 10L)
  20L -> listOf(10L, 10L)
  10L -> listOf(5L, 5L)
  5L -> listOf(2L, 2L, 1L)
  2L -> listOf(1L, 1L)
  else -> null
}

private fun buildLargeBundledStack(
  targetRupees: Long,
  desiredCount: Int,
  maxAnimatedBills: Int
): List<DecomposedCashBill> {
  // Group exact counts by denomination
  val groups = mutableListOf<DecomposedCashBill>()
  var rem = targetRupees
  for (denom in RupeeDenominationPalette.entries) {
    if (rem <= 0L) break
    val cnt = (rem / denom.valueRupees).toInt()
    if (cnt > 0) {
      groups.add(DecomposedCashBill(palette = denom, bundleCount = cnt))
      rem -= cnt.toLong() * denom.valueRupees
    }
  }
  if (rem > 0L) {
    if (groups.isNotEmpty()) {
      val last = groups.removeAt(groups.lastIndex)
      groups.add(last.copy(extraChangeRupees = last.extraChangeRupees + rem))
    } else {
      groups.add(DecomposedCashBill(palette = RupeeDenominationPalette.NOTE_1, bundleCount = rem.toInt()))
    }
  }

  // If distinct denomination groups exceed maxAnimatedBills (e.g. ₹1,888 has 7+ distinct notes),
  // fold the tiny tail change onto the last visible bill so animated count stays <= maxAnimatedBills
  // while keeping totalRupees 100% exact.
  while (groups.size > maxAnimatedBills) {
    val tail = groups.removeAt(groups.lastIndex)
    val prev = groups.removeAt(groups.lastIndex)
    groups.add(prev.copy(extraChangeRupees = prev.extraChangeRupees + tail.totalRupees))
  }

  // Unroll individual notes from bundled stacks (e.g. ₹18,000 = [₹500×36] -> [₹500×33, ₹500, ₹500, ₹500])
  // so large spends still fan out across multiple physical notes!
  val targetFan = desiredCount.coerceAtMost(maxAnimatedBills)
  while (groups.size < targetFan) {
    val idx = groups.indexOfFirst { it.bundleCount > 1 && it.extraChangeRupees == 0L }
    if (idx == -1) break
    val bundled = groups[idx]
    groups[idx] = bundled.copy(bundleCount = bundled.bundleCount - 1)
    groups.add(idx + 1, DecomposedCashBill(palette = bundled.palette, bundleCount = 1))
  }

  return groups
}

internal fun formatCompactBillBreakdown(bills: List<DecomposedCashBill>): String {
  if (bills.isEmpty()) return ""
  // Collapse identical single notes for crisp readability, e.g. [₹10, ₹5, ₹5] -> "₹10 + 2×₹5"
  val parts = mutableListOf<String>()
  var i = 0
  while (i < bills.size) {
    val current = bills[i]
    if (current.bundleCount == 1 && current.extraChangeRupees == 0L) {
      var runCount = 1
      while (
        i + runCount < bills.size &&
        bills[i + runCount].palette == current.palette &&
        bills[i + runCount].bundleCount == 1 &&
        bills[i + runCount].extraChangeRupees == 0L
      ) {
        runCount++
      }
      parts.add(if (runCount > 1) "$runCount × ${current.palette.label}" else current.palette.label)
      i += runCount
    } else {
      parts.add(current.compactBreakdownToken)
      i++
    }
  }
  return parts.joinToString(" + ")
}

/**
 * Artisanal Cotton-Rag Indian Paper Note Composable.
 * Features guilloché lathe border, watermark window with lotus rosette,
 * woven security thread, micro-serial number, optional bank-strap bundle badge, and hand-inked denomination.
 */
@Composable
fun ArtisanalRupeeNote(
  palette: RupeeDenominationPalette,
  serialNumber: String = "482910",
  strapBadgeText: String? = null,
  width: Dp = 208.dp,
  height: Dp = 98.dp,
  modifier: Modifier = Modifier
) {
  val noteShape = RoundedCornerShape(8.dp)

  Box(
    modifier = modifier
      .size(width = width, height = height)
      .shadow(12.dp, noteShape)
      .background(
        brush = Brush.linearGradient(
          colors = listOf(
            palette.paperLight,
            palette.paperMid,
            palette.paperLight,
            palette.paperEdge
          )
        ),
        shape = noteShape
      )
  ) {
    // Guilloché lathe border, center fold crease, metallic security thread & lotus watermark
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      val m1 = 5.dp.toPx()
      val m2 = 8.5.dp.toPx()
      drawRoundRect(
        color = palette.inkPrimary.copy(alpha = 0.45f),
        topLeft = Offset(m1, m1),
        size = Size(w - m1 * 2, h - m1 * 2),
        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
        style = Stroke(width = 1.3.dp.toPx())
      )
      drawRoundRect(
        color = palette.inkPrimary.copy(alpha = 0.22f),
        topLeft = Offset(m2, m2),
        size = Size(w - m2 * 2, h - m2 * 2),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        style = Stroke(
          width = 0.9.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f), 0f)
        )
      )

      // Subtle vertical center wallet-fold shadow
      drawRect(
        brush = Brush.horizontalGradient(
          colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.22f),
            Color.Transparent
          ),
          startX = w * 0.47f,
          endX = w * 0.53f
        ),
        topLeft = Offset(w * 0.47f, 0f),
        size = Size(w * 0.06f, h)
      )

      // Woven metallic security thread strip
      val threadX = w * 0.58f
      drawLine(
        color = palette.threadColor.copy(alpha = 0.72f),
        start = Offset(threadX, m1),
        end = Offset(threadX, h - m1),
        strokeWidth = 2.8.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 7f), 2f)
      )

      // Translucent cotton-rag watermark medallion window on right
      val wmCenter = Offset(w * 0.79f, h * 0.52f)
      val wmRadius = h * 0.28f
      drawCircle(
        color = Color.White.copy(alpha = 0.68f),
        radius = wmRadius,
        center = wmCenter
      )
      drawCircle(
        color = palette.inkPrimary.copy(alpha = 0.24f),
        radius = wmRadius,
        center = wmCenter,
        style = Stroke(width = 1.dp.toPx())
      )
      for (i in 0 until 8) {
        val angle = (i * Math.PI / 4.0)
        val px = wmCenter.x + (wmRadius * 0.52f * cos(angle)).toFloat()
        val py = wmCenter.y + (wmRadius * 0.52f * sin(angle)).toFloat()
        drawCircle(
          color = palette.inkPrimary.copy(alpha = 0.16f),
          radius = wmRadius * 0.28f,
          center = Offset(px, py),
          style = Stroke(width = 0.9.dp.toPx())
        )
      }
    }

    // Crisp typography & bold scribbled denomination on the note
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 13.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "GHAR KHATA RESERVE",
          fontSize = 8.sp,
          letterSpacing = 1.2.sp,
          fontWeight = FontWeight.Bold,
          color = palette.inkPrimary.copy(alpha = 0.78f)
        )
        Text(
          text = "${palette.valueRupees}",
          style = GharHandStyles.moneySmall,
          fontSize = 14.sp,
          color = palette.inkPrimary
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = palette.label,
          style = GharHandStyles.moneyLarge,
          fontSize = 27.sp,
          color = palette.inkPrimary
        )
        if (strapBadgeText != null) {
          Surface(
            shape = CircleShape,
            color = palette.inkPrimary.copy(alpha = 0.90f)
          ) {
            Text(
              text = strapBadgeText,
              style = MaterialTheme.typography.labelSmall,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = palette.paperLight,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        Text(
          text = "${palette.serialPrefix} $serialNumber",
          fontSize = 8.5.sp,
          letterSpacing = 1.sp,
          fontWeight = FontWeight.SemiBold,
          color = palette.inkPrimary.copy(alpha = 0.72f)
        )
        Text(
          text = "मैं धारक को वचन देता हूँ",
          fontSize = 7.sp,
          color = palette.inkPrimary.copy(alpha = 0.55f)
        )
      }
    }
  }
}

/**
 * Full-Screen Tactile Cash Overlay for BOTH Spending (outgoing from Leather Batua)
 * and Receiving/Tucking In (incoming cascade into Leather Batua).
 * Every flying note stack is decomposed so the face values sum to the exact Rupee amount.
 */
@Composable
fun PhysicalCashAnimationOverlay(
  amountPaise: Long,
  painDial: PainDial = PainDial.BALANCED,
  hapticsEnabled: Boolean = true,
  soundEnabled: Boolean = true,
  isIncoming: Boolean = false,
  onAnimationFinished: () -> Unit
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val amountRupees = (amountPaise / 100L).coerceAtLeast(1L)

  val decomposedBills = remember(amountRupees, painDial) {
    decomposeExactRupeeBills(amountRupees, painDial)
  }
  val noteCount = decomposedBills.size

  val notes = remember(amountPaise, decomposedBills, isIncoming) {
    val rng = Random(amountPaise)
    decomposedBills.mapIndexed { i, bill ->
      val lane = if (noteCount <= 1) 0f else ((i.toFloat() / (noteCount - 1)) - 0.5f) * 2f
      CashFlyingNote(
        id = i,
        bill = bill,
        serialSuffix = (104820 + rng.nextInt(890000)).toString(),
        startAngle = lane * 14f + (rng.nextFloat() * 8f - 4f),
        endAngle = lane * 26f + (rng.nextFloat() * 16f - 8f),
        spreadXFraction = lane * 0.34f + (rng.nextFloat() * 0.08f - 0.04f),
        peakHeightFraction = 0.14f + (i % 3) * 0.09f + rng.nextFloat() * 0.06f,
        delayMs = i * 65
      )
    }
  }

  val scrimAlpha = remember { Animatable(0f) }
  val walletBounce = remember { Animatable(0.75f) }

  LaunchedEffect(Unit) {
    GharSoundEffects.playCrispNoteRustle(context, soundEnabled)
    if (hapticsEnabled) {
      triggerCashHaptic(context, painDial)
    }
    launch {
      scrimAlpha.animateTo(1f, tween(180))
    }
    launch {
      walletBounce.animateTo(
        targetValue = 1.0f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMediumLow
        )
      )
    }
    val totalDuration = (noteCount * 65 + 980).toLong()
    delay(totalDuration - 240)
    scrimAlpha.animateTo(0f, tween(220))
    onAnimationFinished()
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF1A120B).copy(alpha = 0.30f * scrimAlpha.value))
      .testTag("physical_cash_overlay")
  ) {
    val wPx = with(density) { maxWidth.toPx() }
    val hPx = with(density) { maxHeight.toPx() }
    val noteWidthDp = 212.dp
    val noteHeightDp = 100.dp
    val noteHalfW = with(density) { (noteWidthDp / 2).toPx() }
    val noteHalfH = with(density) { (noteHeightDp / 2).toPx() }

    if (painDial != PainDial.SUBTLE) {
      notes.forEach { note ->
        val progress = remember { Animatable(0f) }
        val noteAlpha = remember { Animatable(0f) }

        LaunchedEffect(note.id) {
          delay(note.delayMs.toLong())
          launch {
            noteAlpha.animateTo(1f, tween(110))
          }
          launch {
            progress.animateTo(
              targetValue = 1f,
              animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessLow
              )
            )
          }
          delay(540)
          noteAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
          )
        }

        val p = progress.value
        val walletCenterY = hPx * 0.80f
        val upperStageY = hPx * note.peakHeightFraction

        val currentY = if (!isIncoming) {
          walletCenterY + (upperStageY - walletCenterY) * p
        } else {
          upperStageY + (walletCenterY - upperStageY) * p
        }

        val spreadEnvelope = if (!isIncoming) p else (1f - p * 0.72f)
        val currentX = (wPx / 2f) + (wPx * note.spreadXFraction * spreadEnvelope)
        val currentRot = note.startAngle + (note.endAngle - note.startAngle) * p
        val scaleFactor = if (!isIncoming) {
          0.68f + 0.38f * sin(p * Math.PI.toFloat() * 0.75f).coerceAtLeast(0.2f)
        } else {
          1.06f - 0.34f * p
        }

        ArtisanalRupeeNote(
          palette = note.bill.palette,
          serialNumber = note.serialSuffix,
          strapBadgeText = note.bill.strapBadgeText,
          width = noteWidthDp,
          height = noteHeightDp,
          modifier = Modifier.graphicsLayer {
            translationX = currentX - noteHalfW
            translationY = currentY - noteHalfH
            rotationZ = currentRot
            rotationX = (1f - p) * 18f
            scaleX = scaleFactor
            scaleY = scaleFactor
            alpha = noteAlpha.value
          }
        )
      }
    }

    // Bottom Stage: Tactile Stitched Leather Batua + Exact Denomination Tag
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 36.dp)
        .graphicsLayer {
          scaleX = walletBounce.value
          scaleY = walletBounce.value
          alpha = scrimAlpha.value
        },
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFF6C7),
        shadowElevation = 8.dp,
        border = BorderStroke(1.2.dp, if (isIncoming) Color(0xFF2D6A4F) else Color(0xFFA93818))
      ) {
        Text(
          text = if (isIncoming) {
            "+ ${CurrencyFormatter.formatRupees(amountRupees)}"
          } else {
            "- ${CurrencyFormatter.formatRupees(amountRupees)}"
          },
          style = GharHandStyles.moneyLarge,
          color = if (isIncoming) Color(0xFF1F5E3B) else Color(0xFFA93818),
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      WalletGraphic(
        thicknessRatio = if (isIncoming) 0.95f else 0.55f,
        size = 104.dp
      )
    }
  }
}

private fun triggerCashHaptic(context: Context, painDial: PainDial) {
  try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      val vibrator = vibratorManager?.defaultVibrator
      val duration = when (painDial) {
        PainDial.SUBTLE -> 25L
        PainDial.BALANCED -> 50L
        PainDial.PUNCHY -> 85L
      }
      val amplitude = when (painDial) {
        PainDial.SUBTLE -> 65
        PainDial.BALANCED -> 150
        PainDial.PUNCHY -> 255
      }
      vibrator?.vibrate(VibrationEffect.createOneShot(duration, amplitude))
    } else {
      @Suppress("DEPRECATION")
      val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(55)
      }
    }
  } catch (_: Exception) {
    // Graceful no-op if device has no vibrator
  }
}
