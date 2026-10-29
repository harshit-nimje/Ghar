package com.example.capture.parser

import com.example.core.model.CaptureSource
import com.example.core.model.Direction
import java.util.regex.Pattern
import kotlin.math.roundToLong

object SmsParser {

  private val REJECT_KEYWORDS = listOf(
    "otp",
    "one time password",
    "one-time password",
    "verification code",
    "security code",
    "do not share",
    "never share",
    "pre-approved",
    "loan up to",
    "apply now",
    "credit limit",
    "minimum amount due",
    "total amount due",
    "statement is ready"
  )

  private val DEBIT_KEYWORDS = listOf(
    "debited",
    "spent",
    "paid",
    "sent",
    "withdrawn",
    "charged",
    "purchase of",
    "dr.",
    "deducted"
  )

  private val CREDIT_KEYWORDS = listOf(
    "credited",
    "received",
    "deposited",
    "refund",
    "cashback"
  )

  private val AMOUNT_PATTERN = Pattern.compile(
    "(?:Rs\\.?|INR|₹|USD|\\$)\\s*([\\d,]+(?:\\.\\d{1,2})?)|([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:Rs\\.?|INR)",
    Pattern.CASE_INSENSITIVE
  )

  private val BALANCE_PATTERN = Pattern.compile(
    "(?:avl\\.?\\s*bal(?:ance)?|available\\s*bal(?:ance)?|avbl\\s*bal|acct\\s*bal|clear\\s*bal|bal\\s*(?:is|:))\\s*(?:is|:|-)?\\s*(?:Rs\\.?|INR|₹|USD|\\$)?\\s*([\\d,]+(?:\\.\\d{1,2})?)",
    Pattern.CASE_INSENSITIVE
  )

  private val DEBIT_MERCHANT_PATTERNS = listOf(
    Pattern.compile("\\b(?:to|at|towards)\\s+(?:vpa\\s+)?([A-Za-z0-9@._*&'\\s/-]+?)(?:\\s+on\\b|\\s+ref\\b|\\s+upi\\b|\\s+txn\\b|\\s+avl\\b|\\s+avbl\\b|\\s+bal\\b|\\s+not\\s+you|\\.|,|;|\\(|\\)|$)", Pattern.CASE_INSENSITIVE),
    Pattern.compile("\\b(?:vpa|info)[:\\s/-]+([A-Za-z0-9@._*&'\\s-]+?)(?:\\s+on\\b|\\s+ref\\b|\\s+avl\\b|\\.|,|;|\\(|\\)|$)", Pattern.CASE_INSENSITIVE)
  )

  private val CREDIT_MERCHANT_PATTERNS = listOf(
    Pattern.compile("\\b(?:from|by)\\s+(?:vpa\\s+)?([A-Za-z0-9@._*&'\\s-]+?)(?:\\s+on\\b|\\s+ref\\b|\\s+upi\\b|\\s+in\\s+your|\\s+avl\\b|\\.|,|;|\\(|\\)|$)", Pattern.CASE_INSENSITIVE),
    Pattern.compile("\\b(?:vpa|info)[:\\s/-]+([A-Za-z0-9@._*&'\\s-]+?)(?:\\s+on\\b|\\s+ref\\b|\\s+avl\\b|\\.|,|;|\\(|\\)|$)", Pattern.CASE_INSENSITIVE)
  )

  fun parse(sender: String, messageBody: String): ParsedCapture? {
    val body = messageBody.trim()
    if (body.length < 12) return null

    val lower = body.lowercase()
    if (REJECT_KEYWORDS.any { lower.contains(it) }) {
      return null
    }

    val hasDebit = DEBIT_KEYWORDS.any { lower.contains(it) }
    val hasCredit = CREDIT_KEYWORDS.any { lower.contains(it) }
    if (!hasDebit && !hasCredit) {
      return null
    }

    // Extract available balance span first so we never mistake "Avl Bal Rs 12,450" for the transaction amount
    var balancePaise: Long? = null
    var balanceMatchStart = -1
    val balMatcher = BALANCE_PATTERN.matcher(body)
    if (balMatcher.find()) {
      balanceMatchStart = balMatcher.start()
      val balStr = balMatcher.group(1)?.replace(",", "")
      val balDouble = balStr?.toDoubleOrNull()
      if (balDouble != null && balDouble >= 0.0) {
        balancePaise = (balDouble * 100.0).roundToLong()
      }
    }

    val amountMatcher = AMOUNT_PATTERN.matcher(body)
    var amountPaise: Long? = null
    while (amountMatcher.find()) {
      if (balanceMatchStart >= 0 && amountMatcher.start() >= balanceMatchStart) {
        continue
      }
      val rawDigits = (amountMatcher.group(1) ?: amountMatcher.group(2))?.replace(",", "")
      val parsed = rawDigits?.toDoubleOrNull()
      if (parsed != null && parsed > 0.0) {
        amountPaise = (parsed * 100.0).roundToLong()
        break
      }
    }
    val finalAmountPaise = amountPaise ?: return null

    // Determine direction carefully ("debited ... credited to merchant" is a DEBIT from user's account!)
    val debitIdx = DEBIT_KEYWORDS.mapNotNull { kw -> lower.indexOf(kw).takeIf { it >= 0 } }.minOrNull() ?: Int.MAX_VALUE
    val creditIdx = CREDIT_KEYWORDS.mapNotNull { kw -> lower.indexOf(kw).takeIf { it >= 0 } }.minOrNull() ?: Int.MAX_VALUE
    val direction = if (hasCredit && (!hasDebit || creditIdx < debitIdx)) {
      Direction.CREDIT
    } else {
      Direction.DEBIT
    }

    val orderedPatterns = if (direction == Direction.CREDIT) {
      CREDIT_MERCHANT_PATTERNS + DEBIT_MERCHANT_PATTERNS
    } else {
      DEBIT_MERCHANT_PATTERNS + CREDIT_MERCHANT_PATTERNS
    }

    var merchant = if (direction == Direction.CREDIT) "Bank Credit" else "Bank / UPI Spend"
    for (pattern in orderedPatterns) {
      val m = pattern.matcher(body)
      while (m.find()) {
        val candidate = cleanMerchantLabel(m.group(1).orEmpty())
        if (candidate.isNotBlank() && candidate.length in 2..44) {
          merchant = candidate
          break
        }
      }
      if (merchant != "Bank / UPI Spend" && merchant != "Bank Credit") break
    }

    if ((merchant == "Bank / UPI Spend" || merchant == "Bank Credit") && sender.isNotBlank()) {
      val cleanSender = sender.replace(Regex("^[A-Za-z]{2}-"), "").trim()
      if (cleanSender.length in 3..16 && cleanSender.any { it.isLetter() }) {
        merchant = "$cleanSender Alert"
      }
    }

    return ParsedCapture(
      amountPaise = finalAmountPaise,
      direction = direction,
      merchant = merchant,
      rawText = body,
      source = CaptureSource.SMS,
      availableBalancePaise = balancePaise
    )
  }

  private fun cleanMerchantLabel(raw: String): String {
    var s = raw.trim()
      .removePrefix("VPA ")
      .removePrefix("vpa ")
      .removePrefix("UPI/")
      .removePrefix("POS ")
    if (s.contains("@")) {
      s = s.substringBefore("@")
    }
    s = s.replace(Regex("[._-]+"), " ")
      .replace(Regex("\\b(a/c|ac|acct|xx\\d+|xxxx\\d+)\\b", RegexOption.IGNORE_CASE), "")
      .replace(Regex("\\s+"), " ")
      .trim()
    if (s.equals("your account", ignoreCase = true) || s.equals("account", ignoreCase = true)) {
      return ""
    }
    return s.split(" ").joinToString(" ") { word ->
      if (word.length <= 3 && word.all { it.isUpperCase() }) word
      else word.lowercase().replaceFirstChar { ch -> ch.titlecase() }
    }
  }
}

