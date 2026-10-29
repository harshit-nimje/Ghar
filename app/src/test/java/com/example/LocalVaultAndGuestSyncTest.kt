package com.example

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.BudgetProfile
import com.example.core.model.CaptureSource
import com.example.core.model.Category
import com.example.core.model.Direction
import com.example.core.model.GuiltSensitivity
import com.example.core.model.PainDial
import com.example.core.model.QuickChip
import com.example.core.model.SavingsGoal
import com.example.core.model.StampColorKey
import com.example.core.model.Transaction
import com.example.core.model.UserHousehold
import androidx.compose.ui.text.font.FontSynthesis
import androidx.core.content.res.ResourcesCompat
import com.example.data.local.LocalHouseholdVault
import com.example.ui.common.GharImageCache
import com.example.ui.theme.DisplaySerifFontFamily
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.GharFontWarmup
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.HandwritingFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.ScribbleMoneyFontFamily
import com.example.ui.theme.TillanaFontFamily
import com.example.ui.theme.Typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalVaultAndGuestSyncTest {

  @Test
  fun guestContainer_persistsChangesAcrossReloadsWithoutResettingOrReseeding() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val root1 = DefaultRootAppContainer(context)
    val guest1 = root1.guestContainer

    assertFalse(guest1.isCloudAuthenticated)

    val customProfile = BudgetProfile(
      monthlyIncomePaise = 85_000_00L,
      fixedCommitmentsPaise = 28_000_00L,
      savingsTargetPaise = 18_000_00L,
      guiltSensitivity = GuiltSensitivity.LOW,
      painDial = PainDial.PUNCHY,
      lifeStage = "Freelancer / Self-Employed"
    )
    guest1.budgetRepository.saveHouseholdCalibration(
      userName = "Harshit",
      onboardingCompleted = true,
      profile = customProfile,
      habitLeaks = setOf("CHAI_SNACKS", "ONLINE_SHOPPING")
    )
    guest1.goalRepository.saveGoal(
      SavingsGoal(
        id = "primary_goal",
        name = "Ladakh Bike Ride",
        targetAmountPaise = 75_000_00L,
        currentAmountPaise = 25_000_00L
      )
    )
    guest1.seedDefaultDataOnce()
    delay(80)

    // User adds a custom expense, adds ₹500 to Gullak, customizes a QuickChip, and deletes starter items
    val customTx = Transaction(
      id = "tx_custom_1",
      amountPaise = 320_00L,
      direction = Direction.DEBIT,
      merchant = "Irani Cafe Bun Maska",
      category = Category.CHAI_SNACKS,
      source = CaptureSource.MANUAL
    )
    guest1.transactionRepository.insert(customTx)
    guest1.goalRepository.addToGullak(500_00L)
    guest1.quickChipRepository.upsert(
      QuickChip(
        id = "chip_custom_metro",
        label = "Metro",
        merchant = "DMRC",
        amountPaise = 45_00L,
        category = Category.COMMUTE,
        colorKey = StampColorKey.SKY,
        sortOrder = 0
      )
    )

    // Erase all seed items and verify starterSeeded prevents them from coming back on reload
    guest1.transactionRepository.transactionsState.value
      .filter { it.source == CaptureSource.SEED }
      .forEach { guest1.transactionRepository.delete(it.id) }

    // Wait briefly for background disk flush
    delay(120)

    // Simulate cold app restart with a brand-new RootAppContainer
    val root2 = DefaultRootAppContainer(context)
    val guest2 = root2.guestContainer
    assertTrue("Guest onboarding must stay completed after restart", guest2.isOnboardingCompletedLocally)
    assertEquals("Harshit", guest2.budgetRepository.userNameState.value)
    assertEquals(85_000_00L, guest2.budgetRepository.profileState.value.monthlyIncomePaise)
    assertEquals(PainDial.PUNCHY, guest2.budgetRepository.profileState.value.painDial)
    assertEquals("Ladakh Bike Ride", guest2.goalRepository.primaryGoalState.value.name)
    assertEquals(25_500_00L, guest2.goalRepository.primaryGoalState.value.currentAmountPaise)

    // Trigger seedDefaultDataOnce() on restart (just like GharNavHost does) — must NOT re-insert deleted seed items!
    guest2.seedDefaultDataOnce()
    delay(80)

    val reloadedTxs = guest2.transactionRepository.transactionsState.value
    assertEquals(1, reloadedTxs.size)
    assertEquals("Irani Cafe Bun Maska", reloadedTxs.first().merchant)
    assertTrue(guest2.quickChipRepository.quickChipsState.value.any { it.id == "chip_custom_metro" })
  }

  @Test
  fun localHouseholdVault_protectsOptimisticWritesFromStaleCloudSnapshots() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vault = LocalHouseholdVault(context, vaultKey = "test_grace_uid_${System.nanoTime()}")

    // Local user updates household name & income optimistically
    vault.updateHouseholdFromLocal(isCalibration = true) {
      UserHousehold(
        userId = "u1",
        userName = "Aarav",
        onboardingCompleted = true,
        profile = BudgetProfile(monthlyIncomePaise = 90_000_00L),
        habitLeaks = listOf("CHAI_SNACKS")
      )
    }
    vault.upsertTransactionFromLocal(
      Transaction(id = "fresh_tx", amountPaise = 120_00L, merchant = "Fresh Sabzi")
    )

    // Stale Firestore snapshot arrives 50ms later with init defaults / empty list
    vault.applyCloudHouseholdIfFresher(
      UserHousehold(
        userId = "u1",
        userName = "Friend",
        onboardingCompleted = false,
        profile = BudgetProfile(monthlyIncomePaise = 50_000_00L)
      )
    )
    vault.applyCloudTransactionsIfFresher(emptyList())

    // Vault MUST retain the fresh local state instead of resetting to init state!
    assertTrue(vault.isOnboardingCompleted)
    assertEquals("Aarav", vault.household.value.userName)
    assertEquals(90_000_00L, vault.household.value.profile.monthlyIncomePaise)
    assertEquals(1, vault.transactions.value.size)
    assertEquals("Fresh Sabzi", vault.transactions.value.first().merchant)
  }

  @Test
  fun gharImageCache_decodesOptimizedRgb565BitmapsWithoutUpscaling() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val kitchenBmp = GharImageCache.getOrDecode(context, R.drawable.img_kitchen_hearth_1792931327219)
    assertNotNull(kitchenBmp)
    assertEquals(ImageBitmapConfig.Rgb565, kitchenBmp!!.config)
    assertTrue("Resized nodpi banner width should be <= 960px, was ${kitchenBmp.width}", kitchenBmp.width <= 960)
  }

  @Test
  fun bundledFontsAndExpressiveTypography_resolveAllSevenCustomFontFamilies() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals(7, GharFontWarmup.allBundledFontResIds.size)
    for (fontResId in GharFontWarmup.allBundledFontResIds) {
      val typeface = ResourcesCompat.getFont(context, fontResId)
      assertNotNull("Bundled font resource $fontResId must load a valid Typeface", typeface)
    }

    assertEquals(DisplaySerifFontFamily, Typography.headlineLarge.fontFamily)
    assertEquals(FrauncesFontFamily, Typography.headlineSmall.fontFamily)
    assertEquals(FrauncesFontFamily, Typography.titleLarge.fontFamily)
    assertEquals(PlusJakartaFontFamily, Typography.bodyLarge.fontFamily)
    assertEquals(FontSynthesis.All, Typography.titleMedium.fontSynthesis)

    assertEquals(ScribbleMoneyFontFamily, GharHandStyles.moneyHero.fontFamily)
    assertEquals(ScribbleMoneyFontFamily, GharHandStyles.moneyMedium.fontFamily)
    assertEquals(TillanaFontFamily, GharHandStyles.moneySmall.fontFamily)
    assertEquals(HandwritingFontFamily, GharHandStyles.stickyNoteTitle.fontFamily)
    assertEquals(HandwritingFontFamily, GharHandStyles.stickyNoteBody.fontFamily)
    assertEquals(HandwritingFontFamily, GharHandStyles.stickyNoteMeta.fontFamily)
    assertNotEquals(PlusJakartaFontFamily, GharHandStyles.moneyHero.fontFamily)
    assertNotEquals(PlusJakartaFontFamily, GharHandStyles.stickyNoteMeta.fontFamily)
  }
}
