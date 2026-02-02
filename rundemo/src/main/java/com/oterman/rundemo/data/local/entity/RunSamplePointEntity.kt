package com.oterman.rundemo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 采样点表Entity
 * 核心优化：将序列数据(心率、功率、配速等) + GPS轨迹合并为一行存储
 * 
 * 存储效率对比：
 * - iOS方案：1000采样点 × 8类型 = 8000行
 * - Android优化方案：1000采样点 = 1000行
 */
@Entity(
    tableName = "run_sample_point",
    foreignKeys = [ForeignKey(
        entity = RunRecordEntity::class,
        parentColumns = ["workoutId"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId")]
)
data class RunSamplePointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val workoutId: String,              // 外键关联RunRecord
    val sequence: Int,                  // 序列号(从0开始)
    val timestamp: Long,                // 采样时间戳(ms)
    val timeOffset: Int,                // 距离开始时间的秒数
    
    // GPS数据（可为null表示无GPS，如室内跑）
    val latitude: Double? = null,       // 纬度(WGS84)
    val longitude: Double? = null,      // 经度(WGS84)
    val altitude: Double? = null,       // 海拔(米)
    
    // 运动指标（可为null表示该时刻无数据）
    val heartRate: Int? = null,             // 心率(bpm)
    val power: Int? = null,                 // 功率(W)
    val speed: Double? = null,              // 配速(min/km)
    val cadence: Int? = null,               // 步频(spm，已×2)
    val strideLength: Double? = null,       // 步幅(cm)
    val verticalOscillation: Double? = null,// 垂直振幅(cm)
    val contactTime: Double? = null,        // 触地时间(ms)
    val cumulativeDistance: Double? = null  // 累积距离(米)
)

