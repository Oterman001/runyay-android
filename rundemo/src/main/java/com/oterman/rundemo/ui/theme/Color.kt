package com.oterman.rundemo.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Primary blue aligned with iOS Color.theme.blue (#1AA9F8)
val RunBlue = Color(0xFF1AA9F8)
val RunBlueGradient1 = Color(0xFF2BCCFC)
val RunBlueGradient2 = Color(0xFF53E4FC)
val RunBlueLiner = Color(0xFF38CCFF)
val RunLiteBlue1Light = Color(0xFFEFF6FF)
val RunLiteBlue1Dark = Color(0xFF1B1C1E)

val RunOrange = Color(0xFFFF9500)
val SecondaryTextColor = Color(0xFF8E8E93)
val CardBgLight = Color(0xFFFFFFFF)
val CardBgDark = Color(0xFF1C1C1E)
val NoDataBg = Color(0xFFF2F2F7)
val NoDataBgDark = Color(0xFF2C2C2E)

val DayCellBadgeBg = Color(0xFFE0E0E0)
val DayCellBadgeText = Color(0xFFFF3B30)

val DividerLight = Color(0xFFE5E5EA)
val DividerDark = Color(0xFF3A3A3C)

val CountdownRed = Color(0xFFFF3B30)
val CountdownOrange = Color(0xFFFF9500)
val CountdownGreen = Color(0xFF34C759)
val CountdownGray = Color(0xFF8E8E93)
val VerifiedGreen = Color(0xFF34C759)

@Immutable
data class RunColorScheme(
    val blue: Color,
    val blueGradient1: Color,
    val blueGradient2: Color,
    val blueLiner: Color,
    val liteBlue1: Color,
    val orange: Color,
    val secondaryText: Color,
    val cardBg: Color,
    val noDataBg: Color,
    val divider: Color,
    val dayCellActive: Color,
    val dayCellBadgeBg: Color,
    val dayCellBadgeText: Color,
)

val LightRunColorScheme = RunColorScheme(
    blue = RunBlue,
    blueGradient1 = RunBlueGradient1,
    blueGradient2 = RunBlueGradient2,
    blueLiner = RunBlueLiner,
    liteBlue1 = RunLiteBlue1Light,
    orange = RunOrange,
    secondaryText = SecondaryTextColor,
    cardBg = CardBgLight,
    noDataBg = NoDataBg,
    divider = DividerLight,
    dayCellActive = RunBlue,
    dayCellBadgeBg = DayCellBadgeBg,
    dayCellBadgeText = DayCellBadgeText,
)

val DarkRunColorScheme = RunColorScheme(
    blue = RunBlue,
    blueGradient1 = RunBlueGradient1,
    blueGradient2 = RunBlueGradient2,
    blueLiner = RunBlueLiner,
    liteBlue1 = RunLiteBlue1Dark,
    orange = RunOrange,
    secondaryText = SecondaryTextColor,
    cardBg = CardBgDark,
    noDataBg = NoDataBgDark,
    divider = DividerDark,
    dayCellActive = RunBlue,
    dayCellBadgeBg = DayCellBadgeBg,
    dayCellBadgeText = DayCellBadgeText,
)

val LocalRunColorScheme = staticCompositionLocalOf { LightRunColorScheme }
