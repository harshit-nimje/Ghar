package com.example.core.localization

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/**
 * Global CompositionLocal indicating whether Plain English (non-slang / zero-Hinglish) mode is active.
 * - false = Warm Indian Household Terms (Rasoi, Gullak, Batua, Khata, Chitthi, Maa, Papa)
 * - true  = Universal Plain English Terms (Home, Savings, Daily Budget, Ledger, Saved Notes, Mom, Dad)
 *
 * Financial metrics & analysis statements remain strictly direct and factual in BOTH modes.
 */
val LocalPlainEnglishMode = compositionLocalOf { false }

@Immutable
object GharTerms {

  fun maaName(plainEnglish: Boolean): String = if (plainEnglish) "Mom" else "Maa"

  fun papaName(plainEnglish: Boolean): String = if (plainEnglish) "Dad" else "Papa"

  fun bhaiName(plainEnglish: Boolean): String = papaName(plainEnglish)

  fun walletName(plainEnglish: Boolean): String = if (plainEnglish) "Daily Wallet" else "Batua"

  fun savingsPotName(plainEnglish: Boolean): String = if (plainEnglish) "Savings Pot" else "Gullak"

  fun savingsPotsPlural(plainEnglish: Boolean): String = if (plainEnglish) "Savings Pots" else "Gullaks"

  fun ledgerName(plainEnglish: Boolean): String = if (plainEnglish) "Transactions" else "Kitchen Khata"

  fun shortLedgerName(plainEnglish: Boolean): String = if (plainEnglish) "Ledger" else "Khata"

  fun noteName(plainEnglish: Boolean): String = if (plainEnglish) "Saved Note" else "Chitthi"

  fun notesCollectionName(plainEnglish: Boolean): String = if (plainEnglish) "Saved Notes" else "Chitthi Dabba"

  fun notesBadgeLabel(plainEnglish: Boolean, count: Int): String =
    if (plainEnglish) {
      if (count > 0) "Notes ($count)" else "Saved Notes"
    } else {
      if (count > 0) "Chitthis ($count)" else "Chitthi Dabba"
    }

  fun recurringBillsTitle(plainEnglish: Boolean): String =
    if (plainEnglish) "Monthly Bills, Subs & Receivables" else "Pakke Bills, Subs & Receivables"

  fun commitmentsCardShortTitle(plainEnglish: Boolean): String =
    if (plainEnglish) "Bills & Receivables" else "Pakke Bills & Dues"

  fun calculatorTitle(plainEnglish: Boolean): String =
    if (plainEnglish) "Budget Calculator" else "Calculator"

  fun quickEntryTitle(plainEnglish: Boolean): String =
    if (plainEnglish) "Quick Entry" else "Quick Khata Jot"

  fun quickEntryPlaceholder(plainEnglish: Boolean): String =
    if (plainEnglish) "e.g. coffee 40, groceries 420, savings 500" else "e.g. chai 30, blinkit 420, gullak 500"

  fun quickStampsTitle(plainEnglish: Boolean): String =
    if (plainEnglish) "Quick Shortcuts" else "Quick Khata Stamps"

  fun monthlyReportButton(plainEnglish: Boolean): String =
    if (plainEnglish) "Share Monthly Summary" else "Share Monthly Hisaab"

  fun chatWithMaaLabel(plainEnglish: Boolean): String =
    if (plainEnglish) "Ask Mom" else "Chat with Maa"

  fun chatWithPapaLabel(plainEnglish: Boolean): String =
    if (plainEnglish) "Ask Dad" else "Sit with Papa"
}

