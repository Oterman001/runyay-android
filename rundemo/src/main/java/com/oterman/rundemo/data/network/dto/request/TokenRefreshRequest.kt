package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Token刷新请求DTO
 * 对应iOS的TokenRefreshRequestDto
 */
data class TokenRefreshRequest(
    @SerializedName("deviceId")
    val deviceId: String
)
