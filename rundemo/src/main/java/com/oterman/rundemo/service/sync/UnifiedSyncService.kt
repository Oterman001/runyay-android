package com.oterman.rundemo.service.sync

import com.oterman.rundemo.data.fit.FitFileParser
import com.oterman.rundemo.data.fit.RunSummaryMapper
import com.oterman.rundemo.data.fit.RunSummaryMerger
import com.oterman.rundemo.data.fit.UserPhysiologyConfig
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
            deviceInfo = fileInfo.deviceName
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

        // Step 5: 保存（使用更新后的record）
        val updatedResult = processResult.copy(runRecord = finalRecord)
        fitRecordProcessor.saveProcessResult(updatedResult)

        // Step 6: 服务端无runSummary时异步上传
        if (serverRunSummary == null) {
            // asyncUploadRunData(finalRecord.workoutId, fileInfo.summaryId)
        }

        return ImportedRunSummary(
            originId = finalRecord.originId ?: fileInfo.summaryId,
            platformCode = fileInfo.platformCode,
            runDate = Date(finalRecord.startTime),
            distance = finalRecord.totalDistance,
            duration = finalRecord.activeDuration,
            displayText = formatDisplayText(finalRecord.startTime, finalRecord.totalDistance)
        )
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
