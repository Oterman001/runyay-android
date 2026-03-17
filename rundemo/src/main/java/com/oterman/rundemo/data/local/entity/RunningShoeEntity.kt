package com.oterman.rundemo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "running_shoe",
    indices = [Index(value = ["userId"])]
)
data class RunningShoeEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val brand: String? = null,
    val model: String? = null,
    val shoeSize: String? = null,
    val nickname: String? = null,
    val shoeType: String = "training",
    val price: Double? = null,
    val expectedLifespan: Double = 700.0,
    val firstUseDate: Long? = null,
    val retireDate: Long? = null,
    val initialDistance: Double = 0.0,
    val totalDistance: Double = 0.0,
    val totalDuration: Double = 0.0,
    val totalRuns: Int = 0,
    val imagePath: String? = null,
    val imageUrl: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    val color: String? = null,
    val syncStatus: String = "localOnly",
    val syncRetryCount: Int = 0,
    val serverShoeId: String? = null,
    val lastSyncAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
