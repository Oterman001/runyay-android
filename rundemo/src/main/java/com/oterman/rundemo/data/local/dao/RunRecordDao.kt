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

    /**
     * 批量删除记录
     */
    @Query("DELETE FROM run_record WHERE workoutId IN (:workoutIds)")
    suspend fun deleteByWorkoutIds(workoutIds: List<String>)

    /**
     * 获取所有不同的数据源
     */
    @Query("SELECT DISTINCT datasource FROM run_record WHERE datasource IS NOT NULL ORDER BY datasource")
    suspend fun getAllDatasources(): List<String>

    // ==================== userId 过滤查询 ====================

    @Query("SELECT * FROM run_record WHERE userId = :userId ORDER BY startTime DESC")
    fun getAllByStartTimeDescForUser(userId: String): Flow<List<RunRecordEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM run_record WHERE originId = :originId AND datasource = :datasource AND userId = :userId)")
    suspend fun existsByOriginIdForUser(originId: String, datasource: String, userId: String): Boolean

    @Query("SELECT * FROM run_record WHERE originId = :originId AND datasource = :datasource AND userId = :userId")
    suspend fun getByOriginIdForUser(originId: String, datasource: String, userId: String): RunRecordEntity?

    @Query("SELECT * FROM run_record WHERE userId = :userId AND startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    suspend fun getByTimeRangeForUser(userId: String, startTime: Long, endTime: Long): List<RunRecordEntity>

    @Query("SELECT DISTINCT datasource FROM run_record WHERE userId = :userId AND datasource IS NOT NULL ORDER BY datasource")
    suspend fun getAllDatasourcesForUser(userId: String): List<String>

    @Query("SELECT * FROM run_record WHERE userId = :userId AND datasource = :datasource ORDER BY startTime DESC")
    suspend fun getByDatasourceForUser(userId: String, datasource: String): List<RunRecordEntity>

    @Query("UPDATE run_record SET userId = :userId WHERE userId = ''")
    suspend fun migrateOrphanedRecords(userId: String)

    // ==================== 上传状态查询 ====================

    @Query("SELECT * FROM run_record WHERE userId = :userId AND uploadStatus IN (0, 3)")
    suspend fun getRecordsNeedingUploadForUser(userId: String): List<RunRecordEntity>

    @Query("UPDATE run_record SET uploadStatus = :status WHERE workoutId = :workoutId")
    suspend fun updateUploadStatus(workoutId: String, status: Int)

    @Query("UPDATE run_record SET overallVdot = :value WHERE workoutId = :workoutId")
    suspend fun updateOverallVdot(workoutId: String, value: Double)

    @Query("""
        SELECT * FROM run_record
        WHERE userId = :userId
        AND inclusiveLevel != 0
        AND startTime < :newEndTime
        AND endTime > :newStartTime
        ORDER BY startTime DESC
    """)
    suspend fun getConflictingRecordsForUser(
        userId: String,
        newStartTime: Long,
        newEndTime: Long
    ): List<RunRecordEntity>
}

