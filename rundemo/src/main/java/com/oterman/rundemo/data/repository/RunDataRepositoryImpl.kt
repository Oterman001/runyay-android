package com.oterman.rundemo.data.repository

import com.oterman.rundemo.data.local.dao.RunAbilityZoneDao
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.data.local.dao.RunSamplePointDao
import com.oterman.rundemo.data.local.dao.RunSegmentDao
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.local.entity.RunAbilityZoneEntity
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.local.entity.RunSamplePointEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.domain.model.AbilityZoneType
import com.oterman.rundemo.domain.model.ChartDataPoint
import com.oterman.rundemo.domain.model.IntervalType
import com.oterman.rundemo.domain.model.RunSegment
import com.oterman.rundemo.domain.model.SegmentType
import com.oterman.rundemo.domain.model.TrackPoint
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 跑步数据仓库实现
 */
class RunDataRepositoryImpl(
    private val database: RunDatabase
) : RunDataRepository {
    
    private val runRecordDao: RunRecordDao = database.runRecordDao()
    private val samplePointDao: RunSamplePointDao = database.runSamplePointDao()
    private val segmentDao: RunSegmentDao = database.runSegmentDao()
    private val abilityZoneDao: RunAbilityZoneDao = database.runAbilityZoneDao()
    
    // ==================== 保存操作 ====================
    
    override suspend fun saveRunRecord(
        record: RunRecordEntity,
        samplePoints: List<RunSamplePointEntity>,
        segments: List<RunSegmentEntity>,
        zones: List<RunAbilityZoneEntity>
    ) {
        // 使用事务保存所有数据
        database.withTransaction {
            // 1. 保存主记录
            runRecordDao.insert(record)
            
            // 2. 批量保存采样点
            if (samplePoints.isNotEmpty()) {
                samplePointDao.insertAll(samplePoints)
            }
            
            // 3. 保存分段
            if (segments.isNotEmpty()) {
                segmentDao.insertAll(segments)
            }
            
            // 4. 保存能力区间
            if (zones.isNotEmpty()) {
                abilityZoneDao.insertAll(zones)
            }
        }
    }
    
    override suspend fun saveRunRecord(record: RunRecordEntity) {
        runRecordDao.insert(record)
    }
    
    override suspend fun updateRunRecord(record: RunRecordEntity) {
        runRecordDao.update(record)
    }
    
    // ==================== 去重检查 ====================
    
    override suspend fun existsByOriginId(originId: String, datasource: String): Boolean {
        return runRecordDao.existsByOriginId(originId, datasource)
    }
    
    override suspend fun getByOriginId(originId: String, datasource: String): RunRecordEntity? {
        return runRecordDao.getByOriginId(originId, datasource)
    }
    
    // ==================== 查询操作 ====================
    
    override suspend fun getRunRecord(workoutId: String): RunRecordEntity? {
        return runRecordDao.getByWorkoutId(workoutId)
    }
    
    override suspend fun getRunDetail(workoutId: String): RunDetailData? {
        val record = runRecordDao.getByWorkoutId(workoutId) ?: return null
        val segments = segmentDao.getByWorkoutId(workoutId)
        val zones = abilityZoneDao.getByWorkoutId(workoutId)
        val trackPointCount = samplePointDao.getTrackPointCount(workoutId)
        val samplePointCount = samplePointDao.getCountByWorkoutId(workoutId)
        
        return RunDetailData(
            record = record,
            segments = segments,
            zones = zones,
            trackPointCount = trackPointCount,
            samplePointCount = samplePointCount
        )
    }
    
    override fun getAllRunRecords(): Flow<List<RunRecordEntity>> {
        return runRecordDao.getAllByStartTimeDesc()
    }
    
    override suspend fun getRunRecordsByTimeRange(startTime: Long, endTime: Long): List<RunRecordEntity> {
        return runRecordDao.getByTimeRange(startTime, endTime)
    }
    
    // ==================== GPS轨迹 ====================
    
    override suspend fun getTrackPoints(workoutId: String): List<TrackPoint> {
        return samplePointDao.getTrackPoints(workoutId).map { entity ->
            TrackPoint(
                latitude = entity.latitude ?: 0.0,
                longitude = entity.longitude ?: 0.0,
                altitude = entity.altitude,
                timestamp = entity.timestamp,
                timeOffset = entity.timeOffset,
                heartRate = entity.heartRate,
                speed = entity.speed,
                cadence = entity.cadence
            )
        }
    }
    
    override suspend fun getTrackPointCount(workoutId: String): Int {
        return samplePointDao.getTrackPointCount(workoutId)
    }
    
    override suspend fun getSamplePoints(workoutId: String): List<RunSamplePointEntity> {
        return samplePointDao.getByWorkoutId(workoutId)
    }
    
    // ==================== 分段数据 ====================
    
    override suspend fun getSegments(workoutId: String): List<RunSegment> {
        return segmentDao.getByWorkoutId(workoutId).map { it.toRunSegment() }
    }
    
    override suspend fun getKilometerSegments(workoutId: String): List<RunSegment> {
        return segmentDao.getKilometerSegments(workoutId).map { it.toRunSegment() }
    }
    
    override suspend fun getTrainingSegments(workoutId: String): List<RunSegment> {
        return segmentDao.getTrainingSegments(workoutId).map { it.toRunSegment() }
    }
    
    // ==================== 图表数据 ====================
    
    override suspend fun getHeartRateSeries(workoutId: String): List<ChartDataPoint> {
        return samplePointDao.getHeartRateSeries(workoutId).map {
            ChartDataPoint(
                timeOffset = it.timeOffset,
                value = it.heartRate?.toDouble() ?: 0.0
            )
        }
    }
    
    override suspend fun getSpeedSeries(workoutId: String): List<ChartDataPoint> {
        return samplePointDao.getSpeedSeries(workoutId).map {
            ChartDataPoint(
                timeOffset = it.timeOffset,
                value = it.speed ?: 0.0
            )
        }
    }
    
    override suspend fun getCadenceSeries(workoutId: String): List<ChartDataPoint> {
        return samplePointDao.getCadenceSeries(workoutId).map {
            ChartDataPoint(
                timeOffset = it.timeOffset,
                value = it.cadence?.toDouble() ?: 0.0
            )
        }
    }
    
    override suspend fun getPowerSeries(workoutId: String): List<ChartDataPoint> {
        return samplePointDao.getPowerSeries(workoutId).map {
            ChartDataPoint(
                timeOffset = it.timeOffset,
                value = it.power?.toDouble() ?: 0.0
            )
        }
    }
    
    // ==================== 能力区间 ====================
    
    override suspend fun getHeartRate7Zones(workoutId: String): List<AbilityZone> {
        return abilityZoneDao.getHeartRate7Zones(workoutId).map { it.toAbilityZone() }
    }
    
    override suspend fun getHeartRate5Zones(workoutId: String): List<AbilityZone> {
        return abilityZoneDao.getHeartRate5Zones(workoutId).map { it.toAbilityZone() }
    }
    
    override suspend fun getSpeedZones(workoutId: String): List<AbilityZone> {
        return abilityZoneDao.getSpeedZones(workoutId).map { it.toAbilityZone() }
    }
    
    // ==================== 删除操作 ====================
    
    override suspend fun deleteRunRecord(workoutId: String) {
        // 由于设置了ForeignKey CASCADE，删除主记录会自动删除关联数据
        runRecordDao.deleteByWorkoutId(workoutId)
    }
    
    // ==================== 扩展函数 ====================
    
    private fun RunSegmentEntity.toRunSegment(): RunSegment {
        return RunSegment(
            seq = seq,
            segmentType = SegmentType.fromValue(segmentType),
            beginTime = Date(beginTime),
            endTime = Date(endTime),
            duration = duration,
            activeDuration = activeDuration,
            distance = distance,
            averageSpeed = averageSpeed,
            averageHeartRate = averageHeartRate,
            averagePower = averagePower,
            averageCadence = averageCadence,
            averageStrideLength = averageStrideLength,
            averageVerticalOscillation = averageVerticalOscillation,
            averageContactTime = averageContactTime,
            stepCount = stepCount,
            intervalType = IntervalType.fromValue(intervalType),
            wktStepIndex = wktStepIndex,
            displayName = displayName
        )
    }
    
    private fun RunAbilityZoneEntity.toAbilityZone(): AbilityZone {
        return AbilityZone(
            zoneType = AbilityZoneType.fromValue(zoneType) ?: AbilityZoneType.HEART_RATE_7,
            zoneIndex = zoneIndex,
            duration = duration,
            minValue = minValue,
            maxValue = maxValue
        )
    }
}

