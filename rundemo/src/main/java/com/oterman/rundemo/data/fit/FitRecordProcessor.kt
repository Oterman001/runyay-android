package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.local.entity.OverallVdotEntity
import com.oterman.rundemo.data.local.entity.PBRecordEntity
import com.oterman.rundemo.data.local.entity.RunAbilityZoneEntity
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.local.entity.RunSamplePointEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.domain.model.ImportedRunSummary
import com.oterman.rundemo.util.RLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FIT数据处理结果
 * 包含所有计算结果，用于后续持久化
 */
data class FitProcessResult(
    val runRecord: RunRecordEntity,
    val samplePoints: List<RunSamplePointEntity>,
    val segments: List<RunSegmentEntity>,
    val zones: List<RunAbilityZoneEntity>,
    val vdotEntity: OverallVdotEntity?,
    val pbRecords: List<PBRecordEntity>,
    val trainLoad: Double
)

/**
 * 用户生理参数配置
 * 用于心率区间和VDOT计算
 */
data class UserPhysiologyConfig(
    val maxHR: Double = 190.0,
    val restHR: Double = 60.0,
    val isMale: Boolean = true
)

/**
 * FIT数据统一处理器
 * 封装FIT文件解析后的完整处理流程：
 * 1. 数据转换 (FitParseResult -> Entity)
 * 2. 计算心率区间 (7区间 + 5区间)
 * 3. 计算配速区间
 * 4. 计算VDOT和OverallVDOT
 * 5. 计算PB记录
 *
 * 设计原则：
 * - 接收已解析的FitParseResult，不负责解析
 * - 支持灵活的用户生理参数配置
 * - 数据库操作通过Repository进行，保证事务一致性
 */
class FitRecordProcessor(
    private val repository: RunDataRepository
) {
    companion object {
        private const val TAG = "FitRecordProcessor"

        val DEFAULT_CONFIG = UserPhysiologyConfig()
    }

    /**
     * 处理FIT解析结果（完整流程）
     *
     * @param parseResult FIT解析结果
     * @param originId 数据源原始ID（用于去重和标识）
     * @param datasource 数据源标识（如 LOCAL_FIT, GCN, COROS 等）
     * @param userConfig 用户生理参数配置（可选，默认使用标准值）
     * @param deviceInfo 设备信息覆盖（可选，用于同步场景）
     * @return FitProcessResult 完整处理结果，失败返回null
     */
    suspend fun processFitData(
        parseResult: FitParseResult,
        originId: String,
        datasource: String,
        userConfig: UserPhysiologyConfig = DEFAULT_CONFIG,
        deviceInfo: String? = null,
        workoutIdFileName: String? = null,
        skipOverallVdot: Boolean = false
    ): FitProcessResult? {
        RLog.i(TAG, "========== 开始处理FIT数据 ==========")
        RLog.i(TAG, "originId: $originId, datasource: $datasource")

        // Step 1: 转换为RunRecordEntity
        var runRecord = FitDataMapper.toRunRecordEntity(parseResult, workoutIdFileName ?: originId)
        if (runRecord == null) {
            RLog.e(TAG, "转换RunRecord失败")
            return null
        }

        // 更新数据源信息
        runRecord = runRecord.copy(
            datasource = datasource,
            originId = originId,
            deviceInfo = deviceInfo ?: runRecord.deviceInfo
        )

        // Step 2: 转换SamplePoints
        val samplePoints = FitDataMapper.toSamplePointEntities(
            parseResult,
            runRecord.workoutId,
            runRecord.startTime
        )

        // Step 3: 解析暂停事件
        val pauseList = FitEventConverter.convertEventStrToPauseList(runRecord.eventStr)

        // Step 4: 确定maxHR和restHR（完全由userConfig提供，即用户生理最大心率）
        // 注意：runRecord.maxHeartRate 是本次运动峰值，不代表用户生理极限，不应参与区间计算
        val maxHR = userConfig.maxHR
        val restHR = userConfig.restHR

        // Step 5: 转换Segments（完整版，含分段类型推断）
        val segments = FitDataMapper.toSegmentEntities(
            parseResult = parseResult,
            workoutId = runRecord.workoutId,
            startTimeMs = runRecord.startTime,
            maxHR = maxHR,
            restHR = restHR,
            pauseList = pauseList
        )

        // Step 6: 计算心率7区间 + 训练负荷
        val (hr7Zones, trainLoad) = AbilityZoneCalculator.calculateHeartRate7Zones(
            records = parseResult.records,
            workoutId = runRecord.workoutId,
            startTimeMs = runRecord.startTime,
            restHR = restHR,
            maxHR = maxHR,
            pauseList = pauseList,
            isMale = userConfig.isMale
        )

        // Step 7: 计算心率5区间
        val hr5Zones = AbilityZoneCalculator.calculateHeartRate5Zones(
            records = parseResult.records,
            workoutId = runRecord.workoutId,
            startTimeMs = runRecord.startTime,
            restHR = restHR,
            maxHR = maxHR,
            pauseList = pauseList
        )

        val zones = mutableListOf<RunAbilityZoneEntity>()
        zones.addAll(hr7Zones)
        zones.addAll(hr5Zones)

        // Step 8: 更新训练负荷到record
        if (trainLoad > 0 && runRecord.trainingLoad <= 0) {
            runRecord = runRecord.copy(trainingLoad = trainLoad)
        }

        // Step 9: 计算VDOT
        var vdotEntity: OverallVdotEntity? = null
        val vdotCalcResult = calculateVdot(runRecord, segments, maxHR, restHR, skipOverallVdot)
        if (vdotCalcResult != null) {
            runRecord = runRecord.copy(
                vdot = vdotCalcResult.vdot,
                overallVdot = vdotCalcResult.overallVdot
            )

            // 构建OverallVdotEntity
            vdotEntity = OverallVdotEntity(
                workoutId = runRecord.workoutId,
                date = runRecord.startTime,
                originValue = vdotCalcResult.vdot,
                value = vdotCalcResult.overallVdot,
                confidence = vdotCalcResult.confidence,
                inclusiveLevel = runRecord.inclusiveLevel
            )

            // Step 10: 用VDOT计算配速区间
            val speedZones = AbilityZoneCalculator.calculateSpeedZones(
                records = parseResult.records,
                workoutId = runRecord.workoutId,
                startTimeMs = runRecord.startTime,
                vdot = vdotCalcResult.vdot,
                pauseList = pauseList
            )
            zones.addAll(speedZones)
        }

        // Step 11: 计算PB
        val pbRecords = PBCalculator.calculateCurrentPBs(runRecord, segments)

        RLog.i(TAG, """
            处理完成:
            - workoutId: ${runRecord.workoutId}
            - 距离: ${String.format("%.2f", runRecord.totalDistance)}km
            - 时长: ${String.format("%.1f", runRecord.activeDuration)}min
            - 采样点: ${samplePoints.size}
            - 分段: ${segments.size}
            - 区间: ${zones.size}
            - 训练负荷: ${String.format("%.1f", trainLoad)}
            - VDOT: ${String.format("%.1f", runRecord.vdot)}
            - PB候选: ${pbRecords.size}
        """.trimIndent())

        return FitProcessResult(
            runRecord = runRecord,
            samplePoints = samplePoints,
            segments = segments,
            zones = zones,
            vdotEntity = vdotEntity,
            pbRecords = pbRecords,
            trainLoad = trainLoad
        )
    }

    /**
     * 保存处理结果到数据库
     *
     * @param result FitProcessResult 处理结果
     */
    suspend fun saveProcessResult(result: FitProcessResult) {
        RLog.d(TAG, "开始保存数据到数据库...")

        // 1. 保存RunRecord + SamplePoints + Segments + Zones（事务操作）
        repository.saveRunRecord(
            record = result.runRecord,
            samplePoints = result.samplePoints,
            segments = result.segments,
            zones = result.zones
        )

        // 2. 保存OverallVdot
        result.vdotEntity?.let { entity ->
            repository.saveOverallVdot(entity)
            RLog.d(TAG, "OverallVdot保存成功: workoutId=${entity.workoutId}")
        }

        // 3. 保存PB记录（全量存储，幂等）
        repository.savePBRecords(result.pbRecords)
        RLog.d(TAG, "PB全量保存完成: 共${result.pbRecords.size}项")
    }

    /**
     * 处理并保存（便捷方法）
     * 组合 processFitData + saveProcessResult
     *
     * @return ImportedRunSummary 导入摘要，用于UI显示，失败返回null
     */
    suspend fun processAndSave(
        parseResult: FitParseResult,
        originId: String,
        datasource: String,
        userConfig: UserPhysiologyConfig = DEFAULT_CONFIG,
        deviceInfo: String? = null
    ): ImportedRunSummary? {
        val result = processFitData(parseResult, originId, datasource, userConfig, deviceInfo)
            ?: return null

        saveProcessResult(result)

        return ImportedRunSummary(
            originId = result.runRecord.originId ?: originId,
            platformCode = datasource,
            runDate = Date(result.runRecord.startTime),
            distance = result.runRecord.totalDistance,
            duration = result.runRecord.activeDuration,
            displayText = formatDisplayText(result.runRecord.startTime, result.runRecord.totalDistance)
        )
    }

    /**
     * VDOT计算结果
     */
    private data class VdotCalcResult(
        val vdot: Double,
        val overallVdot: Double,
        val confidence: Double
    )

    /**
     * VDOT计算（私有方法）
     *
     * @return VdotCalcResult，无法计算时返回null
     */
    private suspend fun calculateVdot(
        runRecord: RunRecordEntity,
        segments: List<RunSegmentEntity>,
        maxHR: Double,
        restHR: Double,
        skipOverallVdot: Boolean = false
    ): VdotCalcResult? {
        // 数据不足检查
        if (runRecord.totalDistance <= 0 || runRecord.activeDuration <= 0 || runRecord.averageHeartRate <= 0) {
            RLog.w(TAG, "数据不足，跳过VDOT计算: distance=${runRecord.totalDistance}, duration=${runRecord.activeDuration}, hr=${runRecord.averageHeartRate}")
            return null
        }

        val temperature = runRecord.weatherTemperature.takeIf { it != 0.0 }
        val humidity = runRecord.weatherHumidity.takeIf { it != 0.0 }

        // 1. 检查是否有间歇训练分段（"active" 是 Garmin FIT 中 intensity=0 的标准工作段类型）
        val hasIntervalTraining = segments.any { seg ->
            val type = seg.intervalType
            !type.isNullOrEmpty() && (type == "work" || type == "active" || type == "warmup" || type == "cooldown")
        }

        var vdotResult: VdotResult? = null

        // 始终计算整体 VDOT 作为基准
        val overallVdotResult = VdotCalculator.calculateWithResult(
            distanceMeters = runRecord.totalDistance * 1000,
            timeMinute = runRecord.activeDuration,
            heartRate = runRecord.averageHeartRate,
            temperature = temperature,
            humidity = humidity,
            maxHR = maxHR,
            restHR = restHR
        )
        RLog.i(TAG, "整体VDOT基准: ${overallVdotResult?.vdot}")

        if (hasIntervalTraining) {
            RLog.i(TAG, "检测到间歇训练，使用分段计算VDOT")
            val intervalVdotResult = VdotCalculator.calculateFromSegmentsWithResult(
                segments = segments,
                temperature = temperature,
                humidity = humidity,
                maxHR = maxHR,
                restHR = restHR
            )

            if (intervalVdotResult != null) {
                // 兜底：间歇算法结果不应低于整体算法结果
                if (overallVdotResult != null && intervalVdotResult.vdot < overallVdotResult.vdot) {
                    RLog.w(TAG, "间歇VDOT(${intervalVdotResult.vdot}) < 整体VDOT(${overallVdotResult.vdot})，使用整体VDOT")
                    vdotResult = overallVdotResult
                } else {
                    RLog.i(TAG, "间歇训练VDOT计算成功: ${intervalVdotResult.vdot}, confidence=${intervalVdotResult.confidence}")
                    vdotResult = intervalVdotResult
                }
            } else {
                RLog.w(TAG, "间歇训练VDOT计算失败，使用整体数据计算")
            }
        }

        // 普通跑步或间歇训练计算失败：使用整体数据
        if (vdotResult == null) {
            vdotResult = overallVdotResult
        }

        if (vdotResult == null || vdotResult.vdot <= 0) {
            RLog.w(TAG, "VDOT计算结果无效")
            return null
        }

        val vdot = vdotResult.vdot
        val confidence = vdotResult.confidence

        // skipOverallVdot模式：跳过历史查询和overallVdot计算，用单次值占位
        if (skipOverallVdot) {
            RLog.i(TAG, "跳过OverallVDOT计算(同步模式): vdot=$vdot, confidence=$confidence")
            return VdotCalcResult(vdot, vdot, confidence)
        }

        // 2. 获取历史45天VDOT数据并计算OverallVDOT
        val historyStartDate = runRecord.startTime - 45L * 24 * 60 * 60 * 1000 // 45天前
        val hisVdotList = repository.getVdotsByDateRange(historyStartDate, runRecord.startTime)

        // 获取上一次的综合VDOT（用于钳制）
        val previousOverallVdot = hisVdotList.firstOrNull()?.value

        val overallVdot = VdotCalculator.calculateOverallVdot(
            hisVdotList = hisVdotList,
            originVdot = vdot,
            currentConfidence = confidence,
            currentDateMs = runRecord.startTime,
            totalDistance = runRecord.totalDistance,
            activeDuration = runRecord.activeDuration,
            previousOverallVdot = previousOverallVdot
        ) ?: vdot // 如果Overall计算失败，使用原始VDOT

        RLog.i(TAG, "OverallVDOT计算: vdot=$vdot, overallVdot=$overallVdot, confidence=$confidence")
        return VdotCalcResult(vdot, overallVdot, confidence)
    }

    /**
     * 格式化显示文本
     */
    private fun formatDisplayText(startTime: Long, distance: Double): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(Date(startTime))
        val distanceStr = String.format(Locale.getDefault(), "%.1f", distance)
        return "已导入记录 $dateStr ${distanceStr}KM"
    }
}
