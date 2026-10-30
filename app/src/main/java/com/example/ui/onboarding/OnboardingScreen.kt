package com.example.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.withFrameNanos
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppContainer
import com.example.R
import com.example.RootAppContainer
import com.example.capture.HouseholdNotificationHelper
import com.example.core.perf.GharResourcePrewarmer
import com.example.core.localization.GharTerms
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.model.BudgetProfile
import com.example.core.model.GuiltSensitivity
import com.example.core.model.HabitType
import com.example.core.model.InstrumentType
import com.example.core.model.PainDial
import com.example.core.model.SavingsGoal
import com.example.core.util.CurrencyFormatter
import com.example.ui.auth.OptionalCloudVaultChoiceSection
import com.example.ui.auth.performInteractiveGoogleSignIn
import com.example.ui.common.GullakCanvas
import com.example.ui.common.HabitIcon
import com.example.ui.common.HandwrittenHomeNote
import com.example.ui.common.JharokhaCardShape
import com.example.ui.common.MaaAvatar
import com.example.ui.common.PapaAvatar
import com.example.ui.common.PebbleCardShape
import com.example.ui.common.PhysicalCashAnimationOverlay
import com.example.ui.common.SafeGrowthGauge
import com.example.ui.common.StickyNotePaperColors
import com.example.ui.common.pastelHomeBackdrop
import com.example.ui.common.rememberCachedGharPainter
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelSageContainer
import com.example.ui.theme.PastelSky
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.PastelTerracottaContainer
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.contrastingContentColor
import com.example.ui.theme.readableAccentOn
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
  rootContainer: RootAppContainer,
  container: AppContainer,
  onStepChanged: (Int) -> Unit = {},
  onRestoredExistingCloudVault: () -> Unit = {},
  onFinishOnboarding: () -> Unit
) {
  var currentStep by remember { mutableIntStateOf(0) }
  val totalSteps = 8

  LaunchedEffect(currentStep) {
    onStepChanged(currentStep)
  }

  val prefs by rootContainer.preferencesRepository.prefsSnapshot.collectAsStateWithLifecycle()
  val plainEnglish = prefs.plainEnglishMode
  val savedUserName = prefs.userName
  val googleFirstName = remember(container.userEmailOrDisplayName) {
    container.userEmailOrDisplayName
      ?.trim()
      ?.substringBefore(" ")
      ?.takeIf { it.isNotBlank() }
      .orEmpty()
  }

  // Form State
  var userNameText by remember { mutableStateOf(googleFirstName) }
  var hasEditedName by remember { mutableStateOf(false) }
  LaunchedEffect(savedUserName, googleFirstName) {
    if (!hasEditedName) {
      val preferred = savedUserName.ifBlank { googleFirstName }
      if (preferred.isNotBlank()) {
        userNameText = preferred
      }
    }
  }

  var lifeStage by remember { mutableStateOf("Salaried") }
  var customIncomeText by remember { mutableStateOf("60000") }
  var fixedCommitmentsPercent by remember { mutableIntStateOf(35) }
  var selectedHabitLeaks by remember {
    mutableStateOf(setOf(HabitType.CHAI_SNACKS, HabitType.FOOD_DELIVERY))
  }
  var selectedGuiltSensitivity by remember { mutableStateOf(GuiltSensitivity.MEDIUM) }
  var savingsGoalName by remember { mutableStateOf("Emergency Fund") }
  var customGoalAmountText by remember { mutableStateOf("40000") }
  var initialSavedAmountText by remember { mutableStateOf("") }
  var selectedPainDial by remember { mutableStateOf(PainDial.BALANCED) }

  fun togglePlainEnglishMode(newValue: Boolean) {
    rootContainer.appScope.launch {
      rootContainer.preferencesRepository.setPlainEnglishMode(newValue)
    }
  }

  fun persistCalibratedHousehold(targetContainer: AppContainer, markOnboardingDone: Boolean) {
    val incomeRupees = customIncomeText.toLongOrNull()?.coerceAtLeast(1000L) ?: 50000L
    val fixedRupees = (incomeRupees * fixedCommitmentsPercent) / 100
    val savingsRupees = if (lifeStage == "Student") {
      (incomeRupees * 15) / 100
    } else {
      (incomeRupees * 20) / 100
    }

    val primaryHabit = selectedHabitLeaks.firstOrNull() ?: HabitType.CHAI_SNACKS
    val resolvedName = userNameText.trim()
      .ifBlank {
        targetContainer.userEmailOrDisplayName
          ?.trim()
          ?.substringBefore(" ")
          .orEmpty()
          .ifBlank { googleFirstName.ifBlank { "Friend" } }
      }
      .take(60)
    val leaksSet = selectedHabitLeaks.map { it.name }.toSet()

    val profile = BudgetProfile(
      monthlyIncomePaise = incomeRupees * 100L,
      fixedCommitmentsPaise = fixedRupees * 100L,
      savingsTargetPaise = savingsRupees * 100L,
      monthStartDay = 1,
      guiltSensitivity = selectedGuiltSensitivity,
      habitType = primaryHabit,
      painDial = selectedPainDial,
      ageBand = if (lifeStage == "Student") "18–24" else "25–34",
      lifeStage = lifeStage
    )

    targetContainer.budgetRepository.saveHouseholdCalibration(
      userName = resolvedName,
      onboardingCompleted = markOnboardingDone,
      profile = profile,
      habitLeaks = leaksSet
    )

    val goalAmountRupees = customGoalAmountText.toLongOrNull()?.coerceAtLeast(1000L) ?: 30000L
    val enteredOpeningRupees = initialSavedAmountText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
    val goal = SavingsGoal(
      id = "primary_goal",
      name = savingsGoalName.ifBlank { "Emergency Fund" },
      targetAmountPaise = goalAmountRupees * 100L,
      currentAmountPaise = enteredOpeningRupees * 100L,
      instrumentType = if (lifeStage == "Student") InstrumentType.NONE else InstrumentType.SIP
    )
    targetContainer.goalRepository.saveGoal(goal)
    targetContainer.seedDefaultDataOnce()

    rootContainer.appScope.launch {
      rootContainer.preferencesRepository.setUserName(resolvedName)
      rootContainer.preferencesRepository.setGuiltSensitivity(selectedGuiltSensitivity)
      rootContainer.preferencesRepository.setPainDial(selectedPainDial)
      rootContainer.preferencesRepository.setHabitLeaks(leaksSet)
      if (markOnboardingDone) {
        rootContainer.preferencesRepository.setOnboardingCompleted(true)
      }
    }
  }

  CompositionLocalProvider(LocalPlainEnglishMode provides plainEnglish) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding(),
      color = MaterialTheme.colorScheme.background
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .pastelHomeBackdrop(
            primaryWash = PastelTerracotta,
            secondaryWash = PastelSage,
            tertiaryWash = PastelHoney
          )
      ) {
        // Top Navigation & Stepping-Stone Pebble Trail (Steps 1..8)
        if (currentStep > 0) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 22.dp, vertical = 10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              IconButton(
                onClick = { currentStep-- },
                modifier = Modifier.testTag("onboarding_back_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back",
                  tint = MaterialTheme.colorScheme.onBackground
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = if (plainEnglish) "Step $currentStep" else "Room Note $currentStep",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = " of $totalSteps",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              for (stepIdx in 1..totalSteps) {
                val isReached = stepIdx <= currentStep
                val isCurrent = stepIdx == currentStep
                val stoneColor by animateColorAsState(
                  targetValue = when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isReached -> PastelSage
                    else -> MaterialTheme.colorScheme.surfaceVariant
                  },
                  label = "pebbleColor_$stepIdx"
                )
                Box(
                  modifier = Modifier
                    .weight(if (isCurrent) 1.6f else 1f)
                    .height(if (isCurrent) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(stoneColor)
                )
              }
            }
          }
        }

        // Step Body
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
              if (targetState > initialState) {
                (slideInHorizontally(
                  initialOffsetX = { it / 4 },
                  animationSpec = tween(180)
                ) + fadeIn(animationSpec = tween(180))) togetherWith
                  (slideOutHorizontally(
                    targetOffsetX = { -it / 6 },
                    animationSpec = tween(140)
                  ) + fadeOut(animationSpec = tween(130)))
              } else {
                (slideInHorizontally(
                  initialOffsetX = { -it / 4 },
                  animationSpec = tween(180)
                ) + fadeIn(animationSpec = tween(180))) togetherWith
                  (slideOutHorizontally(
                    targetOffsetX = { it / 6 },
                    animationSpec = tween(140)
                  ) + fadeOut(animationSpec = tween(130)))
              }
            },
            label = "stepContent"
          ) { step ->
            val stepScrollState = remember(step) { ScrollState(0) }
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(stepScrollState)
                .padding(horizontal = if (step == 0) 0.dp else 22.dp)
            ) {
              when (step) {
                0 -> StepWelcome(
                  plainEnglish = plainEnglish,
                  onTogglePlainEnglish = { togglePlainEnglishMode(it) },
                  onStart = { currentStep = 1 },
                  onRestoredOrSignedIn = { firebaseUser ->
                    val userCont = rootContainer.forUser(
                      userId = firebaseUser.uid,
                      displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@")
                    )
                    withTimeoutOrNull(4_500L) {
                      userCont.isInitialSyncReady.first { it }
                    }
                    if (userCont.isOnboardingCompletedLocally) {
                      onRestoredExistingCloudVault()
                    } else {
                      val gName = firebaseUser.displayName
                        ?.trim()
                        ?.substringBefore(" ")
                        .orEmpty()
                      if (gName.isNotBlank() && !hasEditedName) {
                        userNameText = gName
                      }
                      currentStep = 1
                    }
                  }
                )
                1 -> StepLifeStage(
                  plainEnglish = plainEnglish,
                  userName = userNameText,
                  onUserNameChange = {
                    hasEditedName = true
                    userNameText = it
                  },
                  selected = lifeStage,
                  onSelect = { lifeStage = it }
                )
                2 -> StepIncomeTailored(
                  lifeStage = lifeStage,
                  amountText = customIncomeText,
                  onAmountChange = { customIncomeText = it }
                )
                3 -> StepFixedTailored(
                  plainEnglish = plainEnglish,
                  lifeStage = lifeStage,
                  incomeRupees = customIncomeText.toLongOrNull() ?: 50000L,
                  currentPercent = fixedCommitmentsPercent,
                  onPercentChange = { fixedCommitmentsPercent = it }
                )
                4 -> StepHabitLeaksMultiSelect(
                  plainEnglish = plainEnglish,
                  selectedLeaks = selectedHabitLeaks,
                  onToggle = { habit ->
                    selectedHabitLeaks = if (selectedHabitLeaks.contains(habit)) {
                      if (selectedHabitLeaks.size > 1) selectedHabitLeaks - habit else selectedHabitLeaks
                    } else {
                      selectedHabitLeaks + habit
                    }
                  }
                )
                5 -> StepGuiltSensitivity(
                  plainEnglish = plainEnglish,
                  selected = selectedGuiltSensitivity,
                  onSelect = { selectedGuiltSensitivity = it }
                )
                6 -> StepSavingsGoalCustom(
                  plainEnglish = plainEnglish,
                  name = savingsGoalName,
                  onNameChange = { savingsGoalName = it },
                  amountText = customGoalAmountText,
                  onAmountChange = { customGoalAmountText = it },
                  initialSavedText = initialSavedAmountText,
                  onInitialSavedChange = { initialSavedAmountText = it }
                )
                7 -> StepPermissionsOptional(
                  plainEnglish = plainEnglish,
                  rootContainer = rootContainer
                )
                8 -> OptionalCloudVaultChoiceSection(
                  userName = userNameText.ifBlank { googleFirstName.ifBlank { "Friend" } },
                  isAlreadySignedIn = container.isCloudAuthenticated || Firebase.auth.currentUser != null,
                  signedInLabel = container.userEmailOrDisplayName
                    ?: Firebase.auth.currentUser?.email
                    ?: Firebase.auth.currentUser?.displayName,
                  onSignedInProceed = { firebaseUser ->
                    val targetContainer = if (firebaseUser != null) {
                      rootContainer.forUser(
                        userId = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@")
                      )
                    } else {
                      container
                    }
                    rootContainer.guestContainer.budgetRepository.setOnboardingCompleted(true)
                    persistCalibratedHousehold(targetContainer, markOnboardingDone = true)
                    rootContainer.appScope.launch {
                      rootContainer.preferencesRepository.setGuestModeChosen(false)
                      rootContainer.preferencesRepository.setOnboardingCompleted(true)
                    }
                    onFinishOnboarding()
                  },
                  onContinueAsGuest = {
                    persistCalibratedHousehold(container, markOnboardingDone = true)
                    rootContainer.appScope.launch {
                      rootContainer.preferencesRepository.setGuestModeChosen(true)
                      rootContainer.preferencesRepository.setOnboardingCompleted(true)
                    }
                    onFinishOnboarding()
                  },
                  modifier = Modifier.padding(bottom = 20.dp)
                )
              }
            }
          }
        }

        // Bottom CTA Button for Steps 1..7
        if (currentStep in 1..7) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 22.dp, vertical = 12.dp)
          ) {
            Button(
              onClick = {
                if (currentStep == 8) {
                  persistCalibratedHousehold(container, markOnboardingDone = false)
                  currentStep = 9
                } else {
                  currentStep++
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 54.dp)
                .testTag("onboarding_next_button"),
              shape = RoundedCornerShape(16.dp),
              elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              )
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Text(
                  text = if (currentStep == 1) "Continue" else "Continue",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary
                )
              }
            }
          }
        }
      }

      // Silently compile room banners, Skia glyph strikes, foley WAVs, and Compose paragraph
      // layouts in the background while the user steps through onboarding — zero initial lag!
      OnboardingBackgroundCompilerHost(rootContainer = rootContainer)
    }
  }
}

@Composable
private fun OnboardingBackgroundCompilerHost(
  rootContainer: RootAppContainer
) {
  val context = LocalContext.current
  var mountOffscreenGlyphPass by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    GharResourcePrewarmer.startWarmup(
      context = context,
      rootContainer = rootContainer,
      isOnboardingCompleted = false
    )
    // Wait until the first Welcome frame is drawn before warming offscreen Compose ParagraphIntrinsics
    withFrameNanos {}
    kotlinx.coroutines.delay(140L)
    mountOffscreenGlyphPass = true
  }

  if (mountOffscreenGlyphPass) {
    Box(
      modifier = Modifier
        .size(1.dp)
        .graphicsLayer { alpha = 0f }
    ) {
      val sample = "₹2,450 Safe to Spend • Rasoi • Gullak • Khata"
      Text(text = sample, style = MaterialTheme.typography.displayMedium)
      Text(text = sample, style = MaterialTheme.typography.headlineMedium)
      Text(text = sample, style = MaterialTheme.typography.titleLarge)
      Text(text = sample, style = MaterialTheme.typography.titleMedium)
      Text(text = sample, style = MaterialTheme.typography.bodyMedium)
      Text(text = sample, style = MaterialTheme.typography.labelSmall)
      Text(text = sample, style = GharHandStyles.stickyNoteTitle)
      Text(text = sample, style = GharHandStyles.stickyNoteBody)
      Text(text = sample, style = GharHandStyles.moneyHero)
      Text(text = sample, style = GharHandStyles.moneyLarge)
      Text(text = sample, style = GharHandStyles.moneySmall)
      MaaAvatar(size = 24.dp, isSpeaking = false)
      PapaAvatar(size = 24.dp)
      GullakCanvas(size = 32.dp)
    }
  }
}

// ----------------------------------------------------------------------------
// STEP 0: WELCOME — FULL-BLEED ILLUSTRATION + PLAIN ENGLISH MODE TOGGLE
// ----------------------------------------------------------------------------
@Composable
private fun StepWelcome(
  plainEnglish: Boolean,
  onTogglePlainEnglish: (Boolean) -> Unit,
  onStart: () -> Unit,
  onRestoredOrSignedIn: suspend (com.google.firebase.auth.FirebaseUser) -> Unit
) {
  val bgColor = MaterialTheme.colorScheme.background
  val context = LocalContext.current
  val restoreScope = rememberCoroutineScope()
  var isRestoring by remember { mutableStateOf(false) }
  var restoreError by remember { mutableStateOf<String?>(null) }
  val maaName = GharTerms.maaName(plainEnglish)
  val papaName = GharTerms.papaName(plainEnglish)

  val scrimBrush = remember(bgColor) {
    Brush.verticalGradient(
      0.0f to Color(0xFF1F1610).copy(alpha = 0.28f),
      0.28f to Color.Transparent,
      0.60f to bgColor.copy(alpha = 0.22f),
      0.84f to bgColor.copy(alpha = 0.84f),
      1.0f to bgColor
    )
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(290.dp)
    ) {
      Image(
        painter = rememberCachedGharPainter(R.drawable.img_living_courtyard_1792931331737),
        contentDescription = "Sunlit Ghar Courtyard",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(scrimBrush)
      )

      Surface(
        shape = CircleShape,
        color = Color(0xFF231811).copy(alpha = 0.48f),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.28f)),
        modifier = Modifier
          .align(Alignment.TopStart)
          .statusBarsPadding()
          .padding(horizontal = 22.dp, vertical = 12.dp)
      ) {
        Text(
          text = if (plainEnglish) "GHAR • PERSONAL BUDGET" else "GHAR • YOUR HOME",
          style = MaterialTheme.typography.labelSmall,
          letterSpacing = 1.8.sp,
          color = Color(0xFFFFF8EC),
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
      }

      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(horizontal = 22.dp, vertical = 6.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          MaaAvatar(size = 40.dp, isSpeaking = false)
          PapaAvatar(size = 40.dp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Money That Feels Like Home",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onBackground,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 22.dp)
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      // Character Card 1: Maa / Mom
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(2.dp, PebbleCardShape),
        shape = PebbleCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PastelTerracotta.copy(alpha = 0.35f))
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.Top
        ) {
          MaaAvatar(size = 46.dp, isSpeaking = false)
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = maaName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.width(8.dp))
              Surface(
                shape = CircleShape,
                color = PastelTerracottaContainer.copy(alpha = 0.75f)
              ) {
                Text(
                  text = if (plainEnglish) "Daily Budget Guide" else "In the Rasoi",
                  style = MaterialTheme.typography.labelSmall,
                  color = PastelTerracotta,
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (plainEnglish) {
                "Manages your daily petty cash, everyday expenses, quick daily math, habit nudges, and gives warm daily motivation."
              } else {
                "Looks after your daily Batua, petty kharcha, quick household math, habit nudges, and gives warm daily motivation."
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 18.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Character Card 2: Papa / Dad
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(2.dp, PebbleCardShape),
        shape = PebbleCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.35f))
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.Top
        ) {
          PapaAvatar(size = 46.dp)
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = papaName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.width(8.dp))
              Surface(
                shape = CircleShape,
                color = PastelSageContainer.copy(alpha = 0.75f)
              ) {
                Text(
                  text = if (plainEnglish) "Savings & Investments" else "Gullak & Study",
                  style = MaterialTheme.typography.labelSmall,
                  color = PastelSage,
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (plainEnglish) {
                "Oversees your long-term savings, Gullak goal guidance, fixed commitments, investments, and real-world financial wisdom."
              } else {
                "Oversees your long-term Gullak goals, fixed commitments, investments, inflation advice, and real-world financial wisdom."
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 18.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = onStart,
        enabled = !isRestoring,
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 54.dp)
          .testTag("begin_calibration_button"),
        shape = CircleShape,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Text(
            text = if (plainEnglish) "Set Up My Budget" else "Set Up My Home",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      TextButton(
        onClick = {
          if (isRestoring) return@TextButton
          restoreError = null
          isRestoring = true
          restoreScope.launch {
            val res = performInteractiveGoogleSignIn(context)
            res.fold(
              onSuccess = { fbUser ->
                onRestoredOrSignedIn(fbUser)
                isRestoring = false
              },
              onFailure = { err ->
                isRestoring = false
                if (err !is GetCredentialCancellationException) {
                  restoreError = err.localizedMessage ?: "Could not sign in right now. Tap above to start in Guest Mode."
                }
              }
            )
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 48.dp)
          .testTag("restore_cloud_vault_button")
      ) {
        if (isRestoring) {
          CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (plainEnglish) "Checking your Cloud Backup..." else "Checking your Cloud Khata...")
        } else {
          Icon(
            imageVector = Icons.Default.CloudDone,
            contentDescription = null,
            tint = PastelSage,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (plainEnglish) "Already have a Cloud Backup? Sign in" else "Already have a Cloud Khata? Sign in",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      restoreError?.let { errText ->
        Text(
          text = errText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}

// ----------------------------------------------------------------------------
// STEP 1: OPTIONAL PERMISSIONS & THEIR PURPOSE (Right after Welcome)
// ----------------------------------------------------------------------------
@Composable
private fun StepPermissionsOptional(
  plainEnglish: Boolean,
  rootContainer: RootAppContainer
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var notifGranted by remember {
    mutableStateOf(HouseholdNotificationHelper.hasNotificationPermission(context))
  }
  var smsGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    )
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        notifGranted = HouseholdNotificationHelper.hasNotificationPermission(context)
        smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
          ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val notifPermLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    notifGranted = granted
    if (granted) {
      val prefs = rootContainer.preferencesRepository.prefsSnapshot.value
      HouseholdNotificationHelper.syncAllScheduledNotifications(
        context = context,
        routines = prefs.dailyRoutines,
        smartNudgesEnabled = prefs.smartRandomNudgesEnabled
      )
    }
  }

  val smsPermLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { result ->
    smsGranted = (result[Manifest.permission.READ_SMS] == true ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
  }

  Surface(
    shape = CircleShape,
    color = PastelSage.copy(alpha = 0.16f),
    border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.42f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Security,
        contentDescription = null,
        tint = PastelSage.readableAccentOn(MaterialTheme.colorScheme.background),
        modifier = Modifier.size(15.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "100% OPTIONAL • PROCESSED ON YOUR PHONE",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = PastelSage.readableAccentOn(MaterialTheme.colorScheme.background),
        letterSpacing = 0.6.sp
      )
    }
  }

  Spacer(modifier = Modifier.height(10.dp))

  Text(
    text = "Helpful Permissions",
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onBackground,
    fontWeight = FontWeight.Bold
  )
  Text(
    text = "Enable only what helps your routine. Every permission below is completely optional — Ghar works offline even with none enabled.",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
  )

  OnboardingPermissionCard(
    icon = Icons.Default.NotificationsActive,
    accentColor = PastelTerracotta,
    title = "Daily Routines & Smart Nudges",
    purpose = "Lets Ghar send your scheduled daily reminder routines and occasional daytime nudges when bills are due or days go unlogged.",
    isGranted = notifGranted,
    actionLabel = "Allow Notifications",
    testTag = "onboarding_perm_notifications_button",
    onEnableClick = {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        notifGranted = true
      }
    }
  )

  Spacer(modifier = Modifier.height(12.dp))

  OnboardingPermissionCard(
    icon = Icons.Default.Sms,
    accentColor = PastelSky,
    title = "Bank & UPI SMS Capture",
    purpose = if (plainEnglish) {
      "Automatically detects bank or UPI SMS alerts to log spends and update your latest bank balance on-device. Personal chats and OTPs are ignored."
    } else {
      "Automatically detects bank or UPI SMS alerts to update your Batua and latest bank balance on-device. Personal chats and OTPs are ignored."
    },
    isGranted = smsGranted,
    actionLabel = "Allow Bank SMS",
    testTag = "onboarding_perm_sms_button",
    onEnableClick = {
      smsPermLauncher.launch(
        arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
      )
    }
  )

  Spacer(modifier = Modifier.height(14.dp))

  Text(
    text = "You can change or revoke any of these anytime in Settings or Android App Info.",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp)
  )
}

@Composable
private fun OnboardingPermissionCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  title: String,
  purpose: String,
  isGranted: Boolean,
  actionLabel: String,
  testTag: String,
  onEnableClick: () -> Unit
) {
  val readableAccent = accentColor.readableAccentOn(MaterialTheme.colorScheme.surface)
  val buttonContent = accentColor.contrastingContentColor()

  Surface(
    shape = PebbleCardShape,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(
      width = if (isGranted) 1.6.dp else 1.dp,
      color = if (isGranted) PastelSage.copy(alpha = 0.65f) else accentColor.copy(alpha = 0.36f)
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = readableAccent,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f)
            )
            Surface(
              shape = CircleShape,
              color = if (isGranted) PastelSage.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                text = if (isGranted) "Enabled" else "Optional",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) PastelSage.readableAccentOn(MaterialTheme.colorScheme.surface) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = purpose,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (isGranted) {
        Surface(
          shape = CircleShape,
          color = PastelSage.copy(alpha = 0.15f),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .testTag(testTag)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = PastelSage.readableAccentOn(MaterialTheme.colorScheme.surface),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Permission Active",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = PastelSage.readableAccentOn(MaterialTheme.colorScheme.surface)
            )
          }
        }
      } else {
        Button(
          onClick = onEnableClick,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = buttonContent
          ),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag(testTag)
        ) {
          Text(
            text = actionLabel,
            fontWeight = FontWeight.Bold,
            color = buttonContent
          )
        }
      }
    }
  }
}

// ----------------------------------------------------------------------------
// STEP 2: YOUR NAME & LIFE STAGE
// ----------------------------------------------------------------------------
@Composable
private fun StepLifeStage(
  plainEnglish: Boolean,
  userName: String,
  onUserNameChange: (String) -> Unit,
  selected: String,
  onSelect: (String) -> Unit
) {
  val maaName = GharTerms.maaName(plainEnglish)
  val options = if (plainEnglish) {
    listOf(
      LifeStageData("Salaried", "Monthly paycheck, fixed rent, loan payments, weekend dining", PastelTerracotta),
      LifeStageData("Student", "Monthly allowance, dorm/housing, campus meals & supplies", PastelSage),
      LifeStageData("Freelancer / Self-Employed", "Variable client income, business expenses, buffer months", PastelHoney),
      LifeStageData("Homemaker / Other", "Monthly household budget, groceries, family savings", PastelSky)
    )
  } else {
    listOf(
      LifeStageData("Salaried", "Monthly paycheck, fixed rent, EMIs, Friday takeout", PastelTerracotta),
      LifeStageData("Student", "Pocket money, hostel/mess, college canteen & notes", PastelSage),
      LifeStageData("Freelancer / Self-Employed", "Variable billing cadence, client payouts, lean periods", PastelHoney),
      LifeStageData("Homemaker / Other", "Household groceries pool, family upkeep & savings", PastelSky)
    )
  }

  Text(
    text = "Tell $maaName a little about you",
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onBackground,
    fontWeight = FontWeight.Bold
  )
  Text(
    text = if (plainEnglish) {
      "Personalize your greeting and tailor your daily budget defaults."
    } else {
      "Maa greets you by name in the rasoi and tailors Ghar to your everyday routine."
    },
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
  )

  OutlinedTextField(
    value = userName,
    onValueChange = { onUserNameChange(it.take(60)) },
    label = {
      Text(
        if (plainEnglish) "Your first name (e.g. Alex)" else "What should Maa call you? (e.g. Harshit)"
      )
    },
    singleLine = true,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("onboarding_name_input"),
    shape = RoundedCornerShape(16.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = MaterialTheme.colorScheme.primary,
      unfocusedBorderColor = MaterialTheme.colorScheme.outline,
      focusedContainerColor = MaterialTheme.colorScheme.surface,
      unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )
  )

  Spacer(modifier = Modifier.height(16.dp))

  Text(
    text = "Your current life stage",
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onBackground,
    modifier = Modifier.padding(bottom = 10.dp)
  )

  options.forEach { item ->
    val isSelected = selected == item.title
    val scale by animateFloatAsState(
      targetValue = if (isSelected) 1.02f else 1.0f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
      label = "lifeStageScale"
    )

    Surface(
      shape = PebbleCardShape,
      color = if (isSelected) item.accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
      border = BorderStroke(
        width = if (isSelected) 2.dp else 1.dp,
        color = if (isSelected) item.accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .clickable { onSelect(item.title) }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isSelected) item.accentColor else MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isSelected) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) item.accentColor.contrastingContentColor() else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) item.accentColor.readableAccentOn(MaterialTheme.colorScheme.surface) else MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(10.dp))
  }
}

private data class LifeStageData(
  val title: String,
  val subtitle: String,
  val accentColor: Color
)

// ----------------------------------------------------------------------------
// STEP 2: TAILORED MONTHLY INCOME
// ----------------------------------------------------------------------------
@Composable
private fun StepIncomeTailored(
  lifeStage: String,
  amountText: String,
  onAmountChange: (String) -> Unit
) {
  val (headline, desc, chips) = when (lifeStage) {
    "Student" -> Triple(
      "Monthly Allowance / Stipend",
      "How much allowance or stipend do you manage each month?",
      listOf(5000L, 8000L, 12000L, 20000L)
    )
    "Freelancer / Self-Employed" -> Triple(
      "Typical Monthly Income",
      "Average monthly income credited to your account.",
      listOf(40000L, 75000L, 120000L, 200000L)
    )
    "Homemaker / Other" -> Triple(
      "Monthly Household Budget",
      "Monthly amount allocated for household and discretionary expenses.",
      listOf(30000L, 50000L, 80000L, 120000L)
    )
    else -> Triple(
      "Monthly Take-Home Salary",
      "Net monthly income credited to your account after tax.",
      listOf(35000L, 60000L, 100000L, 160000L)
    )
  }

  val incomeVal = amountText.toLongOrNull() ?: 50000L
  val walletRatio = (incomeVal.toFloat() / 150000f).coerceIn(0.15f, 1.0f)

  Text(
    text = headline,
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onBackground,
    fontWeight = FontWeight.Bold
  )
  Text(
    text = desc,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
  )

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "MONTHLY INCOME",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = CurrencyFormatter.formatRupees(incomeVal),
            style = GharHandStyles.moneyHero,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "≈ ₹${incomeVal / 30} / day gross",
            style = GharHandStyles.moneySmall,
            color = PastelSage,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        SafeGrowthGauge(
          percentageSafe = walletRatio,
          isPaceHot = false,
          modifier = Modifier.padding(start = 10.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = amountText,
        onValueChange = { input ->
          val digits = input.filter { it.isDigit() }
          onAmountChange(digits)
        },
        label = { Text("Exact monthly amount") },
        prefix = {
          Text(
            text = "₹ ",
            style = GharHandStyles.moneyMedium,
            color = MaterialTheme.colorScheme.primary
          )
        },
        textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("custom_income_input"),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline,
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          focusedTextColor = MaterialTheme.colorScheme.onSurface,
          unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
      )
    }
  }

  Spacer(modifier = Modifier.height(16.dp))

  Text(
    text = "Or tap a quick preset:",
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
  )
  Spacer(modifier = Modifier.height(8.dp))

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    chips.forEach { chipAmount ->
      val isSelected = amountText == chipAmount.toString()
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
          1.5.dp,
          if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = Modifier
          .weight(1f)
          .defaultMinSize(minHeight = 46.dp)
          .clickable { onAmountChange(chipAmount.toString()) }
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)
        ) {
          Text(
            text = CurrencyFormatter.formatCompactRupees(chipAmount),
            style = GharHandStyles.moneySmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(14.dp))

  var showUnsureNudgeDialog by remember { mutableStateOf(false) }

  Surface(
    onClick = {
      onAmountChange("30000")
      showUnsureNudgeDialog = true
    },
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.50f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("unsure_income_button")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.EventRepeat,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Not a fixed income or unsure?",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "We'll set a starter baseline and gently remind you periodically to log incoming money.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }

  if (showUnsureNudgeDialog) {
    AlertDialog(
      onDismissRequest = { showUnsureNudgeDialog = false },
      title = { Text("Variable Income Mode Active") },
      text = { Text("Ghar handles freelancing, variable commission, and flexible allowances with ease! We've set a ₹30,000 baseline for now, and Maa & Papa will periodically remind you to log new earnings when they arrive.") },
      confirmButton = {
        TextButton(onClick = { showUnsureNudgeDialog = false }) {
          Text("Got it")
        }
      }
    )
  }
}

// ----------------------------------------------------------------------------
// STEP 3: FIXED MONTHLY BILLS
// ----------------------------------------------------------------------------
private data class FixedOptionItem(
  val percent: Int,
  val title: String,
  val subtitle: String
)

@Composable
private fun StepFixedTailored(
  plainEnglish: Boolean,
  lifeStage: String,
  incomeRupees: Long,
  currentPercent: Int,
  onPercentChange: (Int) -> Unit
) {
  val walletName = GharTerms.walletName(plainEnglish)
  val options: List<FixedOptionItem> = when (lifeStage) {
    "Student" -> listOf(
      FixedOptionItem(0, "Zero fixed bills (All flexible allowance)", "₹0 fixed commitments"),
      FixedOptionItem(
        20,
        if (plainEnglish) "Dorm / housing or meal plan (~20%)" else "Hostel / PG or Mess contribution (~20%)",
        "Around ${CurrencyFormatter.formatRupees(incomeRupees * 20 / 100)} committed"
      ),
      FixedOptionItem(35, "Rent, books & transit pass (~35%)", "Around ${CurrencyFormatter.formatRupees(incomeRupees * 35 / 100)} committed")
    )
    "Freelancer / Self-Employed" -> listOf(
      FixedOptionItem(20, "Low overheads (~20% workspace/tools)", "Around ${CurrencyFormatter.formatRupees(incomeRupees * 20 / 100)} fixed"),
      FixedOptionItem(35, "Moderate home & work bills (~35%)", "Around ${CurrencyFormatter.formatRupees(incomeRupees * 35 / 100)} fixed"),
      FixedOptionItem(50, "High rent, studio & commitments (~50%)", "Around ${CurrencyFormatter.formatRupees(incomeRupees * 50 / 100)} fixed")
    )
    else -> listOf(
      FixedOptionItem(
        25,
        if (plainEnglish) "Light commitments (~25% rent/loans)" else "Light commitments (~25% rent/EMI)",
        "Around ${CurrencyFormatter.formatRupees(incomeRupees * 25 / 100)} fixed"
      ),
      FixedOptionItem(
        40,
        if (plainEnglish) "Standard city living (~40% rent, bills, loans)" else "Typical city living (~40% rent, bills, EMIs)",
        "Around ${CurrencyFormatter.formatRupees(incomeRupees * 40 / 100)} fixed"
      ),
      FixedOptionItem(
        55,
        if (plainEnglish) "High commitments (~55% rent, loans)" else "Heavy commitments (~55% rent, EMIs)",
        "Around ${CurrencyFormatter.formatRupees(incomeRupees * 55 / 100)} fixed"
      )
    )
  }

  val fixedRupees = (incomeRupees * currentPercent) / 100
  val discretionaryRupees = incomeRupees - fixedRupees - ((incomeRupees * 15) / 100)

  Text(
    text = if (lifeStage == "Student") "Fixed vs Flexible Budget" else "Fixed Monthly Bills",
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onBackground,
    fontWeight = FontWeight.Bold
  )
  Text(
    text = if (lifeStage == "Student") {
      "What share of your monthly allowance is already committed to housing or fixed bills?"
    } else {
      "Rent, loan payments, and utility bills you pay every month. What remains after savings becomes your daily safe-to-spend budget."
    },
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
  )

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PastelTerracotta))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Fixed ${CurrencyFormatter.formatCompactRupees(fixedRupees)}",
            style = GharHandStyles.moneySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PastelSage))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "$walletName ${CurrencyFormatter.formatCompactRupees(discretionaryRupees.coerceAtLeast(0))}",
            style = GharHandStyles.moneySmall,
            color = PastelSage,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      val animatedFixedRatio by animateFloatAsState(
        targetValue = currentPercent / 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "splitBarFixedRatio"
      )

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
          .clip(RoundedCornerShape(5.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Row(modifier = Modifier.fillMaxSize()) {
          Box(
            modifier = Modifier
              .weight(animatedFixedRatio.coerceAtLeast(0.02f))
              .fillMaxSize()
              .background(PastelTerracotta)
          )
          Box(
            modifier = Modifier
              .weight((1f - animatedFixedRatio).coerceAtLeast(0.02f))
              .fillMaxSize()
              .background(PastelSage)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Drag slider to set exact bills: $currentPercent% (${CurrencyFormatter.formatRupees(fixedRupees)})",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )

      androidx.compose.material3.Slider(
        value = currentPercent.toFloat(),
        onValueChange = { onPercentChange(kotlin.math.round(it).toInt().coerceIn(0, 75)) },
        valueRange = 0f..75f,
        steps = 74,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("fixed_bills_slider")
      )
    }
  }

  Spacer(modifier = Modifier.height(14.dp))

  options.forEach { item ->
    SelectCard(
      title = item.title,
      subtitle = item.subtitle,
      isSelected = currentPercent == item.percent,
      onClick = { onPercentChange(item.percent) }
    )
    Spacer(modifier = Modifier.height(10.dp))
  }
}

// ----------------------------------------------------------------------------
// STEP 4: SPENDING HABITS MULTI-CHOICE
// ----------------------------------------------------------------------------
@Composable
private fun StepHabitLeaksMultiSelect(
  plainEnglish: Boolean,
  selectedLeaks: Set<HabitType>,
  onToggle: (HabitType) -> Unit
) {
  val maaName = GharTerms.maaName(plainEnglish)
  Text(
    text = "Where do you tend to overspend?",
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onBackground,
    fontWeight = FontWeight.Bold
  )
  Text(
    text = "Select all that apply. $maaName will watch for recurring spending spikes in these categories.",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
  )

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
    border = BorderStroke(0.5.dp, PastelSage.copy(alpha = 0.4f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.AutoAwesome,
        contentDescription = null,
        tint = PastelSage,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "${selectedLeaks.size} categories selected for budget tracking",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }

  Spacer(modifier = Modifier.height(12.dp))

  HabitType.entries.forEach { habit ->
    val isSelected = selectedLeaks.contains(habit)
    val scale by animateFloatAsState(
      targetValue = if (isSelected) 1.015f else 1.0f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
      label = "leakScale"
    )

    Surface(
      shape = PebbleCardShape,
      color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
      border = BorderStroke(
        width = if (isSelected) 1.8.dp else 1.dp,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 56.dp)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .clickable { onToggle(habit) }
        .testTag("leak_item_${habit.name.lowercase()}")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            HabitIcon(
              habit = habit,
              size = 20.dp,
              tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Text(
            text = habit.displayLabel(plainEnglish),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Icon(
          imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
          contentDescription = null,
          tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
      }
    }
    Spacer(modifier = Modifier.height(10.dp))
  }
}

// ----------------------------------------------------------------------------
// STEP 5: GUIDANCE TONE PREVIEW
// ----------------------------------------------------------------------------
@Composable
private fun StepGuiltSensitivity(
  plainEnglish: Boolean,
  selected: GuiltSensitivity,
  onSelect: (GuiltSensitivity) -> Unit
) {
  val maaName = GharTerms.maaName(plainEnglish)
  val sampleSpeech = if (plainEnglish) {
    when (selected) {
      GuiltSensitivity.LOW ->
        "“You exceeded your daily budget by ₹650 today. Keep tomorrow's discretionary spend under ₹300 to stay on track.”"
      GuiltSensitivity.MEDIUM ->
        "“3 coffee & snack purchases this week. Your monthly budget is still on track, just keep an eye on small add-ons.”"
      GuiltSensitivity.HIGH ->
        "“Takeout today was ₹650. Having a home-cooked dinner tonight will keep tomorrow's daily allowance comfortable.”"
      GuiltSensitivity.NONE ->
        "“No overspend nudges will be sent. $maaName will only display your standard daily totals.”"
    }
  } else {
    when (selected) {
      GuiltSensitivity.LOW ->
        "“Beta, takeout crossed today's safe limit by ₹650. Keep tomorrow light so month-end stays comfortable.”"
      GuiltSensitivity.MEDIUM ->
        "“3 chai stops this week. We are still on track for the month, let's just keep evenings simple.”"
      GuiltSensitivity.HIGH ->
        "“Takeout today was ₹650. Let's make dinner a home-cooked meal so tomorrow's Batua stays relaxed.”"
      GuiltSensitivity.NONE ->
        "“Overspend nudges are turned off. $maaName will stay silent and only display your daily totals.”"
    }
  }

  Text(
    text = "How should $maaName guide you?",
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onBackground,
    fontWeight = FontWeight.Bold
  )
  Text(
    text = "Choose how direct or gentle budget check-in notes should feel.",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
  )

  HandwrittenHomeNote(
    isMintPaper = selected == GuiltSensitivity.LOW,
    tiltDegrees = -0.7f,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.Top,
      modifier = Modifier.fillMaxWidth()
    ) {
      MaaAvatar(size = 44.dp, isSpeaking = false)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "$maaName's Note (${selected.label})",
          style = GharHandStyles.stickyNoteMeta,
          color = StickyNotePaperColors.InkAccentRed,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = sampleSpeech,
          style = GharHandStyles.stickyNoteBody,
          color = StickyNotePaperColors.InkDark
        )
      }
    }
  }

  Spacer(modifier = Modifier.height(18.dp))

  GuiltSensitivity.entries.forEach { sens ->
    SelectCard(
      title = sens.label,
      subtitle = sens.description,
      isSelected = selected == sens,
      onClick = { onSelect(sens) }
    )
    Spacer(modifier = Modifier.height(10.dp))
  }
}

// ----------------------------------------------------------------------------
// STEP 6: SAVINGS GOAL (Starts honestly at ₹0 unless user enters existing savings)
// ----------------------------------------------------------------------------
@Composable
private fun StepSavingsGoalCustom(
  plainEnglish: Boolean,
  name: String,
  onNameChange: (String) -> Unit,
  amountText: String,
  onAmountChange: (String) -> Unit,
  initialSavedText: String,
  onInitialSavedChange: (String) -> Unit
) {
  val potName = GharTerms.savingsPotName(plainEnglish)
  val suggestions = if (plainEnglish) {
    listOf("Emergency Fund", "Vacation Trip", "New Laptop", "Vehicle Fund")
  } else {
    listOf("Emergency Fund", "Himachal Trip", "MacBook", "Gift for Maa")
  }
  val goalRupees = amountText.toLongOrNull() ?: 30000L
  val openingSavedRupees = initialSavedText.toLongOrNull() ?: 0L

  Text(
    text = if (plainEnglish) "Set Your First Savings Goal" else "What is Papa's Gullak for?",
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onBackground,
    fontWeight = FontWeight.Bold
  )
  Text(
    text = "Pick a concrete savings target. Your $potName starts at ₹0 unless you enter an amount already saved.",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
  )

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.4f))
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      GullakCanvas(size = 82.dp)
      Spacer(modifier = Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = name.ifBlank { "Savings Goal" }.uppercase(),
          style = MaterialTheme.typography.labelSmall,
          color = PastelSage,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = CurrencyFormatter.formatRupees(goalRupees),
          style = GharHandStyles.moneyHero,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = if (openingSavedRupees > 0L) {
            "Starts with ${CurrencyFormatter.formatRupees(openingSavedRupees)} already saved."
          } else {
            "Starts honestly at ₹0 — add money as you save."
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }

  Spacer(modifier = Modifier.height(14.dp))

  OutlinedTextField(
    value = name,
    onValueChange = onNameChange,
    label = { Text("Savings goal name") },
    singleLine = true,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("savings_goal_name_input"),
    shape = RoundedCornerShape(14.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = MaterialTheme.colorScheme.primary,
      unfocusedBorderColor = MaterialTheme.colorScheme.outline,
      focusedContainerColor = MaterialTheme.colorScheme.surface,
      unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )
  )

  Spacer(modifier = Modifier.height(10.dp))

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    suggestions.take(3).forEach { suggestion ->
      val isSelected = name == suggestion
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
          1.dp,
          if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = Modifier
          .weight(1f)
          .defaultMinSize(minHeight = 40.dp)
          .clickable { onNameChange(suggestion) }
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
          Text(
            text = suggestion,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(16.dp))

  OutlinedTextField(
    value = amountText,
    onValueChange = { input -> onAmountChange(input.filter { it.isDigit() }) },
    label = { Text("Target Goal Amount (₹)") },
    prefix = {
      Text(
        text = "₹ ",
        style = GharHandStyles.moneyMedium,
        color = MaterialTheme.colorScheme.primary
      )
    },
    textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("savings_goal_amount_input"),
    shape = RoundedCornerShape(14.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = MaterialTheme.colorScheme.primary,
      unfocusedBorderColor = MaterialTheme.colorScheme.outline,
      focusedContainerColor = MaterialTheme.colorScheme.surface,
      unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )
  )

  Spacer(modifier = Modifier.height(12.dp))

  OutlinedTextField(
    value = initialSavedText,
    onValueChange = { input -> onInitialSavedChange(input.filter { it.isDigit() }) },
    label = { Text("Already saved toward this? (Optional — defaults to ₹0)") },
    placeholder = { Text("0") },
    prefix = {
      Text(
        text = "₹ ",
        style = GharHandStyles.moneyMedium,
        color = PastelSage
      )
    },
    textStyle = GharHandStyles.moneyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("savings_goal_initial_saved_input"),
    shape = RoundedCornerShape(14.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = PastelSage,
      unfocusedBorderColor = MaterialTheme.colorScheme.outline,
      focusedContainerColor = MaterialTheme.colorScheme.surface,
      unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )
  )
}

// ----------------------------------------------------------------------------
// STEP 7: VISUAL CASH FEEDBACK
// ----------------------------------------------------------------------------
@Composable
private fun StepPainDial(
  plainEnglish: Boolean,
  selected: PainDial,
  onSelect: (PainDial) -> Unit
) {
  var testingDirection by remember { mutableStateOf<Boolean?>(null) }

  Text(
    text = "Visual Cash Feedback",
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onBackground,
    fontWeight = FontWeight.Bold
  )
  Text(
    text = if (plainEnglish) {
      "Choose how strongly currency notes animate when you log expenses or income."
    } else {
      "Choose how strongly rupee notes animate out of or into your daily Batua."
    },
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
  )

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      modifier = Modifier
        .weight(1f)
        .defaultMinSize(minHeight = 56.dp)
        .clickable { testingDirection = false }
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.Payments,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Preview Spend",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Icon(
          imageVector = Icons.Default.TouchApp,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
      }
    }

    Surface(
      shape = RoundedCornerShape(14.dp),
      color = RupeeGreen.copy(alpha = 0.14f),
      border = BorderStroke(1.dp, RupeeGreen.copy(alpha = 0.45f)),
      modifier = Modifier
        .weight(1f)
        .defaultMinSize(minHeight = 56.dp)
        .clickable { testingDirection = true }
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.Payments,
            contentDescription = null,
            tint = RupeeGreen,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (plainEnglish) "Preview Income" else "Feel Shagun",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = RupeeGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Icon(
          imageVector = Icons.Default.TouchApp,
          contentDescription = null,
          tint = RupeeGreen,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }

  testingDirection?.let { incoming ->
    if (selected == PainDial.SUBTLE) {
      LaunchedEffect(incoming) {
        testingDirection = null
      }
    } else {
      PhysicalCashAnimationOverlay(
        amountPaise = 750_00L,
        painDial = selected,
        hapticsEnabled = true,
        isIncoming = incoming,
        onAnimationFinished = { testingDirection = null }
      )
    }
  }

  Spacer(modifier = Modifier.height(14.dp))

  PainDial.entries.forEach { dial ->
    SelectCard(
      title = dial.label,
      subtitle = dial.description,
      isSelected = selected == dial,
      onClick = { onSelect(dial) }
    )
    Spacer(modifier = Modifier.height(10.dp))
  }
}

@Composable
private fun SelectCard(
  title: String,
  subtitle: String? = null,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val scale by animateFloatAsState(
    targetValue = if (isSelected) 1.02f else 1.0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    label = "selectCardScale"
  )

  Surface(
    shape = PebbleCardShape,
    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface,
    border = BorderStroke(
      width = if (isSelected) 2.dp else 1.dp,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 56.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clickable { onClick() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
      )
      if (subtitle != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
