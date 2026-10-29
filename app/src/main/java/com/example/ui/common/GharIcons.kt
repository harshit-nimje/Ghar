package com.example.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.model.Category
import com.example.core.model.HabitType
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelPlum
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelSky
import com.example.ui.theme.PastelTerracotta

@Composable
fun CategoryIcon(
  category: Category,
  size: Dp = 22.dp,
  tint: Color = PastelTerracotta,
  modifier: Modifier = Modifier
) {
  val icon: ImageVector = when (category) {
    Category.CHAI_SNACKS -> Icons.Default.Coffee
    Category.FOOD_DELIVERY -> Icons.Default.Fastfood
    Category.COMMUTE -> Icons.Default.DirectionsCar
    Category.SHOPPING -> Icons.Default.LocalMall
    Category.GROCERIES -> Icons.Default.ShoppingCart
    Category.BILLS_UTILITIES -> Icons.AutoMirrored.Filled.ReceiptLong
    Category.ENTERTAINMENT -> Icons.Default.Movie
    Category.HEALTH_MEDICAL -> Icons.Default.Favorite
    Category.TRANSFERS -> Icons.Default.AccountBalance
    Category.ALLOWANCE_INCOME -> Icons.Default.CardGiftcard
    Category.OTHER -> Icons.Default.LocalOffer
  }

  Icon(
    imageVector = icon,
    contentDescription = category.displayName,
    tint = tint,
    modifier = modifier.size(size)
  )
}

@Composable
fun HabitIcon(
  habit: HabitType,
  size: Dp = 22.dp,
  tint: Color = PastelTerracotta,
  modifier: Modifier = Modifier
) {
  val icon: ImageVector = when (habit) {
    HabitType.CHAI_SNACKS -> Icons.Default.Coffee
    HabitType.FOOD_DELIVERY -> Icons.Default.Fastfood
    HabitType.RIDES_COMMUTE -> Icons.Default.DirectionsCar
    HabitType.IMPULSE_SHOPPING -> Icons.Default.LocalMall
    HabitType.SUBSCRIPTIONS -> Icons.Default.Movie
    HabitType.NIGHTS_OUT -> Icons.Default.Nightlight
  }

  Icon(
    imageVector = icon,
    contentDescription = habit.label,
    tint = tint,
    modifier = modifier.size(size)
  )
}

@Composable
fun RoomVectorIcon(
  room: HouseRoom,
  size: Dp = 22.dp,
  tint: Color = PastelTerracotta,
  modifier: Modifier = Modifier
) {
  val icon: ImageVector = when (room) {
    HouseRoom.KITCHEN -> Icons.Default.Home
    HouseRoom.BHAI_ROOM -> Icons.Default.Savings
    HouseRoom.LIVING_ROOM -> Icons.Default.Weekend
    HouseRoom.STUDY -> Icons.AutoMirrored.Filled.MenuBook
    HouseRoom.BALCONY -> Icons.Default.Spa
  }

  Icon(
    imageVector = icon,
    contentDescription = room.title,
    tint = tint,
    modifier = modifier.size(size)
  )
}
