package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

data class FitFileUploadResponseData(
    @SerializedName("FitFileUploadResponseDto")
    val fitFileUploadResponseDto: List<FitFileUploadResponseDto>? = null
)

data class FitFileUploadResponseDto(
    @SerializedName("workoutId")
    val workoutId: String? = null,

    @SerializedName("ossUrl")
    val ossUrl: String? = null,

    @SerializedName("status")
    val status: String? = null
)
