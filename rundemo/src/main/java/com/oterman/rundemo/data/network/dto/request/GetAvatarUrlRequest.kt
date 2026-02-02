package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 获取头像URL请求DTO
 * 对应iOS的获取头像临时访问URL接口
 */
data class GetAvatarUrlRequest(
    @SerializedName("userId")
    val userId: String
)
