package com.oterman.rundemo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.oterman.rundemo.data.local.entity.RunSamplePointEntity

/**
 * 采样点数据投影类
 */
data class HeartRatePoint(
    val sequence: Int,
    val timeOffset: Int,
    val heartRate: Int?
)

data class SpeedPoint(
    val sequence: Int,
    val timeOffset: Int,
    val speed: Double?
)

data class CadencePoint(
    val sequence: Int,
    val timeOffset: Int,
    val cadence: Int?
)

data class PowerPoint(
    val sequence: Int,
    val timeOffset: Int,
    val power: Int?
)

data class StrideLengthPoint(
    val sequence: Int,
    val timeOffset: Int,
    val strideLength: Double?
)

data class VerticalOscillationPoint(
    val sequence: Int,
    val timeOffset: Int,
    val verticalOscillation: Double?
)

data class ContactTimePoint(
    val sequence: Int,
    val timeOffset: Int,
    val contactTime: Double?
)

data class AltitudePoint(
    val sequence: Int,
    val timeOffset: Int,
    val altitude: Double?
)

/**
 * 采样点DAO
 */
@Dao
interface RunSamplePointDao {
    
    @Insert
    suspend fun insertAll(points: List<RunSamplePointEntity>)
    
    /**
     * 获取指定workout的所有采样点
     */
    @Query("SELECT * FROM run_sample_point WHERE workoutId = :workoutId ORDER BY sequence")
    suspend fun getByWorkoutId(workoutId: String): List<RunSamplePointEntity>
    
    /**
     * 获取GPS轨迹点（只返回有GPS数据的点）
     */
    @Query("SELECT * FROM run_sample_point WHERE workoutId = :workoutId AND latitude IS NOT NULL ORDER BY sequence")
    suspend fun getTrackPoints(workoutId: String): List<RunSamplePointEntity>
    
    /**
     * 获取心率序列数据（用于图表）
     */
    @Query("SELECT sequence, timeOffset, heartRate FROM run_sample_point WHERE workoutId = :workoutId AND heartRate IS NOT NULL ORDER BY sequence")
    suspend fun getHeartRateSeries(workoutId: String): List<HeartRatePoint>
    
    /**
     * 获取配速序列数据（用于图表）
     */
    @Query("SELECT sequence, timeOffset, speed FROM run_sample_point WHERE workoutId = :workoutId AND speed IS NOT NULL ORDER BY sequence")
    suspend fun getSpeedSeries(workoutId: String): List<SpeedPoint>
    
    /**
     * 获取步频序列数据（用于图表）
     */
    @Query("SELECT sequence, timeOffset, cadence FROM run_sample_point WHERE workoutId = :workoutId AND cadence IS NOT NULL ORDER BY sequence")
    suspend fun getCadenceSeries(workoutId: String): List<CadencePoint>
    
    /**
     * 获取功率序列数据（用于图表）
     */
    @Query("SELECT sequence, timeOffset, power FROM run_sample_point WHERE workoutId = :workoutId AND power IS NOT NULL ORDER BY sequence")
    suspend fun getPowerSeries(workoutId: String): List<PowerPoint>
    
    /**
     * 获取步幅序列数据（用于图表）
     */
    @Query("SELECT sequence, timeOffset, strideLength FROM run_sample_point WHERE workoutId = :workoutId AND strideLength IS NOT NULL ORDER BY sequence")
    suspend fun getStrideLengthSeries(workoutId: String): List<StrideLengthPoint>

    /**
     * 获取垂直振幅序列数据（用于图表）
     */
    @Query("SELECT sequence, timeOffset, verticalOscillation FROM run_sample_point WHERE workoutId = :workoutId AND verticalOscillation IS NOT NULL ORDER BY sequence")
    suspend fun getVerticalOscillationSeries(workoutId: String): List<VerticalOscillationPoint>

    /**
     * 获取触地时间序列数据（用于图表）
     */
    @Query("SELECT sequence, timeOffset, contactTime FROM run_sample_point WHERE workoutId = :workoutId AND contactTime IS NOT NULL ORDER BY sequence")
    suspend fun getContactTimeSeries(workoutId: String): List<ContactTimePoint>

    /**
     * 获取海拔序列数据（用于图表）
     */
    @Query("SELECT sequence, timeOffset, altitude FROM run_sample_point WHERE workoutId = :workoutId AND altitude IS NOT NULL ORDER BY sequence")
    suspend fun getAltitudeSeries(workoutId: String): List<AltitudePoint>

    /**
     * 删除指定workout的所有采样点
     */
    @Query("DELETE FROM run_sample_point WHERE workoutId = :workoutId")
    suspend fun deleteByWorkoutId(workoutId: String)
    
    /**
     * 获取采样点数量
     */
    @Query("SELECT COUNT(*) FROM run_sample_point WHERE workoutId = :workoutId")
    suspend fun getCountByWorkoutId(workoutId: String): Int
    
    /**
     * 获取GPS轨迹点数量
     */
    @Query("SELECT COUNT(*) FROM run_sample_point WHERE workoutId = :workoutId AND latitude IS NOT NULL")
    suspend fun getTrackPointCount(workoutId: String): Int
}

