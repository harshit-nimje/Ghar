package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.ai.MaaMiniIntent
import com.example.core.ai.MaaMiniModelManager
import com.example.core.ai.MiniModelStatus
import com.example.core.engine.ChitthiWisdomBank
import com.example.core.engine.SafeToSpendCalculator
import com.example.core.model.BudgetProfile
import com.example.core.model.Category
import com.example.core.model.ChatSender
import com.example.core.model.CommitmentType
import com.example.core.model.Direction
import com.example.core.model.MaaChatMessage
import com.example.core.model.PakkaCommitment
import com.example.core.model.QuickChip
import com.example.core.model.SavingsGoal
import com.example.core.model.StampColorKey
import com.example.core.model.Transaction
import com.example.ui.common.HisaabCalculatorEngine
import com.example.ui.kitchen.MaaChatUiState
import com.example.ui.kitchen.MaaChatViewModel
import com.example.ui.kitchen.MaaHouseholdSnapshot
import com.example.ui.kitchen.formatQuickChipSublabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MaaMiniAndQuickChipsTest {

  @Test
  fun maaMiniModelManager_0_5b492MbInstallsRunsMultiTurnAndCompoundAsksAndUninstallsOnDisk() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = MaaMiniModelManager(context, simulatePacing = false)
    manager.uninstallModel()
    assertFalse(manager.isInstalled)
    assertTrue(manager.status.value is MiniModelStatus.NotInstalled)

    // Install quantized 0.5B Q4_K_M model artifact (492.0 MB = 515,899,392 bytes) to filesDir
    val installResult = manager.installModel()
    assertTrue(installResult.isSuccess)
    assertTrue(manager.isInstalled)
    val installed = manager.status.value as MiniModelStatus.Installed
    assertEquals(MaaMiniModelManager.TARGET_MODEL_SIZE_BYTES, installed.sizeBytes)
    assertEquals("492.0 MB", MaaMiniModelManager.formatMegabytes(installed.sizeBytes))
    assertEquals(10, installed.sha256Short.length)

    // Fresh manager instance detects persisted 492 MB artifact from disk automatically
    val reloadedManager = MaaMiniModelManager(context, simulatePacing = false)
    assertTrue(reloadedManager.isInstalled)

    val engine = reloadedManager.getOrLoadEngine()
    val profile = BudgetProfile(
      monthlyIncomePaise = 60_000_00L,
      fixedCommitmentsPaise = 24_000_00L,
      savingsTargetPaise = 12_000_00L
    )
    val txs = listOf(
      Transaction(amountPaise = 30_00L, merchant = "Sharma Tea", category = Category.CHAI_SNACKS),
      Transaction(amountPaise = 40_00L, merchant = "Sharma Tea", category = Category.CHAI_SNACKS)
    )
    val commitments = listOf(
      PakkaCommitment(
        id = "pakka_wifi",
        title = "Home Wi-Fi & Broadband",
        amountPaise = 899_00L,
        type = CommitmentType.BILL,
        category = Category.BILLS_UTILITIES,
        dueDayOfMonth = 10
      ),
      PakkaCommitment(
        id = "pakka_netflix",
        title = "Streaming Subscription",
        amountPaise = 199_00L,
        type = CommitmentType.SUBSCRIPTION,
        category = Category.ENTERTAINMENT,
        dueDayOfMonth = 15
      ),
      PakkaCommitment(
        id = "pakka_retainer",
        title = "Client Retainer",
        amountPaise = 5_000_00L,
        type = CommitmentType.RECEIVABLE,
        category = Category.ALLOWANCE_INCOME,
        dueDayOfMonth = 25
      )
    )
    val safeToSpend = SafeToSpendCalculator.calculate(profile, 70_00L)
    val goal = SavingsGoal(
      name = "Himachal Trip",
      targetAmountPaise = 40_000_00L,
      currentAmountPaise = 18_000_00L
    )
    val snapshot = MaaHouseholdSnapshot(
      userName = "Harshit",
      safeToSpend = safeToSpend,
      profile = profile,
      goal = goal,
      allGoals = listOf(goal),
      transactions = txs,
      commitments = commitments
    )

    // 1. Multi-item complex basket affordability check (₹180 + ₹260 = ₹440)
    val basketReply = engine.generateReply(
      "Maa, can I buy ₹180 cold coffee and ₹260 biryani tonight?",
      snapshot
    )
    assertTrue(basketReply.contains("Harshit"))
    assertTrue("Expected combined ₹440 basket math, got: $basketReply", basketReply.contains("440"))

    // 2. Multi-turn conversational follow-up coreference ("what if it's half of that?" after ₹1,200)
    val history = listOf(
      MaaChatMessage(sender = ChatSender.USER, text = "Can I buy a ₹1200 jacket tonight?"),
      MaaChatMessage(sender = ChatSender.MAA, text = "₹1,200 is above today's limit.")
    )
    val followUpReply = engine.generateReply(
      "What if I buy one for half of that tonight?",
      snapshot,
      priorHistory = history
    )
    assertTrue("Expected coreference half of ₹1,200 -> ₹600, got: $followUpReply", followUpReply.contains("600"))

    // 3. A-vs-B comparison ask (₹2,400 vs ₹900 -> ₹1,500 difference)
    val (cmpIntent, _) = engine.classifyIntent("Should I pick ₹2400 cafe party vs ₹900 home dinner?")
    assertEquals(MaaMiniIntent.MULTI_OPTION_COMPARISON, cmpIntent)
    val cmpReply = engine.generateReply("Should I pick ₹2400 cafe party vs ₹900 home dinner?", snapshot)
    assertTrue("Expected ₹1,500 comparison savings, got: $cmpReply", cmpReply.contains("1,500"))

    // 4. Daily saving projection ask (₹150 daily for 30 days -> ₹4,500)
    val projReply = engine.generateReply("If I save ₹150 daily for 30 days how much will I have?", snapshot)
    assertTrue("Expected ₹4,500 projection, got: $projReply", projReply.contains("4,500"))

    // 5. Pakke bills & subscriptions reasoning
    val billsReply = engine.generateReply("Which bills and subscriptions are still due this month?", snapshot)
    assertTrue("Expected Wi-Fi or total ₹1,098 pending outflow, got: $billsReply", billsReply.contains("1,098"))

    // 6. Receivables reasoning
    val recReply = engine.generateReply("Check my pending receivables owed to me this cycle", snapshot)
    assertTrue("Expected ₹5,000 Client Retainer receivable, got: $recReply", recReply.contains("5,000"))

    // 7. Bill split & SIP compounding math
    val splitReply = engine.generateReply("Split ₹1800 dinner among 3 friends", snapshot)
    assertTrue("Expected ₹600 per person split, got: $splitReply", splitReply.contains("600"))

    val sipReply = engine.generateReply("Project a ₹3000 monthly SIP for 5 years", snapshot)
    assertTrue("Expected SIP maturity projection, got: $sipReply", sipReply.contains("3,000"))

    // 8. Compound 2-part prompt decomposition
    val compoundReply = engine.generateReply(
      "Split ₹1500 among 3 friends and check my pending receivables",
      snapshot
    )
    assertTrue("Expected both ₹500 split and ₹5,000 receivable, got: $compoundReply", compoundReply.contains("500") && compoundReply.contains("5,000"))

    // 9. Habit check grounded in actual Khata transactions
    val habitReply = engine.generateReply("How is my chai habit doing this week?", snapshot)
    assertTrue(habitReply.contains("Sharma Tea"))
    assertTrue(habitReply.contains("₹70"))

    // 10. Gullak progress check
    val gullakReply = engine.generateReply("Is Bhai's clay Gullak filling up?", snapshot)
    assertTrue(gullakReply.contains("Himachal Trip"))
    assertTrue(gullakReply.contains("45%"))

    // 11. Off-topic refusal
    val (offIntent, _) = engine.classifyIntent("Write python script to scrape websites")
    assertEquals(MaaMiniIntent.OFF_TOPIC_REFUSAL, offIntent)
    val offReply = engine.generateReply("Tell me the capital of France", snapshot)
    assertTrue(offReply.contains("Batua"))

    // 12. Streaming chunks concatenate cleanly
    val streamedChunks = engine.streamReply("Hi Maa", snapshot, tokenDelayMs = 0L).toList()
    assertTrue(streamedChunks.isNotEmpty())
    assertTrue(streamedChunks.joinToString("").contains("Harshit"))

    // Clean uninstall deletes 492 MB artifact from disk
    assertTrue(reloadedManager.uninstallModel())
    assertFalse(reloadedManager.isInstalled)
    assertTrue(reloadedManager.status.value is MiniModelStatus.NotInstalled)
  }

  @Test
  fun hisaabCalculatorEngine_evaluatesPrecedenceDiscountsAndSplitsAccurately() {
    assertEquals(440L, HisaabCalculatorEngine.evaluateRoundedRupees("180+260"))
    assertEquals(200L, HisaabCalculatorEngine.evaluateRoundedRupees("100+50×2"))
    assertEquals(300L, HisaabCalculatorEngine.evaluateRoundedRupees("1200÷4"))
    assertEquals(800L, HisaabCalculatorEngine.evaluateRoundedRupees("1000-20%"))
    assertEquals(590L, HisaabCalculatorEngine.evaluateRoundedRupees("500+18%"))

    var expr = ""
    listOf("2", "5", "0", "+", "1", "5", "0", "=").forEach { key ->
      expr = HisaabCalculatorEngine.appendKey(expr, key)
    }
    assertEquals("400", expr)
  }

  @Test
  fun chitthiWisdomBankAndGullakImpact_maintainRichnessAndAccurateDeltas() {
    assertTrue(ChitthiWisdomBank.maaSeeds.size >= 14)
    assertTrue(ChitthiWisdomBank.bhaiSeeds.size >= 6)
    val note = ChitthiWisdomBank.pickMaaSeed(0).toChitthiNote()
    assertNotNull(note.title)
    assertTrue(note.body.isNotBlank())

    val depositTx = Transaction(
      amountPaise = 500_00L,
      direction = Direction.DEBIT,
      merchant = "Gullak: Himachal Trip",
      category = Category.TRANSFERS
    )
    val dipTx = Transaction(
      amountPaise = 200_00L,
      direction = Direction.CREDIT,
      merchant = "Gullak Dip: Himachal Trip",
      category = Category.TRANSFERS
    )
    val chaiTx = Transaction(
      amountPaise = 30_00L,
      direction = Direction.DEBIT,
      merchant = "Sharma Tea",
      category = Category.CHAI_SNACKS
    )

    assertEquals(500_00L, depositTx.gullakImpactPaise())
    assertEquals(-200_00L, dipTx.gullakImpactPaise())
    assertEquals(0L, chaiTx.gullakImpactPaise())
  }

  @Test
  fun cloudHighDemandClassifier_detectsOverloadedAndRejectsPermanent404Or400Errors() {
    assertTrue(
      MaaChatViewModel.isHighDemandOrTransientCloudError(
        RuntimeException("HTTP 503 UNAVAILABLE: The model is overloaded due to high demand. Please try again later.")
      )
    )
    assertTrue(
      MaaChatViewModel.isHighDemandOrTransientCloudError(
        RuntimeException("429 RESOURCE_EXHAUSTED: Quota exceeded")
      )
    )
    assertFalse(
      MaaChatViewModel.isHighDemandOrTransientCloudError(
        RuntimeException("404 NOT_FOUND: Model gemini-1.5-flash is retired and unavailable")
      )
    )
    assertFalse(
      MaaChatViewModel.isHighDemandOrTransientCloudError(
        RuntimeException("400 INVALID_ARGUMENT: Please ensure that multiturn requests alternate between user and model")
      )
    )
    assertFalse(
      MaaChatViewModel.isHighDemandOrTransientCloudError(
        IllegalArgumentException("Invalid API key format")
      )
    )
  }

  @Test
  fun activeCloudModelsAndLiveMarketGate_useSupportedGemini3AndNeverSpoofOfflineRates() = runBlocking {
    assertEquals("gemini-3.5-flash-lite", MaaChatViewModel.PRIMARY_CLOUD_MODEL)
    assertEquals("gemini-3.8-flash", MaaChatViewModel.FALLBACK_LITE_CLOUD_MODEL)
    assertEquals("gemini-3.8-flash", com.example.ui.bhairoom.PapaChatViewModel.PRIMARY_CLOUD_MODEL)
    assertEquals("gemini-3.5-flash-lite", com.example.ui.bhairoom.PapaChatViewModel.FALLBACK_LITE_CLOUD_MODEL)

    assertTrue(com.example.core.ai.FamilyDomainRouter.isLiveMarketDataQuery("What is today's 24K gold price in India?"))
    assertTrue(com.example.core.ai.FamilyDomainRouter.shouldMaaReferToPapa("What is the gold price today?"))

    // Multi-turn history sanitizer drops leading model message & trailing unanswered user message
    val contents = MaaChatViewModel.buildAlternatingConversationContents(
      priorTurns = listOf(
        "model" to "Welcome beta!",
        "user" to "First question",
        "model" to "First answer",
        "user" to "Unanswered retry"
      ),
      finalUserPrompt = "Fresh grounded question"
    )
    assertEquals(3, contents.size)
    assertEquals("user", contents[0].role)
    assertEquals("model", contents[1].role)
    assertEquals("user", contents[2].role)

    // Verify Papa refuses to silently spoof live gold prices from offline templates when Online mode is selected
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val root = DefaultRootAppContainer(app)
    val offlineManager = MaaMiniModelManager(app, simulatePacing = false)
    offlineManager.networkAvailabilityOverrideForTest = false
    root.preferencesRepository.setMaaAiEngineMode(com.example.core.model.MaaAiEngineMode.CLOUD_AUTO)
    root.guestContainer.papaChatRepository.clearHistory()

    val papaVm = com.example.ui.bhairoom.PapaChatViewModel(
      chatRepository = root.guestContainer.papaChatRepository,
      preferencesRepository = root.preferencesRepository,
      miniModelManager = offlineManager
    )
    val profile = BudgetProfile(monthlyIncomePaise = 60_000_00L, fixedCommitmentsPaise = 20_000_00L, savingsTargetPaise = 10_000_00L)
    val safe = SafeToSpendCalculator.calculate(profile, spentSoFarPaise = 0L)
    val goal = SavingsGoal(name = "Emergency Pot", targetAmountPaise = 30_000_00L, currentAmountPaise = 5_000_00L)
    val snapshot = MaaHouseholdSnapshot(
      userName = "Harshit",
      safeToSpend = safe,
      profile = profile,
      goal = goal,
      allGoals = listOf(goal),
      transactions = emptyList()
    )

    papaVm.sendMessage("What is today's 24K gold price in India?", snapshot)
    val mainShadowLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
    var attempts = 0
    while (attempts < 40 && papaVm.uiState.value !is com.example.ui.bhairoom.PapaChatUiState.Error) {
      mainShadowLooper.idleFor(java.time.Duration.ofMillis(40))
      delay(15L)
      attempts++
    }
    assertTrue(
      "Expected Error state refusing offline spoofing for live gold price, got ${papaVm.uiState.value}",
      papaVm.uiState.value is com.example.ui.bhairoom.PapaChatUiState.Error
    )
  }

  @Test
  fun quickChipDefaultsAndFormatting_areConsistent() {
    val defaults = QuickChip.defaultChips()
    assertTrue(defaults.size >= 4)
    val customMetro = QuickChip(
      id = "chip_metro",
      label = "Metro",
      merchant = "DMRC Card",
      amountPaise = 45_00L,
      category = Category.COMMUTE,
      colorKey = StampColorKey.SKY
    )
    assertEquals("₹45", formatQuickChipSublabel(customMetro))
  }

  @Test
  fun maaChatViewModel_works100PercentOfflineImmediatelyEvenBefore492MbUnpack() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val root = DefaultRootAppContainer(app)
    val offlineManager = MaaMiniModelManager(app, simulatePacing = false)
    offlineManager.uninstallModel()
    assertFalse("Disk 492 MB file should be uninstalled first", offlineManager.isInstalled)

    // Simulate 100% offline / Airplane Mode
    offlineManager.networkAvailabilityOverrideForTest = false
    assertFalse(offlineManager.isNetworkAvailable())

    // Verify instant in-memory engine works with zero disk file and zero network
    val instantEngine = offlineManager.getOrCreateInstantOfflineEngine()
    assertNotNull(instantEngine)

    root.guestContainer.maaChatRepository.clearHistory()
    val viewModel = MaaChatViewModel(
      chatRepository = root.guestContainer.maaChatRepository,
      preferencesRepository = root.preferencesRepository,
      miniModelManager = offlineManager
    )

    val profile = BudgetProfile(
      monthlyIncomePaise = 60_000_00L,
      fixedCommitmentsPaise = 20_000_00L,
      savingsTargetPaise = 10_000_00L
    )
    val safe = SafeToSpendCalculator.calculate(profile, spentSoFarPaise = 100_00L)
    val goal = SavingsGoal(name = "Emergency Pot", targetAmountPaise = 30_000_00L, currentAmountPaise = 5_000_00L)
    val snapshot = MaaHouseholdSnapshot(
      userName = "Riya",
      safeToSpend = safe,
      profile = profile,
      goal = goal,
      allGoals = listOf(goal),
      transactions = emptyList()
    )

    viewModel.sendMessage("Can I buy ₹200 coffee and ₹300 dinner tonight?", snapshot)

    val mainShadowLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
    var stored = emptyList<MaaChatMessage>()
    var attempts = 0
    while (attempts < 120 && (stored.size < 2 || viewModel.uiState.value !is MaaChatUiState.Idle)) {
      mainShadowLooper.idleFor(java.time.Duration.ofMillis(50))
      delay(15L)
      mainShadowLooper.idleFor(java.time.Duration.ofMillis(50))
      stored = root.guestContainer.maaChatRepository.observeMessages().first()
      attempts++
    }

    assertTrue("Expected both USER and MAA offline messages in store, got ${stored.size}", stored.size >= 2)
    val maaReply = stored.last { it.sender == ChatSender.MAA }.text
    assertTrue("Offline local LLM should compute combined ₹500 basket, got: $maaReply", maaReply.contains("500"))
    assertTrue("ViewModel must finish in Idle without cloud timeout error", viewModel.uiState.value is MaaChatUiState.Idle)
  }
}
