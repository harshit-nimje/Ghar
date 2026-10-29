package com.example.capture

import com.example.core.model.Transaction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object CaptureEventBus {
  private val _newTransactionEvent = MutableSharedFlow<Transaction>(extraBufferCapacity = 5)
  val newTransactionEvent = _newTransactionEvent.asSharedFlow()

  fun notifyNewTransaction(transaction: Transaction) {
    _newTransactionEvent.tryEmit(transaction)
  }
}
