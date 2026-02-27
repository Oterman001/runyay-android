package com.oterman.rundemo.data.fit

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.HealthRepository
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.presentation.feature.home.FitImportResult
import com.oterman.rundemo.util.FitDataConverter
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FIT文件导入服务
 * 封装完整的导入流程：解析 -> 去重检查 -> 处理 -> 存储
 *
 * 使用 FitRecordProcessor 进行数据处理，包含：
 * - 心率区间计算 (7区间 + 5区间)
 * - 配速区间计算
 * - VDOT 和 OverallVDOT 计算
 * - PB 记录计算
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

    /** 健康数据仓库（用于获取真实静息心率） */
    private val healthRepository: HealthRepository? by lazy {
        try {
            val preferencesManager = PreferencesManager(context)
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

    /** FIT数据处理器 */
    private val fitRecordProcessor: FitRecordProcessor by lazy {
        FitRecordProcessor(repository)
    }

    /**
     * 导入FIT文件
     * @param uri 文件Uri
     * @return 导入结果
     */
    suspend fun importFitFile(uri: Uri): FitImportResult = withContext(Dispatchers.IO) {
        RLog.i(TAG, "========== 开始导入FIT文件 ==========")
        RLog.i(TAG, "文件Uri: $uri")

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

            // 4. 去重检查
            val originId = fileName
            RLog.i(TAG, "检查是否已导入: originId=$originId")

            if (repository.existsByOriginId(originId, FitDataConverter.Datasource.LOCAL_FIT)) {
                RLog.w(TAG, "文件已导入过，跳过")
                return@withContext FitImportResult.AlreadyExists
            }

            // 5. 使用 FitRecordProcessor 处理并保存
            // 尝试从健康数据获取真实静息心率
            val userConfig = buildUserConfig(parseResult.session?.startTime)

            val importResult = fitRecordProcessor.processAndSave(
                parseResult = parseResult,
                originId = originId,
                datasource = FitDataConverter.Datasource.LOCAL_FIT,
                userConfig = userConfig
            )

            if (importResult == null) {
                RLog.e(TAG, "数据处理失败")
                return@withContext FitImportResult.Error("数据处理失败")
            }

            RLog.i(TAG, "========== FIT文件导入成功 ==========")

            FitImportResult.Success(
                distance = importResult.distance,
                duration = importResult.duration
            )

        } catch (e: Exception) {
            RLog.e(TAG, "导入失败", e)
            FitImportResult.Error("导入失败: ${e.message}")
        }
    }

    /**
     * 构建用户生理参数配置
     * 尝试从HealthRepository获取真实静息心率，失败则使用默认值
     */
    private suspend fun buildUserConfig(startTimeMs: Long?): UserPhysiologyConfig {
        if (startTimeMs == null || startTimeMs <= 0) return UserPhysiologyConfig()

        try {
            val repo = healthRepository ?: return UserPhysiologyConfig()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendarDate = dateFormat.format(Date(FitFileParser.fitTimestampToMillis(startTimeMs)))
            val restHR = repo.getRestingHRForDate(calendarDate)
            if (restHR != null && restHR > 0) {
                RLog.i(TAG, "使用真实静息心率: restHR=$restHR, date=$calendarDate")
                return UserPhysiologyConfig(restHR = restHR.toDouble())
            }
        } catch (e: Exception) {
            RLog.w(TAG, "获取静息心率失败（使用默认值）: ${e.message}")
        }

        return UserPhysiologyConfig()
    }

    /**
     * 从Uri获取文件名
     */
    private fun getFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        cursor.getString(nameIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            RLog.e(TAG, "获取文件名失败", e)
            null
        }
    }
}
