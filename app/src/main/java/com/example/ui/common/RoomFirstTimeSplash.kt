package com.example.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.localization.LocalPlainEnglishMode
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelPlum
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelSky
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.contrastingContentColor

private data class RoomSplashBullet(
  val icon: ImageVector,
  val text: String
)

private data class RoomSplashSpec(
  val badge: String,
  val title: String,
  val warmIntro: String,
  val isBhaiHost: Boolean,
  val accent: Color,
  val bullets: List<RoomSplashBullet>
)

private fun HouseRoom.splashSpec(plainEnglish: Boolean): RoomSplashSpec = when (this) {
  HouseRoom.KITCHEN -> if (plainEnglish) {
    RoomSplashSpec(
      badge = "First Visit • Home",
      title = "Welcome to Home",
      warmIntro = "Track today's safe-to-spend budget, log transactions in seconds, and import bank SMS alerts.",
      isBhaiHost = false,
      accent = PastelTerracotta,
      bullets = listOf(
        RoomSplashBullet(Icons.Default.WbSunny, "Check your exact Safe to Spend Today amount and remaining cycle balance."),
        RoomSplashBullet(Icons.Default.Calculate, "Type quick entries like 'coffee 40', scan bank SMS, or use the Calculator."),
        RoomSplashBullet(Icons.Default.BookmarkBorder, "Tap any transaction to edit or undo, ask Mom for advice, and save notes.")
      )
    )
  } else {
    RoomSplashSpec(
      badge = "First Visit • Maa's Rasoi",
      title = "Welcome to the Kitchen",
      warmIntro = "Track today's safe-to-spend budget, log expenses quickly, and scan bank SMS alerts.",
      isBhaiHost = false,
      accent = PastelTerracotta,
      bullets = listOf(
        RoomSplashBullet(Icons.Default.WbSunny, "See your exact Safe to Spend Today amount and remaining monthly balance."),
        RoomSplashBullet(Icons.Default.Calculate, "Type quick entries like 'chai 30', scan bank SMS, or use the Calculator."),
        RoomSplashBullet(Icons.Default.BookmarkBorder, "Tap any entry to edit or undo, chat with Maa, and stash favourite Chitthis.")
      )
    )
  }

  HouseRoom.BHAI_ROOM -> if (plainEnglish) {
    RoomSplashSpec(
      badge = "First Visit • Savings",
      title = "Welcome to Savings Goals",
      warmIntro = "Every Savings Pot starts at ₹0 and grows only as you add money toward your target.",
      isBhaiHost = true,
      accent = PastelSage,
      bullets = listOf(
        RoomSplashBullet(Icons.Default.Savings, "Add coins, sweep today's unspent daily budget, or track multiple Savings Pots."),
        RoomSplashBullet(Icons.Default.Calculate, "Ask Dad for live investment & savings advice, or calculate your target pace."),
        RoomSplashBullet(Icons.Default.Edit, "Tap any deposit to edit or undo, and save helpful financial notes.")
      )
    )
  } else {
    RoomSplashSpec(
      badge = "First Visit • Papa's Sill",
      title = "Welcome to Papa's Gullak",
      warmIntro = "Every Gullak starts honestly at ₹0 and grows as you add savings toward your goal.",
      isBhaiHost = true,
      accent = PastelSage,
      bullets = listOf(
        RoomSplashBullet(Icons.Default.Savings, "Add coins, sweep today's unspent Batua balance, or keep multiple Gullaks."),
        RoomSplashBullet(Icons.Default.Calculate, "Sit with Papa for savings & investment advice, or check Papa's Target Pace."),
        RoomSplashBullet(Icons.Default.Edit, "Tap any Gullak deposit to edit or undo, and stash Papa's practical notes.")
      )
    )
  }

  HouseRoom.LIVING_ROOM -> if (plainEnglish) {
    RoomSplashSpec(
      badge = "First Visit • Insights",
      title = "Welcome to Monthly Insights",
      warmIntro = "Review your monthly spending pace, category breakdown, and recurring bills.",
      isBhaiHost = false,
      accent = PastelSky,
      bullets = listOf(
        RoomSplashBullet(Icons.Default.WbSunny, "Compare your 7-day spending bars and projected month-end spend against budget."),
        RoomSplashBullet(Icons.Default.Spa, "Mark recurring monthly bills paid and export a clean Monthly Summary."),
        RoomSplashBullet(Icons.Default.Edit, "Tap any spending category row to inspect, edit, or undo transactions.")
      )
    )
  } else {
    RoomSplashSpec(
      badge = "First Visit • Courtyard",
      title = "Welcome to the Courtyard",
      warmIntro = "Review your monthly spending pace, category breakdown, and Pakke recurring bills.",
      isBhaiHost = false,
      accent = PastelSky,
      bullets = listOf(
        RoomSplashBullet(Icons.Default.WbSunny, "Compare your 7-day spending bars and projected month-end spend against budget."),
        RoomSplashBullet(Icons.Default.Spa, "Track Pakke recurring bills and share a clean Monthly Hisaab anytime."),
        RoomSplashBullet(Icons.Default.Edit, "Tap any category row to inspect, edit, or undo transactions right in place.")
      )
    )
  }

  HouseRoom.STUDY -> RoomSplashSpec(
    badge = if (plainEnglish) "First Visit • Plan" else "First Visit • Study",
    title = if (plainEnglish) "Welcome to Budget & Growth" else "Welcome to the Study",
    warmIntro = "Calibrate your monthly budget numbers and compare long-term investment returns.",
    isBhaiHost = true,
    accent = PastelHoney,
    bullets = listOf(
      RoomSplashBullet(Icons.Default.Tune, "Update monthly income, fixed bills, savings target, or cycle start day anytime."),
      RoomSplashBullet(Icons.Default.Spa, "Compare Index SIP, Bank RD, PPF, or Cash compounding across 1 to 10 years."),
      RoomSplashBullet(Icons.Default.Check, "Every update recalculates your daily safe budget immediately and can be undone.")
    )
  )

  HouseRoom.BALCONY -> RoomSplashSpec(
    badge = if (plainEnglish) "First Visit • Settings" else "First Visit • Balcony",
    title = if (plainEnglish) "Welcome to Settings" else "Welcome to the Balcony",
    warmIntro = if (plainEnglish) {
      "Manage Plain English mode, SMS & reminder alerts, saved notes, and cloud backup."
    } else {
      "Manage Plain English mode, SMS & reminder alerts, your Chitthi Dabba, and cloud backup."
    },
    isBhaiHost = false,
    accent = PastelPlum,
    bullets = listOf(
      RoomSplashBullet(Icons.Default.BookmarkBorder, "Switch between Indian Household mode and Plain English mode in one tap."),
      RoomSplashBullet(Icons.Default.Tune, "Configure daily log reminders, quick balance notifications, and local AI."),
      RoomSplashBullet(Icons.Default.Check, "Export your ledger summary or link Google Sign-In for cloud sync.")
    )
  )
}

/**
 * In-tree scrim + card splash overlay.
 * Avoids OS WindowManager.addView stalls during room transitions while preserving
 * back-button and scrim dismissal.
 */
@Composable
fun RoomFirstTimeSplashDialog(
  room: HouseRoom,
  onDismiss: () -> Unit
) {
  val plainEnglish = LocalPlainEnglishMode.current
  val spec = remember(room, plainEnglish) { room.splashSpec(plainEnglish) }
  val backdropInteraction = remember { MutableInteractionSource() }
  val cardInteraction = remember { MutableInteractionSource() }

  BackHandler(onBack = onDismiss)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.50f))
      .clickable(
        interactionSource = backdropInteraction,
        indication = null,
        onClick = onDismiss
      )
      .padding(horizontal = 20.dp, vertical = 24.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = JharokhaCardShape,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      border = BorderStroke(1.2.dp, spec.accent.copy(alpha = 0.38f)),
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 440.dp)
        .clickable(
          interactionSource = cardInteraction,
          indication = null,
          onClick = {}
        )
        .testTag("room_first_splash_${room.name.lowercase()}")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 22.dp, vertical = 20.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (spec.isBhaiHost) {
            PapaAvatar(size = 44.dp)
          } else {
            MaaAvatar(size = 44.dp)
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = spec.badge,
              style = GharHandStyles.stickyNoteMeta,
              color = spec.accent,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = spec.title,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = spec.warmIntro,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          spec.bullets.forEach { bullet ->
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = spec.accent.copy(alpha = 0.10f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(spec.accent.copy(alpha = 0.20f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = bullet.icon,
                    contentDescription = null,
                    tint = spec.accent,
                    modifier = Modifier.size(16.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = bullet.text,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        val ctaContentColor = spec.accent.contrastingContentColor()
        Button(
          onClick = onDismiss,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = spec.accent,
            contentColor = ctaContentColor
          ),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("room_splash_dismiss_button")
        ) {
          Text(
            text = stringResource(R.string.room_guide_step_in),
            color = ctaContentColor,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
fun RoomGuideHelpPill(
  roomName: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = CircleShape,
    color = Color(0xFF1E1510).copy(alpha = 0.62f),
    border = BorderStroke(0.9.dp, Color.White.copy(alpha = 0.28f)),
    modifier = modifier
      .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
      .clip(CircleShape)
      .clickable { onClick() }
      .testTag("room_guide_help_button")
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
        contentDescription = "About $roomName",
        tint = Color(0xFFFFFBF4),
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
