package com.example.capture.parser

import com.example.core.model.Category

object MerchantLookupTable {

  private val merchantMap = mapOf(
    // Chai & Coffee
    "chai" to Category.CHAI_SNACKS,
    "chaayos" to Category.CHAI_SNACKS,
    "chai point" to Category.CHAI_SNACKS,
    "tea" to Category.CHAI_SNACKS,
    "tapri" to Category.CHAI_SNACKS,
    "starbucks" to Category.CHAI_SNACKS,
    "third wave" to Category.CHAI_SNACKS,
    "blue tokai" to Category.CHAI_SNACKS,
    "cafe" to Category.CHAI_SNACKS,

    // Food delivery & Dining
    "swiggy" to Category.FOOD_DELIVERY,
    "zomato" to Category.FOOD_DELIVERY,
    "eatclub" to Category.FOOD_DELIVERY,
    "dominos" to Category.FOOD_DELIVERY,
    "mcdonalds" to Category.FOOD_DELIVERY,
    "burger king" to Category.FOOD_DELIVERY,
    "kfc" to Category.FOOD_DELIVERY,
    "restaurant" to Category.FOOD_DELIVERY,

    // Commute & Rides
    "uber" to Category.COMMUTE,
    "ola" to Category.COMMUTE,
    "rapido" to Category.COMMUTE,
    "metro" to Category.COMMUTE,
    "irctc" to Category.COMMUTE,
    "petrol" to Category.COMMUTE,
    "fuel" to Category.COMMUTE,

    // Groceries
    "blinkit" to Category.GROCERIES,
    "zepto" to Category.GROCERIES,
    "instamart" to Category.GROCERIES,
    "bigbasket" to Category.GROCERIES,
    "dmart" to Category.GROCERIES,
    "supermarket" to Category.GROCERIES,

    // Shopping & eCommerce
    "amazon" to Category.SHOPPING,
    "flipkart" to Category.SHOPPING,
    "myntra" to Category.SHOPPING,
    "ajio" to Category.SHOPPING,
    "nykaa" to Category.SHOPPING,
    "zara" to Category.SHOPPING,
    "uniqlo" to Category.SHOPPING,

    // Subscriptions
    "netflix" to Category.ENTERTAINMENT,
    "spotify" to Category.ENTERTAINMENT,
    "hotstar" to Category.ENTERTAINMENT,
    "prime" to Category.ENTERTAINMENT,
    "youtube" to Category.ENTERTAINMENT,
    "apple" to Category.ENTERTAINMENT,

    // Medical & Health
    "apollo" to Category.HEALTH_MEDICAL,
    "pharmacy" to Category.HEALTH_MEDICAL,
    "medplus" to Category.HEALTH_MEDICAL,
    "hospital" to Category.HEALTH_MEDICAL,
    "clinic" to Category.HEALTH_MEDICAL,
    "1mg" to Category.HEALTH_MEDICAL,
    "pharmeasy" to Category.HEALTH_MEDICAL,

    // Bills & Utilities
    "airtel" to Category.BILLS_UTILITIES,
    "jio" to Category.BILLS_UTILITIES,
    "bescom" to Category.BILLS_UTILITIES,
    "electricity" to Category.BILLS_UTILITIES,
    "broadband" to Category.BILLS_UTILITIES,
    "wifi" to Category.BILLS_UTILITIES,
    "insurance" to Category.BILLS_UTILITIES,
    "rent" to Category.BILLS_UTILITIES,

    // Income / Salary
    "salary" to Category.ALLOWANCE_INCOME,
    "payroll" to Category.ALLOWANCE_INCOME,
    "dividend" to Category.ALLOWANCE_INCOME,
    "refund" to Category.ALLOWANCE_INCOME
  )

  fun lookupCategory(merchantOrText: String, isCredit: Boolean = false): Category {
    if (isCredit) return Category.ALLOWANCE_INCOME
    val lower = merchantOrText.lowercase()
    for ((keyword, category) in merchantMap) {
      if (lower.contains(keyword)) {
        return category
      }
    }
    return Category.OTHER
  }
}
