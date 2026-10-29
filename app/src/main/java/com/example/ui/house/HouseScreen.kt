package com.example.ui.house

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppContainer
import com.example.capture.SmsInboxScanner
import com.example.core.localization.LocalPlainEnglishMode
import com.example.ui.balcony.BalconyScreen
import com.example.ui.bhairoom.BhaiRoomScreen
import com.example.ui.common.GharUndoSnackbarHost
import com.example.ui.common.HallwayAffordance
import com.example.ui.common.HouseRoom
import com.example.ui.common.LocalGharUndoController
import com.example.ui.common.RoomFirstTimeSplashDialog
import com.example.ui.common.rememberGharUndoController
import com.example.ui.kitchen.KitchenScreen
import com.example.ui.livingroom.LivingRoomScreen
import com.example.ui.study.StudyScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HouseScreen(
  container: AppContainer,
  onResetToOnboarding: () -> Unit,
  onSignOut: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val rooms = HouseRoom.entries
  val pagerState = rememberPagerState(initialPage = HouseRoom.KITCHEN.ordinal) {
    rooms.size
  }

  // Dedicated LazyListState per room so every room switch starts cleanly at (0, 0).
  val kitchenListState = remember { LazyListState(0, 0) }
  val bhaiListState = remember { LazyListState(0, 0) }
  val livingListState = remember { LazyListState(0, 0) }
  val studyListState = remember { LazyListState(0, 0) }
  val balconyListState = remember { LazyListState(0, 0) }

  fun listStateFor(room: HouseRoom): LazyListState = when (room) {
    HouseRoom.KITCHEN -> kitchenListState
    HouseRoom.BHAI_ROOM -> bhaiListState
    HouseRoom.LIVING_ROOM -> livingListState
    HouseRoom.STUDY -> studyListState
    HouseRoom.BALCONY -> balconyListState
  }

  suspend fun snapRoomToTop(room: HouseRoom) {
    val state = listStateFor(room)
    if (state.firstVisibleItemIndex != 0 || state.firstVisibleItemScrollOffset != 0) {
      state.scrollToItem(0, 0)
    }
  }

  // Staggered idle-frame pre-warming: Frame 1 composes only Kitchen (<16ms cold start),
  // then idle frames warm the remaining 4 rooms ~85ms apart so the very first swipe or tab tap
  // hits an already-composed, zero-jank room tree.
  var prewarmedBeyondCount by remember { mutableIntStateOf(0) }
  LaunchedEffect(Unit) {
    withFrameNanos {}
    for (step in 1..4) {
      delay(85L)
      while (pagerState.isScrollInProgress) {
        delay(32L)
      }
      withFrameNanos {}
      prewarmedBeyondCount = step
    }
  }

  val currentRoom = rooms[pagerState.currentPage.coerceIn(0, rooms.lastIndex)]
  val undoController = rememberGharUndoController()
  val coroutineScope = rememberCoroutineScope()

  // GPU-only micro-entrance for instant bottom-dock jumps (zero layout passes).
  val dockJumpEntrance = remember { Animatable(1f) }
  var dockJumpTargetPage by remember { mutableIntStateOf(-1) }

  // Guarantee top-of-page scroll reset on horizontal swipe AND reset offscreen previous room
  // so peeking back during a drag already shows the top of that page.
  var lastSettledPage by remember { mutableIntStateOf(pagerState.settledPage) }
  LaunchedEffect(pagerState.currentPage) {
    snapRoomToTop(rooms[pagerState.currentPage.coerceIn(0, rooms.lastIndex)])
  }
  LaunchedEffect(pagerState.settledPage) {
    val settled = pagerState.settledPage.coerceIn(0, rooms.lastIndex)
    val previous = lastSettledPage.coerceIn(0, rooms.lastIndex)
    snapRoomToTop(rooms[settled])
    if (previous != settled) {
      snapRoomToTop(rooms[previous])
      lastSettledPage = settled
    }
  }

  fun jumpToRoomFromTop(targetRoom: HouseRoom) {
    val targetIndex = targetRoom.ordinal
    val previousRoom = rooms[pagerState.currentPage.coerceIn(0, rooms.lastIndex)]
    if (previousRoom == targetRoom && !pagerState.isScrollInProgress) {
      coroutineScope.launch { snapRoomToTop(targetRoom) }
      return
    }
    coroutineScope.launch {
      dockJumpTargetPage = targetIndex
      dockJumpEntrance.snapTo(0f)
      snapRoomToTop(targetRoom)
      pagerState.scrollToPage(targetIndex)
      if (previousRoom != targetRoom) {
        snapRoomToTop(previousRoom)
      }
      lastSettledPage = targetIndex
      dockJumpEntrance.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
      )
      dockJumpTargetPage = -1
    }
  }

  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val prefs by container.preferencesRepository.prefsSnapshot.collectAsStateWithLifecycle()
  var lastSmsRefreshAtMs by remember { mutableLongStateOf(0L) }

  // Zero-constant-RAM foreground catch-up: when returning to Ghar with READ_SMS granted,
  // refresh the latest reported bank balance on IO (throttled to at most once every 60s).
  DisposableEffect(lifecycleOwner, container.userId) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        val now = System.currentTimeMillis()
        val hasReadSms = ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasReadSms && now - lastSmsRefreshAtMs >= 60_000L) {
          lastSmsRefreshAtMs = now
          container.appScope.launch {
            val summary = SmsInboxScanner.scanRecentInbox(
              context = context,
              existingTransactions = container.transactionRepository.transactionsState.value,
              lookbackDays = 2,
              maxMessages = 30
            ).getOrNull()
            summary?.latestAvailableBalancePaise?.let { latestBal ->
              if (latestBal != container.preferencesRepository.prefsSnapshot.value.lastDetectedBankBalancePaise) {
                container.preferencesRepository.setLastDetectedBankBalancePaise(latestBal)
              }
            }
          }
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  var manualGuideRoom by remember { mutableStateOf<HouseRoom?>(null) }
  var optimisticSeenRooms by remember { mutableStateOf(emptySet<String>()) }

  val activeSplashRoom: HouseRoom? = manualGuideRoom ?: run {
    if (prefs.isLoaded &&
      currentRoom.name !in prefs.seenRoomSplashes &&
      currentRoom.name !in optimisticSeenRooms
    ) {
      currentRoom
    } else {
      null
    }
  }

  BackHandler(enabled = currentRoom != HouseRoom.KITCHEN && activeSplashRoom == null) {
    jumpToRoomFromTop(HouseRoom.KITCHEN)
  }

  CompositionLocalProvider(
    LocalGharUndoController provides undoController,
    LocalPlainEnglishMode provides prefs.plainEnglishMode
  ) {
    Scaffold(
      modifier = modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = {
        GharUndoSnackbarHost(
          controller = undoController,
          modifier = Modifier.padding(bottom = 8.dp)
        )
      },
      bottomBar = {
        HallwayAffordance(
          currentRoom = currentRoom,
          onRoomSelected = { room ->
            jumpToRoomFromTop(room)
          }
        )
      }
    ) { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = paddingValues.calculateBottomPadding())
      ) {
        HorizontalPager(
          state = pagerState,
          beyondViewportPageCount = prewarmedBeyondCount,
          key = { page -> rooms[page].name },
          modifier = Modifier.fillMaxSize()
        ) { page ->
          val room = rooms[page]
          val roomModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
              if (dockJumpTargetPage == page) {
                val p = dockJumpEntrance.value
                alpha = 0.92f + 0.08f * p
                translationY = (1f - p) * 10.dp.toPx()
              }
            }

          when (room) {
            HouseRoom.KITCHEN -> KitchenScreen(
              container = container,
              listState = kitchenListState,
              onShowRoomGuide = { manualGuideRoom = HouseRoom.KITCHEN },
              modifier = roomModifier
            )
            HouseRoom.BHAI_ROOM -> BhaiRoomScreen(
              container = container,
              listState = bhaiListState,
              onShowRoomGuide = { manualGuideRoom = HouseRoom.BHAI_ROOM },
              modifier = roomModifier
            )
            HouseRoom.LIVING_ROOM -> LivingRoomScreen(
              container = container,
              listState = livingListState,
              onShowRoomGuide = { manualGuideRoom = HouseRoom.LIVING_ROOM },
              modifier = roomModifier
            )
            HouseRoom.STUDY -> StudyScreen(
              container = container,
              listState = studyListState,
              onShowRoomGuide = { manualGuideRoom = HouseRoom.STUDY },
              modifier = roomModifier
            )
            HouseRoom.BALCONY -> BalconyScreen(
              container = container,
              listState = balconyListState,
              onResetToOnboarding = onResetToOnboarding,
              onSignOut = onSignOut,
              onShowRoomGuide = { manualGuideRoom = HouseRoom.BALCONY },
              modifier = roomModifier
            )
          }
        }

        if (activeSplashRoom != null) {
          RoomFirstTimeSplashDialog(
            room = activeSplashRoom,
            onDismiss = {
              val dismissedRoom = activeSplashRoom
              optimisticSeenRooms = optimisticSeenRooms + dismissedRoom.name
              manualGuideRoom = null
              coroutineScope.launch {
                container.preferencesRepository.markRoomSplashSeen(dismissedRoom.name)
              }
            }
          )
        }
      }
    }
  }
}
