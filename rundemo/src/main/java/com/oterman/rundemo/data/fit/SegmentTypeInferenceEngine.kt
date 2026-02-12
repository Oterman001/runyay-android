package com.oterman.rundemo.data.fit

import com.oterman.rundemo.util.RLog
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 训练分段类型推断引擎
 * 对齐iOS SegmentTypeInferenceEngine
 * 用于在FIT文件缺少intensity字段时推断训练分段类型
 */
class SegmentTypeInferenceEngine {

    companion object {
        private const val TAG = "SegmentTypeInference"
    }

    /** Lap上下文信息 */
    data class LapContext(
        val avgHeartRate: Double,  // 平均心率 (bpm)
        val avgPace: Double,       // 平均配速 (min/km)
        val duration: Double,      // 时长 (秒)
        val distance: Double       // 距离 (米)
    )

    /** Lap分组信息 */
    private data class LapGroup(
        val indices: MutableList<Int>,
        val avgDuration: Double,
        val avgDistance: Double,
        val inferredTypes: MutableList<String> = mutableListOf()
    )

    /** 全局统计信息 */
    private data class GlobalStats(
        val meanHR: Double,
        val stdDevHR: Double,
        val meanPace: Double,
        val stdDevPace: Double,
        val isIntervalTraining: Boolean,
        val fluctuationCount: Int
    )

    /** 存储所有Lap的推断结果 */
    private val inferredResults = mutableMapOf<Int, String>()

    /** 缓存的Lap分组信息 */
    private var cachedGroups: List<LapGroup> = emptyList()

    // ==================== 公共接口 ====================

    /**
     * 推断分段类型
     * @param lapIndex 当前Lap索引（0-based）
     * @param lapContexts 所有Lap的上下文信息
     * @param maxHR 最大心率（bpm）
     * @param restHR 静息心率（bpm）
     * @return 分段类型字符串 ("warmup"/"work"/"recovery"/"cooldown")
     */
    fun inferSegmentType(
        lapIndex: Int,
        lapContexts: List<LapContext>,
        maxHR: Double,
        restHR: Double
    ): String {
        if (lapIndex < 0 || lapIndex >= lapContexts.size) {
            RLog.w(TAG, "无效的lapIndex: $lapIndex, 总数: ${lapContexts.size}")
            return "work"
        }

        val currentLap = lapContexts[lapIndex]
        val totalLapCount = lapContexts.size

        // 1. 计算全局统计信息
        val stats = calculateGlobalStats(lapContexts)

        // 2. Lap分组（仅在第一次调用时执行）
        if (lapIndex == 0) {
            cachedGroups = groupSimilarLaps(lapContexts)
            inferredResults.clear()
        }

        // 3. 计算各因子评分
        val positionScore = calculatePositionScore(lapIndex, totalLapCount)
        val heartRateScore = calculateHeartRateScore(currentLap, lapIndex, lapContexts, stats, maxHR, restHR)
        val paceScore = calculatePaceScore(currentLap, lapIndex, lapContexts, stats)
        val durationScore = calculateDurationScore(currentLap)

        // 4. 综合加权评分
        val config = SegmentInferenceConfig
        val totalScore = positionScore * config.positionWeight +
                heartRateScore * config.heartRateWeight +
                paceScore * config.paceWeight +
                durationScore * config.durationWeight

        // 5. 基于决策树判断类型（初步推断）
        val preliminaryType = makeDecision(
            lapIndex, totalLapCount, currentLap, lapContexts, stats, totalScore, maxHR, restHR
        )

        // 6. 应用分组一致性修正
        val consistencyType = applyGroupConsistency(lapIndex, cachedGroups, preliminaryType)

        // 7. 应用逻辑约束验证
        val finalType = validateSegmentLogic(lapIndex, totalLapCount, consistencyType)

        // 8. 保存推断结果到缓存
        inferredResults[lapIndex] = finalType

        RLog.i(TAG, buildString {
            append("Lap[$lapIndex] 推断: ")
            append("HR=${String.format("%.0f", currentLap.avgHeartRate)}bpm ")
            append("配速=${formatPace(currentLap.avgPace)} ")
            append("时长=${String.format("%.1f", currentLap.duration / 60)}min ")
            append("评分=[位置=${String.format("%.2f", positionScore)}, HR=${String.format("%.2f", heartRateScore)}, 配速=${String.format("%.2f", paceScore)}, 时长=${String.format("%.2f", durationScore)}] ")
            append("总分=${String.format("%.2f", totalScore)} ")
            append("→ $finalType")
        })

        return finalType
    }

    /**
     * 批量推断所有分段类型
     * 从FitLap列表构建LapContext并推断
     */
    fun inferAllSegmentTypes(
        laps: List<FitLap>,
        maxHR: Double,
        restHR: Double
    ): List<String> {
        val lapContexts = laps.map { lap ->
            LapContext(
                avgHeartRate = lap.avgHeartRate?.toDouble() ?: 0.0,
                avgPace = FitFileParser.speedToPace(lap.avgSpeed),
                duration = lap.totalElapsedTime.toDouble(),
                distance = lap.totalDistance.toDouble()
            )
        }

        return lapContexts.indices.map { index ->
            inferSegmentType(index, lapContexts, maxHR, restHR)
        }
    }

    // ==================== 全局统计 ====================

    private fun calculateGlobalStats(lapContexts: List<LapContext>): GlobalStats {
        if (lapContexts.isEmpty()) {
            return GlobalStats(0.0, 0.0, 0.0, 0.0, false, 0)
        }

        val hrValues = lapContexts.map { it.avgHeartRate }.filter { it > 0 }
        val meanHR = if (hrValues.isEmpty()) 0.0 else hrValues.average()
        val stdDevHR = calculateStandardDeviation(hrValues, meanHR)

        val paceValues = lapContexts.map { it.avgPace }.filter { it > 0 }
        val meanPace = if (paceValues.isEmpty()) 0.0 else paceValues.average()
        val stdDevPace = calculateStandardDeviation(paceValues, meanPace)

        // 检测波动（间歇训练特征）
        var fluctuationCount = 0
        for (i in 1 until lapContexts.size) {
            val hrDiff = abs(lapContexts[i].avgHeartRate - lapContexts[i - 1].avgHeartRate)
            val paceDiff = abs(lapContexts[i].avgPace - lapContexts[i - 1].avgPace)
            if (hrDiff > stdDevHR * SegmentInferenceConfig.fluctuationThreshold ||
                paceDiff > stdDevPace * SegmentInferenceConfig.fluctuationThreshold
            ) {
                fluctuationCount++
            }
        }

        val fluctuationRatio = if (lapContexts.size > 1) fluctuationCount.toDouble() / (lapContexts.size - 1) else 0.0
        val isIntervalTraining = fluctuationRatio >= SegmentInferenceConfig.intervalTrainingThreshold

        RLog.i(TAG, "全局统计: HR均值=${String.format("%.1f", meanHR)}bpm, 配速均值=${formatPace(meanPace)}, " +
                "波动=${fluctuationCount}次(${String.format("%.0f", fluctuationRatio * 100)}%), " +
                "训练模式=${if (isIntervalTraining) "间歇" else "连续"}")

        return GlobalStats(meanHR, stdDevHR, meanPace, stdDevPace, isIntervalTraining, fluctuationCount)
    }

    private fun calculateStandardDeviation(values: List<Double>, mean: Double): Double {
        if (values.size <= 1) return 0.0
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }

    // ==================== 因子评分 ====================

    private fun calculatePositionScore(lapIndex: Int, totalLapCount: Int): Double {
        return when (lapIndex) {
            0 -> 0.8                        // 首个Lap，warmup倾向
            totalLapCount - 1 -> 0.8        // 最后Lap，cooldown倾向
            else -> 0.0                      // 中间Lap，中立
        }
    }

    private fun calculateHeartRateScore(
        currentLap: LapContext,
        lapIndex: Int,
        lapContexts: List<LapContext>,
        stats: GlobalStats,
        maxHR: Double,
        restHR: Double
    ): Double {
        if (currentLap.avgHeartRate <= 0 || maxHR <= restHR) return 0.5

        val hrr = (currentLap.avgHeartRate - restHR) / (maxHR - restHR)
        var score = 0.5

        if (hrr < SegmentInferenceConfig.warmupHRThreshold) {
            score += 0.4
        } else if (hrr < SegmentInferenceConfig.recoveryHRThreshold) {
            score += 0.2
        } else if (hrr >= SegmentInferenceConfig.workHRThreshold) {
            score -= 0.3
        }

        // 趋势分析
        if (lapIndex > 0) {
            val prevHR = lapContexts[lapIndex - 1].avgHeartRate
            val hrChange = currentLap.avgHeartRate - prevHR
            if (hrChange > SegmentInferenceConfig.hrRisingThreshold) {
                score += 0.1
            } else if (hrChange < -SegmentInferenceConfig.hrFallingThreshold) {
                score += 0.1
            }
        }

        return score.coerceIn(0.0, 1.0)
    }

    private fun calculatePaceScore(
        currentLap: LapContext,
        lapIndex: Int,
        lapContexts: List<LapContext>,
        stats: GlobalStats
    ): Double {
        if (currentLap.avgPace <= 0 || stats.meanPace <= 0) return 0.5

        val paceRatio = currentLap.avgPace / stats.meanPace
        var score = 0.5

        if (paceRatio >= SegmentInferenceConfig.verySlowPaceThreshold) {
            score += 0.4
        } else if (paceRatio >= SegmentInferenceConfig.slowPaceThreshold) {
            score += 0.2
        } else if (paceRatio <= SegmentInferenceConfig.veryFastPaceThreshold) {
            score -= 0.4
        } else if (paceRatio <= SegmentInferenceConfig.fastPaceThreshold) {
            score -= 0.2
        }

        // 波动检测（间歇训练特征）
        if (stats.isIntervalTraining && lapIndex > 0 && lapIndex < lapContexts.size - 1) {
            val prevPace = lapContexts[lapIndex - 1].avgPace
            val nextPace = lapContexts[lapIndex + 1].avgPace
            // 快-慢-快模式 → recovery
            if (prevPace < stats.meanPace && currentLap.avgPace > stats.meanPace && nextPace < stats.meanPace) {
                score += 0.5
            }
            // 慢-快-慢模式 → work
            if (prevPace > stats.meanPace && currentLap.avgPace < stats.meanPace && nextPace > stats.meanPace) {
                score -= 0.5
            }
        }

        return score.coerceIn(0.0, 1.0)
    }

    private fun calculateDurationScore(currentLap: LapContext): Double {
        var score = 0.5
        if (currentLap.duration < SegmentInferenceConfig.shortLapDuration) {
            score = 0.5
        } else if (currentLap.duration > SegmentInferenceConfig.longLapDuration) {
            score += 0.2
        }
        return score
    }

    // ==================== 决策树 ====================

    private fun makeDecision(
        lapIndex: Int,
        totalLapCount: Int,
        currentLap: LapContext,
        lapContexts: List<LapContext>,
        stats: GlobalStats,
        totalScore: Double,
        maxHR: Double,
        restHR: Double
    ): String {
        val hrr = if (maxHR > restHR) (currentLap.avgHeartRate - restHR) / (maxHR - restHR) else 0.5
        val paceRatio = if (stats.meanPace > 0) currentLap.avgPace / stats.meanPace else 1.0

        // 1. 首个Lap判断
        if (lapIndex == 0) {
            return if (totalScore >= SegmentInferenceConfig.warmupCooldownScoreThreshold && hrr < 0.65) {
                "warmup"
            } else if (hrr > 0.80 && paceRatio < 0.9) {
                "work"
            } else {
                "warmup"
            }
        }

        // 2. 最后Lap判断
        if (lapIndex == totalLapCount - 1) {
            return if (totalScore >= SegmentInferenceConfig.warmupCooldownScoreThreshold && hrr < 0.65) {
                "cooldown"
            } else if (hrr > 0.80 && paceRatio < 0.9) {
                "work"
            } else {
                "cooldown"
            }
        }

        // 3. 中间Lap（间歇训练模式）
        if (stats.isIntervalTraining && lapIndex > 0 && lapIndex < totalLapCount - 1) {
            val prevPace = lapContexts[lapIndex - 1].avgPace
            val nextPace = lapContexts[lapIndex + 1].avgPace
            if (prevPace > stats.meanPace && currentLap.avgPace < stats.meanPace && nextPace > stats.meanPace) {
                return "work"
            }
            if (prevPace < stats.meanPace && currentLap.avgPace > stats.meanPace && nextPace < stats.meanPace) {
                return "recovery"
            }
        }

        // 4. 中间Lap（非间歇）
        return if (hrr < SegmentInferenceConfig.recoveryHRThreshold && paceRatio >= SegmentInferenceConfig.slowPaceThreshold) {
            "recovery"
        } else {
            "work"
        }
    }

    // ==================== 逻辑约束验证 ====================

    private fun validateSegmentLogic(
        lapIndex: Int,
        totalLapCount: Int,
        proposedType: String
    ): String {
        val hasWorkBefore = (0 until lapIndex).any { inferredResults[it] == "work" }
        val hasWarmupBefore = (0 until lapIndex).any { inferredResults[it] == "warmup" }
        val previousType = if (lapIndex > 0) inferredResults[lapIndex - 1] else null
        val isLastLap = lapIndex == totalLapCount - 1

        return when (proposedType) {
            "warmup" -> {
                if (hasWarmupBefore) {
                    RLog.i(TAG, "  ⚠️ 逻辑约束: 已存在热身段，Lap[$lapIndex] warmup → work")
                    "work"
                } else if (hasWorkBefore) {
                    RLog.i(TAG, "  ⚠️ 逻辑约束: warmup不能在work之后，修正为work")
                    "work"
                } else {
                    "warmup"
                }
            }
            "recovery" -> {
                if (!hasWorkBefore) {
                    RLog.i(TAG, "  ⚠️ 逻辑约束: recovery前面必须有work，修正为warmup")
                    "warmup"
                } else if (isLastLap && previousType == "recovery") {
                    RLog.i(TAG, "  ⚠️ 逻辑约束: 最后一段recovery且上一段也是recovery，修正为cooldown")
                    "cooldown"
                } else {
                    "recovery"
                }
            }
            "cooldown" -> {
                if (!hasWorkBefore) {
                    RLog.i(TAG, "  ⚠️ 逻辑约束: cooldown前面必须有work，修正为warmup")
                    "warmup"
                } else {
                    "cooldown"
                }
            }
            else -> proposedType
        }
    }

    // ==================== 分组功能 ====================

    private fun groupSimilarLaps(lapContexts: List<LapContext>): List<LapGroup> {
        val groups = mutableListOf<LapGroup>()

        for ((index, lap) in lapContexts.withIndex()) {
            var addedToGroup = false
            for (group in groups) {
                val timeSimilar = abs(lap.duration - group.avgDuration) <= SegmentInferenceConfig.timeSimilarityThreshold
                val distanceSimilar = abs(lap.distance - group.avgDistance) <= SegmentInferenceConfig.distanceSimilarityThreshold
                if (timeSimilar || distanceSimilar) {
                    group.indices.add(index)
                    addedToGroup = true
                    break
                }
            }
            if (!addedToGroup) {
                groups.add(LapGroup(
                    indices = mutableListOf(index),
                    avgDuration = lap.duration,
                    avgDistance = lap.distance
                ))
            }
        }

        return groups.filter { it.indices.size >= SegmentInferenceConfig.minGroupSizeForConsistency }
    }

    private fun applyGroupConsistency(
        lapIndex: Int,
        groups: List<LapGroup>,
        preliminaryType: String
    ): String {
        val targetGroup = groups.find { lapIndex in it.indices } ?: return preliminaryType

        val groupInferredTypes = targetGroup.indices
            .filter { it < lapIndex }
            .mapNotNull { inferredResults[it] }

        if (groupInferredTypes.isEmpty()) return preliminaryType

        val typeCounts = groupInferredTypes.groupBy { it }.mapValues { it.value.size }
        val majorityEntry = typeCounts.maxByOrNull { it.value } ?: return preliminaryType
        val ratio = majorityEntry.value.toDouble() / groupInferredTypes.size

        return if (ratio >= SegmentInferenceConfig.groupConsistencyThreshold) {
            RLog.i(TAG, "  分组一致性修正: Lap[$lapIndex] → ${majorityEntry.key} (占比${String.format("%.0f", ratio * 100)}%)")
            majorityEntry.key
        } else {
            preliminaryType
        }
    }

    // ==================== 辅助方法 ====================

    private fun formatPace(pace: Double): String {
        if (pace <= 0 || pace >= 100) return "N/A"
        val minutes = pace.toInt()
        val seconds = ((pace - minutes) * 60).toInt()
        return String.format("%d:%02d", minutes, seconds)
    }
}

