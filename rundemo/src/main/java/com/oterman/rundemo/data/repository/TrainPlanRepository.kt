package com.oterman.rundemo.data.repository

import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.RequestBuilder
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.network.api.TrainPlanApi
import com.oterman.rundemo.data.network.dto.request.DeleteTrainPlanRequestDto
import com.oterman.rundemo.data.network.dto.request.PushTrainPlanRequestDto
import com.oterman.rundemo.data.network.dto.request.SaveTrainPlanRequestDto
import com.oterman.rundemo.data.network.dto.request.TrainBlockDto
import com.oterman.rundemo.data.network.dto.request.TrainPlanDetailRequestDto
import com.oterman.rundemo.data.network.dto.request.TrainPlanListRequestDto
import com.oterman.rundemo.data.network.dto.request.TrainStepDto
import com.oterman.rundemo.data.network.dto.response.PushTrainPlanResponseData
import com.oterman.rundemo.data.network.dto.response.TrainPlanListResponseData
import com.oterman.rundemo.data.network.dto.response.toDomain
import com.oterman.rundemo.domain.model.TrainBlock
import com.oterman.rundemo.domain.model.TrainPlan
import com.oterman.rundemo.domain.model.TrainPlanSummary
import com.oterman.rundemo.domain.model.TrainStep
import com.oterman.rundemo.util.RLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TrainPlanRepository(
    private val preferencesManager: PreferencesManager,
    private val api: TrainPlanApi = RetrofitClient.trainPlanApi
) {
    companion object {
        private const val TAG = "TrainPlanRepo"
        private val SERVER_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
    }

    suspend fun savePlan(plan: TrainPlan): Result<Unit> {
        return try {
            val dto = plan.toSaveDto()
            val request = RequestBuilder.createRequest(
                dtoName = "SaveTrainPlanRequestDto",
                data = dto,
                preferencesManager = preferencesManager
            )
            val response = api.saveTrainPlans(request)
            if (response.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "savePlan failed", e)
            Result.failure(e)
        }
    }

    suspend fun getPlanDetail(planId: String): Result<TrainPlan> {
        return try {
            val request = RequestBuilder.createRequest(
                dtoName = "TrainPlanDetailRequestDto",
                data = TrainPlanDetailRequestDto(planId = planId),
                preferencesManager = preferencesManager
            )
            val response = api.getTrainPlanDetail(request)
            if (response.isSuccess() && response.data != null) {
                Result.success(response.data.toDomain())
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "getPlanDetail failed", e)
            Result.failure(e)
        }
    }

    suspend fun listPlans(
        startDate: String? = null,
        endDate: String? = null,
        finishFlag: String? = null,
        pageNum: Int = 1,
        pageSize: Int = 50
    ): Result<TrainPlanListResponseData> {
        return try {
            val request = RequestBuilder.createRequest(
                dtoName = "TrainPlanListRequestDto",
                data = TrainPlanListRequestDto(
                    startDate = startDate,
                    endDate = endDate,
                    finishFlag = finishFlag,
                    pageNum = pageNum,
                    pageSize = pageSize
                ),
                preferencesManager = preferencesManager
            )
            val response = api.listTrainPlans(request)
            if (response.isSuccess() && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "listPlans failed", e)
            Result.failure(e)
        }
    }

    suspend fun listPlanSummaries(
        startDate: LocalDate,
        endDate: LocalDate,
        pageSize: Int = 500
    ): Result<List<TrainPlanSummary>> {
        return listPlans(
            startDate = startDate.format(SERVER_DATE_FORMATTER),
            endDate = endDate.format(SERVER_DATE_FORMATTER),
            pageSize = pageSize
        ).map { data ->
            data.records?.map { it.toDomain() } ?: emptyList()
        }
    }

    suspend fun deletePlans(planIds: List<String>): Result<Unit> {
        return try {
            val request = RequestBuilder.createRequest(
                dtoName = "DeleteTrainPlanRequestDto",
                data = DeleteTrainPlanRequestDto(planIds = planIds),
                preferencesManager = preferencesManager
            )
            val response = api.deleteTrainPlans(request)
            if (response.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "deletePlans failed", e)
            Result.failure(e)
        }
    }

    suspend fun pushPlan(planId: String, platformCode: String): Result<PushTrainPlanResponseData> {
        return try {
            val request = RequestBuilder.createRequest(
                dtoName = "PushTrainPlanRequestDto",
                data = PushTrainPlanRequestDto(planId = planId, platformCode = platformCode),
                preferencesManager = preferencesManager
            )
            val response = api.pushTrainPlan(request)
            if (response.isSuccess() && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "pushPlan failed", e)
            Result.failure(e)
        }
    }

    suspend fun deletePushedPlan(planId: String, platformCode: String): Result<Unit> {
        return try {
            val request = RequestBuilder.createRequest(
                dtoName = "PushTrainPlanRequestDto",
                data = PushTrainPlanRequestDto(planId = planId, platformCode = platformCode),
                preferencesManager = preferencesManager
            )
            val response = api.deletePushedTrainPlan(request)
            if (response.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "deletePushedPlan failed", e)
            Result.failure(e)
        }
    }

    // ==================== Domain → DTO Mapping ====================

    private fun TrainPlan.toSaveDto() = SaveTrainPlanRequestDto(
        planId = planId,
        name = name,
        description = description,
        trainWholeType = trainWholeType.value,
        scheduledDate = scheduledDate?.let { d ->
            runCatching {
                LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE).format(SERVER_DATE_FORMATTER)
            }.getOrElse { d }
        },
        hardLevel = hardLevel,
        finishFlag = finishFlag,
        locationType = locationType.value,
        templateId = templateId,
        workoutId = workoutId,
        planIdOfAW = planIdOfAW,
        warmupBlock = warmupBlock?.toDto(),
        blockList = blockList.map { it.toDto() }.takeIf { it.isNotEmpty() },
        cooldownBlock = cooldownBlock?.toDto(),
        calGoalStep = calGoalStep?.toDto(),
        distanceGoalStep = distanceGoalStep?.toDto(),
        timeGoalStep = timeGoalStep?.toDto(),
        pacerGoalStep = pacerGoalStep?.toDto()
    )

    private fun TrainBlock.toDto() = TrainBlockDto(
        blockId = blockId,
        blockType = blockType.value,
        seq = seq,
        loopCnt = loopCnt,
        stepList = stepList.map { it.toDto() }.takeIf { it.isNotEmpty() }
    )

    private fun TrainStep.toDto() = TrainStepDto(
        stepId = stepId,
        seq = seq,
        descName = descName,
        purpose = purpose,
        warmupFlag = warmupFlag,
        cooldownFlag = cooldownFlag,
        skipStatus = skipStatus,
        goalType = goalType.value,
        distanceUnit = distanceUnit,
        distanceValue = distanceValue,
        timeGoalSeconds = timeGoalSeconds,
        caloriesUnit = caloriesUnit,
        caloriesValue = caloriesValue,
        intensityType = intensityType?.value,
        minHeartRate = minHeartRate,
        maxHeartRate = maxHeartRate,
        heartZoneType = heartZoneType,
        minPace = minPace,
        maxPace = maxPace,
        speedZoneType = speedZoneType
    )
}
