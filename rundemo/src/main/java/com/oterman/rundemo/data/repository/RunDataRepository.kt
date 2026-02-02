package com.oterman.rundemo.data.repository

import com.oterman.rundemo.data.local.entity.RunAbilityZoneEntity
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.local.entity.RunSamplePointEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.domain.model.ChartDataPoint
import com.oterman.rundemo.domain.model.RunRecord
import com.oterman.rundemo.domain.model.RunSegment
import com.oterman.rundemo.domain.model.TrackPoint
import kotlinx.coroutines.flow.Flow

/**
 * 跑步数据详情（包含完整数据）
 */
data class RunDetailData(
    val record: RunRecordEntity,
    val segments: List<RunSegmentEntity>,
    val zones: List<RunAbilityZoneEntity>,
    val trackPointCount: Int,
    val samplePointCount: Int
)

/**
 * 跑步数据仓库接口
 * 封装跑步数据的存储和读取操作
 */
interface RunDataRepository {
    
    // ==================== 保存操作 ====================
    
    /**
     * 保存完整的跑步数据（事务操作）
     * @param record 跑步记录
     * @param samplePoints 采样点列表
     * @param segments 分段列表
     * @param zones 能力区间列表
     */
    suspend fun saveRunRecord(
        record: RunRecordEntity,
        samplePoints: List<RunSamplePointEntity>,
        segments: List<RunSegmentEntity>,
        zones: List<RunAbilityZoneEntity>
    )
    
    /**
     * 只保存跑步记录（不含详细数据）
     */
    suspend fun saveRunRecord(record: RunRecordEntity)
    
    /**
     * 更新跑步记录
     */
    suspend fun updateRunRecord(record: RunRecordEntity)
    
    // ==================== 去重检查 ====================
    
    /**
     * 检查是否已存在相同来源的记录
     */
    suspend fun existsByOriginId(originId: String, datasource: String): Boolean
    
    /**
     * 根据原始ID获取记录
     */
    suspend fun getByOriginId(originId: String, datasource: String): RunRecordEntity?
    
    // ==================== 查询操作 ====================
    
    /**
     * 根据workoutId获取跑步记录
     */
    suspend fun getRunRecord(workoutId: String): RunRecordEntity?
    
    /**
     * 获取跑步记录详情（包含分段和区间）
     */
    suspend fun getRunDetail(workoutId: String): RunDetailData?
    
    /**
     * 获取所有跑步记录（按时间降序）
     */
    fun getAllRunRecords(): Flow<List<RunRecordEntity>>
    
    /**
     * 获取指定时间范围的记录
     */
    suspend fun getRunRecordsByTimeRange(startTime: Long, endTime: Long): List<RunRecordEntity>
    
    // ==================== GPS轨迹 ====================
    
    /**
     * 获取GPS轨迹点
     */
    suspend fun getTrackPoints(workoutId: String): List<TrackPoint>
    
    /**
     * 获取轨迹点数量
     */
    suspend fun getTrackPointCount(workoutId: String): Int
    
    /**
     * 获取所有采样点
     */
    suspend fun getSamplePoints(workoutId: String): List<RunSamplePointEntity>
    
    // ==================== 分段数据 ====================
    
    /**
     * 获取所有分段
     */
    suspend fun getSegments(workoutId: String): List<RunSegment>
    
    /**
     * 获取公里分段
     */
    suspend fun getKilometerSegments(workoutId: String): List<RunSegment>
    
    /**
     * 获取训练分段
     */
    suspend fun getTrainingSegments(workoutId: String): List<RunSegment>
    
    // ==================== 图表数据 ====================
    
    /**
     * 获取心率序列数据
     */
    suspend fun getHeartRateSeries(workoutId: String): List<ChartDataPoint>
    
    /**
     * 获取配速序列数据
     */
    suspend fun getSpeedSeries(workoutId: String): List<ChartDataPoint>
    
    /**
     * 获取步频序列数据
     */
    suspend fun getCadenceSeries(workoutId: String): List<ChartDataPoint>
    
    /**
     * 获取功率序列数据
     */
    suspend fun getPowerSeries(workoutId: String): List<ChartDataPoint>
    
    // ==================== 能力区间 ====================
    
    /**
     * 获取心率区间（7区间）
     */
    suspend fun getHeartRate7Zones(workoutId: String): List<AbilityZone>
    
    /**
     * 获取心率区间（5区间）
     */
    suspend fun getHeartRate5Zones(workoutId: String): List<AbilityZone>
    
    /**
     * 获取配速区间
     */
    suspend fun getSpeedZones(workoutId: String): List<AbilityZone>
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除跑步记录及所有关联数据（级联删除）
     */
    suspend fun deleteRunRecord(workoutId: String)
}

