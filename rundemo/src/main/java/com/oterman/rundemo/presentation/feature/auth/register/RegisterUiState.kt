package com.oterman.rundemo.presentation.feature.auth.register

/**
 * 注册步骤枚举
 * 对应iOS的RegistrationStep
 */
enum class RegistrationStep {
    PHONE_NUMBER,   // 手机号输入
    VERIFICATION,   // 验证码验证
    PASSWORD        // 密码设置
}

/**
 * 注册界面UI状态
 * 对应iOS的PhoneRegisterViewModel中的状态
 */
data class RegisterUiState(
    // 当前步骤
    val currentStep: RegistrationStep = RegistrationStep.PHONE_NUMBER,
    
    // 手机号
    val phoneNumber: String = "",
    val phoneNumberError: String? = null,
    
    // 验证码
    val smsCode: String = "",
    val smsCodeError: String? = null,
    
    // 昵称
    val nickname: String = "",
    val nicknameError: String? = null,
    
    // 密码
    val password: String = "",
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    
    // 确认密码
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val isConfirmPasswordVisible: Boolean = false,
    
    // 协议同意状态
    val hasAgreedToTerms: Boolean = false,
    val shouldShake: Boolean = false,
    
    // 加载状态
    val isLoading: Boolean = false,
    
    // 验证码倒计时
    val resendCountdown: Int = 0,
    
    // 注册成功标志
    val registerSuccess: Boolean = false,
    
    // 错误信息
    val errorMessage: String? = null,
    
    // 是否需要跳转到登录页（用户已存在）
    val shouldNavigateToLogin: Boolean = false,
    
    // 注册后保存的用户信息
    val registeredUserId: String? = null,
    val registeredToken: String? = null,

    // 图形验证码
    val showCaptcha: Boolean = false,
    val captchaSceneCode: String? = null
) {
    /**
     * 是否可以发送验证码
     */
    val canSendSms: Boolean
        get() = phoneNumber.isNotEmpty() &&
                phoneNumberError == null &&
                hasAgreedToTerms &&
                !isLoading
    
    /**
     * 是否可以验证验证码
     */
    val canVerifyCode: Boolean
        get() = smsCode.isNotEmpty() &&
                smsCodeError == null &&
                !isLoading
    
    /**
     * 是否可以完成注册
     */
    val canRegister: Boolean
        get() = nickname.isNotEmpty() &&
                password.isNotEmpty() &&
                confirmPassword.isNotEmpty() &&
                nicknameError == null &&
                passwordError == null &&
                confirmPasswordError == null &&
                password == confirmPassword &&
                !isLoading
    
    /**
     * 是否可以返回上一步
     */
    val canGoBack: Boolean
        get() = currentStep != RegistrationStep.PHONE_NUMBER
}

