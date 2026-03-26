package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

data class GetLatestVersionResponse(
    @SerializedName("platform")
    val platform: String?,
    @SerializedName("versionName")
    val versionName: String?,
    @SerializedName("versionCode")
    val versionCode: Int?,
    @SerializedName("changelog")
    val changelog: String?,
    @SerializedName("downloadUrl")
    val downloadUrl: String?,
    @SerializedName("forceUpgrade")
    val forceUpgrade: Boolean?,
    @SerializedName("fileSize")
    val fileSize: Long?,
    @SerializedName("marketUrls")
    val marketUrls: Map<String, String>? = null
)
