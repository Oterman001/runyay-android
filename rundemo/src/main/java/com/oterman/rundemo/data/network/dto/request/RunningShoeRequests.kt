package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 跑鞋批量保存请求DTO
 * POST /api/runningshoes/batch/save
 */
data class RunningShoeSaveDto(
    @SerializedName("shoeId") val shoeId: String,
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
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("isActive") val isActive: Boolean? = null,
    @SerializedName("isDefault") val isDefault: Boolean? = null,
    @SerializedName("syncStatus") val syncStatus: String? = null
)

/**
 * 跑鞋更新请求DTO
 * POST /api/runningshoes/update
 */
data class UpdateRunningShoeRequestDto(
    @SerializedName("shoeId") val shoeId: String,
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
    @SerializedName("isActive") val isActive: Boolean? = null,
    @SerializedName("isDefault") val isDefault: Boolean? = null,
    @SerializedName("syncStatus") val syncStatus: String? = null
)

/**
 * 跑鞋删除请求DTO
 * POST /api/runningshoes/delete
 */
data class DeleteRunningShoeRequestDto(
    @SerializedName("shoeId") val shoeId: String
)
