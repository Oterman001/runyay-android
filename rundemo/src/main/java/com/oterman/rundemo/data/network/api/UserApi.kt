package com.oterman.rundemo.data.network.api

import com.oterman.rundemo.data.network.dto.request.BaseRequest
import com.oterman.rundemo.data.network.dto.request.GetAvatarUrlRequest
import com.oterman.rundemo.data.network.dto.request.ResetPasswordRequest
import com.oterman.rundemo.data.network.dto.request.SendVerificationCodeRequest
import com.oterman.rundemo.data.network.dto.request.SetPasswordAndNameRequest
import com.oterman.rundemo.data.network.dto.request.SetPasswordRequest
import com.oterman.rundemo.data.network.dto.request.UpdateNicknameRequest
import com.oterman.rundemo.data.network.dto.request.UserDeactivateRequest
import com.oterman.rundemo.data.network.dto.request.UserLoginRequest
import com.oterman.rundemo.data.network.dto.request.UserLogoutRequest
import com.oterman.rundemo.data.network.dto.request.UserRegisterRequest
import com.oterman.rundemo.data.network.dto.response.AvatarUrlResponseData
import com.oterman.rundemo.data.network.dto.response.BaseResponse
import com.oterman.rundemo.data.network.dto.response.ResetPasswordResponse
import com.oterman.rundemo.data.network.dto.response.ResponseData
import com.oterman.rundemo.data.network.dto.response.SendVerificationCodeResponse
import com.oterman.rundemo.data.network.dto.response.SetPasswordResponse
import com.oterman.rundemo.data.network.dto.response.UpdateAvatarResponseData
import com.oterman.rundemo.data.network.dto.response.UpdateNicknameResponseData
import com.oterman.rundemo.data.network.dto.response.UserLoginResponse
import com.oterman.rundemo.data.network.dto.response.UserRegisterResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

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

    /**
     * 获取头像临时访问URL
     * 对应iOS的AvatarManager获取签名URL接口
     */
    @POST("api/user/profile/avatar/url")
    suspend fun getAvatarUrl(
        @Body request: BaseRequest<GetAvatarUrlRequest>
    ): BaseResponse<AvatarUrlResponseData>

    /**
     * 退出登录
     * 对应iOS的logout接口
     */
    @POST("api/user/login/logout")
    suspend fun logout(
        @Body request: BaseRequest<UserLogoutRequest>
    ): BaseResponse<Unit>

    /**
     * 注销账号
     * 对应iOS的deactivateAccount接口
     */
    @POST("api/user/login/deactivate")
    suspend fun deactivate(
        @Body request: BaseRequest<UserDeactivateRequest>
    ): BaseResponse<Unit>

    /**
     * 上传头像
     * 对应iOS的uploadAvatar接口
     */
    @Multipart
    @POST("api/user/profile/avatar")
    suspend fun uploadAvatar(
        @Part image: MultipartBody.Part,
        @Part("userId") userId: okhttp3.RequestBody
    ): BaseResponse<UpdateAvatarResponseData>

    /**
     * 更新昵称
     * 对应iOS的updateNickname接口
     */
    @POST("api/user/profile/nickname")
    suspend fun updateNickname(
        @Body request: BaseRequest<UpdateNicknameRequest>
    ): BaseResponse<UpdateNicknameResponseData>
}

