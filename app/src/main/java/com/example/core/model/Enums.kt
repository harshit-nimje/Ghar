package com.example.core.model

enum class Direction {
  DEBIT,
  CREDIT
}

enum class CaptureSource {
  NOTIFICATION,
  SMS,
  MANUAL,
  SEED
}

enum class Category(
  val displayName: String,
  val iconEmoji: String,
  val isEssential: Boolean
) {
  CHAI_SNACKS("Chai & Coffee", "☕", false),
  FOOD_DELIVERY("Food Delivery", "🛵", false),
  COMMUTE("Cabs & Travel", "🚕", false),
  SHOPPING("Shopping & Impulse", "🛍️", false),
  GROCERIES("Groceries & Staples", "🛒", true),
  BILLS_UTILITIES("Bills & EMIs", "⚡", true),
  ENTERTAINMENT("Subscriptions & Media", "🎬", false),
  HEALTH_MEDICAL("Health & Medical", "💊", true),
  TRANSFERS("Transfers & Savings", "💸", false),
  ALLOWANCE_INCOME("Allowance & Income", "💰", false),
  OTHER("General & Miscellaneous", "📦", false);

  fun displayLabel(plainEnglish: Boolean): String {
    if (!plainEnglish) return displayName
    return when (this) {
      CHAI_SNACKS -> "Coffee & Snacks"
      BILLS_UTILITIES -> "Bills & Loans"
      else -> displayName
    }
  }

  companion object {
    fun fromName(name: String?): Category {
      return entries.find { it.name.equals(name, ignoreCase = true) } ?: OTHER
    }
  }
}

enum class GuiltSensitivity(
  val label: String,
  val description: String
) {
  LOW("Firm & Direct", "Keep me honest with sharper budget checkpoints."),
  MEDIUM("Balanced & Calm", "A quiet watchful presence. Speaks only when it matters."),
  HIGH("Gentle & Patient", "No guilt, looser thresholds, plenty of warmth."),
  NONE("None / Off", "Do not nudge or alert me on overspending.");

  companion object {
    fun fromName(name: String?): GuiltSensitivity {
      return entries.find { it.name.equals(name, ignoreCase = true) } ?: MEDIUM
    }
  }
}

enum class HabitType(
  val label: String,
  val emoji: String,
  val category: Category
) {
  CHAI_SNACKS("Chai, coffee & stall snacks", "☕", Category.CHAI_SNACKS),
  FOOD_DELIVERY("Food delivery (Swiggy / Zomato)", "🛵", Category.FOOD_DELIVERY),
  RIDES_COMMUTE("Daily cabs & auto rides (Uber / Ola)", "🚕", Category.COMMUTE),
  IMPULSE_SHOPPING("Quick eCommerce checkout sprees", "🛍️", Category.SHOPPING),
  SUBSCRIPTIONS("Quiet streaming & app renewals", "📱", Category.ENTERTAINMENT),
  NIGHTS_OUT("Weekend dinners & social outings", "🎉", Category.FOOD_DELIVERY);

  fun displayLabel(plainEnglish: Boolean): String {
    if (!plainEnglish) return label
    return when (this) {
      CHAI_SNACKS -> "Coffee, tea & daily snacks"
      FOOD_DELIVERY -> "Food delivery & takeout apps"
      RIDES_COMMUTE -> "Daily rideshares & cab trips"
      IMPULSE_SHOPPING -> "Quick online shopping sprees"
      SUBSCRIPTIONS -> "Recurring streaming & app renewals"
      NIGHTS_OUT -> "Weekend dinners & social outings"
    }
  }

  companion object {
    fun fromName(name: String?): HabitType {
      return entries.find { it.name.equals(name, ignoreCase = true) } ?: CHAI_SNACKS
    }
  }
}

enum class PainDial(
  val label: String,
  val description: String
) {
  SUBTLE("Subtle (No Flying Cash)", "No notes flying out at all. Quiet and calm."),
  BALANCED("Balanced (Flying Cash)", "Balanced cash notes flying out when logging spends."),
  PUNCHY("Punchy (Cash + Vibration)", "Energetic cash notes flying out with haptic vibration motors cranking.");

  companion object {
    fun fromName(name: String?): PainDial {
      return entries.find { it.name.equals(name, ignoreCase = true) } ?: BALANCED
    }
  }
}

enum class InsightTone {
  WARM,
  GENTLE,
  INFORMATIVE,
  PRACTICAL,
  REASSURING,
  QUESTION,
  OFFER,
  PRAISE,
  WELCOME,
  SILENCE
}

enum class InsightPriority {
  SAFETY,
  SAVINGS_OPPORTUNITY,
  PRAISE,
  STREAK
}

enum class InstrumentType(
  val displayName: String,
  val typicalAnnualReturnPercent: Double,
  val description: String
) {
  SIP("Equity Index Fund (SIP)", 12.0, "Long-term market index compounding with monthly contributions."),
  RD("Bank Recurring Deposit (RD)", 7.1, "Fixed interest rate with bank deposit safety."),
  PPF("Public Provident Fund (PPF)", 7.1, "Government-backed long-term tax-exempt compounding."),
  NONE("Liquid Savings Pot (Cash)", 3.5, "Liquid cash savings accessible anytime.");

  companion object {
    fun fromName(name: String?): InstrumentType {
      return entries.find { it.name.equals(name, ignoreCase = true) } ?: SIP
    }
  }
}

enum class StampColorKey(
  val label: String
) {
  HONEY("Warm Honey"),
  TERRACOTTA("Terracotta"),
  SKY("Clear Sky"),
  SAGE("Mint Sage"),
  PLUM("Dusk Plum");

  companion object {
    fun fromName(name: String?): StampColorKey {
      return entries.find { it.name.equals(name, ignoreCase = true) } ?: HONEY
    }
  }
}

enum class MaaAiEngineMode(
  val shortLabel: String,
  val subtitle: String
) {
  CLOUD_AUTO(
    shortLabel = "Cloud AI",
    subtitle = "Gemini 3.8 Flash + 3.5 Lite backup"
  ),
  MINI_LOCAL(
    shortLabel = "Family Local 0.5B",
    subtitle = "Shared 0.5B Q4 LLM (492 MB) • Powers both Maa & Papa offline"
  );

  fun displayShortLabel(plainEnglish: Boolean): String = when (this) {
    CLOUD_AUTO -> "Cloud AI"
    MINI_LOCAL -> if (plainEnglish) "Local 0.5B LLM" else "Family Local 0.5B"
  }

  companion object {
    fun fromName(name: String?): MaaAiEngineMode {
      return entries.find { it.name.equals(name, ignoreCase = true) } ?: CLOUD_AUTO
    }
  }
}

enum class ChitthiAuthor(
  val displayName: String
) {
  MAA("Maa"),
  PAPA("Papa");

  fun displayLabel(plainEnglish: Boolean): String = when (this) {
    MAA -> if (plainEnglish) "Mom" else "Maa"
    PAPA -> if (plainEnglish) "Dad" else "Papa"
  }

  companion object {
    fun fromName(name: String?): ChitthiAuthor {
      if (name.equals("PAPA", ignoreCase = true) || name.equals("BHAI", ignoreCase = true)) {
        return PAPA
      }
      return entries.find { it.name.equals(name, ignoreCase = true) } ?: MAA
    }
  }
}

enum class CommitmentType(
  val displayName: String,
  val shortBadge: String,
  val defaultCategory: Category,
  val isIncoming: Boolean
) {
  BILL(
    displayName = "Monthly Bill",
    shortBadge = "Bill",
    defaultCategory = Category.BILLS_UTILITIES,
    isIncoming = false
  ),
  PAYMENT(
    displayName = "Fixed Payment / EMI",
    shortBadge = "Payment",
    defaultCategory = Category.BILLS_UTILITIES,
    isIncoming = false
  ),
  SUBSCRIPTION(
    displayName = "Subscription",
    shortBadge = "Sub",
    defaultCategory = Category.ENTERTAINMENT,
    isIncoming = false
  ),
  RECEIVABLE(
    displayName = "Receivable / Expected In",
    shortBadge = "Receivable",
    defaultCategory = Category.ALLOWANCE_INCOME,
    isIncoming = true
  );

  fun displayLabel(plainEnglish: Boolean): String = when (this) {
    BILL -> "Monthly Bill"
    PAYMENT -> if (plainEnglish) "Fixed Payment / Loan" else "Payment / EMI"
    SUBSCRIPTION -> "Subscription"
    RECEIVABLE -> "Receivable"
  }

  companion object {
    fun fromName(name: String?): CommitmentType {
      return entries.find { it.name.equals(name, ignoreCase = true) } ?: BILL
    }
  }
}



