package com.oterman.rundemo.data.network.api

import com.oterman.rundemo.data.network.dto.request.BaseRequest
import com.oterman.rundemo.data.network.dto.request.DeleteRunningShoeRequestDto
import com.oterman.rundemo.data.network.dto.request.RunningShoeSaveDto
import com.oterman.rundemo.data.network.dto.request.UpdateRunningShoeRequestDto
import com.oterman.rundemo.data.network.dto.response.BaseResponse
import com.oterman.rundemo.data.network.dto.response.BatchSaveShoeResponseData
import com.oterman.rundemo.data.network.dto.response.DeleteShoeResponseData
import com.oterman.rundemo.data.network.dto.response.ListShoesResponseData
import com.oterman.rundemo.data.network.dto.response.UpdateShoeResponseData
import com.oterman.rundemo.data.network.dto.response.UploadShoeImageResponseData
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RunningShoeApi {

    @POST("api/runningshoes/batch/save")
    suspend fun batchSaveShoes(
        @Body request: BaseRequest<RunningShoeSaveDto>
    ): BaseResponse<BatchSaveShoeResponseData>

    @POST("api/runningshoes/update")
    suspend fun updateShoe(
        @Body request: BaseRequest<UpdateRunningShoeRequestDto>
    ): BaseResponse<UpdateShoeResponseData>

    @Multipart
    @POST("api/runningshoes/image/upload")
    suspend fun uploadShoeImage(
        @Part avatar: MultipartBody.Part,
        @Part("request") request: RequestBody
    ): BaseResponse<UploadShoeImageResponseData>

    @POST("api/runningshoes/delete")
    suspend fun deleteShoe(
        @Body request: BaseRequest<DeleteRunningShoeRequestDto>
    ): BaseResponse<DeleteShoeResponseData>

    @POST("api/runningshoes/list")
    suspend fun listShoes(
        @Body request: BaseRequest<Any>
    ): BaseResponse<ListShoesResponseData>
}
