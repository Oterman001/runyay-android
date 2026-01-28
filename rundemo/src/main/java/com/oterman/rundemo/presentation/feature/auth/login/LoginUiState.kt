package com.oterman.rundemo.presentation.feature.auth.login

/**
 * 登录界面UI状态
 * 对应iOS的PhoneLoginViewModel中的Published属性
 */
data class LoginUiState(
    // 输入字段
    val phoneNumber: String = "",
    val password: String = "",
    
    // UI控制
    val isPasswordVisible: Boolean = false,
    val hasAgreedToTerms: Boolean = false,
    val isLoading: Boolean = false,
    val shouldShake: Boolean = false,
    
    // 验证错误
    val phoneNumberError: String? = null,
    val passwordError: String? = null,
    
    // 业务状态
    val errorMessage: String? = null,
    val remainingAttempts: Int? = null,
    val showForgotPasswordAlert: Boolean = false,
    val loginSuccess: Boolean = false
) {
    /**
     * 是否可以登录
     */
    val canLogin: Boolean
        get() = phoneNumber.isNotEmpty() &&
                password.isNotEmpty() &&
                phoneNumberError == null &&
                passwordError == null &&
                hasAgreedToTerms &&
                !isLoading
    
    /**
     * 是否显示剩余尝试次数警告
     */
    val shouldShowAttemptsWarning: Boolean
        get() = remainingAttempts != null && remainingAttempts <= 3 && remainingAttempts > 0
}

