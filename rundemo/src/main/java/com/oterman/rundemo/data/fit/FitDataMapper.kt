package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.local.entity.RunSamplePointEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import com.oterman.rundemo.util.FitDataConverter
import com.oterman.rundemo.util.RLog
import java.security.MessageDigest
import java.util.UUID

/**
 * FIT数据到Entity的转换映射器
 */
object FitDataMapper {
    
    private const val TAG = "FitDataMapper"
    
    /**
     * 将FIT解析结果转换为RunRecordEntity
     * 包含Session数据回退逻辑：当Session中某些字段为0时，从Record数据计算平均值
     */
    fun toRunRecordEntity(
        parseResult: FitParseResult,
        fileName: String
    ): RunRecordEntity? {
        val session = parseResult.session ?: return null
        
        val startTimeMs = FitFileParser.fitTimestampToMillis(session.startTime)
        val workoutId = generateWorkoutId(fileName, startTimeMs)
        val endTimeMs = startTimeMs + (session.totalElapsedTime * 1000).toLong()
        
        // 判断是否为室内跑步
        val isIndoor = session.subSport?.lowercase()?.let {
            it.contains("treadmill") || it.contains("indoor")
        } ?: false
        
        // 判断是否有GPS轨迹
        val hasGps = parseResult.records.any { it.positionLat != null && it.positionLong != null }
        
        // Session数据回退：当Session中垂直振幅/触地时间为0时，从Record数据计算
        val fallbackData = calculateFallbackFromRecords(session, parseResult.records)
        
        // 转换暂停事件为eventStr
        val eventStr = FitEventConverter.convertEventsToJson(parseResult.events)
        
        return RunRecordEntity(
            workoutId = workoutId,
            startTime = startTimeMs,
            endTime = endTimeMs,
            duration = session.totalElapsedTime / 60.0,
            activeDuration = session.totalTimerTime / 60.0,
            totalDistance = session.totalDistance / 1000.0,
            originDistance = session.totalDistance / 1000.0,
            
            // 配速：m/s转min/km
            averageSpeed = FitFileParser.speedToPace(session.avgSpeed),
            maxSpeed = FitFileParser.speedToPace(session.maxSpeed),
            
            // 心率
            averageHeartRate = session.avgHeartRate?.toDouble() ?: 0.0,
            maxHeartRate = session.maxHeartRate?.toDouble() ?: 0.0,
            minHeartRate = session.minHeartRate?.toDouble() ?: 0.0,
            
            // 功率
            averagePower = session.avgPower?.toDouble() ?: 0.0,
            maxPower = session.maxPower?.toDouble() ?: 0.0,
            
            // 步频（FIT中是单脚步频，需要*2）
            averageCadence = (session.avgCadence?.toDouble() ?: 0.0) * 2,
            // 步幅（mm转cm）
            averageStrideLength = (session.avgStepLength ?: 0f) / 10.0,
            
            // 跑步动态（使用回退数据）
            averageVerticalOscillation = fallbackData.verticalOscillation,
            averageContactTime = fallbackData.contactTime,
            
            // 消耗
            totalCalories = session.totalCalories?.toDouble() ?: 0.0,
            totalStepCount = (session.totalStrides ?: 0L).toDouble() * 2,
            elevationAscended = session.totalAscent?.toDouble() ?: 0.0,
            
            // 训练效果
            trainingEffect = session.trainingEffect?.toDouble() ?: 0.0,
            anaerobicTrainingEffect = session.anaerobicTrainingEffect?.toDouble() ?: 0.0,
            
            // 环境
            weatherTemperature = session.avgTemperature?.toDouble() ?: 0.0,
            outdoor = if (isIndoor) 1 else 0,
            
            // 设备信息
            deviceInfo = parseResult.deviceInfo?.manufacturer ?: parseResult.fileId?.manufacturer,
            deviceVersion = parseResult.deviceInfo?.product?.toString(),
            
            // 数据来源
            datasource = FitDataConverter.Datasource.MANUAL,
            originId = fileName,
            
            // 状态
            trajectoryStatus = if (hasGps) FitDataConverter.TrajectoryStatus.EXISTS else FitDataConverter.TrajectoryStatus.NOT_EXISTS,
            inclusiveLevel = 1,
            
            // 暂停事件JSON
            eventStr = eventStr.ifEmpty { null }
        )
    }
    
    /**
     * Session数据回退：计算从Record数据中的平均值
     * 对齐iOS: 当Session缺少verticalOscillation/stanceTime时，从RecordMesg计算
     */
    private data class FallbackData(
        val verticalOscillation: Double,
        val contactTime: Double
    )
    
    private fun calculateFallbackFromRecords(session: FitSession, records: List<FitRecord>): FallbackData {
        val sessionVO = (session.avgVerticalOscillation ?: 0f) / 10.0
        val sessionCT = session.avgStanceTime?.toDouble() ?: 0.0
        
        var finalVO = sessionVO
        var finalCT = sessionCT
        
        // 垂直振幅回退
        if (finalVO <= 0.0 && records.isNotEmpty()) {
            val voValues = records.mapNotNull { it.verticalOscillation?.toDouble() }
                .filter { it > 0 }
            if (voValues.isNotEmpty()) {
                finalVO = voValues.average() / 10.0  // mm转cm
                RLog.i(TAG, "Session垂直振幅为0，从Record回退计算: ${String.format("%.1f", finalVO)}cm (${voValues.size}个样本)")
            }
        }
        
        // 触地时间回退
        if (finalCT <= 0.0 && records.isNotEmpty()) {
            val ctValues = records.mapNotNull { it.stanceTime?.toDouble() }
                .filter { it > 0 }
            if (ctValues.isNotEmpty()) {
                finalCT = ctValues.average()
                RLog.i(TAG, "Session触地时间为0，从Record回退计算: ${String.format("%.0f", finalCT)}ms (${ctValues.size}个样本)")
            }
        }
        
        return FallbackData(
            verticalOscillation = finalVO,
            contactTime = finalCT
        )
    }
    
    /**
     * 将FIT记录转换为采样点Entity列表
     * 核心优化：同一时刻的数据合并为一行
     */
    fun toSamplePointEntities(
        parseResult: FitParseResult,
        workoutId: String,
        startTimeMs: Long
    ): List<RunSamplePointEntity> {
        return parseResult.records.mapIndexed { index, record ->
            val timestampMs = FitFileParser.fitTimestampToMillis(record.timestamp)
            
            RunSamplePointEntity(
                workoutId = workoutId,
                sequence = index,
                timestamp = timestampMs,
                timeOffset = ((timestampMs - startTimeMs) / 1000).toInt(),
                
                // GPS数据
                latitude = record.positionLat?.let { FitFileParser.semicirclesToDegrees(it) },
                longitude = record.positionLong?.let { FitFileParser.semicirclesToDegrees(it) },
                altitude = record.altitude?.toDouble(),
                
                // 运动指标
                heartRate = record.heartRate,
                power = record.power,
                speed = record.speed?.let { FitFileParser.speedToPace(it) },
                cadence = record.cadence?.let { it * 2 },  // 单脚转双脚
                strideLength = record.stepLength?.let { it.toDouble() / 10.0 },  // mm转cm
                verticalOscillation = record.verticalOscillation?.let { it.toDouble() / 10.0 },
                contactTime = record.stanceTime?.toDouble(),
                cumulativeDistance = record.distance?.toDouble()
            )
        }
    }
    
    /**
     * 将FIT分段转换为分段Entity列表
     * 对齐iOS逻辑：
     * - 公里自动分段模式 → Lap直接作为segmentType=1的公里分段
     * - 训练分段模式 → Lap作为segmentType=2的训练分段（含类型推断）+ 额外从Record计算segmentType=1的公里分段
     *
     * @param parseResult FIT解析结果
     * @param workoutId workoutId
     * @param startTimeMs 跑步开始时间(ms)
     * @param maxHR 最大心率(bpm)，用于分段类型推断
     * @param restHR 静息心率(bpm)，用于分段类型推断
     * @param pauseList 暂停事件列表，用于公里分段计算
     * @return 所有分段Entity列表
     */
    fun toSegmentEntities(
        parseResult: FitParseResult,
        workoutId: String,
        startTimeMs: Long = 0L,
        maxHR: Double = 200.0,
        restHR: Double = 60.0,
        pauseList: List<FitEventConverter.PauseEvent> = emptyList()
    ): List<RunSegmentEntity> {
        val laps = parseResult.laps
        if (laps.isEmpty()) return emptyList()
        
        // 判断是否为公里自动分段
        val isKilometerMode = detectKilometerAutoLap(laps)
        
        return if (isKilometerMode) {
            // 公里自动分段模式：Lap直接作为公里分段
            RLog.i(TAG, "检测到公里自动分段模式，${laps.size}个Lap直接转为公里分段")
            convertLapsToSegments(laps, workoutId, FitDataConverter.SegmentType.KILOMETER)
        } else {
            // 训练分段模式
            RLog.i(TAG, "检测到训练分段模式，${laps.size}个Lap")
            
            // 1. 将Lap转为训练分段（segmentType=2），并推断分段类型
            val trainingSegments = convertLapsToTrainingSegments(laps, workoutId, maxHR, restHR)
            
            // 2. 额外从Record数据计算公里分段（segmentType=1）
            val kmSegments = KilometerSegmentCalculator.calculateKilometerSegments(
                records = parseResult.records,
                workoutId = workoutId,
                startTimeMs = startTimeMs,
                pauseList = pauseList
            )
            
            RLog.i(TAG, "训练分段: ${trainingSegments.size}个, 公里分段: ${kmSegments.size}个")
            
            // 合并两种分段
            trainingSegments + kmSegments
        }
    }
    
    /**
     * 将Lap直接转为分段（公里自动分段模式）
     */
    private fun convertLapsToSegments(
        laps: List<FitLap>,
        workoutId: String,
        segmentType: Int
    ): List<RunSegmentEntity> {
        return laps.mapIndexed { index, lap ->
            val startTimeMs = FitFileParser.fitTimestampToMillis(lap.startTime)
            val endTimeMs = startTimeMs + (lap.totalElapsedTime * 1000).toLong()
            
            RunSegmentEntity(
                workoutId = workoutId,
                seq = index,
                segmentType = segmentType,
                beginTime = startTimeMs,
                endTime = endTimeMs,
                duration = lap.totalElapsedTime / 60.0,
                activeDuration = lap.totalTimerTime / 60.0,
                distance = lap.totalDistance / 1000.0,
                
                averageSpeed = FitFileParser.speedToPace(lap.avgSpeed),
                averageHeartRate = lap.avgHeartRate?.toDouble() ?: 0.0,
                averagePower = lap.avgPower?.toDouble() ?: 0.0,
                averageCadence = (lap.avgCadence?.toDouble() ?: 0.0) * 2,
                averageStrideLength = (lap.avgStepLength ?: 0f) / 10.0,
                averageVerticalOscillation = (lap.avgVerticalOscillation ?: 0f) / 10.0,
                averageContactTime = lap.avgStanceTime?.toDouble() ?: 0.0,
                stepCount = (lap.totalStrides ?: 0L).toDouble() * 2,
                
                intervalType = null,
                wktStepIndex = null
            )
        }
    }
    
    /**
     * 将Lap转为训练分段（含类型推断）
     * 对齐iOS逻辑：
     * - 如果Lap有intensity字段，直接使用
     * - 如果没有intensity，使用SegmentTypeInferenceEngine推断
     */
    private fun convertLapsToTrainingSegments(
        laps: List<FitLap>,
        workoutId: String,
        maxHR: Double,
        restHR: Double
    ): List<RunSegmentEntity> {
        // 检查是否所有Lap都有intensity信息
        val hasIntensity = laps.any { it.intensity != null }
        
        // 如果没有intensity信息，使用推断引擎
        val inferenceEngine = if (!hasIntensity) {
            RLog.i(TAG, "Lap无intensity信息，启用分段类型推断引擎")
            SegmentTypeInferenceEngine()
        } else {
            RLog.i(TAG, "Lap有intensity信息，直接使用")
            null
        }
        
        // 构建LapContext（用于推断引擎）
        val lapContexts = if (inferenceEngine != null) {
            laps.map { lap ->
                SegmentTypeInferenceEngine.LapContext(
                    avgHeartRate = lap.avgHeartRate?.toDouble() ?: 0.0,
                    avgPace = FitFileParser.speedToPace(lap.avgSpeed),
                    duration = lap.totalTimerTime.toDouble(),
                    distance = lap.totalDistance.toDouble()
                )
            }
        } else null
        
        return laps.mapIndexed { index, lap ->
            val startTimeMs = FitFileParser.fitTimestampToMillis(lap.startTime)
            val endTimeMs = startTimeMs + (lap.totalElapsedTime * 1000).toLong()
            
            // 确定分段类型
            val intervalType = if (hasIntensity && lap.intensity != null) {
                convertIntensity(lap.intensity)
            } else if (inferenceEngine != null && lapContexts != null) {
                inferenceEngine.inferSegmentType(
                    lapIndex = index,
                    lapContexts = lapContexts,
                    maxHR = maxHR,
                    restHR = restHR
                )
            } else {
                "work"  // 默认
            }
            
            RunSegmentEntity(
                workoutId = workoutId,
                seq = index,
                segmentType = FitDataConverter.SegmentType.TRAINING,
                beginTime = startTimeMs,
                endTime = endTimeMs,
                duration = lap.totalElapsedTime / 60.0,
                activeDuration = lap.totalTimerTime / 60.0,
                distance = lap.totalDistance / 1000.0,
                
                averageSpeed = FitFileParser.speedToPace(lap.avgSpeed),
                averageHeartRate = lap.avgHeartRate?.toDouble() ?: 0.0,
                averagePower = lap.avgPower?.toDouble() ?: 0.0,
                averageCadence = (lap.avgCadence?.toDouble() ?: 0.0) * 2,
                averageStrideLength = (lap.avgStepLength ?: 0f) / 10.0,
                averageVerticalOscillation = (lap.avgVerticalOscillation ?: 0f) / 10.0,
                averageContactTime = lap.avgStanceTime?.toDouble() ?: 0.0,
                stepCount = (lap.totalStrides ?: 0L).toDouble() * 2,
                
                intervalType = intervalType,
                wktStepIndex = lap.wktStepIndex?.let { it + 1 }
            )
        }
    }
    
    /**
     * 检测是否为公里自动分段模式
     */
    private fun detectKilometerAutoLap(laps: List<FitLap>): Boolean {
        if (laps.size < 2) return false
        
        // 排除最后一个Lap（通常不足1km）
        val lapsToCheck = laps.dropLast(1)
        
        val minDistance = 950f  // 0.95km
        val maxDistance = 1050f // 1.05km
        
        return lapsToCheck.all { lap ->
            lap.totalDistance >= minDistance && lap.totalDistance <= maxDistance
        }
    }
    
    /**
     * 转换强度类型
     */
    private fun convertIntensity(intensity: Int?): String? {
        return when (intensity) {
            0 -> "active"
            1 -> "rest"
            2 -> "warmup"
            3 -> "cooldown"
            4 -> "recovery"
            5 -> "work"
            else -> null
        }
    }
    
    /**
     * 生成确定性的workoutId
     */
    private fun generateWorkoutId(fileName: String, startTime: Long): String {
        val input = "$fileName-$startTime"
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(input.toByteArray())
        return UUID.nameUUIDFromBytes(hashBytes.copyOf(16)).toString()
    }
}

