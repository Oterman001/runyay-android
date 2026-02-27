package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 平台状态查询请求
 */
data class PlatformStatusRequest(
    @SerializedName("userId")
    val userId: String
)

/**
 * 平台绑定请求（获取授权URL）
 */
data class PlatformBindRequest(
    @SerializedName("userId")
    val userId: String
)

/**
 * 佳明回调请求（OAuth 1.0a）
 * 对应iOS: GarminCallBackDto
 * dtoName: "GarminCallBackDto"
 */
data class GarminCallbackRequest(
    @SerializedName("oauthToken")
    val oauthToken: String,

    @SerializedName("oauthVerifier")
    val oauthVerifier: String,

    @SerializedName("userId")
    val userId: String
)

/**
 * 高驰回调请求（OAuth 2.0）
 * 对应iOS: CorosCallbackDto
 * dtoName: "CorosCallbackRequest"
 */
data class CorosCallbackRequest(
    @SerializedName("code")
    val code: String,

    @SerializedName("state")
    val state: String,

    @SerializedName("userId")
    val userId: String
)

/**
 * 平台解绑请求
 */
data class PlatformUnbindRequest(
    @SerializedName("userId")
    val userId: String
)

/**
 * 文件列表请求
 */
data class FileListRequest(
    @SerializedName("pageNum")
    val pageNum: Int = 1,
    
    @SerializedName("pageSize")
    val pageSize: Int = 15,
    
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("lastSyncTime")
    val lastSyncTime: String,
    
    @SerializedName("queryManual")
    val queryManual: String = "N"
)

/**
 * 文件下载请求
 */
data class FileDownloadRequest(
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("platformCode")
    val platformCode: String,
    
    @SerializedName("summaryId")
    val summaryId: String
)

/**
 * 数据回填请求
 */
data class BackfillRequest(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("startTime")
    val startTime: String,

    @SerializedName("endTime")
    val endTime: String
)

/**
 * 高驰运动详情请求
 * 对应iOS: CorosSportDetailRequest
 * dtoName: "CorosSportDetailRequest"
 */
data class CorosSportDetailRequest(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("labelId")
    val labelId: String,

    @SerializedName("platformCode")
    val platformCode: String
)

/**
 * 健康数据查询请求
 * 查询静息心率、VO2Max等健康数据
 */
data class HealthQueryRequest(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("platformCode")
    val platformCode: String,

    @SerializedName("startDate")
    val startDate: String,   // "yyyyMMdd"

    @SerializedName("endDate")
    val endDate: String      // "yyyyMMdd", must == startDate for Garmin
)

