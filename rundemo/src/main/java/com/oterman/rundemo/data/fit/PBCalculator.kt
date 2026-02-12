package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.local.entity.PBRecordEntity
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import com.oterman.rundemo.util.RLog
import kotlin.math.abs

/**
 * PB(Personal Best)计算器
 * 对齐iOS RunDataDetailSaveManager中的PB计算逻辑
 *
 * PB类型：
 * - Speed类型：时间越短越好（1k/3k/5k/10k/21k/42k的最快用时）
 * - Ability类型：值越大越好（最长时间MTime、最远距离MDistance、最高VDOT MVdot）
 *
 * 速度PB计算使用滑动窗口算法，在公里分段上滑动找到最快的N公里用时。
 * 对于半马(21k)和全马(42k)，还需要补齐零头距离(0.0975km和0.195km)。
 */
object PBCalculator {

    private const val TAG = "PBCalculator"

    /**
     * 计算当前跑步记录的所有PB候选值
     * 对齐iOS RunDataDetailSaveManager.getCurPB
     *
     * @param runRecord 跑步记录
     * @param segments 所有分段（包含公里分段和训练分段）
     * @return PB记录列表（每项都是该次跑步的成绩，不一定是历史最佳）
     */
    fun calculateCurrentPBs(
        runRecord: RunRecordEntity,
        segments: List<RunSegmentEntity>
    ): List<PBRecordEntity> {
        val result = mutableListOf<PBRecordEntity>()

        // Ability类PB：最长运动时间
        result.add(createAbilityPB(runRecord, "MTime", runRecord.activeDuration))

        // Ability类PB：最远距离
        result.add(createAbilityPB(runRecord, "MDistance", runRecord.totalDistance))

        // Ability类PB：最高VDOT
        if (runRecord.vdot > 0) {
            result.add(createAbilityPB(runRecord, "MVdot", runRecord.vdot))
        }

        // Speed类PB：各距离最快用时
        // 过滤出公里分段(segmentType=1)并按序号排序
        val kmSegments = segments
            .filter { it.segmentType == 1 }
            .sortedBy { it.seq }

        // 1k, 3k, 5k, 10k
        for (kilo in listOf(1, 3, 5, 10)) {
            val minTime = calculateMinTimeForKilo(runRecord, kmSegments, kilo)
            if (minTime != null) {
                result.add(createSpeedPB(runRecord, "${kilo}k", minTime))
            }
        }

        // 21k (半马)
        val halfMarathonTime = calculateMinTimeForKilo(runRecord, kmSegments, 21)
        if (halfMarathonTime != null) {
            result.add(createSpeedPB(runRecord, "21k", halfMarathonTime))
        }

        // 42k (全马)
        val marathonTime = calculateMinTimeForKilo(runRecord, kmSegments, 42)
        if (marathonTime != null) {
            result.add(createSpeedPB(runRecord, "42k", marathonTime))
        }

        RLog.i(TAG, "PB计算完成: ${result.size}项 (${result.map { "${it.subType}=${String.format("%.2f", it.value)}" }.joinToString(", ")})")
        return result
    }

    /**
     * 使用滑动窗口计算指定公里数的最快用时
     * 对齐iOS RunDataDetailSaveManager.getCurMinByKilo
     *
     * 算法：
     * 1. 在公里分段上使用滑动窗口
     * 2. 累计连续N个完整公里分段的时间
     * 3. 对于半马和全马，还需要额外补齐零头距离的时间
     *
     * @param runRecord 跑步记录
     * @param segmentList 公里分段列表（已排序）
     * @param kilo 目标公里数
     * @return 最快用时（分钟），不足公里数返回null
     */
    private fun calculateMinTimeForKilo(
        runRecord: RunRecordEntity,
        segmentList: List<RunSegmentEntity>,
        kilo: Int
    ): Double? {
        val targetDistance = when (kilo) {
            21 -> 21.0975
            42 -> 42.195
            else -> kilo.toDouble()
        }

        // 如果总距离恰好等于目标距离，直接返回总时间
        if (runRecord.totalDistance == targetDistance) {
            return runRecord.activeDuration
        }

        // 距离不够或公里分段不够
        if (runRecord.totalDistance < kilo.toDouble() || segmentList.size < kilo) {
            return null
        }

        // 对于半马/全马，需要额外检查零头
        if (runRecord.totalDistance < targetDistance || segmentList.size < kilo + 1) {
            return null
        }

        val epsilon = 0.05 // 判断完整公里分段的容差
        var minTime: Double? = null

        // 滑动窗口，计算i到endIndex之间的耗时
        for (i in 0..segmentList.size - kilo) {
            var curTime = 0.0
            var endIndex = i
            var curDistance = 0.0

            while (endIndex <= i + kilo - 1) {
                val seg = segmentList[endIndex]
                if (abs(seg.distance - 1.0) < epsilon) {
                    curTime += seg.activeDuration
                    curDistance += 1.0
                } else {
                    break
                }
                if (curDistance >= kilo.toDouble()) {
                    break
                }
                endIndex++
            }

            if (curDistance >= kilo.toDouble() && curTime != 0.0 && (minTime == null || curTime < minTime)) {
                // 半马或全马特殊处理
                if ((kilo == 21 || kilo == 42) && endIndex >= segmentList.size - 1) {
                    // 只满足21或42km，但后面没有距离了
                    continue
                }

                // 21或42km的补齐零头
                if (kilo == 21 || kilo == 42) {
                    val changeKilo = if (kilo == 21) 0.0975 else 0.195
                    if (segmentList[endIndex].distance < changeKilo) {
                        continue
                    }
                    // 用下一个分段的配速补齐零头时间
                    curTime += getChangeTime(segmentList[endIndex + 1].averageSpeed, changeKilo)
                }
                minTime = curTime
            }
        }

        return minTime
    }

    /**
     * 根据配速计算零头距离的用时
     * 对齐iOS RunDataDetailSaveManager.getChangeTime
     *
     * @param speed 配速(min/km)
     * @param changeDistanceKilo 零头距离(km)
     * @return 零头用时(分钟)
     */
    private fun getChangeTime(speed: Double, changeDistanceKilo: Double): Double {
        return speed * changeDistanceKilo
    }

    /**
     * 创建Speed类型PB记录
     */
    private fun createSpeedPB(
        runRecord: RunRecordEntity,
        subType: String,
        value: Double
    ): PBRecordEntity {
        return PBRecordEntity(
            workoutId = runRecord.workoutId,
            type = "Speed",
            subType = subType,
            value = value,
            completeTime = runRecord.startTime,
            inclusiveLevel = if (runRecord.inclusiveLevel == 1) 1 else 0
        )
    }

    /**
     * 创建Ability类型PB记录
     */
    private fun createAbilityPB(
        runRecord: RunRecordEntity,
        subType: String,
        value: Double
    ): PBRecordEntity {
        return PBRecordEntity(
            workoutId = runRecord.workoutId,
            type = "Ability",
            subType = subType,
            value = value,
            completeTime = runRecord.startTime,
            inclusiveLevel = if (runRecord.inclusiveLevel == 1) 1 else 0
        )
    }
}

