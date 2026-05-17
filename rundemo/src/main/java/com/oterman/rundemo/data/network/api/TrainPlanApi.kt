package com.oterman.rundemo.data.network.api

import com.oterman.rundemo.data.network.dto.request.BaseRequest
import com.oterman.rundemo.data.network.dto.request.DeleteTrainPlanRequestDto
import com.oterman.rundemo.data.network.dto.request.ClearPushedPlansRequestDto
import com.oterman.rundemo.data.network.dto.request.PushTrainPlanRequestDto
import com.oterman.rundemo.data.network.dto.request.SaveTrainPlanRequestDto
import com.oterman.rundemo.data.network.dto.request.TrainPlanDetailRequestDto
import com.oterman.rundemo.data.network.dto.request.TrainPlanListRequestDto
import com.oterman.rundemo.data.network.dto.response.BaseResponse
import com.oterman.rundemo.data.network.dto.response.BatchSaveTrainPlanResponseData
import com.oterman.rundemo.data.network.dto.response.DeleteTrainPlanResponseData
import com.oterman.rundemo.data.network.dto.response.PushTrainPlanWrapperResponseData
import com.oterman.rundemo.data.network.dto.response.TrainPlanDetailWrapperResponseData
import com.oterman.rundemo.data.network.dto.response.TrainPlanListWrapperResponseData
import retrofit2.http.Body
import retrofit2.http.POST

interface TrainPlanApi {

    @POST("api/trainplan/save")
    suspend fun saveTrainPlans(
        @Body request: BaseRequest<SaveTrainPlanRequestDto>
    ): BaseResponse<BatchSaveTrainPlanResponseData>

    @POST("api/trainplan/detail")
    suspend fun getTrainPlanDetail(
        @Body request: BaseRequest<TrainPlanDetailRequestDto>
    ): BaseResponse<TrainPlanDetailWrapperResponseData>

    @POST("api/trainplan/list")
    suspend fun listTrainPlans(
        @Body request: BaseRequest<TrainPlanListRequestDto>
    ): BaseResponse<TrainPlanListWrapperResponseData>

    @POST("api/trainplan/delete")
    suspend fun deleteTrainPlans(
        @Body request: BaseRequest<DeleteTrainPlanRequestDto>
    ): BaseResponse<DeleteTrainPlanResponseData>

    @POST("api/trainplan/push")
    suspend fun pushTrainPlan(
        @Body request: BaseRequest<PushTrainPlanRequestDto>
    ): BaseResponse<PushTrainPlanWrapperResponseData>

    @POST("api/trainplan/push/delete")
    suspend fun deletePushedTrainPlan(
        @Body request: BaseRequest<PushTrainPlanRequestDto>
    ): BaseResponse<String>

    @POST("api/trainplan/push/clear")
    suspend fun clearPushedTrainPlans(
        @Body request: BaseRequest<ClearPushedPlansRequestDto>
    ): BaseResponse<String>
}
