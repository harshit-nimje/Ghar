package com.example.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object DateUtils {

  private val timeFormatLocal = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)
  }

  private val shortDateFormatLocal = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("d MMM", Locale.ENGLISH)
  }

  private val monthYearFormatLocal = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
  }

  private val dayShortLabelLocal = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("EEE", Locale.ENGLISH)
  }

  @Volatile
  private var cachedTodayStartMs: Long = 0L

  @Volatile
  private var cachedTomorrowStartMs: Long = 0L

  @Volatile
  private var cachedYesterdayStartMs: Long = 0L

  private val relativeDateCache = ConcurrentHashMap<Long, String>(128)

  private fun ensureDayBoundsCurrent(nowMs: Long = System.currentTimeMillis()) {
    val start = cachedTodayStartMs
    val end = cachedTomorrowStartMs
    if (nowMs in start until end && end > start) return

    synchronized(this) {
      if (nowMs in cachedTodayStartMs until cachedTomorrowStartMs && cachedTomorrowStartMs > cachedTodayStartMs) {
        return
      }
      val cal = Calendar.getInstance().apply {
        timeInMillis = nowMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val todayStart = cal.timeInMillis
      cal.add(Calendar.DAY_OF_YEAR, 1)
      val tomorrowStart = cal.timeInMillis
      cal.add(Calendar.DAY_OF_YEAR, -2)
      val yesterdayStart = cal.timeInMillis

      cachedTodayStartMs = todayStart
      cachedTomorrowStartMs = tomorrowStart
      cachedYesterdayStartMs = yesterdayStart
      relativeDateCache.clear()
    }
  }

  fun getStartOfTodayTimestamp(nowMs: Long = System.currentTimeMillis()): Long {
    ensureDayBoundsCurrent(nowMs)
    return cachedTodayStartMs
  }

  fun formatTime(timestamp: Long): String {
    return timeFormatLocal.get()!!.format(Date(timestamp))
  }

  fun formatShortDayLabel(timestamp: Long): String {
    return dayShortLabelLocal.get()!!.format(Date(timestamp))
  }

  fun formatRelativeDate(timestamp: Long): String {
    ensureDayBoundsCurrent()
    // Bucket cache key to minute granularity so identical timestamps hit O(1) without unbounded growth
    val minuteKey = timestamp / 60_000L
    relativeDateCache[minuteKey]?.let { return it }

    val formatted = when {
      timestamp >= cachedTodayStartMs && timestamp < cachedTomorrowStartMs -> {
        "Today, ${formatTime(timestamp)}"
      }
      timestamp >= cachedYesterdayStartMs && timestamp < cachedTodayStartMs -> {
        "Yesterday, ${formatTime(timestamp)}"
      }
      else -> {
        val dateObj = Date(timestamp)
        "${shortDateFormatLocal.get()!!.format(dateObj)}, ${timeFormatLocal.get()!!.format(dateObj)}"
      }
    }

    if (relativeDateCache.size > 512) {
      relativeDateCache.clear()
    }
    relativeDateCache[minuteKey] = formatted
    return formatted
  }

  fun formatMonthYear(timestamp: Long = System.currentTimeMillis()): String {
    return monthYearFormatLocal.get()!!.format(Date(timestamp))
  }

  /**
   * Calculates days remaining in the current calendar month cycle starting from monthStartDay.
   */
  fun getDaysRemainingInMonth(monthStartDay: Int = 1): Int {
    val calendar = Calendar.getInstance()
    val todayDay = calendar.get(Calendar.DAY_OF_MONTH)
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val safeStart = monthStartDay.coerceIn(1, 28)

    return if (safeStart == 1) {
      (maxDays - todayDay + 1).coerceAtLeast(1)
    } else if (todayDay >= safeStart) {
      (maxDays - todayDay + safeStart).coerceAtLeast(1)
    } else {
      (safeStart - todayDay).coerceAtLeast(1)
    }
  }

  fun getStartOfMonthTimestamp(monthStartDay: Int = 1): Long {
    val cal = Calendar.getInstance()
    val safeStart = monthStartDay.coerceIn(1, 28)
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)
    if (todayDay < safeStart) {
      cal.add(Calendar.MONTH, -1)
    }
    cal.set(Calendar.DAY_OF_MONTH, safeStart.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
  }

  fun getSevenDaysAgoTimestamp(): Long {
    ensureDayBoundsCurrent()
    return cachedTodayStartMs - (6L * 24L * 3600_000L)
  }

  fun getThirtyDaysAgoTimestamp(): Long {
    ensureDayBoundsCurrent()
    return cachedTodayStartMs - (29L * 24L * 3600_000L)
  }

  fun startOfTodayMillis(nowMs: Long = System.currentTimeMillis()): Long {
    return getStartOfTodayTimestamp(nowMs)
  }

  fun startOfBudgetCycleMillis(monthStartDay: Int = 1, nowMs: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
    val safeStart = monthStartDay.coerceIn(1, 28)
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)
    if (todayDay < safeStart) {
      cal.add(Calendar.MONTH, -1)
    }
    cal.set(Calendar.DAY_OF_MONTH, safeStart.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
  }

  fun daysAgoMillis(days: Int, nowMs: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = nowMs
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis - (days.coerceAtLeast(0).toLong() * 24L * 3600_000L)
  }
}
