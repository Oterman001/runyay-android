package com.oterman.rundemo.domain.model

enum class TrainWholeType(val value: String) {
    SELF_DEFINE("SELF_DEFINE"),
    DISTANCE("DISTANCE"),
    TIME("TIME"),
    CALORIES("CALORIES"),
    PACER("PACER");

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
    DISTANCE("DISTANCE"),
    TIME("TIME"),
    CALORIES("CALORIES"),
    PACER("PACER");

    companion object {
        fun fromValue(value: String?): TrainGoalType =
            entries.find { it.value == value } ?: DISTANCE
    }
}

enum class IntensityType(val value: String) {
    HEART_RATE("HEART_RATE"),
    SPEED("SPEED");

    companion object {
        fun fromValue(value: String?): IntensityType =
            entries.find { it.value == value } ?: HEART_RATE
    }
}

enum class LocationType(val value: String) {
    INDOOR("INDOOR"),
    OUTDOOR("OUTDOOR");

    companion object {
        fun fromValue(value: String?): LocationType =
            entries.find { it.value == value } ?: OUTDOOR
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
    val version: Int? = null
)
