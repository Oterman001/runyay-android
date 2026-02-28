package com.oterman.rundemo.data.network.api

import com.oterman.rundemo.data.network.dto.request.ActivityFileListRequest
import com.oterman.rundemo.data.network.dto.request.BaseRequest
import com.oterman.rundemo.data.network.dto.request.RunRecordUploadRequest
import com.oterman.rundemo.data.network.dto.request.RunSummaryDeleteRequest
import com.oterman.rundemo.data.network.dto.request.RunSummaryQueryRequest
import com.oterman.rundemo.data.network.dto.request.RunSummaryUpdateRequest
import com.oterman.rundemo.data.network.dto.response.ActivityFileListResponseData
import com.oterman.rundemo.data.network.dto.response.BaseResponse
import com.oterman.rundemo.data.network.dto.response.RunDataUploadResponseData
import com.oterman.rundemo.data.network.dto.response.RunSummaryDeleteResponseData
import com.oterman.rundemo.data.network.dto.response.RunSummaryListResponseData
import com.oterman.rundemo.data.network.dto.response.RunSummaryUpdateResponseData
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 跑步数据相关API接口
 */
interface RunDataApi {

    /**
     * 获取统一文件列表（含内嵌runSummary）
     */
    @POST("api/activityfile/list")
    suspend fun getActivityFileList(
        @Body request: BaseRequest<ActivityFileListRequest>
    ): BaseResponse<ActivityFileListResponseData>

    /**
     * 批量上传跑步基础数据
     */
    @POST("api/rundata/upload")
    suspend fun uploadRunRecords(
        @Body request: BaseRequest<RunRecordUploadRequest>
    ): BaseResponse<RunDataUploadResponseData>

    /**
     * 更新跑步摘要（note/feeling/shoe等）
     */
    @POST("api/rundata/summary/update")
    suspend fun updateRunSummary(
        @Body request: BaseRequest<RunSummaryUpdateRequest>
    ): BaseResponse<RunSummaryUpdateResponseData>

    /**
     * 删除跑步记录
     */
    @POST("api/rundata/summary/delete")
    suspend fun deleteRunSummary(
        @Body request: BaseRequest<RunSummaryDeleteRequest>
    ): BaseResponse<RunSummaryDeleteResponseData>

    /**
     * 查询跑步摘要列表
     */
    @POST("api/rundata/summary/list")
    suspend fun queryRunSummaryList(
        @Body request: BaseRequest<RunSummaryQueryRequest>
    ): BaseResponse<RunSummaryListResponseData>
}
