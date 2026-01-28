package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 请求头部模型
 * 对应iOS的RequestHead
 */
data class RequestHead(
    @SerializedName("appKey")
    val appKey: String,
    
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("sign")
    val sign: String,
    
    @SerializedName("token")
    val token: String = "",
    
    @SerializedName("userId")
    val userId: String = ""
)

/**
 * 基础请求包装类
 * 对应iOS的BaseRequest，包含head和body
 */
data class BaseRequest<T>(
    @SerializedName("head")
    val head: RequestHead,
    
    @SerializedName("body")
    val body: Map<String, List<T>>
)

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

