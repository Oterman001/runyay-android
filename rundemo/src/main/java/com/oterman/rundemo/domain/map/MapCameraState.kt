package com.oterman.rundemo.domain.map

/**
 * 地图相机状态（供应商无关）
 * 用于跨供应商传递相机位置
 */
data class MapCameraState(
    val centerLatitude: Double,
    val centerLongitude: Double,
    val zoom: Double,
    val bearing: Double = 0.0,
    val pitch: Double = 0.0
)
