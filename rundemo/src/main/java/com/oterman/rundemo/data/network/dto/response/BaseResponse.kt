package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * 服务器统一响应结构
 */
data class BaseResponse<T>(
    @SerializedName("code")
    val code: String,
    
    @SerializedName("msg")
    val msg: String,
    
    @SerializedName("data")
    val data: T? = null
) {
    /**
     * 判断请求是否成功
     */
    fun isSuccess(): Boolean = code == "0000"
}

/**
 * 响应数据包装类
 */
data class ResponseData<T>(
    @SerializedName("UserLoginResponseDto")
    val userLoginResponseDto: List<T>? = null
)

