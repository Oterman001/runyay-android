package com.oterman.rundemo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oterman.rundemo.data.local.entity.TrainPlanEntity

@Dao
interface TrainPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: TrainPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(plans: List<TrainPlanEntity>)

    /**
     * 按日期范围查询摘要字段，不加载 detailJson，避免不必要的大字段读取。
     * scheduledDate 存储格式为 yyyy-MM-dd，BETWEEN 字符串比较可正确排序。
     */
    @Query("""
        SELECT planId, userId, name, description, trainWholeType,
               scheduledDate, hardLevel, finishFlag, locationType,
               workoutId, version, NULL AS detailJson, lastSyncAt, isDirty
        FROM train_plan
        WHERE userId = :userId
          AND scheduledDate IS NOT NULL
          AND scheduledDate BETWEEN :startDate AND :endDate
        ORDER BY scheduledDate ASC
    """)
    suspend fun getByDateRange(
        userId: String,
        startDate: String,
        endDate: String
    ): List<TrainPlanEntity>

    /**
     * 按 planId 查询完整 Entity（含 detailJson），用于编辑页加载详情。
     */
    @Query("SELECT * FROM train_plan WHERE planId = :planId")
    suspend fun getById(planId: String): TrainPlanEntity?

    /**
     * 批量删除，网络删除前先从本地移除。
     */
    @Query("DELETE FROM train_plan WHERE planId IN (:planIds)")
    suspend fun deleteByIds(planIds: List<String>)

    /**
     * 删除指定用户的所有训练计划缓存，退出登录时调用。
     */
    @Query("DELETE FROM train_plan WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    /**
     * 将指定 planId 的 detailJson 清空，强制下次访问时重新拉取详情。
     */
    @Query("UPDATE train_plan SET detailJson = NULL, version = :version WHERE planId = :planId")
    suspend fun invalidateDetail(planId: String, version: Int?)

    /**
     * 更新 detailJson 及版本信息。
     */
    @Query("""
        UPDATE train_plan
        SET detailJson = :detailJson, version = :version, lastSyncAt = :syncAt, isDirty = 0
        WHERE planId = :planId
    """)
    suspend fun updateDetail(planId: String, detailJson: String, version: Int?, syncAt: Long)
}
