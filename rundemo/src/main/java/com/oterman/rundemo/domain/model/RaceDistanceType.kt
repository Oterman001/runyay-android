package com.oterman.rundemo.domain.model

/**
 * Race distance type enum
 * Corresponds to iOS RaceDistanceType
 * Theme colors are resolved in UI layer via RunTheme.colorScheme.raceColorFor(type)
 */
enum class RaceDistanceType(
    val displayName: String,
) {
    MARATHON("全马"),
    HALF_MARATHON("半马"),
    TEN_K("10公里"),
    FIVE_K("5公里"),
    OTHER("其他")
}
