package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 场景类型枚举
 * 对应iOS的SceneType
 */
enum class SceneType(val value: String) {
    LOGIN("LOGIN"),
    REGISTER("REGISTER"),
    RESET_PASSWORD("RESET_PASSWORD")
}

/**
 * 发送验证码请求DTO
 * 对应iOS的SendVerificationCodeRequestDto
 */
data class SendVerificationCodeRequest(
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    
    @SerializedName("sceneType")
    val sceneType: String,
    
    @SerializedName("captureParam")
    val captureParam: String = ""
) {
    companion object {
        /**
         * 创建注册场景的验证码请求
         */
        fun forRegister(phoneNumber: String, captureParam: String = ""): SendVerificationCodeRequest {
            return SendVerificationCodeRequest(
                phoneNumber = phoneNumber,
                sceneType = SceneType.REGISTER.value,
                captureParam = captureParam
            )
        }
        
        /**
         * 创建重置密码场景的验证码请求
         */
        fun forResetPassword(phoneNumber: String, captureParam: String = ""): SendVerificationCodeRequest {
            return SendVerificationCodeRequest(
                phoneNumber = phoneNumber,
                sceneType = SceneType.RESET_PASSWORD.value,
                captureParam = captureParam
            )
        }
    }
}

