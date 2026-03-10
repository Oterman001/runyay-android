package com.oterman.rundemo.presentation.feature.datasource.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.TrackPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class PlatformRecordListUiState(
    val platform: DataSourcePlatform,
    val records: List<RunRecordEntity> = emptyList(),
    val isLoading: Boolean = true
)

class PlatformRecordListViewModel(
    private val platform: DataSourcePlatform,
    private val repository: RunDataRepository
) : ViewModel() {

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
