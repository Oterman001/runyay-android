package com.oterman.rundemo.presentation.feature.home

/**
 * Home screen tab definition
 * Corresponds to iOS MainTabView tabs
 */
enum class HomeTab(val index: Int) {
    HOME(0),
    DATA(1),
    PROFILE(2)
}

/**
 * Home screen UI state
 * Manages bottom navigation and profile tab state
 */
data class HomeUiState(
    // Navigation state
    val selectedTab: HomeTab = HomeTab.HOME,

    // Auth state (from PreferencesManager)
    val isLoggedIn: Boolean = false,
    val userName: String? = null,

    // Profile tab actions state
    val isLoggingOut: Boolean = false,
    val showLogoutConfirmDialog: Boolean = false,

    // Navigation events
    val navigateToLogin: Boolean = false,
    val navigateToWelcome: Boolean = false
)
