package com.example.core.engine

import com.example.core.model.BudgetProfile
import com.example.core.model.Category
import com.example.core.model.GuiltSensitivity
import com.example.core.model.Insight
import com.example.core.model.InsightPriority
import com.example.core.model.SavingsGoal
import com.example.core.model.Transaction
import com.example.core.util.CurrencyFormatter

data class CooldownState(
  val lastInsightTimestamp: Long = 0L,
  val weeklyCount: Int = 0,
  val templateLastFired: Map<String, Long> = emptyMap()
)

object InsightEngine {

  fun evaluate(
    profile: BudgetProfile,
    goal: SavingsGoal,
    transactions: List<Transaction>,
    safeToSpend: SafeToSpendResult,
    cooldownState: CooldownState,
    forcePreview: Boolean = false,
    plainEnglish: Boolean = false
  ): Insight? {
    val pattern = PatternDetector.analyze(transactions)

    // Rule 7: Context-aware silence. Medical bill or hard emergency -> stay quiet.
    if (pattern.hasMedicalExpense7Days && !forcePreview) {
      return null
    }

    val now = System.currentTimeMillis()
    val isWithinDailyCooldown = (now - cooldownState.lastInsightTimestamp) < (20 * 3600 * 1000L) // 20 hours
    if (!forcePreview && isWithinDailyCooldown) {
      return null
    }

    if (!forcePreview && cooldownState.weeklyCount >= 3) {
      return null
    }

    val thresholdMultiplier = when (profile.guiltSensitivity) {
      GuiltSensitivity.LOW -> 1
      GuiltSensitivity.MEDIUM -> 1
      GuiltSensitivity.HIGH -> 2
      GuiltSensitivity.NONE -> 999
    }

    if (profile.guiltSensitivity == GuiltSensitivity.NONE && !forcePreview) {
      return null
    }

    val candidates = mutableListOf<Insight>()

    // 1. Safety check: Pace is hot or safe-to-spend is tight
    if (safeToSpend.isPaceHot) {
      candidates.add(
        Insight(
          templateId = InsightTemplates.TEMPLATE_PACE_HOT.id,
          renderedText = InsightTemplates.TEMPLATE_PACE_HOT.textTemplate,
          actionOffer = InsightTemplates.TEMPLATE_PACE_HOT.actionOfferTemplate,
          tone = InsightTemplates.TEMPLATE_PACE_HOT.tone,
          priority = InsightPriority.SAFETY
        )
      )
    } else if (safeToSpend.isTight && safeToSpend.totalRemainingPaise > 0) {
      val text = InsightTemplates.TEMPLATE_SAFE_TO_SPEND_TIGHT.textTemplate
        .replace("{amount}", CurrencyFormatter.formatRupees(safeToSpend.dailyAllowanceRupees, false))
      candidates.add(
        Insight(
          templateId = InsightTemplates.TEMPLATE_SAFE_TO_SPEND_TIGHT.id,
          renderedText = text,
          actionOffer = InsightTemplates.TEMPLATE_SAFE_TO_SPEND_TIGHT.actionOfferTemplate,
          tone = InsightTemplates.TEMPLATE_SAFE_TO_SPEND_TIGHT.tone,
          priority = InsightPriority.SAFETY
        )
      )
    }

    // 2. Frequency leak check: Chai / Coffee
    val chaiCount = pattern.frequencyByCategory7Days[Category.CHAI_SNACKS] ?: 0
    val chaiThreshold = if (thresholdMultiplier == 1) 3 else 4
    if (chaiCount >= chaiThreshold) {
      val text = InsightTemplates.TEMPLATE_REPEATED_CHAI.textTemplate
        .replace("{n}", chaiCount.toString())
      candidates.add(
        Insight(
          templateId = InsightTemplates.TEMPLATE_REPEATED_CHAI.id,
          renderedText = text,
          actionOffer = InsightTemplates.TEMPLATE_REPEATED_CHAI.actionOfferTemplate,
          tone = InsightTemplates.TEMPLATE_REPEATED_CHAI.tone,
          priority = InsightPriority.SAVINGS_OPPORTUNITY
        )
      )
    }

    // 3. Food Delivery creep
    val deliveryCount = pattern.frequencyByCategory7Days[Category.FOOD_DELIVERY] ?: 0
    val deliveryThreshold = if (thresholdMultiplier == 1) 3 else 4
    if (deliveryCount >= deliveryThreshold) {
      val text = InsightTemplates.TEMPLATE_FOOD_DELIVERY_CREEP.textTemplate
        .replace("{n}", deliveryCount.toString())
      candidates.add(
        Insight(
          templateId = InsightTemplates.TEMPLATE_FOOD_DELIVERY_CREEP.id,
          renderedText = text,
          actionOffer = InsightTemplates.TEMPLATE_FOOD_DELIVERY_CREEP.actionOfferTemplate,
          tone = InsightTemplates.TEMPLATE_FOOD_DELIVERY_CREEP.tone,
          priority = InsightPriority.SAVINGS_OPPORTUNITY
        )
      )
    }

    // 4. Cab habit
    val cabCount = pattern.frequencyByCategory7Days[Category.COMMUTE] ?: 0
    val cabThreshold = if (thresholdMultiplier == 1) 4 else 5
    if (cabCount >= cabThreshold) {
      val text = InsightTemplates.TEMPLATE_CAB_HABIT.textTemplate
        .replace("{n}", cabCount.toString())
      candidates.add(
        Insight(
          templateId = InsightTemplates.TEMPLATE_CAB_HABIT.id,
          renderedText = text,
          actionOffer = InsightTemplates.TEMPLATE_CAB_HABIT.actionOfferTemplate,
          tone = InsightTemplates.TEMPLATE_CAB_HABIT.tone,
          priority = InsightPriority.SAVINGS_OPPORTUNITY
        )
      )
    }

    // 5. Late night ordering
    if (pattern.lateNightSpendCount7Days >= 2) {
      candidates.add(
        Insight(
          templateId = InsightTemplates.TEMPLATE_LATE_NIGHT.id,
          renderedText = InsightTemplates.TEMPLATE_LATE_NIGHT.textTemplate,
          actionOffer = InsightTemplates.TEMPLATE_LATE_NIGHT.actionOfferTemplate,
          tone = InsightTemplates.TEMPLATE_LATE_NIGHT.tone,
          priority = InsightPriority.SAVINGS_OPPORTUNITY
        )
      )
    }

    // 6. Praise & Milestone
    if (goal.progressPercent >= 50f && goal.progressPercent < 55f) {
      val text = InsightTemplates.TEMPLATE_GOAL_PROGRESS.textTemplate
        .replace("{goal}", goal.name)
      candidates.add(
        Insight(
          templateId = InsightTemplates.TEMPLATE_GOAL_PROGRESS.id,
          renderedText = text,
          actionOffer = InsightTemplates.TEMPLATE_GOAL_PROGRESS.actionOfferTemplate?.replace("{goal}", goal.name),
          tone = InsightTemplates.TEMPLATE_GOAL_PROGRESS.tone,
          priority = InsightPriority.PRAISE
        )
      )
    } else if (goal.currentAmountRupees >= 10000 && (goal.currentAmountRupees % 10000) < 500) {
      val text = InsightTemplates.TEMPLATE_GULLAK_MILESTONE.textTemplate
        .replace("{amount}", CurrencyFormatter.formatRupees(goal.currentAmountRupees, false))
      candidates.add(
        Insight(
          templateId = InsightTemplates.TEMPLATE_GULLAK_MILESTONE.id,
          renderedText = text,
          actionOffer = InsightTemplates.TEMPLATE_GULLAK_MILESTONE.actionOfferTemplate,
          tone = InsightTemplates.TEMPLATE_GULLAK_MILESTONE.tone,
          priority = InsightPriority.PRAISE
        )
      )
    } else if (safeToSpend.isUnderBudget && pattern.totalTransactions7Days >= 3) {
      val offer = if (plainEnglish) "Add ₹250 to Savings Pot" else "Add ₹250 to Papa's Gullak"
      candidates.add(
        Insight(
          templateId = InsightTemplates.TEMPLATE_UNDER_BUDGET_STREAK.id,
          renderedText = InsightTemplates.TEMPLATE_UNDER_BUDGET_STREAK.textTemplate,
          actionOffer = offer,
          tone = InsightTemplates.TEMPLATE_UNDER_BUDGET_STREAK.tone,
          priority = InsightPriority.PRAISE
        )
      )
    }

    // Prioritize candidate: SAFETY > SAVINGS_OPPORTUNITY > PRAISE
    return candidates.minByOrNull { it.priority.ordinal }
  }
}
