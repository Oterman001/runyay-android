package com.oterman.rundemo.domain.model

import androidx.compose.ui.graphics.Color
import com.oterman.rundemo.ui.theme.RunBlue

/**
 * Race distance type enum with theme colors
 * Corresponds to iOS RaceDistanceType
 */
enum class RaceDistanceType(
    val displayName: String,
    val themeColor: Color
) {
    MARATHON("全马", Color(0xFFFF3B30)),
    HALF_MARATHON("半马", RunBlue),
    TEN_K("10公里", Color(0xFF34C759)),
    FIVE_K("5公里", Color(0xFFFF9500)),
    OTHER("其他", Color(0xFF8E8E93))
}
