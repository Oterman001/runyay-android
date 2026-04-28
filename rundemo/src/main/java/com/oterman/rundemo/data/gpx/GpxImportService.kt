package com.oterman.rundemo.data.gpx

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.oterman.rundemo.data.fit.FitRecordProcessor
import com.oterman.rundemo.data.fit.InclusiveLevelResolver
import com.oterman.rundemo.data.fit.KilometerSegmentCalculator
import com.oterman.rundemo.data.fit.RunSummaryMapper
import com.oterman.rundemo.data.fit.UserPhysiologyConfig
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.RunDataRemoteRepository
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.presentation.feature.home.FitImportResult
import com.oterman.rundemo.util.FitDataConverter
import com.oterman.rundemo.util.RLog
import com.oterman.rundemo.util.TimestampUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GpxImportService(private val context: Context) {

    companion object {
        private const val TAG = "GpxImportService"
    }

    private val parser = GpxFileParser(context)
    private val database: RunDatabase by lazy { RunDatabase.getInstance(context) }
    private val repository: RunDataRepository by lazy { RunDataRepositoryImpl.getInstance(database) }
    private val preferencesManager: PreferencesManager by lazy { PreferencesManager(context) }
    private val remoteRepository: RunDataRemoteRepository by lazy {
        RunDataRemoteRepository(preferencesManager)
    }
    private val dataSourcePreferences: DataSourcePreferences by lazy {
        DataSourcePreferences(context)
    }
    private val inclusiveLevelResolver: InclusiveLevelResolver by lazy {
        InclusiveLevelResolver(repository, dataSourcePreferences)
    }
    private val fitRecordProcessor: FitRecordProcessor by lazy {
        FitRecordProcessor(repository)
    }

    suspend fun importGpxFile(uri: Uri, forceImport: Boolean = false): FitImportResult =
        withContext(Dispatchers.IO) {
            RLog.i(TAG, "========== 开始导入GPX文件 ==========")

            try {
                val fileName = getFileName(uri) ?: uri.lastPathSegment ?: "unknown.gpx"
                RLog.i(TAG, "文件名: $fileName")

                // 1. 解析 GPX
                val gpxResult = parser.parse(uri).getOrElse { error ->
                    RLog.e(TAG, "GPX解析失败: ${error.message}")
                    return@withContext FitImportResult.Error("GPX解析失败: ${error.message}")
                }
                RLog.i(TAG, "解析成功: ${gpxResult.trackPoints.size}个轨迹点, 距离估算中...")

                // 2. 去重检查
                val originId = GpxDataMapper.generateOriginId(fileName, gpxResult.startTime)
                RLog.i(TAG, "originId: $originId")
                if (repository.existsByOriginId(originId, FitDataConverter.Datasource.MANUAL)) {
                    RLog.w(TAG, "GPX文件已导入过，跳过")
                    return@withContext FitImportResult.AlreadyExists
                }

                // 3. 冲突检测
                if (!forceImport) {
                    val conflicts = repository.getConflictingRecords(gpxResult.startTime, gpxResult.endTime)
                    if (conflicts.isNotEmpty()) {
                        RLog.w(TAG, "发现时间冲突: ${conflicts.size}条记录")
                        return@withContext FitImportResult.ConflictFound(conflicts, uri)
                    }
                }

                // 4. 适配为 FitParseResult，供 FitRecordProcessor 统一处理
                val fitParseResult = GpxDataMapper.toFitParseResult(gpxResult, fileName)

                // 5. 处理数据（心率区间、VDOT、PB 计算）
                val userConfig = buildUserConfig()
                var processResult = fitRecordProcessor.processFitData(
                    parseResult = fitParseResult,
                    originId = originId,
                    datasource = FitDataConverter.Datasource.MANUAL,
                    userConfig = userConfig,
                    workoutIdFileName = fileName
                ) ?: run {
                    RLog.e(TAG, "GPX数据处理失败")
                    return@withContext FitImportResult.Error("数据处理失败")
                }

                // 6. GPX 无 Lap 数据，processFitData 返回空分段，在此补充公里分段
                val kmSegments = KilometerSegmentCalculator.calculateKilometerSegments(
                    records = fitParseResult.records,
                    workoutId = processResult.runRecord.workoutId,
                    startTimeMs = processResult.runRecord.startTime,
                    pauseList = emptyList(),
                    maxDistanceM = fitParseResult.session?.totalDistance?.toDouble()
                )
                processResult = processResult.copy(segments = kmSegments)

                // 7. 强制导入时解决 inclusiveLevel 冲突
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

                // 8. 需要登录才能上传
                preferencesManager.getUserId()
                    ?: return@withContext FitImportResult.UploadFailed("请先登录后再导入")

                // 9. 上传摘要（GPX 不上传原始文件二进制）
                val summaryDto = RunSummaryMapper.toUploadItemDto(runRecord)
                RLog.d(TAG, "上传GPX摘要: hkUUid=${summaryDto.hkUUid}")
                val summaryResult = remoteRepository.uploadRunRecords(listOf(summaryDto))
                if (summaryResult.isFailure) {
                    val msg = summaryResult.exceptionOrNull()?.message ?: "未知错误"
                    RLog.w(TAG, "摘要上传失败: $msg")
                    return@withContext FitImportResult.UploadFailed("上传失败: $msg")
                }
                val uploadResponse = summaryResult.getOrThrow()
                if (uploadResponse.successCount == 0) {
                    val msg = uploadResponse.failedRecords?.firstOrNull()?.errorMessage ?: "服务器拒绝"
                    RLog.w(TAG, "摘要上传被服务器拒绝: $msg")
                    return@withContext FitImportResult.UploadFailed(msg)
                }

                // 10. 保存到 Room，更新上传状态
                fitRecordProcessor.saveProcessResult(processResult)
                repository.updateUploadStatus(runRecord.workoutId, 2)

                // 11. 更新 MANUAL 平台同步时间戳
                dataSourcePreferences.setLastSyncTime(
                    DataSourcePlatform.MANUAL,
                    TimestampUtils.getCurrentTimestamp()
                )

                RLog.i(TAG, "========== GPX文件导入成功 ==========")
                FitImportResult.Success(
                    distance = runRecord.totalDistance,
                    duration = runRecord.activeDuration,
                    workoutId = runRecord.workoutId
                )

            } catch (e: Exception) {
                RLog.e(TAG, "GPX导入失败", e)
                FitImportResult.Error("导入失败: ${e.message}")
            }
        }

    private fun buildUserConfig(): UserPhysiologyConfig {
        val settings = preferencesManager.getHearRateZoneSettings()
        return UserPhysiologyConfig(
            maxHR = settings.maxHeartRate.toDouble(),
            restHR = settings.restingHeartRate.toDouble(),
            isMale = settings.isMale
        )
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (e: Exception) {
            RLog.e(TAG, "获取文件名失败", e)
            null
        }
    }
}
