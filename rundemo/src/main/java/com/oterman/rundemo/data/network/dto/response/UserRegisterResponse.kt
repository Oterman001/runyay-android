package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 用户注册响应DTO
 * 对应iOS的UserRegisterResponseDto
 */
data class UserRegisterResponse(
    /**
     * 用户ID
     */
    @SerializedName("userId")
    val userId: String? = null,
    
    /**
     * 用户昵称
     */
    @SerializedName("userName")
    val userName: String? = null,
    
    /**
     * 手机号码
     */
    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,
    
    /**
     * 邮箱地址
     */
    @SerializedName("email")
    val email: String? = null,
    
    /**
     * 登录令牌
     */
    @SerializedName("token")
    val token: String? = null,
    
    /**
     * 响应消息
     */
    @SerializedName("message")
    val message: String? = null,
    
    /**
     * 是否需要设置密码：Y-需要，N-不需要
     */
    @SerializedName("setPswFlg")
    val setPswFlg: String? = null,
    
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
     * Token过期天数
     */
    @SerializedName("expireDay")
    val expireDay: Int? = null,
    
    /**
     * 应用账户令牌（用于StoreKit购买）
     */
    @SerializedName("appAccountToken")
    val appAccountToken: String? = null,
    
    /**
     * 头像URL
     */
    @SerializedName("imageUrl")
    val imageUrl: String? = null
) {
    /**
     * 是否注册成功
     */
    val isSuccess: Boolean
        get() = success == "Y"
    
    /**
     * 是否需要设置密码
     */
    val needSetPassword: Boolean
        get() = setPswFlg == "Y"
    
    /**
     * 用户是否已存在
     */
    val userExists: Boolean
        get() = notExist == "N"
}

/**
 * 设置密码响应DTO
 */
data class SetPasswordResponse(
    /**
     * 是否成功：Y-成功，N-失败
     */
    @SerializedName("success")
    val success: String? = null,
    
    /**
     * 响应消息
     */
    @SerializedName("message")
    val message: String? = null
) {
    val isSuccess: Boolean
        get() = success == "Y"
}

