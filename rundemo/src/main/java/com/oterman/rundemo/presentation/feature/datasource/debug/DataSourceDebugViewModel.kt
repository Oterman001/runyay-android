package com.oterman.rundemo.presentation.feature.datasource.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.service.sync.SyncUiState
import com.oterman.rundemo.service.sync.UnifiedDataSyncManager
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 数据源调试界面ViewModel
 */
class DataSourceDebugViewModel(
    private val platform: DataSourcePlatform,
    private val dataSourcePreferences: DataSourcePreferences,
    private val runRecordDao: RunRecordDao,
    private val syncManager: UnifiedDataSyncManager
) : ViewModel() {

    companion object {
        private const val TAG = "DataSourceDebugVM"
    }

    private val _uiState = MutableStateFlow(DataSourceDebugUiState(platform = platform))
    val uiState: StateFlow<DataSourceDebugUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeSyncState()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val timestamp = dataSourcePreferences.getLastSyncTime(platform)
                val records = runRecordDao.getByDatasource(platform.code)
                _uiState.update {
                    it.copy(
                        lastSyncTimestamp = timestamp,
                        recordCount = records.size,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "加载数据失败: ${e.message}"
                    )
                }
            }
        }
    }

    private fun observeSyncState() {
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
                            "同步完成，没有新记录"
                        }
                        _uiState.update { it.copy(isSyncing = false, message = message) }
                        loadData()
                    }
                    is SyncUiState.Idle -> {
                        _uiState.update { it.copy(isSyncing = false) }
                    }
                }
            }
        }
    }

    fun manualSync() {
        if (syncManager.isAnySyncing()) {
            _uiState.update { it.copy(message = "正在同步中，请勿重复操作") }
            return
        }
        RLog.i(TAG, "触发手动同步: ${platform.displayName}")
        _uiState.update { it.copy(isSyncing = true) }
        syncManager.startManualSyncViaService(platform)
    }

    fun clearSyncTimestamp() {
        viewModelScope.launch {
            dataSourcePreferences.clearSyncTime(platform)
            _uiState.update { it.copy(message = "同步时间戳已清除") }
            loadData()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun refresh() {
        loadData()
    }
}
