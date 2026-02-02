package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 更新昵称响应DTO
 * 对应iOS的updateNickname接口响应
 */
data class UpdateNicknameResponse(
    @SerializedName("userName")
    val userName: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("success")
    val isSuccess: Boolean = false
)

/**
 * 更新昵称响应数据包装
 */
data class UpdateNicknameResponseData(
    @SerializedName("UpdateNicknameResponseDto")
    val updateNicknameResponseDto: List<UpdateNicknameResponse>? = null
)
