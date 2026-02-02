package com.oterman.rundemo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 跑步记录DAO
 */
@Dao
interface RunRecordDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RunRecordEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<RunRecordEntity>)
    
    @Update
    suspend fun update(record: RunRecordEntity)
    
    @Delete
    suspend fun delete(record: RunRecordEntity)
    
    @Query("DELETE FROM run_record WHERE workoutId = :workoutId")
    suspend fun deleteByWorkoutId(workoutId: String)
    
    /**
     * 根据workoutId查询
     */
    @Query("SELECT * FROM run_record WHERE workoutId = :workoutId")
    suspend fun getByWorkoutId(workoutId: String): RunRecordEntity?
    
    /**
     * 根据原始ID和数据源查询（用于去重）
     */
    @Query("SELECT * FROM run_record WHERE originId = :originId AND datasource = :datasource")
    suspend fun getByOriginId(originId: String, datasource: String): RunRecordEntity?
    
    /**
     * 检查是否存在（用于去重检查）
     */
    @Query("SELECT EXISTS(SELECT 1 FROM run_record WHERE originId = :originId AND datasource = :datasource)")
    suspend fun existsByOriginId(originId: String, datasource: String): Boolean
    
    /**
     * 获取所有记录，按开始时间降序排列
     */
    @Query("SELECT * FROM run_record ORDER BY startTime DESC")
    fun getAllByStartTimeDesc(): Flow<List<RunRecordEntity>>
    
    /**
     * 获取指定时间范围内的记录
     */
    @Query("SELECT * FROM run_record WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<RunRecordEntity>
    
    /**
     * 获取记录总数
     */
    @Query("SELECT COUNT(*) FROM run_record")
    suspend fun getCount(): Int
    
    /**
     * 获取指定数据源的记录
     */
    @Query("SELECT * FROM run_record WHERE datasource = :datasource ORDER BY startTime DESC")
    suspend fun getByDatasource(datasource: String): List<RunRecordEntity>
}

