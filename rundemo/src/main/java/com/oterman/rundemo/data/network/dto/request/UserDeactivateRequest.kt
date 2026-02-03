package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 注销账号请求DTO
 * 对应iOS的deactivateAccount接口
 */
data class UserDeactivateRequest(
    @SerializedName("confirmPassword")
    val confirmPassword: String,

    @SerializedName("reason")
    val reason: String = ""
)
