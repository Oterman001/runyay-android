package com.oterman.rundemo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.oterman.rundemo.data.local.entity.RunSegmentEntity

/**
 * 分段数据DAO
 */
@Dao
interface RunSegmentDao {
    
    @Insert
    suspend fun insertAll(segments: List<RunSegmentEntity>)
    
    /**
     * 获取指定workout的所有分段
     */
    @Query("SELECT * FROM run_segment WHERE workoutId = :workoutId ORDER BY seq")
    suspend fun getByWorkoutId(workoutId: String): List<RunSegmentEntity>
    
    /**
     * 获取公里分段（segmentType=1）
     */
    @Query("SELECT * FROM run_segment WHERE workoutId = :workoutId AND segmentType = 1 ORDER BY seq")
    suspend fun getKilometerSegments(workoutId: String): List<RunSegmentEntity>
    
    /**
     * 获取训练分段（segmentType=2）
     */
    @Query("SELECT * FROM run_segment WHERE workoutId = :workoutId AND segmentType = 2 ORDER BY seq")
    suspend fun getTrainingSegments(workoutId: String): List<RunSegmentEntity>
    
    /**
     * 删除指定workout的所有分段
     */
    @Query("DELETE FROM run_segment WHERE workoutId = :workoutId")
    suspend fun deleteByWorkoutId(workoutId: String)
    
    /**
     * 获取分段数量
     */
    @Query("SELECT COUNT(*) FROM run_segment WHERE workoutId = :workoutId")
    suspend fun getCountByWorkoutId(workoutId: String): Int
    
    /**
     * 获取最快配速的分段（公里分段中）
     */
    @Query("SELECT * FROM run_segment WHERE workoutId = :workoutId AND segmentType = 1 ORDER BY averageSpeed ASC LIMIT 1")
    suspend fun getFastestKilometerSegment(workoutId: String): RunSegmentEntity?
}

