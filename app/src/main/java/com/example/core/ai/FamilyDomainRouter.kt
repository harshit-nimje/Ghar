package com.example.core.ai

import com.example.core.util.CurrencyFormatter
import com.example.ui.kitchen.MaaHouseholdSnapshot

/**
 * Household domain router between Maa / Mom and Papa / Dad:
 * - Maa handles daily petty cash (`Batua` / Safe to Spend Today), everyday transactions,
 *   quick daily arithmetic/bill splits, habit checks, and motherly motivation.
 * - Papa handles investments (SIPs, mutual/index funds, FD/RD/PPF/NPS, gold/SGB, equity),
 *   macro/real-world financial insights (inflation, RBI repo rate, interest rates, 80C/tax),
 *   emergency buffer sizing, and allocating or saving big amounts (>= ₹10,000).
 *
 * When the user asks Maa something in Papa's domain, Maa warmly passes the question to Papa
 * (with a 1-tap handoff pill in UI). Conversely, when the user asks Papa about petty daily
 * wallet spend or tiny everyday expenses, Papa warmly sends them to Maa.
 */
object FamilyDomainRouter {

  private val INVESTMENT_KEYWORDS_REGEX = Regex(
    "\\b(invest|investing|investment|investments|sip|sips|mutual\\s*funds?|index\\s*funds?|nifty|sensex|stocks?|shares?|equity|equities|demat|fd|fds|fixed\\s*deposit|rd|recurring\\s*deposit|ppf|nps|epf|elss|sgb|sovereign\\s*gold|gold|silver|sona|chandi|bullion|mcx|ibja|treasury|repo\\s*rate|repo|rbi|inflation|cpi|cagr|compounding|compound\\s*interest|interest\\s*rates?|80c|tax\\s*sav(ing|er)|tax\\s*slabs?|capital\\s*gains?|portfolio|asset\\s*allocation|liquid\\s*fund|debt\\s*fund|dividend|retire|retirement|corpus|exchange\\s*rate|usd\\s*to\\s*inr)\\b",
    RegexOption.IGNORE_CASE
  )

  private val LIVE_MARKET_DATA_REGEX = Regex(
    "\\b(gold|silver|sona|chandi|bullion|mcx|ibja|sgb|sensex|nifty|bank\\s*nifty|stock\\s*price|share\\s*price|nav|repo\\s*rate|rbi|cpi|inflation\\s*rate|fd\\s*rates?|fixed\\s*deposit\\s*rates?|ppf\\s*rate|ssy\\s*rate|scss\\s*rate|interest\\s*rates?|tax\\s*slabs?|usd|dollar\\s*rate|exchange\\s*rate|bitcoin|crypto)\\b|\\b(price|rate|bhav|value|level|yield)\\s+(today|now|right\\s*now|current|currently|live|latest|in\\s+india)\\b|\\b(today'?s|current|latest|live)\\s+(price|rate|rates|bhav|value|yield|inflation|index)\\b",
    RegexOption.IGNORE_CASE
  )

  private val BIG_AMOUNT_ACTION_REGEX = Regex(
    "\\b(save|park|put|allocate|grow|where\\s+should\\s+i\\s+(put|keep|park|save)|what\\s+to\\s+do\\s+with|bonus|windfall|lump\\s*sum|lumpsum|emergency\\s*fund)\\b",
    RegexOption.IGNORE_CASE
  )

  private val PETTY_DAILY_ITEMS_REGEX = Regex(
    "\\b(chai|tea|coffee|kaapi|iced\\s*coffee|samosa|snack|snacks|biscuit|maggi|momos|vada\\s*pav|swiggy|zomato|dinner\\s+tonight|lunch\\s+today|order\\s+(food|dinner|lunch|pizza|biryani|burger)|sabzi|milk|bread|eggs|grocer(y|ies)|blinkit|zepto|instamart|auto|rickshaw|cab|uber|ola|rapido|metro\\s+fare|bus\\s+fare|petty\\s*cash|batua|daily\\s+wallet|safe\\s+to\\s+spend\\s+today|pocket\\s+money)\\b",
    RegexOption.IGNORE_CASE
  )

  private val SIMPLE_DAILY_MATH_REGEX = Regex(
    "\\b(split\\s+(this|the)?\\s*(bill|dinner|lunch|chai|cab)|how\\s+much\\s+left\\s+today|can\\s+i\\s+(spend|buy|order)|motivat(e|ion)|feeling\\s+(guilty|stressed|broke)|overspent\\s+today)\\b",
    RegexOption.IGNORE_CASE
  )

  /**
   * Returns true if the question asks about real-world asset prices, bullion, market indices,
   * central bank/deposit interest rates, or live macroeconomic figures that MUST be verified
   * via Google Search grounding rather than estimated from static offline templates.
   */
  fun isLiveMarketDataQuery(query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return false
    return LIVE_MARKET_DATA_REGEX.containsMatchIn(trimmed)
  }

  /**
   * Extracts rupee magnitudes from natural-language Indian/English queries, including
   * shorthand like "25k", "1.5 lakh", "2 lacs", "₹50,000".
   */
  fun extractRupeeAmounts(text: String): List<Long> {
    val clean = text.lowercase().replace(",", "")
    val results = mutableListOf<Long>()

    // 1. Lakh / Crore multipliers (e.g., "1.5 lakh", "2 lac", "1 crore")
    Regex("(\\d+(?:\\.\\d+)?)\\s*(lakh|lakhs|lac|lacs|cr|crore|crores)\\b").findAll(clean).forEach { m ->
      val num = m.groupValues[1].toDoubleOrNull() ?: return@forEach
      val unit = m.groupValues[2]
      val multiplier = if (unit.startsWith("cr")) 10_000_000.0 else 100_000.0
      results.add((num * multiplier).toLong())
    }

    // 2. 'k' shorthand (e.g., "15k", "50k")
    Regex("(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?)\\s*k\\b").findAll(clean).forEach { m ->
      val num = m.groupValues[1].toDoubleOrNull() ?: return@forEach
      results.add((num * 1_000.0).toLong())
    }

    // 3. Plain integers >= 10 (avoiding single-digit years like "5 years" unless prefixed with ₹/rs)
    Regex("(?:₹|rs\\.?|inr)\\s*(\\d{1,9})\\b|\\b(\\d{2,9})\\b").findAll(clean).forEach { m ->
      val raw = m.groupValues[1].ifEmpty { m.groupValues[2] }
      val value = raw.toLongOrNull() ?: return@forEach
      if (value !in 1990L..2035L) { // Ignore calendar years if unprefixed
        results.add(value)
      }
    }

    return results.distinct()
  }

  /**
   * Returns true if the user's message to Maa belongs in Papa's savings/investment/macro domain.
   */
  fun shouldMaaReferToPapa(query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return false

    val hasInvestmentTerm = INVESTMENT_KEYWORDS_REGEX.containsMatchIn(trimmed)
    if (hasInvestmentTerm) return true

    val amounts = extractRupeeAmounts(trimmed)
    val maxAmount = amounts.maxOrNull() ?: 0L
    val hasBigAmountSavingIntent = maxAmount >= 10_000L && BIG_AMOUNT_ACTION_REGEX.containsMatchIn(trimmed)
    if (hasBigAmountSavingIntent) return true

    // Explicit mention asking for Papa/Dad's investment view
    val lower = trimmed.lowercase()
    if ((lower.contains("ask papa") || lower.contains("ask dad")) && maxAmount >= 5_000L) {
      return true
    }

    return false
  }

  /**
   * Returns true if the user's message to Papa is about petty daily expenses, small treats,
   * daily Batua allowance, or quick everyday math best answered by Maa.
   */
  fun shouldPapaReferToMaa(query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return false

    // Never refer away if the user is genuinely asking an investment/compounding/macro question
    // (e.g., "If I skip ₹200 coffee daily and invest in a SIP for 5 years...")
    if (INVESTMENT_KEYWORDS_REGEX.containsMatchIn(trimmed)) return false

    val amounts = extractRupeeAmounts(trimmed)
    val maxAmount = amounts.maxOrNull() ?: 0L
    if (maxAmount >= 5_000L) return false

    val hasPettyItem = PETTY_DAILY_ITEMS_REGEX.containsMatchIn(trimmed)
    val hasSimpleDailyCheck = SIMPLE_DAILY_MATH_REGEX.containsMatchIn(trimmed)

    if (hasPettyItem && (maxAmount == 0L || maxAmount <= 2_500L)) {
      return true
    }
    if (hasSimpleDailyCheck && (maxAmount == 0L || maxAmount <= 2_000L) && !BIG_AMOUNT_ACTION_REGEX.containsMatchIn(trimmed)) {
      return true
    }

    return false
  }

  fun composeMaaReferralToPapa(
    userName: String,
    query: String,
    snapshot: MaaHouseholdSnapshot
  ): String {
    val plain = snapshot.plainEnglish
    val monthlySavings = CurrencyFormatter.formatRupees(snapshot.profile.savingsTargetRupees)
    val goalSaved = CurrencyFormatter.formatRupees(snapshot.goal.currentAmountRupees)

    if (plain) {
      val cleanName = userName.trim().ifBlank { "there" }
      return "Hey $cleanName, investments, interest rates, and growing bigger sums are Dad's specialty—I focus on your daily wallet, everyday expenses, and quick math! You already set aside $monthlySavings/month with $goalSaved in '${snapshot.goal.name}'. Tap 'Ask Dad instead' below and he'll walk you through the numbers and real-world options."
    }

    val cleanName = userName.trim().ifBlank { "beta" }
    return "Arre $cleanName beta, main toh roz ke Batua, chhote kharche aur ghar ke hisaab sambhalti hoon—investments, SIP-FD rates aur badi rakam ki planning Papa ka forte hai! You already target $monthlySavings/month with $goalSaved in '${snapshot.goal.name}'. Tap 'Ask Papa instead' below—he's sitting in his room with the newspaper and will guide you properly."
  }

  fun composePapaReferralToMaa(
    userName: String,
    query: String,
    snapshot: MaaHouseholdSnapshot
  ): String {
    val plain = snapshot.plainEnglish
    val safeToday = CurrencyFormatter.formatRupees(snapshot.safeToSpend.dailyAllowanceRupees)

    if (plain) {
      val cleanName = userName.trim().ifBlank { "there" }
      return "Haha $cleanName, everyday petty expenses, snacks, and daily wallet math are Mom's department—I stick to your big savings goals, investments, and long-term rates! Your Safe to Spend Today is $safeToday across ${snapshot.safeToSpend.daysRemaining} days left. Tap 'Ask Mom instead' below and she'll sort out today's spend right away."
    }

    val cleanName = userName.trim().ifBlank { "beta" }
    return "Arre $cleanName beta, roz ke chai-nashte, chhote kharche aur aaj ke Batua ka hisaab Maa se behtar kaun jaanta hai! Main toh badi bachat, Gullak aur investments dekhta hoon. Your Safe to Spend Today is $safeToday (${snapshot.safeToSpend.daysRemaining} days left)—tap 'Ask Maa instead' below and go ask her in the room!"
  }
}
