package com.oterman.rundemo.data.repository

import com.oterman.rundemo.data.local.entity.OverallVdotEntity
import com.oterman.rundemo.data.local.entity.PBRecordEntity
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

    /**
     * 获取步幅序列数据
     */
    suspend fun getStrideLengthSeries(workoutId: String): List<ChartDataPoint>

    /**
     * 获取垂直振幅序列数据
     */
    suspend fun getVerticalOscillationSeries(workoutId: String): List<ChartDataPoint>

    /**
     * 获取触地时间序列数据
     */
    suspend fun getContactTimeSeries(workoutId: String): List<ChartDataPoint>

    /**
     * 获取海拔序列数据
     */
    suspend fun getAltitudeSeries(workoutId: String): List<ChartDataPoint>
    
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

    /**
     * 批量获取心率7区间并汇总（周/月统计用）
     */
    suspend fun getAggregatedHeartRate7Zones(workoutIds: List<String>): List<AbilityZone>

    /**
     * 批量获取心率5区间并汇总（周/月统计用）
     */
    suspend fun getAggregatedHeartRate5Zones(workoutIds: List<String>): List<AbilityZone>

    /**
     * 批量获取配速区间并汇总（周/月统计用）
     */
    suspend fun getAggregatedSpeedZones(workoutIds: List<String>): List<AbilityZone>

    /**
     * 获取全部心率7区间并汇总（Total页面用）
     */
    suspend fun getAllAggregatedHeartRate7Zones(): List<AbilityZone>

    /**
     * 获取全部心率5区间并汇总（Total页面用）
     */
    suspend fun getAllAggregatedHeartRate5Zones(): List<AbilityZone>

    /**
     * 获取全部配速区间并汇总（Total页面用）
     */
    suspend fun getAllAggregatedSpeedZones(): List<AbilityZone>

    // ==================== PB记录 ====================
    
    /**
     * 获取某类型某子类型的当前最佳PB
     */
    suspend fun getBestPB(type: String, subType: String): PBRecordEntity?
    
    /**
     * 保存PB记录（如果是新的PB则替换旧的）
     */
    suspend fun savePBIfBetter(pbRecord: PBRecordEntity): Boolean
    
    /**
     * 批量保存PB记录
     */
    suspend fun savePBRecords(records: List<PBRecordEntity>)
    
    /**
     * 获取某类型所有PB记录
     */
    suspend fun getAllPBByType(type: String): List<PBRecordEntity>
    
    // ==================== VDOT ====================
    
    /**
     * 保存VDOT记录
     */
    suspend fun saveOverallVdot(vdot: OverallVdotEntity)
    
    /**
     * 获取最近N条VDOT记录
     */
    suspend fun getRecentVdots(limit: Int = 10): List<OverallVdotEntity>
    
    /**
     * 获取最新的VDOT记录
     */
    suspend fun getLatestVdot(): OverallVdotEntity?

    /**
     * 获取指定日期范围内的VDOT记录（按日期倒序）
     * 用于Overall VDOT的历史加权计算
     */
    suspend fun getVdotsByDateRange(startDate: Long, endDate: Long): List<OverallVdotEntity>
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除跑步记录及所有关联数据（级联删除）
     */
    suspend fun deleteRunRecord(workoutId: String)

    /**
     * 批量删除跑步记录及所有关联数据
     */
    suspend fun deleteRunRecords(workoutIds: List<String>)

    /**
     * 获取所有不同的数据源标识
     */
    suspend fun getAllDatasources(): List<String>
}

