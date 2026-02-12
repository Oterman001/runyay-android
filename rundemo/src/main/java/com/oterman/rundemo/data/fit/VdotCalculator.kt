package com.oterman.rundemo.data.fit

import android.util.Log
import com.oterman.rundemo.data.local.entity.OverallVdotEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import kotlin.math.exp
import kotlin.math.pow

/**
 * VDOT计算器
 * 对齐iOS VdotCalculater
 *
 * VDOT是衡量跑步能力的综合指标，基于Jack Daniels的跑步公式。
 * 
 * 计算流程：
 * 1. 从距离和时间计算原始VDOT
 * 2. 根据温度调整时间
 * 3. 根据心率区间调整VDOT
 * 4. 结合历史数据计算Overall VDOT（指数衰减加权平均）
 */
object VdotCalculator {

    private const val TAG = "VdotCalculator"

    // ==================== 核心VDOT计算 ====================

    /**
     * 完整的VDOT计算（含温度和心率调整）
     * 对齐iOS VdotCalculater.calculateFromDistanceAndTime
     *
     * @param distanceMeters 距离（米）
     * @param timeMinute 时间（分钟）
     * @param heartRate 平均心率
     * @param temperature 温度（℃），null表示不做温度调整
     * @param maxHR 最大心率
     * @param restHR 静息心率
     * @return VDOT值，无效返回-1
     */
    fun calculateFromDistanceAndTime(
        distanceMeters: Double,
        timeMinute: Double,
        heartRate: Double,
        temperature: Double? = null,
        maxHR: Double,
        restHR: Double
    ): Double {
        if (distanceMeters <= 0 || timeMinute <= 0 || heartRate <= 0) {
            return -1.0
        }

        // 1. 温度修正
        val tempEffect = getTemperatureEffect(temperature, timeMinute)
        val adjustedTime = if (timeMinute - tempEffect < 0) timeMinute else timeMinute - tempEffect

        // 2. 计算原始VDOT
        val originalVdot = getVDot(distanceMeters, adjustedTime)
        if (originalVdot < 0) {
            return -1.0
        }
        Log.d(TAG, "原始VDOT: $originalVdot")

        // 3. 心率调整
        return adjustWithHeartRate(heartRate, originalVdot, maxHR, restHR)
    }

    /**
     * 从距离和时间计算原始VDOT
     * 对齐iOS VdotCalculater.getVDot
     *
     * @param distanceMeters 距离（米）
     * @param timeMinute 时间（分钟）
     * @return VDOT值
     */
    fun getVDot(distanceMeters: Double, timeMinute: Double): Double {
        if (distanceMeters == 0.0 || timeMinute == 0.0) {
            return -1.0
        }
        val v = getVDotSpeedParam(distanceMeters, timeMinute)
        val vo2 = getVO2(v)
        // fraction of VO2 max
        val f = 0.80 + 0.298956 * exp(-0.193261 * timeMinute) + 0.189439 * exp(-0.012778 * timeMinute)
        return vo2 / f
    }

    // ==================== 温度调整 ====================

    /**
     * 计算温度对跑步时间的影响
     * 对齐iOS VdotCalculater.getTempratureEffect
     * 
     * 温度在12°C-20°C之间为最佳区间，不需要调整
     * 温度>20°C或<12°C时，每偏差1°C增加一定的时间补偿
     *
     * @param temperature 温度（℃）
     * @param timeMinute 跑步时间（分钟）
     * @return 温度影响量（分钟），正值表示高温/低温降低了配速
     */
    private fun getTemperatureEffect(temperature: Double?, timeMinute: Double): Double {
        if (temperature == null || temperature == 0.0) {
            return 0.0
        }

        if (temperature > 20) {
            val impact = (temperature - 20) * (0.16667 * timeMinute)
            return impact / 60.0
        }
        if (temperature < 12) {
            val impact = (20 - temperature) * (0.16667 * timeMinute)
            return impact / 60.0
        }
        return 0.0
    }

    // ==================== 心率调整 ====================

    /**
     * 根据心率区间调整VDOT
     * 对齐iOS VdotCalculater.adjustWithHeartRate
     *
     * 心率区间对应的调整因子：
     * - 恢复/热身 (zone1): factor=1.10
     * - 轻松跑E  (zone2): factor=1.07
     * - 马拉松配速M(zone3): factor=1.05
     * - 乳酸阈值T(zone4): factor=1.02
     * - 无氧耐力A及以上(zone5+): 不调整
     *
     * 调整公式: adjustedVdot = originalVdot + (maxHR/heartRate * factor - 1) * (originalVdot - 3.5)
     *
     * @param heartRate 平均心率
     * @param originalVdot 原始VDOT
     * @param maxHR 最大心率
     * @param restHR 静息心率
     * @return 调整后的VDOT
     */
    private fun adjustWithHeartRate(
        heartRate: Double,
        originalVdot: Double,
        maxHR: Double,
        restHR: Double
    ): Double {
        if (heartRate <= 0) {
            return originalVdot
        }

        // 使用AbilityZoneCalculator获取心率区间
        val heartRateRanges = AbilityZoneCalculator.calculateHeartRate7Ranges(restHR, maxHR)
        val zone = AbilityZoneCalculator.getZoneByHeartRate(heartRate, heartRateRanges)
        Log.d(TAG, "心率区间: zone=$zone, HR=$heartRate")

        // zone >= 5 不调整（高强度运动）
        if (zone >= 5) {
            return originalVdot
        }

        val factor = when (zone) {
            1 -> 1.10
            2 -> 1.07
            3 -> 1.05
            4 -> 1.02
            else -> 1.0
        }

        val adjustedVdot = originalVdot + (maxHR / heartRate * factor - 1) * (originalVdot - 3.5)
        Log.d(TAG, "心率调整VDOT: original=$originalVdot, adjusted=$adjustedVdot, factor=$factor")
        return adjustedVdot
    }

    // ==================== Overall VDOT计算 ====================

    /**
     * 基于历史VDOT数据计算Overall VDOT（指数衰减加权平均）
     * 对齐iOS VdotCalculater.calculateOverallVdot
     *
     * 使用指数衰减加权：lambda=0.1
     * 越新的数据权重越高
     *
     * @param hisVdotList 历史VDOT列表（按日期倒序）
     * @param originVdot 当前原始VDOT
     * @param totalDistance 总距离（公里）
     * @param activeDuration 运动时长（分钟）
     * @return Overall VDOT，无效返回null
     */
    fun calculateOverallVdot(
        hisVdotList: List<OverallVdotEntity>,
        originVdot: Double,
        totalDistance: Double,
        activeDuration: Double
    ): Double? {
        // 数据不足不计算：距离<=2km且时长<=15min
        if (originVdot <= 0 || (totalDistance <= 2 && activeDuration <= 15)) {
            return null
        }

        val lambda = 0.1 // 衰减速率
        val totalCount = hisVdotList.size
        val weights = mutableListOf<Double>()

        // 生成指数衰减权重
        for (day in 0..totalCount) {
            val weight = exp(-lambda * day.toDouble())
            weights.add(weight)
        }

        // 归一化权重
        val sumWeights = weights.sum()
        val normalizedWeights = weights.map { it / sumWeights }

        // 加权计算：当前VDOT * 最高权重 + 历史VDOT * 衰减权重
        var result = originVdot * normalizedWeights[0]
        for (i in 0 until totalCount) {
            result += hisVdotList[i].originValue * normalizedWeights[i + 1]
        }

        Log.d(TAG, "Overall VDOT: origin=$originVdot, overall=$result, historyCount=$totalCount")
        return result
    }

    // ==================== 间歇训练VDOT计算 ====================

    /**
     * 从间歇训练分段计算VDOT
     * 对齐iOS GarminFitImportService.calVdotFromSegment
     * 
     * 只统计work/warmup/cooldown分段（排除recovery），
     * 使用时间加权平均心率
     *
     * @param segments 训练分段列表
     * @param temperature 温度
     * @param maxHR 最大心率
     * @param restHR 静息心率
     * @return VDOT值，数据不足返回null
     */
    fun calculateFromSegments(
        segments: List<RunSegmentEntity>,
        temperature: Double? = null,
        maxHR: Double,
        restHR: Double
    ): Double? {
        var sumDistance = 0.0   // 公里
        var sumTime = 0.0      // 分钟
        var sumHeartRate = 0.0  // 心率*时间（用于加权平均）

        // 只统计work/warmup/cooldown分段（排除recovery）
        for (seg in segments) {
            val type = seg.intervalType
            if (type == "work" || type == "warmup" || type == "cooldown") {
                sumDistance += seg.distance
                sumTime += seg.activeDuration
                sumHeartRate += seg.averageHeartRate * seg.activeDuration
            }
        }

        // 验证数据充足
        if (sumDistance <= 0 || sumTime <= 0 || sumHeartRate <= 0) {
            Log.w(TAG, "间歇训练数据不足: distance=$sumDistance, time=$sumTime, heartRate=$sumHeartRate")
            return null
        }

        // 计算加权平均心率
        val avgHeartRate = sumHeartRate / sumTime

        // 计算VDOT
        val vdot = calculateFromDistanceAndTime(
            distanceMeters = sumDistance * 1000, // 公里转米
            timeMinute = sumTime,
            heartRate = avgHeartRate,
            temperature = temperature,
            maxHR = maxHR,
            restHR = restHR
        )

        Log.i(TAG, "间歇训练VDOT: distance=${sumDistance}km, time=${sumTime}min, avgHR=$avgHeartRate, vdot=$vdot")
        return if (vdot > 0) vdot else null
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 计算VO2（摄氧量）
     * 对齐iOS VdotCalculater.getVO2
     */
    private fun getVO2(v: Double): Double {
        return 0.182258 * v + 0.000104 * v.pow(2) - 4.6
    }

    /**
     * 计算VDOT速度参数
     * 对齐iOS VdotCalculater.getVDotSpeedParam
     * 
     * 对于短距离（<1600m），需要进行调整以获得等效的1600m参数
     */
    private fun getVDotSpeedParam(distanceMeters: Double, timeMinute: Double): Double {
        if (distanceMeters >= 1600) {
            return distanceMeters / timeMinute
        }

        if (distanceMeters > 800) {
            val scale = 1600 / distanceMeters
            val percentage = (1600 - distanceMeters) / 800
            val adjustment = scale + 0.1 * percentage
            val m1600Mins = timeMinute * adjustment
            return 1600 / m1600Mins
        } else {
            val m800Adjustment = 2.1
            val distanceFactor = 800 / distanceMeters
            val adjustment = distanceFactor * m800Adjustment
            val m1600Mins = timeMinute * adjustment
            return 1600 / m1600Mins
        }
    }
}

