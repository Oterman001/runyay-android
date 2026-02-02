package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.local.entity.RunSamplePointEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import com.oterman.rundemo.util.FitDataConverter
import java.security.MessageDigest
import java.util.UUID

/**
 * FIT数据到Entity的转换映射器
 */
object FitDataMapper {
    
    private const val TAG = "FitDataMapper"
    
    /**
     * 将FIT解析结果转换为RunRecordEntity
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
            
            // 跑步动态
            averageVerticalOscillation = (session.avgVerticalOscillation ?: 0f) / 10.0,
            averageContactTime = session.avgStanceTime?.toDouble() ?: 0.0,
            
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
            datasource = FitDataConverter.Datasource.LOCAL_FIT,
            originId = fileName,
            
            // 状态
            trajectoryStatus = if (hasGps) FitDataConverter.TrajectoryStatus.EXISTS else FitDataConverter.TrajectoryStatus.NOT_EXISTS,
            inclusiveLevel = 1
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
     */
    fun toSegmentEntities(
        parseResult: FitParseResult,
        workoutId: String
    ): List<RunSegmentEntity> {
        val laps = parseResult.laps
        if (laps.isEmpty()) return emptyList()
        
        // 判断是否为公里自动分段
        val isKilometerMode = detectKilometerAutoLap(laps)
        val segmentType = if (isKilometerMode) 
            FitDataConverter.SegmentType.KILOMETER 
        else 
            FitDataConverter.SegmentType.TRAINING
        
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
                
                intervalType = convertIntensity(lap.intensity),
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

