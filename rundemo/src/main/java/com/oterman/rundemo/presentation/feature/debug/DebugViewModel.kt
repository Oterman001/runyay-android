package com.oterman.rundemo.presentation.feature.debug

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.domain.trajectory.TrajectoryThumbnailManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 调试页面UI状态
 */
data class DebugUiState(
    val isClearingThumbnailCache: Boolean = false
)

/**
 * 调试页面ViewModel
 */
class DebugViewModel(
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()

    private val thumbnailManager = TrajectoryThumbnailManager.getInstance(context)

    /**
     * 清除轨迹缩略图缓存
     */
    fun clearThumbnailCache(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingThumbnailCache = true) }
            try {
                thumbnailManager.clearAll()
            } finally {
                _uiState.update { it.copy(isClearingThumbnailCache = false) }
                onComplete()
            }
        }
    }
}

/**
 * DebugViewModel工厂
 */
class DebugViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebugViewModel::class.java)) {
            return DebugViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
