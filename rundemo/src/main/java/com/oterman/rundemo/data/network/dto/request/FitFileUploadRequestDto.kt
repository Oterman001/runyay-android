package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

data class FitFileUploadRequestDto(
    @SerializedName("workoutId")
    val workoutId: String,

    @SerializedName("platformCode")
    val platformCode: String = "MANUAL",

    @SerializedName("userId")
    val userId: String,

    @SerializedName("deviceName")
    val deviceName: String? = null,

    @SerializedName("activityType")
    val activityType: String,

    @SerializedName("activityStartTime")
    val activityStartTime: String,

    @SerializedName("activityName")
    val activityName: String? = null
)
