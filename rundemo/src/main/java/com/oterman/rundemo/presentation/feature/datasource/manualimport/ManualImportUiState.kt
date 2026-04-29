package com.oterman.rundemo.presentation.feature.datasource.manualimport

import com.oterman.rundemo.data.local.entity.RunRecordEntity

data class ManualImportUiState(
    val records: List<RunRecordEntity> = emptyList(),
    val isLoadingRecords: Boolean = true,
    val isImporting: Boolean = false,
    val importProgress: String? = null,
    val importResult: ManualImportResult? = null,
    val isGpxEnabled: Boolean = false
)

sealed class ManualImportResult {
    data class SingleSuccess(
        val workoutId: String,
        val distance: Double,
        val duration: Double
    ) : ManualImportResult()

    object SingleAlreadyExists : ManualImportResult()

    data class SingleError(val message: String) : ManualImportResult()

    data class BatchComplete(
        val successCount: Int,
        val skipCount: Int,
        val failures: List<Pair<String, String>>
    ) : ManualImportResult()
}
