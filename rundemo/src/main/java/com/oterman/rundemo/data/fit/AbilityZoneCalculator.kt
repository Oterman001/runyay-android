package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.local.entity.RunAbilityZoneEntity
import com.oterman.rundemo.util.FitDataConverter
import com.oterman.rundemo.util.RLog
import java.util.Locale

/**
 * 能力区间计算器
 * 对齐iOS AbilityZoneManager + RunSeriesHandler
 * 
 * 心率7区间 (HRR百分比):
 *  1: 恢复/热身   <59%
 *  2: 轻松跑(E)   59%~74%
 *  3: 马拉松配速(M) 74%~84%
 *  4: 乳酸阈值(T)  84%~88%
 *  5: 无氧耐力(A)  88%~95%
 *  6: 最大摄氧(I)  95%~100%
 *  7: 爆发力训练(R) >100%
 *
 * 心率5区间 (HRR百分比):
 *  1: 恢复区     <60%
 *  2: 有氧基础区  60%~70%
 *  3: 有氧动力区  70%~80%
 *  4: 无氧阈值区  80%~90%
 *  5: 最大摄氧量区 90%~100%
 *
 * 配速7区间 (基于VDOT):
 *  1: 恢复/热身
 *  2: 轻松跑(E)
 *  3: 马拉松配速(M)
 *  4: 乳酸阈值(T)
 *  5: 无氧耐力(A)
 *  6: 间歇(I)
 *  7: 冲刺(R)
 */
object AbilityZoneCalculator {

    private const val TAG = "AbilityZoneCalc"

    // ==================== 心率区间计算 ====================

    /**
     * 心率区间边界 (HRR百分比)
     */
    data class HeartRateRange(
        val minHR: Double,   // 下限心率(bpm)，-1表示无下限
        val maxHR: Double    // 上限心率(bpm)，-1表示无上限
    )

    /**
     * 计算心率7区间范围
     * 对齐iOS AbilityZoneManager.calculateHeartRateRange
     */
    fun calculateHeartRate7Ranges(restHR: Double, maxHR: Double): Map<Int, HeartRateRange> {
        val hrr = maxHR - restHR
        return mapOf(
            1 to HeartRateRange(-1.0, hrr * 0.59 + restHR),
            2 to HeartRateRange(hrr * 0.59 + restHR, hrr * 0.74 + restHR),
            3 to HeartRateRange(hrr * 0.74 + restHR, hrr * 0.84 + restHR),
            4 to HeartRateRange(hrr * 0.84 + restHR, hrr * 0.88 + restHR),
            5 to HeartRateRange(hrr * 0.88 + restHR, hrr * 0.95 + restHR),
            6 to HeartRateRange(hrr * 0.95 + restHR, hrr + restHR),
            7 to HeartRateRange(hrr + restHR, -1.0)
        )
    }

    /**
     * 计算心率5区间范围
     * 对齐iOS AbilityZoneManager.calculateHeartRateRange5Zone
     */
    fun calculateHeartRate5Ranges(restHR: Double, maxHR: Double): Map<Int, HeartRateRange> {
        val hrr = maxHR - restHR
        return mapOf(
            1 to HeartRateRange(-1.0, hrr * 0.60 + restHR),
            2 to HeartRateRange(hrr * 0.60 + restHR, hrr * 0.70 + restHR),
            3 to HeartRateRange(hrr * 0.70 + restHR, hrr * 0.80 + restHR),
            4 to HeartRateRange(hrr * 0.80 + restHR, hrr * 0.90 + restHR),
            5 to HeartRateRange(hrr * 0.90 + restHR, -1.0)
        )
    }

    /**
     * 根据心率值判断所在区间
     */
    fun getZoneByHeartRate(heartRate: Double, ranges: Map<Int, HeartRateRange>): Int {
        for ((zone, range) in ranges) {
            val aboveMin = range.minHR < 0 || heartRate >= range.minHR
            val belowMax = range.maxHR < 0 || heartRate <= range.maxHR
            if (aboveMin && belowMax) return zone
        }
        return -1
    }

    // ==================== 从Record数据计算区间时间分布 ====================

    /**
     * 从Record数据计算心率7区间时间分布
     * 对齐iOS RunSeriesHandler.getHeartRatezone
     *
     * @param records FIT Record数据
     * @param workoutId workoutId
     * @param startTimeMs 开始时间(ms)
     * @param restHR 静息心率
     * @param maxHR 最大心率
     * @param pauseList 暂停事件列表
     * @param isMale 性别(用于计算训练负荷)
     * @return 心率7区间Entity列表
     */
    fun calculateHeartRate7Zones(
        records: List<FitRecord>,
        workoutId: String,
        startTimeMs: Long,
        restHR: Double,
        maxHR: Double,
        pauseList: List<FitEventConverter.PauseEvent>,
        isMale: Boolean = true
    ): Pair<List<RunAbilityZoneEntity>, Double> {
        val ranges = calculateHeartRate7Ranges(restHR, maxHR)
        return calculateHeartRateZones(
            records = records,
            workoutId = workoutId,
            startTimeMs = startTimeMs,
            restHR = restHR,
            maxHR = maxHR,
            ranges = ranges,
            zoneType = FitDataConverter.ZoneType.HEART_RATE_7,
            pauseList = pauseList,
            isMale = isMale
        )
    }

    /**
     * 从Record数据计算心率5区间时间分布
     */
    fun calculateHeartRate5Zones(
        records: List<FitRecord>,
        workoutId: String,
        startTimeMs: Long,
        restHR: Double,
        maxHR: Double,
        pauseList: List<FitEventConverter.PauseEvent>
    ): List<RunAbilityZoneEntity> {
        val ranges = calculateHeartRate5Ranges(restHR, maxHR)
        return calculateHeartRateZones(
            records = records,
            workoutId = workoutId,
            startTimeMs = startTimeMs,
            restHR = restHR,
            maxHR = maxHR,
            ranges = ranges,
            zoneType = FitDataConverter.ZoneType.HEART_RATE_5,
            pauseList = pauseList
        ).first
    }

    /**
     * 从Record数据计算配速区间时间分布
     * 配速区间基于VDOT计算，如果VDOT无效则返回空列表
     *
     * @param records FIT Record数据
     * @param workoutId workoutId
     * @param startTimeMs 开始时间(ms)
     * @param vdot VDOT值
     * @param pauseList 暂停事件列表
     * @return 配速区间Entity列表
     */
    fun calculateSpeedZones(
        records: List<FitRecord>,
        workoutId: String,
        startTimeMs: Long,
        vdot: Double,
        pauseList: List<FitEventConverter.PauseEvent>
    ): List<RunAbilityZoneEntity> {
        if (vdot <= 0) {
            RLog.w(TAG, "VDOT无效($vdot)，跳过配速区间计算")
            return emptyList()
        }

        // 计算配速区间范围
        val speedRanges = VdotSpeedCalculator.calculateSpeedZoneRanges(vdot)
        if (speedRanges.isEmpty()) return emptyList()

        // 初始化区间时间映射
        val zoneTimeMap = speedRanges.keys.associateWith { 0.0 }.toMutableMap()
        var totalTime = 0.0
        var prevTimeMs = startTimeMs

        for (record in records) {
            val speed = record.speed ?: continue
            if (speed <= 0) continue

            val timestampMs = FitFileParser.fitTimestampToMillis(record.timestamp)
            // 配速: min/km
            val pace = FitFileParser.speedToPace(speed)
            if (pace <= 0 || pace > 30) {
                prevTimeMs = timestampMs
                continue
            }

            // 查找所属区间 (配速是min/km，值越小越快)
            val zone = getZoneBySpeed(pace, speedRanges)
            if (zone < 0) {
                prevTimeMs = timestampMs
                continue
            }

            val activeTimeSec = FitEventConverter.getActiveTimeBetween(prevTimeMs, timestampMs, pauseList)
            if (activeTimeSec > 0) {
                zoneTimeMap[zone] = (zoneTimeMap[zone] ?: 0.0) + activeTimeSec
                totalTime += activeTimeSec
            }
            prevTimeMs = timestampMs
        }

        val result = zoneTimeMap.map { (zone, timeSec) ->
            val range = speedRanges[zone]!!
            RunAbilityZoneEntity(
                workoutId = workoutId,
                zoneType = FitDataConverter.ZoneType.SPEED,
                zoneIndex = zone,
                duration = timeSec / 60.0,  // 转为分钟
                minValue = range.minPace,
                maxValue = range.maxPace
            )
        }.sortedBy { it.zoneIndex }

        RLog.i(TAG, "配速区间计算完成：${result.size}个区间，总时间=${String.format(Locale.getDefault(), "%.1f", totalTime / 60)}min")
        return result
    }

    // ==================== 私有方法 ====================

    /**
     * 通用心率区间计算
     * @return Pair<区间Entity列表, 训练负荷>
     */
    private fun calculateHeartRateZones(
        records: List<FitRecord>,
        workoutId: String,
        startTimeMs: Long,
        restHR: Double,
        maxHR: Double,
        ranges: Map<Int, HeartRateRange>,
        zoneType: Int,
        pauseList: List<FitEventConverter.PauseEvent>,
        isMale: Boolean = true
    ): Pair<List<RunAbilityZoneEntity>, Double> {
        val zoneTimeMap = ranges.keys.associateWith { 0.0 }.toMutableMap()
        var totalTime = 0.0
        var trainLoad = 0.0
        var prevTimeMs = startTimeMs

        // 训练负荷系数: 男1.67, 女1.92
        val k = if (isMale) 1.67 else 1.92

        for (record in records) {
            val hr = record.heartRate?.toDouble() ?: continue
            if (hr <= 0) continue

            val timestampMs = FitFileParser.fitTimestampToMillis(record.timestamp)

            val zone = getZoneByHeartRate(hr, ranges)
            if (zone < 0) {
                prevTimeMs = timestampMs
                continue
            }

            val activeTimeSec = FitEventConverter.getActiveTimeBetween(prevTimeMs, timestampMs, pauseList)
            if (activeTimeSec > 0) {
                zoneTimeMap[zone] = (zoneTimeMap[zone] ?: 0.0) + activeTimeSec
                totalTime += activeTimeSec

                // 计算训练负荷 (对齐iOS公式)
                val hrRatio = (hr - restHR) / (maxHR - restHR)
                trainLoad += hrRatio * Math.exp(hrRatio * k) * activeTimeSec / 60.0
            }
            prevTimeMs = timestampMs
        }

        val result = zoneTimeMap.map { (zone, timeSec) ->
            val range = ranges[zone]!!
            RunAbilityZoneEntity(
                workoutId = workoutId,
                zoneType = zoneType,
                zoneIndex = zone,
                duration = timeSec / 60.0,  // 转为分钟
                minValue = if (range.minHR < 0) 0.0 else range.minHR,
                maxValue = if (range.maxHR < 0) 999.0 else range.maxHR
            )
        }.sortedBy { it.zoneIndex }

        RLog.i(TAG, "心率区间(type=$zoneType)计算完成：${result.size}个区间，总时间=${String.format(Locale.getDefault(), "%.1f", totalTime / 60)}min，训练负荷=${String.format(Locale.getDefault(), "%.1f", trainLoad)}")
        return Pair(result, trainLoad)
    }

    /**
     * 配速区间范围
     */
    data class SpeedRange(
        val minPace: Double,   // 最慢配速(min/km)，-1表示无下限（更慢）
        val maxPace: Double    // 最快配速(min/km)，-1表示无上限（更快）
    )

    /**
     * 根据配速判断所在区间
     * 注意：配速是min/km，值越小越快
     */
    private fun getZoneBySpeed(pace: Double, speedRanges: Map<Int, SpeedRange>): Int {
        for ((zone, range) in speedRanges) {
            val slowerThanMin = range.minPace < 0 || pace <= range.minPace
            val fasterThanMax = range.maxPace < 0 || pace >= range.maxPace
            if (slowerThanMin && fasterThanMax) return zone
        }
        return -1
    }
}

