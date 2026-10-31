package com.example.capture

import com.example.core.model.Transaction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object CaptureEventBus {
  const val EXTRA_OPEN_QUICK_KHARCHA = "com.example.extra.OPEN_QUICK_KHARCHA"

  private val _newTransactionEvent = MutableSharedFlow<Transaction>(extraBufferCapacity = 5)
  val newTransactionEvent = _newTransactionEvent.asSharedFlow()

  private val _pendingQuickKharchaRequest = MutableStateFlow(false)
  val pendingQuickKharchaRequest: StateFlow<Boolean> = _pendingQuickKharchaRequest.asStateFlow()

  private val _pendingSharedAlertText = MutableStateFlow<String?>(null)
  val pendingSharedAlertText: StateFlow<String?> = _pendingSharedAlertText.asStateFlow()

  fun notifyNewTransaction(transaction: Transaction) {
    _newTransactionEvent.tryEmit(transaction)
  }

  fun requestOpenQuickKharcha() {
    _pendingQuickKharchaRequest.value = true
  }

  fun consumeQuickKharchaRequest(): Boolean {
    val hadPending = _pendingQuickKharchaRequest.value
    if (hadPending) {
      _pendingQuickKharchaRequest.value = false
    }
    return hadPending
  }

  fun submitSharedAlertText(rawText: String) {
    val clean = rawText.trim()
    if (clean.isNotEmpty()) {
      _pendingSharedAlertText.value = clean
    }
  }

  fun consumeSharedAlertText(): String? {
    val current = _pendingSharedAlertText.value
    if (current != null) {
      _pendingSharedAlertText.value = null
    }
    return current
  }
}
