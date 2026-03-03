package com.oterman.rundemo.presentation.feature.home

/**
 * Home screen tab definition
 * Corresponds to iOS MainTabView tabs
 */
enum class HomeTab(val index: Int) {
    DASHBOARD(0),
    DATA(1),
    PROFILE(2)
}

/**
 * FIT文件导入结果
 */
sealed class FitImportResult {
    data class Success(val distance: Double, val duration: Double) : FitImportResult()
    object AlreadyExists : FitImportResult()
    data class Error(val message: String) : FitImportResult()
}

/**
 * Home screen UI state
 * Manages bottom navigation and profile tab state
 */
data class HomeUiState(
    // Navigation state
    val selectedTab: HomeTab = HomeTab.DASHBOARD,

    // Auth state (from PreferencesManager)
    val isLoggedIn: Boolean = false,
    val userName: String? = null,
    val phoneNumber: String? = null,
    val avatarUrl: String? = null,
    val isLoadingAvatar: Boolean = false,

    // Profile tab actions state
    val isLoggingOut: Boolean = false,
    val showLogoutConfirmDialog: Boolean = false,

    // Navigation events
    val navigateToLogin: Boolean = false,
    val navigateToWelcome: Boolean = false,
    
    // FIT文件导入状态
    val isImportingFit: Boolean = false,
    val showImportResultDialog: Boolean = false,
    val fitImportResult: FitImportResult? = null,

    // 数据同步状态
    val isSyncing: Boolean = false,
    val showSyncIcon: Boolean = false,
    val syncSuccessMessage: String? = null,

    // 通知权限引导
    val needsNotificationPermission: Boolean = false
)
