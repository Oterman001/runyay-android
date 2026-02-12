package com.oterman.rundemo.data.fit

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.presentation.feature.home.FitImportResult
import com.oterman.rundemo.util.FitDataConverter
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    private val repository: RunDataRepository by lazy {
        RunDataRepositoryImpl(RunDatabase.getInstance(context))
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
            // TODO: 后续可从用户设置获取生理参数
            val userConfig = UserPhysiologyConfig(
                maxHR = 190.0,
                restHR = 60.0,
                isMale = true
            )

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
