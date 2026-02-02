package com.oterman.rundemo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 能力区间表Entity
 * 存储心率区间和配速区间数据
 * 
 * zoneType说明：
 * - 1: 心率7区间
 * - 2: 心率5区间
 * - 3: 配速区间
 */
@Entity(
    tableName = "run_ability_zone",
    foreignKeys = [ForeignKey(
        entity = RunRecordEntity::class,
        parentColumns = ["workoutId"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId")]
)
data class RunAbilityZoneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val workoutId: String,
    val zoneType: Int,                  // 1=心率7区间，2=心率5区间，3=配速区间
    val zoneIndex: Int,                 // 区间序号(1-7或1-5)
    val duration: Double = 0.0,         // 在该区间的时长(分钟)
    val minValue: Double = 0.0,         // 区间下限
    val maxValue: Double = 0.0          // 区间上限
)

