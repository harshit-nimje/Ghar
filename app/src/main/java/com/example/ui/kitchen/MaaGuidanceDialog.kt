package com.example.ui.kitchen

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
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
import com.example.core.ai.MaaMiniModelManager
import com.example.core.ai.MiniModelStatus
import com.example.core.audio.GharSoundEffects
import com.example.core.engine.SafeToSpendResult
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.model.BudgetProfile
import com.example.core.model.ChatSender
import com.example.core.model.ChitthiAuthor
import com.example.core.model.ChitthiNote
import com.example.core.model.MaaAiEngineMode
import com.example.core.model.MaaChatMessage
import com.example.core.model.PakkaCommitment
import com.example.core.model.SavingsGoal
import com.example.core.model.Transaction
import com.example.core.util.CurrencyFormatter
import com.example.ui.common.GharConfirmDialog
import com.example.ui.common.JharokhaCardShape
import com.example.ui.common.LocalGharUndoController
import com.example.ui.common.StickyNotePaperColors
import com.example.ui.common.rememberCachedGharPainter
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.contrastingContentColor
import kotlinx.coroutines.launch
import kotlin.random.Random

data class GuidanceTopic(
  val id: String,
  val label: String,
  val question: String
)

private val MAA_ROOM_SITTING_SCENES = intArrayOf(
  R.drawable.img_maa_room_lean_1793191524432,
  R.drawable.img_maa_room_cozy_1793191528081,
  R.drawable.img_maa_room_cross_1793191529210
)

@Composable
fun MaaGuidanceDialog(
  container: AppContainer,
  userName: String,
  safeToSpend: SafeToSpendResult,
  profile: BudgetProfile,
  goal: SavingsGoal,
  transactions: List<Transaction>,
  commitments: List<PakkaCommitment> = emptyList(),
  allGoals: List<SavingsGoal> = emptyList(),
  initialPrompt: String? = null,
  onSwitchToPapa: ((String?) -> Unit)? = null,
  onDismiss: () -> Unit
) {
  val chatViewModel: MaaChatViewModel = viewModel(
    key = "maa_chat_${container.userId}",
    factory = viewModelFactory {
      initializer {
        MaaChatViewModel(
          chatRepository = container.maaChatRepository,
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
  val referralToPapaPrompt by chatViewModel.referralToPapaPrompt.collectAsStateWithLifecycle()
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
  var showMiniModelPanel by remember { mutableStateOf(false) }
  var showClearChatConfirm by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    GharSoundEffects.playChaiCupSettle(context, soundEnabled)
  }

  // Randomized spatial sitting posture on the right sofa each time you sit with Maa
  var sittingPoseIndex by remember {
    mutableIntStateOf(Random.nextInt(MAA_ROOM_SITTING_SCENES.size))
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
        GuidanceTopic("eat_out", "Order dinner?", "Can I afford to order dinner tonight?"),
        GuidanceTopic("split_math", "Split ₹840 bill", "Split an ₹840 dinner bill among 3 people and check my daily wallet"),
        GuidanceTopic("compare_coffee", "Home vs Café?", "Should I buy ₹260 iced coffee outside or ₹20 coffee at home?"),
        GuidanceTopic("chai_leaks", "Coffee & snacks", "How much have I spent on coffee and snacks this week?"),
        GuidanceTopic("maa_wisdom", "Daily motivation", "Share a practical daily money tip or warm motivation"),
        GuidanceTopic("comfort", "Budget check", "I feel a little anxious about my daily budget today.")
      )
    } else {
      listOf(
        GuidanceTopic("eat_out", "Order dinner?", "Maa, can I order dinner outside tonight?"),
        GuidanceTopic("split_math", "Split ₹840 bill", "Maa, split ₹840 dinner among 3 friends and check my Batua today"),
        GuidanceTopic("compare_coffee", "Home vs Café?", "Should I buy ₹260 iced coffee outside or ₹20 coffee at home?"),
        GuidanceTopic("chai_leaks", "Chai habit", "How is my chai & snacking habit doing this week?"),
        GuidanceTopic("gullak_progress", "Gullak check", "Is Papa's clay Gullak filling up okay?"),
        GuidanceTopic("comfort", "Motivation", "Maa, give me a little motivation about money today.")
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

  val isBusy = uiState is MaaChatUiState.Loading || uiState is MaaChatUiState.Streaming
  val isMiniInstalled = miniStatus is MiniModelStatus.Installed
  val isMiniInstalling = uiState !is MaaChatUiState.Streaming && miniStatus is MiniModelStatus.Installing

  val latestUserMessage = remember(messages) {
    messages.lastOrNull { it.sender == ChatSender.USER }
  }
  val latestMaaMessage = remember(messages) {
    messages.lastOrNull { it.sender == ChatSender.MAA }
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

  // Subtle ambient breathing float for the thought & speech clouds
  val ambientTransition = rememberInfiniteTransition(label = "maa_room_ambient")
  val cloudFloatDp by ambientTransition.animateFloat(
    initialValue = -1.8f,
    targetValue = 1.8f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2800),
      repeatMode = RepeatMode.Reverse
    ),
    label = "cloud_float"
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
      color = Color(0xFFFDF5E6),
      tonalElevation = 10.dp,
      shadowElevation = 16.dp,
      border = BorderStroke(1.2.dp, Color(0xFFC89B74).copy(alpha = 0.55f)),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.90f)
        .widthIn(max = 560.dp)
        .imePadding()
        .padding(vertical = 8.dp)
        .testTag("maa_guidance_dialog")
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFFFDF5E6))
      ) {
        // 1. Full-bleed Spatial Hand-Inked Watercolor Baithak Room Scene
        //    Maa sitting cross-legged on the floor rug holding her Khata jotpad on the left;
        //    "You" sitting curiously on the angled teakwood sofa on the right (randomized posture).
        Crossfade(
          targetState = sittingPoseIndex,
          animationSpec = tween(durationMillis = 420),
          label = "sitting_pose_crossfade",
          modifier = Modifier.fillMaxSize()
        ) { poseIdx ->
          val roomPainter = rememberCachedGharPainter(
            MAA_ROOM_SITTING_SCENES[poseIdx % MAA_ROOM_SITTING_SCENES.size]
          )
          Image(
            painter = roomPainter,
            contentDescription = "Cozy room where Maa sits cross-legged with her khata notebook on the left and you sit curiously on the sofa on the right",
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(horizontalBias = 0f, verticalBias = 0.82f),
            modifier = Modifier.fillMaxSize()
          )
        }

        // Soft warm limewash ceiling blend at top + warm floor dusk blend behind bottom typing bar
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .align(Alignment.TopCenter)
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color(0xFFFDF5E6).copy(alpha = 0.88f),
                  Color(0xFFFDF5E6).copy(alpha = 0.35f),
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
                  Color(0xFF2B1C12).copy(alpha = 0.38f),
                  Color(0xFF23160E).copy(alpha = 0.72f)
                )
              )
            )
        )

        // 2. Spatial Room Overlay Layout
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
          // Compact Translucent Room Header + Engine Pill
          SpatialMaaTopBar(
            plainEnglish = plainEnglish,
            engineMode = engineMode,
            miniStatus = miniStatus,
            isPanelExpanded = showMiniModelPanel,
            hasHistory = messages.isNotEmpty(),
            isBusy = isBusy,
            onSelectCloud = {
              chatViewModel.selectEngineMode(MaaAiEngineMode.CLOUD_AUTO)
              showMiniModelPanel = false
            },
            onClickMini = {
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

          // 3. Spatial Conversation Stage:
          //    - Left: Maa's warm handwritten speech bubble (replaces old reply on new message)
          //    - Right: "You" live-filling thought bubble over the sofa (replaces old question)
          //    - Lower Room Window: Unobstructed spatial view of Maa & You sitting across the room
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .testTag("maa_chat_message_list")
          ) {
            // Subtle tap zone over the right sofa allowing playful posture shift if tapped
            Box(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth(0.45f)
                .fillMaxHeight(0.42f)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null
                ) {
                  sittingPoseIndex = (sittingPoseIndex + 1) % MAA_ROOM_SITTING_SCENES.size
                }
            )

            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.Top
            ) {
              // MAA'S SPEECH BUBBLE (Anchored Left above Maa & her Khata jotpad)
              MaaSpatialSpeechBubble(
                uiState = uiState,
                latestMaaMessage = latestMaaMessage,
                cleanName = cleanName,
                safeToSpend = safeToSpend,
                plainEnglish = plainEnglish,
                stashedIds = stashedIds,
                isMiniInstalled = isMiniInstalled,
                isMiniInstalling = isMiniInstalling,
                floatOffsetDp = cloudFloatDp * 0.6f,
                referralToPapaPrompt = referralToPapaPrompt,
                onHandoffToPapa = if (onSwitchToPapa != null) {
                  { promptToPass ->
                    chatViewModel.dismissPapaReferral()
                    onSwitchToPapa(promptToPass)
                  }
                } else null,
                onToggleStash = { msg ->
                  val chitthiId = "chat_${msg.id}"
                  if (chitthiId in stashedIds) {
                    container.chitthiRepository.remove(chitthiId)
                  } else {
                    GharSoundEffects.playPaperChitthiPin(context, soundEnabled)
                    container.chitthiRepository.stash(
                      ChitthiNote(
                        id = chitthiId,
                        author = ChitthiAuthor.MAA,
                        title = if (plainEnglish) "Mom's Advice" else "Maa's Khata Note",
                        body = msg.text,
                        tag = if (plainEnglish) "Chat" else "Baithak Chat"
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
                onInstallOfflineFromError = { lastPrompt ->
                  chatViewModel.installAndActivateMiniModel(
                    retryPromptAfterInstall = lastPrompt,
                    snapshotForRetry = snapshot
                  )
                },
                modifier = Modifier
                  .fillMaxWidth(0.84f)
                  .align(Alignment.Start)
              )

              Spacer(modifier = Modifier.height(8.dp))

              // "YOU" THOUGHT BUBBLE (Anchored Right above You on the sofa; fills live as you type!)
              UserSpatialThoughtBubble(
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

          // Frosted Warm Paper Input Bar — typing fills up the Thought Bubble above "You"
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFFFCF6).copy(alpha = 0.95f),
            border = BorderStroke(1.2.dp, Color(0xFFA6724E).copy(alpha = 0.50f)),
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
                      "Type your question for Mom..."
                    } else {
                      "Type into your thought bubble for Maa..."
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF7B5D47),
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
                  color = Color(0xFF2D1B10),
                  fontSize = 14.sp
                ),
                modifier = Modifier
                  .weight(1f)
                  .focusRequester(inputFocusRequester)
                  .onFocusChanged { isInputFocused = it.isFocused }
                  .testTag("maa_custom_query_input"),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = Color.Transparent,
                  unfocusedContainerColor = Color.Transparent,
                  disabledContainerColor = Color.Transparent,
                  focusedBorderColor = Color.Transparent,
                  unfocusedBorderColor = Color.Transparent,
                  disabledBorderColor = Color.Transparent,
                  cursorColor = Color(0xFFA93818)
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
                    if (canSend) Color(0xFFB84A27) else Color(0xFFE5D7C6)
                  )
                  .testTag("submit_maa_query")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Send,
                  contentDescription = "Send message to Maa",
                  tint = if (canSend) Color(0xFFFFFBF4) else Color(0xFF8C7562),
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
      title = if (plainEnglish) "Reset Conversation?" else "Start Fresh with Maa?",
      message = "This clears the current conversation bubbles (${messages.size} messages). Any notes saved to your collection remain untouched.",
      confirmLabel = "Clear Chat",
      onConfirm = {
        showClearChatConfirm = false
        val backupMessages = messages.toList()
        chatViewModel.clearChat()
        undoController?.recordAction(if (plainEnglish) "Cleared chat history" else "Cleared chat with Maa") {
          container.appScope.launch {
            backupMessages.forEach { oldMsg ->
              container.maaChatRepository.appendMessage(oldMsg.sender, oldMsg.text)
            }
          }
        }
      },
      onDismiss = { showClearChatConfirm = false },
      dialogTestTag = "confirm_clear_maa_chat_dialog",
      confirmTestTag = "confirm_clear_maa_chat_button"
    )
  }
}

@Composable
private fun SpatialMaaTopBar(
  plainEnglish: Boolean,
  engineMode: MaaAiEngineMode,
  miniStatus: MiniModelStatus,
  isPanelExpanded: Boolean,
  hasHistory: Boolean,
  isBusy: Boolean,
  onSelectCloud: () -> Unit,
  onClickMini: () -> Unit,
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

    // Compact Cloud / Offline 0.5B Mode Pills + Clear + Close
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Surface(
        shape = CircleShape,
        color = if (isCloudActive) Color(0xFFC5532D) else Color(0xFFFFFCF5).copy(alpha = 0.82f),
        border = BorderStroke(
          1.dp,
          if (isCloudActive) Color(0xFFC5532D) else Color(0xFF9C6B49).copy(alpha = 0.32f)
        ),
        modifier = Modifier
          .defaultMinSize(minHeight = 36.dp)
          .clip(CircleShape)
          .clickable { onSelectCloud() }
          .testTag("maa_engine_cloud_chip")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.CloudQueue,
            contentDescription = "Cloud AI mode",
            tint = if (isCloudActive) Color(0xFFFFFBF4) else Color(0xFF5D402C),
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Online",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isCloudActive) Color(0xFFFFFBF4) else Color(0xFF5D402C)
          )
        }
      }

      Surface(
        shape = CircleShape,
        color = when {
          isMiniActive -> Color(0xFF3E6B48)
          isPanelExpanded -> Color(0xFFF3DB9B)
          else -> Color(0xFFFFFCF5).copy(alpha = 0.82f)
        },
        border = BorderStroke(
          1.dp,
          if (isMiniActive) Color(0xFF3E6B48) else Color(0xFF9C6B49).copy(alpha = 0.32f)
        ),
        modifier = Modifier
          .defaultMinSize(minHeight = 36.dp)
          .clip(CircleShape)
          .clickable { onClickMini() }
          .testTag("maa_engine_mini_chip")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Memory,
            contentDescription = "Offline 0.5B model",
            tint = if (isMiniActive) Color(0xFFFFFBF4) else Color(0xFF5D402C),
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "0.5B",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isMiniActive) Color(0xFFFFFBF4) else Color(0xFF5D402C)
          )
        }
      }

      if (hasHistory) {
        Surface(
          shape = CircleShape,
          color = Color(0xFFFFFCF5).copy(alpha = 0.85f),
          border = BorderStroke(1.dp, Color(0xFF9C6B49).copy(alpha = 0.32f)),
          modifier = Modifier.size(40.dp)
        ) {
          IconButton(
            onClick = onRequestClear,
            enabled = !isBusy,
            modifier = Modifier
              .fillMaxSize()
              .testTag("clear_maa_chat_button")
          ) {
            Icon(
              imageVector = Icons.Default.DeleteSweep,
              contentDescription = "Reset conversation",
              tint = Color(0xFF5D402C),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Surface(
        shape = CircleShape,
        color = Color(0xFFFFFCF5).copy(alpha = 0.88f),
        border = BorderStroke(1.dp, Color(0xFF9C6B49).copy(alpha = 0.35f)),
        modifier = Modifier.size(40.dp)
      ) {
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .fillMaxSize()
            .testTag("close_maa_guidance")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close chat with Maa",
            tint = Color(0xFF362115),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

/**
 * Maa's single warm speech bubble hovering above her on the left side of the room.
 * Every new reply replaces the previous reply cleanly inside this single bubble.
 */
@Composable
private fun MaaSpatialSpeechBubble(
  uiState: MaaChatUiState,
  latestMaaMessage: MaaChatMessage?,
  cleanName: String,
  safeToSpend: SafeToSpendResult,
  plainEnglish: Boolean,
  stashedIds: Set<String>,
  isMiniInstalled: Boolean,
  isMiniInstalling: Boolean,
  floatOffsetDp: Float,
  referralToPapaPrompt: String?,
  onHandoffToPapa: ((String) -> Unit)?,
  onToggleStash: (MaaChatMessage) -> Unit,
  onRetryPrompt: (String) -> Unit,
  onInstallOfflineFromError: (String?) -> Unit,
  modifier: Modifier = Modifier
) {
  val bubbleKey = when (uiState) {
    is MaaChatUiState.Loading -> "loading"
    is MaaChatUiState.Streaming -> "streaming"
    is MaaChatUiState.Error -> "error"
    MaaChatUiState.Idle -> latestMaaMessage?.id?.let { "msg_$it" } ?: "welcome"
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
      color = Color(0xFFFFFDF5).copy(alpha = 0.96f),
      shadowElevation = 6.dp,
      border = BorderStroke(1.3.dp, Color(0xFF8B5A3C).copy(alpha = 0.52f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      AnimatedContent(
        targetState = bubbleKey,
        transitionSpec = {
          (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
            .togetherWith(fadeOut(tween(150)))
        },
        label = "maa_bubble_replacement"
      ) { stateKey ->
        when {
          stateKey == "loading" && uiState is MaaChatUiState.Loading -> {
            Row(
              modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 12.dp)
                .testTag("maa_thinking_indicator"),
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = Color(0xFFB84A27),
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

          stateKey == "streaming" && uiState is MaaChatUiState.Streaming -> {
            val streamScroll = rememberScrollState()
            LaunchedEffect(uiState.partialText.length) {
              streamScroll.scrollTo(streamScroll.maxValue)
            }
            Column(
              modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 11.dp)
                .testTag("maa_streaming_bubble")
            ) {
              Text(
                text = if (plainEnglish) "Mom:" else "Maa:",
                style = GharHandStyles.stickyNoteTitle,
                color = StickyNotePaperColors.InkAccentSage
              )
              Spacer(modifier = Modifier.height(2.dp))
              Column(
                modifier = Modifier
                  .heightIn(max = 132.dp)
                  .verticalScroll(streamScroll)
              ) {
                Text(
                  text = uiState.partialText,
                  style = GharHandStyles.stickyNoteBody,
                  color = StickyNotePaperColors.InkDark
                )
              }
            }
          }

          stateKey == "error" && uiState is MaaChatUiState.Error -> {
            Column(
              modifier = Modifier
                .padding(14.dp)
                .testTag("maa_error_card")
            ) {
              Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7D2512)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (!uiState.lastPrompt.isNullOrBlank()) {
                  OutlinedButton(
                    onClick = { onRetryPrompt(uiState.lastPrompt) },
                    shape = CircleShape,
                    modifier = Modifier
                      .defaultMinSize(minHeight = 40.dp)
                      .testTag("retry_maa_query_button")
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
                if (uiState.suggestMiniInstall && !isMiniInstalled) {
                  val sageContent = PastelSage.contrastingContentColor()
                  Button(
                    onClick = { onInstallOfflineFromError(uiState.lastPrompt) },
                    enabled = !isMiniInstalling,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                      containerColor = PastelSage,
                      contentColor = sageContent
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .defaultMinSize(minHeight = 40.dp)
                      .testTag("error_install_mini_model_button")
                  ) {
                    Text(
                      text = "Use Offline 0.5B",
                      color = sageContent,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }
          }

          latestMaaMessage != null -> {
            val chitthiId = "chat_${latestMaaMessage.id}"
            val isStashed = chitthiId in stashedIds
            val replyScroll = rememberScrollState()
            Column(
              modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 11.dp)
                .testTag("maa_answer_bubble")
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (plainEnglish) "Mom" else "Maa",
                  style = GharHandStyles.stickyNoteTitle,
                  color = StickyNotePaperColors.InkAccentRed
                )
                Surface(
                  shape = CircleShape,
                  color = Color(0xFFF6ECD8),
                  border = BorderStroke(0.8.dp, Color(0xFF9C6B49).copy(alpha = 0.35f)),
                  modifier = Modifier
                    .defaultMinSize(minHeight = 32.dp)
                    .clip(CircleShape)
                    .clickable { onToggleStash(latestMaaMessage) }
                    .testTag("stash_maa_chat_${latestMaaMessage.id}")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = if (isStashed) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                      contentDescription = if (isStashed) "Saved note" else "Save note",
                      tint = if (isStashed) StickyNotePaperColors.InkAccentSage else StickyNotePaperColors.InkAccentRed,
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
                  .heightIn(max = 136.dp)
                  .verticalScroll(replyScroll)
              ) {
                Text(
                  text = latestMaaMessage.text,
                  style = GharHandStyles.stickyNoteBody,
                  color = StickyNotePaperColors.InkDark
                )
              }
              if (!referralToPapaPrompt.isNullOrBlank() && onHandoffToPapa != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                  shape = CircleShape,
                  color = Color(0xFF3E6B48),
                  shadowElevation = 2.dp,
                  modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(CircleShape)
                    .clickable { onHandoffToPapa(referralToPapaPrompt) }
                    .testTag("handoff_to_papa_button")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = if (plainEnglish) "Ask Dad instead" else "Ask Papa instead",
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
                .testTag("maa_opening_note")
            ) {
              Text(
                text = if (plainEnglish) "Mom" else "Maa",
                style = GharHandStyles.stickyNoteTitle,
                color = StickyNotePaperColors.InkAccentRed
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = if (plainEnglish) {
                  "Sit comfortably, $cleanName. Safe to spend today is ${CurrencyFormatter.formatRupees(safeToSpend.dailyAllowanceRupees)} (${safeToSpend.daysRemaining} days left). What's on your mind?"
                } else {
                  "Come sit, $cleanName. Safe to spend today is ${CurrencyFormatter.formatRupees(safeToSpend.dailyAllowanceRupees)} (${safeToSpend.daysRemaining} days left in this khata cycle). Tell me what's on your mind."
                },
                style = GharHandStyles.stickyNoteBody,
                color = StickyNotePaperColors.InkDark
              )
            }
          }
        }
      }
    }

    // Warm Speech Tail pointing down-left toward Maa sitting cross-legged on the floor mat
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
      drawPath(path = fillPath, color = Color(0xFFFFFDF5))
      drawPath(
        path = fillPath,
        color = Color(0xFF8B5A3C).copy(alpha = 0.52f),
        style = Stroke(width = 1.2.dp.toPx())
      )
    }
  }
}

/**
 * Thought Bubble hovering above "You" on the right sofa.
 * Pops open and fills up live as you type; once sent, displays your current question
 * until replaced by your next thought/message.
 */
@Composable
private fun UserSpatialThoughtBubble(
  liveDraftText: String,
  isInputFocused: Boolean,
  latestUserMessage: MaaChatMessage?,
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
    label = "user_thought_scale"
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
        bottomEnd = 18.dp
      ),
      color = if (isActivelyThinking) {
        Color(0xFFFFFAF0).copy(alpha = 0.98f)
      } else {
        Color(0xFFFFF8EC).copy(alpha = 0.92f)
      },
      shadowElevation = if (isActivelyThinking) 8.dp else 4.dp,
      border = BorderStroke(
        width = if (isActivelyThinking) 1.5.dp else 1.1.dp,
        color = if (isActivelyThinking) {
          Color(0xFFB84A27).copy(alpha = 0.72f)
        } else {
          Color(0xFF9C6B49).copy(alpha = 0.45f)
        }
      ),
      modifier = Modifier
        .let { base ->
          if (displayModeKey == "idle_invite") base else base.fillMaxWidth()
        }
        .clip(
          RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 24.dp,
            bottomEnd = 18.dp
          )
        )
        .clickable { onBubbleTap() }
        .testTag(
          if (!isActivelyThinking && latestUserMessage != null) {
            "user_chat_bubble_${latestUserMessage.id}"
          } else {
            "user_thought_bubble"
          }
        )
    ) {
      if (liveDraftText.isNotBlank()) {
        // Live typing fills up the thought bubble smoothly without re-mounting per character
        val draftScroll = rememberScrollState()
        LaunchedEffect(liveDraftText.length) {
          draftScroll.scrollTo(draftScroll.maxValue)
        }
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
          Text(
            text = "You • thinking...",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB84A27)
          )
          Spacer(modifier = Modifier.height(2.dp))
          Column(
            modifier = Modifier
              .heightIn(max = 88.dp)
              .verticalScroll(draftScroll)
          ) {
            Text(
              text = "$liveDraftText▍",
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFF2B1A10)
            )
          }
        }
      } else {
        AnimatedContent(
          targetState = displayModeKey,
          transitionSpec = {
            fadeIn(tween(180)).togetherWith(fadeOut(tween(130)))
          },
          label = "thought_content_switch"
        ) { mode ->
          when {
            mode == "focused_empty" -> {
              Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                  text = "You • thinking...",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFB84A27)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = if (plainEnglish) "Hmm, let me ask Mom...▍" else "Hmm, let me ask Maa...▍",
                  style = MaterialTheme.typography.bodySmall,
                  fontStyle = FontStyle.Italic,
                  color = Color(0xFF7D5D46)
                )
              }
            }

            latestUserMessage != null -> {
              val sentScroll = rememberScrollState()
              Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                  text = "You",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF9C4223)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Column(
                  modifier = Modifier
                    .heightIn(max = 84.dp)
                    .verticalScroll(sentScroll)
                ) {
                  Text(
                    text = latestUserMessage.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2B1A10)
                  )
                }
              }
            }

            else -> {
              Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (plainEnglish) "💭 Tap to ask Mom..." else "💭 Tap to ask Maa...",
                  style = MaterialTheme.typography.labelMedium,
                  fontStyle = FontStyle.Italic,
                  color = Color(0xFF6B4C37)
                )
              }
            }
          }
        }
      }
    }

    // 3 Trailing Thought-Cloud Circles pointing down-right toward "You" sitting on the sofa
    Canvas(
      modifier = Modifier
        .padding(end = 26.dp, top = 2.dp)
        .size(width = 30.dp, height = 22.dp)
    ) {
      val fill = Color(0xFFFFFAF0)
      val strokeColor = if (isActivelyThinking) {
        Color(0xFFB84A27).copy(alpha = 0.72f)
      } else {
        Color(0xFF9C6B49).copy(alpha = 0.50f)
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

@Composable
internal fun MaaMiniInstallerInlineCard(
  miniStatus: MiniModelStatus,
  isActiveMode: Boolean,
  onInstallAndActivate: () -> Unit,
  onCancelInstall: (() -> Unit)? = null,
  onActivateInstalled: () -> Unit,
  onUninstall: () -> Unit
) {
  var showUninstallConfirm by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = PastelSage.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.38f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("maa_mini_installer_card")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Memory,
          contentDescription = null,
          tint = PastelSage,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.maa_mini_card_title),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      when (miniStatus) {
        is MiniModelStatus.NotInstalled, is MiniModelStatus.InstallError -> {
          Text(
            text = stringResource(R.string.maa_mini_card_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (miniStatus is MiniModelStatus.InstallError) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = miniStatus.reason,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          val sageContent = PastelSage.contrastingContentColor()
          Button(
            onClick = onInstallAndActivate,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
              containerColor = PastelSage,
              contentColor = sageContent
            ),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .testTag("install_maa_mini_button")
          ) {
            Icon(
              imageVector = Icons.Default.Memory,
              contentDescription = null,
              tint = sageContent,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.maa_mini_install_action),
              color = sageContent,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        is MiniModelStatus.Installing -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = miniStatus.stageLabel,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f)
            )
            if (onCancelInstall != null) {
              TextButton(
                onClick = onCancelInstall,
                modifier = Modifier
                  .defaultMinSize(minHeight = 36.dp)
                  .testTag("cancel_maa_mini_install_button")
              ) {
                Text("Cancel", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          LinearProgressIndicator(
            progress = { miniStatus.progress.coerceIn(0f, 1f) },
            color = PastelSage,
            trackColor = PastelSage.copy(alpha = 0.2f),
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(CircleShape)
              .testTag("maa_mini_install_progress")
          )
        }

        is MiniModelStatus.Installed -> {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = PastelSage,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${MaaMiniModelManager.formatMegabytes(miniStatus.sizeBytes)} on device • SHA ${miniStatus.sha256Short}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (!isActiveMode) {
              val sageContent = PastelSage.contrastingContentColor()
              Button(
                onClick = onActivateInstalled,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                  containerColor = PastelSage,
                  contentColor = sageContent
                ),
                modifier = Modifier
                  .weight(1f)
                  .defaultMinSize(minHeight = 48.dp)
                  .testTag("activate_maa_mini_button")
              ) {
                Text(
                  text = stringResource(R.string.maa_mini_use_action),
                  color = sageContent,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1
                )
              }
            }
            TextButton(
              onClick = { showUninstallConfirm = true },
              modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("uninstall_maa_mini_button")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = stringResource(R.string.maa_mini_remove_action),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }
    }
  }

  if (showUninstallConfirm) {
    GharConfirmDialog(
      title = "Remove Local 0.5B LLM?",
      message = "This deletes the 492 MB offline model weights from device storage and switches back to Cloud AI. You can reinstall it anytime.",
      confirmLabel = "Remove 492 MB",
      onConfirm = {
        showUninstallConfirm = false
        onUninstall()
      },
      onDismiss = { showUninstallConfirm = false },
      dialogTestTag = "confirm_uninstall_model_dialog",
      confirmTestTag = "confirm_uninstall_model_button"
    )
  }
}
