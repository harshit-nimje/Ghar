package com.example.core.engine

import com.example.core.model.InsightPriority
import com.example.core.model.InsightTone

data class TemplateDefinition(
  val id: String,
  val priority: InsightPriority,
  val tone: InsightTone,
  val cooldownDays: Int,
  val textTemplate: String,
  val actionOfferTemplate: String?
)

object InsightTemplates {

  val TEMPLATE_REPEATED_CHAI = TemplateDefinition(
    id = "repeated_chai",
    priority = InsightPriority.SAVINGS_OPPORTUNITY,
    tone = InsightTone.OFFER,
    cooldownDays = 3,
    textTemplate = "You bought coffee/tea outside {n} times this week. Making one at home tomorrow keeps extra room in your daily budget.",
    actionOfferTemplate = "Make coffee/tea at home tomorrow"
  )

  val TEMPLATE_FOOD_DELIVERY_CREEP = TemplateDefinition(
    id = "food_delivery_creep",
    priority = InsightPriority.SAVINGS_OPPORTUNITY,
    tone = InsightTone.OFFER,
    cooldownDays = 4,
    textTemplate = "You ordered food delivery {n} times this week. Cooking one meal at home tonight will lower your weekly spend.",
    actionOfferTemplate = "Cook dinner at home tonight"
  )

  val TEMPLATE_CAB_HABIT = TemplateDefinition(
    id = "cab_habit",
    priority = InsightPriority.SAVINGS_OPPORTUNITY,
    tone = InsightTone.QUESTION,
    cooldownDays = 4,
    textTemplate = "You logged {n} cab/ride trips this week. Using public transit once tomorrow can reduce commute costs.",
    actionOfferTemplate = "Consider public transit"
  )

  val TEMPLATE_DAILY_HABIT = TemplateDefinition(
    id = "daily_habit",
    priority = InsightPriority.SAVINGS_OPPORTUNITY,
    tone = InsightTone.QUESTION,
    cooldownDays = 5,
    textTemplate = "{merchant} has appeared almost daily this week. Spacing it out to twice a week lowers recurring spend.",
    actionOfferTemplate = "Save ₹{amount} by spacing it out"
  )

  val TEMPLATE_LATE_NIGHT = TemplateDefinition(
    id = "late_night",
    priority = InsightPriority.SAVINGS_OPPORTUNITY,
    tone = InsightTone.WARM,
    cooldownDays = 3,
    textTemplate = "Two late-night orders were logged this week. Keeping snacks at home avoids late delivery fees.",
    actionOfferTemplate = "Keep snacks stocked at home"
  )

  val TEMPLATE_PACE_HOT = TemplateDefinition(
    id = "pace_hot",
    priority = InsightPriority.SAFETY,
    tone = InsightTone.REASSURING,
    cooldownDays = 2,
    textTemplate = "Your monthly spending pace is running above your daily average target. Review category totals to stay on budget.",
    actionOfferTemplate = "Review monthly spending"
  )

  val TEMPLATE_SAFE_TO_SPEND_TIGHT = TemplateDefinition(
    id = "safe_to_spend_tight",
    priority = InsightPriority.SAFETY,
    tone = InsightTone.PRACTICAL,
    cooldownDays = 2,
    textTemplate = "Safe to spend today is ₹{amount}. Keeping today's discretionary expenses low will bring your cycle back on track.",
    actionOfferTemplate = "Keep discretionary spend low today"
  )

  val TEMPLATE_UNDER_BUDGET_STREAK = TemplateDefinition(
    id = "under_budget_streak",
    priority = InsightPriority.PRAISE,
    tone = InsightTone.PRAISE,
    cooldownDays = 5,
    textTemplate = "Your spending has stayed within your daily budget target this week.",
    actionOfferTemplate = "Add ₹{amount} to savings"
  )

  val TEMPLATE_FIRST_SAVE = TemplateDefinition(
    id = "first_save",
    priority = InsightPriority.PRAISE,
    tone = InsightTone.PRAISE,
    cooldownDays = 7,
    textTemplate = "First savings deposit recorded. Consistent contributions steadily build your goal.",
    actionOfferTemplate = "View savings progress"
  )

  val TEMPLATE_GULLAK_MILESTONE = TemplateDefinition(
    id = "gullak_milestone",
    priority = InsightPriority.PRAISE,
    tone = InsightTone.PRAISE,
    cooldownDays = 7,
    textTemplate = "Your savings goal just crossed ₹{amount}.",
    actionOfferTemplate = "Check projected returns"
  )

  val TEMPLATE_GOAL_PROGRESS = TemplateDefinition(
    id = "goal_progress",
    priority = InsightPriority.PRAISE,
    tone = InsightTone.PRAISE,
    cooldownDays = 5,
    textTemplate = "You have reached 50% of your '{goal}' savings target.",
    actionOfferTemplate = "View {goal} progress"
  )

  val TEMPLATE_QUIET_APPROVAL = TemplateDefinition(
    id = "quiet_approval",
    priority = InsightPriority.PRAISE,
    tone = InsightTone.SILENCE,
    cooldownDays = 1,
    textTemplate = "Daily spending is within your budget target.",
    actionOfferTemplate = null
  )
}

