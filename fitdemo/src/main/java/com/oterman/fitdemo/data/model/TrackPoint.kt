package com.oterman.fitdemo.data.model

/**
 * GPS轨迹点数据
 */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long? = null,
    val altitude: Double? = null,
    val heartRate: Int? = null,
    val speed: Float? = null
)

