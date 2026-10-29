package com.example.ui.kitchen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.model.Category
import com.example.core.model.Direction
import com.example.core.model.Transaction
import com.example.ui.common.CategoryIcon
import com.example.ui.common.HisaabCalculatorDialog
import com.example.ui.common.JharokhaCardShape
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelSky
import com.example.ui.theme.RupeeGreen

@Composable
fun EditTransactionDialog(
  transaction: Transaction,
  onDismiss: () -> Unit,
  onSave: (Transaction) -> Unit,
  onDelete: ((Transaction) -> Unit)? = null
) {
  val plainEnglish = LocalPlainEnglishMode.current
  var amountStr by remember(transaction.id) { mutableStateOf(transaction.amountRupees.toString()) }
  var merchantStr by remember(transaction.id) { mutableStateOf(transaction.merchant) }
  var selectedCategory by remember(transaction.id) { mutableStateOf(transaction.category) }
  var isExpense by remember(transaction.id) { mutableStateOf(transaction.isExpense) }
  var isRecurring by remember(transaction.id) { mutableStateOf(transaction.isRecurring) }
  var showCalculator by remember { mutableStateOf(false) }

  val isGullakEntry = selectedCategory == Category.TRANSFERS &&
    (merchantStr.contains("Gullak", ignoreCase = true) || merchantStr.contains("Savings Pot", ignoreCase = true))

  val allCategories = remember { Category.entries }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = JharokhaCardShape,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("edit_tx_dialog")
    ) {
      Column(modifier = Modifier.padding(22.dp)) {
        Text(
          text = if (isGullakEntry) {
            if (plainEnglish) "Edit Savings Deposit" else "Edit Gullak Clink"
          } else {
            if (plainEnglish) "Edit Transaction" else "Edit Khata Entry"
          },
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isExpense) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
              1.dp,
              if (isExpense) MaterialTheme.colorScheme.primary else Color.Transparent
            ),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable { isExpense = true }
              .testTag("edit_tx_direction_debit")
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (isGullakEntry) {
                  if (plainEnglish) "Deposit to Savings" else "Into Gullak"
                } else {
                  "Spent"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (!isExpense) RupeeGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
              1.dp,
              if (!isExpense) RupeeGreen else Color.Transparent
            ),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                isExpense = false
                if (!isGullakEntry && selectedCategory != Category.ALLOWANCE_INCOME && selectedCategory != Category.TRANSFERS) {
                  selectedCategory = Category.ALLOWANCE_INCOME
                }
              }
              .testTag("edit_tx_direction_credit")
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (isGullakEntry) {
                  if (plainEnglish) "Withdraw to Wallet" else "Dip to Batua"
                } else {
                  "+ Received"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (!isExpense) RupeeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it.filter { c -> c.isDigit() } },
          label = { Text("Amount (₹)") },
          prefix = {
            Text(
              text = "₹ ",
              style = GharHandStyles.moneyMedium,
              color = MaterialTheme.colorScheme.primary
            )
          },
          trailingIcon = {
            IconButton(
              onClick = { showCalculator = true },
              modifier = Modifier
                .size(48.dp)
                .testTag("edit_tx_open_calculator_button")
            ) {
              Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = "Open calculator",
                tint = PastelHoney
              )
            }
          },
          textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_tx_amount_input"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = merchantStr,
          onValueChange = { merchantStr = it },
          label = { Text("Note / Merchant name") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_tx_merchant_input"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Category (scroll for all):",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(vertical = 2.dp)
        ) {
          items(allCategories, key = { it.name }) { cat ->
            val isSelected = selectedCategory == cat
            Surface(
              shape = CircleShape,
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              border = BorderStroke(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
              ),
              modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .clip(CircleShape)
                .clickable { selectedCategory = cat }
                .testTag("edit_tx_cat_${cat.name.lowercase()}")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                CategoryIcon(
                  category = cat,
                  size = 17.dp,
                  tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = cat.displayLabel(plainEnglish),
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1
                )
              }
            }
          }
        }

        if (isExpense && !isGullakEntry) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = PastelSky.copy(alpha = 0.12f),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .clickable { isRecurring = !isRecurring }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.Autorenew,
                  contentDescription = null,
                  tint = PastelSky,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (plainEnglish) "Recurring Monthly Bill (track every month)" else "Monthly Pakka Bill (track every month)",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Switch(
                checked = isRecurring,
                onCheckedChange = { isRecurring = it }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (onDelete != null) {
            TextButton(
              onClick = { onDelete(transaction) },
              modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("edit_tx_delete_button")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Erase entry",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Erase",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
              )
            }
          } else {
            Spacer(modifier = Modifier.width(1.dp))
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
              onClick = onDismiss,
              modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
              Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
              onClick = {
                val amt = amountStr.toLongOrNull() ?: 0L
                if (amt > 0L) {
                  val updated = transaction.copy(
                    amountPaise = amt * 100L,
                    merchant = merchantStr.ifBlank { selectedCategory.displayLabel(plainEnglish) },
                    category = selectedCategory,
                    direction = if (isExpense) Direction.DEBIT else Direction.CREDIT,
                    isRecurring = if (isExpense && !isGullakEntry) isRecurring else false
                  )
                  onSave(updated)
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ),
              shape = CircleShape,
              modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("edit_tx_save_button")
            ) {
              Text(
                text = "Update",
                color = MaterialTheme.colorScheme.onPrimary,
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

  if (showCalculator) {
    HisaabCalculatorDialog(
      initialAmountRupees = amountStr.toLongOrNull(),
      onDismiss = { showCalculator = false },
      onUseAmount = { calculated ->
        amountStr = calculated.toString()
        showCalculator = false
      }
    )
  }
}
