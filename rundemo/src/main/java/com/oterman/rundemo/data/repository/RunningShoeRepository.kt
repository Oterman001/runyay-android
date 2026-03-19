package com.oterman.rundemo.data.repository

import android.content.Context
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.RunningShoeImageManager
import com.oterman.rundemo.data.local.dao.RunRecordDao
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
    private val imageManager: RunningShoeImageManager = RunningShoeImageManager(context),
    private val runRecordDao: RunRecordDao = RunDatabase.getInstance(context).runRecordDao(),
    private val remoteRepository: RunDataRemoteRepository = RunDataRemoteRepository(PreferencesManager(context))
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    private fun getUserId(): String = preferencesManager.getUserId() ?: ""

    private fun RunningShoe.withLocalImage(): RunningShoe {
        val localPath = imageManager.getImagePath(this.id)
        return if (localPath != null) this.copy(localImagePath = localPath) else this
    }

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
            dao.clearShoeIdForShoe(shoeId)  // 先清理关联记录的 shoeId
            dao.softDelete(shoeId)
            Result.success(Unit)
        } catch (e: Exception) {
            RLog.e("RunningShoeRepo", "deleteShoe failed", e)
            Result.failure(e)
        }
    }

    suspend fun getShoe(shoeId: String): RunningShoe? {
        return dao.getById(shoeId)?.toDomainModel()?.withLocalImage()
    }

    fun getActiveShoes(): Flow<List<RunningShoe>> {
        return dao.getActiveShoes(getUserId()).map { list ->
            list.map { it.toDomainModel().withLocalImage() }
        }
    }

    fun getRetiredShoes(): Flow<List<RunningShoe>> {
        return dao.getRetiredShoes(getUserId()).map { list ->
            list.map { it.toDomainModel().withLocalImage() }
        }
    }

    fun getAllShoes(): Flow<List<RunningShoe>> {
        return dao.getAllShoes(getUserId()).map { list ->
            list.map { it.toDomainModel().withLocalImage() }
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
            // 同步到服务器
            syncLinkedRecordsToServer(recordIds)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlinkRecord(recordId: String, shoeId: String): Result<Unit> {
        return try {
            dao.unlinkRecord(recordId)
            recalculateShoeStats(shoeId)
            // 同步到服务器
            syncLinkedRecordsToServer(listOf(recordId))
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
            val localDistance = records.sumOf { it.totalDistance }
            val localDuration = records.sumOf { it.duration }
            val localRuns = records.size
            // 直接使用本地关联记录计算，确保删除记录后统计能正确减少
            val finalDistance = localDistance
            val finalDuration = localDuration
            val finalRuns = localRuns
            val now = System.currentTimeMillis()
            dao.update(shoe.copy(
                totalDistance = finalDistance,
                totalDuration = finalDuration,
                totalRuns = finalRuns,
                updatedAt = now,
                syncStatus = "pending"
            ))
        } catch (e: Exception) {
            RLog.e("RunningShoeRepo", "recalculateShoeStats failed", e)
        }
    }

    private suspend fun syncLinkedRecordsToServer(recordIds: List<String>) {
        for (recordId in recordIds) {
            try {
                val record = runRecordDao.getByWorkoutId(recordId) ?: continue
                if (record.originId == null) continue  // 没有 originId 的记录无法同步
                remoteRepository.syncRunRecord(record)
            } catch (e: Exception) {
                RLog.w("RunningShoeRepo", "同步记录到服务器失败: $recordId, ${e.message}")
            }
        }
    }

    // ==================== Single Record Shoe Change ====================

    suspend fun getDefaultShoe(): RunningShoe? {
        return dao.getDefaultShoe(getUserId())?.toDomainModel()?.withLocalImage()
    }

    suspend fun getShoesByIds(ids: List<String>): Map<String, RunningShoe> {
        if (ids.isEmpty()) return emptyMap()
        return dao.getShoesByIds(ids).associate { it.id to it.toDomainModel().withLocalImage() }
    }

    /**
     * 切换跑步记录的关联跑鞋，同时重算新旧跑鞋统计
     */
    suspend fun changeRecordShoe(recordId: String, oldShoeId: String?, newShoeId: String?): Result<Unit> {
        return try {
            dao.updateRecordShoeId(recordId, newShoeId)
            oldShoeId?.let { recalculateShoeStats(it) }
            newShoeId?.let { recalculateShoeStats(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            RLog.e("RunningShoeRepo", "changeRecordShoe failed", e)
            Result.failure(e)
        }
    }

    suspend fun getActiveShoesSync(): List<RunningShoe> {
        return dao.getAllShoesSync(getUserId())
            .filter { it.isActive && it.deletedAt == null }
            .map { it.toDomainModel().withLocalImage() }
    }

    // ==================== Search & Sort ====================

    suspend fun searchShoes(keyword: String): List<RunningShoe> {
        return dao.searchShoes(getUserId(), keyword).map { it.toDomainModel().withLocalImage() }
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
                    imagePath = dto.imagePath?.takeIf { it.isNotBlank() },
                    imageUrl = dto.imageUrl?.takeIf { it.isNotBlank() },
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
                    nickname = dto.nickname ?: existing.nickname,
                    shoeSize = dto.shoeSize ?: existing.shoeSize,
                    shoeType = dto.shoeType ?: existing.shoeType,
                    price = dto.price ?: existing.price,
                    expectedLifespan = dto.expectedLifespan ?: existing.expectedLifespan,
                    initialDistance = dto.initialDistance ?: existing.initialDistance,
                    totalDistance = dto.totalDistance ?: existing.totalDistance,
                    totalDuration = dto.totalDuration ?: existing.totalDuration,
                    totalRuns = dto.totalRuns ?: existing.totalRuns,
                    notes = dto.notes ?: existing.notes,
                    firstUseDate = dto.firstUseDate?.let { parseDate(it) } ?: existing.firstUseDate,
                    retireDate = dto.retireDate?.let { parseDate(it) } ?: existing.retireDate,
                    imagePath = dto.imagePath?.takeIf { it.isNotBlank() } ?: existing.imagePath,
                    imageUrl = dto.imageUrl?.takeIf { it.isNotBlank() } ?: existing.imageUrl,
                    isActive = dto.isActive ?: existing.isActive,
                    isDefault = dto.isDefault ?: existing.isDefault,
                    lastSyncAt = now,
                    updatedAt = now
                ))
            } else {
                // Local-only or pending sync: only update image fields from server
                val serverImagePath = dto.imagePath?.takeIf { it.isNotBlank() }
                val serverImageUrl = dto.imageUrl?.takeIf { it.isNotBlank() }
                if (serverImagePath != null || serverImageUrl != null) {
                    dao.update(existing.copy(
                        imagePath = serverImagePath ?: existing.imagePath,
                        imageUrl = serverImageUrl ?: existing.imageUrl
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

            // Update updatedAt to trigger UI refresh, and set imagePath as fallback
            // so the image is still available even if local file is later cleaned up
            dao.getById(shoeId)?.let { entity ->
                dao.update(entity.copy(
                    imagePath = entity.imagePath?.takeIf { it.isNotBlank() } ?: localPath,
                    updatedAt = System.currentTimeMillis()
                ))
            }

            // Upload to server in best-effort manner
            val serverUrl = uploadImageToServer(shoeId, localPath)
            if (serverUrl != null) {
                // Store imageUrl and mark pending so pullFromServer won't overwrite,
                // and syncToServer will push the new imageUrl to server
                dao.getById(shoeId)?.let { entity ->
                    dao.update(entity.copy(
                        imageUrl = serverUrl,
                        syncStatus = "pending",
                        updatedAt = System.currentTimeMillis()
                    ))
                }
                // Push shoe data (with new imageUrl) to server
                try { syncToServer() } catch (_: Exception) {}
            }
            // Return success regardless — local image is already saved and displayed
            Result.success(localPath)
        } catch (e: Exception) {
            RLog.e("RunningShoeRepo", "uploadImage failed", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadImageToServer(shoeId: String, localPath: String): String? {
        return try {
            val file = java.io.File(localPath)
            if (!file.exists()) return null

            val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = okhttp3.MultipartBody.Part.createFormData("avatar", file.name, requestBody)

            val jsonRequest = RequestBuilder.createEmptyBodyRequest(preferencesManager)
            val jsonString = com.google.gson.Gson().toJson(jsonRequest)
            val jsonBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

            val response = api.uploadShoeImage(imagePart, jsonBody)
            if (response.isSuccess()) {
                response.data?.uploadResponse?.firstOrNull()?.imageUrl
            } else {
                null
            }
        } catch (e: Exception) {
            RLog.w("RunningShoeRepo", "uploadImageToServer failed: ${e.message}")
            null
        }
    }

    suspend fun retryPendingImageUploads() {
        try {
            val unsyncedShoes = dao.getUnsyncedShoes(getUserId())
            for (shoe in unsyncedShoes) {
                val localPath = imageManager.getImagePath(shoe.id)
                // Has local image but no server imageUrl → needs upload
                if (localPath != null && shoe.imageUrl == null) {
                    uploadImageToServer(shoe.id, localPath)?.let { serverUrl ->
                        dao.update(shoe.copy(
                            imageUrl = serverUrl,
                            updatedAt = System.currentTimeMillis()
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            RLog.w("RunningShoeRepo", "retryPendingImageUploads failed: ${e.message}")
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
