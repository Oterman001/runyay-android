package com.oterman.rundemo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.oterman.rundemo.data.local.entity.RunAbilityZoneEntity

/**
 * 能力区间DAO
 */
@Dao
interface RunAbilityZoneDao {
    
    @Insert
    suspend fun insertAll(zones: List<RunAbilityZoneEntity>)
    
    /**
     * 获取指定workout的所有区间
     */
    @Query("SELECT * FROM run_ability_zone WHERE workoutId = :workoutId ORDER BY zoneType, zoneIndex")
    suspend fun getByWorkoutId(workoutId: String): List<RunAbilityZoneEntity>
    
    /**
     * 获取心率7区间（zoneType=1）
     */
    @Query("SELECT * FROM run_ability_zone WHERE workoutId = :workoutId AND zoneType = 1 ORDER BY zoneIndex")
    suspend fun getHeartRate7Zones(workoutId: String): List<RunAbilityZoneEntity>
    
    /**
     * 获取心率5区间（zoneType=2）
     */
    @Query("SELECT * FROM run_ability_zone WHERE workoutId = :workoutId AND zoneType = 2 ORDER BY zoneIndex")
    suspend fun getHeartRate5Zones(workoutId: String): List<RunAbilityZoneEntity>
    
    /**
     * 获取配速区间（zoneType=3）
     */
    @Query("SELECT * FROM run_ability_zone WHERE workoutId = :workoutId AND zoneType = 3 ORDER BY zoneIndex")
    suspend fun getSpeedZones(workoutId: String): List<RunAbilityZoneEntity>
    
    /**
     * 批量获取心率7区间（zoneType=1）
     */
    @Query("SELECT * FROM run_ability_zone WHERE workoutId IN (:workoutIds) AND zoneType = 1 ORDER BY zoneIndex")
    suspend fun getHeartRate7ZonesByWorkoutIds(workoutIds: List<String>): List<RunAbilityZoneEntity>

    /**
     * 批量获取心率5区间（zoneType=2）
     */
    @Query("SELECT * FROM run_ability_zone WHERE workoutId IN (:workoutIds) AND zoneType = 2 ORDER BY zoneIndex")
    suspend fun getHeartRate5ZonesByWorkoutIds(workoutIds: List<String>): List<RunAbilityZoneEntity>

    /**
     * 批量获取配速区间（zoneType=3）
     */
    @Query("SELECT * FROM run_ability_zone WHERE workoutId IN (:workoutIds) AND zoneType = 3 ORDER BY zoneIndex")
    suspend fun getSpeedZonesByWorkoutIds(workoutIds: List<String>): List<RunAbilityZoneEntity>

    /**
     * 获取全部心率7区间（zoneType=1），Total页面用
     */
    @Query("SELECT * FROM run_ability_zone WHERE zoneType = 1 ORDER BY zoneIndex")
    suspend fun getAllHeartRate7Zones(): List<RunAbilityZoneEntity>

    /**
     * 获取全部心率5区间（zoneType=2），Total页面用
     */
    @Query("SELECT * FROM run_ability_zone WHERE zoneType = 2 ORDER BY zoneIndex")
    suspend fun getAllHeartRate5Zones(): List<RunAbilityZoneEntity>

    /**
     * 获取全部配速区间（zoneType=3），Total页面用
     */
    @Query("SELECT * FROM run_ability_zone WHERE zoneType = 3 ORDER BY zoneIndex")
    suspend fun getAllSpeedZones(): List<RunAbilityZoneEntity>

    /**
     * 删除指定workout的所有区间
     */
    @Query("DELETE FROM run_ability_zone WHERE workoutId = :workoutId")
    suspend fun deleteByWorkoutId(workoutId: String)
}

