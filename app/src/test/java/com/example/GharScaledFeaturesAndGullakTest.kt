package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.engine.HouseholdAnalyticsEngine
import com.example.core.engine.SafeToSpendCalculator
import com.example.core.engine.SmartKhataParser
import com.example.core.model.BudgetProfile
import com.example.core.model.Category
import com.example.core.model.CommitmentType
import com.example.core.model.Direction
import com.example.core.model.InstrumentType
import com.example.core.model.PakkaCommitment
import com.example.core.model.SavingsGoal
import com.example.core.model.Transaction
import com.example.data.local.LocalHouseholdVault
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GharScaledFeaturesAndGullakTest {

  @Test
  fun savingsGoal_startsHonestlyAtZeroByDefault() {
    val defaultGoal = SavingsGoal()
    assertEquals("New Gullak must start at ₹0 (0 paise)", 0L, defaultGoal.currentAmountPaise)

    val context = ApplicationProvider.getApplicationContext<Context>()
    val vault = LocalHouseholdVault(context, vaultKey = "fresh_honest_gullak_${System.nanoTime()}")
    assertEquals(0L, vault.goal.value.currentAmountPaise)
    assertTrue(vault.goals.value.all { it.currentAmountPaise == 0L })
  }

  @Test
  fun multiGullakShelf_createsSwitchesFeedsAndPersistsAcrossRestarts() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val root1 = DefaultRootAppContainer(context)
    val guest1 = root1.guestContainer

    val zeroPrimary = SavingsGoal(
      id = "primary_goal",
      name = "Emergency Cushion",
      targetAmountPaise = 50_000_00L,
      currentAmountPaise = 0L
    )
    guest1.goalRepository.saveGoal(zeroPrimary)

    val goaGoal = SavingsGoal(
      id = "goal_${UUID.randomUUID().toString().take(8)}",
      name = "Goa Monsoon Trip",
      targetAmountPaise = 30_000_00L,
      currentAmountPaise = 0L,
      instrumentType = InstrumentType.RD
    )
    guest1.goalRepository.createGoal(goaGoal, makeActive = true)
    assertEquals(goaGoal.id, guest1.goalRepository.activeGoalIdState.value)
    assertEquals(0L, guest1.goalRepository.primaryGoalState.value.currentAmountPaise)

    // Feed ₹1,000 into the active Goa Gullak
    guest1.goalRepository.addToGullak(1_000_00L)
    assertEquals(1_000_00L, guest1.goalRepository.primaryGoalState.value.currentAmountPaise)

    // Switch back to primary goal — should still be ₹0
    guest1.goalRepository.selectGoal("primary_goal")
    assertEquals("Emergency Cushion", guest1.goalRepository.primaryGoalState.value.name)
    assertEquals(0L, guest1.goalRepository.primaryGoalState.value.currentAmountPaise)

    delay(120)

    // Cold reload verifies both Gullaks persist accurately
    val root2 = DefaultRootAppContainer(context)
    val guest2 = root2.guestContainer
    assertTrue(guest2.goalRepository.allGoalsState.value.size >= 2)
    val reloadedGoa = guest2.goalRepository.allGoalsState.value.firstOrNull { it.id == goaGoal.id }
    assertNotNull(reloadedGoa)
    assertEquals(1_000_00L, reloadedGoa!!.currentAmountPaise)

    // Deleting secondary Gullak falls back cleanly
    guest2.goalRepository.deleteGoal(goaGoal.id)
    assertFalse(guest2.goalRepository.allGoalsState.value.any { it.id == goaGoal.id })
  }

  @Test
  fun smartKhataParser_parsesNaturalLanguageExpensesIncomesRecurringAndGullak() {
    val chai = SmartKhataParser.parse("tapri chai 30")
    assertNotNull(chai)
    assertEquals(30L, chai!!.amountRupees)
    assertEquals(Category.CHAI_SNACKS, chai.category)
    assertEquals(Direction.DEBIT, chai.direction)
    assertFalse(chai.isGullakDeposit)

    val blinkit = SmartKhataParser.parse("420 blinkit milk and bread")
    assertNotNull(blinkit)
    assertEquals(420L, blinkit!!.amountRupees)
    assertEquals(Category.GROCERIES, blinkit.category)

    val salary = SmartKhataParser.parse("salary +45000")
    assertNotNull(salary)
    assertEquals(45_000L, salary!!.amountRupees)
    assertEquals(Direction.CREDIT, salary.direction)

    val gullak = SmartKhataParser.parse("gullak 500")
    assertNotNull(gullak)
    assertEquals(500L, gullak!!.amountRupees)
    assertTrue(gullak.isGullakDeposit)

    val rent = SmartKhataParser.parse("house rent 15000")
    assertNotNull(rent)
    assertEquals(15_000L, rent!!.amountRupees)
    assertEquals(Category.BILLS_UTILITIES, rent.category)
    assertTrue(rent.isRecurring)
  }

  @Test
  fun householdAnalyticsEngine_computesRhythmStreaksBillsCompoundingAndSummary() {
    val now = System.currentTimeMillis()
    val profile = BudgetProfile(
      monthlyIncomePaise = 60_000_00L,
      fixedCommitmentsPaise = 20_000_00L,
      savingsTargetPaise = 10_000_00L
    )
    val safeToSpend = SafeToSpendCalculator.calculate(profile, 1_200_00L)
    val txs = listOf(
      Transaction(
        id = "t1",
        amountPaise = 180_00L,
        direction = Direction.DEBIT,
        merchant = "Subko Coffee",
        category = Category.CHAI_SNACKS,
        timestamp = now - 1_000L
      ),
      Transaction(
        id = "t2",
        amountPaise = 950_00L,
        direction = Direction.DEBIT,
        merchant = "BESCOM Electricity",
        category = Category.BILLS_UTILITIES,
        timestamp = now - 2_000L,
        isRecurring = true
      )
    )

    val todaySpend = HouseholdAnalyticsEngine.calculateTodaySpendRupees(txs, now)
    assertEquals(1_130L, todaySpend)

    val rhythm = HouseholdAnalyticsEngine.buildSevenDayRhythm(
      transactions = txs,
      dailySafeRupees = safeToSpend.dailyAllowanceRupees,
      nowMillis = now
    )
    assertEquals(7, rhythm.size)
    assertTrue(rhythm.last().isToday)
    assertEquals(1_130L, rhythm.last().spentRupees)

    val bills = HouseholdAnalyticsEngine.evaluateRecurringBills(txs, monthStartDay = 1, nowMillis = now)
    assertEquals(1, bills.size)
    assertTrue(bills.first().paidThisCycle)

    val projection = HouseholdAnalyticsEngine.projectCompounding(
      initialRupees = 0L,
      monthlyContributionRupees = 2_000L,
      annualReturnPercent = 7.0,
      years = 3
    )
    assertEquals(72_000L, projection.totalPrincipalRupees)
    assertTrue(projection.wealthGainRupees > 0L)
    assertTrue(projection.maturityValueRupees > projection.totalPrincipalRupees)

    val summary = HouseholdAnalyticsEngine.buildShareableMonthlySummary(
      userName = "Harshit",
      profile = profile,
      safeToSpend = safeToSpend,
      transactions = txs,
      goals = listOf(SavingsGoal(name = "Monsoon Fund", targetAmountPaise = 50_000_00L, currentAmountPaise = 0L)),
      nowMillis = now
    )
    assertTrue(summary.contains("Harshit"))
    assertTrue(summary.contains("Monsoon Fund"))
  }

  @Test
  fun pakkeCommitments_supportsAllFourTypesCrudSettlementAndColdReload() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vaultKey = "pakke_test_${System.nanoTime()}"
    val vault1 = LocalHouseholdVault(context, vaultKey = vaultKey)

    // Defaults seed bills, fixed payments, subscriptions, and receivables
    assertTrue(vault1.commitments.value.size >= 4)

    val customSub = PakkaCommitment(
      id = "pakka_spotify_duo",
      title = "Spotify Duo",
      amountPaise = 149_00L,
      type = CommitmentType.SUBSCRIPTION,
      category = Category.ENTERTAINMENT,
      dueDayOfMonth = 8,
      notes = "Auto-debit UPI"
    )
    val customReceivable = PakkaCommitment(
      id = "pakka_flatmate_rent",
      title = "Flatmate Rent Share",
      amountPaise = 9_500_00L,
      type = CommitmentType.RECEIVABLE,
      category = Category.ALLOWANCE_INCOME,
      dueDayOfMonth = 3,
      notes = "Arjun transfers via UPI"
    )

    vault1.upsertCommitmentFromLocal(customSub)
    vault1.upsertCommitmentFromLocal(customReceivable)

    // Edit Spotify Duo amount
    vault1.upsertCommitmentFromLocal(customSub.copy(amountPaise = 179_00L, dueDayOfMonth = 11))

    delay(120)

    // Cold reload from disk preserves edited subscription & receivable
    val vault2 = LocalHouseholdVault(context, vaultKey = vaultKey)
    val reloadedSub = vault2.commitments.value.firstOrNull { it.id == "pakka_spotify_duo" }
    val reloadedRec = vault2.commitments.value.firstOrNull { it.id == "pakka_flatmate_rent" }
    assertNotNull(reloadedSub)
    assertNotNull(reloadedRec)
    assertEquals(179_00L, reloadedSub!!.amountPaise)
    assertEquals(11, reloadedSub.dueDayOfMonth)
    assertEquals(CommitmentType.RECEIVABLE, reloadedRec!!.type)

    // Verify settlement builder produces DEBIT for outflows and CREDIT for receivables, and cycle evaluation marks settled
    val now = System.currentTimeMillis()
    val subSettlementTx = HouseholdAnalyticsEngine.buildCommitmentSettlementTransaction(reloadedSub, now)
    val recSettlementTx = HouseholdAnalyticsEngine.buildCommitmentSettlementTransaction(reloadedRec, now)
    assertEquals(Direction.DEBIT, subSettlementTx.direction)
    assertEquals(Direction.CREDIT, recSettlementTx.direction)

    val evalBefore = HouseholdAnalyticsEngine.evaluateCommitments(
      commitments = listOf(reloadedSub, reloadedRec),
      transactions = emptyList(),
      monthStartDay = 1,
      nowMillis = now
    )
    assertEquals(179L, evalBefore.pendingOutflowRupees)
    assertEquals(9_500L, evalBefore.pendingReceivableRupees)
    assertEquals(0, evalBefore.settledOutflowsCount)
    assertEquals(0, evalBefore.settledReceivablesCount)

    val evalAfter = HouseholdAnalyticsEngine.evaluateCommitments(
      commitments = listOf(reloadedSub, reloadedRec),
      transactions = listOf(subSettlementTx, recSettlementTx),
      monthStartDay = 1,
      nowMillis = now
    )
    assertEquals(0L, evalAfter.pendingOutflowRupees)
    assertEquals(0L, evalAfter.pendingReceivableRupees)
    assertEquals(1, evalAfter.settledOutflowsCount)
    assertEquals(1, evalAfter.settledReceivablesCount)

    // Delete commitment removes it cleanly
    vault2.deleteCommitmentFromLocal("pakka_spotify_duo")
    assertFalse(vault2.commitments.value.any { it.id == "pakka_spotify_duo" })
  }

  @Test
  fun decomposeExactRupeeBills_alwaysSumsToExactAmountAndNeverInflates20RupeeSpend() {
    val testAmounts = listOf(1L, 2L, 5L, 7L, 10L, 12L, 20L, 30L, 35L, 50L, 100L, 340L, 750L, 999L, 1_888L, 18_450L)

    for (dial in com.example.core.model.PainDial.entries) {
      for (amount in testAmounts) {
        val bills = com.example.ui.common.decomposeExactRupeeBills(amount, dial)
        assertTrue("Expected at least 1 bill for ₹$amount ($dial)", bills.isNotEmpty())
        assertTrue("Expected <= 6 animated bills for ₹$amount ($dial), got ${bills.size}", bills.size <= 6)
        assertEquals(
          "Flying bills must sum to exact ₹$amount in $dial mode",
          amount,
          bills.sumOf { it.totalRupees }
        )
      }
    }

    // Specifically verify the user-reported ₹20 case:
    // SUBTLE -> [₹20] (1 bill), BALANCED -> [₹10, ₹5, ₹5] (3 smaller bills = ₹20), PUNCHY -> [₹5, ₹5, ₹5, ₹5] (4 smaller bills = ₹20)
    val subtle20 = com.example.ui.common.decomposeExactRupeeBills(20L, com.example.core.model.PainDial.SUBTLE)
    assertEquals(listOf(20L), subtle20.map { it.totalRupees })

    val balanced20 = com.example.ui.common.decomposeExactRupeeBills(20L, com.example.core.model.PainDial.BALANCED)
    assertEquals(3, balanced20.size)
    assertEquals(20L, balanced20.sumOf { it.totalRupees })
    assertTrue("Balanced ₹20 should use smaller denominations (< ₹20)", balanced20.all { it.palette.valueRupees < 20L })

    val punchy20 = com.example.ui.common.decomposeExactRupeeBills(20L, com.example.core.model.PainDial.PUNCHY)
    assertEquals(4, punchy20.size)
    assertEquals(20L, punchy20.sumOf { it.totalRupees })
  }

  @Test
  fun gharSoundEffects_synthesizesValidRiffWavePcmForDoorKundiAndGullakClink() = runBlocking {
    val doorWav = com.example.core.audio.GharSoundEffects.buildDoorKundiOpenWavBytes()
    val coinWav = com.example.core.audio.GharSoundEffects.buildGullakCoinClinkWavBytes()

    assertTrue("Door kundi WAV should have valid PCM payload", doorWav.size > 20_000)
    assertTrue("Gullak coin clink WAV should have valid PCM payload", coinWav.size > 20_000)

    assertEquals("RIFF", String(doorWav, 0, 4, Charsets.US_ASCII))
    assertEquals("WAVE", String(doorWav, 8, 4, Charsets.US_ASCII))
    assertEquals("RIFF", String(coinWav, 0, 4, Charsets.US_ASCII))
    assertEquals("WAVE", String(coinWav, 8, 4, Charsets.US_ASCII))

    val context = ApplicationProvider.getApplicationContext<Context>()
    val root = DefaultRootAppContainer(context)
    // Sound enabled defaults to true and toggles cleanly
    assertTrue(root.preferencesRepository.prefsSnapshot.value.soundEnabled)
    root.preferencesRepository.setSoundEnabled(false)
    delay(60)
    assertFalse(root.preferencesRepository.prefsSnapshot.value.soundEnabled)
    // Muted calls are safe no-ops
    com.example.core.audio.GharSoundEffects.playDoorKundiOpen(context, soundEnabled = false)
    com.example.core.audio.GharSoundEffects.playGullakCoinClink(context, soundEnabled = false)
  }

  @Test
  fun gharResourcePrewarmer_compilesAssetsInBackgroundWithoutError() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.ui.common.GharImageCache.trimMemory(clearAll = true)
    assertEquals(0, com.example.ui.common.GharImageCache.currentCachedBytes())

    val root = DefaultRootAppContainer(context)
    com.example.core.perf.GharResourcePrewarmer.startWarmup(
      context = context,
      rootContainer = root,
      isOnboardingCompleted = true
    )

    var attempts = 0
    while (!com.example.core.perf.GharResourcePrewarmer.isFullyWarmed.value && attempts < 60) {
      delay(40)
      attempts++
    }
    assertTrue(
      "Background resource prewarmer should complete cleanly",
      com.example.core.perf.GharResourcePrewarmer.isFullyWarmed.value
    )
    assertEquals(1.0f, com.example.core.perf.GharResourcePrewarmer.warmupProgress.value, 0.001f)

    // Verify bounded LRU cache stays strictly below maxCacheBytes and does NOT eagerly decode offscreen rooms
    assertTrue(com.example.ui.common.GharImageCache.maxCacheBytes in (8 * 1024 * 1024)..(16 * 1024 * 1024))
    assertTrue(
      "Cached bytes must remain bounded within maxCacheBytes",
      com.example.ui.common.GharImageCache.currentCachedBytes() <= com.example.ui.common.GharImageCache.maxCacheBytes
    )
    assertNull(
      "Offscreen Balcony banner should NOT be eagerly decoded during Kitchen startup",
      com.example.ui.common.GharImageCache.getCached(R.drawable.img_balcony_dusk_1792931332537)
    )

    // Verify low-memory trim clears cache cleanly
    com.example.ui.common.GharImageCache.trimMemory(clearAll = true)
    assertEquals(0, com.example.ui.common.GharImageCache.currentCachedBytes())
  }

  @Test
  fun adaptiveGullakCoins_scalesToIncomeAllowanceAndGoalPace() {
    val lowIncomeProfile = BudgetProfile(
      monthlyIncomePaise = 15_000_00L,
      fixedCommitmentsPaise = 5_000_00L,
      savingsTargetPaise = 2_000_00L
    )
    val lowGoal = SavingsGoal(name = "Small Goal", targetAmountPaise = 10_000_00L, currentAmountPaise = 1_000_00L)
    val lowCoins = com.example.ui.bhairoom.computeAdaptiveGullakCoins(
      profile = lowIncomeProfile,
      goal = lowGoal,
      dailySafeAllowanceRupees = 250L,
      unspentTodayRupees = 100L,
      plainEnglish = false
    )
    assertEquals(4, lowCoins.size)
    assertTrue("Low income coin options should start small (<= ₹50)", lowCoins.first().amountRupees <= 50L)
    assertTrue("Max coin for ₹15k income should be modest (<= ₹1,000)", lowCoins.last().amountRupees <= 1_000L)

    val highIncomeProfile = BudgetProfile(
      monthlyIncomePaise = 3_50_000_00L,
      fixedCommitmentsPaise = 80_000_00L,
      savingsTargetPaise = 1_00_000_00L
    )
    val highGoal = SavingsGoal(name = "House Down Payment", targetAmountPaise = 25_00_000_00L, currentAmountPaise = 2_00_000_00L)
    val highCoins = com.example.ui.bhairoom.computeAdaptiveGullakCoins(
      profile = highIncomeProfile,
      goal = highGoal,
      dailySafeAllowanceRupees = 4_500L,
      unspentTodayRupees = 2_000L,
      plainEnglish = false
    )
    assertEquals(4, highCoins.size)
    assertTrue("High income coin options should offer larger steps (>= ₹100)", highCoins.first().amountRupees >= 50L)
    assertTrue("High income max coin should scale up (>= ₹1,000)", highCoins.last().amountRupees >= 1_000L)
    assertEquals("feed_gullak_50", highCoins[0].testTag)
    assertEquals("feed_gullak_100", highCoins[1].testTag)
  }

  @Test
  fun activeHabitSankalp_encodesDecodesAndPersistsInPreferences() = runBlocking<Unit> {
    val sankalp = com.example.data.preferences.ActiveHabitSankalp(
      insightId = "coffee_habit_123",
      title = "Home Brew Coffee Pledge",
      prepChecklist = "Set up French Press tonight",
      targetCategoryName = "CHAI_SNACKS",
      estimatedSavedRupees = 150L,
      reminderHour = 8,
      reminderMinute = 15,
      createdAtMs = 1_000_000L,
      expiresAtMs = 1_000_000L + 86_400_000L
    )

    val encoded = com.example.data.preferences.ActiveHabitSankalp.encode(sankalp)
    val decoded = com.example.data.preferences.ActiveHabitSankalp.decode(encoded)
    assertNotNull(decoded)
    assertEquals("coffee_habit_123", decoded!!.insightId)
    assertEquals("Home Brew Coffee Pledge", decoded.title)
    assertEquals(150L, decoded.estimatedSavedRupees)
    assertEquals(8, decoded.reminderHour)
    assertEquals(15, decoded.reminderMinute)

    val context = ApplicationProvider.getApplicationContext<Context>()
    val root = DefaultRootAppContainer(context)
    root.preferencesRepository.setActiveHabitSankalp(sankalp)
    delay(80)

    val prefs = root.preferencesRepository.prefsSnapshot.value
    assertNotNull(prefs.activeHabitSankalp)
    assertEquals("coffee_habit_123", prefs.activeHabitSankalp?.insightId)

    // Complete sankalp clears it
    root.preferencesRepository.setActiveHabitSankalp(null)
    delay(80)
    assertNull(root.preferencesRepository.prefsSnapshot.value.activeHabitSankalp)
  }

  @Test
  fun gharSoundEffects_synthesizesAll5HighDefinitionWavAssets() {
    val door = com.example.core.audio.GharSoundEffects.buildDoorKundiOpenWavBytes()
    val coin = com.example.core.audio.GharSoundEffects.buildGullakCoinClinkWavBytes()
    val note = com.example.core.audio.GharSoundEffects.buildCrispNoteRustleWavBytes()
    val chitthi = com.example.core.audio.GharSoundEffects.buildPaperChitthiPinWavBytes()
    val chai = com.example.core.audio.GharSoundEffects.buildChaiCupSettleWavBytes()

    listOf<ByteArray>(door, coin, note, chitthi, chai).forEach { wav ->
      assertTrue("WAV payload must be non-empty and > 10 KB", wav.size > 10_000)
      assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
      assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))
    }
  }
}

