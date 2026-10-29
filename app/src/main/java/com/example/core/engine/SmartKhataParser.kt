package com.example.core.engine

import androidx.compose.runtime.Immutable
import com.example.core.model.BudgetProfile
import com.example.core.model.CaptureSource
import com.example.core.model.Category
import com.example.core.model.CommitmentType
import com.example.core.model.Direction
import com.example.core.model.PakkaCommitment
import com.example.core.model.SavingsGoal
import com.example.core.model.Transaction
import com.example.core.util.CurrencyFormatter
import com.example.core.util.DateUtils
import java.util.Calendar
import kotlin.math.pow
import kotlin.math.roundToLong

enum class QuickJotKind {
  EXPENSE,
  INCOME,
  GULLAK_DROP
}

/**
 * Instant (<0.1ms) on-device natural-language ledger parser for both Indian & Plain English phrases.
 * Examples:
 * - "coffee 40" or "chai 30" -> Expense • Coffee & Snacks
 * - "groceries 420" or "blinkit 420" -> Expense • Groceries & Staples
 * - "cab to metro 140" -> Expense • Cabs & Travel
 * - "salary +25000" -> Income • Allowance & Income
 * - "savings 500" or "gullak 500" -> Savings Pot Deposit
 * - "house rent 15000" -> Recurring Expense • House Rent • Bills & Loans
 */
object SmartKhataParser {

  @Immutable
  data class ParsedKhataDraft(
    val amountRupees: Long,
    val merchant: String,
    val category: Category,
    val kind: QuickJotKind,
    val isRecurring: Boolean = false
  ) {
    val direction: Direction
      get() = if (kind == QuickJotKind.INCOME) Direction.CREDIT else Direction.DEBIT

    val isExpense: Boolean
      get() = direction == Direction.DEBIT

    val isGullakDrop: Boolean
      get() = kind == QuickJotKind.GULLAK_DROP

    val isGullakDeposit: Boolean
      get() = kind == QuickJotKind.GULLAK_DROP
  }

  private val numberRegex = Regex("""([+]?\d[\d,]*(?:\.\d{1,2})?)\s*(k)?""", RegexOption.IGNORE_CASE)

  private val incomeKeywords = setOf(
    "salary", "stipend", "income", "received", "refund", "cashback",
    "freelance", "pocketmoney", "allowance", "shagun", "bonus", "credit", "paycheck"
  )

  private val gullakKeywords = setOf(
    "gullak", "piggy", "save", "saved", "sip", "savings", "pot"
  )

  private val recurringKeywords = setOf(
    "rent", "wifi", "broadband", "electricity", "bijli", "emi", "hostel", "mess", "maid", "maintenance", "internet", "mortgage"
  )

  fun parse(
    rawInput: String,
    activeGoalName: String = "Savings",
    plainEnglish: Boolean = false
  ): ParsedKhataDraft? {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) return null

    val match = numberRegex.findAll(trimmed)
      .mapNotNull { m ->
        val rawDigits = m.groupValues[1].replace(",", "").removePrefix("+")
        val base = rawDigits.toDoubleOrNull() ?: return@mapNotNull null
        val mult = if (m.groupValues[2].equals("k", ignoreCase = true)) 1000.0 else 1.0
        val rupees = (base * mult).roundToLong()
        if (rupees > 0L) Triple(m.range, rupees, m.value.trim().startsWith("+")) else null
      }
      .lastOrNull() ?: return null

    val (numRange, amountRupees, hasPlusPrefix) = match
    val strippedText = (trimmed.substring(0, numRange.first) + " " + trimmed.substring(numRange.last + 1))
      .replace(Regex("""\b(rs\.?|inr|rupees?|for|on|at|paid|spent|in|into|to)\b""", RegexOption.IGNORE_CASE), " ")
      .replace("₹", " ")
      .replace(Regex("""\s+"""), " ")
      .trim()

    val lower = trimmed.lowercase()
    val tokens = lower.split(Regex("""[^a-z0-9]+""")).filter { it.isNotBlank() }

    val isGullak = tokens.any { it in gullakKeywords }
    if (isGullak) {
      val prefix = if (plainEnglish) "Savings Pot" else "Gullak"
      return ParsedKhataDraft(
        amountRupees = amountRupees,
        merchant = "$prefix: $activeGoalName",
        category = Category.TRANSFERS,
        kind = QuickJotKind.GULLAK_DROP,
        isRecurring = false
      )
    }

    val isIncome = hasPlusPrefix || tokens.any { it in incomeKeywords }
    val inferredCategory = if (isIncome) {
      Category.ALLOWANCE_INCOME
    } else {
      inferCategory(lower, tokens)
    }

    val recurringDetected = !isIncome && tokens.any { it in recurringKeywords }

    val cleanMerchant = if (strippedText.isNotBlank()) {
      strippedText.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
      }.take(60)
    } else {
      when {
        isIncome -> "Income"
        else -> inferredCategory.displayLabel(plainEnglish)
      }
    }

    return ParsedKhataDraft(
      amountRupees = amountRupees,
      merchant = cleanMerchant,
      category = inferredCategory,
      kind = if (isIncome) QuickJotKind.INCOME else QuickJotKind.EXPENSE,
      isRecurring = recurringDetected
    )
  }

  private fun inferCategory(lower: String, tokens: List<String>): Category {
    return when {
      tokens.any { it in setOf("chai", "tea", "coffee", "latte", "espresso", "samosa", "momos", "snack", "snacks", "maggi", "bun", "tapri", "bakery", "biscuit", "coldcoffee") } ->
        Category.CHAI_SNACKS
      tokens.any { it in setOf("swiggy", "zomato", "ubereats", "doordash", "pizza", "biryani", "burger", "tiffin", "dinner", "lunch", "breakfast", "thali", "cafe", "restaurant", "roll", "dosa", "takeout") } ->
        Category.FOOD_DELIVERY
      tokens.any { it in setOf("auto", "uber", "ola", "lyft", "rapido", "metro", "subway", "bus", "cab", "taxi", "petrol", "fuel", "gasoline", "train", "rickshaw", "ticket", "toll", "transit") } ->
        Category.COMMUTE
      tokens.any { it in setOf("blinkit", "zepto", "instamart", "bigbasket", "sabzi", "milk", "dahi", "paneer", "atta", "fruits", "grocery", "groceries", "kirana", "eggs", "bread", "supermarket") } ->
        Category.GROCERIES
      tokens.any { it in setOf("rent", "wifi", "internet", "broadband", "electricity", "bijli", "recharge", "jio", "airtel", "bill", "water", "gas", "cylinder", "maid", "emi", "loan", "hostel", "mess", "maintenance") } ->
        Category.BILLS_UTILITIES
      tokens.any { it in setOf("amazon", "flipkart", "myntra", "ajio", "nykaa", "shoes", "shirt", "kurta", "clothes", "shopping", "earbuds", "gift") } ->
        Category.SHOPPING
      tokens.any { it in setOf("movie", "netflix", "spotify", "youtube", "prime", "game", "concert", "outing", "bowling") } ->
        Category.ENTERTAINMENT
      tokens.any { it in setOf("medicine", "pharmacy", "apollo", "doctor", "clinic", "tablet", "gym", "protein", "health") } ->
        Category.HEALTH_MEDICAL
      lower.contains("upi") || tokens.any { it in setOf("transfer", "sent", "friend", "split") } ->
        Category.TRANSFERS
      else -> Category.OTHER
    }
  }
}

/**
 * Pure, fast (<0.5ms) household analytics for Home/Kitchen, Insights/Courtyard, Plan/Study & Settings/Balcony.
 */
object HouseholdAnalyticsEngine {

  private const val DAY_MS = 24L * 3600_000L

  @Immutable
  data class DailySpendBar(
    val dayStartMs: Long,
    val dayLabel: String,
    val spentRupees: Long,
    val isToday: Boolean,
    val isOverSafe: Boolean
  )

  @Immutable
  data class RecurringBillTemplate(
    val key: String,
    val title: String,
    val defaultRupees: Long,
    val category: Category,
    val keywords: List<String>
  )

  @Immutable
  data class RecurringBillStatus(
    val key: String,
    val merchant: String,
    val amountRupees: Long,
    val category: Category,
    val paidThisCycle: Boolean,
    val paidTxThisMonth: Transaction? = null
  )

  @Immutable
  data class EvaluatedCommitment(
    val commitment: PakkaCommitment,
    val settledThisCycle: Boolean,
    val matchedTransaction: Transaction? = null,
    val isDueSoonOrOverdue: Boolean = false
  ) {
    val effectiveAmountRupees: Long
      get() = matchedTransaction?.amountRupees ?: commitment.amountRupees
  }

  @Immutable
  data class CommitmentsCycleSummary(
    val items: List<EvaluatedCommitment>,
    val activeOutflowsCount: Int,
    val settledOutflowsCount: Int,
    val pendingOutflowRupees: Long,
    val activeReceivablesCount: Int,
    val settledReceivablesCount: Int,
    val pendingReceivableRupees: Long
  ) {
    val totalPendingCount: Int
      get() = (activeOutflowsCount - settledOutflowsCount).coerceAtLeast(0) +
        (activeReceivablesCount - settledReceivablesCount).coerceAtLeast(0)
  }

  @Immutable
  data class CompoundingProjection(
    val totalPrincipalRupees: Long,
    val wealthGainRupees: Long,
    val maturityValueRupees: Long,
    val annualRatePercent: Double
  )

  private val defaultRecurringTemplates = listOf(
    RecurringBillTemplate("rent", "Rent / Housing", 8_000L, Category.BILLS_UTILITIES, listOf("rent", "hostel", "pg", "flat", "housing")),
    RecurringBillTemplate("wifi", "Internet & Electricity", 1_200L, Category.BILLS_UTILITIES, listOf("wifi", "internet", "broadband", "electricity", "bijli", "bescom")),
    RecurringBillTemplate("mobile", "Phone Plan", 349L, Category.BILLS_UTILITIES, listOf("recharge", "jio", "airtel", "vi", "mobile", "phone"))
  )

  fun calculateTodaySpendRupees(
    transactions: List<Transaction>,
    nowMillis: Long = System.currentTimeMillis()
  ): Long {
    val todayStartMs = DateUtils.startOfTodayMillis(nowMillis)
    var paise = 0L
    for (tx in transactions) {
      if (tx.timestamp >= todayStartMs && tx.isExpense && !tx.isGullakTransfer()) {
        paise += tx.amountPaise
      }
    }
    return paise / 100L
  }

  /**
   * Computes consecutive recent days where non-savings spending stayed within 1.15x daily allowance.
   */
  fun calculateQuietStreakDays(
    transactions: List<Transaction>,
    dailySafeRupees: Long,
    nowMillis: Long = System.currentTimeMillis()
  ): Int {
    val todayStartMs = DateUtils.startOfTodayMillis(nowMillis)
    val safeThresholdPaise = dailySafeRupees.coerceAtLeast(150L) * 115L // 1.15x in paise
    val dayBuckets = LongArray(14)
    val oldestMs = todayStartMs - 13L * DAY_MS
    var hasAnyManualActivity = false

    for (tx in transactions) {
      if (tx.source != CaptureSource.SEED) hasAnyManualActivity = true
      if (!tx.isExpense || tx.isGullakTransfer()) continue
      if (tx.timestamp < oldestMs) continue
      val daysAgo = ((todayStartMs + DAY_MS - 1L - tx.timestamp) / DAY_MS).toInt()
      if (daysAgo in 0..13) {
        dayBuckets[daysAgo] += tx.amountPaise
      }
    }

    var streak = 0
    for (daysAgo in 0..13) {
      if (dayBuckets[daysAgo] <= safeThresholdPaise) {
        streak++
      } else {
        break
      }
    }
    return if (!hasAnyManualActivity && transactions.all { it.source == CaptureSource.SEED }) {
      streak.coerceIn(1, 5)
    } else {
      streak.coerceAtLeast(1)
    }
  }

  fun buildSevenDayRhythm(
    transactions: List<Transaction>,
    dailySafeRupees: Long = 500L,
    nowMillis: Long = System.currentTimeMillis()
  ): List<DailySpendBar> {
    val todayStartMs = DateUtils.startOfTodayMillis(nowMillis)
    val safeCap = dailySafeRupees.coerceAtLeast(100L)
    val buckets = LongArray(7)
    val startWindow = todayStartMs - 6L * DAY_MS

    for (tx in transactions) {
      if (!tx.isExpense || tx.isGullakTransfer() || tx.timestamp < startWindow) continue
      val dayIdx = ((tx.timestamp - startWindow) / DAY_MS).toInt().coerceIn(0, 6)
      buckets[dayIdx] += tx.amountPaise
    }

    return (0..6).map { idx ->
      val dayMs = startWindow + idx * DAY_MS
      val spent = buckets[idx] / 100L
      DailySpendBar(
        dayStartMs = dayMs,
        dayLabel = if (idx == 6) "Today" else DateUtils.formatShortDayLabel(dayMs),
        spentRupees = spent,
        isToday = idx == 6,
        isOverSafe = spent > safeCap
      )
    }
  }

  /**
   * Evaluates recurring monthly bills across the ledger.
   */
  fun evaluateRecurringBills(
    transactions: List<Transaction>,
    monthStartDay: Int = 1,
    nowMillis: Long = System.currentTimeMillis()
  ): List<RecurringBillStatus> {
    val cycleStartMs = DateUtils.startOfBudgetCycleMillis(monthStartDay, nowMillis)
    val recurringTxs = transactions.filter { it.isExpense && it.isRecurring }

    if (recurringTxs.isNotEmpty()) {
      val grouped = recurringTxs.groupBy { it.merchant.trim().lowercase() }
      return grouped.entries.map { (normKey, group) ->
        val latest = group.maxByOrNull { it.timestamp }!!
        val paidInCycle = group.firstOrNull { it.timestamp >= cycleStartMs }
        RecurringBillStatus(
          key = normKey,
          merchant = latest.merchant,
          amountRupees = (paidInCycle ?: latest).amountRupees.coerceAtLeast(1L),
          category = latest.category,
          paidThisCycle = paidInCycle != null,
          paidTxThisMonth = paidInCycle
        )
      }
    }

    val cycleDebits = transactions.filter { it.isExpense && it.timestamp >= cycleStartMs }
    return defaultRecurringTemplates.map { tpl ->
      val match = cycleDebits.firstOrNull { tx ->
        val lower = tx.merchant.lowercase()
        (tx.category == Category.BILLS_UTILITIES || tx.isRecurring) &&
          tpl.keywords.any { kw -> lower.contains(kw) }
      }
      RecurringBillStatus(
        key = tpl.key,
        merchant = match?.merchant ?: tpl.title,
        amountRupees = match?.amountRupees ?: tpl.defaultRupees,
        category = tpl.category,
        paidThisCycle = match != null,
        paidTxThisMonth = match
      )
    }
  }

  /**
   * Evaluates customizable Pakke Monthly Bills, Payments, Subscriptions, and Receivables
   * against transactions recorded in the current budget cycle.
   */
  fun evaluateCommitments(
    commitments: List<PakkaCommitment>,
    transactions: List<Transaction>,
    monthStartDay: Int = 1,
    nowMillis: Long = System.currentTimeMillis()
  ): CommitmentsCycleSummary {
    val cycleStartMs = DateUtils.startOfBudgetCycleMillis(monthStartDay, nowMillis)
    val cycleTxs = transactions.filter { it.timestamp >= cycleStartMs && !it.isGullakTransfer() }
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val currentDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

    val usedTxIds = HashSet<String>()
    val evaluated = commitments.map { item ->
      val marker = item.ledgerMarkerTag()
      val normTitle = item.title.trim().lowercase()
      val matched = cycleTxs.firstOrNull { tx ->
        tx.id !in usedTxIds &&
          tx.direction == item.direction &&
          (
            tx.rawText?.contains(marker) == true ||
              tx.merchant.trim().equals(normTitle, ignoreCase = true) ||
              (normTitle.length >= 4 && tx.merchant.lowercase().contains(normTitle))
            )
      }
      if (matched != null) {
        usedTxIds.add(matched.id)
      }
      val dueDelta = item.safeDueDay - currentDayOfMonth
      val dueSoonOrOverdue = item.isActive && matched == null && (dueDelta <= 3)
      EvaluatedCommitment(
        commitment = item,
        settledThisCycle = matched != null,
        matchedTransaction = matched,
        isDueSoonOrOverdue = dueSoonOrOverdue
      )
    }

    val activeOutflows = evaluated.filter { it.commitment.isActive && !it.commitment.isReceivable }
    val activeReceivables = evaluated.filter { it.commitment.isActive && it.commitment.isReceivable }

    return CommitmentsCycleSummary(
      items = evaluated,
      activeOutflowsCount = activeOutflows.size,
      settledOutflowsCount = activeOutflows.count { it.settledThisCycle },
      pendingOutflowRupees = activeOutflows.filterNot { it.settledThisCycle }.sumOf { it.commitment.amountRupees },
      activeReceivablesCount = activeReceivables.size,
      settledReceivablesCount = activeReceivables.count { it.settledThisCycle },
      pendingReceivableRupees = activeReceivables.filterNot { it.settledThisCycle }.sumOf { it.commitment.amountRupees }
    )
  }

  fun buildCommitmentSettlementTransaction(item: PakkaCommitment, nowMillis: Long = System.currentTimeMillis()): Transaction {
    val cleanNote = buildString {
      append(item.ledgerMarkerTag())
      if (item.notes.isNotBlank()) {
        append(" ")
        append(item.notes.trim())
      }
    }.take(500)
    return Transaction(
      amountPaise = item.amountPaise.coerceAtLeast(100L),
      direction = item.direction,
      merchant = item.title.trim().ifBlank { item.type.displayName }.take(120),
      category = item.category,
      timestamp = nowMillis,
      source = CaptureSource.MANUAL,
      rawText = cleanNote,
      isRecurring = true
    )
  }

  fun projectCompounding(
    initialRupees: Long,
    monthlyContributionRupees: Long,
    annualReturnPercent: Double,
    years: Int
  ): CompoundingProjection {
    val safeRate = (annualReturnPercent / 100.0).coerceAtLeast(0.0)
    val months = years.coerceIn(1, 30) * 12
    val safeInitial = initialRupees.coerceAtLeast(0L)
    val safeMonthly = monthlyContributionRupees.coerceAtLeast(0L)
    val principal = safeInitial + safeMonthly * months

    if (safeRate <= 0.0001) {
      return CompoundingProjection(
        totalPrincipalRupees = principal,
        wealthGainRupees = 0L,
        maturityValueRupees = principal,
        annualRatePercent = 0.0
      )
    }

    val monthlyRate = safeRate / 12.0
    val lumpFuture = safeInitial.toDouble() * (1.0 + monthlyRate).pow(months.toDouble())
    val sipFuture = safeMonthly.toDouble() *
      (((1.0 + monthlyRate).pow(months.toDouble()) - 1.0) / monthlyRate) *
      (1.0 + monthlyRate)

    val maturity = (lumpFuture + sipFuture).roundToLong().coerceAtLeast(principal)
    return CompoundingProjection(
      totalPrincipalRupees = principal,
      wealthGainRupees = (maturity - principal).coerceAtLeast(0L),
      maturityValueRupees = maturity,
      annualRatePercent = annualReturnPercent
    )
  }

  fun buildShareableMonthlySummary(
    userName: String,
    profile: BudgetProfile,
    safeToSpend: SafeToSpendResult,
    transactions: List<Transaction>,
    goals: List<SavingsGoal> = emptyList(),
    goal: SavingsGoal? = null,
    plainEnglish: Boolean = false,
    nowMillis: Long = System.currentTimeMillis()
  ): String {
    val resolvedGoals = when {
      goals.isNotEmpty() -> goals
      goal != null -> listOf(goal)
      else -> emptyList()
    }
    val monthStart = DateUtils.startOfBudgetCycleMillis(profile.monthStartDay, nowMillis)
    val monthTxs = transactions.filter { it.timestamp >= monthStart }
    val monthSpendsRupees = monthTxs
      .filter { it.isExpense && !it.isGullakTransfer() }
      .sumOf { it.amountPaise } / 100L
    val monthGullakDropsRupees = monthTxs
      .filter { it.isGullakTransfer() && it.isExpense }
      .sumOf { it.amountPaise } / 100L
    val totalGullakRupees = resolvedGoals.sumOf { it.currentAmountRupees }

    val reportTitle = if (plainEnglish) {
      "Monthly Budget Summary — ${DateUtils.formatMonthYear(nowMillis)}"
    } else {
      "Ghar Monthly Summary — ${DateUtils.formatMonthYear(nowMillis)}"
    }
    val savingsLabel = if (plainEnglish) "Saved This Cycle" else "Saved in Gullak This Cycle"
    val goalsTotalLabel = if (plainEnglish) "Total Across Savings Goals" else "Total Across Gullaks"
    val ledgerLabel = if (plainEnglish) "Recent Transactions:" else "Recent Khata Entries:"

    val sb = StringBuilder()
    sb.appendLine("📊 $reportTitle")
    sb.appendLine("Prepared for: ${userName.ifBlank { "Friend" }}")
    sb.appendLine("────────────────────────────")
    sb.appendLine("• Monthly Income: ${CurrencyFormatter.formatRupees(profile.monthlyIncomeRupees)}")
    sb.appendLine("• Fixed Bills: ${CurrencyFormatter.formatRupees(profile.fixedCommitmentsRupees)}")
    sb.appendLine("• Spent This Cycle: ${CurrencyFormatter.formatRupees(monthSpendsRupees)}")
    sb.appendLine("• $savingsLabel: ${CurrencyFormatter.formatRupees(monthGullakDropsRupees)}")
    sb.appendLine("• Safe to Spend Today: ${CurrencyFormatter.formatRupees(safeToSpend.dailyAllowanceRupees)}/day (${safeToSpend.daysRemaining} days left in cycle)")
    sb.appendLine("• Projected Month-End Spend: ${CurrencyFormatter.formatRupees(safeToSpend.projectedMonthEndSpendRupees)}")
    if (resolvedGoals.isNotEmpty()) {
      sb.appendLine("• $goalsTotalLabel (${resolvedGoals.size}): ${CurrencyFormatter.formatRupees(totalGullakRupees)}")
      resolvedGoals.forEach { g ->
        sb.appendLine("   - ${g.name}: ${CurrencyFormatter.formatRupees(g.currentAmountRupees)} / ${CurrencyFormatter.formatRupees(g.targetAmountRupees)} (${g.progressPercent.toInt()}%)")
      }
    }
    if (monthTxs.isNotEmpty()) {
      sb.appendLine("────────────────────────────")
      sb.appendLine(ledgerLabel)
      monthTxs.take(12).forEach { tx ->
        val sign = if (tx.isExpense) "-" else "+"
        sb.appendLine("$sign${CurrencyFormatter.formatRupees(tx.amountRupees)} • ${tx.displayMerchant(plainEnglish)} (${tx.category.displayLabel(plainEnglish)})")
      }
    }
    return sb.toString().trim()
  }
}

