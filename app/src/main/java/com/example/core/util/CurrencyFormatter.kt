package com.example.core.util

import java.text.DecimalFormat
import java.util.Locale

object CurrencyFormatter {

  /**
   * Formats paise into Indian currency string: e.g. 150000_00 -> "₹1,50,000"
   */
  fun formatPaise(paise: Long, includeSymbol: Boolean = true): String {
    val rupees = paise / 100L
    return formatRupees(rupees, includeSymbol)
  }

  /**
   * Formats rupees into standard Indian format (e.g., ₹1,20,000 or ₹4,500)
   */
  fun formatRupees(rupees: Long, includeSymbol: Boolean = true): String {
    val isNegative = rupees < 0
    val absValue = kotlin.math.abs(rupees).toString()

    val formatted = if (absValue.length <= 3) {
      absValue
    } else {
      val lastThree = absValue.substring(absValue.length - 3)
      val remaining = absValue.substring(0, absValue.length - 3)
      val grouped = remaining.reversed().chunked(2).joinToString(",").reversed()
      "$grouped,$lastThree"
    }

    val prefix = if (isNegative) "- " else ""
    val symbol = if (includeSymbol) "₹" else ""
    return "$prefix$symbol$formatted"
  }

  fun formatCompactRupees(rupees: Long): String {
    return when {
      rupees >= 1_00_00_000L -> String.format(Locale.US, "₹%.1f Cr", rupees / 1_00_00_000.0)
      rupees >= 1_00_000L -> String.format(Locale.US, "₹%.1f L", rupees / 1_00_000.0)
      rupees >= 1_000L -> String.format(Locale.US, "₹%.1f k", rupees / 1_000.0)
      else -> "₹$rupees"
    }
  }
}
