package com.example.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.util.CurrencyFormatter
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.contrastingContentColor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Pure, deterministic arithmetic engine for the Ghar Household Calculator.
 * Supports +, -, *, /, % with standard operator precedence (*, / before +, -) and decimals.
 */
object HisaabCalculatorEngine {

  private val OPERATORS = setOf('+', '-', '×', '÷', '*', '/')

  fun appendKey(currentExpr: String, key: String): String {
    return when (key) {
      "C" -> ""
      "⌫" -> if (currentExpr.isNotEmpty()) currentExpr.dropLast(1) else ""
      "=" -> {
        val evaluated = evaluate(currentExpr)
        if (evaluated != null) formatCleanNumber(evaluated) else currentExpr
      }
      "%" -> {
        if (currentExpr.isEmpty() || currentExpr.last() in OPERATORS || currentExpr.last() == '%') {
          currentExpr
        } else {
          "$currentExpr%"
        }
      }
      "+", "−", "-", "×", "÷" -> {
        val normOp = if (key == "−") "-" else key
        if (currentExpr.isEmpty()) {
          if (normOp == "-") "-" else ""
        } else if (currentExpr.last() in OPERATORS) {
          currentExpr.dropLast(1) + normOp
        } else {
          currentExpr + normOp
        }
      }
      "." -> {
        val lastNumberToken = currentExpr.takeLastWhile { it.isDigit() || it == '.' }
        if (lastNumberToken.contains('.')) currentExpr
        else if (lastNumberToken.isEmpty()) "${currentExpr}0."
        else "$currentExpr."
      }
      else -> {
        if (currentExpr.length >= 24) currentExpr else currentExpr + key
      }
    }
  }

  fun evaluate(expression: String): Double? {
    val cleaned = expression
      .replace("₹", "")
      .replace(",", "")
      .replace("−", "-")
      .replace("×", "*")
      .replace("÷", "/")
      .replace(" ", "")
      .trimEnd('+', '-', '*', '/')

    if (cleaned.isEmpty() || cleaned == "-") return null

    // Tokenize numbers (with optional trailing %) and binary operators
    val values = mutableListOf<Double>()
    val ops = mutableListOf<Char>()
    var i = 0
    while (i < cleaned.length) {
      val ch = cleaned[i]
      val isUnaryMinus = ch == '-' && (i == 0 || cleaned[i - 1] in setOf('+', '-', '*', '/'))
      if (ch.isDigit() || ch == '.' || isUnaryMinus) {
        val start = i
        if (isUnaryMinus) i++
        while (i < cleaned.length && (cleaned[i].isDigit() || cleaned[i] == '.')) {
          i++
        }
        val numStr = cleaned.substring(start, i)
        var num = numStr.toDoubleOrNull() ?: return null
        if (i < cleaned.length && cleaned[i] == '%') {
          // If preceded by + or - (e.g. 500 + 18% GST or 1000 - 20% discount), compute percentage of running base!
          val prevOp = ops.lastOrNull()
          val base = values.lastOrNull()
          num = if ((prevOp == '+' || prevOp == '-') && base != null) {
            base * (num / 100.0)
          } else {
            num / 100.0
          }
          i++
        }
        values.add(num)
      } else if (ch in setOf('+', '-', '*', '/')) {
        ops.add(ch)
        i++
      } else {
        return null
      }
    }

    if (values.isEmpty() || values.size != ops.size + 1) return null

    // Pass 1: * and /
    val pass1Values = mutableListOf(values[0])
    val pass1Ops = mutableListOf<Char>()
    for (idx in ops.indices) {
      val op = ops[idx]
      val rhs = values[idx + 1]
      when (op) {
        '*' -> pass1Values[pass1Values.lastIndex] = pass1Values.last() * rhs
        '/' -> {
          if (abs(rhs) < 1e-12) return null
          pass1Values[pass1Values.lastIndex] = pass1Values.last() / rhs
        }
        else -> {
          pass1Ops.add(op)
          pass1Values.add(rhs)
        }
      }
    }

    // Pass 2: + and -
    var acc = pass1Values[0]
    for (idx in pass1Ops.indices) {
      val rhs = pass1Values[idx + 1]
      acc = if (pass1Ops[idx] == '+') acc + rhs else acc - rhs
    }
    return if (acc.isFinite()) acc else null
  }

  fun evaluateRoundedRupees(expression: String): Long {
    val v = evaluate(expression) ?: return 0L
    return v.roundToLong().coerceAtLeast(0L)
  }

  fun formatCleanNumber(value: Double): String {
    val asLong = value.roundToLong()
    return if (abs(value - asLong.toDouble()) < 0.005) {
      asLong.toString()
    } else {
      String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
  }
}

@Composable
fun HisaabCalculatorDialog(
  initialAmountRupees: Long? = null,
  onDismiss: () -> Unit,
  onUseAmount: ((Long) -> Unit)? = null,
  onJotExpense: ((Long) -> Unit)? = null,
  onDropInGullak: ((Long) -> Unit)? = null
) {
  val plainEnglish = LocalPlainEnglishMode.current
  var expression by remember {
    mutableStateOf(
      if (initialAmountRupees != null && initialAmountRupees > 0L) initialAmountRupees.toString() else ""
    )
  }

  val liveDouble = remember(expression) { HisaabCalculatorEngine.evaluate(expression) }
  val liveRupees = remember(liveDouble) { (liveDouble?.roundToLong() ?: 0L).coerceAtLeast(0L) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = JharokhaCardShape,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      border = BorderStroke(1.2.dp, PastelHoney.copy(alpha = 0.42f)),
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .widthIn(max = 460.dp)
        .testTag("hisaab_calculator_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(PastelHoney.copy(alpha = 0.22f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = null,
                tint = PastelHoney,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (plainEnglish) "Budget Calculator" else stringResource(R.string.hisaab_calculator_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = if (plainEnglish) {
                  "Quick math — log as an expense or add to Savings"
                } else {
                  stringResource(R.string.hisaab_calculator_subtitle)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(48.dp)
              .testTag("close_calculator_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close calculator",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tactile Ledger Display Tape
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = StickyNotePaperColors.ButterTop.copy(alpha = 0.72f),
          border = BorderStroke(1.dp, PastelHoney.copy(alpha = 0.45f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("calculator_display_card")
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.End
          ) {
            Text(
              text = expression.ifEmpty { "0" },
              style = GharHandStyles.stickyNoteBody.copy(fontSize = 18.sp),
              color = StickyNotePaperColors.InkMuted,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.End,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("calculator_expression_text")
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (liveDouble != null) {
                "₹${HisaabCalculatorEngine.formatCleanNumber(liveDouble)}"
              } else {
                "₹0"
              },
              style = GharHandStyles.moneyLarge.copy(fontSize = 32.sp),
              color = StickyNotePaperColors.InkDark,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("calculator_result_text")
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Keypad Rows (4x5 tactile grid, minimum 48dp height per key)
        val rows = listOf(
          listOf("C", "%", "⌫", "÷"),
          listOf("7", "8", "9", "×"),
          listOf("4", "5", "6", "−"),
          listOf("1", "2", "3", "+"),
          listOf("00", "0", ".", "=")
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          rows.forEach { row ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              row.forEach { key ->
                val isOperator = key in setOf("÷", "×", "−", "+", "=")
                val isUtility = key in setOf("C", "%", "⌫")
                val bgColor = when {
                  key == "=" -> MaterialTheme.colorScheme.primary
                  isOperator -> PastelHoney.copy(alpha = 0.24f)
                  isUtility -> PastelTerracotta.copy(alpha = 0.14f)
                  else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                }
                val contentColor = when {
                  key == "=" -> MaterialTheme.colorScheme.onPrimary
                  isOperator -> MaterialTheme.colorScheme.primary
                  isUtility -> MaterialTheme.colorScheme.primary
                  else -> MaterialTheme.colorScheme.onSurface
                }

                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = bgColor,
                  modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                      expression = HisaabCalculatorEngine.appendKey(expression, key)
                    }
                    .testTag("calc_key_$key")
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    if (key == "⌫") {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = contentColor,
                        modifier = Modifier.size(19.dp)
                      )
                    } else {
                      Text(
                        text = key,
                        style = GharHandStyles.moneyMedium.copy(fontSize = 20.sp),
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Direct Household Actions on the Calculated Amount
        val hasPositiveAmount = liveRupees > 0L
        if (onUseAmount != null) {
          Button(
            onClick = {
              if (hasPositiveAmount) onUseAmount(liveRupees)
            },
            enabled = hasPositiveAmount,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .testTag("calc_use_amount_button")
          ) {
            Text(
              text = if (hasPositiveAmount) {
                "Use ${CurrencyFormatter.formatRupees(liveRupees)}"
              } else {
                "Enter an amount above"
              },
              color = MaterialTheme.colorScheme.onPrimary,
              fontWeight = FontWeight.Bold
            )
          }
        } else if (onJotExpense != null || onDropInGullak != null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (onJotExpense != null) {
              Button(
                onClick = { if (hasPositiveAmount) onJotExpense(liveRupees) },
                enabled = hasPositiveAmount,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                  .weight(1f)
                  .defaultMinSize(minHeight = 48.dp)
                  .testTag("calc_jot_expense_button")
              ) {
                Text(
                  text = if (hasPositiveAmount) {
                    val verb = if (plainEnglish) "Log" else "Jot"
                    "$verb ${CurrencyFormatter.formatCompactRupees(liveRupees)} Spend"
                  } else {
                    if (plainEnglish) "Log Spend" else "Jot Spend"
                  },
                  color = MaterialTheme.colorScheme.onPrimary,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            if (onDropInGullak != null) {
              val sageContent = PastelSage.contrastingContentColor()
              Button(
                onClick = { if (hasPositiveAmount) onDropInGullak(liveRupees) },
                enabled = hasPositiveAmount,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                  containerColor = PastelSage,
                  contentColor = sageContent
                ),
                modifier = Modifier
                  .weight(1f)
                  .defaultMinSize(minHeight = 48.dp)
                  .testTag("calc_drop_gullak_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Savings,
                  contentDescription = null,
                  tint = sageContent,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (hasPositiveAmount) {
                    val potWord = if (plainEnglish) "Save" else "Gullak"
                    "$potWord +${CurrencyFormatter.formatCompactRupees(liveRupees)}"
                  } else {
                    if (plainEnglish) "To Savings" else "To Gullak"
                  },
                  color = sageContent,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    }
  }
}
