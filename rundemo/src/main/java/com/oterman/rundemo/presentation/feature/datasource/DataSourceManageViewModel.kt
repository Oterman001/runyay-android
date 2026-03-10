package com.oterman.rundemo.presentation.feature.datasource

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.fit.FitImportService
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.presentation.feature.home.FitImportResult
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

                // 再从服务器刷新状态
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
        val currentOrder = _uiState.value.sortableDataSources
            .mapIndexed { index, info -> info.platform.code to (index + 1) }
            .toMap()

        repository.saveDataSourceOrder(currentOrder)
        _uiState.update { it.copy(isEditingOrder = false) }

        RLog.d(TAG, "排序已保存: $currentOrder")
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

    // ==================== 手动导入 ====================

    /**
     * 显示手动导入选择弹窗
     */
    fun showManualImportDialog() {
        _uiState.update { it.copy(showManualImportDialog = true) }
    }

    /**
     * 关闭手动导入选择弹窗
     */
    fun dismissManualImportDialog() {
        _uiState.update { it.copy(showManualImportDialog = false) }
    }

    /**
     * 导入单个FIT文件
     */
    fun importSingleFitFile(uri: Uri) {
        RLog.i(TAG, "开始导入单个FIT文件: $uri")

        viewModelScope.launch {
            _uiState.update { it.copy(isImportingFit = true, importProgress = "导入中...") }

            val result = fitImportService.importFitFile(uri)

            _uiState.update {
                it.copy(
                    isImportingFit = false,
                    importProgress = null,
                    showImportResultDialog = true,
                    fitImportResult = result
                )
            }
        }
    }

    /**
     * 批量导入FIT文件
     */
    fun importBatchFitFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        RLog.i(TAG, "开始批量导入FIT文件: ${uris.size}个")

        viewModelScope.launch {
            _uiState.update { it.copy(isImportingFit = true, importProgress = "准备导入 ${uris.size} 个文件...") }

            var successCount = 0
            var failCount = 0
            var skipCount = 0

            uris.forEachIndexed { index, uri ->
                _uiState.update {
                    it.copy(importProgress = "正在导入第 ${index + 1}/${uris.size} 个文件...")
                }

                val result = fitImportService.importFitFile(uri)
                when (result) {
                    is FitImportResult.Success -> successCount++
                    is FitImportResult.AlreadyExists -> skipCount++
                    is FitImportResult.ConflictFound -> {
                        // 批量导入时自动强制导入冲突文件
                        val forceResult = fitImportService.importFitFile(uri, forceImport = true)
                        if (forceResult is FitImportResult.Success) successCount++ else failCount++
                    }
                    else -> failCount++
                }
            }

            val message = buildString {
                append("批量导入完成\n")
                if (successCount > 0) append("成功：${successCount} 个\n")
                if (skipCount > 0) append("已存在跳过：${skipCount} 个\n")
                if (failCount > 0) append("失败：${failCount} 个")
            }.trimEnd()

            _uiState.update {
                it.copy(
                    isImportingFit = false,
                    importProgress = null,
                    showImportResultDialog = true,
                    fitImportResult = if (successCount > 0) {
                        FitImportResult.Success(0.0, 0.0)
                    } else if (failCount > 0) {
                        FitImportResult.Error(message)
                    } else {
                        FitImportResult.AlreadyExists
                    }
                )
            }

            RLog.i(TAG, "批量导入完成: 成功=$successCount, 跳过=$skipCount, 失败=$failCount")
        }
    }

    /**
     * 关闭导入结果弹窗
     */
    fun dismissImportResultDialog() {
        _uiState.update { it.copy(showImportResultDialog = false, fitImportResult = null) }
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
