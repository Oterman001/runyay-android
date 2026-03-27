package com.oterman.rundemo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oterman.rundemo.data.local.entity.DailyHealthEntity

/**
 * 每日健康数据 DAO
 */
@Dao
interface DailyHealthDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: DailyHealthEntity)

    /**
     * 检查缓存是否存在（cache-hit check before API call）
     */
    @Query("SELECT COUNT(*) > 0 FROM daily_health WHERE userId = :userId AND platformCode = :platformCode AND calendarDate = :calendarDate")
    suspend fun exists(userId: String, platformCode: String, calendarDate: String): Boolean

    /**
     * 获取指定用户、平台、日期的健康数据
     */
    @Query("SELECT * FROM daily_health WHERE userId = :userId AND platformCode = :platformCode AND calendarDate = :calendarDate LIMIT 1")
    suspend fun getByUserPlatformDate(userId: String, platformCode: String, calendarDate: String): DailyHealthEntity?

    /**
     * 获取指定用户某一天的最佳静息心率（跨平台取最小值，最接近生理真实值）
     */
    @Query("SELECT MIN(restingHeartRate) FROM daily_health WHERE userId = :userId AND calendarDate = :calendarDate AND restingHeartRate IS NOT NULL")
    suspend fun getBestRestingHR(userId: String, calendarDate: String): Int?

    /**
     * 获取最近一次静息心率（不限日期，取最新日期中跨平台最小值）
     */
    @Query("SELECT MIN(restingHeartRate) FROM daily_health WHERE userId = :userId AND restingHeartRate IS NOT NULL AND calendarDate = (SELECT MAX(calendarDate) FROM daily_health WHERE userId = :userId AND restingHeartRate IS NOT NULL)")
    suspend fun getLatestRestingHR(userId: String): Int?

    /**
     * 获取指定日期之前（含当天）最近一条静息心率（跨平台取最小值）
     * 用于精确日期无数据时的兜底查询
     */
    @Query("""
        SELECT MIN(restingHeartRate) FROM daily_health 
        WHERE userId = :userId 
          AND restingHeartRate IS NOT NULL 
          AND calendarDate <= :calendarDate
          AND calendarDate = (
              SELECT MAX(calendarDate) FROM daily_health 
              WHERE userId = :userId 
                AND restingHeartRate IS NOT NULL 
                AND calendarDate <= :calendarDate
          )
    """)
    suspend fun getMostRecentRestingHRBefore(userId: String, calendarDate: String): Int?

    /**
     * 获取最新的VO2Max记录（用于详情页展示）
     */
    @Query("SELECT * FROM daily_health WHERE userId = :userId AND vo2Max IS NOT NULL ORDER BY calendarDate DESC LIMIT 1")
    suspend fun getLatestVo2Max(userId: String): DailyHealthEntity?

    /**
     * 获取最新两条VO2Max记录（用于delta计算：current - previous）
     */
    @Query("SELECT * FROM daily_health WHERE userId = :userId AND vo2Max IS NOT NULL ORDER BY calendarDate DESC LIMIT 2")
    suspend fun getLatestTwoVo2Max(userId: String): List<DailyHealthEntity>

    /**
     * 获取指定日期的最大VO2Max（跨平台取最大值）
     */
    @Query("SELECT MAX(vo2Max) FROM daily_health WHERE userId = :userId AND calendarDate = :calendarDate AND vo2Max IS NOT NULL")
    suspend fun getVo2MaxForDate(userId: String, calendarDate: String): Double?

    /**
     * 获取指定日期之前最近的VO2Max记录（用于delta计算）
     */
    @Query("SELECT vo2Max FROM daily_health WHERE userId = :userId AND calendarDate < :beforeDate AND vo2Max IS NOT NULL ORDER BY calendarDate DESC LIMIT 1")
    suspend fun getPreviousVo2Max(userId: String, beforeDate: String): Double?

    /**
     * 获取指定日期、指定平台的VO2Max（同平台精确匹配）
     */
    @Query("SELECT vo2Max FROM daily_health WHERE userId = :userId AND platformCode = :platformCode AND calendarDate = :calendarDate AND vo2Max IS NOT NULL ORDER BY calendarDate DESC LIMIT 1")
    suspend fun getVo2MaxForDateByPlatform(userId: String, platformCode: String, calendarDate: String): Double?

    /**
     * 获取指定日期之前、指定平台最近的VO2Max（用于同平台delta计算）
     */
    @Query("SELECT vo2Max FROM daily_health WHERE userId = :userId AND platformCode = :platformCode AND calendarDate < :beforeDate AND vo2Max IS NOT NULL ORDER BY calendarDate DESC LIMIT 1")
    suspend fun getPreviousVo2MaxByPlatform(userId: String, platformCode: String, beforeDate: String): Double?

    /** 诊断导出：指定日期范围内全部健康数据（跨平台），按日期降序 */
    @Query("SELECT * FROM daily_health WHERE userId = :userId AND calendarDate >= :start AND calendarDate <= :end ORDER BY calendarDate DESC")
    suspend fun getByDateRangeForUser(userId: String, start: String, end: String): List<DailyHealthEntity>
}
