package com.oterman.rundemo.presentation.feature.rundetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.fit.RunSummaryMapper
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.local.entity.RunAbilityZoneEntity
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.local.entity.RunSamplePointEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import com.oterman.rundemo.data.repository.RunDataRemoteRepository
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 手动上传操作状态
 */
sealed class UploadActionState {
    object Idle : UploadActionState()
    object Loading : UploadActionState()
    object Success : UploadActionState()
    data class Error(val message: String) : UploadActionState()
}

/**
 * 详情页UI状态
 */
sealed class RunDetailDebugUiState {
    object Loading : RunDetailDebugUiState()
    data class Success(val data: RunDetailFullData) : RunDetailDebugUiState()
    data class Error(val message: String) : RunDetailDebugUiState()
}

/**
 * 完整的跑步详情数据
 */
data class RunDetailFullData(
    val record: RunRecordEntity,
    val segments: List<RunSegmentEntity>,
    val zones: List<RunAbilityZoneEntity>,
    val samplePoints: List<RunSamplePointEntity>,
    val trackPointCount: Int,
    val samplePointCount: Int
)

/**
 * 跑步记录调试详情页ViewModel
 */
class RunDetailDebugViewModel(
    private val workoutId: String,
    private val repository: RunDataRepository,
    private val remoteRepository: RunDataRemoteRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<RunDetailDebugUiState>(RunDetailDebugUiState.Loading)
    val uiState: StateFlow<RunDetailDebugUiState> = _uiState.asStateFlow()

    private val _uploadActionState = MutableStateFlow<UploadActionState>(UploadActionState.Idle)
    val uploadActionState: StateFlow<UploadActionState> = _uploadActionState.asStateFlow()
    
    init {
        loadData()
    }
    
    /**
     * 加载完整的跑步数据
     */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = RunDetailDebugUiState.Loading
            
            try {
                val detailData = repository.getRunDetail(workoutId)
                if (detailData == null) {
                    _uiState.value = RunDetailDebugUiState.Error("未找到该跑步记录")
                    return@launch
                }
                
                // 加载采样点数据
                val samplePoints = repository.getSamplePoints(workoutId)
                
                _uiState.value = RunDetailDebugUiState.Success(
                    RunDetailFullData(
                        record = detailData.record,
                        segments = detailData.segments,
                        zones = detailData.zones,
                        samplePoints = samplePoints,
                        trackPointCount = detailData.trackPointCount,
                        samplePointCount = detailData.samplePointCount
                    )
                )
            } catch (e: Exception) {
                _uiState.value = RunDetailDebugUiState.Error(e.message ?: "加载失败")
            }
        }
    }
    
    /**
     * 手动触发上传
     */
    fun uploadRecord(record: RunRecordEntity) {
        viewModelScope.launch {
            _uploadActionState.value = UploadActionState.Loading
            repository.updateUploadStatus(record.workoutId, 1)
            try {
                val settings = preferencesManager.getHearRateZoneSettings()
                val dto = RunSummaryMapper.toUploadItemDto(record, settings)
                val result = remoteRepository.uploadRunRecords(listOf(dto))
                if (result.isSuccess) {
                    repository.updateUploadStatus(record.workoutId, 2)
                    _uploadActionState.value = UploadActionState.Success
                    refresh()
                } else {
                    repository.updateUploadStatus(record.workoutId, 3)
                    _uploadActionState.value = UploadActionState.Error(
                        result.exceptionOrNull()?.message ?: "上传失败"
                    )
                }
            } catch (e: Exception) {
                repository.updateUploadStatus(record.workoutId, 3)
                _uploadActionState.value = UploadActionState.Error(e.message ?: "上传异常")
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadData()
    }
}

/**
 * RunDetailDebugViewModel Factory
 */
class RunDetailDebugViewModelFactory(
    private val context: Context,
    private val workoutId: String
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunDetailDebugViewModel::class.java)) {
            val database = RunDatabase.getInstance(context)
            val repository = RunDataRepositoryImpl.getInstance(database)
            val preferencesManager = PreferencesManager(context)
            val remoteRepository = RunDataRemoteRepository(preferencesManager)
            return RunDetailDebugViewModel(workoutId, repository, remoteRepository, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

