package com.oterman.rundemo.presentation.feature.syncstatus

import com.oterman.rundemo.service.sync.model.UnifiedSyncResult

/**
 * 同步状态类型
 */
enum class SyncStatusType {
    PREPARING,
    SYNCING,
    COMPLETED
}

/**
 * 导入记录项
 */
data class ImportedRecordItem(
    val originId: String,
    val platformName: String,
    val displayText: String
)

/**
 * 数据同步状态页面UI状态
 */
data class DataSyncStatusUiState(
    val syncStatus: SyncStatusType = SyncStatusType.PREPARING,
    val importedRecords: List<ImportedRecordItem> = emptyList(),
    val syncResult: UnifiedSyncResult? = null
)
