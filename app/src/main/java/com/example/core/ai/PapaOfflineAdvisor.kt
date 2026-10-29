package com.example.core.ai

import com.example.core.engine.HouseholdAnalyticsEngine
import com.example.core.util.CurrencyFormatter
import com.example.ui.kitchen.MaaHouseholdSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Deterministic, 100% offline financial & investment advisor for Papa / Dad.
 * Used whenever `MaaAiEngineMode.MINI_LOCAL` is active or when the device is offline,
 * ensuring Papa's chat works seamlessly without internet while remaining grounded in
 * the user's real household income, fixed bills, Gullak goals, and compounding math.
 */
object PapaOfflineAdvisor {

  fun streamReply(
    userQuery: String,
    snapshot: MaaHouseholdSnapshot
  ): Flow<String> = flow {
    val fullReply = buildGroundedReply(userQuery, snapshot)
    val words = fullReply.split(" ")
    for ((idx, word) in words.withIndex()) {
      val piece = if (idx == 0) word else " $word"
      emit(piece)
      delay(14L)
    }
  }

  fun buildGroundedReply(
    userQuery: String,
    snapshot: MaaHouseholdSnapshot
  ): String {
    val plain = snapshot.plainEnglish
    val cleanName = snapshot.userName.trim().ifBlank { if (plain) "there" else "beta" }
    val greeting = if (plain) "$cleanName," else "$cleanName beta,"
    val lower = userQuery.trim().lowercase()

    val amounts = FamilyDomainRouter.extractRupeeAmounts(userQuery)
    val mentionedAmount = amounts.maxOrNull()
    val yearsMatch = Regex("\\b(\\d{1,2})\\s*(?:yr|yrs|year|years)\\b", RegexOption.IGNORE_CASE)
      .find(userQuery)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 25) ?: 5

    val monthlySave = snapshot.profile.savingsTargetRupees.coerceAtLeast(1_000L)
    val fixedBills = snapshot.profile.fixedCommitmentsRupees.coerceAtLeast(5_000L)
    val threeMonthEmergency = fixedBills * 3L
    val sixMonthEmergency = fixedBills * 6L
    val totalGullakSaved = (snapshot.allGoals.takeIf { it.isNotEmpty() } ?: listOf(snapshot.goal))
      .sumOf { it.currentAmountRupees }

    // 0. Explicit live spot-price honesty when running in Offline 0.5B mode
    val asksSpotPrice = Regex(
      "\\b(gold\\s*(price|rate|bhav)|silver\\s*(price|rate|bhav)|sona.*(bhav|rate|price|kitna)|price\\s+of\\s+(gold|silver)|nifty\\s*(today|now|level|points)?|sensex\\s*(today|now|level|points)?|(today'?s|live|current|latest)\\s*(gold|silver|market|stock|share|repo)\\s*(price|rate|bhav|level)?)\\b",
      RegexOption.IGNORE_CASE
    ).containsMatchIn(lower)
    if (asksSpotPrice) {
      val recommendedCap = ((monthlySave * 15L) / 100L).coerceAtLeast(500L)
      return if (plain) {
        "$greeting you're currently in Offline 0.5B mode, so I won't guess or show outdated live market prices—tap 'Online' at the top so I can look up today's verified Indian market rate via Google Search! As a rule of thumb, cap gold/commodity exposure at 10%–15% (~${CurrencyFormatter.formatRupees(recommendedCap)}/month from your ${CurrencyFormatter.formatRupees(monthlySave)} savings target)."
      } else {
        "$greeting abhi Offline 0.5B mode chalu hai, aur bina internet ke Papa andaze se galat bhav kabhi nahi batayenge—upar 'Online' pill dabao taaki main Google Search se aaj ka bilkul sahi Indian rate dekh kar bataoon! Waise rule yaad rakhna: portfolio mein gold/silver 10%–15% (~${CurrencyFormatter.formatRupees(recommendedCap)}/month) tak hi rakhna."
      }
    }

    // 1. Lump sum / big amount allocation advice (>= ₹10,000)
    if (mentionedAmount != null && mentionedAmount >= 10_000L &&
      (lower.contains("invest") || lower.contains("park") || lower.contains("put") ||
        lower.contains("where") || lower.contains("lakh") || lower.contains("bonus") || lower.contains("save"))
    ) {
      val emergencyPart = (mentionedAmount * 40L) / 100L
      val indexPart = (mentionedAmount * 40L) / 100L
      val goalPart = mentionedAmount - emergencyPart - indexPart
      val fiveYr = HouseholdAnalyticsEngine.projectCompounding(
        initialRupees = indexPart,
        monthlyContributionRupees = 0L,
        annualReturnPercent = 12.0,
        years = yearsMatch
      )
      return if (plain) {
        "$greeting for ${CurrencyFormatter.formatRupees(mentionedAmount)}, build safety before chasing returns: your 3-month emergency target is ${CurrencyFormatter.formatRupees(threeMonthEmergency)} (currently ${CurrencyFormatter.formatRupees(totalGullakSaved)} saved across pots). A balanced split is 40% (${CurrencyFormatter.formatRupees(emergencyPart)}) in a liquid FD, 40% (${CurrencyFormatter.formatRupees(indexPart)}) staggered into a low-cost Nifty 50 index fund (~12% long-term, growing to ~${CurrencyFormatter.formatRupees(fiveYr.maturityValueRupees)} in $yearsMatch yrs), and 20% (${CurrencyFormatter.formatRupees(goalPart)}) toward '${snapshot.goal.name}'."
      } else {
        "$greeting for ${CurrencyFormatter.formatRupees(mentionedAmount)}, rule number one is safety before speed: your 3-month household cushion is ${CurrencyFormatter.formatRupees(threeMonthEmergency)} (${CurrencyFormatter.formatRupees(totalGullakSaved)} currently in your Gullaks). Put 40% (${CurrencyFormatter.formatRupees(emergencyPart)}) in a sweep-in FD, stagger 40% (${CurrencyFormatter.formatRupees(indexPart)}) into a low-cost Nifty 50 index fund (~12% historical CAGR, ~${CurrencyFormatter.formatRupees(fiveYr.maturityValueRupees)} in $yearsMatch yrs), and drop 20% (${CurrencyFormatter.formatRupees(goalPart)}) into '${snapshot.goal.name}'."
      }
    }

    // 2. Inflation / purchasing power / RBI repo / macro question
    if (lower.contains("inflation") || lower.contains("repo") || lower.contains("purchasing power") || lower.contains("idle cash")) {
      val baseCash = mentionedAmount?.takeIf { it >= 1_000L } ?: 100_000L
      val fdProj = HouseholdAnalyticsEngine.projectCompounding(baseCash, 0L, 7.0, yearsMatch)
      val sipProj = HouseholdAnalyticsEngine.projectCompounding(baseCash, 0L, 12.0, yearsMatch)
      return if (plain) {
        "$greeting everyday household inflation in India erodes idle savings-account cash (~3% interest). Over $yearsMatch years, ${CurrencyFormatter.formatRupees(baseCash)} grows to ~${CurrencyFormatter.formatRupees(fdProj.maturityValueRupees)} in a ~7% Bank FD (preserving value) versus ~${CurrencyFormatter.formatRupees(sipProj.maturityValueRupees)} in a ~12% equity index fund for 5+ year goals (switch to Online mode for live RBI/CPI figures)."
      } else {
        "$greeting ghar ki mehangai (education, healthcare, groceries) idle 3% savings account ke paise ko dheere-dheere kamzor karti hai. Over $yearsMatch years, ${CurrencyFormatter.formatRupees(baseCash)} reaches ~${CurrencyFormatter.formatRupees(fdProj.maturityValueRupees)} in a ~7% FD for near-term safety, or ~${CurrencyFormatter.formatRupees(sipProj.maturityValueRupees)} in a ~12% index fund for 5+ year goals (tap Online above for live RBI/CPI rates)."
      }
    }

    // 3. FD / RD / PPF / Debt safe rates comparison
    if (lower.contains("fd") || lower.contains("fixed deposit") || lower.contains("ppf") || lower.contains("rd") || lower.contains("safe")) {
      val monthly = mentionedAmount?.takeIf { it in 500L..100_000L } ?: monthlySave
      val rdProj = HouseholdAnalyticsEngine.projectCompounding(0L, monthly, 6.8, yearsMatch)
      return if (plain) {
        "$greeting for capital safety, bank 1–3 yr FDs/RDs are taxed at your income slab, while PPF offers tax-free (EEE) compounding with a 15-year lock-in. Investing ${CurrencyFormatter.formatRupees(monthly)}/month at an illustrative 6.8% RD builds ~${CurrencyFormatter.formatRupees(rdProj.maturityValueRupees)} in $yearsMatch years (principal ${CurrencyFormatter.formatRupees(rdProj.totalPrincipalRupees)}); switch to Online mode to check today's exact bank FD & PPF rates."
      } else {
        "$greeting for steady safety, bank 1–3 yr FDs/RDs are taxable at your slab, whereas PPF compounds tax-free over 15 years. Putting ${CurrencyFormatter.formatRupees(monthly)}/month into an illustrative 6.8% RD builds ~${CurrencyFormatter.formatRupees(rdProj.maturityValueRupees)} in $yearsMatch years—keep 1–3 year Gullak goals in FD/RD, long-term safe debt in PPF, and tap Online above for live bank rates."
      }
    }

    // 4. Gold / SGB / Gold ETF allocation advice
    if (lower.contains("gold") || lower.contains("sgb")) {
      val recommendedCap = ((monthlySave * 15L) / 100L).coerceAtLeast(500L)
      return if (plain) {
        "$greeting gold works best as a 10%–15% portfolio hedge against inflation and currency swings, not as 100% of your wealth. Avoid jewellery making charges (8%–20%) when investing—use low-cost Gold ETFs or mutual funds (up to ~${CurrencyFormatter.formatRupees(recommendedCap)}/month out of your ${CurrencyFormatter.formatRupees(monthlySave)} target), and tap Online at the top if you want today's live 24K/22K price."
      } else {
        "$greeting sona is a solid shield against inflation, but keep it to 10%–15% of your total investments rather than everything. Skip 10%–18% jewellery making charges for pure investment and use a low-cost Gold ETF/fund (~${CurrencyFormatter.formatRupees(recommendedCap)}/month from your ${CurrencyFormatter.formatRupees(monthlySave)} savings target)—tap Online above if you want today's live 24K/22K bhav."
      }
    }

    // 5. Tax / 80C / ELSS
    if (lower.contains("tax") || lower.contains("80c") || lower.contains("elss")) {
      return if (plain) {
        "$greeting first compare Old vs New Tax Regime—Section 80C's ₹1,50,000 deduction applies only under the Old Regime (via EPF, PPF, or 3-year lock-in ELSS funds). Never buy low-yield insurance-cum-investment policies (~4%–5% IRR) just to save tax; pair term life insurance with pure PPF or low-cost index/ELSS funds."
      } else {
        "$greeting before locking money for tax, check whether Old or New Tax Regime suits your salary—80C (up to ₹1.5 lakh via EPF, PPF, or 3-yr ELSS) helps only in the Old Regime. Never mix insurance with investment in 5%-return endowment policies; keep pure term cover separate and let your SIP/PPF compound cleanly."
      }
    }

    // 6. Default: SIP / Compounding / Goal & Emergency Fund plan
    val monthly = mentionedAmount?.takeIf { it in 500L..200_000L } ?: monthlySave
    val sipProj = HouseholdAnalyticsEngine.projectCompounding(
      initialRupees = snapshot.goal.currentAmountRupees,
      monthlyContributionRupees = monthly,
      annualReturnPercent = 12.0,
      years = yearsMatch
    )
    val rdProj = HouseholdAnalyticsEngine.projectCompounding(
      initialRupees = snapshot.goal.currentAmountRupees,
      monthlyContributionRupees = monthly,
      annualReturnPercent = 7.0,
      years = yearsMatch
    )

    return if (plain) {
      "$greeting with ${CurrencyFormatter.formatRupees(snapshot.profile.monthlyIncomeRupees)} income and ${CurrencyFormatter.formatRupees(fixedBills)} fixed bills, aim first for a 3–6 month emergency buffer (${CurrencyFormatter.formatRupees(threeMonthEmergency)}–${CurrencyFormatter.formatRupees(sixMonthEmergency)}). Investing ${CurrencyFormatter.formatRupees(monthly)}/month on top of your ${CurrencyFormatter.formatRupees(snapshot.goal.currentAmountRupees)} in '${snapshot.goal.name}' grows to ~${CurrencyFormatter.formatRupees(sipProj.maturityValueRupees)} in $yearsMatch years at ~12% Index SIP (gain ${CurrencyFormatter.formatRupees(sipProj.wealthGainRupees)}) vs ~${CurrencyFormatter.formatRupees(rdProj.maturityValueRupees)} at ~7% RD."
    } else {
      "$greeting on ${CurrencyFormatter.formatRupees(snapshot.profile.monthlyIncomeRupees)} income and ${CurrencyFormatter.formatRupees(fixedBills)} fixed bills, secure a 3–6 month emergency cushion (${CurrencyFormatter.formatRupees(threeMonthEmergency)}–${CurrencyFormatter.formatRupees(sixMonthEmergency)}) first. Investing ${CurrencyFormatter.formatRupees(monthly)}/month alongside the ${CurrencyFormatter.formatRupees(snapshot.goal.currentAmountRupees)} already in '${snapshot.goal.name}' builds ~${CurrencyFormatter.formatRupees(sipProj.maturityValueRupees)} in $yearsMatch years via a ~12% Index SIP (gain ${CurrencyFormatter.formatRupees(sipProj.wealthGainRupees)}), compared to ~${CurrencyFormatter.formatRupees(rdProj.maturityValueRupees)} in a ~7% RD."
    }
  }
}
