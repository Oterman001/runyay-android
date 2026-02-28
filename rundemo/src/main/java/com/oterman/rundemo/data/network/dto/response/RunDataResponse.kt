package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName
import com.oterman.rundemo.data.network.dto.RunSummaryBasicInfoDto

/**
 * 统一文件列表响应数据包装
 */
data class ActivityFileListResponseData(
    @SerializedName("ActivityFileListResponse")
    val activityFileListResponse: List<ActivityFileListResponse>? = null
)

/**
 * 统一文件列表响应
 */
data class ActivityFileListResponse(
    @SerializedName("files")
    val files: List<UnifiedFileInfoDto>? = null
)

/**
 * 统一文件信息DTO
 * 扩展现有FileInfoDto，新增runSummary字段
 */
data class UnifiedFileInfoDto(
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
    val fitUrl: String? = null,

    @SerializedName("runSummary")
    val runSummary: RunSummaryBasicInfoDto? = null
)

/**
 * 跑步数据上传响应数据包装
 */
data class RunDataUploadResponseData(
    @SerializedName("RunRecordUploadResponseDto")
    val runRecordUploadResponseDto: List<RunDataUploadResponse>? = null
)

/**
 * 跑步数据上传响应
 */
data class RunDataUploadResponse(
    @SerializedName("totalCount")
    val totalCount: Int = 0,

    @SerializedName("successCount")
    val successCount: Int = 0,

    @SerializedName("failedCount")
    val failedCount: Int = 0,

    @SerializedName("failedRecords")
    val failedRecords: List<String>? = null
)

/**
 * 跑步摘要更新响应数据包装
 */
data class RunSummaryUpdateResponseData(
    @SerializedName("RunSummaryUpdateResponseDto")
    val runSummaryUpdateResponseDto: List<RunSummaryUpdateResponse>? = null
)

/**
 * 跑步摘要更新响应
 */
data class RunSummaryUpdateResponse(
    @SerializedName("summaryId")
    val summaryId: String? = null,

    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("updatedFieldCount")
    val updatedFieldCount: Int = 0,

    @SerializedName("message")
    val message: String? = null
)

/**
 * 跑步记录删除响应数据包装
 */
data class RunSummaryDeleteResponseData(
    @SerializedName("RunSummaryDeleteResponseDto")
    val runSummaryDeleteResponseDto: List<RunSummaryDeleteResponse>? = null
)

/**
 * 跑步记录删除响应
 */
data class RunSummaryDeleteResponse(
    @SerializedName("summaryId")
    val summaryId: String? = null,

    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("activityFileDeleted")
    val activityFileDeleted: Boolean = false,

    @SerializedName("message")
    val message: String? = null
)

/**
 * 跑步摘要列表响应数据包装
 */
data class RunSummaryListResponseData(
    @SerializedName("RunSummaryQueryResponseDto")
    val runSummaryQueryResponseDto: List<RunSummaryListResponse>? = null
)

/**
 * 跑步摘要列表响应
 */
data class RunSummaryListResponse(
    @SerializedName("records")
    val records: List<RunSummaryBasicInfoDto>? = null,

    @SerializedName("totalCount")
    val totalCount: Int = 0
)
