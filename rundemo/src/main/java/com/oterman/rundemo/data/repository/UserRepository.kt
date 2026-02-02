package com.oterman.rundemo.data.repository

import android.content.Context
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.RequestBuilder
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.network.api.UserApi
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
import com.oterman.rundemo.data.network.dto.response.ResetPasswordResponse
import com.oterman.rundemo.data.network.dto.response.SendVerificationCodeResponse
import com.oterman.rundemo.data.network.dto.response.UserLoginResponse
import com.oterman.rundemo.data.network.dto.response.UserRegisterResponse
import com.oterman.rundemo.domain.model.UserInfo
import com.oterman.rundemo.util.Logger
import com.oterman.rundemo.util.SecurityUtils
import com.oterman.rundemo.util.SecurityUtils.md5
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

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
    companion object {
        private const val TAG = "UserRepository"
    }

    /**
     * 获取头像临时访问URL
     * OSS存储的头像需要通过API获取带签名的临时URL才能访问
     * @param userId 用户ID
     * @return Result<String?> 带签名的临时头像URL
     */
    suspend fun getAvatarUrl(userId: String): Result<String?> {
        return try {
            Logger.d(TAG, "获取头像临时URL: userId=$userId")

            val requestDto = GetAvatarUrlRequest(userId = userId)
            val request = RequestBuilder.createRequest(
                dtoName = "GetAvatarUrlRequestDto",
                data = requestDto,
                preferencesManager = preferencesManager
            )

            val response = userApi.getAvatarUrl(request)

            if (response.isSuccess()) {
                val avatarUrl = response.data?.avatarUrlResponseDto?.firstOrNull()?.avatarUrl
                Logger.d(TAG, "获取头像URL成功: $avatarUrl")
                Result.success(avatarUrl)
            } else {
                Logger.e(TAG, "获取头像URL失败: ${response.msg}")
                Result.failure(Exception(response.msg ?: "获取头像URL失败"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "获取头像URL异常: ${e.message}")
            Result.failure(e)
        }
    }

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
            
            // 构建请求体
            val requestDto = UserLoginRequest(
                phoneNumber = phoneNumber,
                password = encryptedPassword,
                deviceId = deviceId
            )
            
            // 构建完整请求
            val request = RequestBuilder.createRequest(
                dtoName = "UserLoginRequestDto",
                data = requestDto,
                preferencesManager = preferencesManager
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
     * 用户登出（仅清除本地数据）
     */
    fun logout() {
        preferencesManager.clearUserData()
    }

    /**
     * 调用服务端退出登录接口
     * 即使失败也会清除本地数据
     * @return Result<Boolean> 服务端是否成功
     */
    suspend fun logoutFromServer(): Result<Boolean> {
        return try {
            val userId = preferencesManager.getUserId() ?: return Result.success(true)

            Logger.d(TAG, "调用服务端退出登录接口: userId=$userId")

            val requestDto = UserLogoutRequest(userId = userId)
            val request = RequestBuilder.createRequest(
                dtoName = "UserLogoutRequestDto",
                data = requestDto,
                preferencesManager = preferencesManager
            )

            val response = userApi.logout(request)

            // 无论服务端是否成功，都清除本地数据
            preferencesManager.clearUserData()

            if (response.isSuccess()) {
                Logger.d(TAG, "服务端退出登录成功")
                Result.success(true)
            } else {
                Logger.w(TAG, "服务端退出登录失败: ${response.msg}")
                Result.success(true) // 即使服务端失败也返回成功，因为本地已清除
            }
        } catch (e: Exception) {
            Logger.e(TAG, "退出登录异常: ${e.message}")
            // 即使异常也清除本地数据
            preferencesManager.clearUserData()
            Result.success(true)
        }
    }

    /**
     * 注销账号
     * @param password 用户密码（明文）
     * @return Result<Boolean> 注销结果
     */
    suspend fun deactivateAccount(password: String): Result<Boolean> {
        return try {
            val userId = preferencesManager.getUserId()
                ?: return Result.failure(Exception("用户未登录"))

            Logger.d(TAG, "注销账号: userId=$userId")

            // MD5加密密码
            val encryptedPassword = password.md5()

            val requestDto = UserDeactivateRequest(
                userId = userId,
                password = encryptedPassword
            )
            val request = RequestBuilder.createRequest(
                dtoName = "UserDeactivateRequestDto",
                data = requestDto,
                preferencesManager = preferencesManager
            )

            val response = userApi.deactivate(request)

            if (response.isSuccess()) {
                Logger.d(TAG, "注销账号成功")
                // 清除所有本地数据
                preferencesManager.clearUserData()
                Result.success(true)
            } else {
                Logger.e(TAG, "注销账号失败: ${response.msg}")
                Result.failure(Exception(response.msg ?: "注销账号失败"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "注销账号异常: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 上传头像
     * @param imageData 图片数据
     * @param fileName 文件名
     * @return Result<String?> 新的头像URL
     */
    suspend fun uploadAvatar(imageData: ByteArray, fileName: String): Result<String?> {
        return try {
            val userId = preferencesManager.getUserId()
                ?: return Result.failure(Exception("用户未登录"))

            Logger.d(TAG, "上传头像: userId=$userId, fileName=$fileName, size=${imageData.size}")

            val requestBody = imageData.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", fileName, requestBody)
            val userIdBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = userApi.uploadAvatar(imagePart, userIdBody)

            if (response.isSuccess()) {
                val avatarUrl = response.data?.updateAvatarResponseDto?.firstOrNull()?.avatarUrl
                Logger.d(TAG, "上传头像成功: $avatarUrl")
                // 更新本地保存的头像URL
                avatarUrl?.let { preferencesManager.saveImageUrl(it) }
                Result.success(avatarUrl)
            } else {
                Logger.e(TAG, "上传头像失败: ${response.msg}")
                Result.failure(Exception(response.msg ?: "上传头像失败"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "上传头像异常: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 更新昵称
     * @param nickname 新昵称
     * @return Result<Boolean> 更新结果
     */
    suspend fun updateNickname(nickname: String): Result<Boolean> {
        return try {
            val userId = preferencesManager.getUserId()
                ?: return Result.failure(Exception("用户未登录"))

            Logger.d(TAG, "更新昵称: userId=$userId, nickname=$nickname")

            val requestDto = UpdateNicknameRequest(
                nickname = nickname
            )
            val request = RequestBuilder.createRequest(
                dtoName = "UpdateNicknameRequestDto",
                data = requestDto,
                preferencesManager = preferencesManager
            )

            val response = userApi.updateNickname(request)

            if (response.isSuccess()) {
                Logger.d(TAG, "更新昵称成功")
                // 更新本地保存的用户名
                preferencesManager.updateUserName(nickname)
                Result.success(true)
            } else {
                Logger.e(TAG, "更新昵称失败: ${response.msg}")
                Result.failure(Exception(response.msg ?: "更新昵称失败"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "更新昵称异常: ${e.message}")
            Result.failure(e)
        }
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
            // 构建请求体
            val requestDto = SendVerificationCodeRequest.forRegister(
                phoneNumber = phoneNumber,
                captureParam = captureParam
            )
            
            // 构建完整请求（未登录场景）
            val request = RequestBuilder.createRequest(
                dtoName = "SendVerificationCodeRequestDto",
                data = requestDto,
                token = "",
                userId = ""
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
            
            // 构建请求体（注册时不设置用户名和密码）
            val requestDto = UserRegisterRequest(
                phoneNumber = phoneNumber,
                verificationCode = verificationCode,
                deviceId = deviceId
            )
            
            // 构建完整请求（未登录场景）
            val request = RequestBuilder.createRequest(
                dtoName = "UserRegisterRequestDto",
                data = requestDto,
                token = "",
                userId = ""
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
            
            // 构建请求体
            val requestDto = SetPasswordAndNameRequest(
                userId = userId,
                newPassword = encryptedPassword,
                userName = nickname
            )
            
            // 构建完整请求（使用传入的token和userId）
            val request = RequestBuilder.createRequest(
                dtoName = "UserSetPasswordAndNameRequestDto",
                data = requestDto,
                token = token,
                userId = userId
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
    
    // ==================== 重置密码相关方法 ====================
    
    /**
     * 发送重置密码验证码
     * @param phoneNumber 手机号
     * @param captureParam 图形验证码参数（可选）
     * @return Result<SendVerificationCodeResponse> 发送结果
     */
    suspend fun sendResetPasswordVerificationCode(
        phoneNumber: String,
        captureParam: String = ""
    ): Result<SendVerificationCodeResponse> {
        return try {
            // 构建请求体
            val requestDto = SendVerificationCodeRequest.forResetPassword(
                phoneNumber = phoneNumber,
                captureParam = captureParam
            )
            
            // 构建完整请求（未登录场景）
            val request = RequestBuilder.createRequest(
                dtoName = "SendVerificationCodeRequestDto",
                data = requestDto,
                token = "",
                userId = ""
            )
            
            // 发送网络请求
            val response = userApi.sendResetVerificationCode(request)
            
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
     * 验证重置密码验证码
     * @param phoneNumber 手机号
     * @param verificationCode 验证码
     * @return Result<ResetPasswordResponse> 验证结果（包含token和userId）
     */
    suspend fun verifyResetCode(
        phoneNumber: String,
        verificationCode: String
    ): Result<ResetPasswordResponse> {
        return try {
            // 获取设备ID
            val deviceId = SecurityUtils.getDeviceId(context)
            
            // 构建请求体
            val requestDto = ResetPasswordRequest(
                phoneNumber = phoneNumber,
                verificationCode = verificationCode,
                deviceId = deviceId
            )
            
            // 构建完整请求（未登录场景）
            val request = RequestBuilder.createRequest(
                dtoName = "ResetPasswordRequestDto",
                data = requestDto,
                token = "",
                userId = ""
            )
            
            // 发送网络请求
            val response = userApi.verifyResetCode(request)
            
            // 检查响应
            if (response.isSuccess()) {
                val data = response.data?.resetPasswordResponseDto?.firstOrNull()
                if (data != null) {
                    if (data.isVerifySuccess) {
                        Result.success(data)
                    } else if (data.userNotExist) {
                        Result.failure(ResetPasswordException("该手机号未注册", data, isUserNotExist = true))
                    } else {
                        Result.failure(ResetPasswordException(data.message ?: "验证码错误", data))
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
     * 设置新密码（重置密码）
     * @param userId 用户ID（验证码验证成功后获取）
     * @param password 新密码（明文）
     * @param token 用户Token（验证码验证成功后获取）
     * @return Result<Boolean> 设置结果
     */
    suspend fun setNewPassword(
        userId: String,
        password: String,
        token: String
    ): Result<Boolean> {
        return try {
            // MD5加密密码
            val encryptedPassword = password.md5()
            
            // 构建请求体
            val requestDto = SetPasswordRequest(
                userId = userId,
                newPassword = encryptedPassword
            )
            
            // 构建完整请求（使用传入的token和userId）
            val request = RequestBuilder.createRequest(
                dtoName = "UserSetPswRequestDto",
                data = requestDto,
                token = token,
                userId = userId
            )
            
            // 发送网络请求
            val response = userApi.setPassword(request)
            
            // 检查响应
            if (response.isSuccess()) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
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

/**
 * 重置密码异常
 * 用于携带详细的重置密码失败信息
 */
class ResetPasswordException(
    message: String,
    val response: ResetPasswordResponse? = null,
    val isUserNotExist: Boolean = false
) : Exception(message)

