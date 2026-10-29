package com.example.core.engine

import androidx.compose.runtime.Immutable
import com.example.core.model.BudgetProfile
import com.example.core.util.DateUtils
import java.util.Calendar

@Immutable
data class SafeToSpendResult(
  val dailyAllowancePaise: Long,
  val daysRemaining: Int,
  val totalRemainingPaise: Long,
  val discretionaryPoolPaise: Long,
  val paceRatio: Double,
  val projectedMonthEndSpendPaise: Long,
  val walletThicknessPercent: Float // 0.0f to 1.0f for visual wallet thinning
) {
  val dailyAllowanceRupees: Long get() = dailyAllowancePaise / 100L
  val totalRemainingRupees: Long get() = totalRemainingPaise / 100L
  val discretionaryPoolRupees: Long get() = discretionaryPoolPaise / 100L
  val projectedMonthEndSpendRupees: Long get() = projectedMonthEndSpendPaise / 100L

  val isPaceHot: Boolean get() = paceRatio > 1.3
  val isUnderBudget: Boolean get() = paceRatio in 0.0..1.0
  val isTight: Boolean get() = totalRemainingPaise < (dailyAllowancePaise * 2) || totalRemainingPaise <= 0L
}

object SafeToSpendCalculator {

  fun calculate(profile: BudgetProfile, spentSoFarPaise: Long, extraIncomePaise: Long = 0L): SafeToSpendResult {
    val discretionaryPool = (profile.monthlyIncomePaise + extraIncomePaise - profile.fixedCommitmentsPaise - profile.savingsTargetPaise)
      .coerceAtLeast(0L)

    val totalRemaining = (discretionaryPool - spentSoFarPaise)
    val daysRemaining = DateUtils.getDaysRemainingInMonth(profile.monthStartDay)

    val calendar = Calendar.getInstance()
    val totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val daysPassed = (totalDaysInMonth - daysRemaining + 1).coerceAtLeast(1)

    // Expected daily discretionary budget across the whole month
    val expectedDailyBudget = if (totalDaysInMonth > 0) discretionaryPool.toDouble() / totalDaysInMonth else 1.0

    // Actual average daily spend so far
    val actualDailySpend = spentSoFarPaise.toDouble() / daysPassed

    val paceRatio = if (expectedDailyBudget > 0) {
      actualDailySpend / expectedDailyBudget
    } else {
      1.0
    }

    val dailyAllowance = if (daysRemaining > 0 && totalRemaining > 0) {
      totalRemaining / daysRemaining
    } else {
      0L
    }

    val projectedMonthEndSpend = spentSoFarPaise + (actualDailySpend * daysRemaining).toLong()

    val walletThickness = if (discretionaryPool > 0) {
      (totalRemaining.toFloat() / discretionaryPool.toFloat()).coerceIn(0.05f, 1.0f)
    } else {
      0.1f
    }

    return SafeToSpendResult(
      dailyAllowancePaise = dailyAllowance,
      daysRemaining = daysRemaining,
      totalRemainingPaise = totalRemaining,
      discretionaryPoolPaise = discretionaryPool,
      paceRatio = paceRatio,
      projectedMonthEndSpendPaise = projectedMonthEndSpend,
      walletThicknessPercent = walletThickness
    )
  }
}
