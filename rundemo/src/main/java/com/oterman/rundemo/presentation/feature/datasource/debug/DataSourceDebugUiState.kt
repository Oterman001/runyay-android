package com.oterman.rundemo.presentation.feature.datasource.debug

import com.oterman.rundemo.domain.model.DataSourcePlatform

/**
 * 数据源调试界面状态
 */
data class DataSourceDebugUiState(
    val platform: DataSourcePlatform = DataSourcePlatform.GARMIN_CHINA,
    val lastSyncTimestamp: String = "",
    val recordCount: Int = 0,
    val isLoading: Boolean = false,
    val message: String? = null
)
