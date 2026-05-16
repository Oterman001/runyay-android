package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName
import com.oterman.rundemo.domain.model.*

// ==================== Save ====================

data class BatchSaveTrainPlanResponseData(
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("successCount") val successCount: Int = 0,
    @SerializedName("failedCount") val failedCount: Int = 0,
    @SerializedName("failedRecords") val failedRecords: List<TrainPlanFailedRecord>? = null
)

data class TrainPlanFailedRecord(
    @SerializedName("planId") val planId: String? = null,
    @SerializedName("errorCode") val errorCode: String? = null,
    @SerializedName("errorMessage") val errorMessage: String? = null
)

// ==================== Detail ====================

data class TrainPlanDetailResponseData(
    @SerializedName("planId") val planId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("trainWholeType") val trainWholeType: String? = null,
    @SerializedName("scheduledDate") val scheduledDate: String? = null,
    @SerializedName("hardLevel") val hardLevel: Int? = null,
    @SerializedName("finishFlag") val finishFlag: String? = null,
    @SerializedName("locationType") val locationType: String? = null,
    @SerializedName("templateId") val templateId: String? = null,
    @SerializedName("workoutId") val workoutId: String? = null,
    @SerializedName("planIdOfAW") val planIdOfAW: String? = null,
    @SerializedName("version") val version: Int? = null,
    @SerializedName("warmupBlock") val warmupBlock: TrainBlockResponseDto? = null,
    @SerializedName("blockList") val blockList: List<TrainBlockResponseDto>? = null,
    @SerializedName("cooldownBlock") val cooldownBlock: TrainBlockResponseDto? = null,
    @SerializedName("calGoalStep") val calGoalStep: TrainStepResponseDto? = null,
    @SerializedName("distanceGoalStep") val distanceGoalStep: TrainStepResponseDto? = null,
    @SerializedName("timeGoalStep") val timeGoalStep: TrainStepResponseDto? = null,
    @SerializedName("pacerGoalStep") val pacerGoalStep: TrainStepResponseDto? = null
)

data class TrainBlockResponseDto(
    @SerializedName("blockId") val blockId: String? = null,
    @SerializedName("blockType") val blockType: String? = null,
    @SerializedName("seq") val seq: Int? = null,
    @SerializedName("loopCnt") val loopCnt: Int? = null,
    @SerializedName("stepList") val stepList: List<TrainStepResponseDto>? = null
)

data class TrainStepResponseDto(
    @SerializedName("stepId") val stepId: String? = null,
    @SerializedName("seq") val seq: Int? = null,
    @SerializedName("descName") val descName: String? = null,
    @SerializedName("purpose") val purpose: String? = null,
    @SerializedName("warmupFlag") val warmupFlag: String? = null,
    @SerializedName("cooldownFlag") val cooldownFlag: String? = null,
    @SerializedName("skipStatus") val skipStatus: Int? = null,
    @SerializedName("goalType") val goalType: String? = null,
    @SerializedName("distanceUnit") val distanceUnit: String? = null,
    @SerializedName("distanceValue") val distanceValue: Double? = null,
    @SerializedName("timeGoalSeconds") val timeGoalSeconds: Int? = null,
    @SerializedName("caloriesUnit") val caloriesUnit: String? = null,
    @SerializedName("caloriesValue") val caloriesValue: Int? = null,
    @SerializedName("intensityType") val intensityType: String? = null,
    @SerializedName("minHeartRate") val minHeartRate: Int? = null,
    @SerializedName("maxHeartRate") val maxHeartRate: Int? = null,
    @SerializedName("heartZoneType") val heartZoneType: String? = null,
    @SerializedName("minPace") val minPace: Int? = null,
    @SerializedName("maxPace") val maxPace: Int? = null,
    @SerializedName("speedZoneType") val speedZoneType: String? = null
)

data class TrainPlanDetailWrapperResponseData(
    @SerializedName("TrainPlanDetailResponseDto")
    val trainPlanDetailResponseDto: List<TrainPlanDetailResponseData>? = null
)

// ==================== List ====================

data class TrainPlanListWrapperResponseData(
    @SerializedName("TrainPlanListResult") val trainPlanListResult: List<TrainPlanListResponseData>? = null
)

data class TrainPlanListResponseData(
    @SerializedName("records") val records: List<TrainPlanSummaryDto>? = null,
    @SerializedName("total") val total: Long = 0,
    @SerializedName("pageNum") val pageNum: Int = 1,
    @SerializedName("pageSize") val pageSize: Int = 50
)

data class TrainPlanSummaryDto(
    @SerializedName("planId") val planId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("trainWholeType") val trainWholeType: String? = null,
    @SerializedName("scheduledDate") val scheduledDate: String? = null,
    @SerializedName("hardLevel") val hardLevel: Int? = null,
    @SerializedName("finishFlag") val finishFlag: String? = null,
    @SerializedName("locationType") val locationType: String? = null,
    @SerializedName("workoutId") val workoutId: String? = null,
    @SerializedName("version") val version: Int? = null
)

// ==================== Delete ====================

data class DeleteTrainPlanResponseData(
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("successCount") val successCount: Int = 0,
    @SerializedName("failedCount") val failedCount: Int = 0,
    @SerializedName("failedRecords") val failedRecords: List<TrainPlanFailedRecord>? = null
)

// ==================== Push ====================

data class PushTrainPlanResponseData(
    @SerializedName("planId") val planId: String? = null,
    @SerializedName("platformCode") val platformCode: String? = null,
    @SerializedName("extWorkoutId") val extWorkoutId: String? = null,
    @SerializedName("pushStatus") val pushStatus: String? = null
)

// ==================== Mapping Extensions ====================

fun TrainStepResponseDto.toDomain(): TrainStep = TrainStep(
    stepId = stepId,
    seq = seq ?: 0,
    descName = descName,
    purpose = purpose,
    warmupFlag = warmupFlag,
    cooldownFlag = cooldownFlag,
    skipStatus = skipStatus ?: 0,
    goalType = TrainGoalType.fromValue(goalType),
    distanceUnit = distanceUnit?.uppercase(),
    distanceValue = distanceValue,
    timeGoalSeconds = timeGoalSeconds,
    caloriesUnit = caloriesUnit,
    caloriesValue = caloriesValue,
    intensityType = intensityType?.let { IntensityType.fromValue(it) },
    minHeartRate = minHeartRate,
    maxHeartRate = maxHeartRate,
    heartZoneType = heartZoneType,
    minPace = minPace,
    maxPace = maxPace,
    speedZoneType = speedZoneType
)

fun TrainBlockResponseDto.toDomain(): TrainBlock = TrainBlock(
    blockId = blockId,
    blockType = BlockType.fromValue(blockType),
    seq = seq ?: 0,
    loopCnt = loopCnt ?: 1,
    stepList = stepList?.map { it.toDomain() } ?: emptyList()
)

fun TrainPlanDetailResponseData.toDomain(): TrainPlan = TrainPlan(
    planId = planId,
    name = name ?: "",
    description = description,
    trainWholeType = TrainWholeType.fromValue(trainWholeType),
    scheduledDate = scheduledDate,
    hardLevel = hardLevel,
    finishFlag = finishFlag,
    locationType = LocationType.fromValue(locationType),
    templateId = templateId,
    workoutId = workoutId,
    planIdOfAW = planIdOfAW,
    version = version,
    warmupBlock = warmupBlock?.toDomain(),
    blockList = blockList?.map { it.toDomain() } ?: emptyList(),
    cooldownBlock = cooldownBlock?.toDomain(),
    calGoalStep = calGoalStep?.toDomain(),
    distanceGoalStep = distanceGoalStep?.toDomain(),
    timeGoalStep = timeGoalStep?.toDomain(),
    pacerGoalStep = pacerGoalStep?.toDomain()
)

fun TrainPlanSummaryDto.toDomain(): TrainPlanSummary = TrainPlanSummary(
    planId = planId ?: "",
    name = name ?: "",
    description = description,
    trainWholeType = TrainWholeType.fromValue(trainWholeType),
    scheduledDate = scheduledDate,
    hardLevel = hardLevel,
    finishFlag = finishFlag,
    locationType = locationType,
    workoutId = workoutId,
    version = version
)
