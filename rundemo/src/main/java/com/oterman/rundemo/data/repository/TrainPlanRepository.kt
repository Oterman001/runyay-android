package com.oterman.rundemo.data.repository

import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.dao.TrainPlanDao
import com.oterman.rundemo.data.local.entity.toDetailDomain
import com.oterman.rundemo.data.local.entity.toEntity
import com.oterman.rundemo.data.local.entity.toSummaryDomain
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
    private val api: TrainPlanApi = RetrofitClient.trainPlanApi,
    private val localDao: TrainPlanDao? = null
) {
    companion object {
        private const val TAG = "TrainPlanRepo"
        private val SERVER_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
        private val ISO_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    // ==================== 对外接口 ====================

    /**
     * 保存训练计划。
     * 策略：本地优先；先写本地 DB（isDirty=true），再后台同步服务端；同步成功则更新 isDirty=false。
     */
    suspend fun savePlan(plan: TrainPlan): Result<Unit> {
        plan.planId ?: return Result.failure(Exception("planId 不能为空"))
        val userId = preferencesManager.getUserId()
            ?: return Result.failure(Exception("用户未登录"))

        // 1. 先写本地，保证数据安全
        try {
            val entity = plan.toEntity(userId, lastSyncAt = 0L, isDirty = true)
            localDao?.upsert(entity)
        } catch (e: Exception) {
            RLog.e(TAG, "savePlan local write failed", e)
            return Result.failure(Exception("本地保存失败：${e.message}"))
        }

        // 2. 后台同步服务端（失败不影响主流程）
        try {
            val dto = plan.toSaveDto()
            val request = RequestBuilder.createRequest(
                dtoName = "SaveTrainPlanRequestDto",
                data = dto,
                preferencesManager = preferencesManager
            )
            val response = api.saveTrainPlans(request)
            if (response.isSuccess()) {
                cacheDetail(plan)
                RLog.d(TAG, "savePlan synced to server: ${plan.planId}")
            } else {
                RLog.w(TAG, "savePlan server rejected, kept dirty: ${response.msg}")
            }
        } catch (e: Exception) {
            RLog.w(TAG, "savePlan network failed, kept dirty: ${plan.planId}")
        }

        return Result.success(Unit)
    }

    /**
     * 查询训练计划详情。
     * 策略：
     *  1. dirty 计划（本地未同步）：直接返回本地，不请求网络（本地是最新版本）
     *  2. 干净缓存：返回缓存并后台静默刷新
     *  3. 无缓存：请求网络，成功后写入本地 DB
     *  4. 网络失败时：尝试返回旧缓存（兜底）
     */
    suspend fun getPlanDetail(planId: String): Result<TrainPlan> {
        val cached = localDao?.getById(planId)
        val cachedDetail = cached?.toDetailDomain()

        if (cachedDetail != null && cached?.isDirty == true) {
            RLog.d(TAG, "getPlanDetail: returning dirty local plan $planId")
            return Result.success(cachedDetail)
        }

        if (cachedDetail != null && cached?.isDirty == false) {
            RLog.d(TAG, "getPlanDetail from cache: $planId v${cached.version}")
            refreshDetailAsync(planId, cached.version)
            return Result.success(cachedDetail)
        }

        return fetchAndCacheDetail(planId)
    }

    /**
     * 查询指定月份内训练计划摘要列表。
     * 策略：
     *  1. 请求网络，成功后 upsert 到本地 DB，返回网络数据
     *  2. 网络失败时，从本地 DB 读取并返回（兜底）
     */
    suspend fun listPlanSummaries(
        startDate: LocalDate,
        endDate: LocalDate,
        pageSize: Int = 500
    ): Result<List<TrainPlanSummary>> {
        val startStr = startDate.format(SERVER_DATE_FORMATTER)
        val endStr = endDate.format(SERVER_DATE_FORMATTER)

        val networkResult = listPlans(startDate = startStr, endDate = endStr, pageSize = pageSize)
            .map { data -> data.records?.map { it.toDomain() } ?: emptyList() }

        networkResult.onSuccess { summaries ->
            cacheSummaries(summaries, startDate, endDate)
            return Result.success(mergeDirtyPlans(summaries, startDate, endDate))
        }

        // 网络失败：尝试本地兜底
        val userId = preferencesManager.getUserId()
        if (userId != null && localDao != null) {
            val localStartStr = startDate.format(ISO_DATE_FORMATTER)
            val localEndStr = endDate.format(ISO_DATE_FORMATTER)
            val cached = localDao.getByDateRange(userId, localStartStr, localEndStr)
            if (cached.isNotEmpty()) {
                RLog.w(TAG, "listPlanSummaries: network failed, using ${cached.size} cached items")
                return Result.success(cached.map { it.toSummaryDomain() })
            }
        }

        return networkResult
    }

    /**
     * 删除训练计划（批量）。
     * 策略：先从本地 DB 删除（乐观删除），再请求网络。
     */
    suspend fun deletePlans(planIds: List<String>): Result<Unit> {
        // 乐观删除：先从本地移除
        localDao?.deleteByIds(planIds)

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

    // ==================== 内部：网络请求 ====================

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
            val listData = response.data?.trainPlanListResult?.firstOrNull()
            if (response.isSuccess() && listData != null) {
                Result.success(listData)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "listPlans failed", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchAndCacheDetail(planId: String): Result<TrainPlan> {
        return try {
            val request = RequestBuilder.createRequest(
                dtoName = "TrainPlanDetailRequestDto",
                data = TrainPlanDetailRequestDto(planId = planId),
                preferencesManager = preferencesManager
            )
            val response = api.getTrainPlanDetail(request)
            if (response.isSuccess() && response.data != null) {
                val plan = response.data.toDomain()
                cacheDetail(plan)
                Result.success(plan)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "fetchAndCacheDetail failed for $planId", e)
            // 网络失败时尝试返回旧缓存（即使 isDirty 或版本过期）
            val stale = localDao?.getById(planId)?.toDetailDomain()
            if (stale != null) Result.success(stale) else Result.failure(e)
        }
    }

    /** 后台静默刷新详情（仅当网络成功且版本更新时才写 DB） */
    private suspend fun refreshDetailAsync(planId: String, cachedVersion: Int?) {
        try {
            val request = RequestBuilder.createRequest(
                dtoName = "TrainPlanDetailRequestDto",
                data = TrainPlanDetailRequestDto(planId = planId),
                preferencesManager = preferencesManager
            )
            val response = api.getTrainPlanDetail(request)
            if (response.isSuccess() && response.data != null) {
                val plan = response.data.toDomain()
                if (plan.version != null && plan.version != cachedVersion) {
                    cacheDetail(plan)
                    RLog.d(TAG, "refreshDetail: updated $planId v$cachedVersion → v${plan.version}")
                }
            }
        } catch (e: Exception) {
            RLog.d(TAG, "refreshDetailAsync silently failed for $planId: ${e.message}")
        }
    }

    // ==================== 内部：本地缓存操作 ====================

    private suspend fun cacheDetail(plan: TrainPlan) {
        val userId = preferencesManager.getUserId() ?: return
        val planId = plan.planId ?: return
        try {
            val entity = plan.toEntity(userId, lastSyncAt = System.currentTimeMillis(), isDirty = false)
            localDao?.upsert(entity)
            RLog.d(TAG, "cacheDetail: saved $planId to DB")
        } catch (e: Exception) {
            RLog.w(TAG, "cacheDetail failed for $planId: ${e.message}")
        }
    }

    private suspend fun saveLocalDirty(plan: TrainPlan) {
        val userId = preferencesManager.getUserId() ?: return
        val planId = plan.planId ?: return
        try {
            val entity = plan.toEntity(userId, lastSyncAt = 0L, isDirty = true)
            localDao?.upsert(entity)
            RLog.d(TAG, "saveLocalDirty: $planId saved offline")
        } catch (e: Exception) {
            RLog.w(TAG, "saveLocalDirty failed for $planId: ${e.message}")
        }
    }

    /** 将服务端未收录的本地 dirty 计划追加到列表末尾 */
    private suspend fun mergeDirtyPlans(
        summaries: List<TrainPlanSummary>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TrainPlanSummary> {
        val userId = preferencesManager.getUserId() ?: return summaries
        val dao = localDao ?: return summaries
        val localStartStr = startDate.format(ISO_DATE_FORMATTER)
        val localEndStr = endDate.format(ISO_DATE_FORMATTER)
        val serverIds = summaries.map { it.planId }.toSet()
        val dirtyOnly = dao.getByDateRange(userId, localStartStr, localEndStr)
            .filter { it.isDirty && it.planId !in serverIds }
            .map { it.toSummaryDomain() }
        return if (dirtyOnly.isEmpty()) summaries else summaries + dirtyOnly
    }

    /**
     * 将网络返回的摘要列表 upsert 到 DB。
     * 若本地 detailJson 的 version 仍与服务端一致，保留旧 detailJson；
     * 若版本有变化，清空 detailJson 强制下次重新拉取。
     */
    private suspend fun cacheSummaries(
        summaries: List<TrainPlanSummary>,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        val userId = preferencesManager.getUserId() ?: return
        val dao = localDao ?: return
        try {
            val localStartStr = startDate.format(ISO_DATE_FORMATTER)
            val localEndStr = endDate.format(ISO_DATE_FORMATTER)
            val existingMap = dao.getByDateRange(userId, localStartStr, localEndStr)
                .associateBy { it.planId }

            val now = System.currentTimeMillis()
            val newIds = summaries.map { it.planId }.toSet()

            // 删除本地有但服务端已不存在的计划（isDirty=true 的跳过，保留未同步数据）
            val toDelete = existingMap.values
                .filter { it.planId !in newIds && !it.isDirty }
                .map { it.planId }
            if (toDelete.isNotEmpty()) dao.deleteByIds(toDelete)

            val entities = summaries.map { summary ->
                val existing = existingMap[summary.planId]
                val serverVersion = summary.version
                val localVersion = existing?.version

                // 若服务端 version 与本地一致，保留已缓存的 detailJson；否则清空强制重拉
                val detailJson = if (existing != null && serverVersion != null
                    && serverVersion == localVersion && existing.detailJson != null
                ) {
                    existing.detailJson
                } else {
                    null
                }

                summary.toEntity(
                    userId = userId,
                    existingDetailJson = detailJson,
                    lastSyncAt = now
                )
            }
            dao.upsertAll(entities)
            RLog.d(TAG, "cacheSummaries: upserted ${entities.size} plans")
        } catch (e: Exception) {
            RLog.w(TAG, "cacheSummaries failed: ${e.message}")
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
