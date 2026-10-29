package com.example.core.engine

import androidx.compose.runtime.Immutable
import com.example.core.model.ChitthiAuthor
import com.example.core.model.ChitthiNote
import kotlin.math.abs

@Immutable
data class WisdomChitthiSeed(
  val id: String,
  val author: ChitthiAuthor,
  val title: String,
  val body: String,
  val tag: String,
  val suggestedGullakRupees: Long? = null,
  val actionLabel: String? = null
) {
  fun toChitthiNote(savedAtMs: Long = System.currentTimeMillis()): ChitthiNote = ChitthiNote(
    id = id,
    author = author,
    title = title,
    body = body,
    tag = tag,
    savedAt = savedAtMs
  )
}

/**
 * Curated collection of household money notes in both:
 * 1) Warm Indian mode (Maa's nostalgic Indian-mom wit & Papa's rational, grounded savings & investment notes)
 * 2) Plain English mode (universal, zero-slang practical wisdom with clear terms)
 */
object ChitthiWisdomBank {

  val maaSeeds: List<WisdomChitthiSeed> = listOf(
    WisdomChitthiSeed(
      id = "maa_steel_glass_coffee",
      author = ChitthiAuthor.MAA,
      title = "Maa's Rasoi Coffee Rule",
      body = "Beta, don't drink ₹280 iced coffee outside every afternoon—beat one spoon of coffee with sugar and two drops of hot water in a steel glass at home. Froths better, and your Batua won't cry.",
      tag = "Nostalgic Wit",
      suggestedGullakRupees = 100L,
      actionLabel = "Skip café • Save ₹100"
    ),
    WisdomChitthiSeed(
      id = "maa_boond_boond",
      author = ChitthiAuthor.MAA,
      title = "Boond-Boond Se Ghada",
      body = "Save a little money daily, beta—even ₹50 before switching off the light. Boond-boond se ghada bharta hai, and nobody ever woke up regretting a heavy clay Gullak.",
      tag = "Daily Habit",
      suggestedGullakRupees = 50L,
      actionLabel = "Save ₹50 in Gullak"
    ),
    WisdomChitthiSeed(
      id = "maa_hungry_cart",
      author = ChitthiAuthor.MAA,
      title = "Never Shop Hungry",
      body = "Never open a food or grocery app on an empty stomach, beta. Eat one hot phulka with achaar first—half the 'urgent' things in your cart will immediately look unnecessary.",
      tag = "Rasoi Smarts"
    ),
    WisdomChitthiSeed(
      id = "maa_quiet_wealth",
      author = ChitthiAuthor.MAA,
      title = "What Real Wealth Looks Like",
      body = "Main koi badi economist nahi hoon, beta, bas itna dekha hai: real wealth is sleeping peacefully on the 28th of the month without checking when salary will arrive.",
      tag = "Quiet Wisdom"
    ),
    WisdomChitthiSeed(
      id = "maa_sale_arithmetic",
      author = ChitthiAuthor.MAA,
      title = "Maa's Sale Math",
      body = "Beta, a 'Flat 50% Off' sticker on something you didn't need yesterday is still 100% money leaving your Batua today. Don't let red banners rush a calm mind.",
      tag = "Sharp Eye"
    ),
    WisdomChitthiSeed(
      id = "maa_evening_walk_cab",
      author = ChitthiAuthor.MAA,
      title = "Ten-Minute Evening Walk",
      body = "Why pay ₹180 surge fare for a one-kilometre cab when evening air is free? Walk ten minutes, call me on the way, and let Papa's Gullak keep the cab fare.",
      tag = "Nostalgic Wit",
      suggestedGullakRupees = 100L,
      actionLabel = "Walk & save ₹100"
    ),
    WisdomChitthiSeed(
      id = "maa_water_bottle",
      author = ChitthiAuthor.MAA,
      title = "Fill the Bottle Before Leaving",
      body = "Always tuck a water bottle into your bag before stepping out, beta. Buying ₹60 bottled water and then a ₹190 snack just to sit in AC is the oldest trap in the market.",
      tag = "Everyday Sense"
    ),
    WisdomChitthiSeed(
      id = "maa_two_sleeps_rule",
      author = ChitthiAuthor.MAA,
      title = "Give Impulse Buys Two Nights",
      body = "If an online cart still feels genuinely useful after two nights of sleep and a home-cooked meal, buy it happily without guilt. Most impulse urges fade by morning.",
      tag = "Smart Pause"
    ),
    WisdomChitthiSeed(
      id = "maa_sugar_jar_inflation",
      author = ChitthiAuthor.MAA,
      title = "Keep Idle Cash Working",
      body = "Inflation quietly erodes idle cash, beta. Keep your monthly emergency buffer accessible, and put long-term savings into steady compounding investments.",
      tag = "Smart Money"
    ),
    WisdomChitthiSeed(
      id = "maa_home_chai_friends",
      author = ChitthiAuthor.MAA,
      title = "Real Friends & Home Chai",
      body = "Never feel shy saying 'come home for adrak chai instead of a ₹900 café bill', beta. People who truly care about you come for the conversation, not the receipt.",
      tag = "Humble Grace"
    ),
    WisdomChitthiSeed(
      id = "maa_raise_gullak_first",
      author = ChitthiAuthor.MAA,
      title = "When Salary Grows",
      body = "Whenever a raise or bonus arrives, increase your Gullak savings before upgrading fixed habits. Lifestyle upgrades feel normal in a week, beta, while savings compound for years.",
      tag = "Quiet Wisdom"
    ),
    WisdomChitthiSeed(
      id = "maa_forgive_heavy_week",
      author = ChitthiAuthor.MAA,
      title = "A Heavy Week Is Just Life",
      body = "Medicines, tickets home, or helping a tired friend—some weeks simply cost more, beta. Your Khata is a clear record, never a reason for guilt. Reset tomorrow.",
      tag = "Motherly Comfort"
    ),
    WisdomChitthiSeed(
      id = "maa_grocery_list_focus",
      author = ChitthiAuthor.MAA,
      title = "Write a List Before Ordering",
      body = "Write down the exact three groceries you need before opening an instant-delivery app, beta. Free-delivery thresholds trick people into adding ₹200 of extra snacks.",
      tag = "Rasoi Smarts"
    ),
    WisdomChitthiSeed(
      id = "maa_rainy_day_peace",
      author = ChitthiAuthor.MAA,
      title = "Three Months of Calm",
      body = "Keep three months of fixed bills safe in an emergency cushion before chasing risky returns, beta. Peace of mind lets you make smarter career and money decisions.",
      tag = "Smart Money"
    ),
    WisdomChitthiSeed(
      id = "maa_weekend_tiffin_prep",
      author = ChitthiAuthor.MAA,
      title = "Sunday Prep Saves Wednesday",
      body = "Spending twenty minutes chopping onions or boiling chana on Sunday evening saves three ₹350 weekday takeout orders when work runs late, beta.",
      tag = "Everyday Sense",
      suggestedGullakRupees = 150L,
      actionLabel = "Cook & save ₹150"
    )
  )

  val maaSeedsPlainEnglish: List<WisdomChitthiSeed> = listOf(
    WisdomChitthiSeed(
      id = "maa_steel_glass_coffee",
      author = ChitthiAuthor.MAA,
      title = "Mom's Home Coffee Rule",
      body = "Skip the ₹280 café iced coffee on regular afternoons—make frothy iced coffee at home in two minutes. Tastes great, and your daily budget stays intact.",
      tag = "Everyday Tip",
      suggestedGullakRupees = 100L,
      actionLabel = "Skip café • Save ₹100"
    ),
    WisdomChitthiSeed(
      id = "maa_boond_boond",
      author = ChitthiAuthor.MAA,
      title = "Small Daily Transfers Add Up",
      body = "Save a small amount every day—even ₹50 before bed. Consistent daily transfers build your savings pot faster than waiting for leftover money at month-end.",
      tag = "Daily Habit",
      suggestedGullakRupees = 50L,
      actionLabel = "Add ₹50 to Savings"
    ),
    WisdomChitthiSeed(
      id = "maa_hungry_cart",
      author = ChitthiAuthor.MAA,
      title = "Never Order Hungry",
      body = "Never open a food delivery or grocery app on an empty stomach. Have a quick snack at home first—half the items in your cart will stop looking urgent.",
      tag = "Smart Habit"
    ),
    WisdomChitthiSeed(
      id = "maa_quiet_wealth",
      author = ChitthiAuthor.MAA,
      title = "What Real Financial Peace Is",
      body = "Real financial comfort isn't flashy purchases—it is sleeping peacefully on the 28th of the month without worrying when your next paycheck arrives.",
      tag = "Practical Wisdom"
    ),
    WisdomChitthiSeed(
      id = "maa_sale_arithmetic",
      author = ChitthiAuthor.MAA,
      title = "50% Off Is Still Spending",
      body = "A '50% Off' discount on something you did not need yesterday is still 100% money leaving your wallet today. Buy for actual need, not countdown timers.",
      tag = "Clear Math"
    ),
    WisdomChitthiSeed(
      id = "maa_evening_walk_cab",
      author = ChitthiAuthor.MAA,
      title = "Short Walk Over Surge Rides",
      body = "Before booking a ₹180 surge ride for a short distance, check if a 10-minute walk works. You get fresh air and keep the fare in your savings pot.",
      tag = "Everyday Tip",
      suggestedGullakRupees = 100L,
      actionLabel = "Walk & save ₹100"
    ),
    WisdomChitthiSeed(
      id = "maa_two_sleeps_rule",
      author = ChitthiAuthor.MAA,
      title = "The 48-Hour Cart Rule",
      body = "Leave non-essential online purchases in your cart for 48 hours. If it still solves a real problem two days later, buy it guilt-free.",
      tag = "Smart Pause"
    ),
    WisdomChitthiSeed(
      id = "maa_raise_gullak_first",
      author = ChitthiAuthor.MAA,
      title = "Save Raises First",
      body = "Whenever your income increases or a bonus arrives, raise your monthly savings transfer before increasing recurring expenses.",
      tag = "Long-Term Plan"
    ),
    WisdomChitthiSeed(
      id = "maa_forgive_heavy_week",
      author = ChitthiAuthor.MAA,
      title = "High-Spend Weeks Happen",
      body = "Medical bills, travel, or helping family—some weeks cost more than planned. Your ledger is a factual log, not a scorecard. Adjust calmly tomorrow.",
      tag = "Perspective"
    ),
    WisdomChitthiSeed(
      id = "maa_grocery_list_focus",
      author = ChitthiAuthor.MAA,
      title = "Write a List Before Ordering",
      body = "List the exact items you need before opening a delivery app. Free-delivery minimums often nudge people into adding ₹200 of unplanned extras.",
      tag = "Smart Habit"
    ),
    WisdomChitthiSeed(
      id = "maa_rainy_day_peace",
      author = ChitthiAuthor.MAA,
      title = "Build an Emergency Buffer First",
      body = "Keep three months of fixed expenses in an accessible emergency fund before taking higher investment risk. Financial margin improves every decision.",
      tag = "Financial Safety"
    ),
    WisdomChitthiSeed(
      id = "maa_weekend_tiffin_prep",
      author = ChitthiAuthor.MAA,
      title = "Sunday Meal Prep Pays Off",
      body = "Twenty minutes of meal prep on Sunday evening prevents multiple ₹350 weekday takeout orders when work gets busy.",
      tag = "Everyday Tip",
      suggestedGullakRupees = 150L,
      actionLabel = "Cook & save ₹150"
    )
  )

  val papaSeeds: List<WisdomChitthiSeed> = listOf(
    WisdomChitthiSeed(
      id = "papa_day_one_transfer",
      author = ChitthiAuthor.PAPA,
      title = "Papa's Day-1 Rule",
      body = "Pay your Gullak and monthly SIP on the 1st of the month like non-negotiable rent to your future self. Waiting until the 30th to save leftovers usually leaves zero.",
      tag = "Payday Rule",
      suggestedGullakRupees = 200L,
      actionLabel = "Add ₹200 to Gullak"
    ),
    WisdomChitthiSeed(
      id = "papa_days_of_freedom",
      author = ChitthiAuthor.PAPA,
      title = "Compare Price to Daily Budget",
      body = "Before buying anything over ₹2,000, divide the price by your daily safe-to-spend amount. Checking how many days of budget it equals makes every trade-off clear.",
      tag = "Papa's Math"
    ),
    WisdomChitthiSeed(
      id = "papa_boring_compounding",
      author = ChitthiAuthor.PAPA,
      title = "Stay Consistent With SIPs",
      body = "Compounding looks quiet in month three and accelerates after year five. Keep your monthly index SIP or RD running through noisy headlines instead of pausing early.",
      tag = "Long Game"
    ),
    WisdomChitthiSeed(
      id = "papa_emergency_before_risk",
      author = ChitthiAuthor.PAPA,
      title = "Emergency Buffer Before Equity",
      body = "Park 3 to 6 months of fixed household bills in a safe FD or liquid pot before chasing equity returns. Never invest money you might need next monsoon in volatile assets.",
      tag = "Risk Sense"
    ),
    WisdomChitthiSeed(
      id = "papa_beat_inflation_calmly",
      author = ChitthiAuthor.PAPA,
      title = "Beat 6% Household Inflation",
      body = "Idle savings account cash earning 3% loses purchasing power against ~6% everyday inflation. Match 1–3 year goals with FDs/RDs and 5+ year wealth with low-cost index SIPs.",
      tag = "Real Returns"
    ),
    WisdomChitthiSeed(
      id = "papa_honest_split",
      author = ChitthiAuthor.PAPA,
      title = "Order Within Your Own Numbers",
      body = "On group outings, spend what fits your own monthly plan rather than matching the flashiest spender at the table. Quiet financial discipline builds lasting respect.",
      tag = "Practical Advice"
    ),
    WisdomChitthiSeed(
      id = "papa_subscription_audit",
      author = ChitthiAuthor.PAPA,
      title = "Audit Unused Subscriptions",
      body = "One unused ₹299/month subscription costs ₹3,588 per year—nearly ₹23,000 if invested over 5 years. Cancel any recurring plan unused in the past 21 days.",
      tag = "Expense Audit"
    ),
    WisdomChitthiSeed(
      id = "papa_micro_streak",
      author = ChitthiAuthor.PAPA,
      title = "Match Treats With Savings",
      body = "Spent ₹300 on takeout? Transfer ₹50 into your Gullak right after. You enjoy your meal while keeping your long-term goal moving forward.",
      tag = "Papa's Math",
      suggestedGullakRupees = 50L,
      actionLabel = "Save ₹50 match"
    )
  )

  val papaSeedsPlainEnglish: List<WisdomChitthiSeed> = listOf(
    WisdomChitthiSeed(
      id = "papa_day_one_transfer",
      author = ChitthiAuthor.PAPA,
      title = "Save & Invest on Payday First",
      body = "Automate your savings pot and monthly SIP on day one of your pay cycle like a fixed bill. Waiting until month-end to invest leftovers rarely works.",
      tag = "Payday Rule",
      suggestedGullakRupees = 200L,
      actionLabel = "Add ₹200 to Savings"
    ),
    WisdomChitthiSeed(
      id = "papa_days_of_freedom",
      author = ChitthiAuthor.PAPA,
      title = "Measure Buys in Daily Allowance",
      body = "Before buying an item over ₹2,000, divide the price by your daily safe-to-spend budget. Seeing how many days of spending it represents clarifies every purchase.",
      tag = "Dad's Math"
    ),
    WisdomChitthiSeed(
      id = "papa_boring_compounding",
      author = ChitthiAuthor.PAPA,
      title = "Let Compounding Work",
      body = "Investment growth looks modest in the first few months and compounds significantly over 5+ years. Staying consistent matters far more than timing the market.",
      tag = "Long-Term"
    ),
    WisdomChitthiSeed(
      id = "papa_emergency_before_risk",
      author = ChitthiAuthor.PAPA,
      title = "Fund Emergency Cushion First",
      body = "Keep 3 to 6 months of fixed expenses in an accessible FD or liquid fund before taking equity market risk. Short-term money belongs in stable instruments.",
      tag = "Risk Management"
    ),
    WisdomChitthiSeed(
      id = "papa_beat_inflation_calmly",
      author = ChitthiAuthor.PAPA,
      title = "Protect Against ~6% Inflation",
      body = "Excess cash sitting at 3% savings interest loses real value to ~6% inflation. Pair short-term FDs/RDs for 1–3 year goals with diversified index SIPs for 5+ year horizons.",
      tag = "Real Returns"
    ),
    WisdomChitthiSeed(
      id = "papa_subscription_audit",
      author = ChitthiAuthor.PAPA,
      title = "Cancel Unused Renewals",
      body = "One forgotten ₹299/month subscription equals ₹3,588 a year. Cancel any recurring plan you have not used in the last 3 weeks and redirect it to savings.",
      tag = "Expense Audit"
    ),
    WisdomChitthiSeed(
      id = "papa_micro_streak",
      author = ChitthiAuthor.PAPA,
      title = "Match Takeout With Savings",
      body = "Ordered ₹300 takeout? Put ₹50 into your savings goal right afterward so discretionary treats also build your target.",
      tag = "Habit Rule",
      suggestedGullakRupees = 50L,
      actionLabel = "Add ₹50 to Savings"
    )
  )

  val bhaiSeeds: List<WisdomChitthiSeed>
    get() = papaSeeds

  val bhaiSeedsPlainEnglish: List<WisdomChitthiSeed>
    get() = papaSeedsPlainEnglish

  fun getMaaSeeds(plainEnglish: Boolean = false): List<WisdomChitthiSeed> =
    if (plainEnglish) maaSeedsPlainEnglish else maaSeeds

  fun getPapaSeeds(plainEnglish: Boolean = false): List<WisdomChitthiSeed> =
    if (plainEnglish) papaSeedsPlainEnglish else papaSeeds

  fun getBhaiSeeds(plainEnglish: Boolean = false): List<WisdomChitthiSeed> =
    getPapaSeeds(plainEnglish)

  fun pickMaaSeed(indexSeed: Int, plainEnglish: Boolean = false): WisdomChitthiSeed {
    val list = getMaaSeeds(plainEnglish)
    val idx = abs(indexSeed) % list.size
    return list[idx]
  }

  fun pickPapaSeed(indexSeed: Int, plainEnglish: Boolean = false): WisdomChitthiSeed {
    val list = getPapaSeeds(plainEnglish)
    val idx = abs(indexSeed) % list.size
    return list[idx]
  }

  fun pickBhaiSeed(indexSeed: Int, plainEnglish: Boolean = false): WisdomChitthiSeed =
    pickPapaSeed(indexSeed, plainEnglish)
}

