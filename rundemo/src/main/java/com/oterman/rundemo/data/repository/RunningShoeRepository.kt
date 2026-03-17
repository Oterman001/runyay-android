package com.oterman.rundemo.data.repository

import android.content.Context
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.RunningShoeImageManager
import com.oterman.rundemo.data.local.dao.RunningShoeDao
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.local.entity.RunningShoeEntity
import com.oterman.rundemo.data.network.RequestBuilder
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.network.api.RunningShoeApi
import com.oterman.rundemo.data.network.dto.request.DeleteRunningShoeRequestDto
import com.oterman.rundemo.data.network.dto.request.RunningShoeSaveDto
import com.oterman.rundemo.data.network.dto.request.UpdateRunningShoeRequestDto
import com.oterman.rundemo.data.network.dto.response.RunningShoeDto
import com.oterman.rundemo.domain.model.RunningShoe
import com.oterman.rundemo.domain.model.ShoeSortType
import com.oterman.rundemo.domain.model.toDomainModel
import com.oterman.rundemo.domain.model.toEntity
import com.oterman.rundemo.util.RLog
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class RunningShoeRepository(
    private val context: Context,
    private val dao: RunningShoeDao = RunDatabase.getInstance(context).runningShoeDao(),
    private val api: RunningShoeApi = RetrofitClient.runningShoeApi,
    private val preferencesManager: PreferencesManager = PreferencesManager(context),
    private val imageManager: RunningShoeImageManager = RunningShoeImageManager(context)
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    private fun getUserId(): String = preferencesManager.getUserId() ?: ""

    // ==================== Local CRUD ====================

    suspend fun createShoe(shoe: RunningShoe): Result<RunningShoe> {
        return try {
            val entity = shoe.toEntity()
            dao.insert(entity)
            Result.success(shoe)
        } catch (e: Exception) {
            RLog.e("RunningShoeRepo", "createShoe failed", e)
            Result.failure(e)
        }
    }

    suspend fun updateShoe(shoe: RunningShoe): Result<RunningShoe> {
        return try {
            val existing = dao.getById(shoe.id)
            val entity = shoe.toEntity().copy(
                updatedAt = System.currentTimeMillis(),
                imagePath = shoe.imagePath ?: existing?.imagePath,
                imageUrl = shoe.imageUrl ?: existing?.imageUrl
            )
            dao.update(entity)
            Result.success(entity.toDomainModel())
        } catch (e: Exception) {
            RLog.e("RunningShoeRepo", "updateShoe failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteShoe(shoeId: String): Result<Unit> {
        return try {
            dao.softDelete(shoeId)
            Result.success(Unit)
        } catch (e: Exception) {
            RLog.e("RunningShoeRepo", "deleteShoe failed", e)
            Result.failure(e)
        }
    }

    suspend fun getShoe(shoeId: String): RunningShoe? {
        return dao.getById(shoeId)?.toDomainModel()
    }

    fun getActiveShoes(): Flow<List<RunningShoe>> {
        return dao.getActiveShoes(getUserId()).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getRetiredShoes(): Flow<List<RunningShoe>> {
        return dao.getRetiredShoes(getUserId()).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getAllShoes(): Flow<List<RunningShoe>> {
        return dao.getAllShoes(getUserId()).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    // ==================== Default Shoe ====================

    suspend fun setDefaultShoe(shoeId: String): Result<Unit> {
        return try {
            dao.clearDefaultShoe(getUserId())
            dao.setDefaultShoe(shoeId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Retire / Reactivate ====================

    suspend fun retireShoe(shoeId: String): Result<Unit> {
        return try {
            val shoe = dao.getById(shoeId) ?: return Result.failure(Exception("跑鞋不存在"))
            val now = System.currentTimeMillis()
            dao.update(shoe.copy(
                isActive = false,
                retireDate = now,
                updatedAt = now,
                syncStatus = "pending"
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reactivateShoe(shoeId: String): Result<Unit> {
        return try {
            val shoe = dao.getById(shoeId) ?: return Result.failure(Exception("跑鞋不存在"))
            val now = System.currentTimeMillis()
            dao.update(shoe.copy(
                isActive = true,
                retireDate = null,
                updatedAt = now,
                syncStatus = "pending"
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Record Linking ====================

    suspend fun linkRecords(shoeId: String, recordIds: List<String>): Result<Unit> {
        return try {
            dao.batchLinkRecords(shoeId, recordIds)
            recalculateShoeStats(shoeId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlinkRecord(recordId: String, shoeId: String): Result<Unit> {
        return try {
            dao.unlinkRecord(recordId)
            recalculateShoeStats(shoeId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLinkedRecordsCount(shoeId: String): Int {
        return dao.getLinkedRunRecordsCount(shoeId)
    }

    suspend fun getLinkedRecords(shoeId: String) = dao.getLinkedRunRecords(shoeId)

    suspend fun getUnlinkedRecords() = dao.getUnlinkedRunRecords(getUserId())

    suspend fun recalculateShoeStats(shoeId: String) {
        try {
            val records = dao.getLinkedRunRecords(shoeId)
            val shoe = dao.getById(shoeId) ?: return
            val totalDistance = records.sumOf { it.totalDistance }
            val totalDuration = records.sumOf { it.duration }
            val totalRuns = records.size
            val now = System.currentTimeMillis()
            dao.update(shoe.copy(
                totalDistance = totalDistance,
                totalDuration = totalDuration,
                totalRuns = totalRuns,
                updatedAt = now,
                syncStatus = "pending"
            ))
        } catch (e: Exception) {
            RLog.e("RunningShoeRepo", "recalculateShoeStats failed", e)
        }
    }

    // ==================== Search & Sort ====================

    suspend fun searchShoes(keyword: String): List<RunningShoe> {
        return dao.searchShoes(getUserId(), keyword).map { it.toDomainModel() }
    }

    fun sortShoes(shoes: List<RunningShoe>, sortType: ShoeSortType): List<RunningShoe> {
        return when (sortType) {
            ShoeSortType.updateTime -> shoes.sortedByDescending { it.updatedAt }
            ShoeSortType.createTime -> shoes.sortedByDescending { it.createdAt }
            ShoeSortType.totalDistance -> shoes.sortedByDescending { it.totalDistance }
            ShoeSortType.totalRuns -> shoes.sortedByDescending { it.totalRuns }
            ShoeSortType.brand -> shoes.sortedBy { it.brand ?: "" }
        }
    }

    // ==================== Remote Sync ====================

    suspend fun syncToServer(): Result<Unit> {
        return try {
            val unsyncedShoes = dao.getUnsyncedShoes(getUserId())
            if (unsyncedShoes.isEmpty()) return Result.success(Unit)

            val saveDtos = unsyncedShoes.map { shoe ->
                RunningShoeSaveDto(
                    shoeId = shoe.id,
                    brand = shoe.brand,
                    model = shoe.model,
                    shoeSize = shoe.shoeSize,
                    nickname = shoe.nickname,
                    shoeType = shoe.shoeType,
                    color = shoe.color,
                    price = shoe.price,
                    expectedLifespan = shoe.expectedLifespan,
                    firstUseDate = shoe.firstUseDate?.let { dateFormat.format(Date(it)) },
                    retireDate = shoe.retireDate?.let { dateFormat.format(Date(it)) },
                    initialDistance = shoe.initialDistance,
                    totalDistance = shoe.totalDistance,
                    totalDuration = shoe.totalDuration,
                    totalRuns = shoe.totalRuns,
                    imageUrl = shoe.imageUrl,
                    notes = shoe.notes,
                    isActive = shoe.isActive,
                    isDefault = shoe.isDefault,
                    syncStatus = "SYNCED"
                )
            }

            // Batch save (max 50 per request)
            saveDtos.chunked(50).forEach { batch ->
                val request = RequestBuilder.createRequest(
                    dtoName = "RunningShoeSaveDto",
                    data = batch.first(),
                    preferencesManager = preferencesManager
                )
                // For batch, we need the body to contain all items
                val batchRequest = com.oterman.rundemo.data.network.dto.request.BaseRequest(
                    head = request.head,
                    body = mapOf("RunningShoeSaveDto" to batch)
                )
                val response = api.batchSaveShoes(batchRequest)
                if (response.isSuccess()) {
                    val now = System.currentTimeMillis()
                    batch.forEach { dto ->
                        dao.getById(dto.shoeId)?.let { entity ->
                            dao.update(entity.copy(
                                syncStatus = "synced",
                                lastSyncAt = now,
                                updatedAt = now
                            ))
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            RLog.e("RunningShoeRepo", "syncToServer failed", e)
            Result.failure(e)
        }
    }

    suspend fun pullFromServer(): Result<Unit> {
        return try {
            val request = RequestBuilder.createEmptyBodyRequest(preferencesManager)
            val response = api.listShoes(request)
            if (response.isSuccess()) {
                val serverShoes = response.data?.shoes ?: emptyList()
                mergeServerShoes(serverShoes)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            RLog.e("RunningShoeRepo", "pullFromServer failed", e)
            Result.failure(e)
        }
    }

    private suspend fun mergeServerShoes(serverShoes: List<RunningShoeDto>) {
        val userId = getUserId()
        val now = System.currentTimeMillis()
        serverShoes.forEach { dto ->
            val shoeId = dto.shoeId ?: return@forEach
            val existing = dao.getById(shoeId)
            if (existing == null) {
                // New shoe from server
                dao.insert(RunningShoeEntity(
                    id = shoeId,
                    userId = dto.userId ?: userId,
                    brand = dto.brand,
                    model = dto.model,
                    shoeSize = dto.shoeSize,
                    nickname = dto.nickname,
                    shoeType = dto.shoeType ?: "training",
                    price = dto.price,
                    expectedLifespan = dto.expectedLifespan ?: 700.0,
                    firstUseDate = dto.firstUseDate?.let { parseDate(it) },
                    retireDate = dto.retireDate?.let { parseDate(it) },
                    initialDistance = dto.initialDistance ?: 0.0,
                    totalDistance = dto.totalDistance ?: 0.0,
                    totalDuration = dto.totalDuration ?: 0.0,
                    totalRuns = dto.totalRuns ?: 0,
                    imagePath = dto.imagePath,
                    imageUrl = dto.imageUrl,
                    notes = dto.notes,
                    isActive = dto.isActive ?: true,
                    isDefault = dto.isDefault ?: false,
                    color = null,
                    syncStatus = "synced",
                    lastSyncAt = now,
                    createdAt = dto.createdAt?.let { parseServerDate(it) } ?: now,
                    updatedAt = dto.updatedAt?.let { parseServerDate(it) } ?: now
                ))
            } else if (existing.syncStatus == "synced") {
                // Update from server only if local hasn't been modified
                dao.update(existing.copy(
                    brand = dto.brand ?: existing.brand,
                    model = dto.model ?: existing.model,
                    totalDistance = dto.totalDistance ?: existing.totalDistance,
                    totalDuration = dto.totalDuration ?: existing.totalDuration,
                    totalRuns = dto.totalRuns ?: existing.totalRuns,
                    imagePath = dto.imagePath ?: existing.imagePath,
                    imageUrl = dto.imageUrl ?: existing.imageUrl,
                    isActive = dto.isActive ?: existing.isActive,
                    isDefault = dto.isDefault ?: existing.isDefault,
                    lastSyncAt = now,
                    updatedAt = now
                ))
            } else {
                // Local-only or pending sync: only update image fields from server
                if (dto.imagePath != null || dto.imageUrl != null) {
                    dao.update(existing.copy(
                        imagePath = dto.imagePath ?: existing.imagePath,
                        imageUrl = dto.imageUrl ?: existing.imageUrl
                    ))
                }
            }
        }
    }

    suspend fun deleteShoeFromServer(shoeId: String): Result<Unit> {
        return try {
            val request = RequestBuilder.createRequest(
                dtoName = "DeleteRunningShoeRequestDto",
                data = DeleteRunningShoeRequestDto(shoeId = shoeId),
                preferencesManager = preferencesManager
            )
            val response = api.deleteShoe(request)
            if (response.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadImage(shoeId: String, imageUri: android.net.Uri): Result<String> {
        return try {
            // Save locally first
            val localPath = imageManager.saveImage(shoeId, imageUri)
                ?: return Result.failure(Exception("图片保存失败"))

            // Upload to server
            val file = java.io.File(localPath)
            if (!file.exists()) return Result.failure(Exception("图片文件不存在"))

            val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = okhttp3.MultipartBody.Part.createFormData("avatar", file.name, requestBody)

            val jsonRequest = RequestBuilder.createEmptyBodyRequest(preferencesManager)
            val jsonString = com.google.gson.Gson().toJson(jsonRequest)
            val jsonBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

            val response = api.uploadShoeImage(imagePart, jsonBody)
            if (response.isSuccess()) {
                val imageUrl = response.data?.uploadResponse?.firstOrNull()?.imageUrl
                if (imageUrl != null) {
                    // Update local entity: imagePath stores the accessible image URL from server
                    // imageUrl stores the original (non-signed) URL as a stable cache key
                    dao.getById(shoeId)?.let { entity ->
                        dao.update(entity.copy(
                            imagePath = imageUrl,
                            imageUrl = imageUrl,
                            updatedAt = System.currentTimeMillis()
                        ))
                    }
                    Result.success(imageUrl)
                } else {
                    Result.failure(Exception("未获取到图片URL"))
                }
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e("RunningShoeRepo", "uploadImage failed", e)
            Result.failure(e)
        }
    }

    fun generateShoeId(): String = UUID.randomUUID().toString()

    fun getImageManager() = imageManager

    private fun parseDate(dateStr: String): Long? {
        return try {
            dateFormat.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun parseServerDate(dateStr: String): Long? {
        return try {
            serverDateFormat.parse(dateStr)?.time
        } catch (e: Exception) {
            parseDate(dateStr)
        }
    }
}
