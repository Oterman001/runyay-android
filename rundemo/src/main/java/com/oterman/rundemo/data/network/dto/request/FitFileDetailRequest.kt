package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * FIT文件详情请求
 * dtoName: "FitFileDetailRequest"
 */
data class FitFileDetailRequest(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("summaryId")
    val summaryId: String,

    @SerializedName("platformCode")
    val platformCode: String
)
