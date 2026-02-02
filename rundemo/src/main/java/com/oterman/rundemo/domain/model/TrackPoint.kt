package com.oterman.rundemo.domain.model

/**
 * GPS轨迹点领域模型
 */
data class TrackPoint(
    val latitude: Double,           // 纬度(WGS84)
    val longitude: Double,          // 经度(WGS84)
    val altitude: Double? = null,   // 海拔(米)
    val timestamp: Long? = null,    // 时间戳(ms)
    val timeOffset: Int? = null,    // 距离开始时间的秒数
    
    // 可选的运动数据
    val heartRate: Int? = null,
    val speed: Double? = null,
    val cadence: Int? = null
) {
    /**
     * 判断是否为有效坐标
     */
    fun isValidCoordinate(): Boolean {
        return latitude != 0.0 && longitude != 0.0 &&
               latitude >= -90 && latitude <= 90 &&
               longitude >= -180 && longitude <= 180
    }
    
    /**
     * 转换为GCJ-02坐标系（中国地图使用）
     * 注：实际转换需要使用坐标转换库
     */
    fun toGcj02(): TrackPoint {
        // TODO: 实现WGS84到GCJ02的转换
        return this
    }
}

/**
 * 轨迹线段（用于地图显示）
 */
data class TrackSegment(
    val points: List<TrackPoint>,
    val isRunning: Boolean = true   // true=运动中，false=暂停
)

