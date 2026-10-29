package com.example.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.engine.HouseholdAnalyticsEngine.CommitmentsCycleSummary
import com.example.core.engine.HouseholdAnalyticsEngine.EvaluatedCommitment
import com.example.core.localization.GharTerms
import com.example.core.model.Category
import com.example.core.model.CommitmentType
import com.example.core.model.PakkaCommitment
import com.example.core.model.Transaction
import com.example.core.util.CurrencyFormatter
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelPlum
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelSky
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.contrastingContentColor
import com.example.ui.theme.readableAccentOn

enum class CommitmentFilterTab(val label: String) {
  ALL("All"),
  DUE("Pending"),
  SUBSCRIPTIONS("Subs"),
  RECEIVABLES("Receivables")
}

private fun commitmentAccentColor(type: CommitmentType): Color = when (type) {
  CommitmentType.BILL -> PastelHoney
  CommitmentType.PAYMENT -> PastelTerracotta
  CommitmentType.SUBSCRIPTION -> PastelPlum
  CommitmentType.RECEIVABLE -> RupeeGreen
}

/**
 * Courtyard & Home card displaying customizable Pakke Monthly Bills, Fixed Payments/EMIs,
 * Subscriptions, and Expected Receivables with 1-tap Pay/Receive settlement, editing, and filtering.
 */
@Composable
fun PakkeCommitmentsCard(
  summary: CommitmentsCycleSummary,
  plainEnglish: Boolean,
  onAddCommitment: () -> Unit,
  onEditCommitment: (PakkaCommitment) -> Unit,
  onSettleCommitment: (PakkaCommitment) -> Unit,
  onUnsettleCommitment: (PakkaCommitment, Transaction) -> Unit,
  onOpenFullManager: (() -> Unit)? = null,
  maxItemsPreview: Int = 8
) {
  var selectedTab by remember { mutableStateOf(CommitmentFilterTab.ALL) }

  val filteredItems = remember(summary.items, selectedTab) {
    when (selectedTab) {
      CommitmentFilterTab.ALL -> summary.items
      CommitmentFilterTab.DUE -> summary.items.filter { it.commitment.isActive && !it.settledThisCycle }
      CommitmentFilterTab.SUBSCRIPTIONS -> summary.items.filter { it.commitment.type == CommitmentType.SUBSCRIPTION }
      CommitmentFilterTab.RECEIVABLES -> summary.items.filter { it.commitment.isReceivable }
    }
  }

  Surface(
    shape = JharokhaCardShape,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.dp, PastelHoney.copy(alpha = 0.35f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("pakke_commitments_card")
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = CircleShape,
            color = PastelHoney.copy(alpha = 0.16f),
            modifier = Modifier.size(38.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.EventRepeat,
                contentDescription = null,
                tint = PastelHoney,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = GharTerms.recurringBillsTitle(plainEnglish),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            val statusSub = buildString {
              append("${summary.settledOutflowsCount}/${summary.activeOutflowsCount} outflows settled")
              if (summary.activeReceivablesCount > 0) {
                append(" • ${summary.settledReceivablesCount}/${summary.activeReceivablesCount} receivables")
              }
            }
            Text(
              text = statusSub,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
          shape = CircleShape,
          color = PastelHoney.copy(alpha = 0.18f),
          border = BorderStroke(1.dp, PastelHoney.copy(alpha = 0.45f)),
          modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .clickable { onAddCommitment() }
            .testTag("add_pakka_bill_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Add bill, subscription, or receivable",
              tint = PastelHoney,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Add",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Live Cycle Totals Banner
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = PastelTerracotta.copy(alpha = 0.10f),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
              text = "Bills & Subs Due",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = CurrencyFormatter.formatRupees(summary.pendingOutflowRupees),
              style = GharHandStyles.moneySmall,
              color = if (summary.pendingOutflowRupees > 0L) PastelTerracotta else PastelSage
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = RupeeGreen.copy(alpha = 0.10f),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
              text = "Expected In",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "+${CurrencyFormatter.formatRupees(summary.pendingReceivableRupees)}",
              style = GharHandStyles.moneySmall,
              color = RupeeGreen
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Filter Tabs (Plain Row, zero nested LazyRow)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        CommitmentFilterTab.entries.forEach { tab ->
          val selected = tab == selectedTab
          Surface(
            shape = CircleShape,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 38.dp)
              .clip(CircleShape)
              .clickable { selectedTab = tab }
              .testTag("commitment_filter_${tab.name.lowercase()}")
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
              Text(
                text = tab.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (filteredItems.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = when (selectedTab) {
              CommitmentFilterTab.DUE -> "All active bills, subscriptions, and receivables for this cycle are settled."
              CommitmentFilterTab.SUBSCRIPTIONS -> "No monthly subscriptions tracked yet. Tap + Add to add streaming, music, or gym plans."
              CommitmentFilterTab.RECEIVABLES -> "No monthly receivables tracked yet. Tap + Add to track rent splits, retainers, or allowances."
              CommitmentFilterTab.ALL -> "No recurring items tracked yet. Tap + Add to add monthly bills, payments, subscriptions, or receivables."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp)
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          filteredItems.take(maxItemsPreview).forEach { eval ->
            PakkaCommitmentRow(
              evaluated = eval,
              plainEnglish = plainEnglish,
              onEdit = { onEditCommitment(eval.commitment) },
              onSettle = { onSettleCommitment(eval.commitment) },
              onUnsettle = { matchedTx -> onUnsettleCommitment(eval.commitment, matchedTx) }
            )
          }
        }
      }

      if (onOpenFullManager != null) {
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(
          onClick = onOpenFullManager,
          modifier = Modifier
            .align(Alignment.End)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("manage_pakke_commitments_button")
        ) {
          Text(
            text = "Manage All (${summary.items.size})",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }
  }
}

@Composable
fun PakkaCommitmentRow(
  evaluated: EvaluatedCommitment,
  plainEnglish: Boolean,
  onEdit: () -> Unit,
  onSettle: () -> Unit,
  onUnsettle: (Transaction) -> Unit
) {
  val item = evaluated.commitment
  val accent = commitmentAccentColor(item.type)
  val settled = evaluated.settledThisCycle
  val matchedTx = evaluated.matchedTransaction

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = when {
      !item.isActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)
      settled -> PastelSage.copy(alpha = 0.12f)
      evaluated.isDueSoonOrOverdue -> accent.copy(alpha = 0.10f)
      else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
    },
    border = BorderStroke(
      width = 1.dp,
      color = when {
        !item.isActive -> MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        settled -> PastelSage.copy(alpha = 0.35f)
        evaluated.isDueSoonOrOverdue -> accent.copy(alpha = 0.42f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
      }
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { onEdit() }
      .testTag("commitment_row_${item.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Icon(
          imageVector = when {
            !item.isActive -> Icons.Default.PauseCircleOutline
            settled -> Icons.Default.CheckCircle
            item.isReceivable -> Icons.AutoMirrored.Filled.CallReceived
            item.type == CommitmentType.SUBSCRIPTION -> Icons.Default.Subscriptions
            else -> Icons.Default.EventRepeat
          },
          contentDescription = null,
          tint = when {
            !item.isActive -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            settled -> PastelSage
            else -> accent
          },
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = item.displayTitle(plainEnglish),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = if (item.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )
            Surface(
              shape = CircleShape,
              color = accent.copy(alpha = 0.15f)
            ) {
              Text(
                text = item.type.shortBadge,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          val metaLine = buildString {
            when {
              !item.isActive -> append("Paused • ${item.dueDayOrdinal}")
              settled && item.isReceivable -> append("Received this cycle")
              settled -> append("Paid this cycle")
              item.isReceivable -> append("Expected by ${item.dueDayOrdinal}")
              evaluated.isDueSoonOrOverdue -> append("Due ${item.dueDayOrdinal} • Due soon")
              else -> append("Due ${item.dueDayOrdinal} of month")
            }
            if (item.notes.isNotBlank()) {
              append(" • ${item.notes}")
            }
          }
          Text(
            text = metaLine,
            style = MaterialTheme.typography.labelSmall,
            color = if (settled) PastelSage else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        IconButton(
          onClick = onEdit,
          modifier = Modifier
            .size(48.dp)
            .testTag("edit_commitment_${item.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit ${item.displayTitle(plainEnglish)}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            modifier = Modifier.size(17.dp)
          )
        }

        if (!item.isActive) {
          Text(
            text = CurrencyFormatter.formatCompactRupees(item.amountRupees),
            style = GharHandStyles.moneySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else if (settled && matchedTx != null) {
          OutlinedButton(
            onClick = { onUnsettle(matchedTx) },
            shape = CircleShape,
            border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.55f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("settle_commitment_${item.id}")
          ) {
            Text(
              text = if (item.isReceivable) "Received ✓" else "Paid ✓",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = PastelSage
            )
          }
        } else {
          val settleBg = if (item.isReceivable) RupeeGreen else MaterialTheme.colorScheme.primary
          val settleContent = settleBg.contrastingContentColor()
          Button(
            onClick = onSettle,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
              containerColor = settleBg,
              contentColor = settleContent
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("settle_commitment_${item.id}")
          ) {
            val actionPrefix = if (item.isReceivable) "Receive +" else "Pay "
            Text(
              text = "$actionPrefix${CurrencyFormatter.formatCompactRupees(item.amountRupees)}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = settleContent,
              maxLines = 1
            )
          }
        }
      }
    }
  }
}

/**
 * Full-screen/modal manager dialog accessible from Home (`KitchenScreen`) and Courtyard (`LivingRoomScreen`)
 * to customize, add, edit, delete, settle, or reset monthly bills, payments, subscriptions, and receivables.
 */
@Composable
fun PakkeCommitmentsDialog(
  summary: CommitmentsCycleSummary,
  plainEnglish: Boolean,
  onDismiss: () -> Unit,
  onSaveCommitment: (PakkaCommitment, Boolean) -> Unit,
  onDeleteCommitment: (PakkaCommitment) -> Unit,
  onSettleCommitment: (PakkaCommitment) -> Unit,
  onUnsettleCommitment: (PakkaCommitment, Transaction) -> Unit,
  onResetDefaults: () -> Unit
) {
  var editingItem by remember { mutableStateOf<PakkaCommitment?>(null) }
  var isCreatingNew by remember { mutableStateOf(false) }
  var showResetConfirm by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = JharokhaCardShape,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      border = BorderStroke(1.2.dp, PastelHoney.copy(alpha = 0.42f)),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .widthIn(max = 520.dp)
        .fillMaxHeight(0.88f)
        .testTag("pakke_commitments_dialog")
    ) {
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
              text = GharTerms.recurringBillsTitle(plainEnglish),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Customize bills, EMIs, subscriptions, and expected monthly receivables",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(48.dp)
              .testTag("close_pakke_commitments_dialog")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Bills & Receivables"
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          PakkeCommitmentsCard(
            summary = summary,
            plainEnglish = plainEnglish,
            onAddCommitment = { isCreatingNew = true },
            onEditCommitment = { editingItem = it },
            onSettleCommitment = onSettleCommitment,
            onUnsettleCommitment = onUnsettleCommitment,
            onOpenFullManager = null,
            maxItemsPreview = 50
          )
        }

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 10.dp),
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = { showResetConfirm = true },
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("reset_commitments_defaults_button")
          ) {
            Icon(
              imageVector = Icons.Default.RestartAlt,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset Defaults")
          }

          Button(
            onClick = { isCreatingNew = true },
            shape = CircleShape,
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("dialog_add_commitment_button")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Item", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }

  if (isCreatingNew || editingItem != null) {
    PakkaCommitmentEditorDialog(
      existing = editingItem,
      plainEnglish = plainEnglish,
      onDismiss = {
        isCreatingNew = false
        editingItem = null
      },
      onSave = { updated, markSettledNow ->
        isCreatingNew = false
        editingItem = null
        onSaveCommitment(updated, markSettledNow)
      },
      onDelete = editingItem?.let { target ->
        {
          editingItem = null
          onDeleteCommitment(target)
        }
      }
    )
  }

  if (showResetConfirm) {
    GharConfirmDialog(
      title = "Reset Monthly Commitments?",
      message = "This replaces your customized monthly bills, subscriptions, and receivables list with the starter defaults. Your recorded ledger transactions will not be deleted.",
      confirmLabel = "Reset List",
      onConfirm = {
        showResetConfirm = false
        onResetDefaults()
      },
      onDismiss = { showResetConfirm = false },
      dialogTestTag = "confirm_reset_commitments_dialog",
      confirmTestTag = "confirm_reset_commitments_button"
    )
  }
}

private data class CommitmentPreset(
  val title: String,
  val amountRupees: Long,
  val dueDay: Int,
  val category: Category,
  val note: String
)

private fun presetsForType(type: CommitmentType): List<CommitmentPreset> = when (type) {
  CommitmentType.BILL -> listOf(
    CommitmentPreset("Electricity Bill", 1800L, 10, Category.BILLS_UTILITIES, "Monthly power bill"),
    CommitmentPreset("WiFi Broadband", 799L, 5, Category.BILLS_UTILITIES, "Home internet fiber"),
    CommitmentPreset("Cooking Gas / Water", 950L, 15, Category.BILLS_UTILITIES, "Utility refill")
  )
  CommitmentType.PAYMENT -> listOf(
    CommitmentPreset("House Rent", 12000L, 5, Category.BILLS_UTILITIES, "Monthly rent transfer"),
    CommitmentPreset("Loan / Device EMI", 3500L, 7, Category.BILLS_UTILITIES, "Monthly installment"),
    CommitmentPreset("Domestic Help / Cook", 2500L, 1, Category.GROCERIES, "Monthly salary")
  )
  CommitmentType.SUBSCRIPTION -> listOf(
    CommitmentPreset("Streaming Plan", 299L, 15, Category.ENTERTAINMENT, "Monthly video plan"),
    CommitmentPreset("Music / Cloud Plan", 149L, 12, Category.ENTERTAINMENT, "Auto-renewal"),
    CommitmentPreset("Gym / Fitness", 1500L, 1, Category.HEALTH_MEDICAL, "Monthly membership")
  )
  CommitmentType.RECEIVABLE -> listOf(
    CommitmentPreset("Shared Bill Split", 1500L, 7, Category.ALLOWANCE_INCOME, "Flatmate share incoming"),
    CommitmentPreset("Client / Side Retainer", 5000L, 10, Category.ALLOWANCE_INCOME, "Expected payout"),
    CommitmentPreset("Monthly Allowance", 3000L, 1, Category.ALLOWANCE_INCOME, "Incoming transfer")
  )
}

/**
 * Full-featured Editor Dialog to create, customize, pause/resume, or delete (with confirmation)
 * any Monthly Bill, Fixed Payment/EMI, Subscription, or Receivable.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PakkaCommitmentEditorDialog(
  existing: PakkaCommitment?,
  plainEnglish: Boolean,
  onDismiss: () -> Unit,
  onSave: (PakkaCommitment, Boolean) -> Unit,
  onDelete: (() -> Unit)? = null
) {
  var selectedType by remember(existing?.id) {
    mutableStateOf(existing?.type ?: CommitmentType.BILL)
  }
  var titleText by remember(existing?.id) {
    mutableStateOf(existing?.title ?: "")
  }
  var amountText by remember(existing?.id) {
    mutableStateOf(existing?.amountRupees?.toString() ?: "1000")
  }
  var dueDayText by remember(existing?.id) {
    mutableStateOf((existing?.safeDueDay ?: 5).toString())
  }
  var selectedCategory by remember(existing?.id) {
    mutableStateOf(existing?.category ?: (existing?.type ?: CommitmentType.BILL).defaultCategory)
  }
  var notesText by remember(existing?.id) {
    mutableStateOf(existing?.notes ?: "")
  }
  var isActive by remember(existing?.id) {
    mutableStateOf(existing?.isActive ?: true)
  }
  var markSettledNow by remember(existing?.id) {
    mutableStateOf(false)
  }
  var showCalculator by remember { mutableStateOf(false) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  val presets = remember(selectedType) { presetsForType(selectedType) }
  val availableCategories = remember(selectedType) {
    if (selectedType == CommitmentType.RECEIVABLE) {
      listOf(Category.ALLOWANCE_INCOME, Category.TRANSFERS, Category.OTHER)
    } else {
      listOf(
        Category.BILLS_UTILITIES,
        Category.ENTERTAINMENT,
        Category.GROCERIES,
        Category.HEALTH_MEDICAL,
        Category.COMMUTE,
        Category.SHOPPING,
        Category.OTHER
      )
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = JharokhaCardShape,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      border = BorderStroke(1.2.dp, commitmentAccentColor(selectedType).copy(alpha = 0.42f)),
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .widthIn(max = 480.dp)
        .fillMaxHeight(0.88f)
        .testTag("pakka_commitment_editor_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (existing == null) "Add Monthly Item" else "Edit ${existing.displayTitle(plainEnglish)}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close editor")
          }
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // 1. Type Selector (Bill, Payment/EMI, Subscription, Receivable)
          Text(
            text = "Item Type",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            CommitmentType.entries.forEach { type ->
              val selected = type == selectedType
              val accent = commitmentAccentColor(type)
              Surface(
                shape = CircleShape,
                color = if (selected) accent else accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accent.copy(alpha = if (selected) 1f else 0.35f)),
                modifier = Modifier
                  .defaultMinSize(minHeight = 40.dp)
                  .clip(CircleShape)
                  .clickable {
                    selectedType = type
                    if (existing == null) {
                      selectedCategory = type.defaultCategory
                    } else if (type == CommitmentType.RECEIVABLE && selectedCategory != Category.ALLOWANCE_INCOME) {
                      selectedCategory = Category.ALLOWANCE_INCOME
                    }
                  }
                  .testTag("commitment_type_${type.name.lowercase()}")
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                  Text(
                    text = type.displayLabel(plainEnglish),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) accent.contrastingContentColor() else MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }

          // 2. Quick Presets for fast creation
          if (existing == null) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              presets.forEach { preset ->
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                  modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 36.dp)
                    .clip(CircleShape)
                    .clickable {
                      titleText = preset.title
                      amountText = preset.amountRupees.toString()
                      dueDayText = preset.dueDay.toString()
                      selectedCategory = preset.category
                      notesText = preset.note
                    }
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                  ) {
                    Text(
                      text = preset.title.substringBefore(" "),
                      style = MaterialTheme.typography.labelSmall,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }
          }

          // 3. Title Input
          OutlinedTextField(
            value = titleText,
            onValueChange = { titleText = it.take(60) },
            label = {
              Text(
                when (selectedType) {
                  CommitmentType.BILL -> "Bill Name (e.g. Electricity, WiFi)"
                  CommitmentType.PAYMENT -> "Payment / EMI Name (e.g. House Rent)"
                  CommitmentType.SUBSCRIPTION -> "Subscription Name (e.g. Netflix, Gym)"
                  CommitmentType.RECEIVABLE -> "Receivable From / Source (e.g. Roommate Split)"
                }
              )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("pakka_bill_name_input")
          )

          // 4. Amount + Inline Calculator
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = amountText,
              onValueChange = { amountText = it.filter { c -> c.isDigit() }.take(9) },
              label = {
                Text(if (selectedType.isIncoming) "Expected Monthly Amount (₹)" else "Monthly Amount (₹)")
              },
              prefix = {
                Text(
                  text = if (selectedType.isIncoming) "+₹ " else "₹ ",
                  style = GharHandStyles.moneyMedium,
                  color = commitmentAccentColor(selectedType)
                )
              },
              textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("pakka_bill_amount_input")
            )

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = PastelSky.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.4f)),
              modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { showCalculator = true }
                .testTag("open_commitment_calc_button")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Calculate,
                  contentDescription = "Calculate amount",
                  tint = PastelSky
                )
              }
            }
          }

          // 5. Due / Expected Day of Month
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (selectedType.isIncoming) "Expected Day of Month (1–31)" else "Due Day of Month (1–31)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              OutlinedTextField(
                value = dueDayText,
                onValueChange = { raw ->
                  val digits = raw.filter { it.isDigit() }.take(2)
                  dueDayText = digits
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .width(84.dp)
                  .testTag("pakka_commitment_due_day_input")
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              listOf(1, 5, 7, 10, 15, 25).forEach { day ->
                val activeDay = dueDayText.toIntOrNull() == day
                Surface(
                  shape = CircleShape,
                  color = if (activeDay) PastelHoney else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                  modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 36.dp)
                    .clip(CircleShape)
                    .clickable { dueDayText = day.toString() }
                ) {
                  Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(
                      text = PakkaCommitment.formatDayOrdinal(day),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (activeDay) PastelHoney.contrastingContentColor() else MaterialTheme.colorScheme.onSurface
                    )
                  }
                }
              }
            }
          }

          // 6. Category Chips
          Text(
            text = "Category",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            availableCategories.forEach { cat ->
              val isSelected = cat == selectedCategory
              Surface(
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                modifier = Modifier
                  .defaultMinSize(minHeight = 36.dp)
                  .clip(CircleShape)
                  .clickable { selectedCategory = cat }
              ) {
                Text(
                  text = "${cat.iconEmoji} ${cat.displayLabel(plainEnglish)}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
              }
            }
          }

          // 7. Optional Notes
          OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it.take(120) },
            label = { Text("Note (optional — e.g. Auto-debit HDFC, UPI split)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("pakka_commitment_notes_input")
          )

          // 8. Active Switch & Optional Immediate Settlement
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Active monthly tracking",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Pause temporarily without deleting this item",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = isActive,
              onCheckedChange = { isActive = it },
              modifier = Modifier.testTag("pakka_commitment_active_switch")
            )
          }

          if (existing == null && isActive) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { markSettledNow = !markSettledNow }
                .padding(vertical = 4.dp)
            ) {
              Checkbox(
                checked = markSettledNow,
                onCheckedChange = { markSettledNow = it }
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (selectedType.isIncoming) {
                  "Also log as received in this month's ledger"
                } else {
                  "Also log as paid in this month's ledger"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 10.dp),
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (existing != null && onDelete != null) {
            OutlinedButton(
              onClick = { showDeleteConfirm = true },
              shape = CircleShape,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
              modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("delete_pakka_commitment_button")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete item",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
          } else {
            TextButton(
              onClick = onDismiss,
              modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
              Text("Cancel")
            }
          }

          Button(
            onClick = {
              val rupees = amountText.toLongOrNull()?.coerceAtLeast(1L) ?: 0L
              if (rupees > 0L) {
                val safeDay = (dueDayText.toIntOrNull() ?: 5).coerceIn(1, 31)
                val cleanTitle = titleText.trim().ifBlank { selectedType.displayLabel(plainEnglish) }
                val saved = PakkaCommitment(
                  id = existing?.id ?: "pakka_${java.util.UUID.randomUUID().toString().take(8)}",
                  title = cleanTitle,
                  amountPaise = rupees * 100L,
                  type = selectedType,
                  category = selectedCategory,
                  dueDayOfMonth = safeDay,
                  notes = notesText.trim(),
                  isActive = isActive,
                  updatedAtMs = System.currentTimeMillis()
                )
                onSave(saved, markSettledNow)
              }
            },
            shape = CircleShape,
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("confirm_pakka_bill_button")
          ) {
            Text(
              text = if (existing == null) "Save Item" else "Save Changes",
              color = MaterialTheme.colorScheme.onPrimary,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }

  if (showCalculator) {
    HisaabCalculatorDialog(
      initialAmountRupees = amountText.toLongOrNull(),
      onDismiss = { showCalculator = false },
      onUseAmount = { calcRupees ->
        amountText = calcRupees.toString()
        showCalculator = false
      }
    )
  }

  if (showDeleteConfirm && existing != null && onDelete != null) {
    GharConfirmDialog(
      title = "Delete \"${existing.displayTitle(plainEnglish)}\"?",
      message = "This removes \"${existing.displayTitle(plainEnglish)}\" (${CurrencyFormatter.formatRupees(existing.amountRupees)}) from your monthly commitments checklist. Past ledger transactions remain untouched.",
      confirmLabel = "Delete",
      onConfirm = {
        showDeleteConfirm = false
        onDelete()
      },
      onDismiss = { showDeleteConfirm = false },
      dialogTestTag = "confirm_delete_commitment_dialog",
      confirmTestTag = "confirm_delete_commitment_button"
    )
  }
}
