package com.oterman.rundemo.data.repository

import android.content.Context
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.network.api.UserApi
import com.oterman.rundemo.data.network.dto.request.BaseRequest
import com.oterman.rundemo.data.network.dto.request.RequestHead
import com.oterman.rundemo.data.network.dto.request.UserLoginRequest
import com.oterman.rundemo.data.network.dto.response.UserLoginResponse
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
}

/**
 * 登录异常
 * 用于携带详细的登录失败信息
 */
class LoginException(
    message: String,
    val response: UserLoginResponse? = null
) : Exception(message)

