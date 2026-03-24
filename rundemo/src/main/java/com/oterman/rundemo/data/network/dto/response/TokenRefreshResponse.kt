package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Token刷新响应DTO
 * 对应iOS的TokenRefreshResponseDto
 */
data class TokenRefreshResponse(
    @SerializedName("token")
    val token: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("expireDays")
    val expireDays: Int? = null
) {
    val isSuccess get() = !token.isNullOrEmpty()
}

/**
 * Token刷新响应数据包装类
 */
data class TokenRefreshResponseData(
    @SerializedName("TokenRefreshResponseDto")
    val tokenRefreshResponseDto: List<TokenRefreshResponse>? = null
)
