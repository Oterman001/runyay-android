package com.oterman.rundemo.domain.model

/**
 * PB Ability data model
 * Corresponds to iOS PBAbilityInfo
 */
data class PBAbilityInfo(
    val itemKey: PBAbilityKey,
    val itemMaxValue: String?,     // "52.5" or "42.52" or "4h30'20\""
    val itemDate: String?,         // "2024-11-11"
    val workoutId: String? = null  // For navigation to record detail
)
