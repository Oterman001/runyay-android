package com.oterman.rundemo.data.network.api

import com.oterman.rundemo.data.network.dto.request.BaseRequest
import com.oterman.rundemo.data.network.dto.request.UserLoginRequest
import com.oterman.rundemo.data.network.dto.response.BaseResponse
import com.oterman.rundemo.data.network.dto.response.ResponseData
import com.oterman.rundemo.data.network.dto.response.UserLoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 用户相关API接口
 */
interface UserApi {
    /**
     * 密码登录
     */
    @POST("api/user/login")
    suspend fun login(
        @Body request: BaseRequest<UserLoginRequest>
    ): BaseResponse<ResponseData<UserLoginResponse>>
}

