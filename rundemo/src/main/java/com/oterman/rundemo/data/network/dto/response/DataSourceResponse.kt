package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName
import com.oterman.rundemo.domain.model.PlatformBindStatus

/**
 * 数据源响应数据包装类
 */
data class DataSourceResponseData(
    @SerializedName("PlatformStatusResponse")
    val platformStatusResponseDto: List<PlatformStatusResponse>? = null,

    @SerializedName("GarminBindResponse")
    val garminBindResponse: List<GarminBindResponse>? = null,

    @SerializedName("CorosBindResponse")
    val corosBindResponse: List<CorosBindResponse>? = null,

    @SerializedName("GarminCallbackResponse")
    val garminCallbackResponse: List<CallbackResponse>? = null,

    @SerializedName("CorosCallbackResponse")
    val corosCallbackResponse: List<CallbackResponse>? = null,

    @SerializedName("GarminUnbindResponse")
    val garminUnbindResponse: List<UnbindResponse>? = null,

    @SerializedName("CorosUnbindResponse")
    val corosUnbindResponse: List<UnbindResponse>? = null,

    @SerializedName("GarminFileListResponse")
    val garminFileListResponse: List<FileListResponse>? = null,

    @SerializedName("CorosFileListResponse")
    val corosFileListResponse: List<FileListResponse>? = null,

    @SerializedName("GarminBackfillResponse")
    val garminBackfillResponse: List<BackfillResponse>? = null,

    @SerializedName("CorosBackfillResponse")
    val corosBackfillResponse: List<BackfillResponse>? = null,

    @SerializedName("FitFileDetailResponse")
    val fitFileDetailResponse: List<FitFileDetailResponse>? = null,

    @SerializedName("GarminHealthQueryResponse")
    val garminHealthQueryResponse: List<GarminHealthQueryResponse>? = null
)

/**
 * 平台状态响应
 */
data class PlatformStatusResponse(
    @SerializedName("platformCode")
    val platformCode: String,
    
    @SerializedName("bindStatus")
    val bindStatusCode: String
) {
    val bindStatus: PlatformBindStatus
        get() = PlatformBindStatus.fromCode(bindStatusCode)
    
    val isBound: Boolean
        get() = bindStatus.isBound
}

/**
 * 佳明绑定响应
 * 服务端返回: {"GarminBindResponse":[{"redirectUrl":"..."}]}
 */
data class GarminBindResponse(
    @SerializedName("redirectUrl")
    val redirectUrl: String
)

/**
 * 高驰绑定响应
 * 服务端返回: {"CorosBindResponse":[{"authUrl":"..."}]}
 */
data class CorosBindResponse(
    @SerializedName("authUrl")
    val authUrl: String
)

/**
 * 回调响应
 */
data class CallbackResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String? = null
)

/**
 * 解绑响应
 */
data class UnbindResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String? = null
)

/**
 * 文件列表响应
 */
data class FileListResponse(
    @SerializedName("files")
    val files: List<FileInfoDto>
)

/**
 * 文件信息DTO
 */
data class FileInfoDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("platformCode")
    val platformCode: String,

    @SerializedName("summaryId")
    val summaryId: String,

    @SerializedName("dataDate")
    val dataDate: String,

    @SerializedName("deviceName")
    val deviceName: String = "",

    @SerializedName("ossUrl")
    val ossUrl: String? = null,

    @SerializedName("fitUrl")
    val fitUrl: String? = null
)

/**
 * 数据回填响应
 */
data class BackfillResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("totalRequests")
    val totalRequests: Int
)

/**
 * 健康数据查询响应
 */
data class GarminHealthQueryResponse(
    @SerializedName("dailyData")
    val dailyData: List<DailyHealthData>? = null
)

/**
 * 每日健康数据
 */
data class DailyHealthData(
    @SerializedName("calendarDate")
    val calendarDate: String,

    @SerializedName("restingHeartRateInBeatsPerMinute")
    val restingHeartRateInBeatsPerMinute: Int?,

    @SerializedName("vo2Max")
    val vo2Max: Double?
)

