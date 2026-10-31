package com.example.capture

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Immutable
import com.example.capture.parser.MerchantLookupTable
import com.example.capture.parser.SmsParser
import com.example.core.model.CaptureSource
import com.example.core.model.Direction
import com.example.core.model.Transaction
import java.util.UUID
import kotlin.math.abs

@Immutable
data class ParsedSmsCandidate(
  val id: String,
  val sender: String,
  val transaction: Transaction,
  val availableBalancePaise: Long?
)

@Immutable
data class SmsScanSummary(
  val scannedMessagesCount: Int,
  val candidates: List<ParsedSmsCandidate>,
  val latestAvailableBalancePaise: Long?
)

/**
 * Zero-permission on-device Bank & UPI alert parser.
 *
 * Works via Android Share Sheet ("Share -> Ghar"), 1-tap Clipboard scanning, or pasted alert blocks
 * without requesting `READ_SMS` or `RECEIVE_SMS` (preventing OTP exposure and OS fraud-protection blocks).
 */
object SmsInboxScanner {

  val sampleBankAlerts: List<String> = listOf(
    "Rs. 340.00 debited from A/c XX4821 on 26-10-26 to VPA swiggy@icici (UPI Ref 62991042). Avl Bal: Rs. 28,450.00",
    "INR 185.00 spent on HDFC Bank Card XX9012 at Uber Rides on 26-10-26. Available Balance: INR 28,265.00",
    "Your A/c XX4821 is credited with Rs. 4,500.00 from Freelance Client on 25-10-26. Avl Bal Rs. 32,765.00"
  )

  fun scanClipboardAlerts(
    context: Context,
    existingTransactions: List<Transaction>
  ): SmsScanSummary {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = clipboard?.primaryClip
    if (clip == null || clip.itemCount == 0) {
      return SmsScanSummary(
        scannedMessagesCount = 0,
        candidates = emptyList(),
        latestAvailableBalancePaise = null
      )
    }
    val combined = buildString {
      for (i in 0 until clip.itemCount) {
        val text = clip.getItemAt(i)?.coerceToText(context)?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) {
          if (isNotEmpty()) append("\n\n")
          append(text)
        }
      }
    }
    return parseBatchAlertText(
      rawBatch = combined,
      existingTransactions = existingTransactions,
      senderLabel = "Clipboard Alert"
    )
  }

  fun parseBatchAlertText(
    rawBatch: String,
    existingTransactions: List<Transaction> = emptyList(),
    senderLabel: String = "Pasted SMS"
  ): SmsScanSummary {
    val cleanBatch = rawBatch.trim()
    if (cleanBatch.isEmpty()) {
      return SmsScanSummary(
        scannedMessagesCount = 0,
        candidates = emptyList(),
        latestAvailableBalancePaise = null
      )
    }

    // Split multi-message pastes by blank lines first; if single block has multiple lines that each
    // look like standalone bank alerts, evaluate line-by-line as well.
    val rawBlocks = cleanBatch
      .split(Regex("\\n\\s*\\n+"))
      .flatMap { block ->
        val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size > 1 && lines.count { SmsParser.parse("SMS", it) != null } > 1) {
          lines
        } else {
          listOf(block.trim())
        }
      }
      .filter { it.isNotEmpty() }

    val nowMs = System.currentTimeMillis()
    var latestBalance: Long? = null
    val candidates = mutableListOf<ParsedSmsCandidate>()

    for ((idx, chunk) in rawBlocks.withIndex()) {
      val parsed = SmsParser.parse("SMS", chunk) ?: continue
      if (parsed.availableBalancePaise != null) {
        latestBalance = parsed.availableBalancePaise
      }

      val alreadyLogged = existingTransactions.any { existing ->
        (existing.rawText != null && existing.rawText == chunk) ||
          (existing.amountPaise == parsed.amountPaise &&
            existing.direction == parsed.direction &&
            existing.merchant.equals(parsed.merchant, ignoreCase = true) &&
            abs(existing.timestamp - nowMs) < 90_000L)
      }
      if (alreadyLogged) continue

      val category = MerchantLookupTable.lookupCategory(
        merchantOrText = "${parsed.merchant} $chunk",
        isCredit = parsed.direction == Direction.CREDIT
      )
      val tx = Transaction(
        id = "alert_${UUID.randomUUID().toString().take(8)}_$idx",
        amountPaise = parsed.amountPaise,
        direction = parsed.direction,
        merchant = parsed.merchant,
        category = category,
        timestamp = nowMs,
        source = CaptureSource.SMS,
        rawText = chunk
      )
      candidates.add(
        ParsedSmsCandidate(
          id = tx.id,
          sender = senderLabel,
          transaction = tx,
          availableBalancePaise = parsed.availableBalancePaise
        )
      )
    }

    return SmsScanSummary(
      scannedMessagesCount = rawBlocks.size,
      candidates = candidates,
      latestAvailableBalancePaise = latestBalance
    )
  }

  fun parseManualSmsText(rawSms: String): ParsedSmsCandidate? {
    val parsed = SmsParser.parse("SMS", rawSms) ?: return null
    val category = MerchantLookupTable.lookupCategory(
      merchantOrText = "${parsed.merchant} $rawSms",
      isCredit = parsed.direction == Direction.CREDIT
    )
    val tx = Transaction(
      amountPaise = parsed.amountPaise,
      direction = parsed.direction,
      merchant = parsed.merchant,
      category = category,
      timestamp = System.currentTimeMillis(),
      source = CaptureSource.SMS,
      rawText = rawSms.trim()
    )
    return ParsedSmsCandidate(
      id = tx.id,
      sender = "Pasted SMS",
      transaction = tx,
      availableBalancePaise = parsed.availableBalancePaise
    )
  }
}
