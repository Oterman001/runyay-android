package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

data class UserBasicInfoResponse(
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("manualRestingHeartRate") val manualRestingHeartRate: Int? = null,
    @SerializedName("maxHeartRate") val maxHeartRate: Int? = null,
    @SerializedName("birthDate") val birthDate: String? = null,
    @SerializedName("gender") val gender: String? = null
)

data class UserBasicInfoResponseData(
    @SerializedName("UserBasicInfoResponseDto")
    val userBasicInfoResponseDto: List<UserBasicInfoResponse>? = null
)
