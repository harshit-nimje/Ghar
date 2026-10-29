package com.example.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.GharApplication
import com.example.capture.dedupe.TransactionDeduplicator
import com.example.capture.parser.MerchantLookupTable
import com.example.capture.parser.SmsParser
import com.example.core.model.Transaction
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsCaptureReceiver : BroadcastReceiver() {

  private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context == null || intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
    val app = context.applicationContext as? GharApplication ?: return
    val uid = Firebase.auth.currentUser?.uid
    val activeContainer = if (uid != null) app.rootContainer.forUser(uid) else app.rootContainer.guestContainer

    for (sms in messages) {
      val sender = sms.originatingAddress ?: ""
      val body = sms.messageBody ?: ""

      val parsed = SmsParser.parse(sender, body) ?: continue

      if (TransactionDeduplicator.isDuplicate(parsed)) {
        continue
      }

      val category = MerchantLookupTable.lookupCategory(
        merchantOrText = "${parsed.merchant} $body",
        isCredit = parsed.direction == com.example.core.model.Direction.CREDIT
      )
      val transaction = Transaction(
        amountPaise = parsed.amountPaise,
        direction = parsed.direction,
        merchant = parsed.merchant,
        category = category,
        timestamp = sms.timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis(),
        source = parsed.source,
        rawText = parsed.rawText
      )

      val appCtx = context.applicationContext
      receiverScope.launch {
        runCatching {
          activeContainer.transactionRepository.insert(transaction)
          if (parsed.availableBalancePaise != null) {
            activeContainer.preferencesRepository.setLastDetectedBankBalancePaise(parsed.availableBalancePaise)
          }
          CaptureEventBus.notifyNewTransaction(transaction)
          if (activeContainer.preferencesRepository.prefsSnapshot.value.autoBalanceAlertsEnabled) {
            val sign = if (transaction.isExpense) "-" else "+"
            val note = "${sign}₹${transaction.amountRupees} ${transaction.merchant}"
            HouseholdNotificationHelper.postQuickBalanceNotification(
              context = appCtx,
              container = activeContainer,
              triggerNote = note
            )
          }
        }
      }
    }
  }
}
