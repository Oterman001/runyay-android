package com.oterman.rundemo.presentation.feature.auth.forgotpassword

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.repository.ResetPasswordException
import com.oterman.rundemo.data.repository.UserRepository
import com.oterman.rundemo.util.RLog
import com.oterman.rundemo.util.ValidationUtils
import com.umeng.analytics.MobclickAgent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 忘记密码ViewModel
 * 对应iOS的ForgotPasswordViewModel
 */
class ForgotPasswordViewModel(
    private val context: Context,
    private val userRepository: UserRepository = UserRepository(context),
    private val preferencesManager: PreferencesManager = PreferencesManager(context)
) : ViewModel() {

    companion object {
        private const val TAG = "ForgotPasswordViewModel"
        private const val COUNTDOWN_SECONDS = 60
    }

    // UI状态
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    // 倒计时Job
    private var countdownJob: Job? = null

    // 图形验证码参数，验证成功后携带此参数重新发送短信
    private var captchaParam: String = ""

    // ==================== 手机号输入处理 ====================
    
    /**
     * 手机号输入变化
     */
    fun onPhoneNumberChange(phoneNumber: String) {
        _uiState.update { state ->
            state.copy(
                phoneNumber = phoneNumber,
                phoneNumberError = ValidationUtils.getPhoneNumberError(phoneNumber)
            )
        }
    }
    
    /**
     * 请求发送验证码
     */
    fun requestVerificationCode() {
        val state = _uiState.value
        
        if (!state.canSendCode) {
            RLog.w(TAG, "发送验证码条件不满足")
            return
        }
        
        RLog.d(TAG, "请求发送重置密码验证码: ${state.phoneNumber}")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            MobclickAgent.onEvent(context, "forgot_password_start")

            val result = userRepository.sendResetPasswordVerificationCode(state.phoneNumber, captchaParam)
            
            result.onSuccess { response ->
                RLog.i(TAG, "验证码发送响应: sendFlag=${response.sendFlag}")
                captchaParam = ""

                when {
                    response.isSuccess -> {
                        RLog.i(TAG, "验证码发送成功")
                        _uiState.update { it.copy(
                            isLoading = false,
                            currentStep = ResetStep.VERIFICATION
                        )}
                        startResendCountdown()
                    }
                    response.userNotExist -> {
                        RLog.w(TAG, "用户不存在")
                        _uiState.update { it.copy(
                            isLoading = false,
                            showUserNotExistAlert = true
                        )}
                    }
                    response.needCaptcha -> {
                        RLog.w(TAG, "需要图形验证码")
                        _uiState.update { it.copy(
                            isLoading = false,
                            showCaptcha = true,
                            captchaSceneCode = response.captureSceneCode
                        )}
                    }
                    else -> {
                        RLog.e(TAG, "验证码发送失败")
                        _uiState.update { it.copy(
                            isLoading = false,
                            errorMessage = "验证码发送失败，请稍后重试"
                        )}
                    }
                }
            }.onFailure { error ->
                RLog.e(TAG, "发送验证码失败: ${error.message}", error)
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "验证码发送失败：${error.message}"
                )}
            }
        }
    }
    
    // ==================== 图形验证码处理 ====================

    /**
     * 图形验证码验证成功，携带参数重新发送短信
     */
    fun handleCaptchaSuccess(param: String) {
        captchaParam = param
        _uiState.update { it.copy(showCaptcha = false) }
        requestVerificationCode()
    }

    /**
     * 图形验证码验证失败
     */
    fun handleCaptchaFailure(error: String) {
        _uiState.update { it.copy(showCaptcha = false, errorMessage = error) }
    }

    /**
     * 用户取消图形验证码
     */
    fun handleCaptchaCancel() {
        _uiState.update { it.copy(showCaptcha = false) }
    }

    // ==================== 验证码验证处理 ====================
    
    /**
     * 验证码输入变化
     */
    fun onVerificationCodeChange(code: String) {
        // 限制只能输入数字且最多6位
        val filteredCode = code.filter { it.isDigit() }.take(6)
        _uiState.update { state ->
            state.copy(
                verificationCode = filteredCode,
                verificationCodeError = ValidationUtils.getSmsCodeError(filteredCode)
            )
        }
    }
    
    /**
     * 验证验证码
     */
    fun verifyCode() {
        val state = _uiState.value
        
        if (!state.canVerifyCode) {
            RLog.w(TAG, "验证码验证条件不满足")
            return
        }
        
        RLog.d(TAG, "开始验证重置密码验证码")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, verificationCodeError = null) }
            
            val result = userRepository.verifyResetCode(
                phoneNumber = state.phoneNumber,
                verificationCode = state.verificationCode
            )
            
            result.onSuccess { response ->
                RLog.i(TAG, "验证码验证成功: userId=${response.userId}")
                
                // resetPassword 会使旧 Token 在服务端作废并返回新 Token；
                // 立即写入 PreferencesManager，确保 AuthInterceptor 对后续 setPassword 使用新 Token
                preferencesManager.saveUserToken(response.token ?: "")
                preferencesManager.saveUserId(response.userId ?: "")
                
                _uiState.update { it.copy(
                    isLoading = false,
                    currentStep = ResetStep.NEW_PASSWORD,
                    resetToken = response.token,
                    resetUserId = response.userId
                )}
            }.onFailure { error ->
                RLog.e(TAG, "验证码验证失败: ${error.message}", error)
                
                when (error) {
                    is ResetPasswordException -> {
                        if (error.isUserNotExist) {
                            _uiState.update { it.copy(
                                isLoading = false,
                                showUserNotExistAlert = true
                            )}
                        } else {
                            _uiState.update { it.copy(
                                isLoading = false,
                                verificationCodeError = error.message
                            )}
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            verificationCodeError = "验证失败：${error.message}"
                        )}
                    }
                }
            }
        }
    }
    
    // ==================== 新密码设置处理 ====================
    
    /**
     * 新密码输入变化
     */
    fun onNewPasswordChange(password: String) {
        _uiState.update { state ->
            state.copy(
                newPassword = password,
                newPasswordError = ValidationUtils.getPasswordError(password),
                confirmPasswordError = if (state.confirmPassword.isNotEmpty()) {
                    ValidationUtils.getConfirmPasswordError(password, state.confirmPassword)
                } else null
            )
        }
    }
    
    /**
     * 确认密码输入变化
     */
    fun onConfirmPasswordChange(password: String) {
        _uiState.update { state ->
            state.copy(
                confirmPassword = password,
                confirmPasswordError = ValidationUtils.getConfirmPasswordError(state.newPassword, password)
            )
        }
    }
    
    /**
     * 切换新密码可见性
     */
    fun toggleNewPasswordVisibility() {
        _uiState.update { state ->
            state.copy(isNewPasswordVisible = !state.isNewPasswordVisible)
        }
    }
    
    /**
     * 切换确认密码可见性
     */
    fun toggleConfirmPasswordVisibility() {
        _uiState.update { state ->
            state.copy(isConfirmPasswordVisible = !state.isConfirmPasswordVisible)
        }
    }
    
    /**
     * 重置密码
     */
    fun resetPassword() {
        val state = _uiState.value
        
        if (!state.canResetPassword) {
            RLog.w(TAG, "重置密码条件不满足")
            return
        }
        
        val userId = state.resetUserId
        val token = state.resetToken
        
        if (userId.isNullOrEmpty() || token.isNullOrEmpty()) {
            RLog.e(TAG, "用户ID或Token为空")
            _uiState.update { it.copy(errorMessage = "验证信息失效，请重新验证") }
            return
        }
        
        RLog.d(TAG, "开始重置密码")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = userRepository.setNewPassword(
                userId = userId,
                password = state.newPassword,
                token = token
            )
            
            result.onSuccess {
                RLog.i(TAG, "密码重置成功")
                MobclickAgent.onEvent(context, "forgot_password_complete")
                _uiState.update { it.copy(
                    isLoading = false,
                    resetSuccess = true
                )}
            }.onFailure { error ->
                RLog.e(TAG, "密码重置失败: ${error.message}", error)
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "密码重置失败：${error.message}"
                )}
            }
        }
    }
    
    // ==================== 步骤导航 ====================
    
    /**
     * 返回上一步
     */
    fun goToPreviousStep() {
        val state = _uiState.value
        if (!state.canGoBack) return
        
        val previousStep = when (state.currentStep) {
            ResetStep.VERIFICATION -> ResetStep.PHONE_NUMBER
            ResetStep.NEW_PASSWORD -> ResetStep.VERIFICATION
            ResetStep.PHONE_NUMBER -> ResetStep.PHONE_NUMBER
        }
        
        _uiState.update { it.copy(currentStep = previousStep, errorMessage = null) }
    }
    
    // ==================== 倒计时处理 ====================
    
    /**
     * 开始重发验证码倒计时
     */
    private fun startResendCountdown() {
        countdownJob?.cancel()
        
        _uiState.update { it.copy(resendCountdown = COUNTDOWN_SECONDS) }
        
        countdownJob = viewModelScope.launch {
            var remaining = COUNTDOWN_SECONDS
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.update { it.copy(resendCountdown = remaining) }
            }
        }
    }
    
    // ==================== 错误处理 ====================
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(
            errorMessage = null,
            showUserNotExistAlert = false
        )}
    }
    
    /**
     * 关闭用户不存在弹窗
     */
    fun dismissUserNotExistAlert() {
        _uiState.update { it.copy(showUserNotExistAlert = false) }
    }
    
    /**
     * 重置成功状态
     */
    fun resetSuccessState() {
        _uiState.update { it.copy(resetSuccess = false) }
    }
    
    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}

