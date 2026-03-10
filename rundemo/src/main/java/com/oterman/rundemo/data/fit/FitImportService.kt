package com.oterman.rundemo.data.fit

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.HealthRepository
import com.oterman.rundemo.data.repository.RunDataRemoteRepository
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.data.repository.UserRepository
import com.oterman.rundemo.presentation.feature.home.FitImportResult
import com.oterman.rundemo.util.FitDataConverter
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FIT文件导入服务
 * 封装完整的导入流程：解析 -> 去重检查 -> 冲突检测 -> 处理 -> 存储 -> 上传
 */
class FitImportService(private val context: Context) {

    companion object {
        private const val TAG = "FitImportService"
    }

    private val parser = FitFileParser(context)
    private val database: RunDatabase by lazy { RunDatabase.getInstance(context) }
    private val repository: RunDataRepository by lazy {
        RunDataRepositoryImpl.getInstance(database)
    }
    private val preferencesManager: PreferencesManager by lazy { PreferencesManager(context) }
    private val remoteRepository: RunDataRemoteRepository by lazy {
        RunDataRemoteRepository(preferencesManager)
    }

    /** 健康数据仓库（用于获取真实静息心率） */
    private val healthRepository: HealthRepository? by lazy {
        try {
            val dataSourcePreferences = com.oterman.rundemo.data.local.DataSourcePreferences(context)
            val dataSourceRepository = com.oterman.rundemo.data.repository.DataSourceRepository(
                dataSourcePreferences = dataSourcePreferences,
                preferencesManager = preferencesManager
            )
            HealthRepository(
                dailyHealthDao = database.dailyHealthDao(),
                dataSourceRepository = dataSourceRepository,
                preferencesManager = preferencesManager
            )
        } catch (e: Exception) {
            RLog.w(TAG, "初始化HealthRepository失败: ${e.message}")
            null
        }
    }

    /** 用户信息仓库（用于将更新的最大心率同步到服务端） */
    private val userRepository: UserRepository by lazy { UserRepository(context) }

    /** 用于 fire-and-forget 异步操作的协程作用域 */
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 数据源偏好 */
    private val dataSourcePreferences: DataSourcePreferences by lazy {
        DataSourcePreferences(context)
    }

    /** inclusiveLevel 冲突解决器 */
    private val inclusiveLevelResolver: InclusiveLevelResolver by lazy {
        InclusiveLevelResolver(repository, dataSourcePreferences)
    }

    /** FIT数据处理器 */
    private val fitRecordProcessor: FitRecordProcessor by lazy {
        FitRecordProcessor(repository)
    }

    /**
     * 导入FIT文件
     * @param uri 文件Uri
     * @param forceImport 强制导入（忽略时间冲突）
     * @return 导入结果
     */
    suspend fun importFitFile(uri: Uri, forceImport: Boolean = false): FitImportResult = withContext(Dispatchers.IO) {
        RLog.i(TAG, "========== 开始导入FIT文件 ==========")
        RLog.i(TAG, "文件Uri: $uri, forceImport=$forceImport")

        try {
            // 1. 获取文件名
            val fileName = getFileName(uri) ?: uri.lastPathSegment ?: "unknown.fit"
            RLog.i(TAG, "文件名: $fileName")

            // 2. 解析FIT文件
            RLog.i(TAG, "开始解析FIT文件...")
            val parseResult = parser.parse(uri).getOrElse { error ->
                RLog.e(TAG, "解析失败: ${error.message}")
                return@withContext FitImportResult.Error("解析失败: ${error.message}")
            }

            // 3. 验证解析结果
            if (parseResult.session == null) {
                RLog.e(TAG, "FIT文件中没有Session数据")
                return@withContext FitImportResult.Error("无效的FIT文件：缺少运动数据")
            }

            RLog.i(TAG, "解析成功: records=${parseResult.records.size}, laps=${parseResult.laps.size}")

            // 4. 去重检查（基于内容：session startTime，与文件名无关）
            val sessionStartMs = FitFileParser.fitTimestampToMillis(parseResult.session!!.startTime)
            val originId = FitDataMapper.generateWorkoutId(fileName, sessionStartMs)
            RLog.i(TAG, "检查是否已导入: originId=$originId")

            if (repository.existsByOriginId(originId, FitDataConverter.Datasource.MANUAL)) {
                RLog.w(TAG, "文件已导入过，跳过")
                return@withContext FitImportResult.AlreadyExists
            }

            // 4b. 冲突检测（forceImport=true 时跳过）
            if (!forceImport) {
                val session = parseResult.session
                val newStartTime = FitFileParser.fitTimestampToMillis(session.startTime)
                val newEndTime = newStartTime + (session.totalElapsedTime * 1000).toLong()

                val conflicts = repository.getConflictingRecords(newStartTime, newEndTime)
                if (conflicts.isNotEmpty()) {
                    RLog.w(TAG, "发现时间冲突: ${conflicts.size}条记录")
                    return@withContext FitImportResult.ConflictFound(conflicts, uri)
                }
            }

            // 5. 使用 FitRecordProcessor 处理（不保存）
            val userConfig = buildUserConfig(parseResult.session?.startTime)
            maybeUpdateMaxHR(parseResult.session?.maxHeartRate?.toDouble() ?: 0.0)

            var processResult = fitRecordProcessor.processFitData(
                parseResult = parseResult,
                originId = originId,
                datasource = FitDataConverter.Datasource.MANUAL,
                userConfig = userConfig,
                workoutIdFileName = fileName
            ) ?: run {
                RLog.e(TAG, "数据处理失败")
                return@withContext FitImportResult.Error("数据处理失败")
            }

            // 5b. 解决 inclusiveLevel 冲突（forceImport 时执行，非 forceImport 已在前面拦截冲突）
            if (forceImport) {
                val resolveResult = inclusiveLevelResolver.resolve(processResult.runRecord)
                processResult = processResult.copy(
                    runRecord = resolveResult.adjustedRecord,
                    vdotEntity = processResult.vdotEntity?.copy(
                        inclusiveLevel = resolveResult.adjustedRecord.inclusiveLevel
                    )
                )
            }

            val runRecord = processResult.runRecord

            // 6. 必须登录才能上传（userId由请求头携带，无需放入Body）
            preferencesManager.getUserId()
                ?: return@withContext FitImportResult.UploadFailed("请先登录后再导入")

            // 7. 读取FIT文件字节
            val fileBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext FitImportResult.Error("无法读取文件")

            // 8. 上传跑步摘要
            val summaryDto = RunSummaryMapper.toUploadItemDto(runRecord)
            RLog.d(TAG, "上传摘要: hkUUid=${summaryDto.hkUUid}, startTime=${summaryDto.startTime}")
            val summaryResult = remoteRepository.uploadRunRecords(listOf(summaryDto))
            if (summaryResult.isFailure) {
                val msg = summaryResult.exceptionOrNull()?.message ?: "未知错误"
                RLog.w(TAG, "摘要上传失败: $msg")
                return@withContext FitImportResult.UploadFailed("摘要上传失败: $msg")
            }
            val uploadResponse = summaryResult.getOrThrow()
            if (uploadResponse.successCount == 0) {
                val msg = uploadResponse.failedRecords?.firstOrNull()?.errorMessage ?: "服务器拒绝摘要"
                RLog.w(TAG, "摘要上传被服务器拒绝: $msg")
                return@withContext FitImportResult.UploadFailed(msg)
            }

            // 9. 上传FIT文件
            val activityType = if (runRecord.outdoor == 1) "ir" else "or"
            val activityStartTime = formatActivityStartTime(runRecord.startTime)
            val fitUploadResult = remoteRepository.uploadFitFile(
                workoutId = runRecord.workoutId,
                fileBytes = fileBytes,
                fileName = fileName,
                deviceName = runRecord.deviceInfo,
                activityType = activityType,
                activityStartTime = activityStartTime
            )
            if (fitUploadResult.isFailure) {
                val msg = fitUploadResult.exceptionOrNull()?.message ?: "未知错误"
                RLog.w(TAG, "FIT文件上传失败: $msg")
                return@withContext FitImportResult.UploadFailed("FIT文件上传失败: $msg")
            }

            // 10. 两次上传均成功，保存到 Room
            fitRecordProcessor.saveProcessResult(processResult)
            repository.updateUploadStatus(runRecord.workoutId, 2)

            RLog.i(TAG, "========== FIT文件导入成功 ==========")
            FitImportResult.Success(
                distance = runRecord.totalDistance,
                duration = runRecord.activeDuration,
                workoutId = runRecord.workoutId
            )

        } catch (e: Exception) {
            RLog.e(TAG, "导入失败", e)
            FitImportResult.Error("导入失败: ${e.message}")
        }
    }

    /**
     * 构建用户生理参数配置
     * 优先级：isAutoSyncEnabled=false → 用户手动设置；isAutoSyncEnabled=true → DB查询当日/最近静息心率 → 用户设置兜底
     */
    private suspend fun buildUserConfig(startTimeMs: Long?): UserPhysiologyConfig {
        val settings = preferencesManager.getHearRateZoneSettings()

        if (!settings.isAutoSyncEnabled) {
            RLog.i(TAG, "使用用户手动配置（自动同步已关闭）: maxHR=${settings.maxHeartRate}, restHR=${settings.restingHeartRate}")
            return UserPhysiologyConfig(
                maxHR = settings.maxHeartRate.toDouble(),
                restHR = settings.restingHeartRate.toDouble(),
                isMale = settings.isMale
            )
        }

        if (startTimeMs != null && startTimeMs > 0) {
            try {
                val repo = healthRepository ?: return defaultConfig(settings)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendarDate = dateFormat.format(Date(FitFileParser.fitTimestampToMillis(startTimeMs)))
                val restHR = repo.getRestingHRForDate(calendarDate)
                if (restHR != null && restHR > 0) {
                    RLog.i(TAG, "使用平台静息心率: restHR=$restHR, date=$calendarDate")
                    return UserPhysiologyConfig(
                        maxHR = settings.maxHeartRate.toDouble(),
                        restHR = restHR.toDouble(),
                        isMale = settings.isMale
                    )
                }
            } catch (e: Exception) {
                RLog.w(TAG, "获取静息心率失败，使用用户设置值: ${e.message}")
            }
        }

        return defaultConfig(settings)
    }

    private fun defaultConfig(settings: com.oterman.rundemo.data.local.HearRateZoneSettings) = UserPhysiologyConfig(
        maxHR = settings.maxHeartRate.toDouble(),
        restHR = settings.restingHeartRate.toDouble(),
        isMale = settings.isMale
    )

    /**
     * 探测本次运动峰值心率，若超过用户配置的最大心率则自动更新，并异步同步到服务端
     */
    private fun maybeUpdateMaxHR(workoutPeakHR: Double) {
        val peakInt = workoutPeakHR.toInt()
        if (peakInt <= 0) return
        val settings = preferencesManager.getHearRateZoneSettings()
        if (peakInt > settings.maxHeartRate) {
            preferencesManager.saveHearRateZoneSettings(settings.copy(maxHeartRate = peakInt))
            RLog.i(TAG, "自动更新最大心率: ${settings.maxHeartRate} → $peakInt bpm")
            bgScope.launch {
                userRepository.updateBasicInfo(maxHR = peakInt).onFailure { e ->
                    RLog.w(TAG, "最大心率同步到服务端失败（非致命）: ${e.message}")
                }
            }
        }
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

    private fun formatActivityStartTime(startTimeMs: Long): String {
        return SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date(startTimeMs))
    }

}
