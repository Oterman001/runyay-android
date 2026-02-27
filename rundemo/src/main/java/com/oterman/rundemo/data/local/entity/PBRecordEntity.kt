package com.oterman.rundemo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 个人最佳记录表Entity
 * 对齐iOS PBRecord
 * 
 * type说明：
 * - "Speed": 配速PB（某个距离的最快用时）
 * - "Ability": 能力PB（最远距离、最大步频等）
 * 
 * subType说明（Speed类型）：
 * - "1k", "3k", "5k", "10k", "21k", "42k"
 * 
 * subType说明（Ability类型）：
 * - "maxDistance", "maxCadence", "maxPower" 等
 */
@Entity(
    tableName = "pb_record",
    indices = [
        Index(value = ["type", "subType"]),
        Index(value = ["workoutId"]),
        Index(value = ["userId", "type", "subType"])
    ]
)
data class PBRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val workoutId: String,
    val type: String,                    // "Speed" 或 "Ability"
    val subType: String,                 // 子类型 "1k"/"3k"/"5k"/"10k"/"21k"/"42k" / 能力指标名
    val value: Double = 0.0,             // PB值（Speed类型为分钟，Ability类型为对应指标值）
    val completeTime: Long = 0L,         // 完成时间戳(ms)
    val inclusiveLevel: Int = 1,          // 0=不纳入统计，1+=纳入统计
    val userId: String = ""               // 所属用户ID
)

