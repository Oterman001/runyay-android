package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * FIT文件详情响应
 */
data class FitFileDetailResponse(
    @SerializedName("fitUrl")
    val fitUrl: String,

    @SerializedName("ossUrl")
    val ossUrl: String?,

    @SerializedName("summaryId")
    val summaryId: String
)
