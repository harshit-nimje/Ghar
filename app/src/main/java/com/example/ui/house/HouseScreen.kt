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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppContainer
import com.example.capture.CaptureEventBus
import com.example.capture.SmsInboxScanner
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.util.CurrencyFormatter
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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

  // Keep beyondViewportPageCount = 0 so cold start and normal state updates compose ONLY the
  // active room (and incoming neighbor during an active swipe), avoiding 5x synchronous main-thread
  // room compositions and 5x fan-out recompositions on every ledger/preference update.
  val currentPage by remember { derivedStateOf { pagerState.currentPage.coerceIn(0, rooms.lastIndex) } }
  val currentRoom = rooms[currentPage]
  val undoController = rememberGharUndoController()
  val coroutineScope = rememberCoroutineScope()

  // GPU-only micro-entrance for instant bottom-dock jumps (zero layout passes).
  val dockJumpEntrance = remember { Animatable(1f) }
  var dockJumpTargetPage by remember { mutableIntStateOf(-1) }

  // Reset room scroll position cleanly once per settled page transition without duplicate snaps.
  var lastSettledPage by remember { mutableIntStateOf(pagerState.settledPage) }
  LaunchedEffect(pagerState.settledPage) {
    val settled = pagerState.settledPage.coerceIn(0, rooms.lastIndex)
    val previous = lastSettledPage.coerceIn(0, rooms.lastIndex)
    if (previous != settled) {
      snapRoomToTop(rooms[settled])
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

  val pendingWidgetKharcha by CaptureEventBus.pendingQuickKharchaRequest.collectAsStateWithLifecycle()
  LaunchedEffect(pendingWidgetKharcha) {
    if (pendingWidgetKharcha && currentRoom != HouseRoom.KITCHEN) {
      jumpToRoomFromTop(HouseRoom.KITCHEN)
    }
  }

  val housePrefsSliceFlow = remember(container) {
    container.preferencesRepository.prefsSnapshot
      .map { snap -> Triple(snap.isLoaded, snap.plainEnglishMode, snap.seenRoomSplashes) }
      .distinctUntilChanged()
  }
  val housePrefsSlice by housePrefsSliceFlow.collectAsStateWithLifecycle(
    initialValue = container.preferencesRepository.prefsSnapshot.value.let {
      Triple(it.isLoaded, it.plainEnglishMode, it.seenRoomSplashes)
    }
  )
  val (prefsLoaded, plainEnglishMode, seenRoomSplashes) = housePrefsSlice

  val pendingSharedAlert by CaptureEventBus.pendingSharedAlertText.collectAsStateWithLifecycle()
  LaunchedEffect(pendingSharedAlert, container.userId) {
    val rawShared = pendingSharedAlert ?: return@LaunchedEffect
    val consumed = CaptureEventBus.consumeSharedAlertText() ?: return@LaunchedEffect
    val summary = SmsInboxScanner.parseBatchAlertText(
      rawBatch = consumed,
      existingTransactions = container.transactionRepository.transactionsState.value,
      senderLabel = "Shared Alert"
    )
    if (summary.latestAvailableBalancePaise != null) {
      container.preferencesRepository.setLastDetectedBankBalancePaise(summary.latestAvailableBalancePaise)
    }
    if (summary.candidates.isNotEmpty()) {
      val batch = summary.candidates.map { it.transaction }
      container.transactionRepository.insertAll(batch)
      batch.lastOrNull()?.let { CaptureEventBus.notifyNewTransaction(it) }
      val msg = if (batch.size == 1) {
        val single = batch.first()
        "Logged shared alert: ${single.merchant} (${CurrencyFormatter.formatRupees(single.amountRupees)})"
      } else {
        "Logged ${batch.size} shared bank/UPI alerts"
      }
      undoController.recordAction(msg) {
        batch.forEach { container.transactionRepository.delete(it.id) }
      }
    } else {
      undoController.recordAction("No new bank/UPI debit or credit found in shared text") {}
    }
  }

  var manualGuideRoom by remember { mutableStateOf<HouseRoom?>(null) }
  var optimisticSeenRooms by remember { mutableStateOf(emptySet<String>()) }

  val activeSplashRoom: HouseRoom? = manualGuideRoom ?: run {
    if (prefsLoaded &&
      currentRoom.name !in seenRoomSplashes &&
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

  val onRoomSelectedStable: (HouseRoom) -> Unit = remember {
    { room -> jumpToRoomFromTop(room) }
  }
  val onShowKitchenGuide = remember { { manualGuideRoom = HouseRoom.KITCHEN } }
  val onShowBhaiGuide = remember { { manualGuideRoom = HouseRoom.BHAI_ROOM } }
  val onShowLivingGuide = remember { { manualGuideRoom = HouseRoom.LIVING_ROOM } }
  val onShowStudyGuide = remember { { manualGuideRoom = HouseRoom.STUDY } }
  val onShowBalconyGuide = remember { { manualGuideRoom = HouseRoom.BALCONY } }

  CompositionLocalProvider(
    LocalGharUndoController provides undoController,
    LocalPlainEnglishMode provides plainEnglishMode
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
          onRoomSelected = onRoomSelectedStable
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
          beyondViewportPageCount = 0,
          key = { page -> rooms[page].name },
          modifier = Modifier.fillMaxSize()
        ) { page ->
          val room = rooms[page]
          val isJumpTarget = dockJumpTargetPage == page
          val roomModifier = if (isJumpTarget) {
            Modifier
              .fillMaxSize()
              .graphicsLayer {
                val p = dockJumpEntrance.value
                alpha = 0.92f + 0.08f * p
                translationY = (1f - p) * 10.dp.toPx()
              }
          } else {
            Modifier.fillMaxSize()
          }

          when (room) {
            HouseRoom.KITCHEN -> KitchenScreen(
              container = container,
              listState = kitchenListState,
              onShowRoomGuide = onShowKitchenGuide,
              modifier = roomModifier
            )
            HouseRoom.BHAI_ROOM -> BhaiRoomScreen(
              container = container,
              listState = bhaiListState,
              onShowRoomGuide = onShowBhaiGuide,
              modifier = roomModifier
            )
            HouseRoom.LIVING_ROOM -> LivingRoomScreen(
              container = container,
              listState = livingListState,
              onShowRoomGuide = onShowLivingGuide,
              modifier = roomModifier
            )
            HouseRoom.STUDY -> StudyScreen(
              container = container,
              listState = studyListState,
              onShowRoomGuide = onShowStudyGuide,
              modifier = roomModifier
            )
            HouseRoom.BALCONY -> BalconyScreen(
              container = container,
              listState = balconyListState,
              onResetToOnboarding = onResetToOnboarding,
              onSignOut = onSignOut,
              onShowRoomGuide = onShowBalconyGuide,
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
