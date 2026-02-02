package com.oterman.rundemo.presentation.feature.datasource.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.util.TimestampUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

/**
 * 数据源记录列表ViewModel
 */
class DataSourceRecordListViewModel(
    private val platform: DataSourcePlatform,
    private val runRecordDao: RunRecordDao,
    private val dataSourcePreferences: DataSourcePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataSourceRecordListUiState(platform = platform))
    val uiState: StateFlow<DataSourceRecordListUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val records = runRecordDao.getByDatasource(platform.code)
                _uiState.update { it.copy(records = records, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "加载记录失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun toggleSelection(workoutId: String) {
        _uiState.update { state ->
            val newSelection = if (workoutId in state.selectedIds) {
                state.selectedIds - workoutId
            } else {
                state.selectedIds + workoutId
            }
            state.copy(selectedIds = newSelection)
        }
    }

    fun toggleSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = !it.isSelectionMode, selectedIds = emptySet()) }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedIds = state.records.map { it.workoutId }.toSet())
        }
    }

    fun deselectAll() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }

            try {
                val selectedIds = _uiState.value.selectedIds
                val recordsToDelete = _uiState.value.records.filter { it.workoutId in selectedIds }

                if (recordsToDelete.isEmpty()) {
                    _uiState.update { it.copy(isDeleting = false, message = "没有选中的记录") }
                    return@launch
                }

                // 找到最早的startTime
                val earliestStartTime = recordsToDelete.minOfOrNull { it.startTime }

                // 删除记录
                selectedIds.forEach { workoutId ->
                    runRecordDao.deleteByWorkoutId(workoutId)
                }

                // 更新同步时间戳到最早删除记录的时间
                if (earliestStartTime != null) {
                    val timestamp = TimestampUtils.formatFromDate(Date(earliestStartTime))
                    dataSourcePreferences.setLastSyncTime(platform, timestamp)
                }

                // 刷新列表
                loadRecords()
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        selectedIds = emptySet(),
                        isSelectionMode = false,
                        message = "已删除 ${recordsToDelete.size} 条记录，同步时间戳已更新"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        message = "删除失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun refresh() {
        loadRecords()
    }
}
