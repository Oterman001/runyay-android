package com.oterman.rundemo.presentation.feature.datasource

import com.oterman.rundemo.domain.model.DataSourceInfo
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.ImportedRunSummary

/**
 * 数据源详情页面UI状态
 */
data class DataSourceDetailUiState(
    val platform: DataSourcePlatform = DataSourcePlatform.GARMIN_CHINA,
    val dataSourceInfo: DataSourceInfo? = null,
    val isAuthorized: Boolean = false,
    val isLoading: Boolean = false,
    val isUnbinding: Boolean = false,
    val isSyncing: Boolean = false,
    val authUrl: String? = null,
    val showOAuthWebView: Boolean = false,
    val error: String? = null,
    val alertMessage: String? = null,
    val showUnbindConfirmDialog: Boolean = false,
    val showSyncOptionsDialog: Boolean = false,
    val importedRecords: List<ImportedRunSummary> = emptyList(),
    val isSyncFinished: Boolean = false
)

