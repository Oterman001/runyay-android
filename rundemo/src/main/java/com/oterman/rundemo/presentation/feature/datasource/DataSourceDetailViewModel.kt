package com.oterman.rundemo.presentation.feature.datasource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.domain.model.DataSourceInfo
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.ImportedRunSummary
import com.oterman.rundemo.domain.model.SyncTimeRange
import com.oterman.rundemo.service.sync.SyncUiState
import com.oterman.rundemo.service.sync.UnifiedDataSyncManager
import com.oterman.rundemo.service.sync.model.SyncNotification
import com.oterman.rundemo.BuildConfig
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据源详情ViewModel
 */
class DataSourceDetailViewModel(
    private val platform: DataSourcePlatform,
    private val repository: DataSourceRepository,
    private val syncManager: UnifiedDataSyncManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "DataSourceDetailVM"
    }
    
    private val _uiState = MutableStateFlow(DataSourceDetailUiState(platform = platform))
    val uiState: StateFlow<DataSourceDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadDataSourceInfo()
        observeSyncState()
        // 若启动时已有同步在进行（用户返回详情页场景），恢复 isSyncing 状态
        if (syncManager.isAnySyncing()) {
            _uiState.update { it.copy(isSyncing = true) }
        }
    }
    
    /**
     * 加载数据源信息
     */
    private fun loadDataSourceInfo() {
        val info = DataSourceInfo.getDataSource(platform)
        val isAuthorized = repository.isPlatformBound(platform)
        
        _uiState.update { 
            it.copy(
                dataSourceInfo = info,
                isAuthorized = isAuthorized
            ) 
        }
    }
    
    /**
     * 观察全局同步状态和通知
     * - syncUiState：驱动 isSyncing / isSyncFinished 状态（包括退出后返回的场景）
     * - syncNotifications：过滤本平台通知，实时更新 importedRecords
     */
    private fun observeSyncState() {
        // 观察全局同步UI状态
        viewModelScope.launch {
            syncManager.syncUiState.collect { state ->
                when (state) {
                    is SyncUiState.Syncing -> {
                        _uiState.update { it.copy(isSyncing = true) }
                    }
                    is SyncUiState.Completed -> {
                        val importedCount = state.result.totalImportedCount
                        val message = if (importedCount > 0) {
                            "同步完成，已导入 $importedCount 条记录"
                        } else {
                            if (platform == DataSourcePlatform.GARMIN_GLOBAL) {
                                "发起请求完成，数据会陆续同步下来哦"
                            } else {
                                null
                            }
                        }
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                isSyncFinished = true,
                                alertMessage = if (it.isSyncing) message else it.alertMessage
                            )
                        }
                    }
                    is SyncUiState.Idle -> {
                        _uiState.update { it.copy(isSyncing = false) }
                    }
                }
            }
        }

        // 观察同步通知，过滤本平台事件更新实时进度列表
        viewModelScope.launch {
            syncManager.syncNotifications.collect { notification ->
                when (notification) {
                    is SyncNotification.RecordImported -> {
                        if (notification.platform == platform) {
                            val newRecord = ImportedRunSummary(
                                originId = notification.originId,
                                platformCode = platform.code,
                                runDate = Date(),
                                distance = 0.0,
                                displayText = notification.displayText
                            )
                            _uiState.update { state ->
                                val newRecords = (listOf(newRecord) + state.importedRecords).take(50)
                                state.copy(importedRecords = newRecords)
                            }
                        }
                    }
                    is SyncNotification.BackfillCompleted -> {
                        if (notification.platform == platform &&
                            (platform == DataSourcePlatform.GARMIN_CHINA || platform == DataSourcePlatform.COROS)) {
                            _uiState.update { it.copy(showBackfillSuccessDialog = true) }
                        }
                    }
                    else -> { /* 其他通知类型由 syncUiState 驱动处理 */ }
                }
            }
        }
    }

    /**
     * 刷新授权状态
     */
    fun refreshAuthStatus() {
        viewModelScope.launch {
            repository.queryPlatformStatus()
                .onSuccess {
                    val isAuthorized = repository.isPlatformBound(platform)
                    _uiState.update { it.copy(isAuthorized = isAuthorized) }
                }
                .onFailure { error ->
                    RLog.e(TAG, "刷新授权状态失败", error)
                }
        }
    }
    
    /**
     * 开始授权流程
     */
    fun startAuthorization() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            repository.getBindUrl(platform)
                .onSuccess { authUrl ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            authUrl = authUrl,
                            showOAuthWebView = true
                        ) 
                    }
                }
                .onFailure { error ->
                    RLog.e(TAG, "获取授权URL失败", error)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message,
                            alertMessage = "获取授权URL失败：${error.message}"
                        ) 
                    }
                }
        }
    }
    
    /**
     * 处理OAuth回调
     * 根据平台类型调用不同的回调处理方法
     */
    fun handleOAuthCallback(params: OAuthCallbackParams) {
        RLog.i(TAG, "处理OAuth回调: platform=${platform.code}, params=$params")

        _uiState.update {
            it.copy(
                showOAuthWebView = false,
                isLoading = true
            )
        }

        viewModelScope.launch {
            val result = when (params) {
                is OAuthCallbackParams.OAuth1 -> {
                    RLog.d(TAG, "调用佳明回调处理, token=${params.oauthToken}, verifier=${params.oauthVerifier}")
                    repository.handleGarminOAuthCallback(
                        platform = platform,
                        oauthToken = params.oauthToken,
                        oauthVerifier = params.oauthVerifier
                    )
                }
                is OAuthCallbackParams.OAuth2 -> {
                    RLog.d(TAG, "调用高驰回调处理, code=${params.code}, state=${params.state}")
                    repository.handleCorosOAuthCallback(
                        code = params.code,
                        state = params.state
                    )
                }
            }

            result
                .onSuccess {
                    RLog.i(TAG, "OAuth回调处理成功，授权完成")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthorized = true,
                            alertMessage = "授权成功"
                        )
                    }
                }
                .onFailure { error ->
                    RLog.e(TAG, "处理OAuth回调失败", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message,
                            alertMessage = "授权失败：${error.message}"
                        )
                    }
                }
        }
    }
    
    /**
     * 关闭OAuth WebView
     */
    fun dismissOAuthWebView() {
        _uiState.update { 
            it.copy(
                showOAuthWebView = false,
                authUrl = null,
                isLoading = false
            ) 
        }
    }
    
    /**
     * 显示解绑确认弹窗
     */
    fun showUnbindConfirmDialog() {
        _uiState.update { it.copy(showUnbindConfirmDialog = true) }
    }
    
    /**
     * 关闭解绑确认弹窗
     */
    fun dismissUnbindConfirmDialog() {
        _uiState.update { it.copy(showUnbindConfirmDialog = false) }
    }
    
    /**
     * 确认解绑
     */
    fun confirmUnbind() {
        _uiState.update { 
            it.copy(
                showUnbindConfirmDialog = false,
                isUnbinding = true
            ) 
        }
        
        viewModelScope.launch {
            repository.unbindPlatform(platform)
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            isUnbinding = false,
                            isAuthorized = false,
                            alertMessage = "已取消授权"
                        ) 
                    }
                }
                .onFailure { error ->
                    RLog.e(TAG, "解绑失败", error)
                    _uiState.update { 
                        it.copy(
                            isUnbinding = false,
                            error = error.message,
                            alertMessage = "取消授权失败：${error.message}"
                        ) 
                    }
                }
        }
    }
    
    /**
     * 显示同步选项
     */
    fun showSyncOptions() {
        _uiState.update { it.copy(showSyncOptionsDialog = true) }
    }
    
    /**
     * 关闭同步选项
     */
    fun dismissSyncOptions() {
        _uiState.update { it.copy(showSyncOptionsDialog = false) }
    }
    
    /**
     * 手动同步（通过前台服务启动，退出界面后同步继续在后台执行）
     * 佳明中国/高驰弹出时间范围选择弹窗，其他平台直接同步
     */
    fun manualSync() {
        // 全局防重复：任意平台在同步中都不允许再次触发
        if (syncManager.isAnySyncing()) {
            _uiState.update { it.copy(alertMessage = "正在同步中，请勿重复操作") }
            return
        }

        when (platform) {
            DataSourcePlatform.GARMIN_CHINA, DataSourcePlatform.COROS -> {
                // 佳明中国/高驰弹出时间范围选择弹窗
                showSyncOptions()
            }
            DataSourcePlatform.APPLE_HEALTH -> {
                // 苹果健康直接同步，不弹时间范围选择
                startSyncWithTimeRange(null)
            }
            else -> {
                // 其他平台直接开始同步
                startSyncWithTimeRange(null)
            }
        }
    }

    /**
     * 选择时间范围后开始同步
     */
    fun startSyncWithTimeRange(timeRange: SyncTimeRange?) {
        RLog.i(TAG, "触发手动同步（前台服务模式）: ${platform.displayName}, timeRange=${timeRange?.displayName}")

        // 重置本次同步的进度数据
        _uiState.update {
            it.copy(
                showSyncOptionsDialog = false,
                isSyncing = true,
                isSyncFinished = false,
                importedRecords = emptyList()
            )
        }

        // 通过前台服务启动同步，即使退出界面同步也会持续执行
        // 实际同步运行在 UnifiedDataSyncManager.scope（App级别），不受 ViewModel 生命周期约束
        syncManager.startManualSyncViaService(platform, timeRange)
    }
    
    /**
     * 关闭回填成功弹窗
     */
    fun dismissBackfillSuccessDialog() {
        _uiState.update { it.copy(showBackfillSuccessDialog = false) }
    }

    /**
     * 选择同步时间范围（带渠道控制）
     * ALL时：fir渠道弹口令弹窗，其他渠道提示联系客服
     */
    fun onSyncTimeRangeSelected(timeRange: SyncTimeRange) {
        if (timeRange == SyncTimeRange.ALL) {
            _uiState.update { it.copy(showSyncOptionsDialog = false) }
            if (BuildConfig.UMENG_CHANNEL == "fir") {
                _uiState.update { it.copy(showPassphraseDialog = true, passphraseError = false) }
            } else {
                _uiState.update { it.copy(alertMessage = "当前版本暂不支持同步所有数据，如有需要请联系客服(微信加yayarunya)处理") }
            }
        } else {
            startSyncWithTimeRange(timeRange)
        }
    }

    /**
     * 确认口令，校验通过后触发同步所有数据
     */
    fun confirmPassphrase(input: String) {
        val today = SimpleDateFormat("ddMM", Locale.getDefault()).format(Date())
        val expected = "runyay$today"
        if (input == expected) {
            _uiState.update { it.copy(showPassphraseDialog = false, passphraseError = false) }
            startSyncWithTimeRange(SyncTimeRange.ALL)
        } else {
            _uiState.update { it.copy(passphraseError = true) }
        }
    }

    /**
     * 关闭口令弹窗
     */
    fun dismissPassphraseDialog() {
        _uiState.update { it.copy(showPassphraseDialog = false, passphraseError = false) }
    }

    /**
     * 清除提示消息
     */
    fun clearAlertMessage() {
        _uiState.update { it.copy(alertMessage = null) }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

