package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.capture.HouseholdNotificationHelper
import com.example.capture.SmsInboxScanner
import com.example.capture.parser.SmsParser
import com.example.core.ai.FamilyDomainRouter
import com.example.core.ai.PapaOfflineAdvisor
import com.example.core.engine.ChitthiWisdomBank
import com.example.core.engine.HouseholdAnalyticsEngine
import com.example.core.engine.SafeToSpendCalculator
import com.example.core.engine.SmartKhataParser
import com.example.core.localization.GharTerms
import com.example.core.model.BudgetProfile
import com.example.core.model.Category
import androidx.compose.ui.graphics.Color
import com.example.core.model.CommitmentType
import com.example.core.model.Direction
import com.example.core.model.PakkaCommitment
import com.example.core.model.SavingsGoal
import com.example.core.model.Transaction
import com.example.data.preferences.DailyReminderRoutine
import com.example.data.preferences.RoutineKind
import com.example.ui.kitchen.MaaHouseholdSnapshot
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ContrastDarkInk
import com.example.ui.theme.ContrastLightInk
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelSky
import com.example.ui.theme.PastelTerracottaDark
import com.example.ui.theme.contrastingContentColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GharPlainModeSmsAndAlertsTest {

  @Test
  fun smsParser_extractsDebitMerchantCategoryAndAvailableBankBalance() {
    val rawSms =
      "Rs. 340.00 debited from a/c XX4821 on 26-Oct-26 to VPA zomato@axisbank (UPI Ref 629104829102). Avl Bal: INR 18,420.50."
    val parsed = SmsParser.parse("AD-HDFCBK", rawSms)

    assertNotNull("Valid bank debit SMS should parse", parsed)
    assertEquals(340_00L, parsed!!.amountPaise)
    assertEquals(Direction.DEBIT, parsed.direction)
    assertEquals(18_420_50L, parsed.availableBalancePaise)
    assertTrue("Merchant should resolve cleanly from zomato VPA", parsed.merchant.contains("Zomato", ignoreCase = true))
  }

  @Test
  fun smsParser_extractsCreditAndRejectsOtpOrPromoSms() {
    val salarySms =
      "INR 60,000.00 credited to your A/c XX4821 by ACME CORP SALARY on 01-Oct. Available Balance Rs 74,210.00"
    val parsedCredit = SmsParser.parse("VM-ICICIB", salarySms)
    assertNotNull(parsedCredit)
    assertEquals(60_000_00L, parsedCredit!!.amountPaise)
    assertEquals(Direction.CREDIT, parsedCredit.direction)
    assertEquals(74_210_00L, parsedCredit.availableBalancePaise)

    val otpSms = "849201 is your OTP for Rs. 1,250.00 transaction at Amazon. Do not share this code."
    assertNull("OTP messages must never be imported as transactions", SmsParser.parse("VK-HDFCBK", otpSms))

    val loanPromoSms = "Pre-approved personal loan up to Rs. 5,00,000! Apply now with zero processing fee."
    assertNull("Loan promo SMS must be rejected", SmsParser.parse("BZ-LOAN", loanPromoSms))
  }

  @Test
  fun manualSmsParser_parsesSampleAlertsAndExtractsTransactionAndBalance() {
    val parsedSamples = SmsInboxScanner.sampleBankAlerts.mapNotNull { SmsInboxScanner.parseManualSmsText(it) }
    assertEquals(3, parsedSamples.size)

    val first = parsedSamples[0]
    assertEquals(340_00L, first.transaction.amountPaise)
    assertEquals(Direction.DEBIT, first.transaction.direction)
    assertEquals(Category.FOOD_DELIVERY, first.transaction.category)
    assertEquals(28_450_00L, first.availableBalancePaise)

    val credit = parsedSamples[2]
    assertEquals(4_500_00L, credit.transaction.amountPaise)
    assertEquals(Direction.CREDIT, credit.transaction.direction)
    assertEquals(32_765_00L, credit.availableBalancePaise)
  }

  @Test
  fun plainEnglishMode_removesIndianSlangAcrossTermsSeedsAndParser() {
    val draft = SmartKhataParser.parse(
      rawInput = "savings 500",
      activeGoalName = "Emergency Fund",
      plainEnglish = true
    )
    assertNotNull(draft)
    assertTrue(draft!!.isGullakDeposit)
    assertEquals("Savings Pot: Emergency Fund", draft.merchant)

    val savingsTx = Transaction(
      amountPaise = 500_00L,
      merchant = draft.merchant,
      category = Category.TRANSFERS,
      direction = Direction.DEBIT
    )
    assertTrue("Savings Pot prefix must be recognized as a savings transfer", savingsTx.isGullakTransfer())
    assertEquals("Coffee & Snacks", Category.CHAI_SNACKS.displayLabel(plainEnglish = true))
    assertEquals("Mom", GharTerms.maaName(plainEnglish = true))
    assertEquals("Maa", GharTerms.maaName(plainEnglish = false))
    assertEquals("Dad", GharTerms.papaName(plainEnglish = true))
    assertEquals("Papa", GharTerms.papaName(plainEnglish = false))
    assertEquals("Dad", GharTerms.bhaiName(plainEnglish = true))
    assertEquals("Daily Wallet", GharTerms.walletName(plainEnglish = true))
    assertEquals("Savings Pot", GharTerms.savingsPotName(plainEnglish = true))

    val forbiddenHinglish = listOf("beta", "batua", "gullak", "rasoi", "khata", "chitthi", "boond", "shagun", "bhai")
    val allPlainSeeds = ChitthiWisdomBank.getMaaSeeds(plainEnglish = true) +
      ChitthiWisdomBank.getPapaSeeds(plainEnglish = true)
    assertTrue(allPlainSeeds.isNotEmpty())
    for (seed in allPlainSeeds) {
      val combined = "${seed.title} ${seed.body} ${seed.actionLabel.orEmpty()}".lowercase()
      for (slang in forbiddenHinglish) {
        assertFalse("Plain English seed '${seed.id}' should not contain '$slang'", combined.contains(slang))
      }
    }

    val profile = BudgetProfile(
      monthlyIncomePaise = 60_000_00L,
      fixedCommitmentsPaise = 20_000_00L,
      savingsTargetPaise = 10_000_00L
    )
    val safe = SafeToSpendCalculator.calculate(profile, spentSoFarPaise = 5_000_00L)
    val summary = HouseholdAnalyticsEngine.buildShareableMonthlySummary(
      userName = "Alex",
      profile = profile,
      safeToSpend = safe,
      transactions = listOf(savingsTx),
      goals = listOf(SavingsGoal(name = "Emergency Fund", targetAmountPaise = 50_000_00L, currentAmountPaise = 500_00L)),
      plainEnglish = true
    )
    assertTrue(summary.contains("Safe to Spend Today"))
    assertTrue(summary.contains("Projected Month-End Spend"))
    assertFalse(summary.lowercase().contains("gullak"))
    assertFalse(summary.lowercase().contains("khata"))
  }

  @Test
  fun notificationHelper_postsQuickBalanceAndCustomAlertsWhenPermitted() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    val root = DefaultRootAppContainer(app)
    root.preferencesRepository.setPlainEnglishMode(true)
    root.preferencesRepository.setLastDetectedBankBalancePaise(24_500_00L)

    val postedBalance = HouseholdNotificationHelper.postQuickBalanceNotification(
      context = app,
      container = root.guestContainer,
      plainEnglish = true
    )
    assertTrue("Quick balance notification should post when permission is granted", postedBalance)

    val postedCustom = HouseholdNotificationHelper.postCustomNotification(
      context = app,
      title = "Pay Electricity Bill",
      message = "Due before 8 PM tonight"
    )
    assertTrue("Custom notification should post when permission is granted", postedCustom)

    HouseholdNotificationHelper.scheduleDailyLogReminder(
      context = app,
      enabled = true,
      hourOfDay = 21,
      minute = 0
    )

    val notifManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val activeNotifs = shadowOf(notifManager).allNotifications
    assertTrue("At least 2 notifications should be posted", activeNotifs.size >= 2)
  }

  @Test
  fun materialContrastAndRoutinesAndSmartContextualNudges_adaptCleanly() = runBlocking {
    // 1. WCAG contrast picker switches between dark ink and light ink automatically
    assertEquals(ContrastDarkInk, PastelTerracottaDark.contrastingContentColor())
    assertEquals(ContrastDarkInk, PastelHoney.contrastingContentColor())
    assertEquals(ContrastDarkInk, PastelSky.contrastingContentColor())
    assertEquals(ContrastDarkInk, Color.White.contrastingContentColor())
    assertEquals(ContrastLightInk, BackgroundDark.contrastingContentColor())
    assertEquals(ContrastLightInk, Color(0xFF9E3E22).contrastingContentColor())

    // 2. Timed Daily Reminder Routines persist custom HH:mm schedules cleanly
    val app = ApplicationProvider.getApplicationContext<Application>()
    val root = DefaultRootAppContainer(app)
    val initialRoutines = root.preferencesRepository.prefsSnapshot.first { it.isLoaded }.dailyRoutines
    assertTrue(initialRoutines.size >= 3)

    val customTeaRoutine = DailyReminderRoutine(
      id = "routine_evening_tea",
      title = "Evening Tea Check",
      hour = 17,
      minute = 30,
      kind = RoutineKind.CUSTOM,
      enabled = true
    )
    assertEquals("5:30 PM", customTeaRoutine.formattedTime())
    root.preferencesRepository.upsertDailyRoutine(customTeaRoutine)

    val afterUpsert = root.preferencesRepository.prefsSnapshot.first {
      it.isLoaded && it.dailyRoutines.any { r -> r.id == "routine_evening_tea" }
    }.dailyRoutines
    assertTrue(afterUpsert.any { it.id == "routine_evening_tea" && it.hour == 17 && it.minute == 30 })

    // 3. Smart contextual nudge selects due Pakka bills & avoids repeating the exact last key
    val nowMs = System.currentTimeMillis()
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMs }
    val todayDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val dueBill = PakkaCommitment(
      id = "test_due_elec",
      title = "Electricity Bill",
      amountPaise = 1_450_00L,
      type = CommitmentType.BILL,
      category = Category.BILLS_UTILITIES,
      dueDayOfMonth = todayDay
    )
    val dueReceivable = PakkaCommitment(
      id = "test_due_rent_split",
      title = "Roommate Rent Share",
      amountPaise = 8_000_00L,
      type = CommitmentType.RECEIVABLE,
      category = Category.ALLOWANCE_INCOME,
      dueDayOfMonth = todayDay
    )
    val profile = BudgetProfile(
      monthlyIncomePaise = 60_000_00L,
      fixedCommitmentsPaise = 20_000_00L,
      savingsTargetPaise = 10_000_00L
    )

    val firstNudge = HouseholdNotificationHelper.buildSmartContextualNudge(
      profile = profile,
      transactions = emptyList(),
      commitments = listOf(dueBill, dueReceivable),
      goals = listOf(SavingsGoal(name = "Emergency Fund", targetAmountPaise = 50_000_00L)),
      plainEnglish = true,
      lastNudgeKey = "",
      nowMs = nowMs
    )
    val expectedKeys = setOf(
      "bill_test_due_elec_$todayDay",
      "receivable_test_due_rent_split_$todayDay",
      "unlogged_day_$todayDay",
      "pace_brief_$todayDay"
    )
    assertTrue("Unexpected smart nudge key: ${firstNudge.key}", firstNudge.key in expectedKeys)
    assertTrue(firstNudge.title.isNotBlank() && firstNudge.body.isNotBlank())
    assertFalse(firstNudge.body.lowercase().contains("khata"))
    assertFalse(firstNudge.body.lowercase().contains("gullak"))

    val secondNudge = HouseholdNotificationHelper.buildSmartContextualNudge(
      profile = profile,
      transactions = emptyList(),
      commitments = listOf(dueBill, dueReceivable),
      goals = listOf(SavingsGoal(name = "Emergency Fund", targetAmountPaise = 50_000_00L)),
      plainEnglish = true,
      lastNudgeKey = firstNudge.key,
      nowMs = nowMs
    )
    assertFalse("Smart nudge should rotate away from lastNudgeKey when alternatives exist", secondNudge.key == firstNudge.key)

    root.preferencesRepository.deleteDailyRoutine("routine_evening_tea")
    val afterDelete = root.preferencesRepository.prefsSnapshot.first {
      it.isLoaded && it.dailyRoutines.none { r -> r.id == "routine_evening_tea" }
    }.dailyRoutines
    assertFalse(afterDelete.any { it.id == "routine_evening_tea" })
  }

  @Test
  fun familyDomainRouterAndPapaOfflineAdvisor_routeCorrectlyAndGiveGroundedAdvice() {
    // 1. Maa -> Papa referrals for investments, macro rates, and big sums (>= ₹10,000)
    assertTrue(FamilyDomainRouter.shouldMaaReferToPapa("Which Nifty 50 index SIP should I start with ₹5,000/month?"))
    assertTrue(FamilyDomainRouter.shouldMaaReferToPapa("Are bank FD interest rates better than PPF right now?"))
    assertTrue(FamilyDomainRouter.shouldMaaReferToPapa("Where should I park ₹50,000 bonus safely?"))
    assertTrue(FamilyDomainRouter.shouldMaaReferToPapa("How does RBI repo rate and inflation affect my savings?"))
    assertFalse(FamilyDomainRouter.shouldMaaReferToPapa("Can I spend ₹180 on iced coffee today?"))
    assertFalse(FamilyDomainRouter.shouldMaaReferToPapa("Help me split ₹840 dinner bill among 3 friends"))

    // 2. Papa -> Maa referrals for petty daily cash, snacks, food delivery, and daily wallet math
    assertTrue(FamilyDomainRouter.shouldPapaReferToMaa("Papa can I buy ₹20 chai and samosa?"))
    assertTrue(FamilyDomainRouter.shouldPapaReferToMaa("Should I order ₹350 Swiggy biryani for dinner tonight?"))
    assertTrue(FamilyDomainRouter.shouldPapaReferToMaa("How much is left in my Batua today?"))
    assertTrue(FamilyDomainRouter.shouldPapaReferToMaa("Split the dinner bill of ₹900"))
    // Must NOT refer away if small habit is framed as a compounding SIP question
    assertFalse(FamilyDomainRouter.shouldPapaReferToMaa("If I skip ₹200 coffee and put it in an index SIP for 10 years, how much will it compound?"))
    assertFalse(FamilyDomainRouter.shouldPapaReferToMaa("Where should I invest 1.5 lakh for 5 years?"))

    val profile = BudgetProfile(
      monthlyIncomePaise = 75_000_00L,
      fixedCommitmentsPaise = 25_000_00L,
      savingsTargetPaise = 15_000_00L
    )
    val goal = SavingsGoal(
      name = "Emergency Cushion",
      targetAmountPaise = 150_000_00L,
      currentAmountPaise = 30_000_00L
    )
    val safe = SafeToSpendCalculator.calculate(profile, spentSoFarPaise = 6_000_00L)

    val indianSnapshot = MaaHouseholdSnapshot(
      userName = "Aarav",
      safeToSpend = safe,
      profile = profile,
      goal = goal,
      transactions = emptyList(),
      plainEnglish = false,
      allGoals = listOf(goal)
    )
    val plainSnapshot = indianSnapshot.copy(plainEnglish = true)

    val maaHandoffIndian = FamilyDomainRouter.composeMaaReferralToPapa("Aarav", "Best SIP?", indianSnapshot)
    assertTrue(maaHandoffIndian.contains("Ask Papa instead"))
    val maaHandoffPlain = FamilyDomainRouter.composeMaaReferralToPapa("Aarav", "Best SIP?", plainSnapshot)
    assertTrue(maaHandoffPlain.contains("Ask Dad instead"))

    val papaHandoffIndian = FamilyDomainRouter.composePapaReferralToMaa("Aarav", "₹30 chai?", indianSnapshot)
    assertTrue(papaHandoffIndian.contains("Ask Maa instead"))
    val papaHandoffPlain = FamilyDomainRouter.composePapaReferralToMaa("Aarav", "₹30 coffee?", plainSnapshot)
    assertTrue(papaHandoffPlain.contains("Ask Mom instead"))

    // 3. PapaOfflineAdvisor grounded calculations in both Household and Plain English modes
    val lumpSumReply = PapaOfflineAdvisor.buildGroundedReply(
      userQuery = "Where should I invest ₹50,000 for 5 years?",
      snapshot = indianSnapshot
    )
    assertTrue(lumpSumReply.contains("₹20,000"))
    assertTrue(lumpSumReply.contains("Emergency Cushion"))

    val plainFdReply = PapaOfflineAdvisor.buildGroundedReply(
      userQuery = "Compare FD vs PPF safe rates",
      snapshot = plainSnapshot
    )
    assertTrue(plainFdReply.contains("PPF"))
    assertFalse(plainFdReply.lowercase().contains("gullak"))
    assertFalse(plainFdReply.lowercase().contains("beta"))
  }
}
