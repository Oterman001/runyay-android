package com.oterman.rundemo.domain.model

import androidx.compose.ui.graphics.Color
import com.oterman.rundemo.data.local.entity.RunningShoeEntity
import java.util.concurrent.TimeUnit

data class RunningShoe(
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
    val serverShoeId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val localImagePath: String? = null
) {
    val displayImageSource: Any?
        get() = localImagePath?.let { java.io.File(it) }?.takeIf { it.exists() }
            ?: imagePath?.takeIf { it.isNotBlank() }

    val displayName: String
        get() = nickname?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(brand, model).joinToString(" ").takeIf { it.isNotBlank() }
            ?: "未命名跑鞋"

    val displaySubtitle: String?
        get() {
            val brandModel = listOfNotNull(brand, model).joinToString(" ").takeIf { it.isNotBlank() }
            return when {
                !nickname.isNullOrBlank() -> brandModel
                else -> null
            }
        }

    val effectiveDistance: Double
        get() = totalDistance + initialDistance

    val wearPercentage: Double
        get() = if (expectedLifespan > 0) {
            (effectiveDistance / expectedLifespan * 100).coerceIn(0.0, 100.0)
        } else 0.0

    val wearStatusColor: Color
        get() = when {
            wearPercentage < 60 -> Color(0xFF43A047)  // green
            wearPercentage < 80 -> Color(0xFFFDD835)  // yellow
            wearPercentage < 90 -> Color(0xFFFB8C00)  // orange
            else -> Color(0xFFE53935)                   // red
        }

    val wearStatusText: String
        get() = when {
            wearPercentage < 60 -> "良好"
            wearPercentage < 80 -> "中等"
            wearPercentage < 90 -> "较高"
            else -> "需更换"
        }

    val usageDays: Int
        get() {
            val start = firstUseDate ?: createdAt
            val end = retireDate ?: System.currentTimeMillis()
            return TimeUnit.MILLISECONDS.toDays(end - start).toInt().coerceAtLeast(0)
        }

    val isRetired: Boolean
        get() = !isActive

    val remainingDistance: Double
        get() = (expectedLifespan - effectiveDistance).coerceAtLeast(0.0)

    val averageDistancePerRun: Double
        get() = if (totalRuns > 0) totalDistance / totalRuns else 0.0

    val costPerKm: Double?
        get() = price?.let { p ->
            if (effectiveDistance > 0) p / effectiveDistance else null
        }
}

fun RunningShoeEntity.toDomainModel(): RunningShoe {
    return RunningShoe(
        id = id,
        userId = userId,
        brand = brand,
        model = model,
        shoeSize = shoeSize,
        nickname = nickname,
        shoeType = shoeType,
        price = price,
        expectedLifespan = expectedLifespan,
        firstUseDate = firstUseDate,
        retireDate = retireDate,
        initialDistance = initialDistance,
        totalDistance = totalDistance,
        totalDuration = totalDuration,
        totalRuns = totalRuns,
        imagePath = imagePath,
        imageUrl = imageUrl,
        notes = notes,
        isActive = isActive,
        isDefault = isDefault,
        color = color,
        syncStatus = syncStatus,
        serverShoeId = serverShoeId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun RunningShoe.toEntity(): RunningShoeEntity {
    return RunningShoeEntity(
        id = id,
        userId = userId,
        brand = brand,
        model = model,
        shoeSize = shoeSize,
        nickname = nickname,
        shoeType = shoeType,
        price = price,
        expectedLifespan = expectedLifespan,
        firstUseDate = firstUseDate,
        retireDate = retireDate,
        initialDistance = initialDistance,
        totalDistance = totalDistance,
        totalDuration = totalDuration,
        totalRuns = totalRuns,
        imagePath = imagePath,
        imageUrl = imageUrl,
        notes = notes,
        isActive = isActive,
        isDefault = isDefault,
        color = color,
        syncStatus = syncStatus,
        serverShoeId = serverShoeId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
