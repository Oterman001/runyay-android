package com.oterman.rundemo.presentation.feature.auth.forgotpassword

/**
 * 重置密码步骤枚举
 * 对应iOS的ResetStep
 */
enum class ResetStep {
    PHONE_NUMBER,   // 手机号输入
    VERIFICATION,   // 验证码验证
    NEW_PASSWORD    // 新密码设置
}

/**
 * 忘记密码界面UI状态
 * 对应iOS的ForgotPasswordViewModel中的状态
 */
data class ForgotPasswordUiState(
    // 当前步骤
    val currentStep: ResetStep = ResetStep.PHONE_NUMBER,
    
    // 手机号
    val phoneNumber: String = "",
    val phoneNumberError: String? = null,
    
    // 验证码
    val verificationCode: String = "",
    val verificationCodeError: String? = null,
    
    // 新密码
    val newPassword: String = "",
    val newPasswordError: String? = null,
    val isNewPasswordVisible: Boolean = false,
    
    // 确认新密码
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val isConfirmPasswordVisible: Boolean = false,
    
    // 加载状态
    val isLoading: Boolean = false,
    
    // 验证码倒计时
    val resendCountdown: Int = 0,
    
    // 重置成功标志
    val resetSuccess: Boolean = false,
    
    // 错误信息
    val errorMessage: String? = null,
    
    // 是否需要显示用户不存在弹窗
    val showUserNotExistAlert: Boolean = false,
    
    // 验证成功后获取的token和userId（用于设置新密码）
    val resetToken: String? = null,
    val resetUserId: String? = null
) {
    /**
     * 是否可以发送验证码
     */
    val canSendCode: Boolean
        get() = phoneNumber.isNotEmpty() &&
                phoneNumberError == null &&
                resendCountdown == 0 &&
                !isLoading
    
    /**
     * 是否可以验证验证码
     */
    val canVerifyCode: Boolean
        get() = verificationCode.length == 6 &&
                verificationCodeError == null &&
                !isLoading
    
    /**
     * 是否可以重置密码
     */
    val canResetPassword: Boolean
        get() = newPassword.isNotEmpty() &&
                confirmPassword.isNotEmpty() &&
                newPassword.length >= 6 &&
                newPassword == confirmPassword &&
                newPasswordError == null &&
                confirmPasswordError == null &&
                !isLoading
    
    /**
     * 是否可以返回上一步
     */
    val canGoBack: Boolean
        get() = currentStep != ResetStep.PHONE_NUMBER
}

