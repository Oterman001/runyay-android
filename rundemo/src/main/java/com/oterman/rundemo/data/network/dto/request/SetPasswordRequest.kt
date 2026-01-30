package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 设置密码请求DTO
 * 对应iOS的UserSetPswRequestDto
 */
data class SetPasswordRequest(
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("newPsw")
    val newPassword: String,
    
    @SerializedName("oldPsw")
    val oldPassword: String? = null
)

