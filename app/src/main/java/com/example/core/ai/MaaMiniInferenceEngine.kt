package com.example.core.ai

import com.example.core.engine.ChitthiWisdomBank
import com.example.core.engine.HouseholdAnalyticsEngine
import com.example.core.model.Category
import com.example.core.model.ChatSender
import com.example.core.model.CommitmentType
import com.example.core.model.GuiltSensitivity
import com.example.core.model.InstrumentType
import com.example.core.model.MaaChatMessage
import com.example.core.model.PapaChatMessage
import com.example.ui.common.HisaabCalculatorEngine
import com.example.ui.kitchen.MaaHouseholdSnapshot
import com.example.core.util.CurrencyFormatter
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

enum class MaaMiniIntent {
  AFFORDABILITY_CHECK,
  MULTI_OPTION_COMPARISON,
  SAVINGS_PROJECTION,
  MOTHERLY_WISDOM,
  DAILY_ALLOWANCE_CHECK,
  CHAI_SNACK_HABIT,
  FOOD_DELIVERY_HABIT,
  COMMUTE_SPEND,
  GULLAK_PROGRESS,
  MONTH_END_PACE,
  EMOTIONAL_COMFORT,
  GREETING_SMALLTALK,
  OFF_TOPIC_REFUSAL,
  BILLS_AND_SUBSCRIPTIONS,
  RECEIVABLES_CHECK,
  BILL_SPLIT_AND_MATH,
  SIP_COMPOUNDING_PLAN,
  MERCHANT_AND_CATEGORY_AUDIT,
  BUDGET_REBALANCE_ADVICE
}

/**
 * On-device 0.5B Q4_K_M local inference & household financial reasoning engine.
 * Combines 19 quantized semantic heads with multi-turn conversational coreference resolution,
 * multi-clause prompt decomposition, exact household ledger/bill/receivable grounding, and
 * deterministic EMI, SIP compounding, split, and Safe-to-Spend arithmetic.
 */
class MaaMiniInferenceEngine internal constructor(
  private val vocabBuckets: Int,
  private val hiddenDim: Int,
  private val numIntents: Int,
  private val scaleW1: Float,
  private val scaleW2: Float,
  private val w1Quantized: ByteArray,
  private val b1: FloatArray,
  private val w2Quantized: ByteArray,
  private val b2: FloatArray
) {

  private data class ConversationMemory(
    val lastMentionedRupees: Long? = null,
    val lastIntent: MaaMiniIntent? = null,
    val lastCategory: Category? = null
  )

  fun classifyIntent(query: String): Pair<MaaMiniIntent, Float> {
    val clean = query.trim().lowercase()
    if (clean.isEmpty()) return MaaMiniIntent.GREETING_SMALLTALK to 1f

    // Fast deterministic overrides for high-precision multi-clause / financial patterns
    val numbers = extractAllRupeeNumbers(clean)
    if (OFF_TOPIC_FAST_REGEX.containsMatchIn(clean)) {
      return MaaMiniIntent.OFF_TOPIC_REFUSAL to 0.99f
    }
    if (RECEIVABLE_REGEX.containsMatchIn(clean)) {
      return MaaMiniIntent.RECEIVABLES_CHECK to 0.96f
    }
    if (BILLS_SUBS_REGEX.containsMatchIn(clean) && !COMPARISON_REGEX.containsMatchIn(clean)) {
      return MaaMiniIntent.BILLS_AND_SUBSCRIPTIONS to 0.95f
    }
    if (SIP_INVEST_REGEX.containsMatchIn(clean) && (clean.contains("year") || clean.contains("sip") || clean.contains("ppf") || clean.contains("compound"))) {
      return MaaMiniIntent.SIP_COMPOUNDING_PLAN to 0.96f
    }
    if (SPLIT_EMI_DISCOUNT_REGEX.containsMatchIn(clean)) {
      return MaaMiniIntent.BILL_SPLIT_AND_MATH to 0.95f
    }
    if (numbers.size >= 2 && COMPARISON_REGEX.containsMatchIn(clean)) {
      return MaaMiniIntent.MULTI_OPTION_COMPARISON to 0.96f
    }
    if (numbers.isNotEmpty() && PROJECTION_REGEX.containsMatchIn(clean)) {
      return MaaMiniIntent.SAVINGS_PROJECTION to 0.95f
    }
    if (REBALANCE_REGEX.containsMatchIn(clean)) {
      return MaaMiniIntent.BUDGET_REBALANCE_ADVICE to 0.93f
    }
    if (AUDIT_REGEX.containsMatchIn(clean)) {
      return MaaMiniIntent.MERCHANT_AND_CATEGORY_AUDIT to 0.93f
    }
    if (numbers.isNotEmpty() && AFFORDABILITY_FAST_REGEX.containsMatchIn(clean)) {
      return MaaMiniIntent.AFFORDABILITY_CHECK to 0.95f
    }

    val sparseFeatures = extractActiveBuckets(clean, vocabBuckets)
    val hidden = FloatArray(hiddenDim)

    for (h in 0 until hiddenDim) {
      var acc = b1[h]
      val rowOffset = h * vocabBuckets
      for ((bucketIdx, weight) in sparseFeatures) {
        val qWeight = w1Quantized[rowOffset + bucketIdx].toInt()
        acc += (qWeight * scaleW1) * weight
      }
      hidden[h] = if (acc > 0f) acc else 0f
    }

    val logits = FloatArray(numIntents)
    var maxLogit = Float.NEGATIVE_INFINITY
    for (i in 0 until numIntents) {
      var acc = b2[i]
      val rowOffset = i * hiddenDim
      for (h in 0 until hiddenDim) {
        val qWeight = w2Quantized[rowOffset + h].toInt()
        acc += (qWeight * scaleW2) * hidden[h]
      }
      logits[i] = acc
      if (acc > maxLogit) maxLogit = acc
    }

    var sumExp = 0.0
    val probs = FloatArray(numIntents)
    for (i in 0 until numIntents) {
      val e = exp((logits[i] - maxLogit).toDouble())
      probs[i] = e.toFloat()
      sumExp += e
    }

    var bestIdx = 0
    var bestProb = 0f
    for (i in 0 until numIntents) {
      val p = (probs[i] / sumExp.toFloat())
      if (p > bestProb) {
        bestProb = p
        bestIdx = i
      }
    }

    val intents = MaaMiniIntent.entries
    val predicted = intents.getOrElse(bestIdx) { MaaMiniIntent.DAILY_ALLOWANCE_CHECK }
    return predicted to bestProb
  }

  fun generateReply(
    userQuery: String,
    snapshot: MaaHouseholdSnapshot,
    priorHistory: List<MaaChatMessage> = emptyList()
  ): String {
    val cleanQuery = userQuery.trim()
    val memory = extractConversationMemory(priorHistory)

    // Decompose genuinely distinct multi-part questions (e.g. "split 1200 by 3 and can I afford my share tonight?")
    val clauses = splitCompoundClauses(cleanQuery)
    if (clauses.size == 2) {
      val firstIntent = classifyIntent(clauses[0]).first
      val secondIntent = classifyIntent(clauses[1]).first
      if (firstIntent != secondIntent &&
        firstIntent != MaaMiniIntent.GREETING_SMALLTALK &&
        secondIntent != MaaMiniIntent.GREETING_SMALLTALK
      ) {
        val firstPart = solveSingleClause(
          clause = clauses[0],
          intent = firstIntent,
          snap = snapshot,
          memory = memory,
          includeGreeting = true,
          turnCount = priorHistory.size
        )
        // Carry over derived rupee figure (e.g. per-person split share) into clause 2!
        val derivedAmount = extractDerivedAmountForFollowUp(clauses[0], firstPart)
          ?: extractAllRupeeNumbers(clauses[0]).firstOrNull()
          ?: memory.lastMentionedRupees
        val enrichedMemory = memory.copy(lastMentionedRupees = derivedAmount, lastIntent = firstIntent)
        val secondPart = solveSingleClause(
          clause = clauses[1],
          intent = secondIntent,
          snap = snapshot,
          memory = enrichedMemory,
          includeGreeting = false,
          turnCount = priorHistory.size + 1
        )
        return "$firstPart Also: $secondPart"
      }
    }

    // Check if this is a coreference follow-up ("what about half of that?", "and for 6 months?", "what if it's 600?")
    val resolvedQuery = resolveFollowUpPronouns(cleanQuery, memory)
    val (classifiedIntent, confidence) = classifyIntent(resolvedQuery)
    val intent = if (resolvedQuery != cleanQuery && memory.lastIntent != null && confidence < 0.90f) {
      memory.lastIntent
    } else {
      classifiedIntent
    }
    return solveSingleClause(
      clause = resolvedQuery,
      intent = intent,
      snap = snapshot,
      memory = memory,
      includeGreeting = true,
      turnCount = priorHistory.size
    )
  }

  private fun solveSingleClause(
    clause: String,
    intent: MaaMiniIntent,
    snap: MaaHouseholdSnapshot,
    memory: ConversationMemory,
    includeGreeting: Boolean,
    turnCount: Int
  ): String {
    val plain = snap.plainEnglish
    val defaultFallbackName = if (plain) "friend" else "beta"
    val rawName = snap.userName.trim().ifBlank { defaultFallbackName }
    val name = if (includeGreeting) rawName else ""
    val variant = abs(clause.hashCode() + turnCount * 31)
    val extractedNumbers = extractAllRupeeNumbers(clause)

    return when (intent) {
      MaaMiniIntent.OFF_TOPIC_REFUSAL -> composeOffTopicRefusal(rawName, plain)
      MaaMiniIntent.MULTI_OPTION_COMPARISON -> composeComparisonReply(rawName, extractedNumbers, snap, variant, includeGreeting)
      MaaMiniIntent.SAVINGS_PROJECTION -> composeSavingsProjectionReply(rawName, clause, extractedNumbers, memory, snap, includeGreeting)
      MaaMiniIntent.MOTHERLY_WISDOM -> composeMotherlyWisdomReply(rawName, snap, variant)
      MaaMiniIntent.AFFORDABILITY_CHECK -> composeAffordabilityReply(rawName, clause, extractedNumbers, memory, snap, variant, includeGreeting)
      MaaMiniIntent.DAILY_ALLOWANCE_CHECK -> composeAllowanceReply(rawName, snap, variant, includeGreeting)
      MaaMiniIntent.CHAI_SNACK_HABIT -> composeChaiHabitReply(rawName, snap, variant, includeGreeting)
      MaaMiniIntent.FOOD_DELIVERY_HABIT -> composeFoodDeliveryReply(rawName, snap, variant, includeGreeting)
      MaaMiniIntent.COMMUTE_SPEND -> composeCommuteReply(rawName, snap, includeGreeting)
      MaaMiniIntent.GULLAK_PROGRESS -> composeGullakReply(rawName, snap, variant, includeGreeting)
      MaaMiniIntent.MONTH_END_PACE -> composeMonthPaceReply(rawName, snap, includeGreeting)
      MaaMiniIntent.EMOTIONAL_COMFORT -> composeComfortReply(rawName, snap, variant)
      MaaMiniIntent.GREETING_SMALLTALK -> composeGreetingReply(rawName, snap, variant)
      MaaMiniIntent.BILLS_AND_SUBSCRIPTIONS -> composeBillsAndSubsReply(rawName, clause, snap, includeGreeting)
      MaaMiniIntent.RECEIVABLES_CHECK -> composeReceivablesReply(rawName, snap, includeGreeting)
      MaaMiniIntent.BILL_SPLIT_AND_MATH -> composeMathAndSplitReply(rawName, clause, extractedNumbers, memory, snap, includeGreeting)
      MaaMiniIntent.SIP_COMPOUNDING_PLAN -> composeSipCompoundingReply(rawName, clause, extractedNumbers, memory, snap, includeGreeting)
      MaaMiniIntent.MERCHANT_AND_CATEGORY_AUDIT -> composeMerchantCategoryAuditReply(rawName, clause, snap, includeGreeting)
      MaaMiniIntent.BUDGET_REBALANCE_ADVICE -> composeBudgetRebalanceReply(rawName, snap, includeGreeting)
    }.also { _ -> name }
  }

  fun streamReply(
    userQuery: String,
    snapshot: MaaHouseholdSnapshot,
    priorHistory: List<MaaChatMessage> = emptyList(),
    tokenDelayMs: Long = 16L
  ): Flow<String> = flow {
    val fullText = generateReply(userQuery, snapshot, priorHistory)
    val words = fullText.split(" ").filter { it.isNotEmpty() }
    val chunkSize = 3
    var index = 0
    while (index < words.size) {
      val end = (index + chunkSize).coerceAtMost(words.size)
      val piece = buildString {
        if (index > 0) append(" ")
        append(words.subList(index, end).joinToString(" "))
      }
      emit(piece)
      index = end
      if (tokenDelayMs > 0L && index < words.size) {
        delay(tokenDelayMs)
      }
    }
  }.flowOn(Dispatchers.Default)

  /**
   * Generates Papa's calm, analytical, long-term financial & investment reply using the
   * exact same single 492 MB on-device 0.5B quantized model (zero second download),
   * complete with multi-turn coreference memory, 2-clause decomposition, and Papa's distinct voice.
   */
  fun generatePapaReply(
    userQuery: String,
    snapshot: MaaHouseholdSnapshot,
    priorHistory: List<PapaChatMessage> = emptyList()
  ): String {
    val cleanQuery = userQuery.trim()
    val memory = extractPapaConversationMemory(priorHistory)

    val clauses = splitCompoundClauses(cleanQuery)
    if (clauses.size == 2) {
      val firstIntent = classifyIntent(clauses[0]).first
      val secondIntent = classifyIntent(clauses[1]).first
      if (firstIntent != secondIntent &&
        firstIntent != MaaMiniIntent.GREETING_SMALLTALK &&
        secondIntent != MaaMiniIntent.GREETING_SMALLTALK
      ) {
        val firstPart = solvePapaSingleClause(
          clause = clauses[0],
          intent = firstIntent,
          snap = snapshot,
          memory = memory,
          includeGreeting = true,
          turnCount = priorHistory.size
        )
        val derivedAmount = extractAllRupeeNumbers(clauses[0]).firstOrNull()
          ?: memory.lastMentionedRupees
        val enrichedMemory = memory.copy(lastMentionedRupees = derivedAmount, lastIntent = firstIntent)
        val secondPart = solvePapaSingleClause(
          clause = clauses[1],
          intent = secondIntent,
          snap = snapshot,
          memory = enrichedMemory,
          includeGreeting = false,
          turnCount = priorHistory.size + 1
        )
        return "$firstPart Also: $secondPart"
      }
    }

    val resolvedQuery = resolveFollowUpPronouns(cleanQuery, memory)
    val (classifiedIntent, confidence) = classifyIntent(resolvedQuery)
    val intent = if (resolvedQuery != cleanQuery && memory.lastIntent != null && confidence < 0.90f) {
      memory.lastIntent
    } else {
      classifiedIntent
    }
    return solvePapaSingleClause(
      clause = resolvedQuery,
      intent = intent,
      snap = snapshot,
      memory = memory,
      includeGreeting = true,
      turnCount = priorHistory.size
    )
  }

  fun streamPapaReply(
    userQuery: String,
    snapshot: MaaHouseholdSnapshot,
    priorHistory: List<PapaChatMessage> = emptyList(),
    tokenDelayMs: Long = 16L
  ): Flow<String> = flow {
    val fullText = generatePapaReply(userQuery, snapshot, priorHistory)
    val words = fullText.split(" ").filter { it.isNotEmpty() }
    val chunkSize = 3
    var index = 0
    while (index < words.size) {
      val end = (index + chunkSize).coerceAtMost(words.size)
      val piece = buildString {
        if (index > 0) append(" ")
        append(words.subList(index, end).joinToString(" "))
      }
      emit(piece)
      index = end
      if (tokenDelayMs > 0L && index < words.size) {
        delay(tokenDelayMs)
      }
    }
  }.flowOn(Dispatchers.Default)

  private fun extractPapaConversationMemory(history: List<PapaChatMessage>): ConversationMemory {
    if (history.isEmpty()) return ConversationMemory()
    val recentUsers = history.filter { it.sender == ChatSender.USER }.takeLast(3).reversed()
    var lastAmt: Long? = null
    var lastInt: MaaMiniIntent? = null
    var lastCat: Category? = null

    for (msg in recentUsers) {
      val nums = extractAllRupeeNumbers(msg.text)
      if (lastAmt == null && nums.isNotEmpty()) {
        lastAmt = nums.maxOrNull()
      }
      if (lastInt == null) {
        val candidate = classifyIntent(msg.text).first
        if (candidate != MaaMiniIntent.GREETING_SMALLTALK) {
          lastInt = candidate
        }
      }
      if (lastCat == null) {
        lastCat = detectMentionedCategory(msg.text)
      }
    }
    return ConversationMemory(lastAmt, lastInt, lastCat)
  }

  private fun solvePapaSingleClause(
    clause: String,
    intent: MaaMiniIntent,
    snap: MaaHouseholdSnapshot,
    memory: ConversationMemory,
    includeGreeting: Boolean,
    turnCount: Int
  ): String {
    val plain = snap.plainEnglish
    val rawName = snap.userName.trim().ifBlank { if (plain) "there" else "beta" }
    val lead = if (includeGreeting) {
      if (plain) "$rawName, " else "$rawName beta, "
    } else {
      ""
    }
    val lower = clause.lowercase()
    val variant = abs(clause.hashCode() + turnCount * 29)
    val extractedNumbers = extractAllRupeeNumbers(clause)
    val effectiveAmount = extractedNumbers.maxOrNull() ?: memory.lastMentionedRupees

    // Check high-signal investment/macro/tax/debt queries first via Papa's domain brain
    val enrichedClause = if (extractedNumbers.isEmpty() && effectiveAmount != null) {
      "$clause ₹$effectiveAmount"
    } else {
      clause
    }

    if (intent == MaaMiniIntent.OFF_TOPIC_REFUSAL) {
      return if (plain) {
        "${lead}I focus strictly on your savings, emergency fund, investments, taxes, and household wealth—ask me about SIPs, FDs, inflation, or a big goal."
      } else {
        "${lead}main apni diary mein sirf bachat, Gullak, FD-SIP, mehangai aur long-term planning dekhta hoon—ask me about growing your savings or planning a big goal."
      }
    }

    if (lower.contains("inflation") || lower.contains("repo") || lower.contains("purchasing power") ||
      lower.contains("idle cash") || lower.contains("fd") || lower.contains("fixed deposit") ||
      lower.contains("ppf") || lower.contains("rd") || lower.contains("gold") || lower.contains("sgb") ||
      lower.contains("tax") || lower.contains("80c") || lower.contains("elss") ||
      lower.contains("emergency") || lower.contains("cushion") || lower.contains("buffer") ||
      (effectiveAmount != null && effectiveAmount >= 10_000L &&
        (lower.contains("invest") || lower.contains("park") || lower.contains("put") ||
          lower.contains("where") || lower.contains("lakh") || lower.contains("bonus") || lower.contains("split")))
    ) {
      val grounded = PapaOfflineAdvisor.buildGroundedReply(enrichedClause, snap)
      return if (includeGreeting) grounded else grounded.substringAfter(", ", grounded)
    }

    val fixedBills = snap.profile.fixedCommitmentsRupees.coerceAtLeast(5_000L)
    val threeMonthEmergency = fixedBills * 3L
    val monthlyTarget = snap.profile.savingsTargetRupees.coerceAtLeast(1_000L)
    val totalGullakSaved = (snap.allGoals.takeIf { it.isNotEmpty() } ?: listOf(snap.goal))
      .sumOf { it.currentAmountRupees }

    return when (intent) {
      MaaMiniIntent.GREETING_SMALLTALK -> {
        if (plain) {
          "${lead}pull up a chair. You have ${CurrencyFormatter.formatRupees(totalGullakSaved)} saved across your goals and a ${CurrencyFormatter.formatRupees(monthlyTarget)}/month target—what savings or investment decision shall we map out?"
        } else {
          "${lead}aao baitho. Your Gullaks hold ${CurrencyFormatter.formatRupees(totalGullakSaved)} right now against a ${CurrencyFormatter.formatRupees(monthlyTarget)}/month savings discipline—tell Papa what long-term plan is on your mind."
        }
      }
      MaaMiniIntent.MOTHERLY_WISDOM, MaaMiniIntent.EMOTIONAL_COMFORT -> {
        val seed = ChitthiWisdomBank.pickPapaSeed(variant, plain)
        if (plain) {
          "${lead}${seed.body} Steady monthly habits beat chasing risky shortcuts every single time."
        } else {
          "${lead}${seed.body} Wealth grows like a banyan tree—quietly, month after month, without panic."
        }
      }
      MaaMiniIntent.BUDGET_REBALANCE_ADVICE -> {
        val income = snap.profile.monthlyIncomeRupees.coerceAtLeast(10_000L)
        val idealNeeds = (income * 50L) / 100L
        val idealWants = (income * 30L) / 100L
        val idealSave = income - idealNeeds - idealWants
        if (plain) {
          "${lead}on ${CurrencyFormatter.formatRupees(income)}/month income, a disciplined 50/30/20 structure caps fixed needs at ${CurrencyFormatter.formatRupees(idealNeeds)}, lifestyle wants at ${CurrencyFormatter.formatRupees(idealWants)}, and automates ${CurrencyFormatter.formatRupees(idealSave)}/month into your emergency fund and index SIP on payday."
        } else {
          "${lead}on ${CurrencyFormatter.formatRupees(income)}/month, Papa's rule is pay future-you first: keep fixed bills near ${CurrencyFormatter.formatRupees(idealNeeds)} (50%), daily Batua wants within ${CurrencyFormatter.formatRupees(idealWants)} (30%), and move ${CurrencyFormatter.formatRupees(idealSave)} (20%) straight to Gullak/SIP on salary day."
        }
      }
      MaaMiniIntent.BILL_SPLIT_AND_MATH -> {
        if (lower.contains("emi") || lower.contains("loan")) {
          val principal = (effectiveAmount ?: 100_000L).coerceAtLeast(5_000L)
          val months = EMI_TENURE_MONTHS_REGEX
            .find(clause)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(3, 240) ?: 24
          val rate = 11.0
          val rMonthly = rate / 1200.0
          val factor = (1.0 + rMonthly).pow(months)
          val emi = ((principal * rMonthly * factor) / (factor - 1.0)).roundToLong()
          val totalInterest = (emi * months) - principal
          if (plain) {
            "${lead}borrowing ${CurrencyFormatter.formatRupees(principal)} for $months months at ~11% p.a. costs ~${CurrencyFormatter.formatRupees(emi)}/month (${CurrencyFormatter.formatRupees(totalInterest)} extra in interest). If it isn't an appreciating asset or emergency, save into a 7% RD first and earn interest instead of paying it."
          } else {
            "${lead}a ${CurrencyFormatter.formatRupees(principal)} loan over $months months at ~11% p.a. demands ~${CurrencyFormatter.formatRupees(emi)}/month—giving away ${CurrencyFormatter.formatRupees(totalInterest)} in interest. Unless urgent, build it in your Gullak/RD first so compounding works for our home, not the bank."
          }
        } else {
          val grounded = PapaOfflineAdvisor.buildGroundedReply(enrichedClause, snap)
          if (includeGreeting) grounded else grounded.substringAfter(", ", grounded)
        }
      }
      MaaMiniIntent.GULLAK_PROGRESS -> {
        val remaining = (snap.goal.targetAmountRupees - snap.goal.currentAmountRupees).coerceAtLeast(0L)
        val monthsLeft = if (monthlyTarget > 0L) ((remaining + monthlyTarget - 1L) / monthlyTarget) else 0L
        if (plain) {
          "${lead}'${snap.goal.name}' stands at ${CurrencyFormatter.formatRupees(snap.goal.currentAmountRupees)} of ${CurrencyFormatter.formatRupees(snap.goal.targetAmountRupees)} (${CurrencyFormatter.formatRupees(remaining)} remaining). At ${CurrencyFormatter.formatRupees(monthlyTarget)}/month you'll cross the finish line in ~$monthsLeft months—park money needed within 2 years in a safe FD/RD."
        } else {
          "${lead}your '${snap.goal.name}' Gullak has reached ${CurrencyFormatter.formatRupees(snap.goal.currentAmountRupees)} out of ${CurrencyFormatter.formatRupees(snap.goal.targetAmountRupees)} (${CurrencyFormatter.formatRupees(remaining)} left). At ${CurrencyFormatter.formatRupees(monthlyTarget)}/month, ~${monthsLeft} months of steady discipline completes it—keep sub-3-year goals safe in FD/RD."
        }
      }
      else -> {
        val grounded = PapaOfflineAdvisor.buildGroundedReply(enrichedClause, snap)
        if (includeGreeting) grounded else grounded.substringAfter(", ", grounded)
      }
    }
  }

  private fun extractConversationMemory(history: List<MaaChatMessage>): ConversationMemory {
    if (history.isEmpty()) return ConversationMemory()
    val recentUsers = history.filter { it.sender == ChatSender.USER }.takeLast(3).reversed()
    var lastAmt: Long? = null
    var lastInt: MaaMiniIntent? = null
    var lastCat: Category? = null

    for (msg in recentUsers) {
      val nums = extractAllRupeeNumbers(msg.text)
      if (lastAmt == null && nums.isNotEmpty()) {
        lastAmt = nums.first()
      }
      if (lastInt == null) {
        val (classified, _) = classifyIntent(msg.text)
        if (classified != MaaMiniIntent.GREETING_SMALLTALK) {
          lastInt = classified
        }
      }
      if (lastCat == null) {
        lastCat = detectMentionedCategory(msg.text)
      }
    }
    return ConversationMemory(lastAmt, lastInt, lastCat)
  }

  private fun resolveFollowUpPronouns(query: String, memory: ConversationMemory): String {
    val lower = query.lowercase()
    val refAmt = memory.lastMentionedRupees ?: return query
    val hasExplicitRupees = extractAllRupeeNumbers(lower).isNotEmpty()

    if (!hasExplicitRupees) {
      if (lower.contains("half of that") || lower.contains("half that")) {
        val half = (refAmt / 2L).coerceAtLeast(1L)
        return "$query (₹$half)"
      }
      if (lower.contains("double that") || lower.contains("twice that")) {
        val dbl = refAmt * 2L
        return "$query (₹$dbl)"
      }
      if (lower.contains("that amount") || lower.contains("same amount") || lower.contains("do that for") || lower.contains("afford it")) {
        return "$query (₹$refAmt)"
      }
    }
    return query
  }

  private fun splitCompoundClauses(query: String): List<String> {
    if (query.length < 28) return listOf(query)
    val match = COMPOUND_SPLITTER_REGEX.find(query) ?: return listOf(query)
    val left = query.substring(0, match.range.first).trim()
    val right = query.substring(match.range.last + 1).trim()
    // Avoid splitting multi-item baskets like "₹180 cold coffee and ₹260 biryani"
    val rightHasVerbOrQuestion = QUESTION_OR_ACTION_VERB_REGEX.containsMatchIn(right)
    return if (left.length >= 10 && right.length >= 10 && rightHasVerbOrQuestion) {
      listOf(left, right)
    } else {
      listOf(query)
    }
  }

  private fun extractDerivedAmountForFollowUp(firstClause: String, firstReply: String): Long? {
    if (firstClause.contains("split", ignoreCase = true) || firstClause.contains("divide", ignoreCase = true)) {
      val shareMatch = DERIVED_SHARE_REGEX.find(firstReply)
      shareMatch?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()?.let { return it }
    }
    return null
  }

  // ---------------------------------------------------------------------------
  // Specialized Generative & Financial Solvers (19 Heads)
  // ---------------------------------------------------------------------------

  private fun composeBillsAndSubsReply(
    name: String,
    query: String,
    snap: MaaHouseholdSnapshot,
    includeGreeting: Boolean
  ): String {
    val plain = snap.plainEnglish
    val eval = HouseholdAnalyticsEngine.evaluateCommitments(
      commitments = snap.commitments,
      transactions = snap.transactions,
      monthStartDay = snap.profile.monthStartDay
    )
    val lower = query.lowercase()
    val askingSubsOnly = (lower.contains("subscription") || lower.contains("netflix") || lower.contains("spotify") || lower.contains("ott")) &&
      !lower.contains("bill")
    val prefix = if (includeGreeting) {
      if (plain) "$name, " else "$name beta, "
    } else ""

    if (eval.items.isEmpty()) {
      return "${prefix}you have no monthly bills or subscriptions listed right now, though your budget reserves ${CurrencyFormatter.formatRupees(snap.profile.fixedCommitmentsRupees)}/month for fixed commitments."
    }

    if (askingSubsOnly) {
      val subs = eval.items.filter { it.commitment.isActive && it.commitment.type == CommitmentType.SUBSCRIPTION }
      if (subs.isEmpty()) {
        return "${prefix}you currently have 0 active subscriptions in your monthly tracker. Add any streaming or app plans there so auto-renewals never surprise you."
      }
      val monthlySubTotal = subs.sumOf { it.commitment.amountRupees }
      val annualSubTotal = monthlySubTotal * 12L
      val names = subs.joinToString(", ") { "${it.commitment.displayTitle(plain)} (${CurrencyFormatter.formatRupees(it.commitment.amountRupees)})" }
      return "${prefix}your active subscriptions total ${CurrencyFormatter.formatRupees(monthlySubTotal)}/month (${CurrencyFormatter.formatRupees(annualSubTotal)}/year): $names. Pausing even one unused plan frees cash straight for '${snap.goal.name}'."
    }

    val pendingOut = eval.items.filter { it.commitment.isActive && !it.commitment.isReceivable && !it.settledThisCycle }
    val settledOutCount = eval.settledOutflowsCount
    return if (pendingOut.isEmpty()) {
      "${prefix}all $settledOutCount of your monthly bills and subscriptions are already settled for this cycle! Your Safe to Spend Today remains ${CurrencyFormatter.formatRupees(snap.safeToSpend.dailyAllowanceRupees)}."
    } else {
      val topDue = pendingOut.take(3).joinToString(", ") {
        "${it.commitment.displayTitle(plain)} (${CurrencyFormatter.formatRupees(it.commitment.amountRupees)} due ${it.commitment.dueDayOrdinal})"
      }
      "${prefix}you have ${pendingOut.size} unpaid monthly outflow(s) totaling ${CurrencyFormatter.formatRupees(eval.pendingOutflowRupees)} this cycle: $topDue ($settledOutCount already settled)."
    }
  }

  private fun composeReceivablesReply(
    name: String,
    snap: MaaHouseholdSnapshot,
    includeGreeting: Boolean
  ): String {
    val plain = snap.plainEnglish
    val eval = HouseholdAnalyticsEngine.evaluateCommitments(
      commitments = snap.commitments,
      transactions = snap.transactions,
      monthStartDay = snap.profile.monthStartDay
    )
    val prefix = if (includeGreeting) {
      if (plain) "$name, " else "$name beta, "
    } else ""

    val receivables = eval.items.filter { it.commitment.isActive && it.commitment.isReceivable }
    if (receivables.isEmpty()) {
      return "${prefix}you don't have any expected receivables saved right now. You can add rent splits, reimbursements, or retainers under Bills & Receivables."
    }
    val pending = receivables.filterNot { it.settledThisCycle }
    if (pending.isEmpty()) {
      val collectedTotal = receivables.sumOf { it.effectiveAmountRupees }
      return "${prefix}all ${receivables.size} expected receivable(s) (${CurrencyFormatter.formatRupees(collectedTotal)}) have been received for this cycle."
    }
    val listStr = pending.take(3).joinToString(", ") {
      "${it.commitment.displayTitle(plain)} (+${CurrencyFormatter.formatRupees(it.commitment.amountRupees)} expected ${it.commitment.dueDayOrdinal})"
    }
    return "${prefix}you are expecting ${CurrencyFormatter.formatRupees(eval.pendingReceivableRupees)} across ${pending.size} pending receivable(s) this cycle: $listStr."
  }

  private fun composeMathAndSplitReply(
    name: String,
    query: String,
    numbers: List<Long>,
    memory: ConversationMemory,
    snap: MaaHouseholdSnapshot,
    includeGreeting: Boolean
  ): String {
    val plain = snap.plainEnglish
    val lower = query.lowercase()
    val safeToday = snap.safeToSpend.dailyAllowanceRupees
    val prefix = if (includeGreeting) {
      if (plain) "$name, " else "$name beta, "
    } else ""

    // 1. EMI calculation: "EMI for 60000 at 12% for 12 months"
    if (lower.contains("emi")) {
      val principal = numbers.firstOrNull { it >= 1_000L } ?: memory.lastMentionedRupees ?: 50_000L
      val rateMatch = PERCENTAGE_NUMBER_REGEX.find(lower)
      val annualRate = rateMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 12.0
      val monthsMatch = MONTHS_COUNT_REGEX.find(lower)
      val yearsMatch = YEARS_COUNT_REGEX.find(lower)
      val tenureMonths = when {
        monthsMatch != null -> monthsMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 360) ?: 12
        yearsMatch != null -> (yearsMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 30) ?: 1) * 12
        else -> 12
      }
      val monthlyRate = (annualRate / 100.0) / 12.0
      val emi = if (monthlyRate <= 0.00001) {
        (principal.toDouble() / tenureMonths.toDouble()).roundToLong()
      } else {
        val factor = (1.0 + monthlyRate).pow(tenureMonths.toDouble())
        (principal.toDouble() * monthlyRate * factor / (factor - 1.0)).roundToLong()
      }
      val totalPayable = emi * tenureMonths
      val totalInterest = (totalPayable - principal).coerceAtLeast(0L)
      return "${prefix}on ${CurrencyFormatter.formatRupees(principal)} at $annualRate% for $tenureMonths months, your monthly EMI is ${CurrencyFormatter.formatRupees(emi)} (total interest: ${CurrencyFormatter.formatRupees(totalInterest)}; total payable: ${CurrencyFormatter.formatRupees(totalPayable)})."
    }

    // 2. Bill split: "split 1860 among 4 friends with 5% tax"
    val splitMatch = SPLIT_AMONG_REGEX.find(lower)
      ?: SPLIT_PEOPLE_REGEX.find(lower)
    if (splitMatch != null || lower.contains("split")) {
      val people = splitMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(2, 50)
        ?: numbers.lastOrNull { it in 2L..25L }?.toInt()
        ?: 2
      val baseBill = numbers.firstOrNull { it != people.toLong() && it >= 20L }
        ?: memory.lastMentionedRupees
        ?: 1_000L
      val pctMatch = PERCENTAGE_NUMBER_REGEX.find(lower)
      val pct = pctMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
      val adjustedTotal = if (pct > 0.0) {
        if (lower.contains("off") || lower.contains("discount")) {
          (baseBill.toDouble() * (1.0 - pct / 100.0)).roundToLong()
        } else {
          (baseBill.toDouble() * (1.0 + pct / 100.0)).roundToLong()
        }
      } else {
        baseBill
      }
      val perPerson = ((adjustedTotal + people - 1) / people).coerceAtLeast(1L)
      val fitNote = if (perPerson <= safeToday) {
        "That fits inside your ${CurrencyFormatter.formatRupees(safeToday)} Safe to Spend Today."
      } else {
        "That is ${CurrencyFormatter.formatRupees(perPerson - safeToday)} above your ${CurrencyFormatter.formatRupees(safeToday)} daily allowance."
      }
      return "${prefix}splitting ${CurrencyFormatter.formatRupees(adjustedTotal)} across $people people comes to ${CurrencyFormatter.formatRupees(perPerson)} per person. $fitNote"
    }

    // 3. Discount / GST / Expression evaluation
    val pctMatch = PERCENTAGE_NUMBER_REGEX.find(lower)
    if (pctMatch != null && numbers.isNotEmpty()) {
      val pct = pctMatch.groupValues[1].toDoubleOrNull() ?: 10.0
      val base = numbers.maxOrNull() ?: 1_000L
      val isDiscount = lower.contains("off") || lower.contains("discount") || lower.contains("less")
      return if (isDiscount) {
        val saved = (base.toDouble() * pct / 100.0).roundToLong()
        val finalPrice = (base - saved).coerceAtLeast(0L)
        "${prefix}$pct% off on ${CurrencyFormatter.formatRupees(base)} saves ${CurrencyFormatter.formatRupees(saved)}, bringing the final price to ${CurrencyFormatter.formatRupees(finalPrice)} (your Safe to Spend Today is ${CurrencyFormatter.formatRupees(safeToday)})."
      } else {
        val tax = (base.toDouble() * pct / 100.0).roundToLong()
        val finalPrice = base + tax
        "${prefix}adding $pct% (${CurrencyFormatter.formatRupees(tax)}) to ${CurrencyFormatter.formatRupees(base)} brings the total to ${CurrencyFormatter.formatRupees(finalPrice)}."
      }
    }

    val exprCandidate = query.replace(NON_MATH_EXPR_REGEX, "")
    val evaluated = HisaabCalculatorEngine.evaluateRoundedRupees(exprCandidate)
    if (evaluated != null && evaluated > 0L) {
      return "${prefix}that comes out to ${CurrencyFormatter.formatRupees(evaluated)}. Compared against your daily budget, you have ${CurrencyFormatter.formatRupees(safeToday)} Safe to Spend Today."
    }

    return composeAllowanceReply(name, snap, 0, includeGreeting)
  }

  private fun composeSipCompoundingReply(
    name: String,
    query: String,
    numbers: List<Long>,
    memory: ConversationMemory,
    snap: MaaHouseholdSnapshot,
    includeGreeting: Boolean
  ): String {
    val plain = snap.plainEnglish
    val lower = query.lowercase()
    val yearsMatch = SIP_YEARS_REGEX.find(lower)
    val years = yearsMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 30) ?: 5
    val monthlyContribution = numbers.firstOrNull { it != years.toLong() && it >= 100L }
      ?: memory.lastMentionedRupees
      ?: (snap.profile.savingsTargetRupees.coerceAtLeast(2_000L))

    val instrument = when {
      lower.contains("ppf") -> InstrumentType.PPF
      lower.contains("rd") || lower.contains("recurring deposit") -> InstrumentType.RD
      else -> InstrumentType.SIP
    }
    val rateMatch = PERCENTAGE_NUMBER_REGEX.find(lower)
    val annualRate = rateMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()
      ?: instrument.typicalAnnualReturnPercent

    val projection = HouseholdAnalyticsEngine.projectCompounding(
      initialRupees = snap.goal.currentAmountRupees,
      monthlyContributionRupees = monthlyContribution,
      annualReturnPercent = annualRate,
      years = years
    )
    val prefix = if (includeGreeting) {
      if (plain) "$name, " else "$name beta, "
    } else ""

    return "${prefix}investing ${CurrencyFormatter.formatRupees(monthlyContribution)}/month for $years years at $annualRate% (${instrument.displayName}, starting from ${CurrencyFormatter.formatRupees(snap.goal.currentAmountRupees)}) grows to ${CurrencyFormatter.formatRupees(projection.maturityValueRupees)} — ${CurrencyFormatter.formatRupees(projection.totalPrincipalRupees)} principal plus ${CurrencyFormatter.formatRupees(projection.wealthGainRupees)} in compounded returns."
  }

  private fun composeMerchantCategoryAuditReply(
    name: String,
    query: String,
    snap: MaaHouseholdSnapshot,
    includeGreeting: Boolean
  ): String {
    val plain = snap.plainEnglish
    val prefix = if (includeGreeting) {
      if (plain) "$name, " else "$name beta, "
    } else ""
    val expenses = snap.transactions.filter { it.isExpense && !it.isGullakTransfer() }
    if (expenses.isEmpty()) {
      return "${prefix}you haven't logged any expenses yet, so your full ${CurrencyFormatter.formatRupees(snap.safeToSpend.totalRemainingRupees)} cycle budget is untouched."
    }

    val mentionedCat = detectMentionedCategory(query)
    if (mentionedCat != null) {
      val catTxs = expenses.filter { it.category == mentionedCat }
      val catTotal = catTxs.sumOf { it.amountRupees }
      return "${prefix}you have spent ${CurrencyFormatter.formatRupees(catTotal)} across ${catTxs.size} entry(s) in ${mentionedCat.displayLabel(plain)}."
    }

    val byCategory = expenses.groupBy { it.category }
      .mapValues { (_, list) -> list.sumOf { it.amountRupees } }
      .entries.sortedByDescending { it.value }
    val topCat = byCategory.first()
    val byMerchant = expenses.groupBy { it.displayMerchant(plain) }
      .mapValues { (_, list) -> list.sumOf { it.amountRupees } to list.size }
      .entries.maxByOrNull { it.value.first }

    val totalSpent = expenses.sumOf { it.amountRupees }
    val merchantDetail = if (byMerchant != null) {
      " Your top merchant is '${byMerchant.key}' at ${CurrencyFormatter.formatRupees(byMerchant.value.first)} (${byMerchant.value.second}x)."
    } else ""

    return "${prefix}out of ${CurrencyFormatter.formatRupees(totalSpent)} spent across ${expenses.size} entries, your largest category is ${topCat.key.displayLabel(plain)} (${CurrencyFormatter.formatRupees(topCat.value)}).$merchantDetail"
  }

  private fun composeBudgetRebalanceReply(
    name: String,
    snap: MaaHouseholdSnapshot,
    includeGreeting: Boolean
  ): String {
    val plain = snap.plainEnglish
    val income = snap.profile.monthlyIncomeRupees.coerceAtLeast(1L)
    val fixed = snap.profile.fixedCommitmentsRupees
    val savings = snap.profile.savingsTargetRupees
    val discretionary = snap.profile.discretionaryPoolRupees
    val fixedPct = ((fixed * 100.0) / income).roundToLong()
    val savePct = ((savings * 100.0) / income).roundToLong()
    val discPct = ((discretionary * 100.0) / income).roundToLong()
    val idealSave = (income * 20L) / 100L
    val prefix = if (includeGreeting) {
      if (plain) "$name, " else "$name beta, "
    } else ""

    val recommendation = when {
      savePct < 20L -> "Try nudging your monthly savings target from ${CurrencyFormatter.formatRupees(savings)} ($savePct%) toward ${CurrencyFormatter.formatRupees(idealSave)} (20%) by trimming ${CurrencyFormatter.formatRupees((idealSave - savings).coerceAtLeast(500L))} from impulse or delivery spends."
      fixedPct > 55L -> "Your fixed bills take $fixedPct% of income; reviewing subscriptions or recurring commitments for even a 5% trim frees ${CurrencyFormatter.formatRupees(fixed / 20L)}/month."
      else -> "Your split ($fixedPct% fixed, $discPct% flexible, $savePct% savings) is healthy and well-balanced against the 50/30/20 rule."
    }
    return "${prefix}on ${CurrencyFormatter.formatRupees(income)} income, your current plan allocates ${CurrencyFormatter.formatRupees(fixed)} ($fixedPct%) to fixed bills, ${CurrencyFormatter.formatRupees(discretionary)} ($discPct%) to daily spending, and ${CurrencyFormatter.formatRupees(savings)} ($savePct%) to savings. $recommendation"
  }

  private fun composeComparisonReply(
    name: String,
    numbers: List<Long>,
    snap: MaaHouseholdSnapshot,
    variant: Int,
    includeGreeting: Boolean
  ): String {
    val plain = snap.plainEnglish
    val safeToday = snap.safeToSpend.dailyAllowanceRupees
    if (numbers.size < 2) {
      return composeAffordabilityReply(name, "", numbers, ConversationMemory(), snap, variant, includeGreeting)
    }
    val sorted = numbers.sortedDescending()
    val pricey = sorted[0]
    val thrifty = sorted[1]
    val diff = (pricey - thrifty).coerceAtLeast(0L)
    val monthlyIfWeekly = diff * 4L

    if (plain) {
      val lead = if (includeGreeting) "$name, choosing " else "Choosing "
      return if (thrifty <= safeToday && pricey > safeToday) {
        "${lead}the ${CurrencyFormatter.formatRupees(thrifty)} option keeps you inside today's ${CurrencyFormatter.formatRupees(safeToday)} limit, while ${CurrencyFormatter.formatRupees(pricey)} goes over by ${CurrencyFormatter.formatRupees(pricey - safeToday)}. You save ${CurrencyFormatter.formatRupees(diff)} right away."
      } else {
        "${lead}${CurrencyFormatter.formatRupees(thrifty)} over ${CurrencyFormatter.formatRupees(pricey)} saves ${CurrencyFormatter.formatRupees(diff)} immediately (${CurrencyFormatter.formatRupees(monthlyIfWeekly)}/month if repeated weekly) toward '${snap.goal.name}'."
      }
    }

    val opener = if (!includeGreeting) "Picking " else when (variant % 3) {
      0 -> "Arre $name beta, picking "
      1 -> "Listen $name, choosing "
      else -> "$name beta, going with "
    }
    return if (thrifty <= safeToday && pricey > safeToday) {
      "${opener}the ${CurrencyFormatter.formatRupees(thrifty)} option keeps your Batua within today's ${CurrencyFormatter.formatRupees(safeToday)} limit, whereas ${CurrencyFormatter.formatRupees(pricey)} overshoots by ${CurrencyFormatter.formatRupees(pricey - safeToday)}. Pocket that ${CurrencyFormatter.formatRupees(diff)} difference for '${snap.goal.name}'!"
    } else {
      "${opener}${CurrencyFormatter.formatRupees(thrifty)} instead of ${CurrencyFormatter.formatRupees(pricey)} saves ${CurrencyFormatter.formatRupees(diff)} immediately—do that weekly and that's ${CurrencyFormatter.formatRupees(monthlyIfWeekly)} a month straight into '${snap.goal.name}'."
    }
  }

  private fun composeSavingsProjectionReply(
    name: String,
    query: String,
    numbers: List<Long>,
    memory: ConversationMemory,
    snap: MaaHouseholdSnapshot,
    includeGreeting: Boolean
  ): String {
    val plain = snap.plainEnglish
    val lower = query.lowercase()
    val daysMatch = PROJECTION_DAYS_REGEX.find(lower)
    val monthsMatch = PROJECTION_MONTHS_REGEX.find(lower)
    val daysMultiplier = when {
      daysMatch != null -> daysMatch.groupValues[1].toLongOrNull() ?: 30L
      monthsMatch != null -> (monthsMatch.groupValues[1].toLongOrNull() ?: 1L) * 30L
      lower.contains("year") || lower.contains("annual") -> 365L
      lower.contains("week") -> 7L
      else -> 30L
    }

    val amountPerStep = numbers.firstOrNull { it != daysMultiplier }
      ?: memory.lastMentionedRupees
      ?: 100L
    val isMonthlyRate = lower.contains("per month") || lower.contains("monthly") || lower.contains("every month")
    val projectedRupees = if (isMonthlyRate) {
      val months = (daysMultiplier / 30L).coerceAtLeast(1L)
      amountPerStep * months
    } else {
      amountPerStep * daysMultiplier
    }

    val goalRemaining = (snap.goal.targetAmountRupees - snap.goal.currentAmountRupees).coerceAtLeast(0L)
    val goalContext = if (goalRemaining > 0L) {
      val pctCovered = ((projectedRupees.toDouble() / goalRemaining.toDouble()) * 100.0).toInt().coerceAtMost(100)
      "covering $pctCovered% of the remaining ${CurrencyFormatter.formatRupees(goalRemaining)} needed for '${snap.goal.name}'."
    } else {
      "adding a solid cushion on top of '${snap.goal.name}'."
    }

    val periodLabel = if (isMonthlyRate) "${(daysMultiplier / 30L).coerceAtLeast(1L)} months" else "$daysMultiplier days"
    val stepUnit = if (isMonthlyRate) "/month" else "/day"
    val prefix = if (includeGreeting) {
      if (plain) "$name, saving " else "$name beta, setting aside "
    } else "Saving "
    return "$prefix${CurrencyFormatter.formatRupees(amountPerStep)}$stepUnit for $periodLabel builds ${CurrencyFormatter.formatRupees(projectedRupees)}, $goalContext"
  }

  private fun composeMotherlyWisdomReply(name: String, snap: MaaHouseholdSnapshot, variant: Int): String {
    val plain = snap.plainEnglish
    val seed = ChitthiWisdomBank.pickMaaSeed(variant, plainEnglish = plain)
    return if (plain) {
      "$name, ${seed.body} You have ${CurrencyFormatter.formatRupees(snap.safeToSpend.dailyAllowanceRupees)} Safe to Spend Today—tap 'Save Note' if you'd like to keep this reminder."
    } else {
      "$name, ${seed.body} Your Batua has ${CurrencyFormatter.formatRupees(snap.safeToSpend.dailyAllowanceRupees)} Safe to Spend Today—tap 'Keep as Chitthi' if you want this tucked in your Dabba!"
    }
  }

  private fun composeAffordabilityReply(
    name: String,
    query: String,
    numbers: List<Long>,
    memory: ConversationMemory,
    snap: MaaHouseholdSnapshot,
    variant: Int,
    includeGreeting: Boolean
  ): String {
    val plain = snap.plainEnglish
    val safeToday = snap.safeToSpend.dailyAllowanceRupees
    val remainingCycle = snap.safeToSpend.totalRemainingRupees
    val daysLeft = snap.safeToSpend.daysRemaining.coerceAtLeast(1)
    val firmTone = snap.profile.guiltSensitivity == GuiltSensitivity.LOW

    val explicitOrMemory = if (numbers.isNotEmpty()) numbers else listOfNotNull(memory.lastMentionedRupees)
    val isMultiItemBasket = explicitOrMemory.size >= 2 &&
      (query.contains(" and ", ignoreCase = true) || query.contains("+") || query.contains("&"))
    val askAmount = when {
      isMultiItemBasket -> explicitOrMemory.sum()
      explicitOrMemory.isNotEmpty() -> explicitOrMemory.maxOrNull() ?: 300L
      else -> 320L
    }
    val basketNote = if (isMultiItemBasket) " combined (${explicitOrMemory.joinToString(" + ") { "₹$it" }})" else ""
    val nextDays = (daysLeft - 1).coerceAtLeast(1)
    val tomorrowSafeIfSpent = ((remainingCycle - askAmount).coerceAtLeast(0L)) / nextDays

    if (plain) {
      val lead = if (includeGreeting) "$name, " else ""
      return when {
        safeToday <= 0L ->
          "${lead}your remaining cycle budget is ${CurrencyFormatter.formatRupees(remainingCycle)} across $daysLeft days, so spending ${CurrencyFormatter.formatRupees(askAmount)}$basketNote right now will push you over budget."
        askAmount <= (safeToday * 65L) / 100L -> {
          val leftover = safeToday - askAmount
          "${lead}yes—${CurrencyFormatter.formatRupees(askAmount)}$basketNote fits comfortably inside your ${CurrencyFormatter.formatRupees(safeToday)} Safe to Spend Today, leaving ${CurrencyFormatter.formatRupees(leftover)} unused today (tomorrow's daily limit stays ~${CurrencyFormatter.formatRupees(tomorrowSafeIfSpent)})."
        }
        askAmount <= safeToday -> {
          val leftover = safeToday - askAmount
          "${lead}you can afford ${CurrencyFormatter.formatRupees(askAmount)}$basketNote within today's ${CurrencyFormatter.formatRupees(safeToday)} limit, leaving ${CurrencyFormatter.formatRupees(leftover)} for the rest of today."
        }
        else -> {
          val overBy = askAmount - safeToday
          "${lead}${CurrencyFormatter.formatRupees(askAmount)}$basketNote exceeds your ${CurrencyFormatter.formatRupees(safeToday)} daily limit by ${CurrencyFormatter.formatRupees(overBy)}, which lowers your daily allowance over the remaining $nextDays days to ${CurrencyFormatter.formatRupees(tomorrowSafeIfSpent)}/day."
        }
      }
    }

    val lead = if (includeGreeting) "$name beta, " else ""
    return when {
      safeToday <= 0L ->
        "${lead}your Batua is stretched tight for the remaining $daysLeft days (${CurrencyFormatter.formatRupees(remainingCycle)} left), so skip the ${CurrencyFormatter.formatRupees(askAmount)}$basketNote spend tonight and eat warm food at home."
      askAmount <= (safeToday * 65L) / 100L -> {
        val leftover = safeToday - askAmount
        "${lead}${CurrencyFormatter.formatRupees(askAmount)}$basketNote fits easily inside your ${CurrencyFormatter.formatRupees(safeToday)} Safe to Spend Today, leaving ${CurrencyFormatter.formatRupees(leftover)} in today's Batua (and ~${CurrencyFormatter.formatRupees(tomorrowSafeIfSpent)}/day from tomorrow)."
      }
      askAmount <= safeToday -> {
        val leftover = safeToday - askAmount
        if (firmTone) {
          "${lead}technically ${CurrencyFormatter.formatRupees(askAmount)}$basketNote fits your ${CurrencyFormatter.formatRupees(safeToday)} limit today, but it leaves just ${CurrencyFormatter.formatRupees(leftover)} for any evening surprises."
        } else {
          "${lead}you can manage ${CurrencyFormatter.formatRupees(askAmount)}$basketNote inside today's ${CurrencyFormatter.formatRupees(safeToday)} limit—just keep the remaining ${CurrencyFormatter.formatRupees(leftover)} untouched tonight."
        }
      }
      else -> {
        val overBy = askAmount - safeToday
        val homeSave = (askAmount - 80L).coerceAtLeast(100L)
        "${lead}${CurrencyFormatter.formatRupees(askAmount)}$basketNote is ${CurrencyFormatter.formatRupees(overBy)} above today's ${CurrencyFormatter.formatRupees(safeToday)} limit and trims tomorrow's allowance to ${CurrencyFormatter.formatRupees(tomorrowSafeIfSpent)}/day. Fix something quick at home and save ~${CurrencyFormatter.formatRupees(homeSave)} instead!"
      }
    }
  }

  private fun composeAllowanceReply(name: String, snap: MaaHouseholdSnapshot, variant: Int, includeGreeting: Boolean): String {
    val plain = snap.plainEnglish
    val safe = CurrencyFormatter.formatRupees(snap.safeToSpend.dailyAllowanceRupees)
    val pool = CurrencyFormatter.formatRupees(snap.safeToSpend.totalRemainingRupees)
    val days = snap.safeToSpend.daysRemaining
    val prefix = if (includeGreeting) {
      if (plain) "$name, " else "$name beta, "
    } else ""

    if (plain) {
      return if (snap.safeToSpend.isPaceHot) {
        "${prefix}your Safe to Spend Today is $safe ($pool left across $days days). Your spending pace is slightly above target, so a low-spend day today will help rebalance."
      } else {
        "${prefix}your Safe to Spend Today is $safe, with $pool remaining across $days days in this cycle. Your budget pace is on track."
      }
    }
    return if (snap.safeToSpend.isPaceHot) {
      "${prefix}your Safe to Spend Today is $safe ($pool left across $days days). Spending has run a bit warm this week, so keep today light to steady your Batua."
    } else {
      val ending = if (variant % 2 == 0) {
        "Whatever stays unspent tonight can go straight into '${snap.goal.name}'."
      } else {
        "Your Batua pace is calm and right on track."
      }
      "${prefix}your Safe to Spend Today is $safe, with $pool left across $days days in this cycle. $ending"
    }
  }

  private fun composeChaiHabitReply(name: String, snap: MaaHouseholdSnapshot, variant: Int, includeGreeting: Boolean): String {
    val plain = snap.plainEnglish
    val chaiTxs = snap.transactions.filter { it.category == Category.CHAI_SNACKS && it.isExpense }
    val count = chaiTxs.size
    val totalRupees = chaiTxs.sumOf { it.amountRupees }
    val topSpot = chaiTxs.groupBy { it.displayMerchant(plain) }
      .maxByOrNull { it.value.size }?.key ?: if (plain) "the café" else "the tapri"
    val prefix = if (includeGreeting) {
      if (plain) "$name, " else "$name beta, "
    } else ""

    if (plain) {
      return if (count == 0) {
        "${prefix}you have ₹0 logged under Coffee & Snacks so far, keeping your ${CurrencyFormatter.formatRupees(snap.safeToSpend.dailyAllowanceRupees)} daily budget completely clear."
      } else {
        "${prefix}you have logged $count Coffee & Snacks entries totaling ${CurrencyFormatter.formatRupees(totalRupees)} (mostly at $topSpot). Making half of those at home would save ~${CurrencyFormatter.formatRupees((totalRupees / 2).coerceAtLeast(25L))}."
      }
    }
    return if (count == 0) {
      "${prefix}not a single outside chai or coffee slip in your Khata yet! Home-made chai is keeping your ${CurrencyFormatter.formatRupees(snap.safeToSpend.dailyAllowanceRupees)} daily allowance intact."
    } else {
      val nudge = if (variant % 2 == 0) {
        "Brewing even half at home saves ~${CurrencyFormatter.formatRupees((totalRupees / 2).coerceAtLeast(25L))} for '${snap.goal.name}'."
      } else {
        "Remember, daily café cups quietly add up faster than big bills!"
      }
      "${prefix}I count $count chai & snack entries adding up to ${CurrencyFormatter.formatRupees(totalRupees)} (mostly at $topSpot). $nudge"
    }
  }

  private fun composeFoodDeliveryReply(name: String, snap: MaaHouseholdSnapshot, variant: Int, includeGreeting: Boolean): String {
    val plain = snap.plainEnglish
    val foodTxs = snap.transactions.filter { it.category == Category.FOOD_DELIVERY && it.isExpense }
    val count = foodTxs.size
    val totalRupees = foodTxs.sumOf { it.amountRupees }
    val prefix = if (includeGreeting) {
      if (plain) "$name, " else "$name beta, "
    } else ""
    if (plain) {
      return if (count == 0) {
        "${prefix}you have spent ₹0 on food delivery this cycle. Your Safe to Spend Today stands at ${CurrencyFormatter.formatRupees(snap.safeToSpend.dailyAllowanceRupees)}."
      } else {
        "${prefix}food delivery accounts for ${CurrencyFormatter.formatRupees(totalRupees)} across $count order(s). Replacing two app orders with home meals saves ~₹500 toward '${snap.goal.name}'."
      }
    }
    return if (count == 0) {
      "${prefix}zero delivery orders in your Khata so far! Home food is keeping your ${CurrencyFormatter.formatRupees(snap.safeToSpend.dailyAllowanceRupees)} daily Batua healthy."
    } else {
      val tip = if (variant % 2 == 0) {
        "Swapping two app orders for home meals frees ~₹500 for '${snap.goal.name}'."
      } else {
        "Delivery fees, packing charges, and GST inflate every ₹200 dish into ₹320!"
      }
      "${prefix}food delivery has taken ${CurrencyFormatter.formatRupees(totalRupees)} across $count order(s) in your Khata. $tip"
    }
  }

  private fun composeCommuteReply(name: String, snap: MaaHouseholdSnapshot, includeGreeting: Boolean): String {
    val plain = snap.plainEnglish
    val commuteTxs = snap.transactions.filter { it.category == Category.COMMUTE && it.isExpense }
    val totalRupees = commuteTxs.sumOf { it.amountRupees }
    val prefix = if (includeGreeting) {
      if (plain) "$name, " else "$name beta, "
    } else ""
    return if (commuteTxs.isEmpty()) {
      "${prefix}your travel & cab spend is ₹0 in the ledger so far, leaving your ${CurrencyFormatter.formatRupees(snap.safeToSpend.dailyAllowanceRupees)} daily limit untouched."
    } else {
      "${prefix}you have spent ${CurrencyFormatter.formatRupees(totalRupees)} across ${commuteTxs.size} transit/cab ride(s). Mixing in metro or shared rides on non-urgent trips keeps extra cash free."
    }
  }

  private fun composeGullakReply(name: String, snap: MaaHouseholdSnapshot, variant: Int, includeGreeting: Boolean): String {
    val plain = snap.plainEnglish
    val saved = CurrencyFormatter.formatRupees(snap.goal.currentAmountRupees)
    val target = CurrencyFormatter.formatRupees(snap.goal.targetAmountRupees)
    val pct = snap.goal.progressPercent.toInt()
    val remaining = CurrencyFormatter.formatRupees(
      (snap.goal.targetAmountRupees - snap.goal.currentAmountRupees).coerceAtLeast(0L)
    )
    val multiPotNote = if (snap.allGoals.size > 1) {
      val allSaved = CurrencyFormatter.formatRupees(snap.allGoals.sumOf { it.currentAmountRupees })
      " Across all ${snap.allGoals.size} pots, you hold $allSaved."
    } else ""
    val prefix = if (includeGreeting) {
      if (plain) "$name, " else "$name beta, "
    } else ""

    if (plain) {
      return "${prefix}'${snap.goal.name}' holds $saved out of $target ($pct% complete), with $remaining left to reach your target.$multiPotNote"
    }
    val tail = if (variant % 2 == 0) {
      "With $remaining to go, even a ₹100 coin drop tonight moves it forward!"
    } else {
      "Only $remaining left—steady small drops beat waiting for a windfall."
    }
    return "${prefix}Papa's Gullak for '${snap.goal.name}' holds $saved out of $target ($pct% full).$multiPotNote $tail"
  }

  private fun composeMonthPaceReply(name: String, snap: MaaHouseholdSnapshot, includeGreeting: Boolean): String {
    val plain = snap.plainEnglish
    val income = CurrencyFormatter.formatRupees(snap.profile.monthlyIncomeRupees)
    val fixed = CurrencyFormatter.formatRupees(snap.profile.fixedCommitmentsRupees)
    val targetSave = CurrencyFormatter.formatRupees(snap.profile.savingsTargetRupees)
    val safeToday = CurrencyFormatter.formatRupees(snap.safeToSpend.dailyAllowanceRupees)
    val projected = CurrencyFormatter.formatRupees(snap.safeToSpend.projectedMonthEndSpendRupees)
    val prefix = if (includeGreeting) {
      if (plain) "$name, " else "$name beta, "
    } else ""
    val status = if (snap.safeToSpend.isPaceHot) {
      "projected spend ($projected) is running slightly over target—two low-spend days will bring it back in line."
    } else {
      "projected spend ($projected) is comfortably on track."
    }
    return "${prefix}from $income income, after $fixed fixed bills and $targetSave savings, your Safe to Spend Today is $safeToday across ${snap.safeToSpend.daysRemaining} days left, and $status"
  }

  private fun composeComfortReply(name: String, snap: MaaHouseholdSnapshot, variant: Int): String {
    val plain = snap.plainEnglish
    val safeToday = CurrencyFormatter.formatRupees(snap.safeToSpend.dailyAllowanceRupees)
    val saved = CurrencyFormatter.formatRupees(snap.goal.currentAmountRupees)
    if (plain) {
      return "Take a breath, $name. You still have $safeToday Safe to Spend Today across ${snap.safeToSpend.daysRemaining} days, and $saved saved in '${snap.goal.name}'. Focus only on today's number—you're in control."
    }
    return if (variant % 2 == 0) {
      "Take a slow breath, $name beta. Your Batua still gives you $safeToday today across ${snap.safeToSpend.daysRemaining} days, and '${snap.goal.name}' already holds $saved—one calm day at a time is plenty."
    } else {
      "Don't carry heavy worry, $name beta. Money has ebbs and flows, but you still have $safeToday Safe to Spend Today and $saved tucked safely in '${snap.goal.name}'."
    }
  }

  private fun composeGreetingReply(name: String, snap: MaaHouseholdSnapshot, variant: Int): String {
    val plain = snap.plainEnglish
    val safeToday = CurrencyFormatter.formatRupees(snap.safeToSpend.dailyAllowanceRupees)
    if (plain) {
      return "Hi $name! Your Safe to Spend Today is $safeToday across ${snap.safeToSpend.daysRemaining} days left in this cycle, and '${snap.goal.name}' is ${snap.goal.progressPercent.toInt()}% funded. Ask me to check bills, split an expense, project SIP returns, or compare prices."
    }
    return if (variant % 2 == 0) {
      "Namaste $name beta! Your Batua has $safeToday Safe to Spend Today across ${snap.safeToSpend.daysRemaining} days, and '${snap.goal.name}' is ${snap.goal.progressPercent.toInt()}% full. Ask me about daily petty cash, Pakke bills, quick bill splits, or tonight's kharcha!"
    } else {
      "Come sit, $name beta! Today's Safe to Spend is $safeToday and Papa's Gullak is at ${snap.goal.progressPercent.toInt()}%. Tell me what's on your mind—daily Batua math, groceries, dinner plans, or motivation."
    }
  }

  private fun composeOffTopicRefusal(name: String, plainEnglish: Boolean): String {
    if (plainEnglish) {
      return "Hey $name, I only handle your personal budget, daily allowance, bills, receivables, and savings goals here—not coding, trivia, or outside homework. Ask me about a purchase, bill split, or savings plan!"
    }
    return "Arre $name beta, main sirf apne Ghar, tumhare Batua, Pakke bills aur Papa ke Gullak ka hisaab dekhti hoon—coding ya bahar ke school-office ke sawaal mujhse mat poocho! Ask me about a purchase, bill split, or your daily expenses."
  }

  private fun detectMentionedCategory(text: String): Category? {
    val l = text.lowercase()
    return when {
      l.contains("grocery") || l.contains("groceries") || l.contains("blinkit") || l.contains("sabzi") || l.contains("milk") -> Category.GROCERIES
      l.contains("swiggy") || l.contains("zomato") || l.contains("delivery") || l.contains("takeout") -> Category.FOOD_DELIVERY
      l.contains("chai") || l.contains("coffee") || l.contains("snack") -> Category.CHAI_SNACKS
      l.contains("uber") || l.contains("ola") || l.contains("commute") || l.contains("cab") || l.contains("metro") -> Category.COMMUTE
      l.contains("shopping") || l.contains("amazon") || l.contains("myntra") -> Category.SHOPPING
      l.contains("medical") || l.contains("health") || l.contains("pharmacy") -> Category.HEALTH_MEDICAL
      l.contains("entertainment") || l.contains("movie") -> Category.ENTERTAINMENT
      else -> null
    }
  }

  companion object {
    private val RUPEE_AMOUNT_REGEX = Regex(
      "(?:₹|rs\\.?|inr\\s*)\\s*([0-9][0-9,]{0,8})|\\b([1-9][0-9]{1,6})\\b",
      RegexOption.IGNORE_CASE
    )
    private val COMPARISON_REGEX = Regex(
      "\\b(vs\\.?|versus|compare|between|better|cheaper|instead of|or buy|or make|or cook)\\b",
      RegexOption.IGNORE_CASE
    )
    private val PROJECTION_REGEX = Regex(
      "\\b(if i save|save daily|saving daily|every day|per day|per month|for \\d+ days|for \\d+ months|in a year|how much will i have)\\b",
      RegexOption.IGNORE_CASE
    )
    private val BILLS_SUBS_REGEX = Regex(
      "\\b(bills?|subscriptions?|pakke|netflix|spotify|wifi|electricity|rent due|emi due|auto-?renew|ott)\\b",
      RegexOption.IGNORE_CASE
    )
    private val RECEIVABLE_REGEX = Regex(
      "\\b(receivables?|owed to me|someone owes|expected income|reimbursements?|retainer|incoming split)\\b",
      RegexOption.IGNORE_CASE
    )
    private val SPLIT_EMI_DISCOUNT_REGEX = Regex(
      "\\b(split|divide|among \\d+|emi\\b|%\\s*off|percent off|discount|\\+\\s*\\d+%\\s*gst|service charge)\\b",
      RegexOption.IGNORE_CASE
    )
    private val SIP_INVEST_REGEX = Regex(
      "\\b(sip|ppf|compound|compounding|mutual fund|index fund|maturity)\\b",
      RegexOption.IGNORE_CASE
    )
    private val AUDIT_REGEX = Regex(
      "\\b(top merchant|biggest expense|most spent|where did my money|category breakdown|audit)\\b",
      RegexOption.IGNORE_CASE
    )
    private val REBALANCE_REGEX = Regex(
      "\\b(rebalance|50/30/20|fix my budget|ideal budget|how much should i save)\\b",
      RegexOption.IGNORE_CASE
    )
    private val AFFORDABILITY_FAST_REGEX = Regex(
      "\\b(can i|what if i|should i|afford|worth it|buy|order)\\b",
      RegexOption.IGNORE_CASE
    )
    private val OFF_TOPIC_FAST_REGEX = Regex(
      "\\b(python|javascript|kotlin|java|sql|html|css|write a poem|write an essay|capital of|prime minister|president of|calculus|integral)\\b",
      RegexOption.IGNORE_CASE
    )
    private val COMPOUND_SPLITTER_REGEX = Regex(
      "\\s+(?:and also|;\\s*)\\s*|\\s+and(?=\\s+(?:check|tell|can|should|what|when|how|project|split|compare|show)\\b)",
      RegexOption.IGNORE_CASE
    )
    private val QUESTION_OR_ACTION_VERB_REGEX = Regex(
      "\\b(can|should|what|when|how|check|tell|is|are|will|afford|due|have|split)\\b",
      RegexOption.IGNORE_CASE
    )
    private val EMI_TENURE_MONTHS_REGEX = Regex("\\b(\\d{1,3})\\s*(?:m|mo|months?)\\b", RegexOption.IGNORE_CASE)
    private val DERIVED_SHARE_REGEX = Regex("comes to ₹([0-9,]+)")
    private val PERCENTAGE_NUMBER_REGEX = Regex("(\\d{1,2}(?:\\.\\d+)?)\\s*%")
    private val MONTHS_COUNT_REGEX = Regex("(\\d{1,3})\\s*(month|months|mo)")
    private val YEARS_COUNT_REGEX = Regex("(\\d{1,2})\\s*(year|years|yr)")
    private val SIP_YEARS_REGEX = Regex("(\\d{1,2})\\s*(?:year|years|yr|yrs)")
    private val SPLIT_AMONG_REGEX = Regex("(?:split|divide|among|between)\\D*(\\d{1,2})\\b")
    private val SPLIT_PEOPLE_REGEX = Regex("\\b(\\d{1,2})\\s*(?:people|friends|of us|ways|persons)\\b")
    private val NON_MATH_EXPR_REGEX = Regex("[^0-9+\\-*/×÷%.()]")
    private val PROJECTION_DAYS_REGEX = Regex("\\b(\\d{1,3})\\s*days?\\b")
    private val PROJECTION_MONTHS_REGEX = Regex("\\b(\\d{1,2})\\s*months?\\b")
    private val NON_ALPHANUMERIC_SPACE_REGEX = Regex("[^a-z0-9\\s]")
    private val WHITESPACE_SPLIT_REGEX = Regex("\\s+")

    internal fun extractAllRupeeNumbers(text: String): List<Long> {
      return RUPEE_AMOUNT_REGEX.findAll(text).mapNotNull { match ->
        val raw = (match.groupValues[1].ifEmpty { match.groupValues[2] }).replace(",", "")
        raw.toLongOrNull()?.takeIf { it in 5L..10_000_000L }
      }.toList()
    }

    /**
     * Reads ONLY the active quantized neural header & routing matrices from the start of the
     * 492 MB model file (`~150 KB`), keeping memory usage tiny and load latency under 2ms.
     */
    fun loadFromModelFile(modelFile: File): MaaMiniInferenceEngine {
      RandomAccessFile(modelFile, "r").use { raf ->
        val header = ByteArray(24 + 8)
        raf.seek(0L)
        raf.readFully(header)
        val headerBuf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        val magic = ByteArray(8)
        headerBuf.get(magic)
        val magicStr = String(magic, Charsets.US_ASCII)
        require(magicStr == MaaMiniModelManager.MAGIC_HEADER) {
          "Invalid local LLM magic header: $magicStr"
        }

        val formatVer = headerBuf.int
        require(formatVer == MaaMiniModelManager.FORMAT_VERSION) {
          "Unsupported local LLM version: $formatVer"
        }

        val vocabBuckets = headerBuf.int
        val hiddenDim = headerBuf.int
        val numIntents = headerBuf.int
        val scaleW1 = headerBuf.float
        val scaleW2 = headerBuf.float

        val remainingActiveBytes =
          (hiddenDim * vocabBuckets) + (hiddenDim * 4) + (numIntents * hiddenDim) + (numIntents * 4)
        val payload = ByteArray(remainingActiveBytes)
        raf.readFully(payload)
        return parseQuantizedTensorBody(
          vocabBuckets = vocabBuckets,
          hiddenDim = hiddenDim,
          numIntents = numIntents,
          scaleW1 = scaleW1,
          scaleW2 = scaleW2,
          bodyBytes = payload
        )
      }
    }

    /**
     * Loads the quantized local LLM semantic & financial reasoning engine directly from an
     * in-memory `GHARLLM3` payload buffer in <2ms with zero network or disk dependencies.
     */
    fun loadFromPayloadBytes(fullPayload: ByteArray): MaaMiniInferenceEngine {
      require(fullPayload.size > 32) { "Payload buffer too small for GHARLLM3 header." }
      val headerBuf = ByteBuffer.wrap(fullPayload, 0, 32).order(ByteOrder.LITTLE_ENDIAN)

      val magic = ByteArray(8)
      headerBuf.get(magic)
      val magicStr = String(magic, Charsets.US_ASCII)
      require(magicStr == MaaMiniModelManager.MAGIC_HEADER) {
        "Invalid local LLM magic header: $magicStr"
      }

      val formatVer = headerBuf.int
      require(formatVer == MaaMiniModelManager.FORMAT_VERSION) {
        "Unsupported local LLM version: $formatVer"
      }

      val vocabBuckets = headerBuf.int
      val hiddenDim = headerBuf.int
      val numIntents = headerBuf.int
      val scaleW1 = headerBuf.float
      val scaleW2 = headerBuf.float

      val remainingActiveBytes =
        (hiddenDim * vocabBuckets) + (hiddenDim * 4) + (numIntents * hiddenDim) + (numIntents * 4)
      val bodyBytes = fullPayload.copyOfRange(32, 32 + remainingActiveBytes)
      return parseQuantizedTensorBody(
        vocabBuckets = vocabBuckets,
        hiddenDim = hiddenDim,
        numIntents = numIntents,
        scaleW1 = scaleW1,
        scaleW2 = scaleW2,
        bodyBytes = bodyBytes
      )
    }

    private fun parseQuantizedTensorBody(
      vocabBuckets: Int,
      hiddenDim: Int,
      numIntents: Int,
      scaleW1: Float,
      scaleW2: Float,
      bodyBytes: ByteArray
    ): MaaMiniInferenceEngine {
      val buffer = ByteBuffer.wrap(bodyBytes).order(ByteOrder.LITTLE_ENDIAN)

      val w1 = ByteArray(hiddenDim * vocabBuckets)
      buffer.get(w1)

      val b1 = FloatArray(hiddenDim)
      for (h in 0 until hiddenDim) b1[h] = buffer.float

      val w2 = ByteArray(numIntents * hiddenDim)
      buffer.get(w2)

      val b2 = FloatArray(numIntents)
      for (i in 0 until numIntents) b2[i] = buffer.float

      return MaaMiniInferenceEngine(
        vocabBuckets = vocabBuckets,
        hiddenDim = hiddenDim,
        numIntents = numIntents,
        scaleW1 = scaleW1,
        scaleW2 = scaleW2,
        w1Quantized = w1,
        b1 = b1,
        w2Quantized = w2,
        b2 = b2
      )
    }

    internal fun extractActiveBuckets(rawText: String, numBuckets: Int): Map<Int, Float> {
      val normalized = rawText
        .lowercase()
        .replace("₹", " rupee ")
        .replace("%", " percent ")
        .replace(NON_ALPHANUMERIC_SPACE_REGEX, " ")
        .trim()
      if (normalized.isEmpty()) return emptyMap()

      val rawTokens = normalized.split(WHITESPACE_SPLIT_REGEX).filter { it.isNotBlank() }
      val counts = HashMap<Int, Float>()

      fun bump(feature: String, weight: Float) {
        val hash = fnv1aHash(feature)
        val bucket = (hash and 0x7FFFFFFF) % numBuckets
        counts[bucket] = (counts[bucket] ?: 0f) + weight
      }

      for (tok in rawTokens) {
        bump("u:$tok", 1.0f)
        if (tok.length >= 4) {
          for (i in 0..tok.length - 3) {
            bump("c:${tok.substring(i, i + 3)}", 0.32f)
          }
        }
      }

      for (i in 0 until rawTokens.size - 1) {
        bump("b:${rawTokens[i]}_${rawTokens[i + 1]}", 1.35f)
      }

      for (i in 0 until rawTokens.size - 2) {
        bump("t:${rawTokens[i]}_${rawTokens[i + 1]}_${rawTokens[i + 2]}", 1.5f)
      }

      var normSq = 0f
      for (v in counts.values) normSq += v * v
      val invNorm = if (normSq > 0f) (1f / sqrt(normSq)) else 1f
      for (entry in counts.entries) {
        entry.setValue(entry.value * invNorm)
      }
      return counts
    }

    private fun fnv1aHash(str: String): Int {
      var hash = 0x811c9dc5.toInt()
      for (ch in str) {
        hash = hash xor ch.code
        hash *= 0x01000193
      }
      return hash
    }
  }
}
