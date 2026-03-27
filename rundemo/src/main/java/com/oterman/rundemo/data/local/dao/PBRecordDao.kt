package com.oterman.rundemo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oterman.rundemo.data.local.entity.PBRecordEntity

/**
 * PB记录DAO
 */
@Dao
interface PBRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PBRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<PBRecordEntity>)

    /**
     * 获取某类型某子类型的当前PB记录（仅纳入统计的）
     */
    @Query("SELECT * FROM pb_record WHERE type = :type AND subType = :subType AND inclusiveLevel = 1 ORDER BY value ASC LIMIT 1")
    suspend fun getBestRecord(type: String, subType: String): PBRecordEntity?

    /**
     * 获取某类型所有子类型的PB记录
     */
    @Query("SELECT * FROM pb_record WHERE type = :type AND inclusiveLevel = 1")
    suspend fun getAllByType(type: String): List<PBRecordEntity>

    /**
     * 获取指定workout关联的所有PB
     */
    @Query("SELECT * FROM pb_record WHERE workoutId = :workoutId")
    suspend fun getByWorkoutId(workoutId: String): List<PBRecordEntity>

    /**
     * 删除指定workout的PB记录
     */
    @Query("DELETE FROM pb_record WHERE workoutId = :workoutId")
    suspend fun deleteByWorkoutId(workoutId: String)

    /**
     * 删除某类型某子类型的所有记录
     */
    @Query("DELETE FROM pb_record WHERE type = :type AND subType = :subType")
    suspend fun deleteByTypeAndSubType(type: String, subType: String)

    /**
     * 获取某类型某子类型的最佳Ability PB记录（值越大越好，仅纳入统计的）
     * 用于 maxDistance / maxDuration 等 Ability 类型，与 getBestRecord（ASC）区分
     */
    @Query("SELECT * FROM pb_record WHERE type = :type AND subType = :subType AND inclusiveLevel = 1 ORDER BY value DESC LIMIT 1")
    suspend fun getBestAbilityRecord(type: String, subType: String): PBRecordEntity?

    /**
     * 更新PB记录（用新的值替换旧的）
     * 通过先删除再插入实现
     */
    @Query("DELETE FROM pb_record WHERE type = :type AND subType = :subType AND inclusiveLevel = 1")
    suspend fun clearBestForTypeAndSubType(type: String, subType: String)

    // ==================== userId 过滤查询 ====================

    @Query("SELECT * FROM pb_record WHERE userId = :userId AND type = :type AND subType = :subType AND inclusiveLevel = 1 ORDER BY value ASC LIMIT 1")
    suspend fun getBestRecordForUser(userId: String, type: String, subType: String): PBRecordEntity?

    @Query("SELECT * FROM pb_record WHERE userId = :userId AND type = :type AND inclusiveLevel = 1")
    suspend fun getAllByTypeForUser(userId: String, type: String): List<PBRecordEntity>

    @Query("SELECT * FROM pb_record WHERE userId = :userId AND type = :type AND subType = :subType AND inclusiveLevel = 1 ORDER BY value DESC LIMIT 1")
    suspend fun getBestAbilityRecordForUser(userId: String, type: String, subType: String): PBRecordEntity?

    @Query("DELETE FROM pb_record WHERE userId = :userId AND type = :type AND subType = :subType AND inclusiveLevel = 1")
    suspend fun clearBestForTypeAndSubTypeForUser(userId: String, type: String, subType: String)

    @Query("DELETE FROM pb_record WHERE userId = :userId AND workoutId = :workoutId")
    suspend fun deleteByWorkoutIdForUser(userId: String, workoutId: String)

    @Query("UPDATE pb_record SET inclusiveLevel = :level WHERE workoutId = :workoutId")
    suspend fun updateInclusiveLevelByWorkoutId(workoutId: String, level: Int)

    @Query("UPDATE pb_record SET userId = :userId WHERE userId = ''")
    suspend fun migrateOrphanedRecords(userId: String)

    // ==================== 诊断导出查询（top5 per subType，区分 inclusiveLevel）====================

    /** Speed subType top N，inclusiveLevel=1，值越小越好（配速 ASC） */
    @Query("SELECT * FROM pb_record WHERE userId = :userId AND type = :type AND subType = :subType AND inclusiveLevel = 1 ORDER BY value ASC LIMIT :limit")
    suspend fun getTopSpeedRecordsForUser(userId: String, type: String, subType: String, limit: Int): List<PBRecordEntity>

    /** Ability subType top N，inclusiveLevel=1，值越大越好（DESC） */
    @Query("SELECT * FROM pb_record WHERE userId = :userId AND type = :type AND subType = :subType AND inclusiveLevel = 1 ORDER BY value DESC LIMIT :limit")
    suspend fun getTopAbilityRecordsForUser(userId: String, type: String, subType: String, limit: Int): List<PBRecordEntity>

    /** Speed subType top N，inclusiveLevel != 1，ASC */
    @Query("SELECT * FROM pb_record WHERE userId = :userId AND type = :type AND subType = :subType AND inclusiveLevel != 1 ORDER BY value ASC LIMIT :limit")
    suspend fun getTopExcludedSpeedRecordsForUser(userId: String, type: String, subType: String, limit: Int): List<PBRecordEntity>

    /** Ability subType top N，inclusiveLevel != 1，DESC */
    @Query("SELECT * FROM pb_record WHERE userId = :userId AND type = :type AND subType = :subType AND inclusiveLevel != 1 ORDER BY value DESC LIMIT :limit")
    suspend fun getTopExcludedAbilityRecordsForUser(userId: String, type: String, subType: String, limit: Int): List<PBRecordEntity>
}

