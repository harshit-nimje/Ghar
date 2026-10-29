package com.example.ui.common

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.contrastingContentColor

/**
 * Reusable high-clarity Material 3 confirmation dialog for irreversible or major household actions
 * (signing out, redoing onboarding, deleting savings pots/commitments, resetting balances, uninstalling offline models).
 */
@Composable
fun GharConfirmDialog(
  title: String,
  message: String,
  confirmLabel: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
  icon: ImageVector = Icons.Default.WarningAmber,
  isDestructive: Boolean = true,
  dismissLabel: String = stringResource(R.string.confirm_dialog_cancel),
  dialogTestTag: String = "ghar_confirm_dialog",
  confirmTestTag: String = "ghar_confirm_action_button",
  dismissTestTag: String = "ghar_confirm_cancel_button"
) {
  val accentColor = if (isDestructive) MaterialTheme.colorScheme.error else PastelHoney

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = JharokhaCardShape,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.35f)),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .widthIn(max = 420.dp)
        .testTag(dialogTestTag)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 22.dp, vertical = 20.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.14f),
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
              )
            }
          }
          Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag(dismissTestTag)
          ) {
            Text(
              text = dismissLabel,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          val confirmContainer = if (isDestructive) PastelTerracotta else MaterialTheme.colorScheme.primary
          val confirmContent = confirmContainer.contrastingContentColor()
          Button(
            onClick = onConfirm,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
              containerColor = confirmContainer,
              contentColor = confirmContent
            ),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag(confirmTestTag)
          ) {
            Text(
              text = confirmLabel,
              fontWeight = FontWeight.Bold,
              color = confirmContent,
              maxLines = 1
            )
          }
        }
      }
    }
  }
}
