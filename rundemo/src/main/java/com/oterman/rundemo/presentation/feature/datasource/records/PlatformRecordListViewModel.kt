package com.oterman.rundemo.presentation.feature.datasource.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.fit.VdotRecalculationService
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.network.dto.request.toUpdateRequest
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.service.sync.UnifiedDataSyncManager
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class PlatformRecordListUiState(
    val platform: DataSourcePlatform,
    val records: List<RunRecordEntity> = emptyList(),
    val isLoading: Boolean = true,
    val pendingDeleteWorkoutId: String? = null,
    val isSelectionMode: Boolean = false,
    val selectedWorkoutIds: Set<String> = emptySet(),
    val isBatchDeleting: Boolean = false,
    val showBatchDeleteConfirm: Boolean = false
)

class PlatformRecordListViewModel(
    private val platform: DataSourcePlatform,
    private val repository: RunDataRepository,
    private val syncManager: UnifiedDataSyncManager
) : ViewModel() {

    private val vdotRecalculationService = VdotRecalculationService(repository)

    companion object {
        private const val TAG = "PlatformRecordListVM"
    }

    private val _uiState = MutableStateFlow(PlatformRecordListUiState(platform = platform))
    val uiState: StateFlow<PlatformRecordListUiState> = _uiState.asStateFlow()

    // TrackPoints 缓存
    private val trackPointsCache = ConcurrentHashMap<String, List<TrackPoint>>()
    private val loadingTrackPoints = ConcurrentHashMap.newKeySet<String>()

    private val _trackPointsVersion = MutableStateFlow(0L)
    val trackPointsVersion: StateFlow<Long> = _trackPointsVersion.asStateFlow()

    init {
        loadRecords()
    }

    fun requestDelete(workoutId: String) {
        _uiState.update { it.copy(pendingDeleteWorkoutId = workoutId) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDeleteWorkoutId = null) }
    }

    fun confirmDelete() {
        val workoutId = _uiState.value.pendingDeleteWorkoutId ?: return
        viewModelScope.launch {
            try {
                repository.deleteRunRecord(workoutId)
                RLog.i(TAG, "删除记录成功: $workoutId")
                loadRecords()
            } catch (e: Exception) {
                RLog.e(TAG, "删除记录失败", e)
            } finally {
                _uiState.update { it.copy(pendingDeleteWorkoutId = null) }
            }
        }
    }

    fun enterSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = true, selectedWorkoutIds = emptySet()) }
    }

    fun exitSelectionMode() {
        _uiState.update {
            it.copy(
                isSelectionMode = false,
                selectedWorkoutIds = emptySet(),
                showBatchDeleteConfirm = false
            )
        }
    }

    fun toggleSelection(workoutId: String) {
        _uiState.update { state ->
            val updated = if (state.selectedWorkoutIds.contains(workoutId)) {
                state.selectedWorkoutIds - workoutId
            } else {
                state.selectedWorkoutIds + workoutId
            }
            state.copy(selectedWorkoutIds = updated)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedWorkoutIds = state.records.map { it.workoutId }.toSet())
        }
    }

    fun requestBatchDelete() {
        if (_uiState.value.selectedWorkoutIds.isEmpty()) return
        _uiState.update { it.copy(showBatchDeleteConfirm = true) }
    }

    fun dismissBatchDeleteConfirm() {
        _uiState.update { it.copy(showBatchDeleteConfirm = false) }
    }

    fun confirmBatchDelete() {
        val selectedIds = _uiState.value.selectedWorkoutIds
        if (selectedIds.isEmpty()) return

        val records = _uiState.value.records.filter { it.workoutId in selectedIds }

        viewModelScope.launch {
            _uiState.update { it.copy(isBatchDeleting = true, showBatchDeleteConfirm = false) }
            try {
                for (record in records) {
                    try {
                        if (record.originId != null) {
                            val result = syncManager.deleteRunSummary(record.originId, record.workoutId)
                            if (result.isFailure) {
                                RLog.w(TAG, "服务端删除失败，仍删除本地: workoutId=${record.workoutId}, err=${result.exceptionOrNull()?.message}")
                                repository.deleteRunRecord(record.workoutId)
                            }
                        } else {
                            repository.deleteRunRecord(record.workoutId)
                        }
                        RLog.i(TAG, "批量删除 - 记录已删除: ${record.workoutId}")
                    } catch (e: Exception) {
                        RLog.e(TAG, "批量删除 - 单条删除失败: ${record.workoutId}", e)
                    }
                }
                loadRecords()
            } finally {
                _uiState.update {
                    it.copy(
                        isBatchDeleting = false,
                        isSelectionMode = false,
                        selectedWorkoutIds = emptySet()
                    )
                }
            }
        }
    }

    fun updateInclusiveLevel(record: RunRecordEntity, newLevel: Int) {
        viewModelScope.launch {
            val updatedRecord = record.copy(inclusiveLevel = newLevel, uploadStatus = 0)
            try {
                repository.updatePBInclusiveLevel(record.workoutId, newLevel)
                repository.updateVdotInclusiveLevel(record.workoutId, newLevel)
                repository.updateRunRecord(updatedRecord)
            } catch (_: Exception) { return@launch }

            // 同步到服务器
            if (updatedRecord.originId != null) {
                try {
                    val request = updatedRecord.toUpdateRequest()
                    val result = syncManager.updateRunSummary(request)
                    if (result.isSuccess) {
                        repository.updateRunRecord(updatedRecord.copy(uploadStatus = 2))
                    } else {
                        RLog.w(TAG, "同步服务器失败: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    RLog.w(TAG, "同步服务器异常: ${e.message}")
                }
            }

            // VDOT级联重算
            try {
                vdotRecalculationService.onInclusiveLevelChanged(record.workoutId, newLevel)
            } catch (e: Exception) {
                RLog.w(TAG, "VDOT级联重算失败: ${e.message}")
            }

            // 刷新列表
            loadRecords()
        }
    }

    private fun loadRecords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val records = repository.getByDatasource(platform.code)
                _uiState.update { it.copy(records = records, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun getCachedTrackPoints(workoutId: String): List<TrackPoint>? {
        trackPointsCache[workoutId]?.let { return it }
        if (loadingTrackPoints.contains(workoutId)) {
            return null
        }
        loadTrackPoints(workoutId)
        return null
    }

    fun isTrackPointsLoading(workoutId: String): Boolean {
        return loadingTrackPoints.contains(workoutId)
    }

    private fun loadTrackPoints(workoutId: String) {
        if (!loadingTrackPoints.add(workoutId)) {
            return
        }
        viewModelScope.launch {
            try {
                val trackPoints = repository.getTrackPoints(workoutId)
                trackPointsCache[workoutId] = trackPoints
                _trackPointsVersion.value++
            } catch (e: Exception) {
                trackPointsCache[workoutId] = emptyList()
                _trackPointsVersion.value++
            } finally {
                loadingTrackPoints.remove(workoutId)
            }
        }
    }
}
