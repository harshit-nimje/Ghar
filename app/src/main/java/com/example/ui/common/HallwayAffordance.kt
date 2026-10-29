package com.example.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.localization.LocalPlainEnglishMode
import com.example.ui.theme.GoldCoin

enum class HouseRoom(val title: String, val plainTitle: String, val subtitle: String) {
  KITCHEN("Rasoi", "Home", "Daily Budget"),
  BHAI_ROOM("Gullak", "Savings", "Savings Goals"),
  LIVING_ROOM("Courtyard", "Insights", "Monthly Analytics"),
  STUDY("Study", "Plan", "Budget & Growth"),
  BALCONY("Balcony", "Settings", "Preferences");

  fun displayTitle(plainEnglish: Boolean): String = if (plainEnglish) plainTitle else title
}

private val DockShape = RoundedCornerShape(36.dp)

@Composable
fun HallwayAffordance(
  currentRoom: HouseRoom,
  onRoomSelected: (HouseRoom) -> Unit,
  modifier: Modifier = Modifier
) {
  val plainEnglish = LocalPlainEnglishMode.current
  val primaryColor = MaterialTheme.colorScheme.primary
  val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("hallway_bar"),
    shape = DockShape,
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
    tonalElevation = 5.dp,
    shadowElevation = 6.dp,
    border = BorderStroke(1.1.dp, primaryColor.copy(alpha = 0.28f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .drawWithCache {
          val topGlow = Brush.horizontalGradient(
            colors = listOf(
              Color.Transparent,
              GoldCoin.copy(alpha = 0.36f),
              primaryColor.copy(alpha = 0.30f),
              GoldCoin.copy(alpha = 0.36f),
              Color.Transparent
            )
          )
          val lineStroke = 1.dp.toPx()
          onDrawBehind {
            drawLine(
              brush = topGlow,
              start = Offset(size.width * 0.12f, lineStroke),
              end = Offset(size.width * 0.88f, lineStroke),
              strokeWidth = lineStroke
            )
          }
        }
        .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      HouseRoom.entries.forEach { room ->
        val isSelected = room == currentRoom
        val itemInteraction = remember(room) { MutableInteractionSource() }

        val bgColor by animateColorAsState(
          targetValue = if (isSelected) primaryColor else Color.Transparent,
          animationSpec = tween(durationMillis = 150),
          label = "dockPillBg"
        )

        val popScale by animateFloatAsState(
          targetValue = if (isSelected) 1.03f else 1.0f,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
          ),
          label = "dockItemScale"
        )

        val iconTint = if (isSelected) {
          onPrimaryColor
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.84f)
        }

        Box(
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .graphicsLayer {
              scaleX = popScale
              scaleY = popScale
            }
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
              interactionSource = itemInteraction,
              indication = null
            ) { onRoomSelected(room) }
            .padding(
              horizontal = if (isSelected) 15.dp else 12.dp,
              vertical = 10.dp
            )
            .testTag("hallway_room_${room.name.lowercase()}"),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            RoomVectorIcon(
              room = room,
              size = 22.dp,
              tint = iconTint
            )

            if (isSelected) {
              Spacer(modifier = Modifier.width(7.dp))
              Text(
                text = room.displayTitle(plainEnglish),
                style = MaterialTheme.typography.labelLarge,
                color = onPrimaryColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
              )
            }
          }
        }
      }
    }
  }
}
