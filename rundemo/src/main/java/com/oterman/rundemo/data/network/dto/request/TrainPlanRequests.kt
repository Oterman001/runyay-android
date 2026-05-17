package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

data class SaveTrainPlanRequestDto(
    @SerializedName("planId") val planId: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("trainWholeType") val trainWholeType: String? = null,
    @SerializedName("scheduledDate") val scheduledDate: String? = null,
    @SerializedName("hardLevel") val hardLevel: Int? = null,
    @SerializedName("finishFlag") val finishFlag: String? = "N",
    @SerializedName("locationType") val locationType: String? = null,
    @SerializedName("templateId") val templateId: String? = null,
    @SerializedName("workoutId") val workoutId: String? = null,
    @SerializedName("planIdOfAW") val planIdOfAW: String? = null,
    @SerializedName("warmupBlock") val warmupBlock: TrainBlockDto? = null,
    @SerializedName("blockList") val blockList: List<TrainBlockDto>? = null,
    @SerializedName("cooldownBlock") val cooldownBlock: TrainBlockDto? = null,
    @SerializedName("calGoalStep") val calGoalStep: TrainStepDto? = null,
    @SerializedName("distanceGoalStep") val distanceGoalStep: TrainStepDto? = null,
    @SerializedName("timeGoalStep") val timeGoalStep: TrainStepDto? = null,
    @SerializedName("pacerGoalStep") val pacerGoalStep: TrainStepDto? = null
)

data class TrainBlockDto(
    @SerializedName("blockId") val blockId: String? = null,
    @SerializedName("blockType") val blockType: String? = null,
    @SerializedName("seq") val seq: Int? = null,
    @SerializedName("loopCnt") val loopCnt: Int? = null,
    @SerializedName("stepList") val stepList: List<TrainStepDto>? = null
)

data class TrainStepDto(
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

data class TrainPlanDetailRequestDto(
    @SerializedName("planId") val planId: String
)

data class TrainPlanListRequestDto(
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("finishFlag") val finishFlag: String? = null,
    @SerializedName("pageNum") val pageNum: Int = 1,
    @SerializedName("pageSize") val pageSize: Int = 50
)

data class DeleteTrainPlanRequestDto(
    @SerializedName("planIds") val planIds: List<String>
)

data class PushTrainPlanRequestDto(
    @SerializedName("planId") val planId: String,
    @SerializedName("platformCode") val platformCode: String
)

data class ClearPushedPlansRequestDto(
    @SerializedName("platformCode") val platformCode: String
)
