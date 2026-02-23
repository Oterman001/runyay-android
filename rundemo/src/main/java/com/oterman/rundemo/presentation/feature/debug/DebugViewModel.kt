package com.oterman.rundemo.presentation.feature.debug

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.domain.trajectory.TrajectoryThumbnailManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 调试页面UI状态
 */
data class DebugUiState(
    val isClearingThumbnailCache: Boolean = false,
    val assetFiles: List<String> = emptyList()
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

    init {
        loadAssetFiles()
    }

    private fun loadAssetFiles() {
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) {
                listAssetsRecursively(context.assets, "")
            }
            _uiState.update { it.copy(assetFiles = files) }
        }
    }

    private fun listAssetsRecursively(
        assetManager: android.content.res.AssetManager,
        path: String
    ): List<String> {
        val result = mutableListOf<String>()
        val children = assetManager.list(path) ?: return result
        for (child in children) {
            val fullPath = if (path.isEmpty()) child else "$path/$child"
            val subChildren = assetManager.list(fullPath)
            if (subChildren != null && subChildren.isNotEmpty()) {
                result.addAll(listAssetsRecursively(assetManager, fullPath))
            } else {
                result.add(fullPath)
            }
        }
        return result
    }

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

    /**
     * 将assets文件保存到用户选择的目标URI
     */
    fun saveAssetToUri(fileName: String, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    context.assets.open(fileName).use { input ->
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            onComplete(success)
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
