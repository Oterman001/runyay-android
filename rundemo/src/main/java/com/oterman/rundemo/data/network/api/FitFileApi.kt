package com.oterman.rundemo.data.network.api

import com.oterman.rundemo.data.network.dto.response.BaseResponse
import com.oterman.rundemo.data.network.dto.response.FitFileUploadResponseData
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FitFileApi {
    @Multipart
    @POST("api/fit/upload")
    suspend fun uploadFitFile(
        @Part("request") request: RequestBody,
        @Part file: MultipartBody.Part
    ): BaseResponse<FitFileUploadResponseData>
}
