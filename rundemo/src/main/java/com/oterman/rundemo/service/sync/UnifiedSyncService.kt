package com.oterman.rundemo.service.sync

import com.oterman.rundemo.data.fit.FitFileParser
import com.oterman.rundemo.data.fit.InclusiveLevelResolver
import com.oterman.rundemo.data.fit.RunSummaryMapper
import com.oterman.rundemo.data.fit.RunSummaryMerger
import com.oterman.rundemo.data.fit.UserPhysiologyConfig
import com.oterman.rundemo.data.fit.VdotRecalculationService
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.data.local.dao.RunSamplePointDao
import com.oterman.rundemo.data.local.dao.RunSegmentDao
import com.oterman.rundemo.data.network.dto.RunSummaryBasicInfoDto
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.data.repository.HealthRepository
import com.oterman.rundemo.data.repository.RunDataRemoteRepository
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.FileInfo
import com.oterman.rundemo.data.fit.FitParseResult
import com.oterman.rundemo.domain.model.ImportedRunSummary
import com.oterman.rundemo.domain.model.SyncResult
import com.oterman.rundemo.domain.model.UnifiedFileInfo
import com.oterman.rundemo.util.RLog
import com.oterman.rundemo.util.TimestampUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 统一同步服务
 * 继承BaseDataSyncService，使用统一API获取文件列表（含内嵌runSummary）
 * 实现新的同步流程：
 * 1. fetchFileList → 统一API获取 [UnifiedFileInfo + optional runSummary]
 * 2. downloadFile → 仅使用ossUrl下载
 * 3. processFileData → FIT解析 → 合并/上传
 */
class UnifiedSyncService(
    dataSourceRepository: DataSourceRepository,
    runRecordDao: RunRecordDao,
    samplePointDao: RunSamplePointDao,
    segmentDao: RunSegmentDao,
    dataSourcePreferences: DataSourcePreferences,
    runDataRepository: RunDataRepository,
    healthRepository: HealthRepository? = null,
    private val runDataRemoteRepository: RunDataRemoteRepository,
    private val targetPlatform: DataSourcePlatform,
    preferencesManager: PreferencesManager? = null
) : BaseDataSyncService(
    dataSourceRepository,
    runRecordDao,
    samplePointDao,
    segmentDao,
    dataSourcePreferences,
    runDataRepository,
    healthRepository,
    preferencesManager
) {
    companion object {
        private const val TAG = "UnifiedSyncService"
    }

    override val platform: DataSourcePlatform = targetPlatform

    override val logTag: String = "UnifiedSync_${targetPlatform.code}"

    /** 缓存UnifiedFileInfo，key=summaryId */
    private val unifiedFileInfoCache = ConcurrentHashMap<String, UnifiedFileInfo>()

    /** 追踪本次同步中最早的记录时间，用于同步后批量重算VDOT */
    private var earliestImportedStartTime: Long = Long.MAX_VALUE

    /** 待批量上传的workoutId列表（服务端无runSummary、非HK/MANUAL平台） */
    private val pendingUploadWorkoutIds = mutableListOf<String>()

    /** inclusiveLevel 冲突解决器 */
    private val inclusiveLevelResolver: InclusiveLevelResolver by lazy {
        InclusiveLevelResolver(runDataRepository, dataSourcePreferences)
    }

    /** 异步上传协程作用域 */
    private val uploadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 下载用OkHttpClient */
    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // ============ 抽象方法实现 ============
    override fun getLastSyncTimestamp(): String {
        val timestamp = dataSourcePreferences.getLastSyncTime(targetPlatform)
        return TimestampUtils.normalizeTimestamp(timestamp)
    }

    override fun updateLastSyncTimestamp(timestamp: String) {
        val normalized = TimestampUtils.normalizeTimestamp(timestamp)
        dataSourcePreferences.setLastSyncTime(targetPlatform, normalized)
    }

    override fun clearSyncTimestampInternal() {
        dataSourcePreferences.setLastSyncTime(targetPlatform, DataSourcePreferences.DEFAULT_SYNC_START_TIME)
    }

    override suspend fun fetchFileList(
        pageNum: Int,
        pageSize: Int,
        lastSyncTime: String
    ): Result<List<FileInfo>> {
        val result = runDataRemoteRepository.getActivityFileList(
            platformCode = targetPlatform.code,
            pageNum = pageNum,
            pageSize = pageSize,
            lastSyncTime = lastSyncTime
        )

        return result.map { unifiedFiles ->
            // 缓存UnifiedFileInfo
            unifiedFiles.forEach { unifiedFile ->
                unifiedFileInfoCache[unifiedFile.summaryId] = unifiedFile
            }
            RLog.d(logTag, "缓存${unifiedFiles.size}个UnifiedFileInfo, 其中${unifiedFiles.count { it.hasRunSummary }}个带runSummary")
            // 转换为FileInfo以兼容基类流程
            unifiedFiles.map { it.toFileInfo() }
        }
    }

    override suspend fun downloadFile(fileInfo: FileInfo): Result<ByteArray> {
        // 优先使用ossUrl直接下载
        val ossUrl = fileInfo.ossUrl
        if (!ossUrl.isNullOrEmpty()) {
            val result = downloadFromOssUrl(ossUrl, fileInfo.summaryId)
            if (result.isSuccess) {
                return result
            }
            RLog.w(logTag, "ossUrl下载失败，尝试fitUrl: ${fileInfo.summaryId}")
        }

        // 降级使用fitUrl
        val fitUrl = fileInfo.fitUrl
        if (!fitUrl.isNullOrEmpty()) {
            val result = downloadFromOssUrl(fitUrl, fileInfo.summaryId)
            if (result.isSuccess) {
                return result
            }
        }

        return Result.failure(Exception("无可用的下载URL: ${fileInfo.summaryId}"))
    }

    override suspend fun processFileData(
        fileInfo: FileInfo,
        fileData: ByteArray
    ): ImportedRunSummary? {
        // Step 1: 解析FIT文件
        val parseResult = FitFileParser.parseFromBytes(fileData)
        if (parseResult == null) {
            RLog.w(logTag, "FIT文件解析失败: ${fileInfo.summaryId}")
            return null
        }

        // Step 2: 构建用户生理参数
        val serverRunSummary = unifiedFileInfoCache[fileInfo.summaryId]?.runSummary
        val summaryRestHR = serverRunSummary?.restingHeartRate
        val summaryVo2Max = serverRunSummary?.vo2Max

        val userConfig: UserPhysiologyConfig
        val settings = preferencesManager?.getHearRateZoneSettings()
        val isHKPlatform = targetPlatform == DataSourcePlatform.APPLE_HEALTH
        val isManualPlatform = targetPlatform == DataSourcePlatform.MANUAL

        if (settings != null && !settings.isAutoSyncEnabled) {
            // 用户关闭了自动同步 → 直接使用手动配置，忽略 summaryRestHR
            RLog.i(logTag, "使用用户手动配置（自动同步已关闭）: maxHR=${settings.maxHeartRate}, restHR=${settings.restingHeartRate}")
            userConfig = UserPhysiologyConfig(
                maxHR = settings.maxHeartRate.toDouble(),
                restHR = settings.restingHeartRate.toDouble(),
                isMale = settings.isMale
            )
        } else if (summaryRestHR != null && summaryRestHR > 0) {
            // runSummary有有效静息心率 → 使用服务端值，存储到本地，跳过额外网络请求
            RLog.i(logTag, "使用runSummary中的静息心率: restHR=$summaryRestHR")
            userConfig = UserPhysiologyConfig(
                maxHR = settings?.maxHeartRate?.toDouble() ?: 190.0,
                restHR = summaryRestHR.toDouble(),
                isMale = settings?.isMale ?: true
            )
            saveHealthDataFromSummary(parseResult, summaryRestHR, summaryVo2Max)
        } else if (isHKPlatform || isManualPlatform) {
            // HK/MANUAL平台 → 不触发网络请求，使用用户设置兜底
            RLog.i(logTag, "${targetPlatform.displayName}平台跳过健康数据网络请求，使用用户设置")
            userConfig = settings?.let {
                UserPhysiologyConfig(
                    maxHR = it.maxHeartRate.toDouble(),
                    restHR = it.restingHeartRate.toDouble(),
                    isMale = it.isMale
                )
            } ?: UserPhysiologyConfig()
        } else {
            // 其他情况 → 走 buildUserConfig 流程（可能触发网络请求，内部已包含用户设置兜底）
            userConfig = buildUserConfig(parseResult.session?.startTime)
        }

        // 同时存储有效的vo2Max（即使restHR无效，vo2Max可能有效）
        if (summaryVo2Max != null && summaryVo2Max > 0 && (summaryRestHR == null || summaryRestHR <= 0)) {
            saveHealthDataFromSummary(parseResult, null, summaryVo2Max)
        }

        // Step 3: 使用FitRecordProcessor处理
        val processResult = fitRecordProcessor.processFitData(
            parseResult = parseResult,
            originId = fileInfo.summaryId,
            datasource = fileInfo.platformCode,
            userConfig = userConfig,
            deviceInfo = fileInfo.deviceName,
            skipOverallVdot = true
        ) ?: return null

        // 探测并自动更新最大心率
        maybeUpdateMaxHR(processResult.runRecord.maxHeartRate)

        // Step 4: 合并或使用本地runSummary

        val finalRecord = if (serverRunSummary != null) {
            // 服务端有runSummary → 合并（服务端优先）
            RLog.i(logTag, "服务端有runSummary，执行合并: ${fileInfo.summaryId}")
            val mergeResult = RunSummaryMerger.merge(processResult.runRecord, serverRunSummary)

            // 合并后设uploadStatus=2（无需上传）
            mergeResult.mergedEntity.copy(uploadStatus = 2)
        } else {
            // 服务端无runSummary → 保留本地，标记需要上传
            RLog.i(logTag, "服务端无runSummary，使用本地解析结果: ${fileInfo.summaryId}")
            processResult.runRecord.copy(uploadStatus = 0)
        }

        // Step 5: 解决 inclusiveLevel 冲突
        val serverInclusiveLevel = serverRunSummary?.inclusiveLevel
        val resolveResult = inclusiveLevelResolver.resolve(finalRecord, serverInclusiveLevel)
        val resolvedRecord = resolveResult.adjustedRecord

        // Step 6: 保存（使用解决冲突后的record）
        val updatedResult = processResult.copy(
            runRecord = resolvedRecord,
            vdotEntity = processResult.vdotEntity?.copy(inclusiveLevel = resolvedRecord.inclusiveLevel)
        )
        fitRecordProcessor.saveProcessResult(updatedResult)

        // 追踪最早导入时间，用于同步后批量重算VDOT
        earliestImportedStartTime = minOf(earliestImportedStartTime, resolvedRecord.startTime)

        // Step 7: 服务端无runSummary时加入待上传列表（同步结束后统一批量上传）
        if (serverRunSummary == null && !isHKPlatform && !isManualPlatform) {
            pendingUploadWorkoutIds.add(resolvedRecord.workoutId)
        }

        return ImportedRunSummary(
            originId = resolvedRecord.originId ?: fileInfo.summaryId,
            platformCode = fileInfo.platformCode,
            runDate = Date(resolvedRecord.startTime),
            distance = resolvedRecord.totalDistance,
            duration = resolvedRecord.activeDuration,
            displayText = formatDisplayText(resolvedRecord.startTime, resolvedRecord.totalDistance)
        )
    }

    // ============ 同步后批量重算 ============

    override suspend fun onAfterSync(result: SyncResult) {
        if (result.importedCount > 0 && earliestImportedStartTime != Long.MAX_VALUE) {
            RLog.i(logTag, "同步完成，开始批量重算VDOT...")
            val recalcService = VdotRecalculationService(runDataRepository)
            recalcService.recalculateAfterSync(earliestImportedStartTime)
            RLog.i(logTag, "VDOT批量重算完成")
        }
        // 批量上传无服务端runSummary的记录（全部下载完毕后统一上传）
        if (pendingUploadWorkoutIds.isNotEmpty()) {
            batchUploadPendingRecords()
        }
        // 重置追踪变量
        earliestImportedStartTime = Long.MAX_VALUE
        pendingUploadWorkoutIds.clear()
    }

    // ============ 私有方法 ============

    /**
     * 从URL下载文件
     */
    private fun downloadFromOssUrl(url: String, fileName: String): Result<ByteArray> {
        return try {
            val request = Request.Builder().url(url).build()
            val response = downloadClient.newCall(request).execute()

            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                    ?: return Result.failure(Exception("响应体为空"))
                Result.success(bytes)
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            RLog.e(logTag, "URL下载异常: $fileName", e)
            Result.failure(e)
        }
    }

    /**
     * 将runSummary中的健康数据存储到本地DB
     */
    private suspend fun saveHealthDataFromSummary(
        parseResult: FitParseResult,
        restingHeartRate: Int?,
        vo2Max: Double?
    ) {
        if (healthRepository == null) return
        val startTimeMs = parseResult.session?.startTime ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendarDate = dateFormat.format(Date(FitFileParser.fitTimestampToMillis(startTimeMs)))
        healthRepository.saveHealthDataFromSummary(
            platformCode = targetPlatform.code,
            calendarDate = calendarDate,
            restingHeartRate = restingHeartRate,
            vo2Max = vo2Max
        )
    }

    /**
     * 批量上传待上传记录（同步结束后统一调用）
     */
    private suspend fun batchUploadPendingRecords() {
        RLog.i(logTag, "开始批量上传 ${pendingUploadWorkoutIds.size} 条记录")
        // 标记所有记录为上传中
        pendingUploadWorkoutIds.forEach { runDataRepository.updateUploadStatus(it, 1) }

        try {
            val settings = preferencesManager?.getHearRateZoneSettings()
            val dtos = pendingUploadWorkoutIds.mapNotNull { workoutId ->
                val record = runDataRepository.getRunRecord(workoutId)
                if (record == null) {
                    RLog.w(logTag, "批量上传：找不到记录 workoutId=$workoutId")
                    null
                } else {
                    RunSummaryMapper.toUploadItemDto(record, settings)
                }
            }

            if (dtos.isEmpty()) {
                RLog.w(logTag, "批量上传：无有效记录可上传")
                return
            }

            val result = runDataRemoteRepository.uploadRunRecords(dtos)
            if (result.isSuccess) {
                pendingUploadWorkoutIds.forEach { runDataRepository.updateUploadStatus(it, 2) }
                RLog.i(logTag, "批量上传成功: ${dtos.size} 条")
            } else {
                pendingUploadWorkoutIds.forEach { runDataRepository.updateUploadStatus(it, 3) }
                RLog.w(logTag, "批量上传失败: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            pendingUploadWorkoutIds.forEach { runDataRepository.updateUploadStatus(it, 3) }
            RLog.e(logTag, "批量上传异常", e)
        }
    }

    /**
     * 异步上传跑步基础数据（不阻塞同步流程）
     */
    private fun asyncUploadRunData(workoutId: String, summaryId: String) {
        uploadScope.launch {
            try {
                // 先标记为上传中
                runDataRepository.updateUploadStatus(workoutId, 1)

                // 获取记录
                val record = runDataRepository.getRunRecord(workoutId)
                if (record == null) {
                    RLog.w(logTag, "上传失败：找不到记录 workoutId=$workoutId")
                    return@launch
                }

                // 转换为上传DTO
                val uploadDto = RunSummaryMapper.toUploadItemDto(record)
                val result = runDataRemoteRepository.uploadRunRecords(listOf(uploadDto))

                if (result.isSuccess) {
                    runDataRepository.updateUploadStatus(workoutId, 2)
                    RLog.i(logTag, "异步上传成功: $summaryId")
                } else {
                    runDataRepository.updateUploadStatus(workoutId, 3)
                    RLog.w(logTag, "异步上传失败: $summaryId, ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                runDataRepository.updateUploadStatus(workoutId, 3)
                RLog.e(logTag, "异步上传异常: $summaryId", e)
            }
        }
    }

}
