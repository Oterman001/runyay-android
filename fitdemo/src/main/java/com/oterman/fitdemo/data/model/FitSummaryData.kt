package com.oterman.fitdemo.data.model

/**
 * FIT文件解析后的摘要数据
 */
data class FitSummaryData(
    // 文件信息
    val fileInfo: FileInfo? = null,
    
    // 会话摘要
    val sessionSummary: SessionSummary? = null,
    
    // 轨迹信息（总是存在）
    val trackInfo: TrackInfo,
    
    // 区间数据列表
    val laps: List<LapData> = emptyList(),
    
    // 设备信息
    val deviceInfo: DeviceInfo? = null
)

/**
 * 文件信息
 */
data class FileInfo(
    val type: String? = null,
    val manufacturer: String? = null,
    val product: String? = null,
    val serialNumber: String? = null,
    val timeCreated: String? = null,
    val number: Int? = null
)

/**
 * 会话摘要信息
 */
data class SessionSummary(
    val sport: String? = null,
    val subSport: String? = null,
    val startTime: String? = null,
    val totalElapsedTime: String? = null,
    val totalTimerTime: String? = null,
    val totalDistance: String? = null,
    val totalCalories: String? = null,
    val avgSpeed: String? = null,
    val maxSpeed: String? = null,
    val avgPace: String? = null,
    val maxPace: String? = null,
    val avgHeartRate: String? = null,
    val maxHeartRate: String? = null,
    val avgCadence: String? = null,
    val maxCadence: String? = null,
    val totalAscent: String? = null,
    val totalDescent: String? = null,
    val avgStride: String? = null
)

/**
 * 轨迹信息
 */
data class TrackInfo(
    val totalRecords: Int = 0,
    val hasGpsData: Boolean = false,
    val hasHeartRateData: Boolean = false,
    val hasCadenceData: Boolean = false
)

/**
 * 区间数据
 */
data class LapData(
    val lapNumber: Int,
    val startTime: String? = null,
    val totalElapsedTime: String? = null,
    val totalTimerTime: String? = null,
    val totalDistance: String? = null,
    val totalCalories: String? = null,
    val avgSpeed: String? = null,
    val maxSpeed: String? = null,
    val avgPace: String? = null,
    val avgHeartRate: String? = null,
    val maxHeartRate: String? = null,
    val avgCadence: String? = null,
    val totalAscent: String? = null,
    val totalDescent: String? = null
)

/**
 * 设备信息
 */
data class DeviceInfo(
    val manufacturer: String? = null,
    val product: String? = null,
    val serialNumber: String? = null,
    val deviceType: String? = null,
    val hardwareVersion: String? = null,
    val softwareVersion: String? = null
)

