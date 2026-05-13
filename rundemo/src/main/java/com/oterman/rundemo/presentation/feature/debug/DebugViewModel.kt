package com.oterman.rundemo.presentation.feature.debug

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.DataSourcePreferences
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
    val assetFiles: List<String> = emptyList(),
    val isGpxImportEnabled: Boolean = false
)

/**
 * 调试页面ViewModel
 */
class DebugViewModel(
    private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "AssetDebug"
    }

    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()

    private val thumbnailManager = TrajectoryThumbnailManager.getInstance(context)
    private val dataSourcePreferences = DataSourcePreferences(context)

    init {
        loadAssetFiles()
        _uiState.update { it.copy(isGpxImportEnabled = dataSourcePreferences.isGpxImportEnabled()) }
    }

    private fun loadAssetFiles() {
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) {
                listAssetsRecursively(context.assets, "")
            }
            android.util.Log.d(TAG, "loadAssetFiles: found ${files.size} files → $files")
            _uiState.update { it.copy(assetFiles = files) }
        }
    }

    private fun listAssetsRecursively(
        assetManager: android.content.res.AssetManager,
        path: String
    ): List<String> {
        val result = mutableListOf<String>()
        val children = assetManager.list(path)
        android.util.Log.d(TAG, "list(\"$path\") → ${children?.size ?: "null"} items: ${children?.toList()}")
        children ?: return result
        for (child in children) {
            val fullPath = if (path.isEmpty()) child else "$path/$child"
            // AssetManager.list() peeks inside ZIP archives and returns their entries,
            // making the ZIP look like a directory. Treat known archive types as leaf files.
            val isArchive = child.endsWith(".zip", ignoreCase = true)
                    || child.endsWith(".jar", ignoreCase = true)
            val subChildren = if (isArchive) null else assetManager.list(fullPath)
            android.util.Log.d(TAG, "  child=\"$child\" isArchive=$isArchive subChildren=${subChildren?.size ?: "null"}")
            if (subChildren != null && subChildren.isNotEmpty()) {
                result.addAll(listAssetsRecursively(assetManager, fullPath))
            } else {
                android.util.Log.d(TAG, "  → leaf file: $fullPath")
                result.add(fullPath)
            }
        }
        return result
    }

    fun setGpxImportEnabled(enabled: Boolean) {
        dataSourcePreferences.setGpxImportEnabled(enabled)
        _uiState.update { it.copy(isGpxImportEnabled = enabled) }
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
