package com.oterman.rundemo.presentation.feature.debug.synccontrol

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.service.sync.UnifiedDataSyncManager
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 同步控制平台信息
 */
data class SyncControlPlatformInfo(
    val platform: DataSourcePlatform,
    val syncEnabled: Boolean,
    val recordCount: Int,
    val lastSyncTime: String
)

/**
 * 同步控制页面UI状态
 */
data class SyncControlUiState(
    val isLoading: Boolean = false,
    val isEditingOrder: Boolean = false,
    val platforms: List<SyncControlPlatformInfo> = emptyList(),
    val showResetDialog: DataSourcePlatform? = null,
    val isResetting: Boolean = false,
    val message: String? = null
) {
    val sortablePlatforms: List<SyncControlPlatformInfo>
        get() = platforms.filter { it.platform.supportsSorting }
}

/**
 * 同步控制ViewModel
 */
class SyncControlViewModel(
    private val context: Context,
    private val dataSourcePreferences: DataSourcePreferences,
    private val runDataRepository: RunDataRepository,
    private val syncManager: UnifiedDataSyncManager
) : ViewModel() {

    companion object {
        private const val TAG = "SyncControlVM"
    }

    private val _uiState = MutableStateFlow(SyncControlUiState())
    val uiState: StateFlow<SyncControlUiState> = _uiState.asStateFlow()

    init {
        loadPlatforms()
    }

    fun loadPlatforms() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val savedOrder = dataSourcePreferences.getDataSourceOrder()
                val platforms = DataSourcePlatform.getSortablePlatforms()
                    .filter { it.isEnabled }
                    .sortedBy { savedOrder[it.code] ?: Int.MAX_VALUE }
                    .map { platform ->
                        val records = withContext(Dispatchers.IO) {
                            runDataRepository.getByDatasource(platform.code)
                        }
                        SyncControlPlatformInfo(
                            platform = platform,
                            syncEnabled = dataSourcePreferences.isDebugSyncEnabled(platform),
                            recordCount = records.size,
                            lastSyncTime = dataSourcePreferences.getLastSyncTime(platform)
                        )
                    }
                _uiState.update { it.copy(isLoading = false, platforms = platforms) }
            } catch (e: Exception) {
                RLog.e(TAG, "加载平台信息失败", e)
                _uiState.update { it.copy(isLoading = false, message = "加载失败: ${e.message}") }
            }
        }
    }

    // ============ 排序 ============

    fun startEditingOrder() {
        _uiState.update { it.copy(isEditingOrder = true) }
    }

    fun cancelEditingOrder() {
        _uiState.update { it.copy(isEditingOrder = false) }
        loadPlatforms()
    }

    fun saveOrder() {
        val currentOrder = _uiState.value.sortablePlatforms
            .mapIndexed { index, info -> info.platform.code to (index + 1) }
            .toMap()
        dataSourcePreferences.saveDataSourceOrder(currentOrder)
        _uiState.update { it.copy(isEditingOrder = false, message = "排序已保存") }
        RLog.d(TAG, "排序已保存: $currentOrder")
    }

    fun movePlatform(fromIndex: Int, toIndex: Int) {
        val currentList = _uiState.value.sortablePlatforms.toMutableList()
        if (fromIndex < 0 || fromIndex >= currentList.size ||
            toIndex < 0 || toIndex >= currentList.size) return

        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        _uiState.update { it.copy(platforms = currentList) }
    }

    // ============ 同步开关 ============

    fun toggleSyncEnabled(platform: DataSourcePlatform, enabled: Boolean) {
        dataSourcePreferences.setDebugSyncEnabled(platform, enabled)
        _uiState.update { state ->
            state.copy(
                platforms = state.platforms.map {
                    if (it.platform == platform) it.copy(syncEnabled = enabled) else it
                }
            )
        }
        RLog.d(TAG, "${platform.displayName} 同步${if (enabled) "启用" else "禁用"}")
    }

    // ============ 数据重置 ============

    fun showResetDialog(platform: DataSourcePlatform) {
        _uiState.update { it.copy(showResetDialog = platform) }
    }

    fun dismissResetDialog() {
        _uiState.update { it.copy(showResetDialog = null) }
    }

    fun confirmReset(platform: DataSourcePlatform) {
        _uiState.update { it.copy(showResetDialog = null, isResetting = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 1. 获取该平台所有记录
                    val records = runDataRepository.getByDatasource(platform.code)
                    val workoutIds = records.map { it.workoutId }

                    // 2. 批量删除本地记录
                    if (workoutIds.isNotEmpty()) {
                        runDataRepository.deleteRunRecords(workoutIds)
                    }

                    // 3. 重置同步时间戳
                    dataSourcePreferences.clearSyncTime(platform)

                    // 4. 清除统一同步服务的时间戳
                    syncManager.clearSyncTimestamp(platform)
                }

                RLog.i(TAG, "${platform.displayName} 数据已重置")
                _uiState.update { it.copy(isResetting = false, message = "${platform.displayName} 数据已重置") }
                loadPlatforms()
            } catch (e: Exception) {
                RLog.e(TAG, "重置${platform.displayName}数据失败", e)
                _uiState.update { it.copy(isResetting = false, message = "重置失败: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

/**
 * SyncControlViewModel工厂
 */
class SyncControlViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SyncControlViewModel::class.java)) {
            val appContext = context.applicationContext
            val dataSourcePreferences = DataSourcePreferences(appContext)
            val database = RunDatabase.getInstance(appContext)
            val runDataRepository = RunDataRepositoryImpl.getInstance(database)
            val syncManager = UnifiedDataSyncManager.getInstance(appContext)
            return SyncControlViewModel(
                context = appContext,
                dataSourcePreferences = dataSourcePreferences,
                runDataRepository = runDataRepository,
                syncManager = syncManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
