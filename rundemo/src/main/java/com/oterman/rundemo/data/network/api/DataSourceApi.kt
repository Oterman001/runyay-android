package com.oterman.rundemo.data.network.api

import com.oterman.rundemo.data.network.dto.request.BackfillRequest
import com.oterman.rundemo.data.network.dto.request.BaseRequest
import com.oterman.rundemo.data.network.dto.request.CorosCallbackRequest
import com.oterman.rundemo.data.network.dto.request.CorosSportDetailRequest
import com.oterman.rundemo.data.network.dto.request.FileDownloadRequest
import com.oterman.rundemo.data.network.dto.request.FileListRequest
import com.oterman.rundemo.data.network.dto.request.FitFileDetailRequest
import com.oterman.rundemo.data.network.dto.request.GarminCallbackRequest
import com.oterman.rundemo.data.network.dto.request.PlatformBindRequest
import com.oterman.rundemo.data.network.dto.request.PlatformStatusRequest
import com.oterman.rundemo.data.network.dto.request.PlatformUnbindRequest
import com.oterman.rundemo.data.network.dto.response.BaseResponse
import com.oterman.rundemo.data.network.dto.response.DataSourceResponseData
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * 数据源相关API接口
 */
interface DataSourceApi {
    
    // ============ 平台状态 ============
    
    /**
     * 查询平台绑定状态
     */
    @POST("api/user/profile/platform/status")
    suspend fun queryPlatformStatus(
        @Body request: BaseRequest<PlatformStatusRequest>
    ): BaseResponse<DataSourceResponseData>
    
    // ============ 佳明相关接口 ============
    
    /**
     * 获取佳明中国授权URL
     */
    @POST("garmin/china/bind")
    suspend fun bindGarminChina(
        @Body request: BaseRequest<PlatformBindRequest>
    ): BaseResponse<DataSourceResponseData>
    
    /**
     * 获取佳明国际授权URL
     */
    @POST("garmin/global/prd/bind")
    suspend fun bindGarminGlobal(
        @Body request: BaseRequest<PlatformBindRequest>
    ): BaseResponse<DataSourceResponseData>
    
    /**
     * 佳明中国授权回调
     */
    @POST("garmin/china/callback")
    suspend fun callbackGarminChina(
        @Body request: BaseRequest<GarminCallbackRequest>
    ): BaseResponse<DataSourceResponseData>

    /**
     * 佳明国际授权回调
     */
    @POST("garmin/global/prd/callback")
    suspend fun callbackGarminGlobal(
        @Body request: BaseRequest<GarminCallbackRequest>
    ): BaseResponse<DataSourceResponseData>
    
    /**
     * 解绑佳明中国
     */
    @POST("garmin/china/unbind")
    suspend fun unbindGarminChina(
        @Body request: BaseRequest<PlatformUnbindRequest>
    ): BaseResponse<DataSourceResponseData>
    
    /**
     * 解绑佳明国际
     */
    @POST("garmin/global/prd/unbind")
    suspend fun unbindGarminGlobal(
        @Body request: BaseRequest<PlatformUnbindRequest>
    ): BaseResponse<DataSourceResponseData>
    
    /**
     * 获取佳明待同步文件列表
     */
    @POST("garmin/file/list")
    suspend fun getGarminFileList(
        @Body request: BaseRequest<FileListRequest>
    ): BaseResponse<DataSourceResponseData>
    
    /**
     * 下载佳明FIT文件
     */
    @POST("garmin/file/download")
    @Streaming
    suspend fun downloadGarminFile(
        @Body request: BaseRequest<FileDownloadRequest>
    ): ResponseBody
    
    /**
     * 佳明中国数据回填
     */
    @POST("garmin/china/backfill")
    suspend fun backfillGarminChina(
        @Body request: BaseRequest<BackfillRequest>
    ): BaseResponse<DataSourceResponseData>
    
    /**
     * 佳明国际数据回填
     */
    @POST("garmin/global/prd/backfill")
    suspend fun backfillGarminGlobal(
        @Body request: BaseRequest<BackfillRequest>
    ): BaseResponse<DataSourceResponseData>
    
    // ============ 高驰相关接口 ============

    /**
     * 获取高驰授权URL
     */
    @POST("coros/bind")
    suspend fun bindCoros(
        @Body request: BaseRequest<PlatformBindRequest>
    ): BaseResponse<DataSourceResponseData>

    /**
     * 高驰授权回调
     */
    @POST("coros/callback")
    suspend fun callbackCoros(
        @Body request: BaseRequest<CorosCallbackRequest>
    ): BaseResponse<DataSourceResponseData>

    /**
     * 解绑高驰
     */
    @POST("coros/unbind")
    suspend fun unbindCoros(
        @Body request: BaseRequest<PlatformUnbindRequest>
    ): BaseResponse<DataSourceResponseData>

    /**
     * 获取高驰待同步文件列表
     */
    @POST("coros/file/list")
    suspend fun getCorosFileList(
        @Body request: BaseRequest<FileListRequest>
    ): BaseResponse<DataSourceResponseData>

    /**
     * 下载高驰运动详情/FIT文件
     */
    @POST("coros/sport/detail")
    @Streaming
    suspend fun downloadCorosFile(
        @Body request: BaseRequest<CorosSportDetailRequest>
    ): ResponseBody

    /**
     * 高驰数据回填
     */
    @POST("coros/backfill")
    suspend fun backfillCoros(
        @Body request: BaseRequest<BackfillRequest>
    ): BaseResponse<DataSourceResponseData>

    // ============ FIT文件下载 ============

    /**
     * 获取FIT文件详情（下载URL）
     */
    @POST("api/fit/detail")
    suspend fun getFitFileDetail(
        @Body request: BaseRequest<FitFileDetailRequest>
    ): BaseResponse<DataSourceResponseData>
}

