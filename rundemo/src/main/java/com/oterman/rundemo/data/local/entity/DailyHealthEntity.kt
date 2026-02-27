package com.oterman.rundemo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 每日健康数据Entity
 * 存储从各平台获取的静息心率、VO2Max等健康数据
 *
 * 复合唯一索引 (userId, platformCode, calendarDate) 确保同一用户同一平台同一天只有一条记录
 * userId索引用于用户隔离查询
 */
@Entity(
    tableName = "daily_health",
    indices = [
        Index(value = ["userId", "platformCode", "calendarDate"], unique = true),
        Index(value = ["userId", "calendarDate"])
    ]
)
data class DailyHealthEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val platformCode: String,          // "GCN", "GGB", "COROS"
    val calendarDate: String,          // "yyyy-MM-dd" (matches API response)
    val restingHeartRate: Int? = null,
    val vo2Max: Double? = null,        // null for COROS
    val fetchedAt: Long = System.currentTimeMillis()
)
