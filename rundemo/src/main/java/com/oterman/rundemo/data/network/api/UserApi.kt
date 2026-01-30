package com.oterman.rundemo.data.network.api

import com.oterman.rundemo.data.network.dto.request.BaseRequest
import com.oterman.rundemo.data.network.dto.request.ResetPasswordRequest
import com.oterman.rundemo.data.network.dto.request.SendVerificationCodeRequest
import com.oterman.rundemo.data.network.dto.request.SetPasswordAndNameRequest
import com.oterman.rundemo.data.network.dto.request.SetPasswordRequest
import com.oterman.rundemo.data.network.dto.request.UserLoginRequest
import com.oterman.rundemo.data.network.dto.request.UserRegisterRequest
import com.oterman.rundemo.data.network.dto.response.BaseResponse
import com.oterman.rundemo.data.network.dto.response.ResetPasswordResponse
import com.oterman.rundemo.data.network.dto.response.ResponseData
import com.oterman.rundemo.data.network.dto.response.SendVerificationCodeResponse
import com.oterman.rundemo.data.network.dto.response.SetPasswordResponse
import com.oterman.rundemo.data.network.dto.response.UserLoginResponse
import com.oterman.rundemo.data.network.dto.response.UserRegisterResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 用户相关API接口
 */
interface UserApi {
    /**
     * 密码登录
     * 对应iOS的NetApiConstants.passwordLogin: "/api/user/login/passwordLogin"
     */
    @POST("api/user/login/passwordLogin")
    suspend fun login(
        @Body request: BaseRequest<UserLoginRequest>
    ): BaseResponse<ResponseData<UserLoginResponse>>
    
    /**
     * 发送验证码
     * 对应iOS的NetApiConstants.sendVerificationCode: "/api/user/register/sendVerificationCode"
     */
    @POST("api/user/register/sendVerificationCode")
    suspend fun sendVerificationCode(
        @Body request: BaseRequest<SendVerificationCodeRequest>
    ): BaseResponse<ResponseData<SendVerificationCodeResponse>>
    
    /**
     * 用户注册
     * 对应iOS的NetApiConstants.userRegister: "/api/user/register/register"
     */
    @POST("api/user/register/register")
    suspend fun register(
        @Body request: BaseRequest<UserRegisterRequest>
    ): BaseResponse<ResponseData<UserRegisterResponse>>
    
    /**
     * 设置密码和昵称
     * 对应iOS的NetApiConstants.userSetPasswordAndName: "/api/user/psw/setPasswordAndName"
     */
    @POST("api/user/psw/setPasswordAndName")
    suspend fun setPasswordAndName(
        @Body request: BaseRequest<SetPasswordAndNameRequest>
    ): BaseResponse<ResponseData<SetPasswordResponse>>
    
    /**
     * 发送重置密码验证码
     * 对应iOS的NetApiConstants.sendResetVerificationCode: "/api/user/psw/sendResetVerificationCode"
     */
    @POST("api/user/psw/sendResetVerificationCode")
    suspend fun sendResetVerificationCode(
        @Body request: BaseRequest<SendVerificationCodeRequest>
    ): BaseResponse<ResponseData<SendVerificationCodeResponse>>
    
    /**
     * 验证重置密码验证码
     * 对应iOS的NetApiConstants.resetPassword: "/api/user/psw/resetPassword"
     */
    @POST("api/user/psw/resetPassword")
    suspend fun verifyResetCode(
        @Body request: BaseRequest<ResetPasswordRequest>
    ): BaseResponse<ResponseData<ResetPasswordResponse>>
    
    /**
     * 设置密码（重置密码时使用）
     * 对应iOS的NetApiConstants.userSetPassword: "/api/user/psw/setPassword"
     */
    @POST("api/user/psw/setPassword")
    suspend fun setPassword(
        @Body request: BaseRequest<SetPasswordRequest>
    ): BaseResponse<ResponseData<SetPasswordResponse>>
}

