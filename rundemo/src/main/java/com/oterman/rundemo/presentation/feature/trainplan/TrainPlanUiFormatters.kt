package com.oterman.rundemo.presentation.feature.trainplan

import com.oterman.rundemo.data.fit.VdotSpeedCalculator
import com.oterman.rundemo.domain.model.BlockType
import com.oterman.rundemo.domain.model.IntensityType
import com.oterman.rundemo.domain.model.LocationType
import com.oterman.rundemo.domain.model.TrainBlock
import com.oterman.rundemo.domain.model.TrainGoalType
import com.oterman.rundemo.domain.model.TrainStep
import com.oterman.rundemo.domain.model.TrainWholeType
import kotlin.math.roundToInt

private const val DEFAULT_PACE_SEC_PER_KM = 360.0
private const val KCAL_PER_KM = 70.0

internal data class TrainEstimate(
    val distanceMeters: Double?,
    val durationSeconds: Int?,
    val avgPaceSecPerKm: Int?,
    val isDistanceEstimated: Boolean = false,
    val isDurationEstimated: Boolean = false
)

internal fun effectivePaceSecPerKm(vdot: Double?): Double {
    if (vdot == null || vdot <= 0.0) return DEFAULT_PACE_SEC_PER_KM
    val range = VdotSpeedCalculator.getEasyPaceRange(vdot, 1000.0)
    val slow = range["slow"] ?: return DEFAULT_PACE_SEC_PER_KM
    val fast = range["fast"] ?: return DEFAULT_PACE_SEC_PER_KM
    return ((slow + fast) / 2.0) * 60.0
}

internal fun estimateSelfDefine(blocks: List<TrainBlock>, vdot: Double?): TrainEstimate {
    val fallbackPace = effectivePaceSecPerKm(vdot)
    var totalDistM = 0.0
    var totalDurSec = 0
    var anyEstimated = false

    for (block in blocks) {
        val loops = block.loopCnt.coerceAtLeast(1)
        for (step in block.stepList) {
            if (step.skipStatus == 1) continue
            val stepPace: Double = when {
                step.minPace != null && step.maxPace != null -> (step.minPace + step.maxPace) / 2.0
                step.minPace != null -> step.minPace.toDouble()
                step.maxPace != null -> step.maxPace.toDouble()
                else -> fallbackPace
            }
            when (step.goalType) {
                TrainGoalType.DISTANCE -> {
                    val distM = step.distanceMeters()
                    totalDistM += distM * loops
                    totalDurSec += ((distM / 1000.0) * stepPace).toInt() * loops
                    if (step.timeGoalSeconds == null) anyEstimated = true
                }
                TrainGoalType.TIME -> {
                    val durSec = step.timeGoalSeconds ?: 0
                    totalDurSec += durSec * loops
                    totalDistM += (durSec / stepPace) * 1000.0 * loops
                    anyEstimated = true
                }
                else -> {}
            }
        }
    }

    val avgPace = if (totalDistM > 0 && totalDurSec > 0)
        (totalDurSec / (totalDistM / 1000.0)).toInt()
    else null

    return TrainEstimate(
        distanceMeters = totalDistM.takeIf { it > 0 },
        durationSeconds = totalDurSec.takeIf { it > 0 },
        avgPaceSecPerKm = avgPace,
        isDistanceEstimated = anyEstimated,
        isDurationEstimated = anyEstimated
    )
}

internal fun estimateDistance(distanceMeters: Double, vdot: Double?): TrainEstimate {
    val pace = effectivePaceSecPerKm(vdot)
    val dur = ((distanceMeters / 1000.0) * pace).toInt()
    return TrainEstimate(
        distanceMeters = distanceMeters,
        durationSeconds = dur.takeIf { it > 0 },
        avgPaceSecPerKm = pace.toInt(),
        isDistanceEstimated = false,
        isDurationEstimated = true
    )
}

internal fun estimateTime(durationSeconds: Int, vdot: Double?): TrainEstimate {
    val pace = effectivePaceSecPerKm(vdot)
    val distM = (durationSeconds / pace) * 1000.0
    return TrainEstimate(
        distanceMeters = distM.takeIf { it > 0 },
        durationSeconds = durationSeconds,
        avgPaceSecPerKm = pace.toInt(),
        isDistanceEstimated = true,
        isDurationEstimated = false
    )
}

internal fun estimateCalories(calories: Int, vdot: Double?): TrainEstimate {
    if (calories <= 0) return TrainEstimate(null, null, null)
    val distKm = calories / KCAL_PER_KM
    val pace = effectivePaceSecPerKm(vdot)
    val dur = (distKm * pace).toInt()
    return TrainEstimate(
        distanceMeters = distKm * 1000.0,
        durationSeconds = dur.takeIf { it > 0 },
        avgPaceSecPerKm = pace.toInt(),
        isDistanceEstimated = true,
        isDurationEstimated = true
    )
}

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
    LocationType.PENDING -> "待定"
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

internal fun TrainStep.canRemoveLikeIos(): Boolean =
    warmupFlag != "Y" && cooldownFlag != "Y"

internal fun TrainStep.canMoveLikeIos(): Boolean =
    warmupFlag != "Y" && cooldownFlag != "Y"

internal fun TrainStep.goalText(): String = when (goalType) {
    TrainGoalType.DISTANCE -> formatDistance(distanceMeters())
    TrainGoalType.TIME -> formatDuration(timeGoalSeconds ?: 0)
    TrainGoalType.CALORIES -> "${caloriesValue ?: 0} kcal"
    TrainGoalType.PACER -> {
        val min = minPace?.let(::formatPace) ?: "--"
        val max = maxPace?.let(::formatPace) ?: "--"
        "$min - $max /km"
    }
    TrainGoalType.OPEN -> "自由训练"
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
