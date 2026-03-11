package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.local.entity.OverallVdotEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import com.oterman.rundemo.util.RLog
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 单次VDOT计算结构化结果
 *
 * @param vdot 最终VDOT值（含环境修正和心率调整）
 * @param confidence 数据质量置信度 [0.0, 1.0]
 * @param rawVdot 原始VDOT（无环境修正）
 * @param environmentalAdjustmentMinutes 环境修正的时间量（分钟）
 */
data class VdotResult(
    val vdot: Double,
    val confidence: Double,
    val rawVdot: Double,
    val environmentalAdjustmentMinutes: Double
)

/**
 * VDOT计算器
 *
 * VDOT是衡量跑步能力的综合指标，基于Jack Daniels的跑步公式。
 *
 * 计算流程：
 * 1. 从距离和时间计算原始VDOT
 * 2. 根据体感温度（温度+湿度）调整时间
 * 3. 根据心率区间调整VDOT
 * 4. 计算数据质量置信度
 * 5. 结合历史数据计算Overall VDOT（基于实际天数衰减 + 置信度加权 + 异常值检测）
 */
object VdotCalculator {

    private const val TAG = "VdotCalculator"

    // ==================== 核心VDOT计算 ====================

    /**
     * 完整的VDOT计算，返回结构化结果
     *
     * @param distanceMeters 距离（米）
     * @param timeMinute 时间（分钟）
     * @param heartRate 平均心率
     * @param temperature 温度（℃），null表示不做温度调整
     * @param humidity 湿度（0-100），null或0表示不做湿度调整
     * @param maxHR 最大心率
     * @param restHR 静息心率
     * @return VdotResult，无效返回null
     */
    fun calculateWithResult(
        distanceMeters: Double,
        timeMinute: Double,
        heartRate: Double,
        temperature: Double? = null,
        humidity: Double? = null,
        maxHR: Double,
        restHR: Double
    ): VdotResult? {
        if (distanceMeters <= 0 || timeMinute <= 0 || heartRate <= 0) {
            return null
        }

        // 1. 体感温度修正
        val tempEffect = getTemperatureEffect(temperature, humidity, timeMinute)
        val adjustedTime = if (timeMinute - tempEffect < 0) timeMinute else timeMinute - tempEffect

        // 2. 计算原始VDOT（无环境修正）
        val rawVdot = getVDot(distanceMeters, timeMinute)
        if (rawVdot < 0) return null

        // 3. 计算环境修正后的VDOT
        val envAdjustedVdot = getVDot(distanceMeters, adjustedTime)
        if (envAdjustedVdot < 0) return null
        RLog.d(TAG, "原始VDOT: $rawVdot, 环境修正后: $envAdjustedVdot")

        // 4. 心率调整
        val finalVdot = adjustWithHeartRate(heartRate, envAdjustedVdot, maxHR, restHR)

        // 5. 计算置信度
        val zone = getHeartRateZone(heartRate, maxHR, restHR)
        val confidence = calculateConfidence(
            distanceMeters = distanceMeters,
            timeMinute = timeMinute,
            heartRateZone = zone,
            hasTemperature = temperature != null && temperature != 0.0
        )

        return VdotResult(
            vdot = finalVdot,
            confidence = confidence,
            rawVdot = rawVdot,
            environmentalAdjustmentMinutes = tempEffect
        )
    }

    /**
     * 完整的VDOT计算（向后兼容接口）
     * 返回单一Double值，humidity为null时行为与旧版一致
     */
    fun calculateFromDistanceAndTime(
        distanceMeters: Double,
        timeMinute: Double,
        heartRate: Double,
        temperature: Double? = null,
        humidity: Double? = null,
        maxHR: Double,
        restHR: Double
    ): Double {
        return calculateWithResult(
            distanceMeters, timeMinute, heartRate,
            temperature, humidity, maxHR, restHR
        )?.vdot ?: -1.0
    }

    /**
     * 从距离和时间计算原始VDOT
     */
    fun getVDot(distanceMeters: Double, timeMinute: Double): Double {
        if (distanceMeters == 0.0 || timeMinute == 0.0) {
            return -1.0
        }
        val v = getVDotSpeedParam(distanceMeters, timeMinute)
        val vo2 = getVO2(v)
        val f = 0.80 + 0.298956 * exp(-0.193261 * timeMinute) + 0.189439 * exp(-0.012778 * timeMinute)
        return vo2 / f
    }

    // ==================== 体感温度调整 ====================

    /**
     * 计算体感温度
     * - 温度 ≤ 20°C 时湿度影响极小，直接用实际温度
     * - 温度 > 20°C 时用简化 Steadman 公式
     *
     * @param temperature 实际温度（℃）
     * @param humidity 相对湿度（0-100）
     * @return 体感温度（℃）
     */
    internal fun getApparentTemperature(temperature: Double, humidity: Double?): Double {
        if (humidity == null || humidity <= 0 || temperature <= 20) {
            return temperature
        }
        // 简化 Steadman 公式：AT = T + 0.33 * (rh/100 * 6.105 * exp(17.27*T/(237.7+T))) - 4.0
        val waterVaporPressure = (humidity / 100.0) * 6.105 * exp(17.27 * temperature / (237.7 + temperature))
        return temperature + 0.33 * waterVaporPressure - 4.0
    }

    /**
     * 计算温度+湿度对跑步时间的影响
     *
     * 体感温度在10-18°C之间为最佳区间，不需要调整
     * 高温惩罚随偏差增大略微递增；低温惩罚比高温轻
     * 安全上限：时间调整不超过10%
     *
     * @param temperature 温度（℃）
     * @param humidity 湿度（0-100）
     * @param timeMinute 跑步时间（分钟）
     * @return 温度影响量（分钟），正值表示高温/低温降低了配速
     */
    internal fun getTemperatureEffect(temperature: Double?, humidity: Double?, timeMinute: Double): Double {
        if (temperature == null || temperature == 0.0) {
            return 0.0
        }

        val apparentTemp = getApparentTemperature(temperature, humidity)

        val impact: Double
        when {
            apparentTemp > 18 -> {
                // 高温：每偏差1°C增加0.18%时间，偏差越大惩罚递增
                val deviation = apparentTemp - 18
                val penaltyPerDegree = 0.18 + 0.005 * deviation // 递增惩罚
                impact = deviation * (penaltyPerDegree / 100.0) * timeMinute
            }
            apparentTemp < 10 -> {
                // 低温：惩罚比高温轻，每偏差1°C增加0.10%时间
                val deviation = 10 - apparentTemp
                impact = deviation * (0.10 / 100.0) * timeMinute
            }
            else -> return 0.0 // 10-18°C 最佳区间
        }

        // 安全上限：不超过10%
        val maxAdjustment = timeMinute * 0.10
        return min(impact, maxAdjustment)
    }

    // ==================== 数据质量置信度 ====================

    /**
     * 计算数据质量置信度 [0.0, 1.0]
     *
     * 四个维度：
     * - 距离可靠性 (0-0.4)：5-42km最高，<2km极低
     * - 时长可靠性 (0-0.3)：25-120min最高
     * - 心率区间适配性 (0-0.2)：T/I区间最高
     * - 环境数据完整性 (0-0.1)：有温度数据更可靠
     */
    fun calculateConfidence(
        distanceMeters: Double,
        timeMinute: Double,
        heartRateZone: Int,
        hasTemperature: Boolean
    ): Double {
        val distanceKm = distanceMeters / 1000.0

        // 距离可靠性 (0-0.4)
        val distanceScore = when {
            distanceKm >= 5 && distanceKm <= 42 -> 0.4
            distanceKm >= 3 -> 0.3
            distanceKm >= 2 -> 0.2
            distanceKm >= 1 -> 0.1
            else -> 0.05
        }

        // 时长可靠性 (0-0.3)
        val durationScore = when {
            timeMinute >= 25 && timeMinute <= 120 -> 0.3
            timeMinute >= 15 -> 0.2
            timeMinute >= 10 -> 0.1
            else -> 0.05
        }

        // 心率区间适配性 (0-0.2)
        // zone 4(T) 和 zone 5(I) 最适合VDOT评估
        val hrZoneScore = when (heartRateZone) {
            4, 5 -> 0.2
            3 -> 0.15
            2 -> 0.10
            1 -> 0.05
            else -> 0.15 // zone 6-7，高强度也有参考价值
        }

        // 环境数据完整性 (0-0.1)
        val envScore = if (hasTemperature) 0.1 else 0.05

        return distanceScore + durationScore + hrZoneScore + envScore
    }

    // ==================== 心率调整 ====================

    /**
     * 获取心率区间（提取为独立方法以便复用）
     */
    private fun getHeartRateZone(heartRate: Double, maxHR: Double, restHR: Double): Int {
        if (heartRate <= 0) return 0
        val heartRateRanges = AbilityZoneCalculator.calculateHeartRate7Ranges(restHR, maxHR)
        return AbilityZoneCalculator.getZoneByHeartRate(heartRate, heartRateRanges)
    }

    /**
     * 根据心率区间调整VDOT
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

        val zone = getHeartRateZone(heartRate, maxHR, restHR)
        RLog.d(TAG, "心率区间: zone=$zone, HR=$heartRate")

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
        RLog.d(TAG, "心率调整VDOT: original=$originalVdot, adjusted=$adjustedVdot, factor=$factor")
        return adjustedVdot
    }

    // ==================== Overall VDOT计算 ====================

    /**
     * 基于历史VDOT数据计算Overall VDOT
     *
     * 改进：
     * - 基于实际天数的时间衰减（λ=0.03/天，半衰期≈23天）
     * - 置信度加权
     * - Modified Z-score + MAD 异常值检测
     * - 稳定性钳制（maxDelta=3.0）
     *
     * @param hisVdotList 历史VDOT列表（按日期倒序）
     * @param originVdot 当前原始VDOT
     * @param currentConfidence 当前VDOT的置信度
     * @param currentDateMs 当前记录的日期时间戳(ms)
     * @param totalDistance 总距离（公里）
     * @param activeDuration 运动时长（分钟）
     * @param previousOverallVdot 上一次的综合VDOT（用于钳制），null表示首次
     * @return Overall VDOT，无效返回null
     */
    fun calculateOverallVdot(
        hisVdotList: List<OverallVdotEntity>,
        originVdot: Double,
        currentConfidence: Double = 0.5,
        currentDateMs: Long = System.currentTimeMillis(),
        totalDistance: Double,
        activeDuration: Double,
        previousOverallVdot: Double? = null
    ): Double? {
        // 数据不足不计算
        if (originVdot <= 0 || (totalDistance <= 2 && activeDuration <= 15)) {
            return null
        }

        // 过滤已排除的记录（inclusiveLevel <= 0）
        val includedList = hisVdotList.filter { it.inclusiveLevel > 0 }

        // 收集所有VDOT值（当前 + 历史）用于异常值检测
        val allVdots = mutableListOf(originVdot)
        allVdots.addAll(includedList.map { it.originValue })

        // 异常值检测
        val outlierFlags = detectOutliers(allVdots)

        // 如果当前值是异常值，仍然计算但不会影响太多（因为钳制会限制）
        val currentIsOutlier = outlierFlags[0]

        val lambda = 0.03 // 衰减速率/天，半衰期≈23天
        val msPerDay = 24.0 * 60 * 60 * 1000

        // 当前记录：daysDiff=0，权重=confidence
        var weightedSum = originVdot * currentConfidence
        var totalWeight = currentConfidence

        for (i in includedList.indices) {
            // outlierFlags[i+1] 对应 includedList[i]
            if (outlierFlags[i + 1]) continue // 跳过异常值

            val entity = includedList[i]
            val daysDiff = (currentDateMs - entity.date).toDouble() / msPerDay
            val recencyWeight = exp(-lambda * daysDiff)
            val confidence = if (entity.confidence > 0) entity.confidence else 0.5
            val weight = recencyWeight * confidence

            weightedSum += entity.originValue * weight
            totalWeight += weight
        }

        if (totalWeight <= 0) return originVdot

        var result = weightedSum / totalWeight

        // 稳定性钳制：单次变化不超过 maxDelta
        if (previousOverallVdot != null && previousOverallVdot > 0) {
            val maxDelta = 3.0
            val delta = result - previousOverallVdot
            if (abs(delta) > maxDelta) {
                result = previousOverallVdot + if (delta > 0) maxDelta else -maxDelta
                RLog.d(TAG, "钳制生效: delta=$delta, clamped to $result")
            }
        }

        RLog.d(TAG, "Overall VDOT: origin=$originVdot, overall=$result, " +
                "historyCount=${includedList.size}, outliers=${outlierFlags.count { it }}")
        return result
    }

    // ==================== 异常值检测 ====================

    /**
     * 使用 Modified Z-score + MAD 检测异常值
     *
     * MAD（中位绝对偏差）比标准差更鲁棒
     * Modified Z-score > 3.0 标记为异常值
     *
     * @param values VDOT值列表
     * @return 布尔列表，true表示该值是异常值
     */
    internal fun detectOutliers(values: List<Double>): List<Boolean> {
        if (values.size < 3) {
            return List(values.size) { false } // 数据太少不检测
        }

        val sorted = values.sorted()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2]
        }

        // 计算 MAD
        val absoluteDeviations = values.map { abs(it - median) }
        val sortedDeviations = absoluteDeviations.sorted()
        val mad = if (sortedDeviations.size % 2 == 0) {
            (sortedDeviations[sortedDeviations.size / 2 - 1] + sortedDeviations[sortedDeviations.size / 2]) / 2.0
        } else {
            sortedDeviations[sortedDeviations.size / 2]
        }

        // MAD 为 0 或数据量不足时用 fallback
        val effectiveMad = if (mad < 0.001 || values.size < 5) {
            if (mad < 0.001) {
                // 所有值几乎相同，用 fallback scale
                5.0
            } else {
                mad
            }
        } else {
            mad
        }

        // Modified Z-score: 0.6745 * (x - median) / MAD
        val threshold = 3.0
        val result = values.map { value ->
            val modifiedZScore = 0.6745 * abs(value - median) / effectiveMad
            modifiedZScore > threshold
        }

        // 如果所有值都被标记为异常值，回退：不标记任何值
        if (result.all { it }) {
            return List(values.size) { false }
        }

        return result
    }

    // ==================== 间歇训练VDOT计算 ====================

    private fun isWorkType(type: String?): Boolean =
        type == "work" || type == "active"

    private fun isSteadyType(type: String?): Boolean =
        type == "warmup" || type == "cooldown"

    /**
     * 从间歇训练分段计算VDOT（返回VdotResult）
     *
     * 分组件独立评估策略：
     * 1. 稳态段（warmup/cooldown）：合并计算，适用于长距离稳态跑
     * 2. 工作段（work/active）：逐个计算 VDOT 取中位数，避免短距离聚合惩罚
     * 3. 取两者中较高的 VDOT 作为最终结果
     */
    fun calculateFromSegmentsWithResult(
        segments: List<RunSegmentEntity>,
        temperature: Double? = null,
        humidity: Double? = null,
        maxHR: Double,
        restHR: Double
    ): VdotResult? {
        val workSegments = segments.filter { isWorkType(it.intervalType) }
        val steadySegments = segments.filter { isSteadyType(it.intervalType) }

        RLog.i(TAG, "间歇分段分析: work=${workSegments.size}个, steady=${steadySegments.size}个")

        // 1. 稳态段 VDOT（warmup/cooldown 合并）
        val steadyResult = calculateSteadyStateVdot(
            steadySegments, temperature, humidity, maxHR, restHR
        )

        // 2. 工作段 VDOT（逐个计算取中位数）
        val intervalResult = calculateIntervalWorkVdot(
            workSegments, temperature, humidity, maxHR, restHR
        )

        RLog.i(TAG, "稳态VDOT: ${steadyResult?.vdot ?: "N/A"}, 间歇VDOT: ${intervalResult?.vdot ?: "N/A"}")

        // 3. 取较高值
        return when {
            steadyResult != null && intervalResult != null -> {
                if (intervalResult.vdot >= steadyResult.vdot) {
                    RLog.i(TAG, "选用间歇VDOT: ${intervalResult.vdot} >= ${steadyResult.vdot}")
                    intervalResult
                } else {
                    RLog.i(TAG, "选用稳态VDOT: ${steadyResult.vdot} > ${intervalResult.vdot}")
                    steadyResult
                }
            }
            intervalResult != null -> intervalResult
            steadyResult != null -> steadyResult
            else -> {
                RLog.w(TAG, "间歇训练数据不足，无法计算VDOT")
                null
            }
        }
    }

    /**
     * 从稳态段（warmup/cooldown）计算 VDOT
     * 要求：合并距离 >= 2km 且 合并时间 >= 10min
     */
    private fun calculateSteadyStateVdot(
        segments: List<RunSegmentEntity>,
        temperature: Double?,
        humidity: Double?,
        maxHR: Double,
        restHR: Double
    ): VdotResult? {
        if (segments.isEmpty()) return null

        var sumDistance = 0.0
        var sumTime = 0.0
        var sumHRxTime = 0.0

        for (seg in segments) {
            sumDistance += seg.distance
            sumTime += seg.activeDuration
            sumHRxTime += seg.averageHeartRate * seg.activeDuration
        }

        if (sumDistance < 2.0 || sumTime < 10.0 || sumHRxTime <= 0) {
            RLog.d(TAG, "稳态段数据不足: distance=${sumDistance}km, time=${sumTime}min")
            return null
        }

        val avgHR = sumHRxTime / sumTime
        return calculateWithResult(
            distanceMeters = sumDistance * 1000,
            timeMinute = sumTime,
            heartRate = avgHR,
            temperature = temperature,
            humidity = humidity,
            maxHR = maxHR,
            restHR = restHR
        )
    }

    /**
     * 从工作段计算间歇 VDOT
     *
     * 逐个工作段独立计算 VDOT，取中位数。
     * 要求：至少 2 个有效工作段，总时间 >= 1min
     */
    private fun calculateIntervalWorkVdot(
        workSegments: List<RunSegmentEntity>,
        temperature: Double?,
        humidity: Double?,
        maxHR: Double,
        restHR: Double
    ): VdotResult? {
        if (workSegments.size < 2) {
            RLog.d(TAG, "工作段数量不足: ${workSegments.size}")
            return null
        }

        val totalWorkTime = workSegments.sumOf { it.activeDuration }
        if (totalWorkTime < 1.0) {
            RLog.d(TAG, "工作段总时间不足: ${totalWorkTime}min")
            return null
        }

        // 逐个计算原始 VDOT
        val perIntervalVdots = mutableListOf<Double>()
        for (seg in workSegments) {
            if (seg.distance <= 0 || seg.activeDuration <= 0) continue
            val rawVdot = getVDot(seg.distance * 1000, seg.activeDuration)
            if (rawVdot > 0) {
                perIntervalVdots.add(rawVdot)
                RLog.d(TAG, "  工作段[${seg.seq}]: ${seg.distance}km, ${seg.activeDuration}min → rawVDOT=$rawVdot")
            }
        }

        if (perIntervalVdots.size < 2) {
            RLog.d(TAG, "有效工作段VDOT不足: ${perIntervalVdots.size}")
            return null
        }

        // 取中位数
        val medianRawVdot = calculateMedian(perIntervalVdots)

        // 温度修正：用总工作时间计算温度影响
        val tempEffect = getTemperatureEffect(temperature, humidity, totalWorkTime)
        val adjustedTime = if (totalWorkTime - tempEffect < 0) totalWorkTime else totalWorkTime - tempEffect
        val timeRatio = if (totalWorkTime > 0) adjustedTime / totalWorkTime else 1.0
        val envAdjustedVdot = medianRawVdot * timeRatio + medianRawVdot * (1 - timeRatio) * 0.5

        // 用工作段的加权平均心率做心率修正
        var sumHRxTime = 0.0
        var sumWorkTime = 0.0
        for (seg in workSegments) {
            if (seg.averageHeartRate > 0 && seg.activeDuration > 0) {
                sumHRxTime += seg.averageHeartRate * seg.activeDuration
                sumWorkTime += seg.activeDuration
            }
        }
        val avgWorkHR = if (sumWorkTime > 0) sumHRxTime / sumWorkTime else 0.0

        val finalVdot = if (avgWorkHR > 0) {
            adjustWithHeartRate(avgWorkHR, envAdjustedVdot, maxHR, restHR)
        } else {
            envAdjustedVdot
        }

        // 间歇训练的置信度：基于工作段数量和心率区间
        val zone = if (avgWorkHR > 0) getHeartRateZone(avgWorkHR, maxHR, restHR) else 0
        val countScore = when {
            perIntervalVdots.size >= 6 -> 0.35
            perIntervalVdots.size >= 4 -> 0.30
            perIntervalVdots.size >= 2 -> 0.20
            else -> 0.10
        }
        val hrZoneScore = when (zone) {
            4, 5 -> 0.20
            3 -> 0.15
            else -> 0.10
        }
        val durationScore = when {
            totalWorkTime >= 10 -> 0.25
            totalWorkTime >= 5 -> 0.20
            totalWorkTime >= 2 -> 0.15
            else -> 0.10
        }
        val envScore = if (temperature != null && temperature != 0.0) 0.10 else 0.05
        val confidence = countScore + hrZoneScore + durationScore + envScore

        RLog.i(TAG, "间歇VDOT: 中位数rawVDOT=$medianRawVdot, avgWorkHR=$avgWorkHR, " +
                "finalVdot=$finalVdot, intervals=${perIntervalVdots.size}, confidence=$confidence")

        return VdotResult(
            vdot = finalVdot,
            confidence = confidence,
            rawVdot = medianRawVdot,
            environmentalAdjustmentMinutes = tempEffect
        )
    }

    private fun calculateMedian(values: List<Double>): Double {
        val sorted = values.sorted()
        return if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2]
        }
    }

    /**
     * 从间歇训练分段计算VDOT（向后兼容）
     */
    fun calculateFromSegments(
        segments: List<RunSegmentEntity>,
        temperature: Double? = null,
        humidity: Double? = null,
        maxHR: Double,
        restHR: Double
    ): Double? {
        return calculateFromSegmentsWithResult(segments, temperature, humidity, maxHR, restHR)?.vdot
    }

    // ==================== 海拔调整（Phase 4 预留） ====================

    /**
     * 海拔调整因子（预留接口）
     * 待 RunRecordEntity 新增 averageAltitude 后激活
     *
     * @param altitudeMeters 平均海拔（米）
     * @return 调整因子，>1.0 表示海拔导致表现下降需要正向修正
     */
    @Suppress("unused")
    fun getAltitudeAdjustmentFactor(altitudeMeters: Double): Double {
        // 低于1000m不调整
        if (altitudeMeters < 1000) return 1.0
        // 每1000m VO2max下降约6-7%
        val altitudeAbove1000 = altitudeMeters - 1000
        return 1.0 + altitudeAbove1000 * 0.065 / 1000.0
    }

    // ==================== 内部辅助方法 ====================

    private fun getVO2(v: Double): Double {
        return 0.182258 * v + 0.000104 * v.pow(2) - 4.6
    }

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
