package com.oterman.rundemo.presentation.feature.datasource

import com.oterman.rundemo.domain.model.DataSourceInfo
import com.oterman.rundemo.domain.model.DataSourcePlatform

/**
 * 数据源管理页面UI状态
 */
data class DataSourceManageUiState(
    val isLoading: Boolean = false,
    val isEditingOrder: Boolean = false,
    val dataSources: List<DataSourceInfo> = emptyList(),
    val error: String? = null,
    val showLoginRequiredDialog: Boolean = false,
    val showComingSoonDialog: Boolean = false
) {
    /**
     * 获取用于显示的数据源列表（支持排序的在前，不支持的在后）
     */
    val displayDataSources: List<DataSourceInfo>
        get() {
            val sortable = dataSources.filter { it.platform.supportsSorting }
                .sortedBy { it.priority }
            val nonSortable = dataSources.filter { !it.platform.supportsSorting }
            return sortable + nonSortable
        }

    /**
     * 获取可排序的数据源列表
     */
    val sortableDataSources: List<DataSourceInfo>
        get() = dataSources.filter { it.platform.supportsSorting }
            .sortedBy { it.priority }
}
