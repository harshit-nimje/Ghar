package com.example.core.model

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class Transaction(
  val id: String = UUID.randomUUID().toString(),
  val amountPaise: Long,
  val direction: Direction = Direction.DEBIT,
  val merchant: String,
  val category: Category = Category.OTHER,
  val timestamp: Long = System.currentTimeMillis(),
  val source: CaptureSource = CaptureSource.MANUAL,
  val rawText: String? = null,
  val isRecurring: Boolean = false
) {
  val amountRupees: Long
    get() = amountPaise / 100L

  val isExpense: Boolean
    get() = direction == Direction.DEBIT

  fun isGullakTransfer(): Boolean {
    return category == Category.TRANSFERS && (
      merchant.contains("Gullak", ignoreCase = true) ||
        merchant.contains("Savings Pot", ignoreCase = true)
      )
  }

  fun displayMerchant(plainEnglish: Boolean): String {
    if (!plainEnglish) return merchant
    return when {
      merchant.startsWith("Gullak:", ignoreCase = true) ->
        "Savings Pot:" + merchant.substringAfter(":")
      merchant.equals("Tapri Masala Chai", ignoreCase = true) -> "Morning Coffee"
      merchant.equals("Sharma Tea Corner", ignoreCase = true) -> "Corner Coffee Shop"
      merchant.equals("Tapri Special Chai", ignoreCase = true) -> "Afternoon Tea"
      merchant.equals("Warm Home Tiffin", ignoreCase = true) -> "Daily Lunch"
      merchant.equals("Evening Auto Ride", ignoreCase = true) -> "Evening Ride"
      merchant.equals("Morning Milk & Sabzi", ignoreCase = true) -> "Milk & Groceries"
      merchant.equals("Ghar Shagun Envelope", ignoreCase = true) -> "Bonus Income"
      else -> merchant
    }
  }

  /**
   * Signed contribution of this transaction to Papa's Gullak balance in paise:
   * - DEBIT Gullak transfer (Batua -> Gullak deposit) = +amountPaise
   * - CREDIT Gullak transfer (Gullak -> Batua withdrawal) = -amountPaise
   * - Non-Gullak transaction = 0L
   */
  fun gullakImpactPaise(): Long {
    if (!isGullakTransfer()) return 0L
    return if (direction == Direction.DEBIT) amountPaise else -amountPaise
  }
}

@Immutable
data class BudgetProfile(
  val monthlyIncomePaise: Long = 60_000_00L, // ₹60,000
  val fixedCommitmentsPaise: Long = 24_000_00L, // ₹24,000 rent, bills, emi
  val savingsTargetPaise: Long = 12_000_00L, // ₹12,000 (20%)
  val monthStartDay: Int = 1,
  val guiltSensitivity: GuiltSensitivity = GuiltSensitivity.MEDIUM,
  val habitType: HabitType = HabitType.CHAI_SNACKS,
  val painDial: PainDial = PainDial.BALANCED,
  val ageBand: String = "25-34",
  val lifeStage: String = "Salaried"
) {
  val monthlyIncomeRupees: Long get() = monthlyIncomePaise / 100L
  val fixedCommitmentsRupees: Long get() = fixedCommitmentsPaise / 100L
  val savingsTargetRupees: Long get() = savingsTargetPaise / 100L
  val discretionaryPoolPaise: Long get() = (monthlyIncomePaise - fixedCommitmentsPaise - savingsTargetPaise).coerceAtLeast(0L)
  val discretionaryPoolRupees: Long get() = discretionaryPoolPaise / 100L
}

@Immutable
data class UserHousehold(
  val userId: String = "",
  val userName: String = "",
  val onboardingCompleted: Boolean = false,
  val profile: BudgetProfile = BudgetProfile(),
  val habitLeaks: List<String> = listOf("CHAI_SNACKS")
)

@Immutable
data class SavingsGoal(
  val id: String = "primary_goal",
  val name: String = "Himachal Trip",
  val targetAmountPaise: Long = 40_000_00L, // ₹40,000
  val currentAmountPaise: Long = 0L, // Starts honestly at ₹0 until user drops coins or sets opening balance
  val targetDateEpochMillis: Long? = null,
  val instrumentType: InstrumentType = InstrumentType.SIP
) {
  val targetAmountRupees: Long get() = targetAmountPaise / 100L
  val currentAmountRupees: Long get() = currentAmountPaise / 100L
  val progressPercent: Float
    get() = if (targetAmountPaise > 0) {
      ((currentAmountPaise.toDouble() / targetAmountPaise.toDouble()) * 100).toFloat().coerceIn(0f, 100f)
    } else 0f
}

@Immutable
data class Insight(
  val id: String = UUID.randomUUID().toString(),
  val templateId: String,
  val renderedText: String,
  val actionOffer: String? = null,
  val tone: InsightTone = InsightTone.WARM,
  val priority: InsightPriority = InsightPriority.SAVINGS_OPPORTUNITY,
  val triggeredAt: Long = System.currentTimeMillis(),
  val isRead: Boolean = false,
  val isDismissed: Boolean = false
)

enum class ChatSender {
  USER,
  MAA,
  PAPA
}

@Immutable
data class MaaChatMessage(
  val id: String = UUID.randomUUID().toString(),
  val sender: ChatSender = ChatSender.MAA,
  val text: String = "",
  val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class GroundingSourceChip(
  val title: String,
  val uri: String,
  val domain: String = ""
)

@Immutable
data class PapaChatMessage(
  val id: String = UUID.randomUUID().toString(),
  val sender: ChatSender = ChatSender.PAPA,
  val text: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val sources: List<GroundingSourceChip> = emptyList()
) {
  fun sourcesToJsonString(): String {
    if (sources.isEmpty()) return ""
    val arr = org.json.JSONArray()
    sources.take(4).forEach { chip ->
      arr.put(
        org.json.JSONObject().apply {
          put("title", chip.title.take(120))
          put("uri", chip.uri.take(600))
          put("domain", chip.domain.take(120))
        }
      )
    }
    return arr.toString().take(4000)
  }

  companion object {
    fun parseSourcesJson(raw: String?): List<GroundingSourceChip> {
      if (raw.isNullOrBlank()) return emptyList()
      return runCatching {
        val arr = org.json.JSONArray(raw)
        buildList {
          for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val uri = o.optString("uri").trim()
            if (uri.isEmpty()) continue
            val domain = o.optString("domain").trim()
            val title = o.optString("title").trim().ifBlank { domain.ifBlank { "Web Source" } }
            add(GroundingSourceChip(title = title, uri = uri, domain = domain))
          }
        }
      }.getOrDefault(emptyList())
    }
  }
}

@Immutable
data class QuickChip(
  val id: String = UUID.randomUUID().toString(),
  val label: String,
  val merchant: String,
  val amountPaise: Long,
  val category: Category = Category.CHAI_SNACKS,
  val direction: Direction = Direction.DEBIT,
  val colorKey: StampColorKey = StampColorKey.HONEY,
  val sortOrder: Int = 0
) {
  val amountRupees: Long
    get() = amountPaise / 100L

  val isExpense: Boolean
    get() = direction == Direction.DEBIT

  fun displayLabel(plainEnglish: Boolean): String {
    if (!plainEnglish) return label
    return when {
      id == "chip_chai" || label.equals("Chai", ignoreCase = true) -> "Coffee"
      id == "chip_tiffin" || label.equals("Tiffin", ignoreCase = true) -> "Lunch"
      id == "chip_auto" || label.equals("Auto", ignoreCase = true) -> "Transit"
      id == "chip_sabzi" || label.equals("Sabzi", ignoreCase = true) -> "Groceries"
      id == "chip_shagun" || label.equals("Shagun", ignoreCase = true) -> "Bonus"
      else -> label
    }
  }

  fun displayMerchant(plainEnglish: Boolean): String {
    if (!plainEnglish) return merchant
    return when {
      id == "chip_chai" || merchant.equals("Tapri Masala Chai", ignoreCase = true) -> "Morning Coffee"
      id == "chip_tiffin" || merchant.equals("Warm Home Tiffin", ignoreCase = true) -> "Daily Lunch"
      id == "chip_auto" || merchant.equals("Evening Auto Ride", ignoreCase = true) -> "Evening Transit"
      id == "chip_sabzi" || merchant.equals("Morning Milk & Sabzi", ignoreCase = true) -> "Milk & Groceries"
      id == "chip_shagun" || merchant.equals("Ghar Shagun Envelope", ignoreCase = true) -> "Bonus Income"
      else -> merchant
    }
  }

  companion object {
    fun defaultChips(plainEnglish: Boolean = false): List<QuickChip> = if (plainEnglish) {
      listOf(
        QuickChip(
          id = "chip_chai",
          label = "Coffee",
          merchant = "Morning Coffee",
          amountPaise = 25_00L,
          category = Category.CHAI_SNACKS,
          direction = Direction.DEBIT,
          colorKey = StampColorKey.HONEY,
          sortOrder = 0
        ),
        QuickChip(
          id = "chip_tiffin",
          label = "Lunch",
          merchant = "Daily Lunch",
          amountPaise = 320_00L,
          category = Category.FOOD_DELIVERY,
          direction = Direction.DEBIT,
          colorKey = StampColorKey.TERRACOTTA,
          sortOrder = 1
        ),
        QuickChip(
          id = "chip_auto",
          label = "Transit",
          merchant = "Evening Transit",
          amountPaise = 180_00L,
          category = Category.COMMUTE,
          direction = Direction.DEBIT,
          colorKey = StampColorKey.SKY,
          sortOrder = 2
        ),
        QuickChip(
          id = "chip_sabzi",
          label = "Groceries",
          merchant = "Milk & Groceries",
          amountPaise = 90_00L,
          category = Category.GROCERIES,
          direction = Direction.DEBIT,
          colorKey = StampColorKey.SAGE,
          sortOrder = 3
        ),
        QuickChip(
          id = "chip_shagun",
          label = "Bonus",
          merchant = "Bonus Income",
          amountPaise = 1000_00L,
          category = Category.ALLOWANCE_INCOME,
          direction = Direction.CREDIT,
          colorKey = StampColorKey.PLUM,
          sortOrder = 4
        )
      )
    } else {
      listOf(
        QuickChip(
          id = "chip_chai",
          label = "Chai",
          merchant = "Tapri Masala Chai",
          amountPaise = 25_00L,
          category = Category.CHAI_SNACKS,
          direction = Direction.DEBIT,
          colorKey = StampColorKey.HONEY,
          sortOrder = 0
        ),
        QuickChip(
          id = "chip_tiffin",
          label = "Tiffin",
          merchant = "Warm Home Tiffin",
          amountPaise = 320_00L,
          category = Category.FOOD_DELIVERY,
          direction = Direction.DEBIT,
          colorKey = StampColorKey.TERRACOTTA,
          sortOrder = 1
        ),
        QuickChip(
          id = "chip_auto",
          label = "Auto",
          merchant = "Evening Auto Ride",
          amountPaise = 180_00L,
          category = Category.COMMUTE,
          direction = Direction.DEBIT,
          colorKey = StampColorKey.SKY,
          sortOrder = 2
        ),
        QuickChip(
          id = "chip_sabzi",
          label = "Sabzi",
          merchant = "Morning Milk & Sabzi",
          amountPaise = 90_00L,
          category = Category.GROCERIES,
          direction = Direction.DEBIT,
          colorKey = StampColorKey.SAGE,
          sortOrder = 3
        ),
        QuickChip(
          id = "chip_shagun",
          label = "Shagun",
          merchant = "Ghar Shagun Envelope",
          amountPaise = 1000_00L,
          category = Category.ALLOWANCE_INCOME,
          direction = Direction.CREDIT,
          colorKey = StampColorKey.PLUM,
          sortOrder = 4
        )
      )
    }
  }
}

@Immutable
data class ChitthiNote(
  val id: String = UUID.randomUUID().toString(),
  val author: ChitthiAuthor = ChitthiAuthor.MAA,
  val title: String,
  val body: String,
  val tag: String = "Rasoi Wisdom",
  val savedAt: Long = System.currentTimeMillis()
)

@Immutable
data class PakkaCommitment(
  val id: String = "pakka_${UUID.randomUUID().toString().take(8)}",
  val title: String,
  val amountPaise: Long,
  val type: CommitmentType = CommitmentType.BILL,
  val category: Category = type.defaultCategory,
  val dueDayOfMonth: Int = 5,
  val notes: String = "",
  val isActive: Boolean = true,
  val updatedAtMs: Long = System.currentTimeMillis()
) {
  val amountRupees: Long
    get() = amountPaise / 100L

  val isReceivable: Boolean
    get() = type == CommitmentType.RECEIVABLE

  val direction: Direction
    get() = if (isReceivable) Direction.CREDIT else Direction.DEBIT

  val safeDueDay: Int
    get() = dueDayOfMonth.coerceIn(1, 31)

  val dueDayOrdinal: String
    get() = formatDayOrdinal(safeDueDay)

  fun ledgerMarkerTag(): String = "[pakka:$id]"

  fun displayTitle(plainEnglish: Boolean): String {
    if (!plainEnglish) return title
    return when {
      title.equals("Ghar Kiraya / Rent", ignoreCase = true) -> "Monthly Rent"
      title.equals("Bijli & WiFi Bill", ignoreCase = true) -> "Electricity & Internet"
      else -> title
    }
  }

  companion object {
    fun formatDayOrdinal(day: Int): String {
      val d = day.coerceIn(1, 31)
      val suffix = if (d in 11..13) {
        "th"
      } else {
        when (d % 10) {
          1 -> "st"
          2 -> "nd"
          3 -> "rd"
          else -> "th"
        }
      }
      return "$d$suffix"
    }

    fun defaultCommitments(): List<PakkaCommitment> = listOf(
      PakkaCommitment(
        id = "pakka_rent",
        title = "House Rent",
        amountPaise = 8_000_00L,
        type = CommitmentType.PAYMENT,
        category = Category.BILLS_UTILITIES,
        dueDayOfMonth = 5,
        notes = "Monthly housing payment",
        isActive = true
      ),
      PakkaCommitment(
        id = "pakka_utilities",
        title = "Internet & Electricity",
        amountPaise = 1_200_00L,
        type = CommitmentType.BILL,
        category = Category.BILLS_UTILITIES,
        dueDayOfMonth = 10,
        notes = "Broadband + monthly power bill",
        isActive = true
      ),
      PakkaCommitment(
        id = "pakka_streaming",
        title = "Streaming & Music Plan",
        amountPaise = 299_00L,
        type = CommitmentType.SUBSCRIPTION,
        category = Category.ENTERTAINMENT,
        dueDayOfMonth = 15,
        notes = "Auto-renewing monthly subscription",
        isActive = true
      ),
      PakkaCommitment(
        id = "pakka_split_in",
        title = "Shared Bill Split Receivable",
        amountPaise = 1_500_00L,
        type = CommitmentType.RECEIVABLE,
        category = Category.ALLOWANCE_INCOME,
        dueDayOfMonth = 7,
        notes = "Expected reimbursement / incoming split",
        isActive = true
      )
    )
  }
}

