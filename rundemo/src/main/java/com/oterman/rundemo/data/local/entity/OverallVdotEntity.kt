package com.oterman.rundemo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 综合VDOT记录表Entity
 * 对齐iOS OverallVdot
 * 
 * 每次跑步后计算一个VDOT值，并进行滑动平均平滑处理
 * originValue是原始计算值，value是平滑后的值
 */
@Entity(
    tableName = "overall_vdot",
    indices = [
        Index(value = ["workoutId"]),
        Index(value = ["date"])
    ]
)
data class OverallVdotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val workoutId: String,
    val date: Long = 0L,                 // 记录日期时间戳(ms)
    val originValue: Double = 0.0,       // 原始VDOT值（未平滑）
    val value: Double = 0.0,             // 平滑后的VDOT值
    val inclusiveLevel: Int = 1          // 0=不纳入统计，1+=纳入统计
)

