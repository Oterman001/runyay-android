package com.oterman.rundemo.domain.model

import java.util.Date

/**
 * 分段类型枚举
 */
enum class SegmentType(val value: Int) {
    KILOMETER(1),   // 公里分段
    TRAINING(2);    // 训练分段
    
    companion object {
        fun fromValue(value: Int): SegmentType {
            return entries.find { it.value == value } ?: KILOMETER
        }
    }
}

/**
 * 间歇类型枚举
 */
enum class IntervalType(val value: String, val displayName: String) {
    WARMUP("warmup", "热身"),
    WORK("work", "训练"),
    RECOVERY("recovery", "恢复"),
    COOLDOWN("cooldown", "放松"),
    UNKNOWN("", "跑步");
    
    companion object {
        fun fromValue(value: String?): IntervalType {
            return entries.find { it.value == value } ?: UNKNOWN
        }
    }
}

/**
 * 跑步分段领域模型
 */
data class RunSegment(
    val seq: Int,                       // 分段序号
    val segmentType: SegmentType,       // 分段类型
    val beginTime: Date,
    val endTime: Date,
    val duration: Double,               // 总时长(分钟)
    val activeDuration: Double,         // 运动时长(分钟)
    val distance: Double,               // 距离(公里)
    
    // 运动指标
    val averageSpeed: Double,           // 平均配速(min/km)
    val averageHeartRate: Double,
    val averagePower: Double,
    val averageCadence: Double,
    val averageStrideLength: Double,
    val averageVerticalOscillation: Double,
    val averageContactTime: Double,
    val stepCount: Double,
    
    // 训练分段特有
    val intervalType: IntervalType,
    val wktStepIndex: Int?,
    val displayName: String?,
    
    // 标记
    val isFastest: Boolean = false
) {
    /**
     * 格式化配速显示
     */
    fun getFormattedSpeed(): String {
        if (averageSpeed <= 0) return "-"
        val minutes = averageSpeed.toInt()
        val seconds = ((averageSpeed - minutes) * 60).toInt()
        return "${minutes}'${seconds.toString().padStart(2, '0')}\""
    }
    
    /**
     * 格式化距离显示
     */
    fun getFormattedDistance(): String {
        return if (distance < 1) {
            String.format("%.2f", distance)
        } else {
            String.format("%.1f", distance)
        }
    }
    
    /**
     * 格式化时长显示
     */
    fun getFormattedDuration(): String {
        val totalMinutes = activeDuration.toInt()
        val minutes = totalMinutes
        val seconds = ((activeDuration - totalMinutes) * 60).toInt()
        return String.format("%d:%02d", minutes, seconds)
    }
    
    /**
     * 获取分段名称
     */
    fun getSegmentName(): String {
        return when (segmentType) {
            SegmentType.KILOMETER -> "第${seq + 1}公里"
            SegmentType.TRAINING -> displayName ?: intervalType.displayName
        }
    }
}

