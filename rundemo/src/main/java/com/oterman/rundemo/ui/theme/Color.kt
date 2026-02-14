package com.oterman.rundemo.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Running Statistics Card Colors (matching iOS design)
val RunBlue = Color(0xFF007AFF)           // Primary metric values
val RunOrange = Color(0xFFFF9500)         // Week card metrics
val SecondaryTextColor = Color(0xFF8E8E93) // Secondary/helper text
val CardBgLight = Color(0xFFFFFFFF)       // Card background (light mode)
val CardBgDark = Color(0xFF1C1C1E)        // Card background (dark mode)
val NoDataBg = Color(0xFFF2F2F7)          // No data cell background
val NoDataBgDark = Color(0xFF2C2C2E)      // No data cell background (dark mode)

// Day Cell Heatmap Colors
val DayCellActiveColor = RunBlue              // Unified active cell base color (for distance heatmap)
val DayCellBadgeBg = Color(0xFFE0E0E0)        // Badge background for multi-run indicator
val DayCellBadgeText = Color(0xFFFF3B30)       // Badge text color (red)

// Divider Colors
val DividerLight = Color(0xFFE5E5EA)           // Divider color (light mode)
val DividerDark = Color(0xFF3A3A3C)            // Divider color (dark mode)

// Countdown circle colors (for race card)
val CountdownRed = Color(0xFFFF3B30)      // Race <= 7 days
val CountdownOrange = Color(0xFFFF9500)   // Race <= 30 days
val CountdownGreen = Color(0xFF34C759)    // Race > 30 days
val CountdownGray = Color(0xFF8E8E93)     // Race passed
val VerifiedGreen = Color(0xFF34C759)     // Device verified dot