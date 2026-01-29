package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 用户注册请求DTO
 * 对应iOS的UserRegisterRequestDto
 */
data class UserRegisterRequest(
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    
    @SerializedName("verificationCode")
    val verificationCode: String,
    
    @SerializedName("userName")
    val userName: String? = null,
    
    @SerializedName("password")
    val password: String? = null,
    
    @SerializedName("email")
    val email: String? = null,
    
    @SerializedName("deviceId")
    val deviceId: String,
    
    @SerializedName("sceneType")
    val sceneType: String = "REGISTER"
)

