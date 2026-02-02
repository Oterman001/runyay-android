package com.oterman.rundemo.data.fit

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.presentation.feature.home.FitImportResult
import com.oterman.rundemo.util.FitDataConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FIT文件导入服务
 * 封装完整的导入流程：解析 -> 去重检查 -> 转换 -> 存储
 */
class FitImportService(private val context: Context) {
    
    companion object {
        private const val TAG = "FitImportService"
    }
    
    private val parser = FitFileParser(context)
    private val repository: RunDataRepository by lazy {
        RunDataRepositoryImpl(RunDatabase.getInstance(context))
    }
    
    /**
     * 导入FIT文件
     * @param uri 文件Uri
     * @return 导入结果
     */
    suspend fun importFitFile(uri: Uri): FitImportResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "========== 开始导入FIT文件 ==========")
        Log.i(TAG, "文件Uri: $uri")
        
        try {
            // 1. 获取文件名
            val fileName = getFileName(uri) ?: uri.lastPathSegment ?: "unknown.fit"
            Log.i(TAG, "文件名: $fileName")
            
            // 2. 解析FIT文件
            Log.i(TAG, "开始解析FIT文件...")
            val parseResult = parser.parse(uri).getOrElse { error ->
                Log.e(TAG, "解析失败: ${error.message}")
                return@withContext FitImportResult.Error("解析失败: ${error.message}")
            }
            
            // 3. 验证解析结果
            if (parseResult.session == null) {
                Log.e(TAG, "FIT文件中没有Session数据")
                return@withContext FitImportResult.Error("无效的FIT文件：缺少运动数据")
            }
            
            Log.i(TAG, "解析成功: records=${parseResult.records.size}, laps=${parseResult.laps.size}")
            
            // 4. 去重检查
            val originId = fileName
            Log.i(TAG, "检查是否已导入: originId=$originId")
            
            if (repository.existsByOriginId(originId, FitDataConverter.Datasource.LOCAL_FIT)) {
                Log.w(TAG, "文件已导入过，跳过")
                return@withContext FitImportResult.AlreadyExists
            }
            
            // 5. 转换为Entity
            Log.i(TAG, "开始转换数据...")
            val runRecord = FitDataMapper.toRunRecordEntity(parseResult, fileName)
            if (runRecord == null) {
                Log.e(TAG, "转换RunRecord失败")
                return@withContext FitImportResult.Error("数据转换失败")
            }
            
            val samplePoints = FitDataMapper.toSamplePointEntities(
                parseResult, 
                runRecord.workoutId, 
                runRecord.startTime
            )
            
            val segments = FitDataMapper.toSegmentEntities(parseResult, runRecord.workoutId)
            
            Log.i(TAG, """
                转换完成:
                - workoutId: ${runRecord.workoutId}
                - 距离: ${String.format("%.2f", runRecord.totalDistance)}km
                - 时长: ${String.format("%.1f", runRecord.activeDuration)}min
                - 采样点: ${samplePoints.size}
                - 分段: ${segments.size}
            """.trimIndent())
            
            // 6. 保存到数据库
            Log.i(TAG, "开始保存数据...")
            repository.saveRunRecord(
                record = runRecord,
                samplePoints = samplePoints,
                segments = segments,
                zones = emptyList()  // 区间数据需要额外计算，暂时为空
            )
            
            Log.i(TAG, "========== FIT文件导入成功 ==========")
            
            FitImportResult.Success(
                distance = runRecord.totalDistance,
                duration = runRecord.activeDuration
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "导入失败", e)
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
            Log.e(TAG, "获取文件名失败", e)
            null
        }
    }
}

