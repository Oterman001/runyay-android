package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 用户登录响应DTO
 * 对应iOS的UserLoginResponseDto
 */
data class UserLoginResponse(
    @SerializedName("userId")
    val userId: String? = null,
    
    @SerializedName("userName")
    val userName: String? = null,
    
    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,
    
    @SerializedName("email")
    val email: String? = null,
    
    @SerializedName("token")
    val token: String? = null,
    
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("expireDay")
    val expireDay: Int? = null,
    
    @SerializedName("setPswFlg")
    val setPswFlg: String? = null,
    
    @SerializedName("success")
    val success: String? = null,
    
    @SerializedName("notExist")
    val notExist: String? = null,
    
    @SerializedName("remainingAttempts")
    val remainingAttempts: Int? = null,
    
    @SerializedName("appAccountToken")
    val appAccountToken: String? = null
) {
    /**
     * 是否需要设置密码
     */
    val needSetPassword: Boolean
        get() = setPswFlg == "Y"
    
    /**
     * 是否登录成功
     */
    val isLoginSuccess: Boolean
        get() = success == "Y"
    
    /**
     * 用户是否存在
     */
    val userExists: Boolean
        get() = notExist == "N"
    
    /**
     * 是否有剩余尝试次数
     */
    val hasRemainingAttempts: Boolean
        get() = remainingAttempts != null && remainingAttempts > 0
}

