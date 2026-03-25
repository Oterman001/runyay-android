package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 服务器统一响应结构
 */
data class BaseResponse<T>(
    @SerializedName("code")
    val code: String,
    
    @SerializedName("msg")
    val msg: String,
    
    @SerializedName("data")
    val data: T? = null
) {
    /**
     * 判断请求是否成功
     */
    fun isSuccess(): Boolean = code == "0000"
}

/**
 * 响应数据包装类
 * 支持多种DTO类型的响应解析
 */
data class ResponseData<T>(
    @SerializedName("UserLoginResponseDto")
    val userLoginResponseDto: List<T>? = null,
    
    @SerializedName("SendVerificationCodeResponseDto")
    val sendVerificationCodeResponseDto: List<T>? = null,
    
    @SerializedName("UserRegisterResponseDto")
    val userRegisterResponseDto: List<T>? = null,
    
    @SerializedName("SetPasswordResponseDto")
    val setPasswordResponseDto: List<T>? = null,
    
    @SerializedName("ResetPasswordResponseDto")
    val resetPasswordResponseDto: List<T>? = null,

    @SerializedName("GetLatestVersionResponseDto")
    val getLatestVersionResponseDto: List<T>? = null
)

