package com.oterman.rundemo.data.local

data class HearRateZoneSettings(
    val maxHeartRate: Int = 190,
    val restingHeartRate: Int = 60,
    val birthdayMillis: Long = 0L,
    val isMale: Boolean = true,
    /** true=自动从健康平台拉取静息心率；false=直接使用上方手动设置的值 */
    val isAutoSyncEnabled: Boolean = true,
    val preferredPlatform: String? = null  // null = 跨平台自动最优
)
