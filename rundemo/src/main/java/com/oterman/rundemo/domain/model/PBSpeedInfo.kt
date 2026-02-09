package com.oterman.rundemo.domain.model

/**
 * PB Speed record data model
 * Corresponds to iOS PBSpeedInfo
 */
data class PBSpeedInfo(
    val pbKey: PBSpeedKey,
    val pbTimeValue: String?,      // "3'45\"" or null
    val pbDate: String?,           // "2024-10-15" or null
    val workoutId: String? = null  // For navigation to record detail
)
