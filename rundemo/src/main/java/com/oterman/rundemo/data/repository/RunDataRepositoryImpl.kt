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
import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    // ==================== 用户隔离状态 ====================

    @Volatile
    private var currentUserId: String? = null

    private val _allRunRecords = MutableStateFlow<List<RunRecordEntity>>(emptyList())
    private var recordsCollectJob: Job? = null

    private fun requireUserId(): String {
        return currentUserId ?: throw IllegalStateException("userId not set, call setCurrentUserId() first")
    }

    override fun setCurrentUserId(userId: String?) {
        Log.i("RunDataRepository", "setCurrentUserId: $userId")
        currentUserId = userId
        rebuildRecordsFlow()
    }

    override suspend fun migrateOrphanedRecords(userId: String) {
        Log.i("RunDataRepository", "migrateOrphanedRecords to userId=$userId")
        runRecordDao.migrateOrphanedRecords(userId)
        pbRecordDao.migrateOrphanedRecords(userId)
        overallVdotDao.migrateOrphanedRecords(userId)
        rebuildRecordsFlow()
    }

    private fun rebuildRecordsFlow() {
        recordsCollectJob?.cancel()
        val uid = currentUserId
        if (uid == null) {
            _allRunRecords.value = emptyList()
            return
        }
        recordsCollectJob = repositoryScope.launch {
            runRecordDao.getAllByStartTimeDescForUser(uid).collect { records ->
                _allRunRecords.value = records
            }
        }
    }
    
    // ==================== 保存操作 ====================
    
    override suspend fun saveRunRecord(
        record: RunRecordEntity,
        samplePoints: List<RunSamplePointEntity>,
        segments: List<RunSegmentEntity>,
        zones: List<RunAbilityZoneEntity>
    ) {
        val taggedRecord = record.copy(userId = requireUserId())
        database.withTransaction {
            runRecordDao.insert(taggedRecord)
            if (samplePoints.isNotEmpty()) {
                samplePointDao.insertAll(samplePoints)
            }
            if (segments.isNotEmpty()) {
                segmentDao.insertAll(segments)
            }
            if (zones.isNotEmpty()) {
                abilityZoneDao.insertAll(zones)
            }
        }
    }

    override suspend fun saveRunRecord(record: RunRecordEntity) {
        runRecordDao.insert(record.copy(userId = requireUserId()))
    }

    override suspend fun updateRunRecord(record: RunRecordEntity) {
        runRecordDao.update(record.copy(userId = requireUserId()))
    }
    
    // ==================== 去重检查 ====================
    
    override suspend fun existsByOriginId(originId: String, datasource: String): Boolean {
        return runRecordDao.existsByOriginIdForUser(originId, datasource, requireUserId())
    }

    override suspend fun getByOriginId(originId: String, datasource: String): RunRecordEntity? {
        return runRecordDao.getByOriginIdForUser(originId, datasource, requireUserId())
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
        return _allRunRecords
    }

    override suspend fun getRunRecordsByTimeRange(startTime: Long, endTime: Long): List<RunRecordEntity> {
        return runRecordDao.getByTimeRangeForUser(requireUserId(), startTime, endTime)
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

    override suspend fun getAllAggregatedHeartRate7Zones(): List<AbilityZone> {
        val entities = abilityZoneDao.getAllHeartRate7ZonesForUser(requireUserId())
        return aggregateZones(entities, AbilityZoneType.HEART_RATE_7)
    }

    override suspend fun getAllAggregatedHeartRate5Zones(): List<AbilityZone> {
        val entities = abilityZoneDao.getAllHeartRate5ZonesForUser(requireUserId())
        return aggregateZones(entities, AbilityZoneType.HEART_RATE_5)
    }

    override suspend fun getAllAggregatedSpeedZones(): List<AbilityZone> {
        val entities = abilityZoneDao.getAllSpeedZonesForUser(requireUserId())
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
        return pbRecordDao.getBestRecordForUser(requireUserId(), type, subType)
    }

    override suspend fun savePBIfBetter(pbRecord: PBRecordEntity): Boolean {
        val uid = requireUserId()
        val taggedRecord = pbRecord.copy(userId = uid)
        val currentBest = pbRecordDao.getBestRecordForUser(uid, pbRecord.type, pbRecord.subType)

        val isBetter = if (currentBest == null) {
            true
        } else if (pbRecord.type == "Speed") {
            pbRecord.value < currentBest.value
        } else {
            pbRecord.value > currentBest.value
        }

        if (isBetter) {
            pbRecordDao.clearBestForTypeAndSubTypeForUser(uid, pbRecord.type, pbRecord.subType)
            pbRecordDao.insert(taggedRecord)
        }

        return isBetter
    }

    override suspend fun savePBRecords(records: List<PBRecordEntity>) {
        val uid = requireUserId()
        val workoutId = records.firstOrNull()?.workoutId ?: return
        pbRecordDao.deleteByWorkoutIdForUser(uid, workoutId)
        pbRecordDao.insertAll(records.map { it.copy(userId = uid) })
    }

    override suspend fun getAllPBByType(type: String): List<PBRecordEntity> {
        val uid = currentUserId ?: return emptyList()
        return pbRecordDao.getAllByTypeForUser(uid, type)
    }
    
    // ==================== VDOT ====================
    
    override suspend fun saveOverallVdot(vdot: OverallVdotEntity) {
        overallVdotDao.insert(vdot.copy(userId = requireUserId()))
    }

    override suspend fun getRecentVdots(limit: Int): List<OverallVdotEntity> {
        val uid = currentUserId ?: return emptyList()
        return overallVdotDao.getRecentVdotsForUser(uid, limit)
    }

    override suspend fun getLatestVdot(): OverallVdotEntity? {
        val uid = currentUserId ?: return null
        return overallVdotDao.getLatestVdotForUser(uid)
    }

    override suspend fun getVdotsByDateRange(startDate: Long, endDate: Long): List<OverallVdotEntity> {
        val uid = currentUserId ?: return emptyList()
        return overallVdotDao.getVdotsByDateRangeForUser(uid, startDate, endDate)
    }

    override suspend fun getAllVdotsByDateRange(startDate: Long, endDate: Long): List<OverallVdotEntity> {
        val uid = currentUserId ?: return emptyList()
        return overallVdotDao.getAllVdotsByDateRangeForUser(uid, startDate, endDate)
    }

    override suspend fun updateOverallVdotValue(workoutId: String, newValue: Double) {
        overallVdotDao.updateOverallValue(workoutId, newValue)
    }

    override suspend fun updateVdotInclusiveLevel(workoutId: String, level: Int) {
        overallVdotDao.updateInclusiveLevel(workoutId, level)
    }

    override suspend fun getVdotByWorkoutId(workoutId: String): OverallVdotEntity? {
        return overallVdotDao.getByWorkoutId(workoutId)
    }

    // ==================== 删除操作 ====================
    
    override suspend fun deleteRunRecord(workoutId: String) {
        runRecordDao.deleteByWorkoutId(workoutId)
        pbRecordDao.deleteByWorkoutId(workoutId)
        overallVdotDao.deleteByWorkoutId(workoutId)
    }

    override suspend fun deleteRunRecords(workoutIds: List<String>) {
        if (workoutIds.isEmpty()) return
        runRecordDao.deleteByWorkoutIds(workoutIds)
        workoutIds.forEach { workoutId ->
            pbRecordDao.deleteByWorkoutId(workoutId)
            overallVdotDao.deleteByWorkoutId(workoutId)
        }
    }

    override suspend fun getAllDatasources(): List<String> {
        return runRecordDao.getAllDatasourcesForUser(requireUserId())
    }

    override suspend fun getByDatasource(datasource: String): List<RunRecordEntity> {
        return runRecordDao.getByDatasourceForUser(requireUserId(), datasource)
    }

    // ==================== 上传状态 ====================

    override suspend fun getRecordsNeedingUpload(): List<RunRecordEntity> {
        return runRecordDao.getRecordsNeedingUploadForUser(requireUserId())
    }

    override suspend fun updateUploadStatus(workoutId: String, status: Int) {
        runRecordDao.updateUploadStatus(workoutId, status)
    }

    override suspend fun getConflictingRecords(newStartTime: Long, newEndTime: Long): List<RunRecordEntity> {
        return runRecordDao.getConflictingRecordsForUser(requireUserId(), newStartTime, newEndTime)
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

