package com.oterman.rundemo.domain.model

enum class TrainWholeType(val value: String) {
    SELF_DEFINE("selfDefine"),
    DISTANCE("distance"),
    TIME("time"),
    CALORIES("calories"),
    PACER("pacer");

    companion object {
        fun fromValue(value: String?): TrainWholeType =
            entries.find { it.value == value } ?: SELF_DEFINE
    }
}

enum class BlockType(val value: String) {
    WARMUP("WARMUP"),
    MAIN("MAIN"),
    COOLDOWN("COOLDOWN");

    companion object {
        fun fromValue(value: String?): BlockType =
            entries.find { it.value == value } ?: MAIN
    }
}

enum class TrainGoalType(val value: String) {
    DISTANCE("distance"),
    TIME("time"),
    CALORIES("calories"),
    PACER("pacer"),
    OPEN("open");

    companion object {
        fun fromValue(value: String?): TrainGoalType =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: DISTANCE
    }
}

enum class IntensityType(val value: String) {
    HEART_RATE("heartRate"),
    SPEED("speed"),
    NONE("none");

    companion object {
        fun fromValue(value: String?): IntensityType =
            entries.find { it.value == value } ?: NONE
    }
}

enum class LocationType(val value: String) {
    INDOOR("indoor"),
    OUTDOOR("outdoor"),
    PENDING("unknown");

    companion object {
        fun fromValue(value: String?): LocationType =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: OUTDOOR
    }
}

data class TrainPlan(
    val planId: String? = null,
    val name: String = "",
    val description: String? = null,
    val trainWholeType: TrainWholeType = TrainWholeType.SELF_DEFINE,
    val scheduledDate: String? = null,
    val hardLevel: Int? = null,
    val finishFlag: String? = "N",
    val locationType: LocationType = LocationType.OUTDOOR,
    val templateId: String? = null,
    val workoutId: String? = null,
    val planIdOfAW: String? = null,
    val sentPlatformCodes: Set<String> = emptySet(),
    val sentPlatformExtWorkoutIds: Map<String, String> = emptyMap(),
    val version: Int? = null,
    val warmupBlock: TrainBlock? = null,
    val blockList: List<TrainBlock> = emptyList(),
    val cooldownBlock: TrainBlock? = null,
    val calGoalStep: TrainStep? = null,
    val distanceGoalStep: TrainStep? = null,
    val timeGoalStep: TrainStep? = null,
    val pacerGoalStep: TrainStep? = null
)

data class TrainBlock(
    val blockId: String? = null,
    val blockType: BlockType = BlockType.MAIN,
    val seq: Int = 0,
    val loopCnt: Int = 1,
    val stepList: List<TrainStep> = emptyList()
)

data class TrainStep(
    val stepId: String? = null,
    val seq: Int = 0,
    val descName: String? = null,
    val purpose: String? = null,
    val warmupFlag: String? = "N",
    val cooldownFlag: String? = "N",
    val skipStatus: Int = 0,
    val goalType: TrainGoalType = TrainGoalType.DISTANCE,
    val distanceUnit: String? = "KM",
    val distanceValue: Double? = null,
    val timeGoalSeconds: Int? = null,
    val caloriesUnit: String? = "KCAL",
    val caloriesValue: Int? = null,
    val intensityType: IntensityType? = null,
    val minHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val heartZoneType: String? = null,
    val minPace: Int? = null,
    val maxPace: Int? = null,
    val speedZoneType: String? = null
)

data class TrainPlanSummary(
    val planId: String,
    val name: String,
    val description: String? = null,
    val trainWholeType: TrainWholeType = TrainWholeType.SELF_DEFINE,
    val scheduledDate: String? = null,
    val hardLevel: Int? = null,
    val finishFlag: String? = "N",
    val locationType: String? = null,
    val workoutId: String? = null,
    val sentPlatformCodes: Set<String> = emptySet(),
    val sentPlatformExtWorkoutIds: Map<String, String> = emptyMap(),
    val version: Int? = null
)

fun TrainPlan.sentDevicePlatforms(): List<DataSourcePlatform> =
    sentDevicePlatforms(sentPlatformCodes)

fun TrainPlanSummary.sentDevicePlatforms(): List<DataSourcePlatform> =
    sentDevicePlatforms(sentPlatformCodes)

private fun sentDevicePlatforms(rawCodes: Set<String>): List<DataSourcePlatform> {
    val order = listOf(DataSourcePlatform.GARMIN_GLOBAL, DataSourcePlatform.COROS)
    return order.filter { rawCodes.contains(it.code) }
}

fun TrainPlan.markSent(platformCode: String, extWorkoutId: String? = null): TrainPlan {
    val updatedExtIds = if (extWorkoutId.isNullOrBlank()) {
        sentPlatformExtWorkoutIds
    } else {
        sentPlatformExtWorkoutIds + (platformCode to extWorkoutId)
    }
    return copy(
        sentPlatformCodes = sentPlatformCodes + platformCode,
        sentPlatformExtWorkoutIds = updatedExtIds
    )
}

fun TrainPlan.removeSent(platformCode: String): TrainPlan =
    copy(
        sentPlatformCodes = sentPlatformCodes - platformCode,
        sentPlatformExtWorkoutIds = sentPlatformExtWorkoutIds - platformCode
    )
