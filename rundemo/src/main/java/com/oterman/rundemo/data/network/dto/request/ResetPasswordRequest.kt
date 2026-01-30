package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 重置密码验证码验证请求DTO
 * 对应iOS的ResetPasswordRequestDto
 */
data class ResetPasswordRequest(
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    
    @SerializedName("verificationCode")
    val verificationCode: String,
    
    @SerializedName("deviceId")
    val deviceId: String,
    
    @SerializedName("sceneType")
    val sceneType: String = "RESET_PASSWORD"
)

