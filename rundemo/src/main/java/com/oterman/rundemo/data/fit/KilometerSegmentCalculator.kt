package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import com.oterman.rundemo.util.FitDataConverter
import com.oterman.rundemo.util.RLog

/**
 * 公里分段计算器
 * 对齐iOS GarminKilometerSegmentCalculator
 * 从Record数据计算每公里的分段数据，用于训练模式下额外计算公里分段
 */
object KilometerSegmentCalculator {

    private const val TAG = "KmSegmentCalc"

    /**
     * 从Record数据计算公里分段
     * 遍历Record数据，当累积距离达到1km时创建一个分段
     *
     * @param records FIT解析的Record列表
     * @param workoutId workoutId
     * @param startTimeMs 跑步开始时间(ms)
     * @param pauseList 暂停事件列表
     * @param maxDistanceM Session总距离上限(米)，超出则停止生成分段，防止FIT文件record.distance异常导致分段数量翻倍
     * @return 公里分段Entity列表（segmentType=1）
     */
    fun calculateKilometerSegments(
        records: List<FitRecord>,
        workoutId: String,
        startTimeMs: Long,
        pauseList: List<FitEventConverter.PauseEvent>,
        maxDistanceM: Double? = null
    ): List<RunSegmentEntity> {
        if (records.isEmpty()) {
            RLog.w(TAG, "Record列表为空，无法计算公里分段")
            return emptyList()
        }

        RLog.i(TAG, "开始计算公里分段: records=${records.size}个, startTimeMs=$startTimeMs, pauseList=${pauseList.size}个, maxDistanceM=$maxDistanceM")

        val segments = mutableListOf<RunSegmentEntity>()
        var currentKm = 1
        var segmentStartIndex = 0
        var segmentStartDistance = 0.0  // 米
        var segmentStartTimeMs = records.firstOrNull()?.let {
            FitFileParser.fitTimestampToMillis(it.timestamp)
        } ?: startTimeMs

        for ((index, record) in records.withIndex()) {
            val currentDistance = record.distance?.toDouble() ?: continue

            val timestampMs = FitFileParser.fitTimestampToMillis(record.timestamp)
            val kmThreshold = currentKm * 1000.0

            // 下一个公里里程碑已超出Session总距离时停止创建完整公里段——
            // 例如 maxDistanceM=10766m 时，km11 threshold=11000 > 10766，应停止，
            // 由后续尾段逻辑生成 10000→10766m 的 0.766km 尾段
            if (maxDistanceM != null && kmThreshold > maxDistanceM) break

            if (currentDistance >= kmThreshold) {
                // 达到公里点，创建分段
                val segment = createSegment(
                    seq = currentKm - 1,
                    startIndex = segmentStartIndex,
                    endIndex = index,
                    startDistanceM = segmentStartDistance,
                    endDistanceM = kmThreshold,
                    startTimeMs = segmentStartTimeMs,
                    endTimeMs = timestampMs,
                    records = records,
                    workoutId = workoutId,
                    pauseList = pauseList
                )
                segments.add(segment)

                currentKm++
                segmentStartIndex = index
                segmentStartDistance = kmThreshold
                segmentStartTimeMs = timestampMs
            }
        }

        // 最后一段（不足1公里）：若已受maxDistanceM约束提前退出，则尾段使用maxDistanceM作为终止距离
        val lastRecord = records.lastOrNull()
        if (lastRecord != null && segmentStartIndex < records.size - 1) {
            val rawLastDistance = lastRecord.distance?.toDouble()
            val lastDistance = if (maxDistanceM != null && rawLastDistance != null)
                minOf(rawLastDistance, maxDistanceM)
            else
                rawLastDistance
            val lastTimeMs = FitFileParser.fitTimestampToMillis(lastRecord.timestamp)
            if (lastDistance != null && lastDistance > segmentStartDistance) {
                val segment = createSegment(
                    seq = currentKm - 1,
                    startIndex = segmentStartIndex,
                    endIndex = records.size - 1,
                    startDistanceM = segmentStartDistance,
                    endDistanceM = lastDistance,
                    startTimeMs = segmentStartTimeMs,
                    endTimeMs = lastTimeMs,
                    records = records,
                    workoutId = workoutId,
                    pauseList = pauseList
                )
                segments.add(segment)
            }
        }

        RLog.i(TAG, "成功计算${segments.size}个公里分段")
        return segments
    }

    // ==================== 私有方法 ====================

    /**
     * 累加分段内相邻Record之间的正向时间差，跳过时间戳倒退的间隔
     */
    private fun computeRecordsDurationSec(records: List<FitRecord>, startIndex: Int, endIndex: Int): Double {
        var totalMs = 0L
        val safeEnd = minOf(endIndex, records.size - 1)
        for (i in startIndex until safeEnd) {
            val t1 = FitFileParser.fitTimestampToMillis(records[i].timestamp)
            val t2 = FitFileParser.fitTimestampToMillis(records[i + 1].timestamp)
            val delta = t2 - t1
            if (delta > 0) totalMs += delta
        }
        return totalMs / 1000.0
    }

    private fun createSegment(
        seq: Int,
        startIndex: Int,
        endIndex: Int,
        startDistanceM: Double,
        endDistanceM: Double,
        startTimeMs: Long,
        endTimeMs: Long,
        records: List<FitRecord>,
        workoutId: String,
        pauseList: List<FitEventConverter.PauseEvent>
    ): RunSegmentEntity {
        // 距离（米转公里）
        val distanceKm = (endDistanceM - startDistanceM) / 1000.0

        // 用Record间正向时间差计算时长，自动跳过时间戳倒退的间隔
        val segmentDurationSec = computeRecordsDurationSec(records, startIndex, endIndex)
        val totalDurationMin = segmentDurationSec / 60.0

        // 减去暂停时间
        val pauseDurationSec = calculatePauseDuration(startTimeMs, endTimeMs, pauseList)
        val activeDurationMin = (segmentDurationSec - pauseDurationSec) / 60.0

        // 收集分段内的数据点
        val safeEndIndex = minOf(endIndex + 1, records.size)
        val segmentRecords = records.subList(startIndex, safeEndIndex)

        // 使用时间加权平均计算各指标（对齐iOS的calculateWeightedAverage）
        val avgHR = calculateTimeWeightedAverage(segmentRecords) { it.heartRate?.toDouble() }
        val avgPower = calculateTimeWeightedAverage(segmentRecords) { it.power?.toDouble() }
        // 步频: FIT中是单脚，需要*2
        val avgCadence = calculateTimeWeightedAverage(segmentRecords) { it.cadence?.toDouble() }?.let { it * 2 } ?: 0.0
        // 步幅: mm转cm
        val avgStrideLength = calculateTimeWeightedAverage(segmentRecords) { it.stepLength?.toDouble() }?.let { it / 10.0 } ?: 0.0
        // 垂直振幅: mm转cm
        val avgVO = calculateTimeWeightedAverage(segmentRecords) { it.verticalOscillation?.toDouble() }?.let { it / 10.0 } ?: 0.0
        // 触地时间
        val avgContactTime = calculateTimeWeightedAverage(segmentRecords) { it.stanceTime?.toDouble() } ?: 0.0

        // 步数累加 (FIT cadence是每秒半步数cycles, 需要*2)
        // 简单累加cadence作为近似步数 (实际iOS用cycles字段)
        val totalStepCount = segmentRecords.mapNotNull { it.cadence?.toDouble() }
            .filter { it > 0 }
            .sum() * 2.0

        // 平均配速：优先使用距离/活跃时间计算
        val avgSpeed = if (distanceKm > 0 && activeDurationMin > 0) {
            activeDurationMin / distanceKm  // min/km
        } else {
            // 备用：使用加权平均速度计算
            val avgSpeedMs = calculateTimeWeightedAverage(segmentRecords) { it.speed?.toDouble() }
            if (avgSpeedMs != null && avgSpeedMs > 0) {
                (1000.0 / avgSpeedMs) / 60.0  // m/s → min/km
            } else {
                0.0
            }
        }

        RLog.d(TAG, "分段[seq=$seq]: distance=${String.format("%.3f", distanceKm)}km, " +
            "activeDuration=${String.format("%.2f", activeDurationMin)}min, " +
            "totalDuration=${String.format("%.2f", totalDurationMin)}min, " +
            "startMs=$startTimeMs, endMs=$endTimeMs, " +
            "pauseSec=${String.format("%.1f", pauseDurationSec)}")

        return RunSegmentEntity(
            workoutId = workoutId,
            seq = seq,
            segmentType = FitDataConverter.SegmentType.KILOMETER,
            beginTime = startTimeMs,
            endTime = endTimeMs,
            duration = totalDurationMin,
            activeDuration = activeDurationMin,
            distance = distanceKm,
            averageSpeed = avgSpeed,
            averageHeartRate = avgHR ?: 0.0,
            averagePower = avgPower ?: 0.0,
            averageCadence = avgCadence,
            averageStrideLength = avgStrideLength,
            averageVerticalOscillation = avgVO,
            averageContactTime = avgContactTime,
            stepCount = totalStepCount,
            intervalType = null,
            wktStepIndex = null
        )
    }

    /**
     * 时间加权平均计算
     * 对齐iOS GarminKilometerSegmentCalculator.calculateWeightedAverage
     * 每个数据点的权重 = 该点到下一个点的时间间隔
     *
     * @param records 分段内的Record数据
     * @param valueGetter 值提取lambda
     * @return 加权平均值，无有效数据返回null
     */
    private fun calculateTimeWeightedAverage(
        records: List<FitRecord>,
        valueGetter: (FitRecord) -> Double?
    ): Double? {
        if (records.isEmpty()) return null

        var weightedSum = 0.0
        var totalTime = 0.0
        var prevTimeMs: Long? = null

        for ((index, record) in records.withIndex()) {
            val value = valueGetter(record) ?: continue
            val timestampMs = FitFileParser.fitTimestampToMillis(record.timestamp)

            // 跳过无效数据：0、负数、无穷大、NaN
            if (value <= 0 || value.isInfinite() || value.isNaN()) continue

            // 计算当前点到下一个点的时间间隔（秒）
            val interval: Double
            if (index < records.size - 1) {
                // 有下一个点，使用到下一个点的时间间隔
                val nextTimeMs = FitFileParser.fitTimestampToMillis(records[index + 1].timestamp)
                interval = (nextTimeMs - timestampMs) / 1000.0
            } else {
                // 最后一个点：使用与上一个点相同的间隔
                if (prevTimeMs != null) {
                    interval = (timestampMs - prevTimeMs) / 1000.0
                } else {
                    // 只有一个点，无法计算
                    continue
                }
            }

            if (interval <= 0) {
                prevTimeMs = timestampMs
                continue
            }

            weightedSum += value * interval
            totalTime += interval
            prevTimeMs = timestampMs
        }

        return if (totalTime > 0) weightedSum / totalTime else null
    }

    /**
     * 计算分段内的暂停时长（秒）
     */
    private fun calculatePauseDuration(
        startTimeMs: Long,
        endTimeMs: Long,
        pauseList: List<FitEventConverter.PauseEvent>
    ): Double {
        var totalPause = 0.0
        for (pause in pauseList) {
            val overlapStart = maxOf(pause.beginTimeMs, startTimeMs)
            val overlapEnd = minOf(pause.endTimeMs, endTimeMs)
            if (overlapStart < overlapEnd) {
                totalPause += (overlapEnd - overlapStart) / 1000.0
            }
        }
        return totalPause
    }
}
