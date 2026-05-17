package com.oterman.rundemo.data.local.entity

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oterman.rundemo.data.network.dto.response.TrainPlanDetailResponseData
import com.oterman.rundemo.data.network.dto.response.toDomain
import com.oterman.rundemo.domain.model.TrainPlan
import com.oterman.rundemo.domain.model.TrainPlanSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE   // yyyy-MM-dd
private val BASIC_ISO: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE  // yyyyMMdd

/**
 * 将日期字符串规范化为 yyyy-MM-dd，Entity 统一存此格式以保证 SQL BETWEEN 比较正确。
 */
private fun String.normalizeToIsoDate(): String? {
    return runCatching {
        if (contains("-")) {
            // 已经是 yyyy-MM-dd，验证合法性后原样返回
            LocalDate.parse(this, ISO_DATE).format(ISO_DATE)
        } else {
            // yyyyMMdd → yyyy-MM-dd
            LocalDate.parse(this, BASIC_ISO).format(ISO_DATE)
        }
    }.getOrNull()
}

// ==================== TrainPlanSummary → TrainPlanEntity ====================

/**
 * 从摘要构建 Entity，detailJson 保留原值（调用方按需传入已有 Entity 的 detailJson 及版本信息）。
 */
fun TrainPlanSummary.toEntity(
    userId: String,
    sentPlatformCodes: Set<String> = this.sentPlatformCodes,
    sentPlatformExtWorkoutIds: Map<String, String> = this.sentPlatformExtWorkoutIds,
    existingDetailJson: String? = null,
    lastSyncAt: Long = System.currentTimeMillis(),
    isDirty: Boolean = false
): TrainPlanEntity = TrainPlanEntity(
    planId = planId,
    userId = userId,
    name = name,
    description = description,
    trainWholeType = trainWholeType.value,
    scheduledDate = scheduledDate?.normalizeToIsoDate(),
    hardLevel = hardLevel,
    finishFlag = finishFlag,
    locationType = locationType,
    workoutId = workoutId,
    sourceType = sourceType,
    sourceName = sourceName,
    sentPlatformCodes = sentPlatformCodes.toStorageString(),
    sentPlatformExtWorkoutIds = sentPlatformExtWorkoutIds.toJsonStorage(),
    version = version,
    detailJson = existingDetailJson,
    lastSyncAt = lastSyncAt,
    isDirty = isDirty
)

// ==================== TrainPlan (detail) → TrainPlanEntity ====================

private val gson = Gson()
private val stringMapType = object : TypeToken<Map<String, String>>() {}.type

private fun Set<String>.toStorageString(): String? =
    takeIf { it.isNotEmpty() }?.sorted()?.joinToString(",")

private fun String?.toPlatformCodeSet(): Set<String> =
    this?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        ?: emptySet()

private fun Map<String, String>.toJsonStorage(): String? =
    takeIf { it.isNotEmpty() }?.let { gson.toJson(it) }

private fun String?.toStringMap(): Map<String, String> =
    runCatching {
        if (isNullOrBlank()) emptyMap() else gson.fromJson<Map<String, String>>(this, stringMapType)
    }.getOrDefault(emptyMap())

/**
 * 将完整 TrainPlan 序列化为 JSON。
 * 序列化目标为 TrainPlanDetailResponseData（纯字符串/数值 DTO），
 * 避免 Kotlin enum 序列化歧义，且反序列化时可直接走现有的 toDomain() 映射逻辑。
 */
fun TrainPlan.toDetailJson(): String {
    val dto = toDetailResponseDto()
    return gson.toJson(dto)
}

/**
 * 从完整 TrainPlan 构建 Entity（同时写入摘要字段 + detailJson）。
 */
fun TrainPlan.toEntity(
    userId: String,
    lastSyncAt: Long = System.currentTimeMillis(),
    isDirty: Boolean = false
): TrainPlanEntity = TrainPlanEntity(
    planId = planId ?: error("TrainPlan.planId must not be null when saving to DB"),
    userId = userId,
    name = name,
    description = description,
    trainWholeType = trainWholeType.value,
    scheduledDate = scheduledDate?.normalizeToIsoDate(),
    hardLevel = hardLevel,
    finishFlag = finishFlag,
    locationType = locationType.value,
    workoutId = workoutId,
    sourceType = sourceType,
    sourceName = sourceName,
    sentPlatformCodes = sentPlatformCodes.toStorageString(),
    sentPlatformExtWorkoutIds = sentPlatformExtWorkoutIds.toJsonStorage(),
    version = version,
    detailJson = toDetailJson(),
    lastSyncAt = lastSyncAt,
    isDirty = isDirty
)

// ==================== TrainPlanEntity → Domain ====================

/**
 * 将 Entity 转换为摘要 Domain 对象（不依赖 detailJson）。
 */
fun TrainPlanEntity.toSummaryDomain(): TrainPlanSummary = TrainPlanSummary(
    planId = planId,
    name = name,
    description = description,
    trainWholeType = com.oterman.rundemo.domain.model.TrainWholeType.fromValue(trainWholeType),
    scheduledDate = scheduledDate,
    hardLevel = hardLevel,
    finishFlag = finishFlag,
    locationType = locationType,
    workoutId = workoutId,
    sourceType = sourceType,
    sourceName = sourceName,
    sentPlatformCodes = sentPlatformCodes.toPlatformCodeSet(),
    sentPlatformExtWorkoutIds = sentPlatformExtWorkoutIds.toStringMap(),
    version = version
)

/**
 * 将 Entity 的 detailJson 反序列化为完整 TrainPlan。
 * 若 detailJson 为 null 或解析失败则返回 null。
 */
fun TrainPlanEntity.toDetailDomain(): TrainPlan? {
    val json = detailJson ?: return null
    return runCatching {
        val dto = gson.fromJson(json, TrainPlanDetailResponseData::class.java)
        dto.toDomain().copy(
            sourceType = sourceType,
            sourceName = sourceName,
            sentPlatformCodes = sentPlatformCodes.toPlatformCodeSet(),
            sentPlatformExtWorkoutIds = sentPlatformExtWorkoutIds.toStringMap()
        )
    }.getOrNull()
}

// ==================== TrainPlan → TrainPlanDetailResponseData ====================

/**
 * 将 domain TrainPlan 逆向映射为 DTO，用于 JSON 序列化存储。
 * 序列化为 DTO 而非 domain 对象，确保反序列化时枚举走 fromValue() 兜底逻辑。
 */
private fun TrainPlan.toDetailResponseDto() = TrainPlanDetailResponseData(
    planId = planId,
    name = name,
    description = description,
    trainWholeType = trainWholeType.value,
    scheduledDate = scheduledDate,
    hardLevel = hardLevel,
    finishFlag = finishFlag,
    locationType = locationType.value,
    templateId = templateId,
    workoutId = workoutId,
    planIdOfAW = planIdOfAW,
    sourceType = sourceType,
    sourceName = sourceName,
    sentPlatformCodes = sentPlatformCodes.toList(),
    sentPlatformExtWorkoutIds = sentPlatformExtWorkoutIds,
    version = version,
    warmupBlock = warmupBlock?.let {
        com.oterman.rundemo.data.network.dto.response.TrainBlockResponseDto(
            blockId = it.blockId,
            blockType = it.blockType.value,
            seq = it.seq,
            loopCnt = it.loopCnt,
            stepList = it.stepList.map { s -> s.toStepResponseDto() }
        )
    },
    blockList = blockList.map { b ->
        com.oterman.rundemo.data.network.dto.response.TrainBlockResponseDto(
            blockId = b.blockId,
            blockType = b.blockType.value,
            seq = b.seq,
            loopCnt = b.loopCnt,
            stepList = b.stepList.map { s -> s.toStepResponseDto() }
        )
    }.takeIf { it.isNotEmpty() },
    cooldownBlock = cooldownBlock?.let {
        com.oterman.rundemo.data.network.dto.response.TrainBlockResponseDto(
            blockId = it.blockId,
            blockType = it.blockType.value,
            seq = it.seq,
            loopCnt = it.loopCnt,
            stepList = it.stepList.map { s -> s.toStepResponseDto() }
        )
    },
    calGoalStep = calGoalStep?.toStepResponseDto(),
    distanceGoalStep = distanceGoalStep?.toStepResponseDto(),
    timeGoalStep = timeGoalStep?.toStepResponseDto(),
    pacerGoalStep = pacerGoalStep?.toStepResponseDto()
)

private fun com.oterman.rundemo.domain.model.TrainStep.toStepResponseDto() =
    com.oterman.rundemo.data.network.dto.response.TrainStepResponseDto(
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
