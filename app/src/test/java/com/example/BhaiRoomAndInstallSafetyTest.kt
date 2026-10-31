package com.example

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.test.core.app.ApplicationProvider
import com.example.capture.CaptureEventBus
import com.example.capture.SmsInboxScanner
import com.example.core.model.BudgetProfile
import com.example.core.model.InstrumentType
import com.example.core.model.SavingsGoal
import com.example.ui.bhairoom.computeAdaptiveGullakCoins
import com.example.ui.house.HouseScreen
import com.example.ui.theme.GharTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BhaiRoomAndInstallSafetyTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun manifest_declaresZeroFraudFlaggedPermissionsAndSupportsTextShareTarget() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val pm = context.packageManager
    val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
    val requested = pkgInfo.requestedPermissions?.toSet().orEmpty()

    // Android Enhanced Fraud Protection in India blocks sideloaded APKs declaring any of these:
    val fraudBlockedPermissions = listOf(
      "android.permission.RECEIVE_SMS",
      "android.permission.READ_SMS",
      "android.permission.SEND_SMS",
      "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
      "android.permission.BIND_ACCESSIBILITY_SERVICE"
    )
    for (forbidden in fraudBlockedPermissions) {
      assertFalse(
        "APK must NOT request $forbidden so Play Protect Enhanced Fraud Protection never blocks installation",
        forbidden in requested
      )
    }

    // Verify Share-to-Ghar (ACTION_SEND text/plain) resolves to MainActivity
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, SmsInboxScanner.sampleBankAlerts.first())
      setPackage(context.packageName)
    }
    val matches = pm.queryIntentActivities(shareIntent, 0)
    assertTrue("MainActivity must accept ACTION_SEND text/plain bank alert shares", matches.isNotEmpty())
  }

  @Test
  fun computeAdaptiveGullakCoins_neverThrowsAcrossEndOfMonthAndExtremeBudgetCombinations() {
    val incomes = listOf(0L, 3_000L, 12_000L, 30_000L, 50_000L, 150_000L, 1_000_000L)
    val savingsTargets = listOf(0L, 500L, 1_500L, 2_000L, 3_000L, 5_000L, 25_000L)
    val dailyAllowances = listOf(-500L, 0L, 40L, 800L, 1_200L, 2_500L, 8_500L, 25_000L)
    val goals = listOf(
      SavingsGoal(name = "Zero Pot", targetAmountPaise = 500_00L, currentAmountPaise = 0L),
      SavingsGoal(name = "Starter Gullak", targetAmountPaise = 25_000_00L, currentAmountPaise = 0L),
      SavingsGoal(name = "Completed Pot", targetAmountPaise = 50_000_00L, currentAmountPaise = 60_000_00L, instrumentType = InstrumentType.entries.last())
    )

    for (inc in incomes) {
      for (sav in savingsTargets) {
        val profile = BudgetProfile(
          monthlyIncomePaise = inc * 100L,
          fixedCommitmentsPaise = (inc / 3L) * 100L,
          savingsTargetPaise = sav * 100L
        )
        for (daily in dailyAllowances) {
          for (goal in goals) {
            val coins = computeAdaptiveGullakCoins(
              profile = profile,
              goal = goal,
              dailySafeAllowanceRupees = daily,
              unspentTodayRupees = daily.coerceAtLeast(0L),
              plainEnglish = false
            )
            assertEquals("Expected 4 coin presets", 4, coins.size)
            assertTrue("Coins must all be positive", coins.all { it.amountRupees > 0L })
          }
        }
      }
    }
  }

  @Test
  fun enteringBhaiRoom_withHighDailyAllowanceAndLowSavingsTarget_rendersAndDropsCoinsCleanly() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val root = DefaultRootAppContainer(app)
    val container = root.guestContainer

    // Exact profile combination that previously triggered IllegalArgumentException in computeAdaptiveGullakCoins
    // (high daily safe-to-spend near month-end paired with modest ₹2,000–₹3,000 savings target)
    container.budgetRepository.saveProfile(
      BudgetProfile(
        monthlyIncomePaise = 45_000_00L,
        fixedCommitmentsPaise = 10_000_00L,
        savingsTargetPaise = 2_000_00L
      )
    )
    container.goalRepository.setGullakBalance(0L)

    composeTestRule.setContent {
      GharTheme(themeMode = "LIGHT", dynamicColor = false) {
        HouseScreen(
          container = container,
          onResetToOnboarding = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    // Dismiss Kitchen first-visit splash if displayed
    if (composeTestRule.onAllNodesWithTag("room_splash_dismiss_button").fetchSemanticsNodes().isNotEmpty()) {
      composeTestRule.onNodeWithTag("room_splash_dismiss_button").performClick()
      composeTestRule.waitForIdle()
    }

    // Navigate into BhaiRoom / Papa's Gullak via bottom navigation dock
    composeTestRule.onNodeWithTag("hallway_room_bhai_room").performClick()
    composeTestRule.waitForIdle()

    // Dismiss BhaiRoom first-visit splash if displayed
    if (composeTestRule.onAllNodesWithTag("room_splash_dismiss_button").fetchSemanticsNodes().isNotEmpty()) {
      composeTestRule.onNodeWithTag("room_splash_dismiss_button").performClick()
      composeTestRule.waitForIdle()
    }

    // Verify BhaiRoom rendered without crashing and displays Gullak hero & coin medallions
    composeTestRule.onNodeWithTag("bhai_room_lazy_list").performScrollToKey("gullak_hero")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("gullak_hero_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("feed_gullak_100").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Verify Share-to-Ghar multi-alert batch parser logs transactions cleanly with zero permissions
    val multiBatch = SmsInboxScanner.sampleBankAlerts.joinToString("\n\n")
    CaptureEventBus.submitSharedAlertText(multiBatch)
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      container.transactionRepository.transactionsState.value.size >= 3
    }

    val loggedTxs = container.transactionRepository.transactionsState.value
    assertTrue("Shared bank alerts should be parsed and logged automatically", loggedTxs.size >= 3)
  }
}
