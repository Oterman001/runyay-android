package com.oterman.rundemo.service.sync

import android.util.Log
import com.oterman.rundemo.data.fit.FitDataMapper
import com.oterman.rundemo.data.fit.FitFileParser
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.data.local.dao.RunSamplePointDao
import com.oterman.rundemo.data.local.dao.RunSegmentDao
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.FileInfo
import com.oterman.rundemo.domain.model.ImportedRunSummary
import com.oterman.rundemo.domain.model.SyncResult
import com.oterman.rundemo.domain.model.SyncTimeRange
import com.oterman.rundemo.service.sync.model.PageSyncResult
import com.oterman.rundemo.service.sync.model.SyncConstants
import com.oterman.rundemo.service.sync.model.TimestampTracker
import com.oterman.rundemo.util.Logger
import com.oterman.rundemo.util.TimestampUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

/**
 * 数据同步服务抽象基类
 * 使用模板方法模式，提供同步的通用流程
 * 子类只需实现平台特定的逻辑
 */
abstract class BaseDataSyncService(
    protected val dataSourceRepository: DataSourceRepository,
    protected val runRecordDao: RunRecordDao,
    protected val samplePointDao: RunSamplePointDao,
    protected val segmentDao: RunSegmentDao,
    protected val dataSourcePreferences: DataSourcePreferences
) : DataSyncService {

    companion object {
        private const val TAG = "BaseDataSyncService"
    }

    /** 同步状态标志 */
    private val isSyncing = AtomicBoolean(false)

    /** 同步任务 */
    protected var syncJob: Job? = null

    /** 时间戳追踪器 */
    protected val timestampTracker = TimestampTracker()

    // ============ 抽象属性（子类必须实现） ============

    /** 数据源平台 */
    abstract override val platform: DataSourcePlatform

    /** 日志标签 */
    protected abstract val logTag: String

    // ============ 抽象方法（子类必须实现） ============

    /**
     * 获取上次同步时间戳
     * @return 17位格式的时间戳
     */
    protected abstract fun getLastSyncTimestamp(): String

    /**
     * 更新同步时间戳
     * @param timestamp 17位格式的时间戳
     */
    protected abstract fun updateLastSyncTimestamp(timestamp: String)

    /**
     * 清除同步时间戳
     */
    protected abstract fun clearSyncTimestampInternal()

    /**
     * 获取待同步文件列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param lastSyncTime 上次同步时间（17位格式）
     * @return 文件列表
     */
    protected abstract suspend fun fetchFileList(
        pageNum: Int,
        pageSize: Int,
        lastSyncTime: String
    ): Result<List<FileInfo>>

    /**
     * 下载文件数据
     * @param fileInfo 文件信息
     * @return 文件字节数据
     */
    protected abstract suspend fun downloadFile(fileInfo: FileInfo): Result<ByteArray>

    // ============ 可选覆盖的方法 ============

    /**
     * 同步前的准备工作
     * 默认不做任何操作，子类可覆盖
     */
    protected open suspend fun onBeforeSync(timeRange: SyncTimeRange) {
        // 默认空实现
    }

    /**
     * 同步后的清理工作
     * 默认不做任何操作，子类可覆盖
     */
    protected open suspend fun onAfterSync(result: SyncResult) {
        // 默认空实现
    }

    /**
     * 处理下载的文件数据
     * 默认使用FIT解析，子类可覆盖（如高驰可能使用不同格式）
     */
    protected open suspend fun processFileData(
        fileInfo: FileInfo,
        fileData: ByteArray
    ): ImportedRunSummary? {
        return processAsFitFile(fileInfo, fileData)
    }

    // ============ DataSyncService接口实现 ============

    override fun isCurrentlySyncing(): Boolean = isSyncing.get()

    override fun executeSync(): Flow<SyncEvent> = executeSync(SyncTimeRange.ONE_MONTH)

    override fun executeSync(timeRange: SyncTimeRange): Flow<SyncEvent> = flow {
        if (!isSyncing.compareAndSet(false, true)) {
            emit(SyncEvent.Failed("正在同步中，请勿重复操作"))
            return@flow
        }

        try {
            // 重置时间戳追踪器
            timestampTracker.reset()

            emit(SyncEvent.Started)
           Logger.i(logTag, "开始${platform.displayName}数据同步，时间范围: ${timeRange.displayName}")

            // 同步前准备
            onBeforeSync(timeRange)

            // 执行分页同步
            val syncResult = executePaginatedSync(timeRange) { event ->
                emit(event)
            }

            // 更新同步时间戳
            updateSyncTimestampAfterSync()

            // 同步后清理
            onAfterSync(syncResult)

            emit(SyncEvent.Completed(syncResult))
           Logger.i(logTag, "${platform.displayName}数据同步完成，导入: ${syncResult.importedCount} 条记录")

        } catch (e: Exception) {
           Logger.e(logTag, "同步过程出错", e)
            emit(SyncEvent.Failed(e.message ?: "同步失败"))
        } finally {
            isSyncing.set(false)
        }
    }.flowOn(Dispatchers.IO)

    override fun cancelSync() {
        syncJob?.cancel()
        isSyncing.set(false)
       Logger.i(logTag, "同步已取消")
    }

    override fun clearSyncTimestamp() {
        clearSyncTimestampInternal()
       Logger.i(logTag, "同步时间戳已清除")
    }

    // ============ 核心同步逻辑 ============

    /**
     * 执行分页同步
     */
    private suspend fun executePaginatedSync(
        timeRange: SyncTimeRange,
        emitEvent: suspend (SyncEvent) -> Unit
    ): SyncResult {
        var totalImported = 0
        var pageNum = 1
        var hasMorePages = true
        val errors = mutableListOf<String>()
        val lastSyncTime = getLastSyncTimestamp()

       Logger.d(logTag, "开始分页同步，lastSyncTime=$lastSyncTime")

        while (hasMorePages && coroutineContext.isActive) {
           Logger.i(logTag, "正在获取第 $pageNum 页数据...")

            val pageResult = fetchAndProcessPage(pageNum, lastSyncTime, emitEvent)

            if (pageResult.success) {
                totalImported += pageResult.importedCount
                hasMorePages = pageResult.hasMorePages
                pageNum++

                // 更新时间戳追踪器
                pageResult.latestDataDate?.let { timestampTracker.recordSuccess(it) }
                pageResult.earliestFailedDataDate?.let { timestampTracker.recordFailure(it) }

                if (pageResult.errors.isNotEmpty()) {
                    errors.addAll(pageResult.errors)
                }
            } else {
                errors.addAll(pageResult.errors)
                hasMorePages = false
            }
        }

        return SyncResult(
            success = errors.isEmpty(),
            importedCount = totalImported,
            error = if (errors.isNotEmpty()) errors.joinToString("; ") else null,
            platform = platform
        )
    }

    /**
     * 获取并处理单页数据
     */
    private suspend fun fetchAndProcessPage(
        pageNum: Int,
        lastSyncTime: String,
        emitEvent: suspend (SyncEvent) -> Unit
    ): PageSyncResult {
        val result = fetchFileList(pageNum, SyncConstants.DEFAULT_PAGE_SIZE, lastSyncTime)

        return result.fold(
            onSuccess = { files ->
               Logger.i(logTag, "第 $pageNum 页获取成功，文件数: ${files.size}")
                processFilePage(files, pageNum, emitEvent)
            },
            onFailure = { error ->
               Logger.e(logTag, "获取文件列表失败", error)
                PageSyncResult.failure("获取文件列表失败: ${error.message}", pageNum)
            }
        )
    }

    /**
     * 处理单页文件
     */
    private suspend fun processFilePage(
        files: List<FileInfo>,
        pageNum: Int,
        emitEvent: suspend (SyncEvent) -> Unit
    ): PageSyncResult {
        if (files.isEmpty()) {
            return PageSyncResult.empty(pageNum)
        }

        var importedCount = 0
        var skippedCount = 0
        var failedCount = 0
        val importedRecords = mutableListOf<ImportedRunSummary>()
        val errors = mutableListOf<String>()
        var latestDataDate: String? = null
        var earliestFailedDataDate: String? = null

        for (file in files) {
            if (!coroutineContext.isActive) break

            // 检查是否已存在
            if (runRecordDao.getByOriginId(file.summaryId, file.platformCode) != null) {
                skippedCount++
               Logger.d(logTag, "文件已存在，跳过: ${file.summaryId}")
                continue
            }

            try {
                val importResult = processFile(file)
                if (importResult != null) {
                    importedCount++
                    importedRecords.add(importResult)
                    emitEvent(SyncEvent.RecordImported(importResult))

                    // 更新最新数据日期
                    val normalizedDate = TimestampUtils.normalizeTimestamp(file.dataDate)
                    if (latestDataDate == null || normalizedDate > latestDataDate!!) {
                        latestDataDate = normalizedDate
                    }
                } else {
                    skippedCount++
                }
            } catch (e: Exception) {
               Logger.e(logTag, "处理文件失败: ${file.summaryId}", e)
                failedCount++
                errors.add("${file.summaryId}: ${e.message}")

                // 记录失败的时间戳
                val normalizedDate = TimestampUtils.normalizeTimestamp(file.dataDate)
                if (earliestFailedDataDate == null || normalizedDate < earliestFailedDataDate!!) {
                    earliestFailedDataDate = normalizedDate
                }
            }

            emitEvent(
                SyncEvent.Progress(
                    current = importedCount,
                    total = 0,
                    message = "已导入 $importedCount 条记录"
                )
            )
        }

        return PageSyncResult(
            success = true,
            importedCount = importedCount,
            totalFilesInPage = files.size,
            skippedCount = skippedCount,
            failedCount = failedCount,
            importedRecords = importedRecords,
            latestDataDate = latestDataDate,
            earliestFailedDataDate = earliestFailedDataDate,
            hasMorePages = files.size >= SyncConstants.DEFAULT_PAGE_SIZE,
            pageNum = pageNum,
            errors = errors
        )
    }

    /**
     * 处理单个文件
     */
    private suspend fun processFile(fileInfo: FileInfo): ImportedRunSummary? {
       Logger.d(logTag, "开始处理文件: ${fileInfo.summaryId}")

        // 下载文件
        val downloadResult = downloadFile(fileInfo)
        val fileData = downloadResult.getOrNull()
            ?: throw Exception("文件下载失败: ${downloadResult.exceptionOrNull()?.message}")

       Logger.d(logTag, "文件下载成功: ${fileInfo.summaryId}, 大小: ${fileData.size} bytes")

        // 处理文件数据
        return processFileData(fileInfo, fileData)
    }

    /**
     * 将文件作为FIT文件处理
     */
    protected suspend fun processAsFitFile(fileInfo: FileInfo, fileData: ByteArray): ImportedRunSummary? {
        // 解析FIT文件
        val parseResult = FitFileParser.parseFromBytes(fileData)
        if (parseResult == null) {
           Logger.w(logTag, "FIT文件解析失败: ${fileInfo.summaryId}")
            return null
        }

       Logger.d(logTag, "FIT文件解析成功: ${fileInfo.summaryId}")

        // 转换为Entity
        val runRecordEntity = FitDataMapper.toRunRecordEntity(parseResult, fileInfo.summaryId)
        if (runRecordEntity == null) {
           Logger.w(logTag, "转换为Entity失败: ${fileInfo.summaryId}")
            return null
        }

        // 更新数据源和originId
        val updatedEntity = runRecordEntity.copy(
            datasource = fileInfo.platformCode,
            originId = fileInfo.summaryId,
            deviceInfo = fileInfo.deviceName
        )

        // 转换采样点和分段数据
        val samplePoints = FitDataMapper.toSamplePointEntities(
            parseResult,
            updatedEntity.workoutId,
            updatedEntity.startTime
        )
        val segments = FitDataMapper.toSegmentEntities(parseResult, updatedEntity.workoutId)

        // 保存到数据库
        runRecordDao.insert(updatedEntity)
        if (samplePoints.isNotEmpty()) {
            samplePointDao.insertAll(samplePoints)
        }
        if (segments.isNotEmpty()) {
            segmentDao.insertAll(segments)
        }
       Logger.d(logTag, "运动记录保存成功: ${fileInfo.summaryId}, 采样点: ${samplePoints.size}, 分段: ${segments.size}")

        // 创建导入摘要
        return ImportedRunSummary(
            originId = fileInfo.summaryId,
            platformCode = fileInfo.platformCode,
            runDate = Date(updatedEntity.startTime),
            distance = updatedEntity.totalDistance,
            displayText = formatDisplayText(updatedEntity.startTime, updatedEntity.totalDistance)
        )
    }

    /**
     * 更新同步时间戳（同步完成后调用）
     */
    private fun updateSyncTimestampAfterSync() {
        val nextTimestamp = timestampTracker.getNextSyncTimestamp()
        if (nextTimestamp != null) {
            val normalized = TimestampUtils.normalizeTimestamp(nextTimestamp)
            updateLastSyncTimestamp(normalized)
           Logger.i(logTag, "同步时间戳已更新: $normalized (hasErrors=${timestampTracker.hasErrors()})")
        }
    }

    /**
     * 格式化显示文本
     */
    protected fun formatDisplayText(startTime: Long, distance: Double): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(Date(startTime))
        val distanceStr = String.format(Locale.getDefault(), "%.1f", distance)
        return "已导入记录 $dateStr ${distanceStr}KM"
    }
}
