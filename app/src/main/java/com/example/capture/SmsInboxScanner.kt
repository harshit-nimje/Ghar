package com.example.capture

import android.content.Context
import android.provider.Telephony
import androidx.compose.runtime.Immutable
import com.example.capture.parser.MerchantLookupTable
import com.example.capture.parser.SmsParser
import com.example.core.model.CaptureSource
import com.example.core.model.Direction
import com.example.core.model.Transaction
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * Smart on-device SMS Inbox scanner & sample parser.
 * Reads recent bank/UPI alerts on Dispatchers.IO when READ_SMS is granted, filters out OTPs
 * and duplicates, and extracts transactions + latest reported bank balance.
 */
object SmsInboxScanner {

  val sampleBankAlerts: List<String> = listOf(
    "Rs. 340.00 debited from A/c XX4821 on 26-10-26 to VPA swiggy@icici (UPI Ref 62991042). Avl Bal: Rs. 28,450.00",
    "INR 185.00 spent on HDFC Bank Card XX9012 at Uber Rides on 26-10-26. Available Balance: INR 28,265.00",
    "Your A/c XX4821 is credited with Rs. 4,500.00 from Freelance Client on 25-10-26. Avl Bal Rs. 32,765.00"
  )

  suspend fun scanRecentInbox(
    context: Context,
    existingTransactions: List<Transaction>,
    lookbackDays: Int = 45,
    maxMessages: Int = 250
  ): Result<SmsScanSummary> = withContext(Dispatchers.IO) {
    runCatching {
      val cutoffMillis = System.currentTimeMillis() - lookbackDays.toLong() * 24L * 3600_000L
      val projection = arrayOf(
        Telephony.Sms._ID,
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE
      )
      val selection = "${Telephony.Sms.DATE} >= ?"
      val selectionArgs = arrayOf(cutoffMillis.toString())
      val sortOrder = "${Telephony.Sms.DATE} DESC"

      var scanned = 0
      var latestBalance: Long? = null
      val found = mutableListOf<ParsedSmsCandidate>()

      context.contentResolver.query(
        Telephony.Sms.Inbox.CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
      )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
        val addrCol = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val bodyCol = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
        val dateCol = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

        while (cursor.moveToNext() && scanned < maxMessages) {
          scanned++
          val smsId = cursor.getLong(idCol)
          val sender = cursor.getString(addrCol).orEmpty()
          val body = cursor.getString(bodyCol).orEmpty()
          val dateMs = cursor.getLong(dateCol).takeIf { it > 0L } ?: System.currentTimeMillis()

          val parsed = SmsParser.parse(sender, body) ?: continue
          if (latestBalance == null && parsed.availableBalancePaise != null) {
            latestBalance = parsed.availableBalancePaise
          }

          val alreadyLogged = existingTransactions.any { existing ->
            (existing.rawText != null && existing.rawText == body) ||
              (existing.amountPaise == parsed.amountPaise &&
                existing.direction == parsed.direction &&
                abs(existing.timestamp - dateMs) < 90_000L)
          }
          if (alreadyLogged) continue

          val category = MerchantLookupTable.lookupCategory(
            merchantOrText = "${parsed.merchant} $body",
            isCredit = parsed.direction == Direction.CREDIT
          )
          val tx = Transaction(
            id = "sms_${smsId}_${parsed.amountPaise}",
            amountPaise = parsed.amountPaise,
            direction = parsed.direction,
            merchant = parsed.merchant,
            category = category,
            timestamp = dateMs,
            source = CaptureSource.SMS,
            rawText = body
          )
          found.add(
            ParsedSmsCandidate(
              id = tx.id,
              sender = sender.ifBlank { "Bank SMS" },
              transaction = tx,
              availableBalancePaise = parsed.availableBalancePaise
            )
          )
        }
      }

      SmsScanSummary(
        scannedMessagesCount = scanned,
        candidates = found,
        latestAvailableBalancePaise = latestBalance
      )
    }
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
