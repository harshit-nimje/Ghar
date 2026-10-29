package com.example.ui.house

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.audio.GharSoundEffects
import com.example.ui.common.HandwrittenHomeNote
import com.example.ui.common.MaaAvatar
import com.example.ui.common.StickyNotePaperColors
import com.example.ui.common.pastelHomeBackdrop
import com.example.ui.common.rememberCachedGharPainter
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelTerracotta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EntryAnimationScreen(
  soundEnabled: Boolean = true,
  onWalkInside: () -> Unit
) {
  val context = LocalContext.current
  val doorAlpha = remember { Animatable(0f) }
  val scaleAnim = remember { Animatable(1.03f) }
  val contentAlpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    launch {
      doorAlpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(360, easing = FastOutSlowInEasing)
      )
    }
    scaleAnim.animateTo(
      targetValue = 1f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
      )
    )
    delay(60)
    contentAlpha.animateTo(1f, animationSpec = tween(260))
  }

  val bgColor = MaterialTheme.colorScheme.background
  val scrimBrush = remember(bgColor) {
    Brush.verticalGradient(
      0.0f to Color(0xFF1F1610).copy(alpha = 0.28f),
      0.28f to Color.Transparent,
      0.60f to bgColor.copy(alpha = 0.20f),
      0.84f to bgColor.copy(alpha = 0.82f),
      1.0f to bgColor
    )
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = bgColor
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .pastelHomeBackdrop(
          primaryWash = PastelTerracotta,
          secondaryWash = PastelSage,
          tertiaryWash = PastelHoney
        )
        .navigationBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1.12f)
          .graphicsLayer {
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
            alpha = doorAlpha.value
          }
      ) {
        Image(
          painter = rememberCachedGharPainter(R.drawable.img_house_entry_1792907129736),
          contentDescription = "Ghar Arched Front Door",
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
          border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.3f)),
          modifier = Modifier
            .align(Alignment.TopStart)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
          Text(
            text = "GHAR • YOUR VERANDAH",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.8.sp,
            color = Color(0xFFFFF8EC),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }

        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
          Text(
            text = "Welcome Home",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .graphicsLayer { alpha = contentAlpha.value }
          .padding(horizontal = 22.dp, vertical = 14.dp)
      ) {
        HandwrittenHomeNote(
          isMintPaper = false,
          tiltDegrees = -0.8f,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            MaaAvatar(size = 42.dp, isSpeaking = false)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Maa — on the Rasoi Fridge",
                style = GharHandStyles.stickyNoteMeta,
                color = StickyNotePaperColors.InkAccentRed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "“Step inside, beta. Take off your shoes and relax. Your home is always waiting for you.”",
                style = GharHandStyles.stickyNoteBody,
                color = StickyNotePaperColors.InkDark
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = {
            runCatching { GharSoundEffects.playDoorKundiOpen(context, soundEnabled) }
            runCatching { onWalkInside() }
          },
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .testTag("step_inside_button"),
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
            Icon(
              imageVector = Icons.Default.MeetingRoom,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Step into Rasoi",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}
