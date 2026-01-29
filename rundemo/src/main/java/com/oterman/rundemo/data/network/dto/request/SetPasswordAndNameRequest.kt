package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 设置密码和昵称请求DTO
 * 对应iOS的UserSetPasswordAndNameRequestDto
 */
data class SetPasswordAndNameRequest(
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("newPassword")
    val newPassword: String,
    
    @SerializedName("oldPassword")
    val oldPassword: String = "",
    
    @SerializedName("userName")
    val userName: String
)

