package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName
import com.oterman.rundemo.data.network.dto.RunSummaryBasicInfoDto

/**
 * 统一文件列表请求
 * POST /api/activityfile/list
 */
data class ActivityFileListRequest(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("platformCode")
    val platformCode: String,

    @SerializedName("lastSyncTime")
    val lastSyncTime: String,

    @SerializedName("pageNum")
    val pageNum: Int = 1,

    @SerializedName("pageSize")
    val pageSize: Int = 15,

    @SerializedName("queryManual")
    val queryManual: String = "Y"
)

/**
 * 跑步记录上传请求
 * POST /api/rundata/upload
 */
data class RunRecordUploadRequest(
    @SerializedName("records")
    val records: List<RunSummaryBasicInfoDto>
)

/**
 * 跑步摘要更新请求
 * POST /api/rundata/summary/update
 */
data class RunSummaryUpdateRequest(
    @SerializedName("summaryId")
    val summaryId: String,

    @SerializedName("activityName")
    val activityName: String? = null,

    @SerializedName("note")
    val note: String? = null,

    @SerializedName("feelingLevel")
    val feelingLevel: Int? = null,

    @SerializedName("shoeId")
    val shoeId: String? = null,

    @SerializedName("raceId")
    val raceId: String? = null
)

/**
 * 跑步记录删除请求
 * POST /api/rundata/summary/delete
 */
data class RunSummaryDeleteRequest(
    @SerializedName("summaryId")
    val summaryId: String
)

/**
 * 跑步摘要查询请求
 * POST /api/rundata/summary/list
 */
data class RunSummaryQueryRequest(
    @SerializedName("startDate")
    val startDate: String,

    @SerializedName("endDate")
    val endDate: String,

    @SerializedName("platformCode")
    val platformCode: String? = null,

    @SerializedName("activityType")
    val activityType: String? = null,

    @SerializedName("pageNum")
    val pageNum: Int = 1,

    @SerializedName("pageSize")
    val pageSize: Int = 20
)
