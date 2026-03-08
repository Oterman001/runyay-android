package com.oterman.rundemo.presentation.feature.auth.register

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.repository.RegisterException
import com.oterman.rundemo.data.repository.UserRepository
import com.oterman.rundemo.util.RLog
import com.oterman.rundemo.util.ValidationUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 注册ViewModel
 * 对应iOS的PhoneRegisterViewModel
 */
class RegisterViewModel(
    private val context: Context,
    private val userRepository: UserRepository = UserRepository(context)
) : ViewModel() {

    companion object {
        private const val TAG = "RegisterViewModel"
        private const val COUNTDOWN_SECONDS = 60
    }

    // UI状态
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

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
     * 切换协议勾选状态
     */
    fun toggleTermsAgreement() {
        _uiState.update { state ->
            state.copy(hasAgreedToTerms = !state.hasAgreedToTerms)
        }
    }
    
    /**
     * 检查协议并发送验证码
     */
    fun checkTermsAgreementAndRequestCode() {
        val state = _uiState.value
        
        if (!state.hasAgreedToTerms) {
            // 触发抖动效果
            viewModelScope.launch {
                _uiState.update { it.copy(shouldShake = true) }
                delay(500)
                _uiState.update { it.copy(shouldShake = false) }
            }
            return
        }
        
        // 已同意协议，发送验证码
        requestVerificationCode()
    }
    
    /**
     * 请求发送验证码
     */
    fun requestVerificationCode() {
        val state = _uiState.value
        
        if (!state.canSendSms) {
            RLog.w(TAG, "发送短信条件不满足")
            return
        }
        
        RLog.d(TAG, "请求发送短信验证码: ${state.phoneNumber}")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = userRepository.sendRegisterVerificationCode(state.phoneNumber, captchaParam)
            
            result.onSuccess { response ->
                RLog.i(TAG, "验证码发送响应: sendFlag=${response.sendFlag}")
                captchaParam = ""

                when {
                    response.isSuccess -> {
                        RLog.i(TAG, "验证码发送成功")
                        _uiState.update { it.copy(
                            isLoading = false,
                            currentStep = RegistrationStep.VERIFICATION
                        )}
                        startResendCountdown()
                    }
                    response.userExists -> {
                        RLog.w(TAG, "用户已存在")
                        _uiState.update { it.copy(
                            isLoading = false,
                            errorMessage = "该手机号已注册，请前往登录",
                            shouldNavigateToLogin = true
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
    fun onSmsCodeChange(smsCode: String) {
        // 限制只能输入数字且最多6位
        val filteredCode = smsCode.filter { it.isDigit() }.take(6)
        _uiState.update { state ->
            state.copy(
                smsCode = filteredCode,
                smsCodeError = ValidationUtils.getSmsCodeError(filteredCode)
            )
        }
    }
    
    /**
     * 验证短信验证码
     */
    fun verifyCodeAndRegister() {
        val state = _uiState.value
        
        if (!state.canVerifyCode) {
            RLog.w(TAG, "验证码验证条件不满足")
            return
        }
        
        RLog.d(TAG, "开始验证短信验证码")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = userRepository.registerWithSmsCode(
                phoneNumber = state.phoneNumber,
                verificationCode = state.smsCode
            )
            
            result.onSuccess { response ->
                RLog.i(TAG, "注册验证成功: userId=${response.userId}")
                
                if (response.needSetPassword) {
                    RLog.i(TAG, "服务端要求设置密码，进入密码设置步骤")
                    _uiState.update { it.copy(
                        isLoading = false,
                        currentStep = RegistrationStep.PASSWORD,
                        registeredUserId = response.userId,
                        registeredToken = response.token
                    )}
                } else {
                    RLog.i(TAG, "注册完成，无需设置密码")
                    _uiState.update { it.copy(
                        isLoading = false,
                        registerSuccess = true
                    )}
                }
            }.onFailure { error ->
                RLog.e(TAG, "注册失败: ${error.message}", error)
                
                when (error) {
                    is RegisterException -> {
                        if (error.isUserExists) {
                            _uiState.update { it.copy(
                                isLoading = false,
                                errorMessage = "该手机号已注册，请前往登录",
                                shouldNavigateToLogin = true
                            )}
                        } else {
                            _uiState.update { it.copy(
                                isLoading = false,
                                errorMessage = error.message
                            )}
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            errorMessage = "注册失败：${error.message}"
                        )}
                    }
                }
            }
        }
    }
    
    // ==================== 密码设置处理 ====================
    
    /**
     * 昵称输入变化
     */
    fun onNicknameChange(nickname: String) {
        _uiState.update { state ->
            state.copy(
                nickname = nickname,
                nicknameError = ValidationUtils.getNicknameError(nickname)
            )
        }
    }
    
    /**
     * 密码输入变化
     */
    fun onPasswordChange(password: String) {
        _uiState.update { state ->
            state.copy(
                password = password,
                passwordError = ValidationUtils.getPasswordError(password),
                confirmPasswordError = if (state.confirmPassword.isNotEmpty()) {
                    ValidationUtils.getConfirmPasswordError(password, state.confirmPassword)
                } else null
            )
        }
    }
    
    /**
     * 确认密码输入变化
     */
    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { state ->
            state.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = ValidationUtils.getConfirmPasswordError(state.password, confirmPassword)
            )
        }
    }
    
    /**
     * 切换密码可见性
     */
    fun togglePasswordVisibility() {
        _uiState.update { state ->
            state.copy(isPasswordVisible = !state.isPasswordVisible)
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
     * 完成注册（设置密码和昵称）
     */
    fun completeRegistration() {
        val state = _uiState.value
        
        if (!state.canRegister) {
            RLog.w(TAG, "注册条件不满足")
            return
        }
        
        val userId = state.registeredUserId
        val token = state.registeredToken
        
        if (userId.isNullOrEmpty() || token.isNullOrEmpty()) {
            RLog.e(TAG, "用户ID或Token为空")
            _uiState.update { it.copy(errorMessage = "注册信息丢失，请重新注册") }
            return
        }
        
        RLog.d(TAG, "开始设置密码和昵称")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = userRepository.setPasswordAndName(
                userId = userId,
                password = state.password,
                nickname = state.nickname,
                token = token
            )
            
            result.onSuccess { success ->
                RLog.i(TAG, "密码设置成功")
                _uiState.update { it.copy(
                    isLoading = false,
                    registerSuccess = true
                )}
            }.onFailure { error ->
                RLog.e(TAG, "密码设置失败: ${error.message}", error)
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "设置失败：${error.message}"
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
            RegistrationStep.VERIFICATION -> RegistrationStep.PHONE_NUMBER
            RegistrationStep.PASSWORD -> RegistrationStep.VERIFICATION
            RegistrationStep.PHONE_NUMBER -> RegistrationStep.PHONE_NUMBER
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
            shouldNavigateToLogin = false
        )}
    }
    
    /**
     * 重置注册成功状态
     */
    fun resetRegisterSuccess() {
        _uiState.update { it.copy(registerSuccess = false) }
    }
    
    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}

