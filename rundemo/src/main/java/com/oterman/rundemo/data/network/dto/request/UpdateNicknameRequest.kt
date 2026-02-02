package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 更新昵称请求DTO
 * 对应iOS的updateNickname接口
 */
data class UpdateNicknameRequest(
    @SerializedName("nickname")
    val nickname: String
)
