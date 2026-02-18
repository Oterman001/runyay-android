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
    UNKNOWN("", "");
    
    companion object {
        fun fromValue(value: String?): IntervalType {
            return entries.find { it.value == value }
                ?: when (value) {
                    "active", "interval" -> WORK
                    else -> UNKNOWN
                }
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
     * 从 activeDuration/distance 计算配速 (min/km)，格式化为 5'30"
     */
    fun getComputedPace(): String {
        if (distance <= 0 || activeDuration <= 0) return "-"
        val pace = activeDuration / distance  // min/km
        val minutes = pace.toInt()
        val seconds = ((pace - minutes) * 60).toInt()
        return "${minutes}'${seconds.toString().padStart(2, '0')}\""
    }

    /**
     * 获取步骤名称（对标iOS getStepNoAndStepName）
     */
    fun getStepName(): String {
        return if (displayName.isNullOrBlank()) {
            intervalType.displayName
        } else {
            displayName
        }
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

/**
 * 合并后的训练分段（对标iOS MergedRunSegment）
 */
data class MergedRunSegment(
    val id: String,
    val wktStepIndex: Int?,
    val intervalType: IntervalType,
    val subSegments: List<RunSegment>,
    val totalDistance: Double,
    val totalTime: Double,
    val averageSpeed: Double,      // 配速 min/km
    val averageHeartRate: Double,
    val averageCadence: Double,
    val firstSegmentSeq: Int,
    val isMerged: Boolean
) {
    fun getDisplayName(): String {
        return if (isMerged) {
            intervalType.displayName
        } else {
            subSegments.firstOrNull()?.getStepName() ?: intervalType.displayName
        }
    }

    fun getFormattedDistance(): String {
        if (totalDistance <= 0) return "-"
        return if (totalDistance < 1) {
            String.format("%.2f", totalDistance)
        } else {
            String.format("%.1f", totalDistance)
        }
    }

    fun getFormattedDuration(): String {
        if (totalTime <= 0) return "-"
        val totalMinutes = totalTime.toInt()
        val seconds = ((totalTime - totalMinutes) * 60).toInt()
        return String.format("%d:%02d", totalMinutes, seconds)
    }

    fun getFormattedSpeed(): String {
        if (averageSpeed <= 0) return "-"
        val minutes = averageSpeed.toInt()
        val seconds = ((averageSpeed - minutes) * 60).toInt()
        return "${minutes}'${seconds.toString().padStart(2, '0')}\""
    }

    fun getFormattedHeartRate(): String {
        val hr = averageHeartRate.toInt()
        return if (hr > 0) hr.toString() else "-"
    }

    fun getFormattedCadence(): String {
        val cadence = averageCadence.toInt()
        return if (cadence > 0) cadence.toString() else "-"
    }

    companion object {
        fun fromSegments(segments: List<RunSegment>): MergedRunSegment {
            require(segments.isNotEmpty()) { "segments cannot be empty" }
            val sorted = segments.sortedBy { it.seq }
            val first = sorted.first()

            val totalDist = sorted.sumOf { it.distance }
            val totalTime = sorted.sumOf { it.activeDuration }
            // 配速 = totalTime / totalDistance (min/km)
            val speed = if (totalDist > 0) totalTime / totalDist else 0.0

            val totalWeight = sorted.sumOf { it.activeDuration }
            val weightedHr = if (totalWeight > 0) {
                sorted.sumOf { it.averageHeartRate * it.activeDuration } / totalWeight
            } else 0.0
            val weightedCadence = if (totalWeight > 0) {
                sorted.sumOf { it.averageCadence * it.activeDuration } / totalWeight
            } else 0.0

            return MergedRunSegment(
                id = "merged_${first.seq}_${sorted.last().seq}",
                wktStepIndex = first.wktStepIndex,
                intervalType = first.intervalType,
                subSegments = sorted,
                totalDistance = totalDist,
                totalTime = totalTime,
                averageSpeed = speed,
                averageHeartRate = weightedHr,
                averageCadence = weightedCadence,
                firstSegmentSeq = first.seq,
                isMerged = sorted.size > 1
            )
        }

        fun fromSingleSegment(segment: RunSegment): MergedRunSegment {
            return fromSegments(listOf(segment))
        }
    }
}

