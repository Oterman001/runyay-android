package com.oterman.rundemo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 分段表Entity
 * 存储公里分段和训练分段数据
 * 
 * segmentType说明：
 * - 1: 公里自动分段（每公里一个分段）
 * - 2: 训练分段（间歇训练的热身、训练、恢复、放松等）
 */
@Entity(
    tableName = "run_segment",
    foreignKeys = [ForeignKey(
        entity = RunRecordEntity::class,
        parentColumns = ["workoutId"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId")]
)
data class RunSegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val workoutId: String,
    val seq: Int,                           // 分段序号(从0开始)
    val segmentType: Int,                   // 1=公里分段，2=训练分段
    
    // 时间信息
    val beginTime: Long,                    // 分段开始时间戳(ms)
    val endTime: Long,                      // 分段结束时间戳(ms)
    val duration: Double = 0.0,             // 总时长(分钟)
    val activeDuration: Double = 0.0,       // 运动时长(分钟)，不含暂停
    
    // 距离
    val distance: Double = 0.0,             // 分段距离(公里)
    
    // 运动指标
    val averageSpeed: Double = 0.0,         // 平均配速(min/km)
    val averageHeartRate: Double = 0.0,
    val averagePower: Double = 0.0,
    val averageCadence: Double = 0.0,       // 平均步频(spm)
    val averageStrideLength: Double = 0.0,  // 平均步幅(cm)
    val averageVerticalOscillation: Double = 0.0, // 垂直振幅(cm)
    val averageContactTime: Double = 0.0,   // 触地时间(ms)
    val stepCount: Double = 0.0,            // 步数
    
    // 训练分段特有字段
    val intervalType: String? = null,       // warmup/work/recovery/cooldown
    val wktStepIndex: Int? = null,          // 训练步骤索引，用于折叠相同步骤
    val displayName: String? = null         // 显示名称
)

