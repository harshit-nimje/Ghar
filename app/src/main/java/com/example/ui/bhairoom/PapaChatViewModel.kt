package com.example.ui.bhairoom

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.FamilyDomainRouter
import com.example.core.ai.MaaMiniModelManager
import com.example.core.ai.MiniModelStatus
import com.example.core.ai.PapaOfflineAdvisor
import com.example.core.model.ChatSender
import com.example.core.model.GroundingSourceChip
import com.example.core.model.MaaAiEngineMode
import com.example.core.model.PapaChatMessage
import com.example.core.util.CurrencyFormatter
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.PapaChatStore
import com.example.ui.kitchen.MaaChatViewModel
import com.example.ui.kitchen.MaaHouseholdSnapshot
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "PapaChatViewModel"

sealed interface PapaChatUiState {
  data object Idle : PapaChatUiState
  data class Loading(val statusHint: String = "Checking savings & rates...") : PapaChatUiState
  data class Streaming(
    val partialText: String,
    val engineBadge: String = "Writing...",
    val sources: List<GroundingSourceChip> = emptyList()
  ) : PapaChatUiState
  data class Error(
    val message: String,
    val lastPrompt: String? = null
  ) : PapaChatUiState
}

/**
 * ViewModel for "Sit with Papa" / "Ask Dad" spatial study chat.
 * Uses `gemini-3.8-flash` with Google Search grounding (`Tool.googleSearch()`) so Papa can
 * cite real-world, up-to-date FD/RD/PPF interest rates, RBI repo/inflation context, index SIP
 * principles, and tax rules alongside the user's exact household numbers—plus seamless
 * `gemini-3.5-flash-lite` backup and 100% offline `PapaOfflineAdvisor` fallback.
 */
class PapaChatViewModel(
  private val chatRepository: PapaChatStore,
  private val preferencesRepository: PreferencesRepository,
  private val miniModelManager: MaaMiniModelManager
) : ViewModel() {

  private val groundedFlashConfig by lazy {
    generationConfig {
      temperature = 0.15f
    }
  }

  private val groundedLiteConfig by lazy {
    generationConfig {
      temperature = 0.2f
    }
  }

  private val groundedPapaCloudModel: GenerativeModel by lazy {
    Firebase.ai.generativeModel(
      modelName = PRIMARY_CLOUD_MODEL,
      generationConfig = groundedFlashConfig,
      systemInstruction = content { text(PAPA_SYSTEM_INSTRUCTION) },
      tools = listOf(Tool.googleSearch())
    )
  }

  private val fallbackPapaCloudModel: GenerativeModel by lazy {
    Firebase.ai.generativeModel(
      modelName = FALLBACK_LITE_CLOUD_MODEL,
      generationConfig = groundedLiteConfig,
      systemInstruction = content { text(PAPA_SYSTEM_INSTRUCTION) },
      tools = listOf(Tool.googleSearch())
    )
  }

  private val plainGroundedPapaCloudModel: GenerativeModel by lazy {
    Firebase.ai.generativeModel(
      modelName = PRIMARY_CLOUD_MODEL,
      generationConfig = groundedFlashConfig,
      systemInstruction = content { text(PLAIN_ENGLISH_DAD_SYSTEM_INSTRUCTION) },
      tools = listOf(Tool.googleSearch())
    )
  }

  private val plainFallbackPapaCloudModel: GenerativeModel by lazy {
    Firebase.ai.generativeModel(
      modelName = FALLBACK_LITE_CLOUD_MODEL,
      generationConfig = groundedLiteConfig,
      systemInstruction = content { text(PLAIN_ENGLISH_DAD_SYSTEM_INSTRUCTION) },
      tools = listOf(Tool.googleSearch())
    )
  }

  val messages: StateFlow<List<PapaChatMessage>> = chatRepository.observeMessages()
    .catch { e ->
      Log.w(TAG, "Error observing Papa chat history", e)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000L),
      initialValue = emptyList()
    )

  val engineMode: StateFlow<MaaAiEngineMode> = preferencesRepository.maaAiEngineMode
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000L),
      initialValue = MaaAiEngineMode.CLOUD_AUTO
    )

  val miniModelStatus: StateFlow<MiniModelStatus> = miniModelManager.status

  private val _uiState = MutableStateFlow<PapaChatUiState>(PapaChatUiState.Idle)
  val uiState: StateFlow<PapaChatUiState> = _uiState.asStateFlow()

  private val _referralToMaaPrompt = MutableStateFlow<String?>(null)
  val referralToMaaPrompt: StateFlow<String?> = _referralToMaaPrompt.asStateFlow()

  fun dismissMaaReferral() {
    _referralToMaaPrompt.value = null
  }

  fun selectEngineMode(mode: MaaAiEngineMode) {
    viewModelScope.launch {
      preferencesRepository.setMaaAiEngineMode(mode)
    }
  }

  fun installAndActivateMiniModel() {
    viewModelScope.launch {
      val result = miniModelManager.installModel()
      if (result.isSuccess) {
        preferencesRepository.setMaaAiEngineMode(MaaAiEngineMode.MINI_LOCAL)
      }
    }
  }

  fun cancelMiniModelInstall() {
    miniModelManager.cancelInstall()
  }

  fun uninstallMiniModel() {
    viewModelScope.launch {
      miniModelManager.uninstallModel()
      if (engineMode.value == MaaAiEngineMode.MINI_LOCAL) {
        preferencesRepository.setMaaAiEngineMode(MaaAiEngineMode.CLOUD_AUTO)
      }
    }
  }

  fun sendMessage(
    userText: String,
    snapshot: MaaHouseholdSnapshot,
    appendUserTurn: Boolean = true
  ) {
    val cleanInput = userText.trim()
    if (cleanInput.isEmpty()) return
    if (_uiState.value is PapaChatUiState.Loading || _uiState.value is PapaChatUiState.Streaming) return

    viewModelScope.launch {
      _referralToMaaPrompt.value = null
      val mode = engineMode.value
      val isOffline = !miniModelManager.isNetworkAvailable()
      val requiresLiveMarketData = FamilyDomainRouter.isLiveMarketDataQuery(cleanInput)
      val useLocalDirectly = mode == MaaAiEngineMode.MINI_LOCAL || (isOffline && !requiresLiveMarketData)

      _uiState.value = PapaChatUiState.Loading(
        statusHint = when {
          mode == MaaAiEngineMode.MINI_LOCAL && snapshot.plainEnglish -> "Dad is calculating offline..."
          mode == MaaAiEngineMode.MINI_LOCAL -> "Papa is checking your Gullak math offline..."
          requiresLiveMarketData && snapshot.plainEnglish -> "Dad is checking live rates..."
          requiresLiveMarketData -> "Papa is checking live rates..."
          snapshot.plainEnglish -> "Dad is thinking..."
          else -> "Papa is thinking..."
        }
      )

      val priorHistory = messages.value.takeLast(8)

      if (appendUserTurn) {
        runCatching {
          chatRepository.appendMessage(ChatSender.USER, cleanInput)
        }
      }

      // Layer 1 fast guardrail against blatant non-financial abuse
      if (MaaChatViewModel.isBlatantOffTopicAbuse(cleanInput)) {
        val boundary = buildFatherlyBoundaryReply(snapshot.userName, snapshot.plainEnglish)
        runCatching {
          chatRepository.appendMessage(ChatSender.PAPA, boundary)
        }
        _uiState.value = PapaChatUiState.Idle
        return@launch
      }

      // Layer 2 household role split: petty daily cash, snacks, dinner orders & tiny wallet math go to Maa / Mom
      if (FamilyDomainRouter.shouldPapaReferToMaa(cleanInput)) {
        val referralReply = FamilyDomainRouter.composePapaReferralToMaa(
          userName = snapshot.userName,
          query = cleanInput,
          snapshot = snapshot
        )
        runCatching {
          chatRepository.appendMessage(ChatSender.PAPA, referralReply)
        }
        _referralToMaaPrompt.value = cleanInput
        _uiState.value = PapaChatUiState.Idle
        return@launch
      }

      // If Online mode is selected for a live market/rate query while the device is offline,
      // refuse to spoof live market data from static offline templates.
      if (mode == MaaAiEngineMode.CLOUD_AUTO && isOffline && requiresLiveMarketData) {
        val speaker = if (snapshot.plainEnglish) "Dad" else "Papa"
        _uiState.value = PapaChatUiState.Error(
          message = "No internet connection detected. $speaker won't guess live market prices offline—please reconnect and tap Retry.",
          lastPrompt = cleanInput
        )
        return@launch
      }

      if (useLocalDirectly) {
        runOfflinePapaInference(cleanInput, snapshot, priorHistory)
        return@launch
      }

      // For live market queries, strip prior assistant turns that may contain stale/offline rates
      // so the model relies strictly on fresh Google Search grounding for today's figures.
      val cleanPriorTurns = if (requiresLiveMarketData) {
        emptyList()
      } else {
        priorHistory.map { msg ->
          (if (msg.sender == ChatSender.USER) "user" else "model") to msg.text
        }
      }

      val contextFramedPrompt = buildPapaContextFramedPrompt(
        userQuestion = cleanInput,
        snap = snapshot,
        requiresLiveMarketData = requiresLiveMarketData
      )
      val conversationContents = MaaChatViewModel.buildAlternatingConversationContents(
        priorTurns = cleanPriorTurns,
        finalUserPrompt = contextFramedPrompt
      )

      val (cloudResult, cloudFailure) = executeGroundedCloudGeneration(
        conversationContents = conversationContents,
        plainEnglish = snapshot.plainEnglish,
        requiresLiveMarketData = requiresLiveMarketData
      )
      if (cloudResult != null) {
        val (replyText, sources) = cloudResult
        runCatching {
          chatRepository.appendMessage(ChatSender.PAPA, replyText, sources)
        }
        _uiState.value = PapaChatUiState.Idle
        return@launch
      }

      // CRITICAL FACTUALITY GATE: Never fall back silently to static offline templates when the user
      // asked for live real-world market prices, bullion rates, indices, or current bank/RBI rates.
      if (requiresLiveMarketData) {
        val speaker = if (snapshot.plainEnglish) "Dad" else "Papa"
        val detail = cloudFailure?.localizedMessage?.take(110)?.let { " ($it)" }.orEmpty()
        _uiState.value = PapaChatUiState.Error(
          message = "Couldn't reach live Google Search right now to verify today's exact rate$detail. $speaker never guesses real-world financial data—tap Retry in a moment.",
          lastPrompt = cleanInput
        )
        return@launch
      }

      Log.i(TAG, "Cloud models unavailable for non-market household query; failing over to Local 0.5B LLM.")
      runOfflinePapaInference(cleanInput, snapshot, priorHistory)
    }
  }

  private suspend fun executeGroundedCloudGeneration(
    conversationContents: List<Content>,
    plainEnglish: Boolean,
    requiresLiveMarketData: Boolean
  ): Pair<Pair<String, List<GroundingSourceChip>>?, Throwable?> {
    val flashModel = if (plainEnglish) plainGroundedPapaCloudModel else groundedPapaCloudModel
    val liteModel = if (plainEnglish) plainFallbackPapaCloudModel else fallbackPapaCloudModel
    val statusLabel = when {
      requiresLiveMarketData && plainEnglish -> "Dad is checking live rates..."
      requiresLiveMarketData -> "Papa is checking live rates..."
      plainEnglish -> "Dad is thinking..."
      else -> "Papa is thinking..."
    }

    // Route live market/rate queries to Gemini 3.8 Flash first for strongest Google Search synthesis,
    // and general household savings math to ultra-fast Gemini 3.5 Flash-Lite first.
    val firstModel = if (requiresLiveMarketData) flashModel else liteModel
    val firstModelName = if (requiresLiveMarketData) PRIMARY_CLOUD_MODEL else FALLBACK_LITE_CLOUD_MODEL
    val secondModel = if (requiresLiveMarketData) liteModel else flashModel
    val secondModelName = if (requiresLiveMarketData) FALLBACK_LITE_CLOUD_MODEL else PRIMARY_CLOUD_MODEL
    var lastError: Throwable? = null

    // Attempt 1: Primary selected Gemini 3.x model + Google Search Grounding
    for (attempt in 1..2) {
      try {
        val (streamedText, sources) = streamWithGrounding(
          model = firstModel,
          contents = conversationContents,
          streamingBadge = statusLabel,
          timeoutMs = 55_000L
        )
        if (streamedText.isNotBlank()) {
          return (streamedText to sources) to null
        }
      } catch (e: CancellationException) {
        if (e !is TimeoutCancellationException) throw e
        lastError = e
        Log.e(TAG, "Timed out waiting for grounded Papa model $firstModelName on attempt $attempt", e)
      } catch (e: Exception) {
        lastError = e
        Log.e(TAG, "Grounded Papa model $firstModelName attempt $attempt failed", e)
        if (attempt == 1 && MaaChatViewModel.isHighDemandOrTransientCloudError(e)) {
          _uiState.value = PapaChatUiState.Loading(statusLabel)
          delay(650L)
          continue
        }
      }
      break
    }

    // Attempt 2: Complementary Gemini 3.x model + Google Search Grounding (stream, then unary fallback)
    try {
      _uiState.value = PapaChatUiState.Loading(statusLabel)
      val (secondText, secondSources) = streamWithGrounding(
        model = secondModel,
        contents = conversationContents,
        streamingBadge = statusLabel,
        timeoutMs = 45_000L
      )
      if (secondText.isNotBlank()) {
        return (secondText to secondSources) to null
      }
    } catch (e: CancellationException) {
      if (e !is TimeoutCancellationException) throw e
      lastError = e
      Log.e(TAG, "Backup Papa model $secondModelName stream timed out", e)
    } catch (e: Exception) {
      lastError = e
      Log.e(TAG, "Backup Papa model $secondModelName stream failed, trying unary grounded call", e)
      try {
        val unaryResponse = withTimeout(40_000L) {
          secondModel.generateContent(conversationContents)
        }
        val unaryText = unaryResponse.text?.trim().orEmpty()
        if (unaryText.isNotBlank()) {
          val unarySources = extractGroundingChips(unaryResponse).distinctBy { it.uri }.take(3)
          return (unaryText to unarySources) to null
        }
      } catch (e2: CancellationException) {
        if (e2 !is TimeoutCancellationException) throw e2
        lastError = e2
      } catch (e2: Exception) {
        lastError = e2
        Log.e(TAG, "Unary grounded Papa fallback also failed", e2)
      }
    }

    return null to lastError
  }

  private suspend fun streamWithGrounding(
    model: GenerativeModel,
    contents: List<Content>,
    streamingBadge: String,
    timeoutMs: Long
  ): Pair<String, List<GroundingSourceChip>> {
    val accumulated = StringBuilder()
    val collectedByUri = LinkedHashMap<String, GroundingSourceChip>()

    withTimeout(timeoutMs) {
      model.generateContentStream(contents).collect { chunk ->
        val piece = chunk.text.orEmpty()
        extractGroundingChips(chunk).forEach { chip ->
          if (chip.uri.isNotBlank() && collectedByUri.size < 3 && !collectedByUri.containsKey(chip.uri)) {
            collectedByUri[chip.uri] = chip
          }
        }
        if (piece.isNotEmpty() || collectedByUri.isNotEmpty()) {
          if (piece.isNotEmpty()) {
            accumulated.append(piece)
          }
          _uiState.value = PapaChatUiState.Streaming(
            partialText = accumulated.toString(),
            engineBadge = streamingBadge,
            sources = collectedByUri.values.toList()
          )
        }
      }
    }

    return accumulated.toString().trim() to collectedByUri.values.toList()
  }

  private fun extractGroundingChips(response: GenerateContentResponse): List<GroundingSourceChip> {
    val chunks = response.candidates.firstOrNull()?.groundingMetadata?.groundingChunks ?: return emptyList()
    return chunks.mapNotNull { chunk ->
      val web = chunk.web ?: return@mapNotNull null
      val uri = web.uri?.trim().orEmpty()
      if (uri.isEmpty()) return@mapNotNull null
      val rawDomain = web.domain?.trim().orEmpty()
      val rawTitle = web.title?.trim().orEmpty()
      val displayTitle = when {
        rawTitle.isNotEmpty() -> rawTitle.take(42)
        rawDomain.isNotEmpty() -> rawDomain.take(36)
        else -> "Web Source"
      }
      GroundingSourceChip(
        title = displayTitle,
        uri = uri,
        domain = rawDomain
      )
    }
  }

  private suspend fun runOfflinePapaInference(
    cleanInput: String,
    snapshot: MaaHouseholdSnapshot,
    priorHistory: List<PapaChatMessage>
  ) {
    try {
      val engine = miniModelManager.getOrCreateInstantOfflineEngine()
      val isFullDiskInstalled = miniModelManager.isInstalled
      val badge = when {
        snapshot.plainEnglish && isFullDiskInstalled -> "Local 0.5B LLM • 492 MB"
        snapshot.plainEnglish -> "Local 0.5B LLM • Offline"
        isFullDiskInstalled -> "Papa Local 0.5B • 492 MB"
        else -> "Papa Local 0.5B • Offline"
      }
      val accumulated = StringBuilder()
      engine.streamPapaReply(cleanInput, snapshot, priorHistory).collect { piece ->
        accumulated.append(piece)
        _uiState.value = PapaChatUiState.Streaming(
          partialText = accumulated.toString(),
          engineBadge = badge,
          sources = emptyList()
        )
      }
      val finalReply = accumulated.toString().trim()
      if (finalReply.isNotEmpty()) {
        runCatching {
          chatRepository.appendMessage(ChatSender.PAPA, finalReply, emptyList())
        }
      }
      _uiState.value = PapaChatUiState.Idle
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Offline Papa inference failed", e)
      _uiState.value = PapaChatUiState.Error(
        message = e.localizedMessage ?: "Couldn't calculate advice right now.",
        lastPrompt = cleanInput
      )
    }
  }

  fun clearChat() {
    viewModelScope.launch {
      _uiState.value = PapaChatUiState.Idle
      _referralToMaaPrompt.value = null
      runCatching { chatRepository.clearHistory() }
    }
  }

  fun dismissError() {
    if (_uiState.value is PapaChatUiState.Error) {
      _uiState.value = PapaChatUiState.Idle
    }
  }

  companion object {
    const val PRIMARY_CLOUD_MODEL = "gemini-3.8-flash"
    const val FALLBACK_LITE_CLOUD_MODEL = "gemini-3.5-flash-lite"

    internal fun buildFatherlyBoundaryReply(userName: String, plainEnglish: Boolean = false): String {
      if (plainEnglish) {
        val cleanName = userName.trim().ifBlank { "there" }
        return "Hey $cleanName, I'm here to help you plan savings goals, investments, interest rates, inflation protection, and big financial decisions—not coding, homework, or unrelated tasks. Ask me about SIPs, FDs, PPF, gold, or growing a lump sum!"
      }
      val cleanName = userName.trim().ifBlank { "beta" }
      return "Arre $cleanName beta, main yahan tumhari bachat, Gullak, SIP-FD rates aur badi rakam ki samajhdari ke liye baitha hoon—coding ya homework ke sawaal mujhse mat poocho! Ask me about investments, live rates, inflation, or where to park a big amount."
    }

    private fun buildPapaContextFramedPrompt(
      userQuestion: String,
      snap: MaaHouseholdSnapshot,
      requiresLiveMarketData: Boolean
    ): String {
      val name = snap.userName.trim().ifBlank { if (snap.plainEnglish) "Friend" else "beta" }
      val allGoals = snap.allGoals.takeIf { it.isNotEmpty() } ?: listOf(snap.goal)
      val goalsSummary = allGoals.joinToString("; ") { g ->
        "'${g.name}': ${CurrencyFormatter.formatRupees(g.currentAmountRupees)} / ${CurrencyFormatter.formatRupees(g.targetAmountRupees)} (${g.progressPercent.toInt()}%, via ${g.instrumentType.displayName})"
      }
      val threeMonthEmergency = snap.profile.fixedCommitmentsRupees * 3L
      val todayDate = MaaChatViewModel.currentRealWorldDateLabel()

      val liveSearchDirective = if (requiresLiveMarketData) {
        """

[LIVE_INDIA_MARKET_GROUNDING_DIRECTIVE]
- Today's exact real-world date is $todayDate (Country: India, Currency: INR ₹).
- You MUST invoke Google Search BEFORE writing a single number. Anchor your search query to INDIA and the current month/year ($todayDate).
- Reject outdated 2022–2025 snippets. Verify the date of the search result matches the current year.
- If asked about GOLD or SILVER price: Search specifically for today's Indian retail / IBJA / MCX 24K and 22K gold (or silver) rate in INR per 10 grams (and per 1 gram) in India as of $todayDate. NEVER convert international USD/oz spot prices in your head (which misses Indian import duties & local bullion premiums). Clearly state the unit (e.g., per 10g for 24K & 22K in ₹) and mention that retail jewellers add 3% GST + making charges.
- If asked about FD, PPF, SSY, RBI Repo, Nifty/Sensex, or CPI inflation: Quote the exact latest verified Indian figure from Google Search and mention the period/as-of date.
- If live search snippets conflict slightly across cities/banks, state the tight verified range rather than inventing a single fake number.
[/LIVE_INDIA_MARKET_GROUNDING_DIRECTIVE]"""
      } else {
        ""
      }

      return """
[GHAR_PAPA_STUDY_CONTEXT]
Current Real-World Date: $todayDate
Country & Currency: India (INR ₹)
Language Mode: ${if (snap.plainEnglish) "PLAIN_ENGLISH (No Hinglish or Indian slang)" else "INDIAN_ENGLISH"}
User Name: $name
Life Stage: ${snap.profile.lifeStage}
Monthly Income: ${CurrencyFormatter.formatRupees(snap.profile.monthlyIncomeRupees)}
Fixed Monthly Commitments: ${CurrencyFormatter.formatRupees(snap.profile.fixedCommitmentsRupees)} (3-month emergency benchmark = ${CurrencyFormatter.formatRupees(threeMonthEmergency)})
Monthly Savings Target: ${CurrencyFormatter.formatRupees(snap.profile.savingsTargetRupees)}
Active Savings Pots / Gullaks: $goalsSummary
Safe to Spend Today (for context): ${CurrencyFormatter.formatRupees(snap.safeToSpend.dailyAllowanceRupees)} (${snap.safeToSpend.daysRemaining} days left in cycle)
[/GHAR_PAPA_STUDY_CONTEXT]$liveSearchDirective

User's question to ${if (snap.plainEnglish) "Dad" else "Papa"}:
$userQuestion
""".trimIndent()
    }

    private val PAPA_SYSTEM_INSTRUCTION = """
You are Papa, a warm, calm, rational, grounded Indian father in the "Ghar" household finance app. You guide long-term savings, Gullaks, investments, and real-world Indian financial decisions.

CRITICAL FACTUALITY & GOOGLE SEARCH GROUNDING RULES:
1. Check [GHAR_PAPA_STUDY_CONTEXT] for "Current Real-World Date" first on EVERY turn.
2. Whenever the user asks about ANY real-world price, rate, index, or rule (24K/22K gold or silver price in India, Nifty/Sensex, SBI/HDFC/ICICI FD rates, PPF/SSY/NPS rates, RBI repo rate, CPI inflation, USD-INR, or income tax slabs), you MUST use Google Search grounding anchored to India and the current date. NEVER rely on training-cutoff memory or guess numbers.
3. For Indian gold prices, always quote Indian market rates in ₹ (INR) per 10 grams (and/or 1g) for 24K and 22K—never quote raw USD/troy-ounce figures or rough mental conversions.
4. Keep responses concise, factual, and warm: 3 to 4 crisp sentences (under 100 words), no markdown bullet lists. Tie advice back to the user's household numbers when relevant.

STRICT DOMAIN BOUNDARY:
- Only discuss financial planning, savings, investments, and economic rates/insights. Politely refuse unrelated topics in 1–2 calm sentences.
""".trimIndent()

    private val PLAIN_ENGLISH_DAD_SYSTEM_INSTRUCTION = """
You are Dad, a warm, calm, rational, and experienced financial mentor in a personal budget & savings app for India (INR ₹).

CRITICAL FACTUALITY & GOOGLE SEARCH GROUNDING RULES:
1. Check [GHAR_PAPA_STUDY_CONTEXT] for "Current Real-World Date" first on EVERY turn.
2. Whenever the user asks about ANY real-world price, rate, index, or rule (24K/22K gold or silver price in India, Nifty/Sensex, Indian bank FD rates, PPF rates, RBI repo rate, inflation, or tax slabs), you MUST use Google Search grounding anchored to India and the current date. NEVER rely on training-cutoff memory or estimate numbers.
3. For gold prices in India, always quote Indian market rates in ₹ (INR) per 10 grams (and/or 1g) for 24K and 22K—never quote raw USD/oz figures or mental conversions.
4. Use clear, universal English only (no Hinglish words like beta, gullak, batua, rasoi). Keep responses concise: 3 to 4 crisp sentences (under 100 words), no markdown bullet lists.

STRICT DOMAIN BOUNDARY:
- Only discuss financial planning, savings, investments, and economic rates/insights. Politely refuse unrelated topics in 1–2 calm sentences.
""".trimIndent()
  }
}
