package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 重置密码验证码验证响应DTO
 * 对应iOS的ResetPasswordResponseDto
 */
data class ResetPasswordResponse(
    /**
     * 响应消息
     */
    @SerializedName("message")
    val message: String? = null,
    
    /**
     * 是否成功：Y-成功，N-失败
     */
    @SerializedName("success")
    val success: String? = null,
    
    /**
     * 用户是否不存在：Y-不存在，N-已存在
     */
    @SerializedName("notExist")
    val notExist: String? = null,
    
    /**
     * 用户ID（验证成功后返回）
     */
    @SerializedName("userId")
    val userId: String? = null,
    
    /**
     * Token（验证成功后返回）
     */
    @SerializedName("token")
    val token: String? = null
) {
    /**
     * 是否验证成功
     */
    val isVerifySuccess: Boolean
        get() = success == "Y"
    
    /**
     * 用户是否存在
     */
    val userExists: Boolean
        get() = notExist == "N"
    
    /**
     * 用户是否不存在
     */
    val userNotExist: Boolean
        get() = notExist == "Y"
}

