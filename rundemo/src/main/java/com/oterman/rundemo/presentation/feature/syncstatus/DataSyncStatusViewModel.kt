package com.oterman.rundemo.presentation.feature.syncstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.service.sync.SyncUiState
import com.oterman.rundemo.service.sync.UnifiedDataSyncManager
import com.oterman.rundemo.service.sync.model.SyncNotification
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DataSyncStatusViewModel(
    private val syncManager: UnifiedDataSyncManager
) : ViewModel() {

    companion object {
        private const val TAG = "DataSyncStatusVM"
    }

    private val _uiState = MutableStateFlow(DataSyncStatusUiState())
    val uiState: StateFlow<DataSyncStatusUiState> = _uiState.asStateFlow()

    init {
        checkInitialSyncStatus()
        observeSyncUiState()
        observeSyncNotifications()
    }

    private fun checkInitialSyncStatus() {
        val currentState = syncManager.syncUiState.value
        RLog.i(TAG, "检查初始同步状态: $currentState")
        when (currentState) {
            is SyncUiState.Syncing -> {
                _uiState.update { it.copy(syncStatus = SyncStatusType.SYNCING) }
            }
            is SyncUiState.Completed -> {
                _uiState.update {
                    it.copy(
                        syncStatus = SyncStatusType.COMPLETED,
                        syncResult = currentState.result
                    )
                }
            }
            is SyncUiState.Idle -> {
                // 保持 PREPARING
            }
        }
    }

    private fun observeSyncUiState() {
        viewModelScope.launch {
            syncManager.syncUiState.collect { state ->
                when (state) {
                    is SyncUiState.Syncing -> {
                        RLog.i(TAG, "同步状态变更: Syncing")
                        _uiState.update {
                            it.copy(
                                syncStatus = SyncStatusType.SYNCING,
                                // 重新开始同步时清空之前的记录和结果
                                importedRecords = emptyList(),
                                syncResult = null
                            )
                        }
                    }
                    is SyncUiState.Completed -> {
                        // 不在这里处理 Completed，由 UnifiedCompleted 通知独立处理
                        // 避免 syncUiState 3秒后回退 Idle 导致状态丢失
                    }
                    is SyncUiState.Idle -> {
                        // 如果已经是 COMPLETED 状态，不回退
                        // 只有当前还是 PREPARING 时才保持
                    }
                }
            }
        }
    }

    private fun observeSyncNotifications() {
        viewModelScope.launch {
            syncManager.syncNotifications.collect { notification ->
                when (notification) {
                    is SyncNotification.RecordImported -> {
                        val record = ImportedRecordItem(
                            originId = notification.originId,
                            platformName = notification.platform.displayName,
                            displayText = notification.displayText
                        )
                        RLog.i(TAG, "导入记录: ${record.displayText}")
                        _uiState.update { state ->
                            state.copy(importedRecords = state.importedRecords + record)
                        }
                    }
                    is SyncNotification.UnifiedCompleted -> {
                        RLog.i(TAG, "同步完成: 导入${notification.result.totalImportedCount}条")
                        _uiState.update {
                            it.copy(
                                syncStatus = SyncStatusType.COMPLETED,
                                syncResult = notification.result
                            )
                        }
                    }
                    is SyncNotification.UnifiedFailed -> {
                        RLog.e(TAG, "同步失败: ${notification.error}")
                        _uiState.update {
                            it.copy(syncStatus = SyncStatusType.COMPLETED)
                        }
                    }
                    else -> {
                        // PlatformStarted, PlatformProgress, PlatformCompleted, PlatformFailed
                        // 不需要额外处理
                    }
                }
            }
        }
    }
}
