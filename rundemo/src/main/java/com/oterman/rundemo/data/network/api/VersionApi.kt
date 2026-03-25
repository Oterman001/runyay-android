package com.oterman.rundemo.data.network.api

import com.oterman.rundemo.data.network.dto.request.BaseRequest
import com.oterman.rundemo.data.network.dto.request.GetLatestVersionRequest
import com.oterman.rundemo.data.network.dto.response.BaseResponse
import com.oterman.rundemo.data.network.dto.response.ResponseData
import com.oterman.rundemo.data.network.dto.response.GetLatestVersionResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 版本检测API接口
 */
interface VersionApi {
    @POST("api/version/app/getLatest")
    suspend fun getLatestVersion(
        @Body request: BaseRequest<GetLatestVersionRequest>
    ): BaseResponse<ResponseData<GetLatestVersionResponse>>
}
