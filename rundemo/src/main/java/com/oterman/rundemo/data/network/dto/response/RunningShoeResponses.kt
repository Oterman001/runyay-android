package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 批量保存跑鞋响应
 */
data class BatchSaveShoeResponseData(
    @SerializedName("BatchSaveRunningShoesResponseDto")
    val batchSaveResponse: List<BatchSaveShoeResponse>? = null
)

data class BatchSaveShoeResponse(
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("successCount") val successCount: Int = 0,
    @SerializedName("failedCount") val failedCount: Int = 0,
    @SerializedName("failedRecords") val failedRecords: List<ShoeFailedRecord>? = null
)

data class ShoeFailedRecord(
    @SerializedName("shoeId") val shoeId: String? = null,
    @SerializedName("errorCode") val errorCode: String? = null,
    @SerializedName("errorMessage") val errorMessage: String? = null
)

/**
 * 更新跑鞋响应
 */
data class UpdateShoeResponseData(
    @SerializedName("UpdateRunningShoeResponseDto")
    val updateResponse: List<UpdateShoeResponse>? = null
)

data class UpdateShoeResponse(
    @SerializedName("shoeId") val shoeId: String? = null,
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("updatedFieldCount") val updatedFieldCount: Int = 0,
    @SerializedName("message") val message: String? = null
)

/**
 * 上传跑鞋图片响应
 */
data class UploadShoeImageResponseData(
    @SerializedName("UploadShoeImageResponseDto")
    val uploadResponse: List<UploadShoeImageResponse>? = null
)

data class UploadShoeImageResponse(
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("message") val message: String? = null
)

/**
 * 删除跑鞋响应
 */
data class DeleteShoeResponseData(
    @SerializedName("DeleteRunningShoeResponseDto")
    val deleteResponse: List<DeleteShoeResponse>? = null
)

data class DeleteShoeResponse(
    @SerializedName("shoeId") val shoeId: String? = null,
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null
)

/**
 * 跑鞋列表查询响应
 */
data class ListShoesResponseData(
    @SerializedName("RunningShoeDto")
    val shoes: List<RunningShoeDto>? = null
)

data class RunningShoeDto(
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("shoeId") val shoeId: String? = null,
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("shoeSize") val shoeSize: String? = null,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("shoeType") val shoeType: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("price") val price: Double? = null,
    @SerializedName("expectedLifespan") val expectedLifespan: Double? = null,
    @SerializedName("firstUseDate") val firstUseDate: String? = null,
    @SerializedName("retireDate") val retireDate: String? = null,
    @SerializedName("initialDistance") val initialDistance: Double? = null,
    @SerializedName("totalDistance") val totalDistance: Double? = null,
    @SerializedName("totalDuration") val totalDuration: Double? = null,
    @SerializedName("totalRuns") val totalRuns: Int? = null,
    @SerializedName("imagePath") val imagePath: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("isActive") val isActive: Boolean? = null,
    @SerializedName("isDefault") val isDefault: Boolean? = null,
    @SerializedName("syncStatus") val syncStatus: String? = null,
    @SerializedName("serverShoeId") val serverShoeId: String? = null,
    @SerializedName("lastSyncAt") val lastSyncAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("remainingLifespan") val remainingLifespan: Double? = null,
    @SerializedName("usagePercentage") val usagePercentage: Double? = null
)
