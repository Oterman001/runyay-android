package com.oterman.rundemo.presentation.feature.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.fit.FitImportService
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.repository.UserRepository
import com.oterman.rundemo.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * HomeScreen ViewModel
 * Manages tab navigation and profile/auth state
 * Corresponds to iOS NavigationCoordinator functionality
 */
class HomeViewModel(
    private val context: Context,
    private val preferencesManager: PreferencesManager = PreferencesManager(context),
    private val fitImportService: FitImportService = FitImportService(context),
    private val userRepository: UserRepository = UserRepository(context)
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAuthState()
    }

    /**
     * Load authentication state from PreferencesManager
     */
    private fun loadAuthState() {
        val isLoggedIn = preferencesManager.isUserLoggedIn()
        val userName = preferencesManager.getUserName()
        val phoneNumber = preferencesManager.getPhoneNumber()
        val userId = preferencesManager.getUserId()

        Logger.d(TAG, "Auth state loaded: isLoggedIn=$isLoggedIn, userName=$userName, userId=$userId")

        _uiState.update { state ->
            state.copy(
                isLoggedIn = isLoggedIn,
                userName = userName,
                phoneNumber = phoneNumber
            )
        }

        // 如果已登录，异步加载头像
        if (isLoggedIn && userId != null) {
            loadAvatarUrl(userId)
        }
    }

    /**
     * 通过 API 获取头像临时访问 URL
     * OSS存储的头像需要带签名的临时URL才能访问
     */
    private fun loadAvatarUrl(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAvatar = true) }

            Logger.d(TAG, "开始加载头像URL: userId=$userId")

            val result = userRepository.getAvatarUrl(userId)

            result.onSuccess { url ->
                Logger.d(TAG, "头像URL加载成功: $url")
                _uiState.update { it.copy(avatarUrl = url, isLoadingAvatar = false) }
            }.onFailure { e ->
                Logger.e(TAG, "头像URL加载失败: ${e.message}")
                _uiState.update { it.copy(avatarUrl = null, isLoadingAvatar = false) }
            }
        }
    }

    /**
     * Refresh auth state (call when returning from login)
     */
    fun refreshAuthState() {
        loadAuthState()
    }

    /**
     * Switch selected tab
     */
    fun selectTab(tab: HomeTab) {
        Logger.d(TAG, "Tab selected: $tab")
        _uiState.update { state ->
            state.copy(selectedTab = tab)
        }
    }

    /**
     * Show logout confirmation dialog
     */
    fun showLogoutConfirmation() {
        _uiState.update { state ->
            state.copy(showLogoutConfirmDialog = true)
        }
    }

    /**
     * Dismiss logout confirmation dialog
     */
    fun dismissLogoutConfirmation() {
        _uiState.update { state ->
            state.copy(showLogoutConfirmDialog = false)
        }
    }

    /**
     * Perform logout
     * Corresponds to iOS coordinator.logout()
     */
    fun logout() {
        viewModelScope.launch {
            Logger.i(TAG, "User logging out")
            _uiState.update { it.copy(isLoggingOut = true, showLogoutConfirmDialog = false) }

            // Clear user data
            preferencesManager.clearUserData()

            _uiState.update { state ->
                state.copy(
                    isLoggingOut = false,
                    isLoggedIn = false,
                    userName = null,
                    phoneNumber = null,
                    avatarUrl = null,
                    isLoadingAvatar = false
                )
            }

            Logger.i(TAG, "Logout completed")
        }
    }

    /**
     * Trigger navigation to login page
     */
    fun navigateToLogin() {
        Logger.d(TAG, "Navigate to login requested")
        _uiState.update { state ->
            state.copy(navigateToLogin = true)
        }
    }

    /**
     * Reset login navigation event
     */
    fun resetNavigateToLogin() {
        _uiState.update { state ->
            state.copy(navigateToLogin = false)
        }
    }

    /**
     * Trigger navigation to welcome page
     * Corresponds to iOS coordinator.showWelcomePage()
     */
    fun navigateToWelcome() {
        Logger.d(TAG, "Navigate to welcome requested")
        _uiState.update { state ->
            state.copy(navigateToWelcome = true)
        }
    }

    /**
     * Reset welcome navigation event
     */
    fun resetNavigateToWelcome() {
        _uiState.update { state ->
            state.copy(navigateToWelcome = false)
        }
    }

    /**
     * Reset first launch state (for debugging)
     * Corresponds to iOS coordinator.resetFirstLaunch()
     */
    fun resetFirstLaunch() {
        Logger.d(TAG, "Reset first launch state")
        navigateToWelcome()
    }
    
    // ==================== FIT文件导入 ====================
    
    /**
     * 导入FIT文件
     * @param uri 选择的FIT文件Uri
     */
    fun importFitFile(uri: Uri) {
        Logger.i(TAG, "开始导入FIT文件: $uri")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingFit = true) }
            
            val result = fitImportService.importFitFile(uri)
            
            _uiState.update { state ->
                state.copy(
                    isImportingFit = false,
                    showImportResultDialog = true,
                    fitImportResult = result
                )
            }
            
            when (result) {
                is FitImportResult.Success -> {
                    Logger.i(TAG, "导入成功: ${result.distance}km, ${result.duration}min")
                }
                is FitImportResult.AlreadyExists -> {
                    Logger.w(TAG, "文件已导入过")
                }
                is FitImportResult.Error -> {
                    Logger.e(TAG, "导入失败: ${result.message}")
                }
            }
        }
    }
    
    /**
     * 关闭导入结果对话框
     */
    fun dismissImportResultDialog() {
        _uiState.update { state ->
            state.copy(
                showImportResultDialog = false,
                fitImportResult = null
            )
        }
    }
}

/**
 * HomeViewModel factory
 */
class HomeViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
