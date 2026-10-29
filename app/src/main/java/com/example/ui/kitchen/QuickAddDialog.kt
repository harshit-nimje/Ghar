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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.model.Category
import com.example.core.model.QuickChip
import com.example.ui.common.CategoryIcon
import com.example.ui.common.HisaabCalculatorDialog
import com.example.ui.common.JharokhaCardShape
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelSky
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.contrastingContentColor

@Composable
internal fun QuickAddDialog(
  quickChips: List<QuickChip>,
  initialAmountRupees: Long? = null,
  onOpenCustomizeChips: () -> Unit,
  onDismiss: () -> Unit,
  onSave: (amount: Long, merchant: String, category: Category, isExpense: Boolean, isRecurring: Boolean) -> Unit
) {
  val plainEnglish = LocalPlainEnglishMode.current
  var amountStr by remember {
    mutableStateOf(
      if (initialAmountRupees != null && initialAmountRupees > 0L) initialAmountRupees.toString() else ""
    )
  }
  var merchantStr by remember { mutableStateOf("") }
  var isExpense by remember { mutableStateOf(true) }
  var selectedCategory by remember { mutableStateOf(Category.CHAI_SNACKS) }
  var isRecurring by remember { mutableStateOf(false) }
  var showCalculator by remember { mutableStateOf(false) }

  val spendCategories = remember {
    listOf(
      Category.CHAI_SNACKS,
      Category.FOOD_DELIVERY,
      Category.COMMUTE,
      Category.SHOPPING,
      Category.GROCERIES,
      Category.BILLS_UTILITIES,
      Category.ENTERTAINMENT,
      Category.HEALTH_MEDICAL,
      Category.OTHER
    )
  }
  val incomeCategories = remember {
    listOf(Category.ALLOWANCE_INCOME, Category.TRANSFERS, Category.OTHER)
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = JharokhaCardShape,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("quick_add_dialog")
    ) {
      Column(modifier = Modifier.padding(22.dp)) {
        Text(
          text = if (plainEnglish) {
            if (isExpense) "Log an Expense" else "Log Income"
          } else {
            if (isExpense) "Jot a Household Spend" else "Add Money to Batua"
          },
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        val matchingChips = remember(quickChips, isExpense) {
          quickChips.filter { it.isExpense == isExpense }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = stringResource(R.string.quick_stamps_presets_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = stringResource(R.string.quick_stamps_customize),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable { onOpenCustomizeChips() }
              .padding(horizontal = 6.dp, vertical = 4.dp)
              .testTag("quick_add_customize_presets_link")
          )
        }

        if (matchingChips.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
          ) {
            items(matchingChips, key = { it.id }) { chip ->
              val accent = chip.colorKey.toAccentColor()
              Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.40f)),
                modifier = Modifier
                  .defaultMinSize(minHeight = 40.dp)
                  .clip(CircleShape)
                  .clickable {
                    amountStr = chip.amountRupees.toString()
                    merchantStr = chip.displayMerchant(plainEnglish)
                    selectedCategory = chip.category
                    isExpense = chip.isExpense
                  }
                  .testTag("quick_add_preset_${chip.id}")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  CategoryIcon(category = chip.category, size = 14.dp, tint = accent)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "${chip.displayLabel(plainEnglish)} • ${formatQuickChipSublabel(chip)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = if (isExpense) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
              1.dp,
              if (isExpense) MaterialTheme.colorScheme.primary else Color.Transparent
            ),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .clip(CircleShape)
              .clickable {
                isExpense = true
                selectedCategory = Category.CHAI_SNACKS
              }
              .testTag("quick_add_tab_expense")
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (plainEnglish) "Expense (-)" else "Spent Outside",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Surface(
            shape = CircleShape,
            color = if (!isExpense) PastelSage.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
              1.dp,
              if (!isExpense) PastelSage else Color.Transparent
            ),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .clip(CircleShape)
              .clickable {
                isExpense = false
                isRecurring = false
                selectedCategory = Category.ALLOWANCE_INCOME
              }
              .testTag("quick_add_tab_income")
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (plainEnglish) "Income (+)" else "Came Home (+)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (!isExpense) PastelSage else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it.filter { char -> char.isDigit() } },
          label = { Text("Rupees (₹)") },
          trailingIcon = {
            IconButton(
              onClick = { showCalculator = true },
              modifier = Modifier
                .size(48.dp)
                .testTag("quick_add_open_calculator_button")
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
            .testTag("quick_add_amount_input"),
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isExpense) MaterialTheme.colorScheme.primary else PastelSage,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = merchantStr,
          onValueChange = { merchantStr = it },
          label = {
            Text(
              if (plainEnglish) {
                if (isExpense) "Merchant or description (e.g. Coffee)" else "Source (e.g. Salary / Bonus)"
              } else {
                if (isExpense) "What for? (e.g. Ginger Chai)" else "From where? (e.g. Shagun)"
              }
            )
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_add_merchant_input"),
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isExpense) MaterialTheme.colorScheme.primary else PastelSage,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Category:",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        val currentCategoryList = if (isExpense) spendCategories else incomeCategories

        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(vertical = 2.dp)
        ) {
          items(currentCategoryList, key = { it.name }) { cat ->
            val isSelected = selectedCategory == cat
            val tint = if (isExpense) MaterialTheme.colorScheme.primary else PastelSage
            Surface(
              shape = CircleShape,
              color = if (isSelected) {
                if (isExpense) MaterialTheme.colorScheme.primaryContainer else PastelSage.copy(alpha = 0.22f)
              } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              },
              border = BorderStroke(
                1.dp,
                if (isSelected) tint else Color.Transparent
              ),
              modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .clip(CircleShape)
                .clickable {
                  selectedCategory = cat
                  if (cat == Category.BILLS_UTILITIES) {
                    isRecurring = true
                  }
                }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                CategoryIcon(
                  category = cat,
                  size = 18.dp,
                  tint = if (isSelected) tint else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = cat.displayLabel(plainEnglish),
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1
                )
              }
            }
          }
        }

        if (isExpense) {
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
                  text = if (plainEnglish) {
                    "Recurring Monthly Bill (track in Insights)"
                  } else {
                    "Monthly Pakka Bill (track in Courtyard)"
                  },
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
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
          ) {
            Text("Close")
          }
          Spacer(modifier = Modifier.width(8.dp))
          val submitBg = if (isExpense) MaterialTheme.colorScheme.primary else RupeeGreen
          val submitContent = submitBg.contrastingContentColor()
          Button(
            onClick = {
              val amt = amountStr.toLongOrNull() ?: 0L
              if (amt > 0L) {
                val merchant = merchantStr.ifBlank { selectedCategory.displayLabel(plainEnglish) }
                onSave(amt, merchant, selectedCategory, isExpense, isExpense && isRecurring)
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = submitBg,
              contentColor = submitContent
            ),
            shape = CircleShape,
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("quick_add_submit_button")
          ) {
            Text(
              text = if (plainEnglish) {
                if (isExpense) "Save Expense" else "Save Income"
              } else {
                if (isExpense) "Write in Khata" else "Add to Batua"
              },
              color = submitContent,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
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
