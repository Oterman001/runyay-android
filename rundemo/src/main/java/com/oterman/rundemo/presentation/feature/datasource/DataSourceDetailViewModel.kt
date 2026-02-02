package com.oterman.rundemo.presentation.feature.datasource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.domain.model.DataSourceInfo
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.ImportedRunSummary
import com.oterman.rundemo.service.sync.UnifiedDataSyncManager
import com.oterman.rundemo.service.sync.model.SyncNotification
import com.oterman.rundemo.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

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
                    Logger.e(TAG, "刷新授权状态失败", error)
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
                    Logger.e(TAG, "获取授权URL失败", error)
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
        Logger.i(TAG, "处理OAuth回调: platform=${platform.code}, params=$params")

        _uiState.update {
            it.copy(
                showOAuthWebView = false,
                isLoading = true
            )
        }

        viewModelScope.launch {
            val result = when (params) {
                is OAuthCallbackParams.OAuth1 -> {
                    Logger.d(TAG, "调用佳明回调处理, token=${params.oauthToken}, verifier=${params.oauthVerifier}")
                    repository.handleGarminOAuthCallback(
                        platform = platform,
                        oauthToken = params.oauthToken,
                        oauthVerifier = params.oauthVerifier
                    )
                }
                is OAuthCallbackParams.OAuth2 -> {
                    Logger.d(TAG, "调用高驰回调处理, code=${params.code}, state=${params.state}")
                    repository.handleCorosOAuthCallback(
                        code = params.code,
                        state = params.state
                    )
                }
            }

            result
                .onSuccess {
                    Logger.i(TAG, "OAuth回调处理成功，授权完成")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthorized = true,
                            alertMessage = "授权成功"
                        )
                    }
                }
                .onFailure { error ->
                    Logger.e(TAG, "处理OAuth回调失败", error)
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
                    Logger.e(TAG, "解绑失败", error)
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
     * 手动同步（使用默认时间范围）
     */
    fun manualSync() {
        // 检查是否正在同步
        if (syncManager.isPlatformSyncing(platform)) {
            _uiState.update { it.copy(alertMessage = "正在同步中，请勿重复操作") }
            return
        }

        // 重置状态
        _uiState.update {
            it.copy(
                showSyncOptionsDialog = false,
                isSyncing = true,
                isSyncFinished = false,
                importedRecords = emptyList()
            )
        }

        viewModelScope.launch {
            Logger.i(TAG, "开始手动同步: ${platform.displayName}")
            syncManager.triggerManualSync(platform).collect { notification ->
                handleSyncNotification(notification)
            }
        }
    }

    /**
     * 处理同步通知
     */
    private fun handleSyncNotification(notification: SyncNotification) {
        when (notification) {
            is SyncNotification.PlatformStarted -> {
                Logger.d(TAG, "同步开始: ${notification.platform.displayName}")
            }
            is SyncNotification.PlatformProgress -> {
                Logger.d(TAG, "同步进度: ${notification.current}/${notification.total} - ${notification.message}")
            }
            is SyncNotification.RecordImported -> {
                Logger.d(TAG, "导入记录: ${notification.displayText}")
                // 添加导入记录到列表（最新的在前，最多50条）
                _uiState.update { state ->
                    val newRecord = createImportedSummary(notification)
                    val newRecords = (listOf(newRecord) + state.importedRecords).take(50)
                    state.copy(importedRecords = newRecords)
                }
            }
            is SyncNotification.PlatformCompleted -> {
                Logger.i(TAG, "同步完成: ${notification.platform.displayName}, 导入: ${notification.result.importedCount}")
                val message = if (notification.result.importedCount > 0) {
                    "同步完成，已导入 ${notification.result.importedCount} 条记录"
                } else {
                    "同步完成，没有新记录"
                }
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        isSyncFinished = true,
                        alertMessage = message
                    )
                }
            }
            is SyncNotification.PlatformFailed -> {
                Logger.e(TAG, "同步失败: ${notification.platform.displayName}, 错误: ${notification.error}")
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        isSyncFinished = true,
                        alertMessage = "同步失败：${notification.error}"
                    )
                }
            }
            else -> {
                // 忽略其他通知类型（UnifiedCompleted, UnifiedFailed等）
                Logger.d(TAG, "忽略通知: $notification")
            }
        }
    }

    /**
     * 从同步通知创建导入记录摘要
     */
    private fun createImportedSummary(notification: SyncNotification.RecordImported): ImportedRunSummary {
        return ImportedRunSummary(
            originId = notification.originId,
            platformCode = platform.code,
            runDate = Date(),
            distance = 0.0,
            displayText = notification.displayText
        )
    }
    
    /**
     * 添加导入记录（用于实时显示）
     */
    fun addImportedRecord(record: ImportedRunSummary) {
        _uiState.update { state ->
            val newRecords = (state.importedRecords + record).takeLast(50)
            state.copy(importedRecords = newRecords)
        }
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

