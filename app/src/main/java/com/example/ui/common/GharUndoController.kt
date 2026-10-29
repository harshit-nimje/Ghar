package com.example.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PastelHoney
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
class GharUndoController(
  val snackbarHostState: SnackbarHostState,
  private val scope: CoroutineScope
) {
  private var activeJob: Job? = null
  private var latestUndoBlock: (() -> Unit)? = null

  /**
   * Displays an immediate Undo snackbar for any mutating action in Ghar.
   * Cancels any prior snackbar first so rapid taps never queue or lag.
   */
  fun recordAction(
    message: String,
    undoLabel: String = "Undo",
    onUndo: () -> Unit
  ) {
    latestUndoBlock = onUndo
    activeJob?.cancel()
    snackbarHostState.currentSnackbarData?.dismiss()
    activeJob = scope.launch {
      val result = snackbarHostState.showSnackbar(
        message = message,
        actionLabel = undoLabel,
        withDismissAction = true,
        duration = SnackbarDuration.Short
      )
      if (result == SnackbarResult.ActionPerformed) {
        onUndo()
        latestUndoBlock = null
      }
    }
  }

  fun showNotice(message: String) {
    activeJob?.cancel()
    snackbarHostState.currentSnackbarData?.dismiss()
    activeJob = scope.launch {
      snackbarHostState.showSnackbar(
        message = message,
        duration = SnackbarDuration.Short
      )
    }
  }

  fun invokeLatestUndoForTest(): Boolean {
    val block = latestUndoBlock ?: return false
    latestUndoBlock = null
    snackbarHostState.currentSnackbarData?.dismiss()
    block()
    return true
  }
}

val LocalGharUndoController = compositionLocalOf<GharUndoController?> { null }

@Composable
fun rememberGharUndoController(): GharUndoController {
  val hostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  return remember(hostState, scope) {
    GharUndoController(snackbarHostState = hostState, scope = scope)
  }
}

@Composable
fun GharUndoSnackbarHost(
  controller: GharUndoController,
  modifier: Modifier = Modifier
) {
  SnackbarHost(
    hostState = controller.snackbarHostState,
    modifier = modifier.testTag("ghar_global_undo_snackbar")
  ) { data ->
    Snackbar(
      snackbarData = data,
      shape = RoundedCornerShape(20.dp),
      containerColor = Color(0xFF281D16),
      contentColor = Color(0xFFFFFBF4),
      actionColor = PastelHoney,
      dismissActionContentColor = Color(0xFFD2C6B8)
    )
  }
}
