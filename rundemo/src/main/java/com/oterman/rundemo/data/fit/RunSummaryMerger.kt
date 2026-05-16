package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.network.dto.RunSummaryBasicInfoDto
import com.oterman.rundemo.util.RLog

/**
 * 字段差异记录
 */
data class FieldDiff(
    val fieldName: String,
    val localValue: Any?,
    val serverValue: Any?,
    val usedValue: Any?,
    val serverRawFieldName: String? = null,
    val serverRawValue: Any? = null
)

/**
 * 合并结果
 */
data class MergeResult(
    val mergedEntity: RunRecordEntity,
    val diffs: List<FieldDiff>
)

/**
 * 跑步摘要合并器
 *
 * 合并算法：
 * - 服务端字段有值 → 使用服务端值（服务端优先）
 * - 服务端字段无值(null) → 保留本地解析值
 * - 对于Double字段，null视为"无值"，0.0视为有效值
 * - 差异日志记录方便排查
 */
object RunSummaryMerger {

    private const val TAG = "RunSummaryMerger"

    /**
     * 合并本地解析的Entity与服务端的RunSummary
     *
     * @param local 本地FIT解析生成的RunRecordEntity
     * @param server 服务端返回的RunSummaryBasicInfoDto
     * @return MergeResult 包含合并后的Entity和差异列表
     */
    fun merge(local: RunRecordEntity, server: RunSummaryBasicInfoDto): MergeResult {
        val diffs = mutableListOf<FieldDiff>()

        // 使用Mapper应用服务端值
        val merged = RunSummaryMapper.applyServerValues(local, server)

        // 记录差异
        trackDiff(diffs, "startTime", local.startTime, merged.startTime, server.startTimeInSeconds?.let { it * 1000 }, "startTimeInSeconds", server.startTimeInSeconds)
        trackDiff(diffs, "duration", local.duration, merged.duration, server.durationInSeconds?.let { it / 60.0 }, "durationInSeconds", server.durationInSeconds)
        trackDiff(diffs, "activeDuration", local.activeDuration, merged.activeDuration, server.activeDuration?.let { it / 60.0 }, "activeDuration(raw)", server.activeDuration)
        trackDiff(diffs, "totalDistance", local.totalDistance, merged.totalDistance, server.distanceInMeters?.let { it / 1000.0 }, "distanceInMeters", server.distanceInMeters)
        trackDiff(diffs, "averageSpeed", local.averageSpeed, merged.averageSpeed, server.averagePace)
        trackDiff(diffs, "maxSpeed", local.maxSpeed, merged.maxSpeed, server.maxPace)
        trackDiff(diffs, "averageHeartRate", local.averageHeartRate, merged.averageHeartRate, server.averageHeartRate)
        trackDiff(diffs, "maxHeartRate", local.maxHeartRate, merged.maxHeartRate, server.maxHeartRate)
        trackDiff(diffs, "minHeartRate", local.minHeartRate, merged.minHeartRate, server.minHeartRate)
        trackDiff(diffs, "averagePower", local.averagePower, merged.averagePower, server.averagePower)
        trackDiff(diffs, "maxPower", local.maxPower, merged.maxPower, server.maxPower)
        trackDiff(diffs, "averageCadence", local.averageCadence, merged.averageCadence, server.averageCadence)
        trackDiff(diffs, "averageStrideLength", local.averageStrideLength, merged.averageStrideLength, server.averageStrideLength)
        trackDiff(diffs, "averageVerticalOscillation", local.averageVerticalOscillation, merged.averageVerticalOscillation, server.averageVerticalOscillation)
        trackDiff(diffs, "averageContactTime", local.averageContactTime, merged.averageContactTime, server.averageContactTime)
        trackDiff(diffs, "totalCalories", local.totalCalories, merged.totalCalories, server.activeKilocalories)
        trackDiff(diffs, "totalStepCount", local.totalStepCount, merged.totalStepCount, server.totalStepCount)
        trackDiff(diffs, "elevationAscended", local.elevationAscended, merged.elevationAscended, server.totalElevationGain)
        trackDiff(diffs, "elevationDescended", local.elevationDescended, merged.elevationDescended, server.totalElevationLoss)
        trackDiff(diffs, "vdot", local.vdot, merged.vdot, server.vdot)
        trackDiff(diffs, "overallVdot", local.overallVdot, merged.overallVdot, server.overallVdot)
        trackDiff(diffs, "trainingEffect", local.trainingEffect, merged.trainingEffect, server.trainingEffect)
        trackDiff(diffs, "anaerobicTrainingEffect", local.anaerobicTrainingEffect, merged.anaerobicTrainingEffect, server.anaerobicTrainingEffect)
        trackDiff(diffs, "trainingLoad", local.trainingLoad, merged.trainingLoad, server.trainingLoad)
        trackDiff(diffs, "outdoor", local.outdoor, merged.outdoor, server.outdoor)
        trackDiff(diffs, "note", local.note, merged.note, server.note)
        trackDiff(diffs, "feelingLevel", local.feelingLevel, merged.feelingLevel, server.feelingLevel)
        trackDiff(diffs, "shoeId", local.shoeId, merged.shoeId, server.shoeId)
        trackDiff(diffs, "trainPlanId", local.trainPlanId, merged.trainPlanId, server.trainPlanId)

        // 打印差异日志
        if (diffs.isNotEmpty()) {
            RLog.i(TAG, "合并 ${local.originId}: ${diffs.size}个字段有差异")
            for (diff in diffs) {
                val serverPart = if (diff.serverRawValue != null) {
                    "${diff.serverValue}(原始:${diff.serverRawFieldName}=${diff.serverRawValue})"
                } else {
                    "${diff.serverValue}"
                }
                RLog.d(TAG, "  ${diff.fieldName}: 本地=${diff.localValue}, 服务端=$serverPart, 使用=${diff.usedValue}")
            }
        } else {
            RLog.d(TAG, "合并 ${local.originId}: 无差异")
        }

        return MergeResult(mergedEntity = merged, diffs = diffs)
    }

    /**
     * 记录有差异的字段
     * 仅当服务端有值且与本地不同时记录
     */
    private fun trackDiff(
        diffs: MutableList<FieldDiff>,
        fieldName: String,
        localValue: Any?,
        mergedValue: Any?,
        serverConverted: Any?,
        serverRawFieldName: String? = null,
        serverRawValue: Any? = null
    ) {
        // 服务端无值，不算差异（保留了本地值）
        if (serverConverted == null) return

        // 比较值是否不同
        val isDifferent = when {
            localValue is Double && serverConverted is Double -> {
                kotlin.math.abs(localValue - serverConverted) > 0.001
            }
            localValue is Long && serverConverted is Long -> {
                localValue != serverConverted
            }
            else -> {
                localValue != serverConverted
            }
        }

        if (isDifferent) {
            diffs.add(FieldDiff(fieldName, localValue, serverConverted, mergedValue, serverRawFieldName, serverRawValue))
        }
    }
}
