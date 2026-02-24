package com.oterman.rundemo.data.repository

import com.oterman.rundemo.data.local.dao.OverallVdotDao
import com.oterman.rundemo.data.local.dao.PBRecordDao
import com.oterman.rundemo.data.local.dao.RunAbilityZoneDao
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.data.local.dao.RunSamplePointDao
import com.oterman.rundemo.data.local.dao.RunSegmentDao
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.local.entity.OverallVdotEntity
import com.oterman.rundemo.data.local.entity.PBRecordEntity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Date

/**
 * 跑步数据仓库实现（单例）
 */
class RunDataRepositoryImpl private constructor(
    private val database: RunDatabase
) : RunDataRepository {

    companion object {
        @Volatile
        private var INSTANCE: RunDataRepositoryImpl? = null

        fun getInstance(database: RunDatabase): RunDataRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RunDataRepositoryImpl(database).also { INSTANCE = it }
            }
        }
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val runRecordDao: RunRecordDao = database.runRecordDao()
    private val samplePointDao: RunSamplePointDao = database.runSamplePointDao()
    private val segmentDao: RunSegmentDao = database.runSegmentDao()
    private val abilityZoneDao: RunAbilityZoneDao = database.runAbilityZoneDao()
    private val pbRecordDao: PBRecordDao = database.pbRecordDao()
    private val overallVdotDao: OverallVdotDao = database.overallVdotDao()

    private val allRunRecords: StateFlow<List<RunRecordEntity>> =
        runRecordDao.getAllByStartTimeDesc()
            .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())
    
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
        return allRunRecords
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

    override suspend fun getStrideLengthSeries(workoutId: String): List<ChartDataPoint> {
        return samplePointDao.getStrideLengthSeries(workoutId).map {
            ChartDataPoint(
                timeOffset = it.timeOffset,
                value = it.strideLength ?: 0.0
            )
        }
    }

    override suspend fun getVerticalOscillationSeries(workoutId: String): List<ChartDataPoint> {
        return samplePointDao.getVerticalOscillationSeries(workoutId).map {
            ChartDataPoint(
                timeOffset = it.timeOffset,
                value = it.verticalOscillation ?: 0.0
            )
        }
    }

    override suspend fun getContactTimeSeries(workoutId: String): List<ChartDataPoint> {
        return samplePointDao.getContactTimeSeries(workoutId).map {
            ChartDataPoint(
                timeOffset = it.timeOffset,
                value = it.contactTime ?: 0.0
            )
        }
    }

    override suspend fun getAltitudeSeries(workoutId: String): List<ChartDataPoint> {
        return samplePointDao.getAltitudeSeries(workoutId).map {
            ChartDataPoint(
                timeOffset = it.timeOffset,
                value = it.altitude ?: 0.0
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

    override suspend fun getAggregatedHeartRate7Zones(workoutIds: List<String>): List<AbilityZone> {
        if (workoutIds.isEmpty()) return emptyList()
        val entities = abilityZoneDao.getHeartRate7ZonesByWorkoutIds(workoutIds)
        return aggregateZones(entities, AbilityZoneType.HEART_RATE_7)
    }

    override suspend fun getAggregatedHeartRate5Zones(workoutIds: List<String>): List<AbilityZone> {
        if (workoutIds.isEmpty()) return emptyList()
        val entities = abilityZoneDao.getHeartRate5ZonesByWorkoutIds(workoutIds)
        return aggregateZones(entities, AbilityZoneType.HEART_RATE_5)
    }

    override suspend fun getAggregatedSpeedZones(workoutIds: List<String>): List<AbilityZone> {
        if (workoutIds.isEmpty()) return emptyList()
        val entities = abilityZoneDao.getSpeedZonesByWorkoutIds(workoutIds)
        return aggregateZones(entities, AbilityZoneType.SPEED)
    }

    /**
     * 汇总多条记录的区间数据：按zoneIndex分组，累加duration，计算percentage
     * 对标iOS TotalHeartRateZone.getFromRecord / TotalSpeedZone.getFromRecord
     */
    private fun aggregateZones(
        entities: List<RunAbilityZoneEntity>,
        zoneType: AbilityZoneType
    ): List<AbilityZone> {
        if (entities.isEmpty()) return emptyList()

        // 按zoneIndex分组，累加duration，取第一条的minValue/maxValue作为区间范围
        val grouped = entities.groupBy { it.zoneIndex }
        val totalDuration = entities.sumOf { it.duration }
        if (totalDuration <= 0) return emptyList()

        return grouped.map { (zoneIndex, zoneEntities) ->
            val duration = zoneEntities.sumOf { it.duration }
            val first = zoneEntities.first()
            AbilityZone(
                zoneType = zoneType,
                zoneIndex = zoneIndex,
                duration = duration,
                minValue = first.minValue,
                maxValue = first.maxValue,
                percentage = duration / totalDuration
            )
        }.sortedBy { it.zoneIndex }
    }

    // ==================== PB记录 ====================
    
    override suspend fun getBestPB(type: String, subType: String): PBRecordEntity? {
        return pbRecordDao.getBestRecord(type, subType)
    }
    
    override suspend fun savePBIfBetter(pbRecord: PBRecordEntity): Boolean {
        val currentBest = pbRecordDao.getBestRecord(pbRecord.type, pbRecord.subType)
        
        // 对于Speed类型，值越小越好（用时更短）
        // 对于Ability类型，值越大越好（距离更远/步频更高等）
        val isBetter = if (currentBest == null) {
            true
        } else if (pbRecord.type == "Speed") {
            pbRecord.value < currentBest.value
        } else {
            pbRecord.value > currentBest.value
        }
        
        if (isBetter) {
            // 清除旧的PB，插入新的
            pbRecordDao.clearBestForTypeAndSubType(pbRecord.type, pbRecord.subType)
            pbRecordDao.insert(pbRecord)
        }
        
        return isBetter
    }
    
    override suspend fun savePBRecords(records: List<PBRecordEntity>) {
        pbRecordDao.insertAll(records)
    }
    
    override suspend fun getAllPBByType(type: String): List<PBRecordEntity> {
        return pbRecordDao.getAllByType(type)
    }
    
    // ==================== VDOT ====================
    
    override suspend fun saveOverallVdot(vdot: OverallVdotEntity) {
        overallVdotDao.insert(vdot)
    }
    
    override suspend fun getRecentVdots(limit: Int): List<OverallVdotEntity> {
        return overallVdotDao.getRecentVdots(limit)
    }
    
    override suspend fun getLatestVdot(): OverallVdotEntity? {
        return overallVdotDao.getLatestVdot()
    }

    override suspend fun getVdotsByDateRange(startDate: Long, endDate: Long): List<OverallVdotEntity> {
        return overallVdotDao.getVdotsByDateRange(startDate, endDate)
    }
    
    // ==================== 删除操作 ====================
    
    override suspend fun deleteRunRecord(workoutId: String) {
        runRecordDao.deleteByWorkoutId(workoutId)
    }

    override suspend fun deleteRunRecords(workoutIds: List<String>) {
        if (workoutIds.isEmpty()) return
        runRecordDao.deleteByWorkoutIds(workoutIds)
    }

    override suspend fun getAllDatasources(): List<String> {
        return runRecordDao.getAllDatasources()
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

