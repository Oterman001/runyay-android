package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 头像URL响应DTO
 * 包含临时带签名的头像访问URL
 */
data class AvatarUrlResponse(
    @SerializedName("avatarUrl")
    val avatarUrl: String?,

    @SerializedName("expirationTime")
    val expirationTime: Long? = null,

    @SerializedName("expirationSeconds")
    val expirationSeconds: Int? = null
)

/**
 * 头像URL响应数据包装类
 */
data class AvatarUrlResponseData(
    @SerializedName("AvatarUrlResponseDto")
    val avatarUrlResponseDto: List<AvatarUrlResponse>? = null
)
