package com.example.core.engine

import com.example.core.model.Category
import com.example.core.model.Transaction
import com.example.core.util.DateUtils
import java.util.Calendar

data class PatternAnalysis(
  val frequencyByMerchant7Days: Map<String, Int>,
  val frequencyByCategory7Days: Map<Category, Int>,
  val totalSpendByCategory7DaysPaise: Map<Category, Long>,
  val lateNightSpendCount7Days: Int,
  val hasMedicalExpense7Days: Boolean,
  val mostFrequentMerchant7Days: Pair<String, Int>?,
  val topLeakCategory7Days: Pair<Category, Int>?,
  val recentConsecutiveHabitMerchant: String?,
  val totalTransactions7Days: Int
)

object PatternDetector {

  private val MEDICAL_KEYWORDS = listOf(
    "hospital", "pharmacy", "medical", "clinic", "apollo", "medplus",
    "doctor", "pathology", "diagnostics", "medicines", "health"
  )

  fun analyze(transactions: List<Transaction>): PatternAnalysis {
    val sevenDaysAgo = DateUtils.getSevenDaysAgoTimestamp()
    val recent7Days = transactions.filter { it.isExpense && it.timestamp >= sevenDaysAgo }

    val merchantCounts = mutableMapOf<String, Int>()
    val categoryCounts = mutableMapOf<Category, Int>()
    val categorySpend = mutableMapOf<Category, Long>()
    var lateNightCount = 0
    var hasMedical = false

    val cal = Calendar.getInstance()

    for (tx in recent7Days) {
      val merchantNormalized = tx.merchant.trim()
      merchantCounts[merchantNormalized] = (merchantCounts[merchantNormalized] ?: 0) + 1
      categoryCounts[tx.category] = (categoryCounts[tx.category] ?: 0) + 1
      categorySpend[tx.category] = (categorySpend[tx.category] ?: 0L) + tx.amountPaise

      // Late night: hour >= 22 or hour < 5
      cal.timeInMillis = tx.timestamp
      val hour = cal.get(Calendar.HOUR_OF_DAY)
      if (hour >= 22 || hour < 5) {
        lateNightCount++
      }

      // Medical check
      val raw = (tx.merchant + " " + (tx.rawText ?: "")).lowercase()
      if (tx.category == Category.HEALTH_MEDICAL || MEDICAL_KEYWORDS.any { raw.contains(it) }) {
        hasMedical = true
      }
    }

    val topMerchant = merchantCounts.maxByOrNull { it.value }?.toPair()
    val topCategory = categoryCounts.maxByOrNull { it.value }?.toPair()

    return PatternAnalysis(
      frequencyByMerchant7Days = merchantCounts,
      frequencyByCategory7Days = categoryCounts,
      totalSpendByCategory7DaysPaise = categorySpend,
      lateNightSpendCount7Days = lateNightCount,
      hasMedicalExpense7Days = hasMedical,
      mostFrequentMerchant7Days = topMerchant,
      topLeakCategory7Days = topCategory,
      recentConsecutiveHabitMerchant = topMerchant?.let { if (it.second >= 4) it.first else null },
      totalTransactions7Days = recent7Days.size
    )
  }
}
