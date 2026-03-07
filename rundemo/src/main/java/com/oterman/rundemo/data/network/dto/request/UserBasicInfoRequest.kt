package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

data class SaveUserBasicInfoRequest(
    @SerializedName("manualRestingHeartRate") val manualRestingHeartRate: Int? = null,
    @SerializedName("maxHeartRate") val maxHeartRate: Int? = null,
    @SerializedName("birthDate") val birthDate: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("restingHeartRateSource") val restingHeartRateSource: String? = "MANUAL",
    @SerializedName("dataSource") val dataSource: String = "MANUAL"
)
