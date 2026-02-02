package com.oterman.rundemo.domain.model

/**
 * 能力区间类型枚举
 */
enum class AbilityZoneType(val value: Int, val displayName: String) {
    HEART_RATE_7(1, "心率7区间"),
    HEART_RATE_5(2, "心率5区间"),
    SPEED(3, "配速区间");
    
    companion object {
        fun fromValue(value: Int): AbilityZoneType? {
            return entries.find { it.value == value }
        }
    }
}

/**
 * 能力区间领域模型
 */
data class AbilityZone(
    val zoneType: AbilityZoneType,
    val zoneIndex: Int,             // 区间序号(1-7或1-5)
    val duration: Double,           // 在该区间的时长(分钟)
    val minValue: Double,           // 区间下限
    val maxValue: Double,           // 区间上限
    val percentage: Double = 0.0    // 占总时长的百分比
) {
    /**
     * 获取区间名称
     */
    fun getZoneName(): String {
        return when (zoneType) {
            AbilityZoneType.HEART_RATE_7 -> "Z$zoneIndex"
            AbilityZoneType.HEART_RATE_5 -> "Z$zoneIndex"
            AbilityZoneType.SPEED -> "E${zoneIndex}"  // E/M/T/I/R
        }
    }
    
    /**
     * 格式化时长显示
     */
    fun getFormattedDuration(): String {
        val minutes = duration.toInt()
        val seconds = ((duration - minutes) * 60).toInt()
        return String.format("%d:%02d", minutes, seconds)
    }
    
    /**
     * 格式化百分比显示
     */
    fun getFormattedPercentage(): String {
        return String.format("%.1f%%", percentage)
    }
}

/**
 * 心率区间汇总
 */
data class HeartRateZoneSummary(
    val zones: List<AbilityZone>,
    val totalDuration: Double,
    val is7Zone: Boolean = true     // true=7区间，false=5区间
)

/**
 * 配速区间汇总
 */
data class SpeedZoneSummary(
    val zones: List<AbilityZone>,
    val totalDuration: Double
)

