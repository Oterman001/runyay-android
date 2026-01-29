package com.oterman.rundemo.data.repository

import android.content.Context
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.network.api.UserApi
import com.oterman.rundemo.data.network.dto.request.BaseRequest
import com.oterman.rundemo.data.network.dto.request.RequestHead
import com.oterman.rundemo.data.network.dto.request.SendVerificationCodeRequest
import com.oterman.rundemo.data.network.dto.request.SetPasswordAndNameRequest
import com.oterman.rundemo.data.network.dto.request.UserLoginRequest
import com.oterman.rundemo.data.network.dto.request.UserRegisterRequest
import com.oterman.rundemo.data.network.dto.response.SendVerificationCodeResponse
import com.oterman.rundemo.data.network.dto.response.UserLoginResponse
import com.oterman.rundemo.data.network.dto.response.UserRegisterResponse
import com.oterman.rundemo.domain.model.UserInfo
import com.oterman.rundemo.util.Constants
import com.oterman.rundemo.util.SecurityUtils
import com.oterman.rundemo.util.SecurityUtils.md5

/**
 * 用户数据仓库
 * 负责处理用户相关的数据操作，包括网络请求和本地存储
 * 对应iOS的UserService部分功能
 */
class UserRepository(
    private val context: Context,
    private val userApi: UserApi = RetrofitClient.userApi,
    private val preferencesManager: PreferencesManager = PreferencesManager(context)
) {
    
    /**
     * 用户登录
     * @param phoneNumber 手机号
     * @param password 密码（明文）
     * @return Result<UserLoginResponse> 登录结果
     */
    suspend fun login(phoneNumber: String, password: String): Result<UserLoginResponse> {
        return try {
            // 获取设备ID
            val deviceId = SecurityUtils.getDeviceId(context)
            
            // MD5加密密码
            val encryptedPassword = password.md5()
            
            // 生成时间戳
            val timestamp = SecurityUtils.getTimestamp()
            
            // 生成签名
            val sign = SecurityUtils.generateSign(
                params = emptyMap(),
                timestamp = timestamp,
                appKey = Constants.Network.APP_KEY
            )
            
            // 构建请求头
            val requestHead = RequestHead(
                appKey = Constants.Network.APP_KEY,
                timestamp = timestamp,
                sign = sign,
                token = preferencesManager.getUserToken() ?: "",
                userId = preferencesManager.getUserId() ?: ""
            )
            
            // 构建请求体
            val requestDto = UserLoginRequest(
                phoneNumber = phoneNumber,
                password = encryptedPassword,
                deviceId = deviceId
            )
            
            // 构建完整请求（对应iOS的BaseRequest结构）
            val request = BaseRequest(
                head = requestHead,
                body = mapOf("UserLoginRequestDto" to listOf(requestDto))
            )
            
            // 发送网络请求
            val response = userApi.login(request)
            
            // 检查响应是否成功
            if (response.isSuccess()) {
                val data = response.data?.userLoginResponseDto?.firstOrNull()
                if (data != null && data.isLoginSuccess) {
                    // 登录成功，保存用户信息
                    saveUserInfoToLocal(data)
                    Result.success(data)
                } else {
                    // 业务逻辑失败（如密码错误）
                    val errorMsg = data?.message ?: "登录失败，请稍后重试"
                    Result.failure(LoginException(errorMsg, data))
                }
            } else {
                // 网络请求失败
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            // 异常处理
            Result.failure(e)
        }
    }
    
    /**
     * 保存用户信息到本地
     */
    private fun saveUserInfoToLocal(response: UserLoginResponse) {
        response.userId?.let { userId ->
            response.token?.let { token ->
                preferencesManager.saveUserInfo(
                    userId = userId,
                    userName = response.userName,
                    phoneNumber = response.phoneNumber,
                    email = response.email,
                    token = token,
                    imageUrl = response.imageUrl,
                    expireDay = response.expireDay
                )
            }
        }
    }
    
    /**
     * 获取当前用户信息
     */
    fun getCurrentUser(): UserInfo? {
        val userId = preferencesManager.getUserId() ?: return null
        val token = preferencesManager.getUserToken() ?: return null
        
        return UserInfo(
            userId = userId,
            userName = preferencesManager.getUserName(),
            phoneNumber = preferencesManager.getPhoneNumber(),
            email = preferencesManager.getEmail(),
            token = token,
            imageUrl = preferencesManager.getImageUrl(),
            tokenExpireDate = preferencesManager.getTokenExpireDate()
        )
    }
    
    /**
     * 检查用户是否已登录
     */
    fun isUserLoggedIn(): Boolean {
        return preferencesManager.isUserLoggedIn()
    }
    
    /**
     * 用户登出
     */
    fun logout() {
        preferencesManager.clearUserData()
    }
    
    /**
     * 获取用户Token
     */
    fun getUserToken(): String? {
        return preferencesManager.getUserToken()
    }
    
    // ==================== 注册相关方法 ====================
    
    /**
     * 发送注册验证码
     * @param phoneNumber 手机号
     * @param captureParam 图形验证码参数（可选）
     * @return Result<SendVerificationCodeResponse> 发送结果
     */
    suspend fun sendRegisterVerificationCode(
        phoneNumber: String,
        captureParam: String = ""
    ): Result<SendVerificationCodeResponse> {
        return try {
            // 生成时间戳和签名
            val timestamp = SecurityUtils.getTimestamp()
            val sign = SecurityUtils.generateSign(
                params = emptyMap(),
                timestamp = timestamp,
                appKey = Constants.Network.APP_KEY
            )
            
            // 构建请求头
            val requestHead = RequestHead(
                appKey = Constants.Network.APP_KEY,
                timestamp = timestamp,
                sign = sign,
                token = "",
                userId = ""
            )
            
            // 构建请求体
            val requestDto = SendVerificationCodeRequest.forRegister(
                phoneNumber = phoneNumber,
                captureParam = captureParam
            )
            
            // 构建完整请求
            val request = BaseRequest(
                head = requestHead,
                body = mapOf("SendVerificationCodeRequestDto" to listOf(requestDto))
            )
            
            // 发送网络请求
            val response = userApi.sendVerificationCode(request)
            
            // 检查响应
            if (response.isSuccess()) {
                val data = response.data?.sendVerificationCodeResponseDto?.firstOrNull()
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("响应数据为空"))
                }
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 用户注册（验证短信验证码）
     * @param phoneNumber 手机号
     * @param verificationCode 验证码
     * @return Result<UserRegisterResponse> 注册结果
     */
    suspend fun registerWithSmsCode(
        phoneNumber: String,
        verificationCode: String
    ): Result<UserRegisterResponse> {
        return try {
            // 获取设备ID
            val deviceId = SecurityUtils.getDeviceId(context)
            
            // 生成时间戳和签名
            val timestamp = SecurityUtils.getTimestamp()
            val sign = SecurityUtils.generateSign(
                params = emptyMap(),
                timestamp = timestamp,
                appKey = Constants.Network.APP_KEY
            )
            
            // 构建请求头
            val requestHead = RequestHead(
                appKey = Constants.Network.APP_KEY,
                timestamp = timestamp,
                sign = sign,
                token = "",
                userId = ""
            )
            
            // 构建请求体（注册时不设置用户名和密码）
            val requestDto = UserRegisterRequest(
                phoneNumber = phoneNumber,
                verificationCode = verificationCode,
                deviceId = deviceId
            )
            
            // 构建完整请求
            val request = BaseRequest(
                head = requestHead,
                body = mapOf("UserRegisterRequestDto" to listOf(requestDto))
            )
            
            // 发送网络请求
            val response = userApi.register(request)
            
            // 检查响应
            if (response.isSuccess()) {
                val data = response.data?.userRegisterResponseDto?.firstOrNull()
                if (data != null) {
                    if (data.isSuccess) {
                        // 注册成功，临时保存用户信息（后续设置密码时会更新）
                        saveRegisterInfoToLocal(data)
                        Result.success(data)
                    } else if (data.userExists) {
                        Result.failure(RegisterException("该手机号已注册", data, isUserExists = true))
                    } else {
                        Result.failure(RegisterException(data.message ?: "注册失败", data))
                    }
                } else {
                    Result.failure(Exception("响应数据为空"))
                }
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 设置密码和昵称
     * @param userId 用户ID
     * @param password 密码（明文）
     * @param nickname 昵称
     * @param token 用户Token
     * @return Result<Boolean> 设置结果
     */
    suspend fun setPasswordAndName(
        userId: String,
        password: String,
        nickname: String,
        token: String
    ): Result<Boolean> {
        return try {
            // MD5加密密码
            val encryptedPassword = password.md5()
            
            // 生成时间戳和签名
            val timestamp = SecurityUtils.getTimestamp()
            val sign = SecurityUtils.generateSign(
                params = emptyMap(),
                timestamp = timestamp,
                appKey = Constants.Network.APP_KEY
            )
            
            // 构建请求头（使用传入的token和userId）
            val requestHead = RequestHead(
                appKey = Constants.Network.APP_KEY,
                timestamp = timestamp,
                sign = sign,
                token = token,
                userId = userId
            )
            
            // 构建请求体
            val requestDto = SetPasswordAndNameRequest(
                userId = userId,
                newPassword = encryptedPassword,
                userName = nickname
            )
            
            // 构建完整请求
            val request = BaseRequest(
                head = requestHead,
                body = mapOf("UserSetPasswordAndNameRequestDto" to listOf(requestDto))
            )
            
            // 发送网络请求
            val response = userApi.setPasswordAndName(request)
            
            // 检查响应
            if (response.isSuccess()) {
                val data = response.data?.setPasswordResponseDto?.firstOrNull()
                if (data?.isSuccess == true) {
                    // 更新本地保存的用户名
                    preferencesManager.updateUserName(nickname)
                    Result.success(true)
                } else {
                    Result.success(true) // 某些接口可能不返回data，只要code成功即可
                }
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 保存注册信息到本地
     */
    private fun saveRegisterInfoToLocal(response: UserRegisterResponse) {
        response.userId?.let { userId ->
            response.token?.let { token ->
                preferencesManager.saveUserInfo(
                    userId = userId,
                    userName = response.userName,
                    phoneNumber = response.phoneNumber,
                    email = response.email,
                    token = token,
                    imageUrl = response.imageUrl,
                    expireDay = response.expireDay
                )
            }
        }
    }
}

/**
 * 登录异常
 * 用于携带详细的登录失败信息
 */
class LoginException(
    message: String,
    val response: UserLoginResponse? = null
) : Exception(message)

/**
 * 注册异常
 * 用于携带详细的注册失败信息
 */
class RegisterException(
    message: String,
    val response: UserRegisterResponse? = null,
    val isUserExists: Boolean = false
) : Exception(message)

