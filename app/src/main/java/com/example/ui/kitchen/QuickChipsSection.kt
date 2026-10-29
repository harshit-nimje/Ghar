package com.example.ui.kitchen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.core.localization.GharTerms
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.model.Category
import com.example.core.model.Direction
import com.example.core.model.QuickChip
import com.example.core.model.StampColorKey
import com.example.core.util.CurrencyFormatter
import com.example.ui.common.CategoryIcon
import com.example.ui.common.GharConfirmDialog
import com.example.ui.common.JharokhaCardShape
import com.example.ui.common.PebbleCardShape
import com.example.ui.common.TactileMomentStamp
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelPlum
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelSky
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.contrastingContentColor
import java.util.UUID

internal fun StampColorKey.toAccentColor(): Color = when (this) {
  StampColorKey.HONEY -> PastelHoney
  StampColorKey.TERRACOTTA -> PastelTerracotta
  StampColorKey.SKY -> PastelSky
  StampColorKey.SAGE -> PastelSage
  StampColorKey.PLUM -> PastelPlum
}

internal fun formatQuickChipSublabel(chip: QuickChip): String {
  val rupees = chip.amountRupees
  val prefix = if (chip.isExpense) "" else "+"
  val body = when {
    rupees >= 1000L && rupees % 1000L == 0L -> "₹${rupees / 1000L}k"
    rupees >= 10_000L -> CurrencyFormatter.formatCompactRupees(rupees)
    else -> "₹$rupees"
  }
  return "$prefix$body"
}

@Composable
fun QuickKhataStampsSection(
  chips: List<QuickChip>,
  onStampClick: (QuickChip) -> Unit,
  onOpenCustomize: (startInCreateMode: Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val plainEnglish = LocalPlainEnglishMode.current
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = GharTerms.quickStampsTitle(plainEnglish),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )

      Surface(
        onClick = { onOpenCustomize(false) },
        shape = CircleShape,
        color = PastelTerracotta.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, PastelTerracotta.copy(alpha = 0.30f)),
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag("customize_quick_chips_button")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = stringResource(R.string.quick_stamps_customize),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.quick_stamps_customize),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(vertical = 2.dp)
        .testTag("quick_chips_strip"),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      chips.forEach { chip ->
        val accent = chip.colorKey.toAccentColor()
        TactileMomentStamp(
          label = chip.displayLabel(plainEnglish),
          sublabel = formatQuickChipSublabel(chip),
          accentColor = accent,
          containerWash = accent.copy(alpha = 0.14f),
          icon = {
            CategoryIcon(
              category = chip.category,
              size = 16.dp,
              tint = accent
            )
          },
          onClick = { onStampClick(chip) },
          modifier = Modifier
            .width(90.dp)
            .testTag("quick_chip_stamp_${chip.id}")
        )
      }

      val addShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 14.dp,
        bottomEnd = 20.dp,
        bottomStart = 14.dp
      )
      Surface(
        onClick = { onOpenCustomize(true) },
        shape = addShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier
          .width(86.dp)
          .defaultMinSize(minHeight = 92.dp)
          .testTag("add_quick_chip_inline_button")
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = stringResource(R.string.quick_stamps_add_new),
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(R.string.quick_stamps_add_new),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "Custom",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
          )
        }
      }
    }
  }
}

@Composable
fun QuickChipsCustomizeDialog(
  chips: List<QuickChip>,
  startInCreateMode: Boolean,
  onSaveChip: (QuickChip) -> Unit,
  onDeleteChip: (QuickChip) -> Unit,
  onReorderChips: (List<QuickChip>) -> Unit,
  onResetDefaults: () -> Unit,
  onDismiss: () -> Unit
) {
  val plainEnglish = LocalPlainEnglishMode.current
  var editingChip by remember { mutableStateOf<QuickChip?>(null) }
  var isCreatingNew by remember { mutableStateOf(startInCreateMode) }
  var showResetConfirm by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = JharokhaCardShape,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      border = BorderStroke(1.dp, PastelTerracotta.copy(alpha = 0.28f)),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.86f)
        .widthIn(max = 540.dp)
        .padding(vertical = 12.dp)
        .testTag("quick_chips_customize_dialog")
    ) {
      if (isCreatingNew || editingChip != null) {
        QuickChipEditorForm(
          initialChip = editingChip,
          nextSortOrder = chips.size,
          onCancel = {
            if (startInCreateMode && editingChip == null && chips.isNotEmpty()) {
              isCreatingNew = false
            } else {
              editingChip = null
              isCreatingNew = false
            }
          },
          onSave = { saved ->
            onSaveChip(saved)
            editingChip = null
            isCreatingNew = false
          }
        )
      } else {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.quick_stamps_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = stringResource(R.string.quick_stamps_dialog_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .size(48.dp)
                .testTag("close_customize_chips_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close quick stamps customizer",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .testTag("customize_chips_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
          ) {
            if (chips.isEmpty()) {
              item {
                Surface(
                  shape = PebbleCardShape,
                  color = PastelHoney.copy(alpha = 0.14f),
                  border = BorderStroke(1.dp, PastelHoney.copy(alpha = 0.35f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "No quick stamps yet. Tap 'Add Stamp' below or restore household defaults.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                  )
                }
              }
            } else {
              itemsIndexed(chips, key = { _, c -> c.id }) { index, chip ->
                val accent = chip.colorKey.toAccentColor()
                Surface(
                  shape = PebbleCardShape,
                  color = MaterialTheme.colorScheme.surface,
                  tonalElevation = 1.dp,
                  border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manage_chip_row_${chip.id}")
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                      contentAlignment = Alignment.Center
                    ) {
                      CategoryIcon(category = chip.category, size = 18.dp, tint = accent)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = chip.displayLabel(plainEnglish),
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = formatQuickChipSublabel(chip),
                          style = GharHandStyles.moneySmall,
                          color = if (chip.isExpense) accent else RupeeGreen,
                          maxLines = 1
                        )
                      }
                      Text(
                        text = "${chip.displayMerchant(plainEnglish)} • ${chip.category.displayLabel(plainEnglish)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }

                    if (chips.size > 1) {
                      IconButton(
                        onClick = {
                          if (index > 0) {
                            val mutable = chips.toMutableList()
                            val moved = mutable.removeAt(index)
                            mutable.add(index - 1, moved)
                            onReorderChips(mutable)
                          }
                        },
                        enabled = index > 0,
                        modifier = Modifier
                          .size(48.dp)
                          .testTag("move_up_chip_${chip.id}")
                      ) {
                        Icon(
                          imageVector = Icons.Default.KeyboardArrowUp,
                          contentDescription = "Move ${chip.label} earlier"
                        )
                      }
                      IconButton(
                        onClick = {
                          if (index < chips.lastIndex) {
                            val mutable = chips.toMutableList()
                            val moved = mutable.removeAt(index)
                            mutable.add(index + 1, moved)
                            onReorderChips(mutable)
                          }
                        },
                        enabled = index < chips.lastIndex,
                        modifier = Modifier
                          .size(48.dp)
                          .testTag("move_down_chip_${chip.id}")
                      ) {
                        Icon(
                          imageVector = Icons.Default.KeyboardArrowDown,
                          contentDescription = "Move ${chip.label} later"
                        )
                      }
                    }

                    IconButton(
                      onClick = { editingChip = chip },
                      modifier = Modifier
                        .size(48.dp)
                        .testTag("edit_chip_${chip.id}")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit ${chip.label}",
                        tint = MaterialTheme.colorScheme.primary
                      )
                    }

                    IconButton(
                      onClick = { onDeleteChip(chip) },
                      modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_chip_${chip.id}")
                    ) {
                      Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete ${chip.label}",
                        tint = MaterialTheme.colorScheme.error
                      )
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(
              onClick = { showResetConfirm = true },
              modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("reset_quick_chips_button")
            ) {
              Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.quick_stamps_reset_defaults),
                maxLines = 1
              )
            }

            Button(
              onClick = { isCreatingNew = true },
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ),
              modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("add_new_quick_chip_button")
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.quick_stamps_add_new),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
              )
            }
          }
        }
      }
    }
  }

  if (showResetConfirm) {
    GharConfirmDialog(
      title = "Reset Quick Shortcuts?",
      message = "This replaces your customized quick-add shortcuts with the 5 starter household defaults.",
      confirmLabel = "Reset Shortcuts",
      onConfirm = {
        showResetConfirm = false
        onResetDefaults()
      },
      onDismiss = { showResetConfirm = false },
      dialogTestTag = "confirm_reset_quick_chips_dialog",
      confirmTestTag = "confirm_reset_quick_chips_button"
    )
  }
}

@Composable
private fun QuickChipEditorForm(
  initialChip: QuickChip?,
  nextSortOrder: Int,
  onCancel: () -> Unit,
  onSave: (QuickChip) -> Unit
) {
  val plainEnglish = LocalPlainEnglishMode.current
  var label by remember(initialChip) { mutableStateOf(initialChip?.label ?: "") }
  var amountStr by remember(initialChip) {
    mutableStateOf(initialChip?.amountRupees?.toString() ?: "")
  }
  var merchant by remember(initialChip) { mutableStateOf(initialChip?.merchant ?: "") }
  var isExpense by remember(initialChip) { mutableStateOf(initialChip?.isExpense ?: true) }
  var selectedCategory by remember(initialChip) {
    mutableStateOf(initialChip?.category ?: Category.CHAI_SNACKS)
  }
  var selectedColor by remember(initialChip) {
    mutableStateOf(initialChip?.colorKey ?: StampColorKey.HONEY)
  }

  val previewAmountRupees = amountStr.toLongOrNull()?.coerceAtLeast(1L) ?: 50L
  val previewChip = QuickChip(
    id = initialChip?.id ?: "preview",
    label = label.trim().ifBlank { "My Stamp" },
    merchant = merchant.trim().ifBlank { label.trim().ifBlank { "My Stamp" } },
    amountPaise = previewAmountRupees * 100L,
    category = selectedCategory,
    direction = if (isExpense) Direction.DEBIT else Direction.CREDIT,
    colorKey = selectedColor,
    sortOrder = initialChip?.sortOrder ?: nextSortOrder
  )

  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 16.dp)
  ) {
    Text(
      text = stringResource(
        if (initialChip == null) R.string.quick_stamps_create_title else R.string.quick_stamps_edit_title
      ),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Live Linen Stamp Preview
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.Center
    ) {
      val accent = selectedColor.toAccentColor()
      TactileMomentStamp(
        label = previewChip.label,
        sublabel = formatQuickChipSublabel(previewChip),
        accentColor = accent,
        containerWash = accent.copy(alpha = 0.16f),
        icon = {
          CategoryIcon(category = selectedCategory, size = 18.dp, tint = accent)
        },
        onClick = {},
        modifier = Modifier.width(108.dp)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Expense vs Income toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Surface(
        shape = CircleShape,
        color = if (isExpense) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
            if (selectedCategory == Category.ALLOWANCE_INCOME) {
              selectedCategory = Category.CHAI_SNACKS
            }
          }
          .testTag("chip_editor_type_expense")
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = "Expense (-)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isExpense) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Surface(
        shape = CircleShape,
        color = if (!isExpense) PastelSage.copy(alpha = 0.32f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
            selectedCategory = Category.ALLOWANCE_INCOME
          }
          .testTag("chip_editor_type_income")
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = "Income (+)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (!isExpense) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
      value = label,
      onValueChange = { label = it.take(24) },
      label = { Text(stringResource(R.string.quick_stamps_label_hint)) },
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("chip_editor_label_input")
    )

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
      value = amountStr,
      onValueChange = { amountStr = it.filter { ch -> ch.isDigit() }.take(8) },
      label = { Text(stringResource(R.string.quick_stamps_amount_hint)) },
      textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("chip_editor_amount_input")
    )

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
      value = merchant,
      onValueChange = { merchant = it.take(120) },
      label = { Text(stringResource(R.string.quick_stamps_merchant_hint)) },
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("chip_editor_merchant_input")
    )

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "Category:",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(6.dp))

    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(Category.entries, key = { it.name }) { cat ->
        val isSelected = selectedCategory == cat
        Surface(
          shape = CircleShape,
          color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
          ),
          modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .clickable { selectedCategory = cat }
            .testTag("chip_editor_cat_${cat.name}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CategoryIcon(
              category = cat,
              size = 16.dp,
              tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = cat.displayLabel(plainEnglish),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "Stamp Pastel Wash:",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      StampColorKey.entries.forEach { colorKey ->
        val swatch = colorKey.toAccentColor()
        val isChosen = selectedColor == colorKey
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(swatch.copy(alpha = if (isChosen) 0.36f else 0.18f))
            .clickable { selectedColor = colorKey }
            .testTag("chip_editor_color_${colorKey.name}"),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(swatch),
            contentAlignment = Alignment.Center
          ) {
            if (isChosen) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = colorKey.label,
                tint = swatch.contrastingContentColor(),
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    val parsedAmount = amountStr.toLongOrNull() ?: 0L
    val canSave = label.trim().isNotBlank() && parsedAmount > 0L

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextButton(
        onClick = onCancel,
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag("cancel_quick_chip_edit_button")
      ) {
        Text("Cancel")
      }
      Spacer(modifier = Modifier.width(8.dp))
      Button(
        onClick = {
          if (canSave) {
            val cleanLabel = label.trim().take(24)
            val cleanMerchant = merchant.trim().ifBlank { cleanLabel }.take(120)
            onSave(
              QuickChip(
                id = initialChip?.id ?: "chip_${UUID.randomUUID()}",
                label = cleanLabel,
                merchant = cleanMerchant,
                amountPaise = parsedAmount * 100L,
                category = selectedCategory,
                direction = if (isExpense) Direction.DEBIT else Direction.CREDIT,
                colorKey = selectedColor,
                sortOrder = initialChip?.sortOrder ?: nextSortOrder
              )
            )
          }
        },
        enabled = canSave,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag("save_quick_chip_button")
      ) {
        Text(
          text = stringResource(R.string.quick_stamps_save),
          color = MaterialTheme.colorScheme.onPrimary,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
