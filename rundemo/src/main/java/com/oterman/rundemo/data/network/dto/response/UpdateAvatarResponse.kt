package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 上传头像响应DTO
 * 对应iOS的uploadAvatar接口响应
 */
data class UpdateAvatarResponse(
    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("success")
    val isSuccess: Boolean = false
)

/**
 * 上传头像响应数据包装
 */
data class UpdateAvatarResponseData(
    @SerializedName("UpdateAvatarResponseDto")
    val updateAvatarResponseDto: List<UpdateAvatarResponse>? = null
)
