package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 退出登录请求DTO
 * 对应iOS的UserLogoutRequestDto
 */
data class UserLogoutRequest(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("deviceId")
    val deviceId: String
)
