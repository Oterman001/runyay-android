package com.oterman.rundemo.presentation.feature.datasource.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.domain.model.DataSourcePlatform
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
    private val runRecordDao: RunRecordDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataSourceDebugUiState(platform = platform))
    val uiState: StateFlow<DataSourceDebugUiState> = _uiState.asStateFlow()

    init {
        loadData()
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
