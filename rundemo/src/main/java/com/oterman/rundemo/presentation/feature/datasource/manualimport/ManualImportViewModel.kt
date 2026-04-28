package com.oterman.rundemo.presentation.feature.datasource.manualimport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.fit.FitImportService
import com.oterman.rundemo.data.fit.ZipFitExtractor
import com.oterman.rundemo.data.gpx.GpxImportService
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.presentation.feature.home.FitImportResult
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManualImportViewModel(
    private val context: Context,
    private val repository: RunDataRepository,
    private val fitImportService: FitImportService,
    private val gpxImportService: GpxImportService
) : ViewModel() {

    companion object {
        private const val TAG = "ManualImportVM"
    }

    private val _uiState = MutableStateFlow(ManualImportUiState())
    val uiState: StateFlow<ManualImportUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRecords = true) }
            try {
                val records = repository.getByDatasource(DataSourcePlatform.MANUAL.code)
                _uiState.update { it.copy(records = records, isLoadingRecords = false) }
            } catch (e: Exception) {
                RLog.e(TAG, "加载手动导入记录失败", e)
                _uiState.update { it.copy(isLoadingRecords = false) }
            }
        }
    }

    fun importFitFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        RLog.i(TAG, "开始导入 ${uris.size} 个文件（含压缩包）")

        viewModelScope.launch {
            _uiState.update {
                it.copy(isImporting = true, importProgress = "准备解析 ${uris.size} 个文件...")
            }

            // 预处理：将压缩包解压为临时 FIT 文件，收集所有待导入的 FIT URI
            val expandedFitUris = mutableListOf<Uri>()
            val expandFailures = mutableListOf<Pair<String, String>>()

            withContext(Dispatchers.IO) {
                uris.forEachIndexed { index, uri ->
                    val srcName = getFileName(uri) ?: "文件${index + 1}"
                    try {
                        when {
                            ZipFitExtractor.isZip(context, uri) -> {
                                _uiState.update { it.copy(importProgress = "正在解压 $srcName...") }
                                val files = ZipFitExtractor.extractZip(context, uri)
                                expandedFitUris += files.map { it.toUri() }
                                RLog.i(TAG, "ZIP $srcName 解压出 ${files.size} 个 FIT 文件")
                            }
                            ZipFitExtractor.isGz(context, uri) -> {
                                _uiState.update { it.copy(importProgress = "正在解压 $srcName...") }
                                val file = ZipFitExtractor.extractGz(context, uri)
                                expandedFitUris += file.toUri()
                                RLog.i(TAG, "GZ $srcName 解压完成")
                            }
                            else -> {
                                expandedFitUris += uri
                            }
                        }
                    } catch (e: Exception) {
                        RLog.e(TAG, "解压 $srcName 失败: ${e.message}")
                        expandFailures.add(srcName to (e.message ?: "解压失败"))
                    }
                }
            }

            val allFitUris = expandedFitUris

            if (allFitUris.isEmpty()) {
                // 所有文件都解压失败或无内容
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importProgress = null,
                        importResult = ManualImportResult.BatchComplete(
                            successCount = 0,
                            skipCount = 0,
                            failures = expandFailures
                        )
                    )
                }
                return@launch
            }

            // 实际导入所有展开后的 FIT 文件
            if (allFitUris.size == 1 && expandFailures.isEmpty()) {
                val uri = allFitUris[0]
                _uiState.update { it.copy(importProgress = "正在导入文件...") }
                val result = fitImportService.importFitFile(uri)
                val importResult = when (result) {
                    is FitImportResult.Success -> {
                        ManualImportResult.SingleSuccess(
                            workoutId = result.workoutId ?: "",
                            distance = result.distance,
                            duration = result.duration
                        )
                    }
                    is FitImportResult.AlreadyExists -> ManualImportResult.SingleAlreadyExists
                    is FitImportResult.ConflictFound -> {
                        val forceResult = fitImportService.importFitFile(uri, forceImport = true)
                        if (forceResult is FitImportResult.Success) {
                            ManualImportResult.SingleSuccess(
                                workoutId = forceResult.workoutId ?: "",
                                distance = forceResult.distance,
                                duration = forceResult.duration
                            )
                        } else {
                            ManualImportResult.SingleError("导入失败（时间冲突）")
                        }
                    }
                    is FitImportResult.Error -> ManualImportResult.SingleError(result.message)
                    is FitImportResult.UploadFailed -> ManualImportResult.SingleError(result.message)
                }
                _uiState.update {
                    it.copy(isImporting = false, importProgress = null, importResult = importResult)
                }
            } else {
                var successCount = 0
                var skipCount = 0
                val failures = mutableListOf<Pair<String, String>>()
                failures += expandFailures

                allFitUris.forEachIndexed { index, uri ->
                    val fileName = getFileName(uri) ?: uri.lastPathSegment ?: "文件${index + 1}"
                    _uiState.update {
                        it.copy(importProgress = "正在导入第 ${index + 1}/${allFitUris.size} 个文件...")
                    }
                    val result = fitImportService.importFitFile(uri)
                    when (result) {
                        is FitImportResult.Success -> successCount++
                        is FitImportResult.AlreadyExists -> skipCount++
                        is FitImportResult.ConflictFound -> {
                            val forceResult = fitImportService.importFitFile(uri, forceImport = true)
                            if (forceResult is FitImportResult.Success) successCount++
                            else failures.add(fileName to "时间冲突且强制导入失败")
                        }
                        is FitImportResult.Error -> failures.add(fileName to result.message)
                        is FitImportResult.UploadFailed -> failures.add(fileName to result.message)
                    }
                }

                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importProgress = null,
                        importResult = ManualImportResult.BatchComplete(
                            successCount = successCount,
                            skipCount = skipCount,
                            failures = failures
                        )
                    )
                }
            }

            // 导入完毕后清理临时解压文件
            withContext(Dispatchers.IO) {
                ZipFitExtractor.cleanTempDir(context)
            }

            loadRecords()
        }
    }

    fun importGpxFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        RLog.i(TAG, "开始导入 ${uris.size} 个 GPX 文件")

        viewModelScope.launch {
            _uiState.update {
                it.copy(isImporting = true, importProgress = "准备导入 ${uris.size} 个 GPX 文件...")
            }

            if (uris.size == 1) {
                val uri = uris[0]
                _uiState.update { it.copy(importProgress = "正在解析 GPX 文件...") }
                val result = gpxImportService.importGpxFile(uri)
                val importResult = when (result) {
                    is FitImportResult.Success -> ManualImportResult.SingleSuccess(
                        workoutId = result.workoutId ?: "",
                        distance = result.distance,
                        duration = result.duration
                    )
                    is FitImportResult.AlreadyExists -> ManualImportResult.SingleAlreadyExists
                    is FitImportResult.ConflictFound -> {
                        val forceResult = gpxImportService.importGpxFile(uri, forceImport = true)
                        if (forceResult is FitImportResult.Success) {
                            ManualImportResult.SingleSuccess(
                                workoutId = forceResult.workoutId ?: "",
                                distance = forceResult.distance,
                                duration = forceResult.duration
                            )
                        } else {
                            ManualImportResult.SingleError("导入失败（时间冲突）")
                        }
                    }
                    is FitImportResult.Error -> ManualImportResult.SingleError(result.message)
                    is FitImportResult.UploadFailed -> ManualImportResult.SingleError(result.message)
                }
                _uiState.update {
                    it.copy(isImporting = false, importProgress = null, importResult = importResult)
                }
            } else {
                var successCount = 0
                var skipCount = 0
                val failures = mutableListOf<Pair<String, String>>()

                uris.forEachIndexed { index, uri ->
                    val fileName = getFileName(uri) ?: uri.lastPathSegment ?: "文件${index + 1}"
                    _uiState.update {
                        it.copy(importProgress = "正在导入第 ${index + 1}/${uris.size} 个 GPX 文件...")
                    }
                    when (val result = gpxImportService.importGpxFile(uri)) {
                        is FitImportResult.Success -> successCount++
                        is FitImportResult.AlreadyExists -> skipCount++
                        is FitImportResult.ConflictFound -> {
                            val forceResult = gpxImportService.importGpxFile(uri, forceImport = true)
                            if (forceResult is FitImportResult.Success) successCount++
                            else failures.add(fileName to "时间冲突且强制导入失败")
                        }
                        is FitImportResult.Error -> failures.add(fileName to result.message)
                        is FitImportResult.UploadFailed -> failures.add(fileName to result.message)
                    }
                }

                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importProgress = null,
                        importResult = ManualImportResult.BatchComplete(
                            successCount = successCount,
                            skipCount = skipCount,
                            failures = failures
                        )
                    )
                }
            }

            loadRecords()
        }
    }

    fun dismissResult() {
        _uiState.update { it.copy(importResult = null) }
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            RLog.e(TAG, "获取文件名失败", e)
            null
        }
    }
}
