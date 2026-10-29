package com.example.ui.bhairoom

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.AppContainer
import com.example.R
import com.example.core.ai.MiniModelStatus
import com.example.core.audio.GharSoundEffects
import com.example.core.engine.SafeToSpendResult
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.model.BudgetProfile
import com.example.core.model.ChatSender
import com.example.core.model.ChitthiAuthor
import com.example.core.model.ChitthiNote
import com.example.core.model.GroundingSourceChip
import com.example.core.model.MaaAiEngineMode
import com.example.core.model.PakkaCommitment
import com.example.core.model.PapaChatMessage
import com.example.core.model.SavingsGoal
import com.example.core.model.Transaction
import com.example.core.util.CurrencyFormatter
import com.example.ui.common.GharConfirmDialog
import com.example.ui.common.JharokhaCardShape
import com.example.ui.common.LocalGharUndoController
import com.example.ui.common.StickyNotePaperColors
import com.example.ui.common.rememberCachedGharPainter
import com.example.ui.kitchen.GuidanceTopic
import com.example.ui.kitchen.MaaHouseholdSnapshot
import com.example.ui.kitchen.MaaMiniInstallerInlineCard
import com.example.ui.theme.GharHandStyles
import kotlinx.coroutines.launch
import kotlin.random.Random

private val PAPA_ROOM_SITTING_SCENES = intArrayOf(
  R.drawable.img_papa_room_mature_lean_1793199208784,
  R.drawable.img_papa_room_mature_cross_1793199209084
)

/**
 * Spatial watercolor Study-Room chat with Papa / Dad:
 * - Papa sits calmly in his cane armchair on the left with spectacles, financial diary, newspaper, and Gullak nearby;
 * - "You" sit curiously on the sofa on the right (randomized posture, tappable to shift);
 * - Typing fills your live Thought Bubble above "You", while Papa's grounded reply (plus optional
 *   Google Search source citations and 1-tap Maa handoff) appears in his single replacing Speech Bubble on the left.
 */
@Composable
fun PapaGuidanceDialog(
  container: AppContainer,
  userName: String,
  safeToSpend: SafeToSpendResult,
  profile: BudgetProfile,
  goal: SavingsGoal,
  transactions: List<Transaction>,
  commitments: List<PakkaCommitment> = emptyList(),
  allGoals: List<SavingsGoal> = emptyList(),
  initialPrompt: String? = null,
  onSwitchToMaa: ((String?) -> Unit)? = null,
  onDismiss: () -> Unit
) {
  val chatViewModel: PapaChatViewModel = viewModel(
    key = "papa_chat_${container.userId}",
    factory = viewModelFactory {
      initializer {
        PapaChatViewModel(
          chatRepository = container.papaChatRepository,
          preferencesRepository = container.preferencesRepository,
          miniModelManager = container.maaMiniModelManager
        )
      }
    }
  )

  val messages by chatViewModel.messages.collectAsStateWithLifecycle()
  val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
  val engineMode by chatViewModel.engineMode.collectAsStateWithLifecycle()
  val miniStatus by chatViewModel.miniModelStatus.collectAsStateWithLifecycle()
  val referralToMaaPrompt by chatViewModel.referralToMaaPrompt.collectAsStateWithLifecycle()
  val stashedChitthis by container.chitthiRepository.chitthisState.collectAsStateWithLifecycle()
  val prefs by container.preferencesRepository.prefsSnapshot.collectAsStateWithLifecycle()
  val soundEnabled = prefs.soundEnabled
  val stashedIds = remember(stashedChitthis) { stashedChitthis.map { it.id }.toSet() }
  val undoController = LocalGharUndoController.current
  val focusManager = LocalFocusManager.current
  val context = LocalContext.current
  val inputFocusRequester = remember { FocusRequester() }

  val plainEnglish = LocalPlainEnglishMode.current
  var customQuery by remember { mutableStateOf("") }
  var isInputFocused by remember { mutableStateOf(false) }
  var showClearChatConfirm by remember { mutableStateOf(false) }
  var showMiniModelPanel by remember { mutableStateOf(false) }
  val isMiniInstalled = miniStatus is MiniModelStatus.Installed
  val isMiniInstalling = miniStatus is MiniModelStatus.Installing

  LaunchedEffect(Unit) {
    GharSoundEffects.playChaiCupSettle(context, soundEnabled)
  }

  var sittingPoseIndex by remember {
    mutableIntStateOf(Random.nextInt(PAPA_ROOM_SITTING_SCENES.size))
  }

  val cleanName = remember(userName, plainEnglish) {
    userName.trim().ifBlank { if (plainEnglish) "friend" else "beta" }
  }
  val snapshot = remember(cleanName, safeToSpend, profile, goal, transactions, commitments, allGoals, plainEnglish) {
    MaaHouseholdSnapshot(
      userName = cleanName,
      safeToSpend = safeToSpend,
      profile = profile,
      goal = goal,
      transactions = transactions,
      commitments = commitments,
      allGoals = allGoals,
      plainEnglish = plainEnglish
    )
  }

  val quickTopics = remember(plainEnglish) {
    if (plainEnglish) {
      listOf(
        GuidanceTopic("invest_50k", "Where to put ₹50k?", "I have ₹50,000 saved up—how should I split it between safety and growth?"),
        GuidanceTopic("fd_vs_sip", "FD vs Index SIP", "What are current Indian FD and PPF interest rates vs starting a Nifty 50 SIP?"),
        GuidanceTopic("inflation_impact", "Inflation check", "How does current inflation in India affect idle cash in my bank account?"),
        GuidanceTopic("emergency_fund", "Emergency buffer", "How large should my emergency fund be before I invest in equity?"),
        GuidanceTopic("gold_hedge", "Gold / ETFs?", "How much of my savings should go into gold or Gold ETFs right now?"),
        GuidanceTopic("tax_basics", "Tax saving", "How should I think about Section 80C vs the New Tax Regime?")
      )
    } else {
      listOf(
        GuidanceTopic("invest_50k", "Park ₹50,000?", "Papa, mere paas ₹50,000 hain—how should I split it between safe FD and growth?"),
        GuidanceTopic("fd_vs_sip", "Live FD vs SIP", "Papa, what are current Indian bank FD & PPF rates vs a Nifty 50 Index SIP?"),
        GuidanceTopic("inflation_impact", "Inflation impact", "Papa, how does ~6% household inflation affect idle bank savings over 5 years?"),
        GuidanceTopic("emergency_fund", "Emergency cushion", "Papa, how big should my emergency buffer be before starting equity SIPs?"),
        GuidanceTopic("gold_hedge", "Gold / SGBs?", "Papa, how much should I allocate to gold or Gold ETFs alongside my Gullak?"),
        GuidanceTopic("tax_basics", "80C vs New Tax", "Papa, explain 80C tax-saving vs New Tax Regime in simple practical terms.")
      )
    }
  }

  var consumedInitialPrompt by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(initialPrompt) {
    val cleanInitial = initialPrompt?.trim().orEmpty()
    if (cleanInitial.isNotEmpty() && consumedInitialPrompt != cleanInitial) {
      consumedInitialPrompt = cleanInitial
      chatViewModel.sendMessage(cleanInitial, snapshot)
    }
  }

  val isBusy = uiState is PapaChatUiState.Loading || uiState is PapaChatUiState.Streaming
  val latestUserMessage = remember(messages) {
    messages.lastOrNull { it.sender == ChatSender.USER }
  }
  val latestPapaMessage = remember(messages) {
    messages.lastOrNull { it.sender == ChatSender.PAPA }
  }

  val canSend = customQuery.isNotBlank() && !isBusy
  val submitCurrentPrompt = {
    if (customQuery.isNotBlank() && !isBusy) {
      val prompt = customQuery.trim()
      customQuery = ""
      focusManager.clearFocus()
      chatViewModel.sendMessage(prompt, snapshot)
    }
  }

  val ambientTransition = rememberInfiniteTransition(label = "papa_room_ambient")
  val cloudFloatDp by ambientTransition.animateFloat(
    initialValue = -1.6f,
    targetValue = 1.6f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3000),
      repeatMode = RepeatMode.Reverse
    ),
    label = "papa_cloud_float"
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false
    )
  ) {
    Surface(
      shape = JharokhaCardShape,
      color = Color(0xFFF7F4E9),
      tonalElevation = 10.dp,
      shadowElevation = 16.dp,
      border = BorderStroke(1.2.dp, Color(0xFF6B846E).copy(alpha = 0.55f)),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.90f)
        .widthIn(max = 560.dp)
        .imePadding()
        .padding(vertical = 8.dp)
        .testTag("papa_guidance_dialog")
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFFF7F4E9))
      ) {
        // 1. Full-bleed Spatial Hand-Inked Watercolor Study Room Scene
        Crossfade(
          targetState = sittingPoseIndex,
          animationSpec = tween(durationMillis = 420),
          label = "papa_sitting_pose_crossfade",
          modifier = Modifier.fillMaxSize()
        ) { poseIdx ->
          val roomPainter = rememberCachedGharPainter(
            PAPA_ROOM_SITTING_SCENES[poseIdx % PAPA_ROOM_SITTING_SCENES.size]
          )
          Image(
            painter = roomPainter,
            contentDescription = "Warm Indian study room where Papa sits calmly in a cane armchair with spectacles and diary on the left and you sit curiously on the sofa on the right",
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(horizontalBias = 0f, verticalBias = 0.80f),
            modifier = Modifier.fillMaxSize()
          )
        }

        // Soft sage-parchment top ceiling wash + warm study dusk bottom gradient
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .align(Alignment.TopCenter)
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color(0xFFF7F4E9).copy(alpha = 0.90f),
                  Color(0xFFF7F4E9).copy(alpha = 0.36f),
                  Color.Transparent
                )
              )
            )
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(155.dp)
            .align(Alignment.BottomCenter)
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color.Transparent,
                  Color(0xFF1E2920).copy(alpha = 0.40f),
                  Color(0xFF172019).copy(alpha = 0.74f)
                )
              )
            )
        )

        // 2. Spatial Room Overlay
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
          SpatialPapaTopBar(
            plainEnglish = plainEnglish,
            engineMode = engineMode,
            miniStatus = miniStatus,
            hasHistory = messages.isNotEmpty(),
            isBusy = isBusy,
            onSelectCloud = {
              chatViewModel.selectEngineMode(MaaAiEngineMode.CLOUD_AUTO)
              showMiniModelPanel = false
            },
            onSelectOffline = {
              if (engineMode != MaaAiEngineMode.MINI_LOCAL) {
                chatViewModel.selectEngineMode(MaaAiEngineMode.MINI_LOCAL)
                if (!isMiniInstalled) {
                  showMiniModelPanel = true
                }
              } else {
                showMiniModelPanel = !showMiniModelPanel
              }
            },
            onRequestClear = { showClearChatConfirm = true },
            onDismiss = onDismiss
          )

          AnimatedVisibility(visible = showMiniModelPanel || isMiniInstalling) {
            Column {
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 6.dp
              ) {
                MaaMiniInstallerInlineCard(
                  miniStatus = miniStatus,
                  isActiveMode = engineMode == MaaAiEngineMode.MINI_LOCAL,
                  onInstallAndActivate = {
                    chatViewModel.installAndActivateMiniModel()
                    showMiniModelPanel = false
                  },
                  onCancelInstall = {
                    chatViewModel.cancelMiniModelInstall()
                  },
                  onActivateInstalled = {
                    chatViewModel.selectEngineMode(MaaAiEngineMode.MINI_LOCAL)
                    showMiniModelPanel = false
                  },
                  onUninstall = {
                    chatViewModel.uninstallMiniModel()
                    showMiniModelPanel = false
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 3. Spatial Conversation Stage
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .testTag("papa_chat_message_list")
          ) {
            // Subtle tap zone over the right sofa to shift sitting posture
            Box(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth(0.45f)
                .fillMaxHeight(0.42f)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null
                ) {
                  sittingPoseIndex = (sittingPoseIndex + 1) % PAPA_ROOM_SITTING_SCENES.size
                }
            )

            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.Top
            ) {
              // PAPA'S SPEECH BUBBLE (Anchored Left above Papa's armchair)
              PapaSpatialSpeechBubble(
                uiState = uiState,
                latestPapaMessage = latestPapaMessage,
                cleanName = cleanName,
                profile = profile,
                goal = goal,
                plainEnglish = plainEnglish,
                stashedIds = stashedIds,
                floatOffsetDp = cloudFloatDp * 0.6f,
                referralToMaaPrompt = referralToMaaPrompt,
                onHandoffToMaa = if (onSwitchToMaa != null) {
                  { promptToPass ->
                    chatViewModel.dismissMaaReferral()
                    onSwitchToMaa(promptToPass)
                  }
                } else null,
                onOpenSourceUrl = { uriStr ->
                  runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                  }
                },
                onToggleStash = { msg ->
                  val chitthiId = "papa_chat_${msg.id}"
                  if (chitthiId in stashedIds) {
                    container.chitthiRepository.remove(chitthiId)
                  } else {
                    GharSoundEffects.playPaperChitthiPin(context, soundEnabled)
                    container.chitthiRepository.stash(
                      ChitthiNote(
                        id = chitthiId,
                        author = ChitthiAuthor.PAPA,
                        title = if (plainEnglish) "Dad's Investment Note" else "Papa's Study Note",
                        body = msg.text,
                        tag = if (plainEnglish) "Investments" else "Gullak & Rates"
                      )
                    )
                  }
                },
                onRetryPrompt = { prompt ->
                  chatViewModel.dismissError()
                  chatViewModel.sendMessage(
                    userText = prompt,
                    snapshot = snapshot,
                    appendUserTurn = false
                  )
                },
                modifier = Modifier
                  .fillMaxWidth(0.86f)
                  .align(Alignment.Start)
              )

              Spacer(modifier = Modifier.height(8.dp))

              // "YOU" THOUGHT BUBBLE (Anchored Right above You on the sofa; fills live as you type!)
              PapaUserSpatialThoughtBubble(
                liveDraftText = customQuery,
                isInputFocused = isInputFocused,
                latestUserMessage = latestUserMessage,
                plainEnglish = plainEnglish,
                floatOffsetDp = -cloudFloatDp,
                onBubbleTap = {
                  runCatching { inputFocusRequester.requestFocus() }
                },
                modifier = Modifier
                  .fillMaxWidth(0.76f)
                  .align(Alignment.End)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFAFBF7).copy(alpha = 0.96f),
            border = BorderStroke(1.2.dp, Color(0xFF4B6E53).copy(alpha = 0.50f)),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = customQuery,
                onValueChange = { customQuery = it },
                placeholder = {
                  Text(
                    text = if (plainEnglish) {
                      "Ask Dad about SIPs, FDs, inflation, or big savings..."
                    } else {
                      "Type into your thought bubble for Papa..."
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF4E6252),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                },
                maxLines = 2,
                enabled = !isBusy,
                keyboardOptions = KeyboardOptions(
                  capitalization = KeyboardCapitalization.Sentences,
                  imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                  onSend = { submitCurrentPrompt() }
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                  color = Color(0xFF1C2B1F),
                  fontSize = 14.sp
                ),
                modifier = Modifier
                  .weight(1f)
                  .focusRequester(inputFocusRequester)
                  .onFocusChanged { isInputFocused = it.isFocused }
                  .testTag("papa_custom_query_input"),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = Color.Transparent,
                  unfocusedContainerColor = Color.Transparent,
                  disabledContainerColor = Color.Transparent,
                  focusedBorderColor = Color.Transparent,
                  unfocusedBorderColor = Color.Transparent,
                  disabledBorderColor = Color.Transparent,
                  cursorColor = Color(0xFF2F5E3D)
                )
              )
              Spacer(modifier = Modifier.width(6.dp))
              IconButton(
                onClick = { submitCurrentPrompt() },
                enabled = canSend,
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(
                    if (canSend) Color(0xFF2F5E3D) else Color(0xFFD5DED6)
                  )
                  .testTag("submit_papa_query")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Send,
                  contentDescription = if (plainEnglish) "Send question to Dad" else "Send question to Papa",
                  tint = if (canSend) Color(0xFFFFFBF4) else Color(0xFF6B7D6E),
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }
    }
  }

  if (showClearChatConfirm) {
    GharConfirmDialog(
      title = if (plainEnglish) "Reset Conversation?" else "Start Fresh with Papa?",
      message = "This clears the current study conversation (${messages.size} messages). Any notes saved to your collection remain untouched.",
      confirmLabel = "Clear Chat",
      onConfirm = {
        showClearChatConfirm = false
        val backupMessages = messages.toList()
        chatViewModel.clearChat()
        undoController?.recordAction(if (plainEnglish) "Cleared chat with Dad" else "Cleared chat with Papa") {
          container.appScope.launch {
            backupMessages.forEach { oldMsg ->
              container.papaChatRepository.appendMessage(oldMsg.sender, oldMsg.text, oldMsg.sources)
            }
          }
        }
      },
      onDismiss = { showClearChatConfirm = false },
      dialogTestTag = "confirm_clear_papa_chat_dialog",
      confirmTestTag = "confirm_clear_papa_chat_button"
    )
  }
}

@Composable
private fun SpatialPapaTopBar(
  plainEnglish: Boolean,
  engineMode: MaaAiEngineMode,
  miniStatus: MiniModelStatus,
  hasHistory: Boolean,
  isBusy: Boolean,
  onSelectCloud: () -> Unit,
  onSelectOffline: () -> Unit,
  onRequestClear: () -> Unit,
  onDismiss: () -> Unit
) {
  val isMiniInstalled = miniStatus is MiniModelStatus.Installed
  val isCloudActive = engineMode == MaaAiEngineMode.CLOUD_AUTO
  val isMiniActive = engineMode == MaaAiEngineMode.MINI_LOCAL

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Left side empty now (clutter reduction)
    Spacer(modifier = Modifier.width(1.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Surface(
        shape = CircleShape,
        color = if (isCloudActive) Color(0xFF2F5E3D) else Color(0xFFF9FBF6).copy(alpha = 0.84f),
        border = BorderStroke(
          1.dp,
          if (isCloudActive) Color(0xFF2F5E3D) else Color(0xFF4B6E53).copy(alpha = 0.34f)
        ),
        modifier = Modifier
          .defaultMinSize(minHeight = 36.dp)
          .clip(CircleShape)
          .clickable { onSelectCloud() }
          .testTag("papa_engine_cloud_chip")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.CloudQueue,
            contentDescription = "Live Web + Cloud AI",
            tint = if (isCloudActive) Color(0xFFFFFBF4) else Color(0xFF2B4631),
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Online",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isCloudActive) Color(0xFFFFFBF4) else Color(0xFF2B4631)
          )
        }
      }

      Surface(
        shape = CircleShape,
        color = if (isMiniActive) Color(0xFF536942) else Color(0xFFF9FBF6).copy(alpha = 0.84f),
        border = BorderStroke(
          1.dp,
          if (isMiniActive) Color(0xFF536942) else Color(0xFF4B6E53).copy(alpha = 0.34f)
        ),
        modifier = Modifier
          .defaultMinSize(minHeight = 36.dp)
          .clip(CircleShape)
          .clickable { onSelectOffline() }
          .testTag("papa_engine_offline_chip")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Memory,
            contentDescription = "Local 0.5B LLM",
            tint = if (isMiniActive) Color(0xFFFFFBF4) else Color(0xFF2B4631),
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "0.5B",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isMiniActive) Color(0xFFFFFBF4) else Color(0xFF2B4631)
          )
        }
      }

      if (hasHistory) {
        Surface(
          shape = CircleShape,
          color = Color(0xFFF9FBF6).copy(alpha = 0.86f),
          border = BorderStroke(1.dp, Color(0xFF4B6E53).copy(alpha = 0.34f)),
          modifier = Modifier.size(40.dp)
        ) {
          IconButton(
            onClick = onRequestClear,
            enabled = !isBusy,
            modifier = Modifier
              .fillMaxSize()
              .testTag("clear_papa_chat_button")
          ) {
            Icon(
              imageVector = Icons.Default.DeleteSweep,
              contentDescription = "Clear chat with Papa",
              tint = Color(0xFF2B4631),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Surface(
        shape = CircleShape,
        color = Color(0xFFF9FBF6).copy(alpha = 0.90f),
        border = BorderStroke(1.dp, Color(0xFF4B6E53).copy(alpha = 0.38f)),
        modifier = Modifier.size(40.dp)
      ) {
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .fillMaxSize()
            .testTag("close_papa_guidance")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close chat with Papa",
            tint = Color(0xFF1F3524),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun PapaSpatialSpeechBubble(
  uiState: PapaChatUiState,
  latestPapaMessage: PapaChatMessage?,
  cleanName: String,
  profile: BudgetProfile,
  goal: SavingsGoal,
  plainEnglish: Boolean,
  stashedIds: Set<String>,
  floatOffsetDp: Float,
  referralToMaaPrompt: String?,
  onHandoffToMaa: ((String) -> Unit)?,
  onOpenSourceUrl: (String) -> Unit,
  onToggleStash: (PapaChatMessage) -> Unit,
  onRetryPrompt: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val bubbleKey = when (uiState) {
    is PapaChatUiState.Loading -> "loading"
    is PapaChatUiState.Streaming -> "streaming"
    is PapaChatUiState.Error -> "error"
    PapaChatUiState.Idle -> latestPapaMessage?.id?.let { "msg_$it" } ?: "welcome"
  }

  Column(
    modifier = modifier.graphicsLayer {
      translationY = floatOffsetDp.dp.toPx()
    }
  ) {
    Surface(
      shape = RoundedCornerShape(
        topStart = 22.dp,
        topEnd = 22.dp,
        bottomEnd = 22.dp,
        bottomStart = 10.dp
      ),
      color = Color(0xFFFBFDF7).copy(alpha = 0.97f),
      shadowElevation = 6.dp,
      border = BorderStroke(1.3.dp, Color(0xFF4B6E53).copy(alpha = 0.54f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      AnimatedContent(
        targetState = bubbleKey,
        transitionSpec = {
          (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
            .togetherWith(fadeOut(tween(150)))
        },
        label = "papa_bubble_replacement"
      ) { stateKey ->
        when {
          stateKey == "loading" && uiState is PapaChatUiState.Loading -> {
            Row(
              modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 12.dp)
                .testTag("papa_thinking_indicator"),
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = Color(0xFF2F5E3D),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = uiState.statusHint,
                style = GharHandStyles.stickyNoteBody,
                color = StickyNotePaperColors.InkDark
              )
            }
          }

          stateKey == "streaming" && uiState is PapaChatUiState.Streaming -> {
            val streamScroll = rememberScrollState()
            LaunchedEffect(uiState.partialText.length) {
              streamScroll.scrollTo(streamScroll.maxValue)
            }
            Column(
              modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 11.dp)
                .testTag("papa_streaming_bubble")
            ) {
              Text(
                text = if (plainEnglish) "Dad:" else "Papa:",
                style = GharHandStyles.stickyNoteTitle,
                color = StickyNotePaperColors.InkAccentSage
              )
              Spacer(modifier = Modifier.height(2.dp))
              Column(
                modifier = Modifier
                  .heightIn(max = 136.dp)
                  .verticalScroll(streamScroll)
              ) {
                Text(
                  text = uiState.partialText,
                  style = GharHandStyles.stickyNoteBody,
                  color = StickyNotePaperColors.InkDark
                )
              }
              if (uiState.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                GroundingSourcesStrip(
                  sources = uiState.sources,
                  onOpenUrl = onOpenSourceUrl
                )
              }
            }
          }

          stateKey == "error" && uiState is PapaChatUiState.Error -> {
            Column(
              modifier = Modifier
                .padding(14.dp)
                .testTag("papa_error_card")
            ) {
              Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7D2512)
              )
              if (!uiState.lastPrompt.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                  onClick = { onRetryPrompt(uiState.lastPrompt) },
                  shape = CircleShape,
                  modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .testTag("retry_papa_query_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text("Retry", fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
              }
            }
          }

          latestPapaMessage != null -> {
            val chitthiId = "papa_chat_${latestPapaMessage.id}"
            val isStashed = chitthiId in stashedIds
            val replyScroll = rememberScrollState()
            Column(
              modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 11.dp)
                .testTag("papa_answer_bubble")
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (plainEnglish) "Dad" else "Papa",
                  style = GharHandStyles.stickyNoteTitle,
                  color = StickyNotePaperColors.InkAccentSage
                )
                Surface(
                  shape = CircleShape,
                  color = Color(0xFFE9F1E6),
                  border = BorderStroke(0.8.dp, Color(0xFF4B6E53).copy(alpha = 0.36f)),
                  modifier = Modifier
                    .defaultMinSize(minHeight = 32.dp)
                    .clip(CircleShape)
                    .clickable { onToggleStash(latestPapaMessage) }
                    .testTag("stash_papa_chat_${latestPapaMessage.id}")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = if (isStashed) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                      contentDescription = if (isStashed) "Saved note" else "Save note",
                      tint = StickyNotePaperColors.InkAccentSage,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (isStashed) "Saved ✓" else if (plainEnglish) "Save" else "Chitthi",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = StickyNotePaperColors.InkDark
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(3.dp))
              Column(
                modifier = Modifier
                  .heightIn(max = 138.dp)
                  .verticalScroll(replyScroll)
              ) {
                Text(
                  text = latestPapaMessage.text,
                  style = GharHandStyles.stickyNoteBody,
                  color = StickyNotePaperColors.InkDark
                )
              }

              if (latestPapaMessage.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                GroundingSourcesStrip(
                  sources = latestPapaMessage.sources,
                  onOpenUrl = onOpenSourceUrl
                )
              }

              if (!referralToMaaPrompt.isNullOrBlank() && onHandoffToMaa != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                  shape = CircleShape,
                  color = Color(0xFFB84A27),
                  shadowElevation = 2.dp,
                  modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(CircleShape)
                    .clickable { onHandoffToMaa(referralToMaaPrompt) }
                    .testTag("handoff_to_maa_button")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = if (plainEnglish) "Ask Mom instead" else "Ask Maa instead",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFFFFBF4)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                      contentDescription = null,
                      tint = Color(0xFFFFFBF4),
                      modifier = Modifier.size(15.dp)
                    )
                  }
                }
              }
            }
          }

          else -> {
            Column(
              modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 11.dp)
                .testTag("papa_opening_note")
            ) {
              Text(
                text = if (plainEnglish) "Dad" else "Papa",
                style = GharHandStyles.stickyNoteTitle,
                color = StickyNotePaperColors.InkAccentSage
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = if (plainEnglish) {
                  "Pull up a chair, $cleanName. '${goal.name}' holds ${CurrencyFormatter.formatRupees(goal.currentAmountRupees)} of ${CurrencyFormatter.formatRupees(goal.targetAmountRupees)}, with ${CurrencyFormatter.formatRupees(profile.savingsTargetRupees)}/month earmarked for savings. Ask me about SIPs, FDs, inflation, or where to park a big sum."
                } else {
                  "Aao baitho, $cleanName. '${goal.name}' holds ${CurrencyFormatter.formatRupees(goal.currentAmountRupees)} of ${CurrencyFormatter.formatRupees(goal.targetAmountRupees)}, and you target ${CurrencyFormatter.formatRupees(profile.savingsTargetRupees)}/month in savings. Ask me about live FD/PPF rates, index SIPs, inflation, or investing a big amount."
                },
                style = GharHandStyles.stickyNoteBody,
                color = StickyNotePaperColors.InkDark
              )
            }
          }
        }
      }
    }

    // Speech Tail pointing down-left toward Papa sitting in his cane study armchair
    Canvas(
      modifier = Modifier
        .padding(start = 34.dp)
        .size(width = 22.dp, height = 13.dp)
    ) {
      val fillPath = Path().apply {
        moveTo(3.dp.toPx(), 0f)
        lineTo(19.dp.toPx(), 0f)
        quadraticTo(11.dp.toPx(), 9.dp.toPx(), 0f, size.height)
        quadraticTo(4.dp.toPx(), 5.dp.toPx(), 3.dp.toPx(), 0f)
        close()
      }
      drawPath(path = fillPath, color = Color(0xFFFBFDF7))
      drawPath(
        path = fillPath,
        color = Color(0xFF4B6E53).copy(alpha = 0.54f),
        style = Stroke(width = 1.2.dp.toPx())
      )
    }
  }
}

@Composable
private fun GroundingSourcesStrip(
  sources: List<GroundingSourceChip>,
  onOpenUrl: (String) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    sources.take(3).forEachIndexed { index, chip ->
      Surface(
        shape = CircleShape,
        color = Color(0xFFE7EFE4),
        border = BorderStroke(0.8.dp, Color(0xFF4B6E53).copy(alpha = 0.42f)),
        modifier = Modifier
          .defaultMinSize(minHeight = 28.dp)
          .clip(CircleShape)
          .clickable { onOpenUrl(chip.uri) }
          .testTag("papa_grounding_source_$index")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Public,
            contentDescription = null,
            tint = Color(0xFF2F5E3D),
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = chip.domain.ifBlank { chip.title }.take(24),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F3524),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.width(3.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open source ${chip.title}",
            tint = Color(0xFF2F5E3D),
            modifier = Modifier.size(11.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun PapaUserSpatialThoughtBubble(
  liveDraftText: String,
  isInputFocused: Boolean,
  latestUserMessage: PapaChatMessage?,
  plainEnglish: Boolean,
  floatOffsetDp: Float,
  onBubbleTap: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isActivelyThinking = liveDraftText.isNotBlank() || isInputFocused
  val bubbleScale by animateFloatAsState(
    targetValue = if (isActivelyThinking) 1.02f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "papa_user_thought_scale"
  )

  val displayModeKey = when {
    liveDraftText.isNotBlank() -> "live_typing"
    isInputFocused -> "focused_empty"
    latestUserMessage != null -> "sent_${latestUserMessage.id}"
    else -> "idle_invite"
  }

  Column(
    horizontalAlignment = Alignment.End,
    modifier = modifier.graphicsLayer {
      scaleX = bubbleScale
      scaleY = bubbleScale
      translationY = floatOffsetDp.dp.toPx()
    }
  ) {
    Surface(
      shape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 24.dp,
        bottomEnd = 16.dp
      ),
      color = Color(0xFFFFFCF3).copy(alpha = if (isActivelyThinking) 0.98f else 0.94f),
      shadowElevation = if (isActivelyThinking) 7.dp else 4.dp,
      border = BorderStroke(
        width = if (isActivelyThinking) 1.5.dp else 1.1.dp,
        color = if (isActivelyThinking) {
          Color(0xFF2F5E3D).copy(alpha = 0.78f)
        } else {
          Color(0xFF6B846E).copy(alpha = 0.48f)
        }
      ),
      modifier = Modifier
        .fillMaxWidth()
        .clip(
          RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 24.dp,
            bottomEnd = 16.dp
          )
        )
        .clickable { onBubbleTap() }
        .testTag("papa_user_thought_bubble")
    ) {
      AnimatedContent(
        targetState = displayModeKey,
        transitionSpec = {
          fadeIn(tween(180)).togetherWith(fadeOut(tween(120)))
        },
        label = "papa_thought_bubble_content"
      ) { mode ->
        Column(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = when (mode) {
                "live_typing", "focused_empty" -> if (plainEnglish) "You (thinking...)" else "You • thinking..."
                else -> "You"
              },
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (isActivelyThinking) Color(0xFF2F5E3D) else Color(0xFF4F6653)
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          when (mode) {
            "live_typing" -> {
              Text(
                text = liveDraftText,
                style = GharHandStyles.stickyNoteBody,
                color = Color(0xFF1C2B1F),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
              )
            }
            "focused_empty" -> {
              Text(
                text = if (plainEnglish) {
                  "Thinking of an investment or savings question..."
                } else {
                  "Soch rahe ho... ask Papa about SIPs, FDs, or big savings"
                },
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = Color(0xFF5D7261)
              )
            }
            else -> {
              if (latestUserMessage != null) {
                Text(
                  text = latestUserMessage.text,
                  style = GharHandStyles.stickyNoteBody,
                  color = Color(0xFF1C2B1F),
                  maxLines = 3,
                  overflow = TextOverflow.Ellipsis
                )
              } else {
                Text(
                  text = if (plainEnglish) {
                    "Tap a topic below or type to ask Dad about rates & investments..."
                  } else {
                    "Tap a chip below or type to fill your thought cloud..."
                  },
                  style = MaterialTheme.typography.bodySmall,
                  fontStyle = FontStyle.Italic,
                  color = Color(0xFF5D7261)
                )
              }
            }
          }
        }
      }
    }

    // Classic 3-circle Thought Cloud Trail descending toward "You" sitting on the right sofa
    Canvas(
      modifier = Modifier
        .padding(end = 28.dp)
        .size(width = 30.dp, height = 22.dp)
    ) {
      val fill = Color(0xFFFFFCF3)
      val strokeColor = if (isActivelyThinking) {
        Color(0xFF2F5E3D).copy(alpha = 0.72f)
      } else {
        Color(0xFF6B846E).copy(alpha = 0.50f)
      }
      val strokeW = 1.2.dp.toPx()

      val c1 = Offset(x = 9.dp.toPx(), y = 5.dp.toPx())
      drawCircle(color = fill, radius = 4.6.dp.toPx(), center = c1)
      drawCircle(color = strokeColor, radius = 4.6.dp.toPx(), center = c1, style = Stroke(width = strokeW))

      val c2 = Offset(x = 18.dp.toPx(), y = 13.dp.toPx())
      drawCircle(color = fill, radius = 3.1.dp.toPx(), center = c2)
      drawCircle(color = strokeColor, radius = 3.1.dp.toPx(), center = c2, style = Stroke(width = strokeW))

      val c3 = Offset(x = 24.dp.toPx(), y = 19.dp.toPx())
      drawCircle(color = fill, radius = 1.9.dp.toPx(), center = c3)
      drawCircle(color = strokeColor, radius = 1.9.dp.toPx(), center = c3, style = Stroke(width = strokeW))
    }
  }
}
