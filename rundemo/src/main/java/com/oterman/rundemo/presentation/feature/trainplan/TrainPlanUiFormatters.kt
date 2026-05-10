package com.oterman.rundemo.presentation.feature.trainplan

import com.oterman.rundemo.domain.model.BlockType
import com.oterman.rundemo.domain.model.IntensityType
import com.oterman.rundemo.domain.model.LocationType
import com.oterman.rundemo.domain.model.TrainBlock
import com.oterman.rundemo.domain.model.TrainGoalType
import com.oterman.rundemo.domain.model.TrainStep
import com.oterman.rundemo.domain.model.TrainWholeType
import kotlin.math.roundToInt

internal fun TrainWholeType.displayName(): String = when (this) {
    TrainWholeType.SELF_DEFINE -> "自定义"
    TrainWholeType.DISTANCE -> "距离"
    TrainWholeType.TIME -> "时间"
    TrainWholeType.CALORIES -> "卡路里"
    TrainWholeType.PACER -> "配速员"
}

internal fun LocationType.displayName(): String = when (this) {
    LocationType.OUTDOOR -> "室外"
    LocationType.INDOOR -> "室内"
}

internal fun BlockType.displayName(loopCnt: Int = 1): String = when {
    this == BlockType.WARMUP -> "热身"
    this == BlockType.COOLDOWN -> "放松"
    loopCnt > 1 -> "循环"
    else -> "训练"
}

internal fun TrainStep.displayName(): String {
    descName?.takeIf { it.isNotBlank() }?.let { return it }
    return when {
        warmupFlag == "Y" -> "热身"
        cooldownFlag == "Y" -> "放松"
        purpose.equals("RECOVERY", ignoreCase = true) -> "恢复"
        purpose.equals("WARMUP", ignoreCase = true) -> "热身"
        purpose.equals("COOLDOWN", ignoreCase = true) -> "放松"
        else -> "训练"
    }
}

internal fun TrainStep.goalText(): String = when (goalType) {
    TrainGoalType.DISTANCE -> formatDistance(distanceMeters())
    TrainGoalType.TIME -> formatDuration(timeGoalSeconds ?: 0)
    TrainGoalType.CALORIES -> "${caloriesValue ?: 0} kcal"
    TrainGoalType.PACER -> {
        val min = minPace?.let(::formatPace) ?: "--"
        val max = maxPace?.let(::formatPace) ?: "--"
        "$min - $max /km"
    }
}

internal fun TrainStep.intensityText(): String? = when (intensityType) {
    IntensityType.HEART_RATE -> {
        val zone = heartZoneType?.takeIf { it.isNotBlank() }
        val range = if (minHeartRate != null && maxHeartRate != null) {
            "$minHeartRate-$maxHeartRate bpm"
        } else null
        listOfNotNull(zone, range).joinToString(" ").takeIf { it.isNotBlank() }
    }
    IntensityType.SPEED -> {
        val min = minPace?.let(::formatPace)
        val max = maxPace?.let(::formatPace)
        if (min != null && max != null) "$min - $max /km" else null
    }
    null -> null
}

internal fun TrainStep.distanceMeters(): Double = when (goalType) {
    TrainGoalType.DISTANCE -> {
        val value = distanceValue ?: 0.0
        if (distanceUnit == "M") value else value * 1000.0
    }
    TrainGoalType.PACER -> distanceValue?.let { if (distanceUnit == "M") it else it * 1000.0 } ?: 0.0
    else -> 0.0
}

internal fun List<TrainBlock>.totalDistanceMeters(): Double =
    sumOf { block -> block.stepList.sumOf { it.distanceMeters() } * block.loopCnt.coerceAtLeast(1) }

internal fun List<TrainBlock>.totalDurationSeconds(): Int =
    sumOf { block -> block.stepList.sumOf { it.timeGoalSeconds ?: 0 } * block.loopCnt.coerceAtLeast(1) }

internal fun formatDistance(meters: Double): String {
    if (meters <= 0.0) return "0 km"
    return if (meters < 1000.0) {
        "${meters.roundToInt()} m"
    } else {
        val km = meters / 1000.0
        if (km == km.toLong().toDouble()) "${km.toLong()} km" else "%.2f km".format(km).trimEnd('0').trimEnd('.')
    }
}

internal fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "0分钟"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return buildString {
        if (h > 0) append("${h}小时")
        if (m > 0) append("${m}分钟")
        if (s > 0 || isEmpty()) append("${s}秒")
    }
}

internal fun formatPace(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "$min'${sec.toString().padStart(2, '0')}\""
}

internal fun formatPaceInput(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}

internal fun parsePaceInput(text: String): Int? {
    val normalized = text.trim().replace("'", ":").replace("\"", "")
    val parts = normalized.split(":")
    if (parts.size != 2) return null
    val min = parts[0].toIntOrNull() ?: return null
    val sec = parts[1].toIntOrNull() ?: return null
    return min * 60 + sec.coerceIn(0, 59)
}
