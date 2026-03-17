package com.oterman.rundemo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.oterman.rundemo.data.local.entity.RunningShoeEntity
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningShoeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shoe: RunningShoeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shoes: List<RunningShoeEntity>)

    @Update
    suspend fun update(shoe: RunningShoeEntity)

    @Query("SELECT * FROM running_shoe WHERE id = :shoeId AND deletedAt IS NULL")
    suspend fun getById(shoeId: String): RunningShoeEntity?

    @Query("UPDATE running_shoe SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :shoeId")
    suspend fun softDelete(shoeId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM running_shoe WHERE userId = :userId AND isActive = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getActiveShoes(userId: String): Flow<List<RunningShoeEntity>>

    @Query("SELECT * FROM running_shoe WHERE userId = :userId AND isActive = 0 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getRetiredShoes(userId: String): Flow<List<RunningShoeEntity>>

    @Query("SELECT * FROM running_shoe WHERE userId = :userId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllShoes(userId: String): Flow<List<RunningShoeEntity>>

    @Query("UPDATE running_shoe SET isDefault = 0, updatedAt = :timestamp WHERE userId = :userId AND isDefault = 1")
    suspend fun clearDefaultShoe(userId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE running_shoe SET isDefault = 1, updatedAt = :timestamp WHERE id = :shoeId")
    suspend fun setDefaultShoe(shoeId: String, timestamp: Long = System.currentTimeMillis())

    @Query("""
        SELECT * FROM running_shoe
        WHERE userId = :userId AND deletedAt IS NULL
        AND (brand LIKE '%' || :keyword || '%'
             OR model LIKE '%' || :keyword || '%'
             OR nickname LIKE '%' || :keyword || '%')
        ORDER BY updatedAt DESC
    """)
    suspend fun searchShoes(userId: String, keyword: String): List<RunningShoeEntity>

    @Query("SELECT COUNT(*) FROM run_record WHERE shoeId = :shoeId")
    suspend fun getLinkedRunRecordsCount(shoeId: String): Int

    @Query("SELECT * FROM run_record WHERE shoeId = :shoeId ORDER BY startTime DESC")
    suspend fun getLinkedRunRecords(shoeId: String): List<RunRecordEntity>

    @Query("SELECT * FROM run_record WHERE userId = :userId AND (shoeId IS NULL OR shoeId = '') ORDER BY startTime DESC")
    suspend fun getUnlinkedRunRecords(userId: String): List<RunRecordEntity>

    @Query("UPDATE run_record SET shoeId = :shoeId WHERE workoutId IN (:recordIds)")
    suspend fun batchLinkRecords(shoeId: String, recordIds: List<String>)

    @Query("UPDATE run_record SET shoeId = NULL WHERE workoutId = :recordId")
    suspend fun unlinkRecord(recordId: String)

    @Query("SELECT * FROM running_shoe WHERE userId = :userId AND deletedAt IS NULL AND (syncStatus = 'localOnly' OR syncStatus = 'pending') AND syncRetryCount < 3")
    suspend fun getUnsyncedShoes(userId: String): List<RunningShoeEntity>

    @Query("SELECT * FROM running_shoe WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun getAllShoesSync(userId: String): List<RunningShoeEntity>
}
