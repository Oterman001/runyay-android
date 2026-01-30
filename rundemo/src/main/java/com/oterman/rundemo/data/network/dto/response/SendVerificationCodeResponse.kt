package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 发送验证码响应DTO
 * 对应iOS的SendVerificationCodeResponseDto
 */
data class SendVerificationCodeResponse(
    /**
     * 发送状态：S-成功，F-发送失败，C-需要验证图形验证码
     */
    @SerializedName("sendFlag")
    val sendFlag: String,
    
    /**
     * sendFlag=C时返回，图形验证码场景代码
     */
    @SerializedName("captureSceneCode")
    val captureSceneCode: String? = null,
    
    /**
     * 用户是否不存在：Y-不存在，N-已存在
     */
    @SerializedName("notExist")
    val notExist: String? = null
) {
    /**
     * 是否发送成功
     */
    val isSuccess: Boolean
        get() = sendFlag == "S"
    
    /**
     * 是否需要验证图形验证码
     */
    val needCaptcha: Boolean
        get() = sendFlag == "C"
    
    /**
     * 是否发送失败
     */
    val isFailed: Boolean
        get() = sendFlag == "F"
    
    /**
     * 用户是否已存在
     */
    val userExists: Boolean
        get() = notExist == "N"
    
    /**
     * 用户是否不存在（可以注册）
     */
    val canRegister: Boolean
        get() = notExist == "Y"
    
    /**
     * 用户是否不存在
     */
    val userNotExist: Boolean
        get() = notExist == "Y"
}

