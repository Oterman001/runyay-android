package com.oterman.rundemo.presentation.feature.auth.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.repository.LoginException
import com.oterman.rundemo.data.repository.UserRepository
import com.oterman.rundemo.util.RLog
import com.oterman.rundemo.util.ValidationUtils
import com.umeng.analytics.MobclickAgent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 登录ViewModel
 * 对应iOS的PhoneLoginViewModel
 */
class LoginViewModel(
    private val context: Context,
    private val userRepository: UserRepository = UserRepository(context)
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    // UI状态
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
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
     * 密码输入变化
     */
    fun onPasswordChange(password: String) {
        _uiState.update { state ->
            state.copy(
                password = password,
                passwordError = ValidationUtils.getPasswordError(password)
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
     * 切换协议勾选状态
     */
    fun toggleTermsAgreement() {
        _uiState.update { state ->
            state.copy(hasAgreedToTerms = !state.hasAgreedToTerms)
        }
    }
    
    /**
     * 检查协议并登录
     */
    fun checkTermsAgreementAndLogin() {
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
        
        // 已同意协议，执行登录
        login()
    }
    
    /**
     * 执行登录
     */
    private fun login() {
        val state = _uiState.value

        // 验证输入
        if (!state.canLogin) {
            RLog.w(TAG, "登录验证失败：手机号或密码格式不正确")
            return
        }

        RLog.d(TAG, "开始登录：手机号=${state.phoneNumber}")

        viewModelScope.launch {
            // 设置加载状态
            _uiState.update { it.copy(isLoading = true, errorMessage = null, remainingAttempts = null) }
            MobclickAgent.onEvent(context, "login_attempt")

            try {
                // 调用登录接口
                RLog.i(TAG, "调用UserRepository.login")
                val result = userRepository.login(state.phoneNumber, state.password)

                result.onSuccess { response ->
                    // 登录成功
                    RLog.i(TAG, "登录成功：userId=${response.userId}")
                    MobclickAgent.onEvent(context, "login_success")
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            loginSuccess = true
                        )
                    }
                }.onFailure { error ->
                    // 登录失败
                    RLog.e(TAG, "登录失败：${error.message}", error)
                    MobclickAgent.onEvent(context, "login_failure")
                    handleLoginError(error)
                }
                
            } catch (e: Exception) {
                // 异常处理
                RLog.e(TAG, "登录异常", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = "登录失败：${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 处理登录错误
     */
    private fun handleLoginError(error: Throwable) {
        when (error) {
            is LoginException -> {
                val response = error.response
                val attempts = response?.remainingAttempts
                
                val errorMsg = when {
                    attempts != null && attempts > 0 -> {
                        "手机号或密码错误"
                    }
                    attempts == 0 -> {
                        "密码错误次数过多，账户已被锁定24小时"
                    }
                    response?.userExists == false -> {
                        "该手机号未注册，请先注册"
                    }
                    else -> {
                        error.message ?: "登录失败，请稍后重试"
                    }
                }
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = errorMsg,
                        remainingAttempts = attempts,
                        showForgotPasswordAlert = attempts == 0
                    )
                }
            }
            else -> {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = "网络请求失败，请稍后重试"
                    )
                }
            }
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { state ->
            state.copy(
                errorMessage = null,
                remainingAttempts = null,
                showForgotPasswordAlert = false
            )
        }
    }
    
    /**
     * 重置登录成功状态
     */
    fun resetLoginSuccess() {
        _uiState.update { state ->
            state.copy(loginSuccess = false)
        }
    }

    /**
     * 填充 Debug 预置账号（仅 Debug 构建使用）
     */
    fun fillDebugAccount(phone: String, password: String) {
        _uiState.update { state ->
            state.copy(
                phoneNumber = phone,
                password = password,
                phoneNumberError = null,
                passwordError = null,
                hasAgreedToTerms = true
            )
        }
    }
}

