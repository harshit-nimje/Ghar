package com.example.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.AppContainer
import com.example.RootAppContainer
import com.example.data.firebase.authStateFlow
import com.example.ui.house.EntryAnimationScreen
import com.example.ui.house.HouseScreen
import com.example.ui.onboarding.OnboardingScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

object GharDestinations {
  const val ONBOARDING = "onboarding"
  const val ENTRY_ANIMATION = "entry_animation"
  const val HOUSE = "house"
}

@Composable
fun GharNavHost(
  rootContainer: RootAppContainer,
  auth: FirebaseAuth = Firebase.auth,
  navController: NavHostController = rememberNavController()
) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  val activeContainer: AppContainer = remember(user?.uid) {
    if (user != null) {
      rootContainer.forUser(
        userId = user.uid,
        displayName = user.displayName ?: user.email?.substringBefore("@")
      )
    } else {
      rootContainer.guestContainer
    }
  }

  val activeHousehold by activeContainer.budgetRepository.householdState.collectAsStateWithLifecycle()
  var manualOnboardingActive by remember { mutableStateOf(false) }

  // Determine start destination synchronously in <1ms from the local household vault.
  // Never blocks on network and never kicks an onboarded user back to Onboarding when offline.
  val startDestination = remember {
    val alreadyOnboarded = activeContainer.isOnboardingCompletedLocally ||
      rootContainer.guestContainer.isOnboardingCompletedLocally
    if (alreadyOnboarded) {
      GharDestinations.HOUSE
    } else {
      GharDestinations.ONBOARDING
    }
  }

  // Ensure starter stamps/seed check runs once if launching straight into HOUSE
  LaunchedEffect(activeContainer.userId, activeHousehold.onboardingCompleted) {
    if (activeHousehold.onboardingCompleted) {
      activeContainer.seedDefaultDataOnce()
      // If user restored an existing Cloud Khata while sitting on Welcome screen, step inside automatically
      if (!manualOnboardingActive &&
        navController.currentDestination?.route == GharDestinations.ONBOARDING
      ) {
        navController.navigate(GharDestinations.HOUSE) {
          popUpTo(GharDestinations.ONBOARDING) { inclusive = true }
        }
      }
    }
  }

  NavHost(
    navController = navController,
    startDestination = startDestination,
    enterTransition = { fadeIn(animationSpec = tween(200)) },
    exitTransition = { fadeOut(animationSpec = tween(160)) }
  ) {
    composable(GharDestinations.ONBOARDING) {
      OnboardingScreen(
        rootContainer = rootContainer,
        container = activeContainer,
        onStepChanged = { step ->
          manualOnboardingActive = step > 0
        },
        onRestoredExistingCloudVault = {
          manualOnboardingActive = false
          runCatching {
            navController.navigate(GharDestinations.HOUSE) {
              popUpTo(0) { inclusive = true }
            }
          }
        },
        onFinishOnboarding = {
          manualOnboardingActive = false
          runCatching {
            navController.navigate(GharDestinations.ENTRY_ANIMATION) {
              popUpTo(0) { inclusive = true }
            }
          }
        }
      )
    }

    composable(GharDestinations.ENTRY_ANIMATION) {
      val prefs by rootContainer.preferencesRepository.prefsSnapshot.collectAsStateWithLifecycle()
      EntryAnimationScreen(
        soundEnabled = prefs.soundEnabled,
        onWalkInside = {
          runCatching {
            navController.navigate(GharDestinations.HOUSE) {
              popUpTo(0) { inclusive = true }
            }
          }
        }
      )
    }

    composable(GharDestinations.HOUSE) {
      HouseScreen(
        container = activeContainer,
        onResetToOnboarding = {
          manualOnboardingActive = true
          runCatching {
            navController.navigate(GharDestinations.ONBOARDING) {
              popUpTo(0) { inclusive = true }
            }
          }
        },
        onSignOut = {
          // Preserve current calibration in Guest Mode so signing out never resets the user to init state
          val snapHousehold = activeContainer.budgetRepository.householdState.value
          val snapGoal = activeContainer.goalRepository.primaryGoalState.value
          val guest = rootContainer.guestContainer
          if (!guest.isOnboardingCompletedLocally) {
            guest.budgetRepository.saveHouseholdCalibration(
              userName = snapHousehold.userName.ifBlank { "Friend" },
              onboardingCompleted = true,
              profile = snapHousehold.profile,
              habitLeaks = snapHousehold.habitLeaks.toSet()
            )
            guest.goalRepository.saveGoal(snapGoal)
            guest.seedDefaultDataOnce()
          }
          rootContainer.appScope.launch {
            rootContainer.preferencesRepository.setGuestModeChosen(true)
            rootContainer.preferencesRepository.setOnboardingCompleted(true)
          }
          auth.signOut()
        }
      )
    }
  }
}
