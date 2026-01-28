package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 用户登录请求DTO
 * 对应iOS的UserLoginRequestDto
 */
data class UserLoginRequest(
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    
    @SerializedName("loginType")
    val loginType: String = "PASSWORD",
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("deviceId")
    val deviceId: String,
    
    @SerializedName("sceneType")
    val sceneType: String = "LOGIN",
    
    @SerializedName("verificationCode")
    val verificationCode: String? = null
)

/**
 * 基础请求包装类
 */
data class BaseRequest<T>(
    @SerializedName("dtoName")
    val dtoName: String,
    
    @SerializedName("data")
    val data: List<T>
)

