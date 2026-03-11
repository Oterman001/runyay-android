package com.oterman.rundemo.presentation.feature.datasource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.fit.FitImportService
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 数据源管理ViewModel
 */
class DataSourceManageViewModel(
    private val repository: DataSourceRepository,
    private val fitImportService: FitImportService
) : ViewModel() {

    companion object {
        private const val TAG = "DataSourceManageVM"
    }

    private val _uiState = MutableStateFlow(DataSourceManageUiState())
    val uiState: StateFlow<DataSourceManageUiState> = _uiState.asStateFlow()

    init {
        loadDataSources()
    }

    /**
     * 加载数据源列表
     */
    fun loadDataSources() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // 先加载本地缓存
                val localDataSources = repository.getAllDataSourceInfos()
                _uiState.update { it.copy(dataSources = localDataSources) }

                // 从服务端查询数据源优先级配置，同步到本地
                repository.queryDatasourceConfigFromServer()
                    .onSuccess {
                        RLog.d(TAG, "服务端数据源配置已同步")
                        val updatedDataSources = repository.getAllDataSourceInfos()
                        _uiState.update { it.copy(dataSources = updatedDataSources) }
                    }
                    .onFailure { error ->
                        RLog.e(TAG, "查询服务端数据源配置失败，使用本地配置", error)
                    }

                // 再从服务器刷新绑定状态
                repository.queryPlatformStatus()
                    .onSuccess {
                        // 重新加载数据源（包含最新的授权状态）
                        val updatedDataSources = repository.getAllDataSourceInfos()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                dataSources = updatedDataSources
                            )
                        }
                    }
                    .onFailure { error ->
                        RLog.e(TAG, "刷新平台状态失败", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message
                            )
                        }
                    }
            } catch (e: Exception) {
                RLog.e(TAG, "加载数据源失败", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    /**
     * 开始编辑排序
     */
    fun startEditingOrder() {
        _uiState.update { it.copy(isEditingOrder = true) }
    }

    /**
     * 取消编辑排序
     */
    fun cancelEditingOrder() {
        _uiState.update { it.copy(isEditingOrder = false) }
        // 重新加载以恢复原有排序
        loadDataSources()
    }

    /**
     * 保存排序
     */
    fun saveOrder() {
        val sortableList = _uiState.value.sortableDataSources
        val currentOrder = sortableList
            .mapIndexed { index, info -> info.platform.code to (index + 1) }
            .toMap()

        // 先本地保存（立即生效）
        repository.saveDataSourceOrder(currentOrder)
        _uiState.update { it.copy(isEditingOrder = false, isSaving = true) }

        RLog.d(TAG, "排序已本地保存: $currentOrder")

        // 异步同步到服务端
        val platformCodes = sortableList.joinToString(",") { it.platform.code }
        viewModelScope.launch {
            repository.saveDatasourceConfigToServer(platformCodes)
                .onSuccess {
                    RLog.d(TAG, "排序已同步到服务端")
                }
                .onFailure { error ->
                    RLog.e(TAG, "排序同步到服务端失败", error)
                }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    /**
     * 移动数据源位置
     */
    fun moveDataSource(fromIndex: Int, toIndex: Int) {
        val currentList = _uiState.value.sortableDataSources.toMutableList()

        if (fromIndex < 0 || fromIndex >= currentList.size ||
            toIndex < 0 || toIndex >= currentList.size) {
            return
        }

        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)

        // 更新优先级
        val updatedList = currentList.mapIndexed { index, info ->
            info.copy(priority = index + 1)
        }

        // 合并回完整列表
        val nonSortable = _uiState.value.dataSources.filter { !it.platform.supportsSorting }
        _uiState.update {
            it.copy(dataSources = updatedList + nonSortable)
        }
    }

    /**
     * 检查是否是即将支持的数据源
     */
    fun isComingSoonDataSource(platform: DataSourcePlatform): Boolean {
        return platform == DataSourcePlatform.HUAWEI
    }

    /**
     * 检查是否是手动导入数据源
     */
    fun isManualImportDataSource(platform: DataSourcePlatform): Boolean {
        return platform == DataSourcePlatform.MANUAL
    }

    /**
     * 显示即将支持弹窗
     */
    fun showComingSoonDialog() {
        _uiState.update { it.copy(showComingSoonDialog = true) }
    }

    /**
     * 关闭即将支持弹窗
     */
    fun dismissComingSoonDialog() {
        _uiState.update { it.copy(showComingSoonDialog = false) }
    }

    /**
     * 显示需要登录弹窗
     */
    fun showLoginRequiredDialog() {
        _uiState.update { it.copy(showLoginRequiredDialog = true) }
    }

    /**
     * 关闭需要登录弹窗
     */
    fun dismissLoginRequiredDialog() {
        _uiState.update { it.copy(showLoginRequiredDialog = false) }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 刷新数据源列表
     * 用于从详情页返回时刷新授权状态
     */
    fun refreshDataSources() {
        loadDataSources()
    }
}
