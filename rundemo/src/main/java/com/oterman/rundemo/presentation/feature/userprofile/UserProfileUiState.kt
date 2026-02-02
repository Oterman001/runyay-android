package com.oterman.rundemo.presentation.feature.userprofile

/**
 * 用户信息页面UI状态
 * 对应iOS的UserProfileView状态
 */
data class UserProfileUiState(
    // 用户信息
    val userId: String = "",
    val userName: String = "",
    val phoneNumber: String = "",
    val avatarUrl: String? = null,

    // 加载状态
    val isLoadingAvatar: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val isUpdatingNickname: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isDeactivating: Boolean = false,

    // 对话框状态
    val showAvatarPickerDialog: Boolean = false,
    val showNicknameEditor: Boolean = false,
    val showLogoutConfirmDialog: Boolean = false,
    val showDeactivateConfirmDialog: Boolean = false,
    val showPasswordConfirmDialog: Boolean = false,

    // 昵称编辑
    val editingNickname: String = "",
    val nicknameError: String? = null,

    // 密码确认
    val confirmPassword: String = "",
    val passwordError: String? = null,

    // 导航事件
    val navigateToLogin: Boolean = false,

    // 错误/成功消息
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    /**
     * 是否可以保存昵称
     */
    val canSaveNickname: Boolean
        get() = editingNickname.isNotEmpty() &&
                nicknameError == null &&
                !isUpdatingNickname

    /**
     * 是否可以确认密码
     */
    val canConfirmPassword: Boolean
        get() = confirmPassword.isNotEmpty() &&
                passwordError == null &&
                !isDeactivating

    /**
     * 手机号脱敏显示
     * e.g., "13812345678" -> "138****5678"
     */
    val maskedPhoneNumber: String
        get() = if (phoneNumber.length >= 11) {
            "${phoneNumber.substring(0, 3)}****${phoneNumber.substring(7)}"
        } else {
            phoneNumber
        }
}
