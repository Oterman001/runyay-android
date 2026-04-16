package com.oterman.rundemo.presentation.feature.rundetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.RunDetailPreferences
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.repository.AvatarManager
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.data.repository.FitDownloadRepository
import com.oterman.rundemo.data.repository.HealthRepository
import com.oterman.rundemo.data.fit.VdotRecalculationService
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.data.repository.RunningShoeRepository
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.network.dto.request.toUpdateRequest
import com.oterman.rundemo.service.sync.UnifiedDataSyncManager
import com.oterman.rundemo.util.RLog
import com.oterman.rundemo.domain.model.CHART_SMOOTH_ENABLED
import com.oterman.rundemo.domain.model.smoothed
import com.oterman.rundemo.domain.model.IntervalType
import com.oterman.rundemo.domain.model.MergedRunSegment
import com.oterman.rundemo.domain.model.RunSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 跑步详情页ViewModel
 * 用于用户友好的详情展示页面
 */
class RunDetailViewModel(
    private val workoutId: String,
    private val repository: RunDataRepository,
    private val fitDownloadRepository: FitDownloadRepository,
    private val avatarManager: AvatarManager,
    private val preferencesManager: PreferencesManager,
    private val healthRepository: HealthRepository,
    private val syncManager: UnifiedDataSyncManager,
    private val shoeRepository: RunningShoeRepository,
    private val runDetailPreferences: RunDetailPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "RunDetailViewModel"
    }

    private val vdotRecalculationService = VdotRecalculationService(repository)

    private val _uiState = MutableStateFlow(
        RunDetailUiState(
            segmentBarChartMode = runDetailPreferences.getSegmentBarChartMode(),
            segmentBarChartMetricIndex = runDetailPreferences.getSegmentMetricIndex()
        )
    )
    val uiState: StateFlow<RunDetailUiState> = _uiState.asStateFlow()

    private val _expandedSegmentIds = mutableSetOf<String>()

    init {
        loadData()
        loadAvatar()
        loadUserName()
    }

    /**
     * 加载跑步数据
     */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val detailData = repository.getRunDetail(workoutId)
                if (detailData == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "未找到该跑步记录")
                    return@launch
                }

                val record = detailData.record

                // 获取GPS轨迹点
                val trackPoints = repository.getTrackPoints(workoutId)

                // 获取公里分段
                val segments = repository.getKilometerSegments(workoutId).let { list ->
                    // 标记最快完整公里（distance >= 1.0km，不足1km的尾段不参与评比；averageSpeed 最小即最快）
                    val fastestSeq = list
                        .filter { it.distance >= 1.0 && it.averageSpeed > 0 }
                        .minByOrNull { it.averageSpeed }
                        ?.seq
                    if (fastestSeq != null)
                        list.map { if (it.seq == fastestSeq) it.copy(isFastest = true) else it }
                    else
                        list
                }

                // 获取训练分段
                val trainingSegments = repository.getTrainingSegments(workoutId)

                // 创建合并分段数据
                val mergedTrainingSegments = createMergedSegmentData(trainingSegments)

                // 构建指标列表
                val metrics = buildMetricsList(record)

                // 判断是否为户外跑（有GPS轨迹）
                val isOutdoor = trackPoints.isNotEmpty()

                // 加载图表数据（移动平均降噪，受 CHART_SMOOTH_ENABLED 控制）
                val heartRateSeries = repository.getHeartRateSeries(workoutId).let { if (CHART_SMOOTH_ENABLED) it.smoothed(7) else it }
                val speedSeries = repository.getSpeedSeries(workoutId).let { if (CHART_SMOOTH_ENABLED) it.smoothed(9) else it }
                val cadenceSeries = repository.getCadenceSeries(workoutId).let { if (CHART_SMOOTH_ENABLED) it.smoothed(5) else it }
                val powerSeries = repository.getPowerSeries(workoutId).let { if (CHART_SMOOTH_ENABLED) it.smoothed(5) else it }
                val strideLengthSeries = repository.getStrideLengthSeries(workoutId).let { if (CHART_SMOOTH_ENABLED) it.smoothed(5) else it }
                val verticalOscillationSeries = repository.getVerticalOscillationSeries(workoutId).let { if (CHART_SMOOTH_ENABLED) it.smoothed(5) else it }
                val contactTimeSeries = repository.getContactTimeSeries(workoutId).let { if (CHART_SMOOTH_ENABLED) it.smoothed(5) else it }
                val altitudeSeries = repository.getAltitudeSeries(workoutId).let { if (CHART_SMOOTH_ENABLED) it.smoothed(7) else it }

                // 加载区间数据（同时加载7区间和5区间）
                val heartRate7Zones = repository.getHeartRate7Zones(workoutId)
                val heartRate5Zones = repository.getHeartRate5Zones(workoutId)
                val speedZones = repository.getSpeedZones(workoutId)

                // 从 daily_health 表获取当天 VO2Max（同平台比较）
                val runDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(record.startTime))
                val platformCode = record.datasource
                val vo2Max = if (!platformCode.isNullOrBlank()) {
                    healthRepository.getVo2MaxForDateByPlatform(platformCode, runDateStr)
                } else {
                    healthRepository.getVo2MaxForDate(runDateStr)
                }
                val previousVo2Max = if (vo2Max != null && !platformCode.isNullOrBlank()) {
                    healthRepository.getPreviousVo2MaxByPlatform(platformCode, runDateStr)
                } else if (vo2Max != null) {
                    healthRepository.getPreviousVo2Max(runDateStr)
                } else null

                // 加载关联跑鞋
                var linkedShoe = record.shoeId?.let { shoeRepository.getShoe(it) }

                // 新设备场景：shoeId 存在但本地无数据，尝试从服务端拉取后重试
                if (record.shoeId != null && linkedShoe == null) {
                    try {
                        shoeRepository.pullFromServer()
                        linkedShoe = shoeRepository.getShoe(record.shoeId!!)
                    } catch (e: Exception) {
                        RLog.e(TAG, "Failed to pull shoe data from server", e)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    record = record,
                    trackPoints = trackPoints,
                    segments = segments,
                    metrics = metrics,
                    isOutdoor = isOutdoor,
                    heartRateSeries = heartRateSeries,
                    speedSeries = speedSeries,
                    cadenceSeries = cadenceSeries,
                    powerSeries = powerSeries,
                    strideLengthSeries = strideLengthSeries,
                    verticalOscillationSeries = verticalOscillationSeries,
                    contactTimeSeries = contactTimeSeries,
                    altitudeSeries = altitudeSeries,
                    heartRate7Zones = heartRate7Zones,
                    heartRate5Zones = heartRate5Zones,
                    speedZones = speedZones,
                    trainingSegments = trainingSegments,
                    mergedTrainingSegments = mergedTrainingSegments,
                    expandedSegmentIds = _expandedSegmentIds.toSet(),
                    vo2Max = vo2Max,
                    previousVo2Max = previousVo2Max,
                    linkedShoe = linkedShoe
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    private fun loadAvatar() {
        val userId = preferencesManager.getUserId() ?: return
        if (!preferencesManager.isUserLoggedIn()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAvatar = true)
            avatarManager.getAvatarUrl(userId).onSuccess { url ->
                _uiState.value = _uiState.value.copy(avatarUrl = url, isLoadingAvatar = false)
            }.onFailure {
                RLog.e(TAG, "头像加载失败: ${it.message}")
                _uiState.value = _uiState.value.copy(isLoadingAvatar = false)
            }
        }
    }

    private fun loadUserName() {
        if (!preferencesManager.isUserLoggedIn()) return
        val userName = preferencesManager.getUserName()
        if (!userName.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(userName = userName)
        }
    }

    /**
     * 切换合并分段的展开/折叠状态
     */
    fun toggleSegmentExpansion(segmentId: String) {
        if (_expandedSegmentIds.contains(segmentId)) {
            _expandedSegmentIds.remove(segmentId)
        } else {
            _expandedSegmentIds.add(segmentId)
        }
        _uiState.value = _uiState.value.copy(
            expandedSegmentIds = _expandedSegmentIds.toSet()
        )
    }

    /**
     * 从训练分段创建合并分段数据（对标iOS createMergedSegmentData）
     */
    private fun createMergedSegmentData(segments: List<RunSegment>): List<MergedRunSegment> {
        if (segments.isEmpty()) return emptyList()

        val groupedSegments = groupContinuousSegments(segments)
        val allMerged = mutableListOf<MergedRunSegment>()

        for (group in groupedSegments) {
            if (group.size > 1 && canMerge(group)) {
                allMerged.add(MergedRunSegment.fromSegments(group))
            } else {
                for (segment in group) {
                    allMerged.add(MergedRunSegment.fromSingleSegment(segment))
                }
            }
        }

        return allMerged.sortedBy { it.firstSegmentSeq }
    }

    /**
     * 判断分段组是否可以合并
     */
    private fun canMerge(segments: List<RunSegment>): Boolean {
        val first = segments.firstOrNull() ?: return false
        val wktStepIndex = first.wktStepIndex ?: return false
        if (wktStepIndex == 0) return false

        return segments.all {
            it.wktStepIndex == wktStepIndex && it.intervalType == first.intervalType
        }
    }

    /**
     * 将分段按连续性分组
     */
    private fun groupContinuousSegments(segments: List<RunSegment>): List<List<RunSegment>> {
        if (segments.isEmpty()) return emptyList()

        val sorted = segments.sortedBy { it.seq }
        val groups = mutableListOf<MutableList<RunSegment>>()
        var currentGroup = mutableListOf(sorted[0])

        for (i in 1 until sorted.size) {
            if (shouldContinueGroup(sorted[i], sorted[i - 1])) {
                currentGroup.add(sorted[i])
            } else {
                groups.add(currentGroup)
                currentGroup = mutableListOf(sorted[i])
            }
        }
        groups.add(currentGroup)
        return groups
    }

    /**
     * 判断当前分段是否应与前一个分段继续分组
     */
    private fun shouldContinueGroup(current: RunSegment, previous: RunSegment): Boolean {
        if (current.seq != previous.seq + 1) return false
        if (current.wktStepIndex != previous.wktStepIndex) return false
        if (current.intervalType != previous.intervalType) return false
        val wktStepIndex = current.wktStepIndex ?: return false
        if (wktStepIndex == 0) return false
        return true
    }

    /**
     * 构建展示用的指标列表
     * 严格对标 iOS V3 initGridCellData() 顺序，最多12项，3列排布
     *
     * 顺序: 运动时间 → 动态跑力(VDOT) → 平均配速 → 运动负荷(条件) → 平均心率 → 最大心率
     *       → 平均步幅(条件) → 平均步频(条件) → 累计上升 → 消耗能量(条件) → 平均功率(条件)
     */
    private fun buildMetricsList(record: com.oterman.rundemo.data.local.entity.RunRecordEntity): List<RunMetricItem> {
        val metrics = mutableListOf<RunMetricItem>()

        // 1. 运动时间（始终显示）
        metrics.add(
            RunMetricItem(
                value = formatDuration(record.activeDuration),
                label = "运动时间"
            )
        )

        // 2. 动态跑力 VDOT（始终显示）
        val vdotValue = if (record.vdot > 0) {
            String.format("%.1f", record.vdot)
        } else {
            "-"
        }
        metrics.add(
            RunMetricItem(
                value = vdotValue,
                label = "动态跑力",
                isVdot = true
            )
        )

        // 3. 平均配速（始终显示，fallback从距离和时间计算）
        val paceValue = if (record.averageSpeed > 0) {
            record.averageSpeed
        } else if (record.totalDistance > 0 && record.activeDuration > 0) {
            record.activeDuration / record.totalDistance
        } else {
            0.0
        }
        metrics.add(
            RunMetricItem(
                value = formatPace(paceValue),
                label = "平均配速",
                unit = "/km"
            )
        )

        // 4. 运动负荷（条件：trainingLoad > 0）
        if (record.trainingLoad > 0) {
            metrics.add(
                RunMetricItem(
                    value = String.format("%.0f", record.trainingLoad),
                    label = "运动负荷",
                    unit = "TL",
                    tag = getTrainingLoadTag(record.trainingLoad)
                )
            )
        }

        // 5. 平均心率（始终显示）
        metrics.add(
            RunMetricItem(
                value = if (record.averageHeartRate > 0) record.averageHeartRate.toInt().toString() else "-",
                label = "平均心率",
                unit = "bpm"
            )
        )

        // 6. 最大心率（始终显示）
        metrics.add(
            RunMetricItem(
                value = if (record.maxHeartRate > 0) record.maxHeartRate.toInt().toString() else "-",
                label = "最大心率",
                unit = "bpm"
            )
        )

        // 7. 平均步幅（条件：averageStrideLength > 0）
        if (record.averageStrideLength > 0) {
            metrics.add(
                RunMetricItem(
                    value = String.format("%.0f", record.averageStrideLength),
                    label = "平均步幅",
                    unit = "cm"
                )
            )
        }

        // 8. 平均步频（条件：averageCadence > 0）
        if (record.averageCadence > 0) {
            metrics.add(
                RunMetricItem(
                    value = record.averageCadence.toInt().toString(),
                    label = "平均步频",
                    unit = "/min"
                )
            )
        }

        // 9. 累计上升（始终显示）
        metrics.add(
            RunMetricItem(
                value = if (record.elevationAscended > 0) String.format("%.0f", record.elevationAscended) else "-",
                label = "累计上升",
                unit = "m"
            )
        )

        // 10. 垂直步幅比（条件：averageStrideLength > 0 && averageVerticalOscillation > 0）
        if (record.averageStrideLength > 0 && record.averageVerticalOscillation > 0) {
            val ratio = record.averageVerticalOscillation / record.averageStrideLength
            val ratioPercent = ratio * 100
            metrics.add(
                RunMetricItem(
                    value = String.format("%.1f", ratioPercent),
                    label = "垂直步幅比",
                    unit = "%",
                    tag = getVerticalStrideRatioTag(ratioPercent)
                )
            )
        }

        // 11. 消耗能量（条件：totalCalories > 0）
        if (record.totalCalories > 0) {
            metrics.add(
                RunMetricItem(
                    value = String.format("%.0f", record.totalCalories),
                    label = "消耗能量",
                    unit = "kcal"
                )
            )
        }

        // 12. 平均功率（条件：averagePower > 0）
        if (record.averagePower > 0) {
            metrics.add(
                RunMetricItem(
                    value = record.averagePower.toInt().toString(),
                    label = "平均功率",
                    unit = "W"
                )
            )
        }

        return metrics
    }

    /**
     * 格式化时长 (分钟 -> H:MM:SS 或 MM:SS)
     */
    private fun formatDuration(durationMinutes: Double): String {
        if (durationMinutes <= 0) return "-"
        val totalSeconds = (durationMinutes * 60).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    /**
     * 格式化配速 (min/km -> 5'30")
     */
    private fun formatPace(paceMinPerKm: Double): String {
        RLog.d(TAG, "formatPace 原始值: $paceMinPerKm")
        if (paceMinPerKm <= 0 || paceMinPerKm > 30) return "-"
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return "${minutes}'${seconds.toString().padStart(2, '0')}\""
    }

    /**
     * 垂直步幅比 tag 等级
     */
    private fun getVerticalStrideRatioTag(ratioPercent: Double): RunPerformanceTag {
        return when {
            ratioPercent < 6 -> RunPerformanceTag("凌波鸭", 0xFF90CAF9, PerformTagType.STRIDE_RATIO)
            ratioPercent < 8 -> RunPerformanceTag("踏浪鸭", 0xFF4CAF50, PerformTagType.STRIDE_RATIO)
            ratioPercent < 10 -> RunPerformanceTag("轻羽鸭", 0xFFFFC107, PerformTagType.STRIDE_RATIO)
            ratioPercent < 12 -> RunPerformanceTag("稳健鸭", 0xFFFF9800, PerformTagType.STRIDE_RATIO)
            else -> RunPerformanceTag("蓄力鸭", 0xFFF44336, PerformTagType.STRIDE_RATIO)
        }
    }

    /**
     * 运动负荷 tag 等级
     */
    private fun getTrainingLoadTag(trainingLoad: Double): RunPerformanceTag {
        return when {
            trainingLoad < 50 -> RunPerformanceTag("很低", 0xFF90CAF9, PerformTagType.TRAINING_LOAD)
            trainingLoad < 120 -> RunPerformanceTag("较低", 0xFF4CAF50, PerformTagType.TRAINING_LOAD)
            trainingLoad < 250 -> RunPerformanceTag("中等", 0xFFFFC107, PerformTagType.TRAINING_LOAD)
            trainingLoad < 400 -> RunPerformanceTag("高", 0xFFFF9800, PerformTagType.TRAINING_LOAD)
            else -> RunPerformanceTag("很高", 0xFFF44336, PerformTagType.TRAINING_LOAD)
        }
    }

    /**
     * 设置分享准备中状态（截图进行时由 Screen 调用）
     */
    fun setPreparingShare(preparing: Boolean) {
        _uiState.value = _uiState.value.copy(isPreparingShare = preparing)
    }

    /**
     * 准备分享数据
     * 地图截图由 Screen 层在调用前完成并存入 ShareDataCache
     */
    fun prepareShareData() {
        _uiState.value = _uiState.value.copy(
            isPreparingShare = false,
            shareDataReady = true
        )
    }

    /**
     * 清除分享准备状态
     */
    fun clearShareState() {
        _uiState.value = _uiState.value.copy(
            isPreparingShare = false,
            shareDataReady = false
        )
    }

    fun updateSegmentBarChart(isBarChart: Boolean, metricIndex: Int) {
        _uiState.value = _uiState.value.copy(
            segmentBarChartMode = isBarChart,
            segmentBarChartMetricIndex = metricIndex
        )
        runDetailPreferences.saveSegmentBarChartMode(isBarChart)
        runDetailPreferences.saveSegmentMetricIndex(metricIndex)
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadData()
    }

    /**
     * 下载FIT文件
     */
    fun downloadFitFile() {
        val record = _uiState.value.record ?: return
        val originId = record.originId ?: return
        val platformCode = record.datasource ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadError = null,
                downloadedFitData = null,
                downloadSuccess = false
            )

            // 1. 获取下载URL
            val urlResult = fitDownloadRepository.getFitFileUrls(originId, platformCode)
            if (urlResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadError = urlResult.exceptionOrNull()?.message ?: "获取下载地址失败"
                )
                return@launch
            }

            // 2. 下载FIT数据
            val downloadResult = fitDownloadRepository.downloadFitData(urlResult.getOrThrow())
            if (downloadResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadError = downloadResult.exceptionOrNull()?.message ?: "下载失败"
                )
                return@launch
            }

            // 3. 下载成功，等待用户选择保存位置
            _uiState.value = _uiState.value.copy(
                isDownloading = false,
                downloadedFitData = downloadResult.getOrThrow(),
                downloadSuccess = true
            )
        }
    }

    /**
     * 清除下载状态
     */
    fun clearDownloadState() {
        _uiState.value = _uiState.value.copy(
            downloadedFitData = null,
            downloadError = null,
            downloadSuccess = false
        )
    }

    fun showDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = true)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
    }

    fun deleteRunRecord() {
        val record = _uiState.value.record ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showDeleteConfirmDialog = false,
                isDeleting = true,
                deleteError = null
            )
            val originId = record.originId
            val result = if (originId != null && record.datasource != null) {
                syncManager.deleteRunSummary(originId, workoutId)
            } else {
                try {
                    repository.deleteRunRecord(workoutId)
                    Result.success(true)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isDeleting = false, deleteSuccess = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        deleteError = e.message ?: "删除失败"
                    )
                }
            )
        }
    }

    fun clearDeleteState() {
        _uiState.value = _uiState.value.copy(
            deleteError = null,
            deleteSuccess = false
        )
    }

    fun showEditDistanceDialog() {
        val d = _uiState.value.record?.totalDistance ?: return
        _uiState.value = _uiState.value.copy(
            showEditDistanceDialog = true,
            editDistanceInput = String.format("%.2f", d),
            editDistanceError = null
        )
    }

    fun dismissEditDistanceDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDistanceDialog = false,
            editDistanceInput = "",
            editDistanceError = null
        )
    }

    fun onEditDistanceInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(editDistanceInput = input, editDistanceError = null)
    }

    fun confirmEditDistance() {
        val record = _uiState.value.record ?: return
        val newDistance = _uiState.value.editDistanceInput.replace(",", ".").toDoubleOrNull()
        if (newDistance == null || newDistance <= 0) {
            _uiState.value = _uiState.value.copy(editDistanceError = "请输入有效距离"); return
        }
        val baseline = if (record.originDistance > 0) record.originDistance else record.totalDistance
        val lower = baseline * 0.9; val upper = baseline * 1.1
        if (newDistance < lower || newDistance > upper) {
            _uiState.value = _uiState.value.copy(
                editDistanceError = String.format(
                    "须在原始距离 %.2f km 的 ±10%% 范围内（%.2f ~ %.2f）", baseline, lower, upper
                )
            ); return
        }
        viewModelScope.launch {
            try {
                val updated = record.copy(
                    totalDistance = newDistance,
                    originDistance = if (record.originDistance <= 0) record.totalDistance else record.originDistance,
                    uploadStatus = 0
                )
                repository.updateRunRecord(updated)
                uploadToServer(updated)
                _uiState.value = _uiState.value.copy(
                    showEditDistanceDialog = false, editDistanceInput = "", editDistanceError = null,
                    updateSuccess = true
                )
                loadData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(editDistanceError = e.message ?: "保存失败")
            }
        }
    }

    fun showEditInclusiveLevelDialog() {
        _uiState.value = _uiState.value.copy(showEditInclusiveLevelDialog = true)
    }

    fun dismissEditInclusiveLevelDialog() {
        _uiState.value = _uiState.value.copy(showEditInclusiveLevelDialog = false)
    }

    fun confirmEditInclusiveLevel(newLevel: Int) {
        val record = _uiState.value.record ?: return
        viewModelScope.launch {
            try {
                // 先更新pb_record和overall_vdot，确保Dashboard的Flow重新collect时数据已一致
                repository.updatePBInclusiveLevel(record.workoutId, newLevel)
                repository.updateVdotInclusiveLevel(record.workoutId, newLevel)

                val updatedRecord = record.copy(inclusiveLevel = newLevel, uploadStatus = 0)
                repository.updateRunRecord(updatedRecord)
                uploadToServer(updatedRecord)

                try {
                    vdotRecalculationService.onInclusiveLevelChanged(record.workoutId, newLevel)
                } catch (e: Exception) {
                    RLog.w(TAG, "VDOT级联重算失败: ${e.message}")
                }

                _uiState.value = _uiState.value.copy(showEditInclusiveLevelDialog = false, updateSuccess = true)
                loadData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(showEditInclusiveLevelDialog = false, updateError = e.message ?: "保存失败")
            }
        }
    }

    private suspend fun uploadToServer(record: RunRecordEntity) {
        if (record.originId == null) return
        try {
            val request = record.toUpdateRequest()
            val result = syncManager.updateRunSummary(request)
            if (result.isSuccess) {
                repository.updateRunRecord(record.copy(uploadStatus = 2))
                RLog.d(TAG, "编辑后上传服务器成功: ${record.originId}")
            } else {
                repository.updateRunRecord(record.copy(uploadStatus = 0))
                RLog.w(TAG, "编辑后上传服务器失败: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            repository.updateRunRecord(record.copy(uploadStatus = 0))
            RLog.w(TAG, "编辑后上传服务器异常: ${e.message}")
        }
    }

    // ==================== 跑鞋关联 ====================

    fun showShoeSelector() {
        viewModelScope.launch {
            try {
                val shoes = shoeRepository.getActiveShoesSync()
                if (shoes.isEmpty()) {
                    _uiState.value = _uiState.value.copy(showNoShoesGuide = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        availableShoes = shoes,
                        showShoeSelector = true
                    )
                }
            } catch (e: Exception) {
                RLog.e(TAG, "加载跑鞋列表失败: ${e.message}")
                _uiState.value = _uiState.value.copy(showNoShoesGuide = true)
            }
        }
    }

    fun dismissShoeSelector() {
        _uiState.value = _uiState.value.copy(showShoeSelector = false)
    }

    fun dismissNoShoesGuide() {
        _uiState.value = _uiState.value.copy(showNoShoesGuide = false)
    }

    fun changeShoe(newShoeId: String?) {
        val record = _uiState.value.record ?: return
        val oldShoeId = record.shoeId
        if (oldShoeId == newShoeId) {
            _uiState.value = _uiState.value.copy(showShoeSelector = false)
            return
        }

        viewModelScope.launch {
            try {
                shoeRepository.changeRecordShoe(workoutId, oldShoeId, newShoeId).getOrThrow()
                // 同步 shoeId 到服务端
                val updatedRecord = record.copy(shoeId = newShoeId, uploadStatus = 0)
                repository.updateRunRecord(updatedRecord)
                uploadToServer(updatedRecord)
                _uiState.value = _uiState.value.copy(
                    showShoeSelector = false,
                    updateSuccess = true
                )
                loadData()
            } catch (e: Exception) {
                RLog.e(TAG, "切换跑鞋失败: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    showShoeSelector = false,
                    updateError = e.message ?: "切换跑鞋失败"
                )
            }
        }
    }

    fun removeShoe() {
        changeShoe(null)
    }

    fun clearUpdateState() {
        _uiState.value = _uiState.value.copy(updateSuccess = false, updateError = null)
    }

    /**
     * 生成默认文件名
     */
    fun getDefaultFileName(): String {
        val record = _uiState.value.record
        val timestamp = record?.startTime?.let {
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date(it))
        } ?: "unknown"
        return "run_${timestamp}.fit"
    }
}

/**
 * RunDetailViewModel Factory
 */
class RunDetailViewModelFactory(
    private val context: Context,
    private val workoutId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunDetailViewModel::class.java)) {
            val database = RunDatabase.getInstance(context)
            val repository = RunDataRepositoryImpl.getInstance(database)
            val preferencesManager = PreferencesManager(context)
            val fitDownloadRepository = FitDownloadRepository(preferencesManager)
            val avatarManager = AvatarManager.getInstance(context)
            val dataSourcePreferences = DataSourcePreferences(context)
            val dataSourceRepository = DataSourceRepository(dataSourcePreferences, preferencesManager)
            val healthRepository = HealthRepository(database.dailyHealthDao(), dataSourceRepository, preferencesManager)
            val syncManager = UnifiedDataSyncManager.getInstance(context)
            val shoeRepository = RunningShoeRepository(context)
            val runDetailPreferences = com.oterman.rundemo.data.local.RunDetailPreferences(context)
            return RunDetailViewModel(workoutId, repository, fitDownloadRepository, avatarManager, preferencesManager, healthRepository, syncManager, shoeRepository, runDetailPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
