package com.oterman.rundemo.domain.model

import java.util.Date

/**
 * 跑步记录领域模型
 */
data class RunRecord(
    val workoutId: String,
    val startTime: Date,
    val endTime: Date,
    val duration: Double,           // 总时长(分钟)
    val activeDuration: Double,     // 运动时长(分钟)
    val totalDistance: Double,      // 总距离(公里)
    
    // 配速
    val averageSpeed: Double,       // 平均配速(min/km)
    val maxSpeed: Double,           // 最快配速(min/km)
    
    // 心率
    val averageHeartRate: Double,
    val maxHeartRate: Double,
    
    // 步频步幅
    val averageCadence: Double,
    val averageStrideLength: Double,
    
    // 跑步动态
    val averageVerticalOscillation: Double,
    val averageContactTime: Double,
    
    // 训练效果
    val vdot: Double,
    val trainingEffect: Double,
    
    // 消耗
    val totalCalories: Double,
    val totalStepCount: Double,
    val elevationAscended: Double,
    
    // 环境
    val outdoor: Boolean,
    val weatherTemperature: Double,
    
    // 设备
    val deviceInfo: String?,
    val deviceVersion: String?,
    
    // 来源
    val datasource: String?,
    
    // 状态
    val hasTrajectory: Boolean,
    
    // 用户信息
    val note: String?,
    val address: String?
) {
    /**
     * 格式化配速显示 (如 5'30")
     */
    fun getFormattedAverageSpeed(): String {
        if (averageSpeed <= 0) return "-"
        val minutes = averageSpeed.toInt()
        val seconds = ((averageSpeed - minutes) * 60).toInt()
        return "${minutes}'${seconds.toString().padStart(2, '0')}\""
    }
    
    /**
     * 格式化距离显示 (如 10.5公里)
     */
    fun getFormattedDistance(): String {
        return String.format("%.2f", totalDistance)
    }
    
    /**
     * 格式化时长显示 (如 1:05:30)
     */
    fun getFormattedDuration(): String {
        val totalMinutes = activeDuration.toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val seconds = ((activeDuration - totalMinutes) * 60).toInt()
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}

