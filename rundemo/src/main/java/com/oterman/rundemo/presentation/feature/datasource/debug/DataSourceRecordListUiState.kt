package com.oterman.rundemo.presentation.feature.datasource.debug

import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.domain.model.DataSourcePlatform

/**
 * 数据源记录列表界面状态
 */
data class DataSourceRecordListUiState(
    val platform: DataSourcePlatform = DataSourcePlatform.GARMIN_CHINA,
    val records: List<RunRecordEntity> = emptyList(),
    val selectedIds: Set<String> = emptySet(),  // workoutId集合
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val message: String? = null
)
