package com.oterman.rundemo.presentation.feature.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.fit.FitImportService
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.AppUpdateRepository
import com.oterman.rundemo.data.repository.AvatarManager
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.data.repository.TokenRefreshManager
import com.oterman.rundemo.data.repository.UserRepository
import com.oterman.rundemo.service.sync.DataSyncForegroundService
import com.oterman.rundemo.service.sync.SyncUiState
import com.oterman.rundemo.service.sync.UnifiedDataSyncManager
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * HomeScreen ViewModel
 * Manages tab navigation and profile/auth state
 * Corresponds to iOS NavigationCoordinator functionality
 */
class HomeViewModel(
    private val context: Context,
    private val preferencesManager: PreferencesManager = PreferencesManager(context),
    private val fitImportService: FitImportService = FitImportService(context),
    private val userRepository: UserRepository = UserRepository(context),
    private val syncManager: UnifiedDataSyncManager = UnifiedDataSyncManager.getInstance(context),
    private val avatarManager: AvatarManager = AvatarManager.getInstance(context)
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
        private const val AUTO_SYNC_INTERVAL_MS = 5 * 60 * 1000L // 5分钟自动同步间隔
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var syncIconDelayJob: Job? = null
    private var lastAutoSyncTime: Long = 0L

    init {
        loadAuthState()
        observeSyncState()
        performDailyTokenRefresh()
        observeTokenExpired()
    }

    /**
     * 启动时触发每日 Token 刷新
     */
    private fun performDailyTokenRefresh() {
        viewModelScope.launch {
            TokenRefreshManager.getInstance(context).performDailyTokenRefreshIfNeeded()
        }
    }

    /**
     * 监听 Token 过期事件，触发退出到欢迎页
     */
    private fun observeTokenExpired() {
        viewModelScope.launch {
            TokenRefreshManager.getInstance(context).tokenExpiredEvent.collect {
                RLog.w(TAG, "Token 已过期，强制登出")
                _uiState.update { it.copy(navigateToWelcome = true) }
            }
        }
    }

    /**
     * Load authentication state from PreferencesManager
     */
    private fun loadAuthState() {
        val isLoggedIn = preferencesManager.isUserLoggedIn()
        val userName = preferencesManager.getUserName()
        val phoneNumber = preferencesManager.getPhoneNumber()
        val userId = preferencesManager.getUserId()

        RLog.d(TAG, "Auth state loaded: isLoggedIn=$isLoggedIn, userName=$userName, userId=$userId")

        _uiState.update { state ->
            state.copy(
                isLoggedIn = isLoggedIn,
                userName = userName,
                phoneNumber = phoneNumber
            )
        }

        // 如果已登录，设置 repository userId 并异步加载头像
        if (isLoggedIn && userId != null) {
            RunDataRepositoryImpl.getInstance(RunDatabase.getInstance(context)).setCurrentUserId(userId)
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

            RLog.d(TAG, "开始加载头像URL: userId=$userId")

            val result = avatarManager.getAvatarUrl(userId)

            result.onSuccess { url ->
                RLog.d(TAG, "头像URL加载成功: $url")
                _uiState.update { it.copy(avatarUrl = url, isLoadingAvatar = false) }
            }.onFailure { e ->
                RLog.e(TAG, "头像URL加载失败: ${e.message}")
                _uiState.update { it.copy(avatarUrl = null, isLoadingAvatar = false) }
            }
        }
    }

    /**
     * 观察同步状态，更新UI
     */
    private fun observeSyncState() {
        viewModelScope.launch {
            syncManager.syncUiState.collect { syncState ->
                when (syncState) {
                    is SyncUiState.Idle -> {
                        syncIconDelayJob?.cancel()
                        _uiState.update {
                            it.copy(isSyncing = false, showSyncIcon = false)
                        }
                    }
                    is SyncUiState.Syncing -> {
                        _uiState.update { it.copy(isSyncing = true) }
                        // 2秒后才显示同步图标（匹配iOS行为）
                        syncIconDelayJob?.cancel()
                        syncIconDelayJob = viewModelScope.launch {
                            delay(2000)
                            _uiState.update { it.copy(showSyncIcon = true) }
                        }
                    }
                    is SyncUiState.Completed -> {
                        syncIconDelayJob?.cancel()
                        val result = syncState.result
                        val message = if (result.totalImportedCount > 0) {
                            "成功导入 ${result.totalImportedCount} 条数据"
                        } else null
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                showSyncIcon = false,
                                syncSuccessMessage = message
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 启动同步（通过ForegroundService）
     * Android 13+ 无通知权限时仍正常同步，但会设置标志触发权限引导弹窗
     */
    fun startSyncIfNeeded() {
        if (_uiState.value.isSyncing) {
            RLog.d(TAG, "已在同步中，跳过")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastAutoSyncTime < AUTO_SYNC_INTERVAL_MS) {
            RLog.d(TAG, "距上次自动同步不足5分钟，跳过")
            return
        }

        // 检查通知权限（API 33+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                if (preferencesManager.wasNotificationDeniedToday()) {
                    RLog.i(TAG, "今日已拒绝通知权限，跳过引导")
                } else {
                    RLog.i(TAG, "通知权限未授予，设置权限引导标志")
                    _uiState.update { it.copy(needsNotificationPermission = true) }
                }
            }
        }

        // 无论权限状态如何，都正常启动同步
        lastAutoSyncTime = now
        RLog.i(TAG, "首页触发自动同步")
        DataSyncForegroundService.start(context)
    }

    /**
     * 手动触发同步（下拉刷新调用），不受自动同步间隔限制
     */
    fun manualSync() {
        if (_uiState.value.isSyncing) {
            RLog.d(TAG, "已在同步中，跳过手动同步")
            return
        }
        lastAutoSyncTime = System.currentTimeMillis()
        RLog.i(TAG, "手动触发同步")
        DataSyncForegroundService.start(context)
    }

    /**
     * 关闭通知权限引导弹窗
     */
    fun dismissNotificationPermissionRequest(saveDenial: Boolean = true) {
        if (saveDenial) {
            preferencesManager.saveNotificationDeniedDate()
            RLog.i(TAG, "通知权限被拒，记录日期: ${java.time.LocalDate.now()}")
        }
        _uiState.update { it.copy(needsNotificationPermission = false) }
    }

    /**
     * 清除同步成功消息
     */
    fun dismissSyncSuccess() {
        _uiState.update { it.copy(syncSuccessMessage = null) }
    }

    /**
     * Refresh auth state (call when returning from login)
     */
    fun refreshAuthState() {
        loadAuthState()
    }

    /**
     * 刷新个人资料数据（从UserProfileScreen返回时调用）
     * 重新读取PreferencesManager中的用户名/手机号，并强制刷新头像URL
     */
    fun refreshProfileData() {
        val currentUserName = preferencesManager.getUserName()
        val currentPhoneNumber = preferencesManager.getPhoneNumber()
        val userId = preferencesManager.getUserId()

        _uiState.update { state ->
            state.copy(
                userName = currentUserName,
                phoneNumber = currentPhoneNumber
            )
        }

        // 复用缓存头像URL（upload后缓存已更新，无需强制刷新）
        if (userId != null) {
            viewModelScope.launch {
                val result = avatarManager.getAvatarUrl(userId, forceRefresh = false)
                result.onSuccess { url ->
                    _uiState.update { it.copy(avatarUrl = url) }
                }.onFailure { e ->
                    RLog.e(TAG, "头像刷新失败: ${e.message}")
                }
            }
        }
    }

    /**
     * Switch selected tab
     */
    fun selectTab(tab: HomeTab) {
        RLog.d(TAG, "Tab selected: $tab")
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
            RLog.i(TAG, "User logging out")
            _uiState.update { it.copy(isLoggingOut = true, showLogoutConfirmDialog = false) }

            // Clear userId from repository
            RunDataRepositoryImpl.getInstance(RunDatabase.getInstance(context)).setCurrentUserId(null)

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

            RLog.i(TAG, "Logout completed")
        }
    }

    /**
     * Trigger navigation to login page
     */
    fun navigateToLogin() {
        RLog.d(TAG, "Navigate to login requested")
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
        RLog.d(TAG, "Navigate to welcome requested")
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
        RLog.d(TAG, "Reset first launch state")
        navigateToWelcome()
    }
    
    // ==================== FIT文件导入 ====================
    
    /**
     * 导入FIT文件
     * @param uri 选择的FIT文件Uri
     */
    fun importFitFile(uri: Uri) {
        RLog.i(TAG, "开始导入FIT文件: $uri")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingFit = true) }
            
            val result = fitImportService.importFitFile(uri)

            when (result) {
                is FitImportResult.ConflictFound -> {
                    RLog.w(TAG, "发现时间冲突: ${result.conflictingRecords.size}条记录")
                    _uiState.update {
                        it.copy(
                            isImportingFit = false,
                            showConflictDialog = true,
                            conflictingRecords = result.conflictingRecords,
                            pendingFitUri = result.pendingUri
                        )
                    }
                }
                is FitImportResult.Success -> {
                    RLog.i(TAG, "导入成功: ${result.distance}km, ${result.duration}min")
                    _uiState.update {
                        it.copy(isImportingFit = false, showImportResultDialog = true, fitImportResult = result)
                    }
                }
                is FitImportResult.AlreadyExists -> {
                    RLog.w(TAG, "文件已导入过")
                    _uiState.update {
                        it.copy(isImportingFit = false, showImportResultDialog = true, fitImportResult = result)
                    }
                }
                is FitImportResult.Error -> {
                    RLog.e(TAG, "导入失败: ${result.message}")
                    _uiState.update {
                        it.copy(isImportingFit = false, showImportResultDialog = true, fitImportResult = result)
                    }
                }
                is FitImportResult.UploadFailed -> {
                    RLog.e(TAG, "上传失败: ${result.message}")
                    _uiState.update {
                        it.copy(isImportingFit = false, showImportResultDialog = true, fitImportResult = result)
                    }
                }
            }
        }
    }

    fun confirmConflictImport() {
        val uri = _uiState.value.pendingFitUri ?: return
        _uiState.update { it.copy(showConflictDialog = false, conflictingRecords = emptyList(), pendingFitUri = null) }
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingFit = true) }
            val result = fitImportService.importFitFile(uri, forceImport = true)
            _uiState.update { it.copy(isImportingFit = false, showImportResultDialog = true, fitImportResult = result) }
        }
    }

    fun skipConflictImport() {
        _uiState.update { it.copy(showConflictDialog = false, conflictingRecords = emptyList(), pendingFitUri = null) }
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

    // ==================== 强制更新 ====================

    /**
     * 启动时自动检查版本，仅当 forceUpgrade==true 时弹窗
     */
    fun checkUpdateOnLaunch() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                AppUpdateRepository.checkLatestVersion(context)
            }
            result.onSuccess { info ->
                if (info != null && info.response.forceUpgrade == true) {
                    _uiState.update { it.copy(forceUpdateInfo = info, showForceUpdateDialog = true) }
                }
            }
        }
    }

    fun dismissForceUpdateDialog() {
        _uiState.update { it.copy(showForceUpdateDialog = false) }
    }

    fun clearForceUpdate() {
        _uiState.update { it.copy(forceUpdateInfo = null, showForceUpdateDialog = false) }
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
