package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.capture.CaptureEventBus
import com.example.core.model.BudgetProfile
import com.example.core.model.CaptureSource
import com.example.core.model.Category
import com.example.core.model.ChitthiAuthor
import com.example.core.model.ChitthiNote
import com.example.core.model.Direction
import com.example.core.model.GuiltSensitivity
import com.example.core.model.PainDial
import com.example.core.model.QuickChip
import com.example.core.model.SavingsGoal
import com.example.core.model.StampColorKey
import com.example.core.model.Transaction
import com.example.data.backup.VaultBackupManager
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VaultBackupLockAndWidgetTest {

  @Before
  fun cleanLocalVaultFiles() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    File(context.filesDir, "ghar_local_vault").deleteRecursively()
  }

  @Test
  fun jsonBackupAndRestore_preservesFullLedgerGoalsStampsAndPreferences() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sourceRoot = DefaultRootAppContainer(context)
    val sourceGuest = sourceRoot.guestContainer

    sourceGuest.budgetRepository.saveHouseholdCalibration(
      userName = "Aarav",
      onboardingCompleted = true,
      profile = BudgetProfile(
        monthlyIncomePaise = 92_000_00L,
        fixedCommitmentsPaise = 30_000_00L,
        savingsTargetPaise = 20_000_00L,
        guiltSensitivity = GuiltSensitivity.HIGH,
        painDial = PainDial.PUNCHY
      ),
      habitLeaks = setOf("FOOD_DELIVERY", "CHAI_SNACKS")
    )
    sourceGuest.goalRepository.createGoal(
      SavingsGoal(
        id = "goal_goa",
        name = "Monsoon Goa Trip",
        targetAmountPaise = 50_000_00L,
        currentAmountPaise = 18_500_00L
      ),
      makeActive = true
    )
    sourceGuest.quickChipRepository.upsert(
      QuickChip(
        id = "stamp_filter_coffee",
        label = "Filter Coffee",
        merchant = "MTR Cafe",
        amountPaise = 45_00L,
        category = Category.CHAI_SNACKS,
        direction = Direction.DEBIT,
        colorKey = StampColorKey.HONEY,
        sortOrder = 0
      )
    )
    sourceGuest.transactionRepository.insert(
      Transaction(
        id = "tx_backup_1",
        amountPaise = 640_00L,
        direction = Direction.DEBIT,
        merchant = "Nature's Basket, Indiranagar",
        category = Category.GROCERIES,
        timestamp = 1_730_000_000_000L,
        source = CaptureSource.MANUAL
      )
    )
    sourceGuest.chitthiRepository.stash(
      ChitthiNote(
        id = "chitthi_backup_1",
        author = ChitthiAuthor.MAA,
        title = "Small Leaks",
        body = "Keep a watch on small evening snacks.",
        tag = "Rasoi",
        savedAt = 1_730_000_100_000L
      )
    )
    sourceGuest.preferencesRepository.setPlainEnglishMode(true)
    sourceGuest.preferencesRepository.setThemeMode("DARK")
    delay(60)

    val exportedJson = VaultBackupManager.buildBackupJsonString(sourceGuest, nowMs = 1_730_000_200_000L)
    assertTrue(exportedJson.contains("ghar_portable_vault"))
    assertTrue(exportedJson.contains("Monsoon Goa Trip"))
    assertTrue(exportedJson.contains("Nature's Basket, Indiranagar"))

    // Wipe local files to simulate switching to a brand-new phone in Guest Mode
    File(context.filesDir, "ghar_local_vault").deleteRecursively()
    val targetRoot = DefaultRootAppContainer(context)
    val targetGuest = targetRoot.guestContainer
    targetGuest.preferencesRepository.setPlainEnglishMode(false)
    targetGuest.preferencesRepository.setThemeMode("LIGHT")
    delay(40)

    val parsedResult = VaultBackupManager.parseBackupJsonString(exportedJson, targetGuest)
    assertTrue(parsedResult.isSuccess)
    val parsed = parsedResult.getOrThrow()
    assertEquals("Aarav", parsed.userNameDisplay)
    assertEquals(1, parsed.transactionCount)

    val applyResult = VaultBackupManager.applyValidatedBackup(context, parsed, targetGuest)
    assertTrue(applyResult.isSuccess)
    delay(60)

    assertEquals("Aarav", targetGuest.budgetRepository.userNameState.value)
    assertEquals(92_000_00L, targetGuest.budgetRepository.profileState.value.monthlyIncomePaise)
    assertEquals("Monsoon Goa Trip", targetGuest.goalRepository.primaryGoalState.value.name)
    assertEquals(18_500_00L, targetGuest.goalRepository.primaryGoalState.value.currentAmountPaise)
    assertEquals(1, targetGuest.transactionRepository.transactionsState.value.size)
    assertEquals(
      "Nature's Basket, Indiranagar",
      targetGuest.transactionRepository.transactionsState.value.first().merchant
    )
    assertTrue(
      targetGuest.quickChipRepository.quickChipsState.value.any { it.id == "stamp_filter_coffee" }
    )
    assertEquals(1, targetGuest.chitthiRepository.chitthisState.value.size)
    assertTrue(targetGuest.preferencesRepository.prefsSnapshot.value.plainEnglishMode)
    assertEquals("DARK", targetGuest.preferencesRepository.prefsSnapshot.value.themeMode)
  }

  @Test
  fun parseBackupJson_rejectsInvalidJsonGracefully() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = DefaultRootAppContainer(context).guestContainer

    val nonJson = VaultBackupManager.parseBackupJsonString("not a json file", container)
    assertTrue(nonJson.isFailure)

    val unrelatedJson = VaultBackupManager.parseBackupJsonString("""{"hello":"world"}""", container)
    assertTrue(unrelatedJson.isFailure)
  }

  @Test
  fun csvExport_includesUtf8BomAndEscapesCommasAndQuotes() {
    val txs = listOf(
      Transaction(
        id = "tx_csv_1",
        amountPaise = 125_50L,
        direction = Direction.DEBIT,
        merchant = "Sharma \"Special\" Chai, Sector 18",
        category = Category.CHAI_SNACKS,
        timestamp = 1_730_000_000_000L,
        source = CaptureSource.MANUAL,
        isRecurring = false
      )
    )

    val csv = VaultBackupManager.buildTransactionsCsvString(txs, plainEnglish = true)
    assertTrue(csv.startsWith("\uFEFFDate,Time,Type,Amount_INR,Amount_Paise"))
    assertTrue(csv.contains("DEBIT,125.50,12550,\"Sharma \"\"Special\"\" Chai, Sector 18\""))
  }

  @Test
  fun appLockPreferenceAndWidgetQuickKharchaSignal_workDeterministically() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = DefaultRootAppContainer(context).preferencesRepository

    prefsRepo.setAppLockEnabled(true)
    delay(50)
    assertTrue(prefsRepo.prefsSnapshot.value.appLockEnabled)

    prefsRepo.setAppLockEnabled(false)
    delay(50)
    assertFalse(prefsRepo.prefsSnapshot.value.appLockEnabled)

    assertFalse(CaptureEventBus.pendingQuickKharchaRequest.value)
    CaptureEventBus.requestOpenQuickKharcha()
    assertTrue(CaptureEventBus.pendingQuickKharchaRequest.value)
    assertTrue(CaptureEventBus.consumeQuickKharchaRequest())
    assertFalse(CaptureEventBus.pendingQuickKharchaRequest.value)
  }
}
