package com.example.capture.parser

import androidx.compose.runtime.Immutable
import com.example.core.model.CaptureSource
import com.example.core.model.Direction
import java.util.regex.Pattern

@Immutable
data class ParsedCapture(
  val amountPaise: Long,
  val direction: Direction,
  val merchant: String,
  val rawText: String,
  val source: CaptureSource,
  val availableBalancePaise: Long? = null
)

object UpiNotificationParser {

  private val AMOUNT_PATTERN = Pattern.compile("(?:₹|Rs\\.?|INR)\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
  private val MERCHANT_TO_PATTERN = Pattern.compile("(?:paid|sent|transfer(?:red)?|debited)\\s+(?:₹|Rs\\.?|INR)?\\s*[\\d,.]*\\s*(?:to|at)\\s+([A-Za-z0-9\\s&.'-]+?)(?:\\s+via|\\s+using|\\s+successfully|\\s+from|\\.|,|$)", Pattern.CASE_INSENSITIVE)

  fun parse(packageName: String, title: String, text: String): ParsedCapture? {
    val fullText = "$title $text".trim()
    val amountMatcher = AMOUNT_PATTERN.matcher(fullText)
    if (!amountMatcher.find()) return null

    val amountStr = amountMatcher.group(1)?.replace(",", "") ?: return null
    val amountDouble = amountStr.toDoubleOrNull() ?: return null
    val amountPaise = (amountDouble * 100).toLong()

    val isCredit = fullText.contains("received", ignoreCase = true) || fullText.contains("credited", ignoreCase = true)
    val direction = if (isCredit) Direction.CREDIT else Direction.DEBIT

    var merchant = "UPI Payment"
    val merchantMatcher = MERCHANT_TO_PATTERN.matcher(fullText)
    if (merchantMatcher.find()) {
      val found = merchantMatcher.group(1)?.trim()
      if (!found.isNullOrBlank() && found.length < 40) {
        merchant = found
      }
    } else {
      // Fallback heuristics: if title has merchant name
      if (title.isNotBlank() && !title.contains("Payment", ignoreCase = true) && !title.contains("GPay", ignoreCase = true) && !title.contains("PhonePe", ignoreCase = true)) {
        merchant = title.trim()
      }
    }

    return ParsedCapture(
      amountPaise = amountPaise,
      direction = direction,
      merchant = merchant,
      rawText = fullText,
      source = CaptureSource.NOTIFICATION
    )
  }
}
