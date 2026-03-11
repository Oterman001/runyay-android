package com.oterman.rundemo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oterman.rundemo.data.local.entity.OverallVdotEntity

/**
 * 综合VDOT DAO
 */
@Dao
interface OverallVdotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vdot: OverallVdotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vdots: List<OverallVdotEntity>)

    /**
     * 获取最近N条VDOT记录（按日期倒序，仅纳入统计的）
     */
    @Query("SELECT * FROM overall_vdot WHERE inclusiveLevel > 0 ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentVdots(limit: Int = 10): List<OverallVdotEntity>

    /**
     * 获取所有VDOT记录（按日期正序，仅纳入统计的）
     */
    @Query("SELECT * FROM overall_vdot WHERE inclusiveLevel > 0 ORDER BY date ASC")
    suspend fun getAllVdots(): List<OverallVdotEntity>

    /**
     * 获取最新的VDOT记录
     */
    @Query("SELECT * FROM overall_vdot WHERE inclusiveLevel > 0 ORDER BY date DESC LIMIT 1")
    suspend fun getLatestVdot(): OverallVdotEntity?

    /**
     * 获取指定workout的VDOT记录
     */
    @Query("SELECT * FROM overall_vdot WHERE workoutId = :workoutId LIMIT 1")
    suspend fun getByWorkoutId(workoutId: String): OverallVdotEntity?

    /**
     * 获取指定日期范围内的VDOT记录（按日期倒序，用于Overall VDOT计算）
     * 对齐iOS VdotDataManager.getVdotByDateDscending
     */
    @Query("SELECT * FROM overall_vdot WHERE date >= :startDate AND date <= :endDate AND inclusiveLevel > 0 AND value > 0 ORDER BY date DESC")
    suspend fun getVdotsByDateRange(startDate: Long, endDate: Long): List<OverallVdotEntity>

    /**
     * 删除指定workout的VDOT记录
     */
    @Query("DELETE FROM overall_vdot WHERE workoutId = :workoutId")
    suspend fun deleteByWorkoutId(workoutId: String)

    // ==================== userId 过滤查询 ====================

    @Query("SELECT * FROM overall_vdot WHERE userId = :userId AND inclusiveLevel > 0 ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentVdotsForUser(userId: String, limit: Int = 10): List<OverallVdotEntity>

    @Query("SELECT * FROM overall_vdot WHERE userId = :userId AND inclusiveLevel > 0 ORDER BY date DESC LIMIT 1")
    suspend fun getLatestVdotForUser(userId: String): OverallVdotEntity?

    @Query("SELECT * FROM overall_vdot WHERE userId = :userId AND date >= :startDate AND date <= :endDate AND inclusiveLevel > 0 AND value > 0 ORDER BY date DESC")
    suspend fun getVdotsByDateRangeForUser(userId: String, startDate: Long, endDate: Long): List<OverallVdotEntity>

    @Query("UPDATE overall_vdot SET userId = :userId WHERE userId = ''")
    suspend fun migrateOrphanedRecords(userId: String)

    // ==================== 动态排除/级联重算 ====================

    /**
     * 获取指定日期范围内的所有VDOT记录（不过滤inclusiveLevel）
     * 用于级联重算
     */
    @Query("SELECT * FROM overall_vdot WHERE userId = :userId AND date >= :startDate AND date <= :endDate AND value > 0 ORDER BY date ASC")
    suspend fun getAllVdotsByDateRangeForUser(userId: String, startDate: Long, endDate: Long): List<OverallVdotEntity>

    /**
     * 更新指定workout的综合VDOT值
     */
    @Query("UPDATE overall_vdot SET value = :newValue WHERE workoutId = :workoutId")
    suspend fun updateOverallValue(workoutId: String, newValue: Double)

    /**
     * 更新指定workout的inclusiveLevel
     */
    @Query("UPDATE overall_vdot SET inclusiveLevel = :level WHERE workoutId = :workoutId")
    suspend fun updateInclusiveLevel(workoutId: String, level: Int)
}

