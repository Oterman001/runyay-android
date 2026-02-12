package com.oterman.rundemo.data.fit

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
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
            
            // 解析暂停事件（用于公里分段计算）
            val pauseList = FitEventConverter.convertEventStrToPauseList(runRecord.eventStr)
            
            val segments = FitDataMapper.toSegmentEntities(
                parseResult = parseResult,
                workoutId = runRecord.workoutId,
                startTimeMs = runRecord.startTime,
                maxHR = runRecord.maxHeartRate.takeIf { it > 0 } ?: 200.0,
                restHR = 60.0,  // TODO: 后续从用户设置获取
                pauseList = pauseList
            )
            
            // 6. 计算心率区间
            Log.i(TAG, "开始计算心率区间...")
            val maxHR = runRecord.maxHeartRate.takeIf { it > 0 } ?: 190.0
            val restHR = 60.0  // TODO: 后续从用户设置获取
            
            val zones = mutableListOf<com.oterman.rundemo.data.local.entity.RunAbilityZoneEntity>()
            
            // 心率7区间 + 训练负荷
            val (hr7Zones, trainLoad) = AbilityZoneCalculator.calculateHeartRate7Zones(
                records = parseResult.records,
                workoutId = runRecord.workoutId,
                startTimeMs = runRecord.startTime,
                restHR = restHR,
                maxHR = maxHR,
                pauseList = pauseList
            )
            zones.addAll(hr7Zones)
            
            // 心率5区间
            val hr5Zones = AbilityZoneCalculator.calculateHeartRate5Zones(
                records = parseResult.records,
                workoutId = runRecord.workoutId,
                startTimeMs = runRecord.startTime,
                restHR = restHR,
                maxHR = maxHR,
                pauseList = pauseList
            )
            zones.addAll(hr5Zones)
            
            // 更新训练负荷到记录
            var updatedRunRecord = if (trainLoad > 0 && runRecord.trainingLoad <= 0) {
                runRecord.copy(trainingLoad = trainLoad)
            } else {
                runRecord
            }
            
            // 7. 计算VDOT
            Log.i(TAG, "开始计算VDOT...")
            val vdotResult = calculateAndSaveVdot(
                runRecord = updatedRunRecord,
                segments = segments,
                maxHR = maxHR,
                restHR = restHR
            )
            if (vdotResult != null) {
                updatedRunRecord = updatedRunRecord.copy(
                    vdot = vdotResult.first,
                    overallVdot = vdotResult.second
                )
                
                // 8. 用VDOT计算配速区间
                Log.i(TAG, "开始计算配速区间...")
                val speedZones = AbilityZoneCalculator.calculateSpeedZones(
                    records = parseResult.records,
                    workoutId = updatedRunRecord.workoutId,
                    startTimeMs = updatedRunRecord.startTime,
                    vdot = vdotResult.first,
                    pauseList = pauseList
                )
                zones.addAll(speedZones)
                Log.i(TAG, "配速区间计算完成: ${speedZones.size}个区间")
            }
            
            Log.i(TAG, """
                转换完成:
                - workoutId: ${updatedRunRecord.workoutId}
                - 距离: ${String.format("%.2f", updatedRunRecord.totalDistance)}km
                - 时长: ${String.format("%.1f", updatedRunRecord.activeDuration)}min
                - 采样点: ${samplePoints.size}
                - 分段: ${segments.size}
                - 区间: ${zones.size} (HR7=${hr7Zones.size}, HR5=${hr5Zones.size})
                - 训练负荷: ${String.format("%.1f", trainLoad)}
                - VDOT: ${String.format("%.1f", updatedRunRecord.vdot)}
                - OverallVDOT: ${String.format("%.1f", updatedRunRecord.overallVdot)}
            """.trimIndent())
            
            // 9. 保存到数据库
            Log.i(TAG, "开始保存数据...")
            repository.saveRunRecord(
                record = updatedRunRecord,
                samplePoints = samplePoints,
                segments = segments,
                zones = zones
            )
            
            // 10. 保存VDOT到OverallVdot表
            if (vdotResult != null) {
                saveOverallVdot(updatedRunRecord, vdotResult.first, vdotResult.second)
            }
            
            // 11. 计算并保存PB记录
            Log.i(TAG, "开始计算PB...")
            calculateAndSavePB(updatedRunRecord, segments)
            
            Log.i(TAG, "========== FIT文件导入成功 ==========")
            
            FitImportResult.Success(
                distance = updatedRunRecord.totalDistance,
                duration = updatedRunRecord.activeDuration
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "导入失败", e)
            FitImportResult.Error("导入失败: ${e.message}")
        }
    }
    
    /**
     * 计算VDOT（支持间歇训练）
     * 对齐iOS GarminFitImportService.calculateAndSaveVdot
     *
     * @param runRecord 跑步记录
     * @param segments 训练分段列表
     * @param maxHR 最大心率
     * @param restHR 静息心率
     * @return Pair<vdot, overallVdot>，无法计算时返回null
     */
    private suspend fun calculateAndSaveVdot(
        runRecord: RunRecordEntity,
        segments: List<RunSegmentEntity>,
        maxHR: Double,
        restHR: Double
    ): Pair<Double, Double>? {
        // 数据不足，跳过VDOT计算
        if (runRecord.totalDistance <= 0 || runRecord.activeDuration <= 0 || runRecord.averageHeartRate <= 0) {
            Log.w(TAG, "数据不足，跳过VDOT计算: distance=${runRecord.totalDistance}, duration=${runRecord.activeDuration}, hr=${runRecord.averageHeartRate}")
            return null
        }

        // 1. 检查是否有间歇训练分段
        val hasIntervalTraining = segments.any { seg ->
            val type = seg.intervalType
            !type.isNullOrEmpty() && (type == "work" || type == "warmup" || type == "cooldown")
        }

        var vdot = 0.0
        var useIntervalVdot = false

        if (hasIntervalTraining) {
            // 1a. 间歇训练：使用分段计算VDOT
            Log.i(TAG, "检测到间歇训练，使用分段计算VDOT")
            val segmentVdot = VdotCalculator.calculateFromSegments(
                segments = segments,
                temperature = runRecord.weatherTemperature.takeIf { it != 0.0 },
                maxHR = maxHR,
                restHR = restHR
            )
            if (segmentVdot != null) {
                vdot = segmentVdot
                useIntervalVdot = true
                Log.i(TAG, "间歇训练VDOT计算成功: $vdot")
            } else {
                Log.w(TAG, "间歇训练VDOT计算失败，使用整体数据计算")
            }
        }

        // 1b. 普通跑步或间歇训练计算失败：使用整体数据计算
        if (!useIntervalVdot) {
            vdot = VdotCalculator.calculateFromDistanceAndTime(
                distanceMeters = runRecord.totalDistance * 1000, // 公里转米
                timeMinute = runRecord.activeDuration,
                heartRate = runRecord.averageHeartRate,
                temperature = runRecord.weatherTemperature.takeIf { it != 0.0 },
                maxHR = maxHR,
                restHR = restHR
            )
            Log.i(TAG, "整体VDOT计算: $vdot")
        }

        if (vdot <= 0) {
            Log.w(TAG, "VDOT计算结果无效: $vdot")
            return null
        }

        Log.i(TAG, "最终VDOT: $vdot")

        // 2. 获取历史45天VDOT数据并计算OverallVDOT
        val historyStartDate = runRecord.startTime - 45L * 24 * 60 * 60 * 1000 // 45天前
        val hisVdotList = repository.getVdotsByDateRange(historyStartDate, runRecord.startTime)

        val overallVdot = VdotCalculator.calculateOverallVdot(
            hisVdotList = hisVdotList,
            originVdot = vdot,
            totalDistance = runRecord.totalDistance,
            activeDuration = runRecord.activeDuration
        ) ?: vdot // 如果Overall计算失败，使用原始VDOT

        Log.i(TAG, "OverallVDOT计算: vdot=$vdot, overallVdot=$overallVdot")
        return Pair(vdot, overallVdot)
    }

    /**
     * 保存VDOT到OverallVdot表
     * 对齐iOS VdotDataManager.buildAndSaveOverallVdot
     */
    private suspend fun saveOverallVdot(
        runRecord: RunRecordEntity,
        vdot: Double,
        overallVdot: Double
    ) {
        try {
            val entity = com.oterman.rundemo.data.local.entity.OverallVdotEntity(
                workoutId = runRecord.workoutId,
                date = runRecord.startTime,
                originValue = vdot,
                value = overallVdot,
                inclusiveLevel = runRecord.inclusiveLevel
            )
            repository.saveOverallVdot(entity)
            Log.i(TAG, "OverallVdot保存成功: workoutId=${runRecord.workoutId}")
        } catch (e: Exception) {
            Log.e(TAG, "OverallVdot保存失败", e)
        }
    }

    /**
     * 计算并保存PB记录
     * 对齐iOS RunDataDetailSaveManager.calculatePBandSave
     *
     * @param runRecord 跑步记录
     * @param segments 分段列表（含公里分段和训练分段）
     */
    private suspend fun calculateAndSavePB(
        runRecord: RunRecordEntity,
        segments: List<RunSegmentEntity>
    ) {
        try {
            // 1. 计算当前跑步的所有PB候选值
            val currentPBs = PBCalculator.calculateCurrentPBs(runRecord, segments)
            if (currentPBs.isEmpty()) {
                Log.w(TAG, "PB计算结果为空")
                return
            }

            // 2. 逐个与历史PB对比，如果是新PB则保存
            var newPBCount = 0
            for (pb in currentPBs) {
                val isBetter = repository.savePBIfBetter(pb)
                if (isBetter) {
                    newPBCount++
                    Log.i(TAG, "新PB! ${pb.type}/${pb.subType} = ${String.format("%.2f", pb.value)}")
                }
            }

            Log.i(TAG, "PB计算保存完成: 共${currentPBs.size}项候选，${newPBCount}项为新PB")
        } catch (e: Exception) {
            Log.e(TAG, "PB计算保存失败", e)
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

