package com.oterman.rundemo.data.local

data class HearRateZoneSettings(
    val isManualEnabled: Boolean = false,
    val maxHeartRate: Int = 190,
    val restingHeartRate: Int = 60,
    val birthdayMillis: Long = 0L,
    val isMale: Boolean = true,
    val isAutoSyncEnabled: Boolean = true,
    val preferredPlatform: String? = null  // null = 跨平台自动最优
)
