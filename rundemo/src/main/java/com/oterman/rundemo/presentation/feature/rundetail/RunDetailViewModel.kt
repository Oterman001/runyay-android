package com.oterman.rundemo.presentation.feature.rundetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.FitDownloadRepository
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
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
    private val fitDownloadRepository: FitDownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunDetailUiState())
    val uiState: StateFlow<RunDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /**
     * 加载跑步数据
     */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = RunDetailUiState(isLoading = true)

            try {
                val detailData = repository.getRunDetail(workoutId)
                if (detailData == null) {
                    _uiState.value = RunDetailUiState(isLoading = false, error = "未找到该跑步记录")
                    return@launch
                }

                val record = detailData.record

                // 获取GPS轨迹点
                val trackPoints = repository.getTrackPoints(workoutId)

                // 获取公里分段
                val segments = repository.getKilometerSegments(workoutId)

                // 获取训练分段
                val trainingSegments = repository.getTrainingSegments(workoutId)

                // 构建指标列表
                val metrics = buildMetricsList(record)

                // 判断是否为户外跑（有GPS轨迹）
                val isOutdoor = trackPoints.isNotEmpty()

                // 加载图表数据
                val heartRateSeries = repository.getHeartRateSeries(workoutId)
                val speedSeries = repository.getSpeedSeries(workoutId)
                val cadenceSeries = repository.getCadenceSeries(workoutId)
                val powerSeries = repository.getPowerSeries(workoutId)
                val strideLengthSeries = repository.getStrideLengthSeries(workoutId)
                val verticalOscillationSeries = repository.getVerticalOscillationSeries(workoutId)
                val contactTimeSeries = repository.getContactTimeSeries(workoutId)
                val altitudeSeries = repository.getAltitudeSeries(workoutId)

                // 加载区间数据（同时加载7区间和5区间）
                val heartRate7Zones = repository.getHeartRate7Zones(workoutId)
                val heartRate5Zones = repository.getHeartRate5Zones(workoutId)
                val speedZones = repository.getSpeedZones(workoutId)

                _uiState.value = RunDetailUiState(
                    isLoading = false,
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
                    trainingSegments = trainingSegments
                )
            } catch (e: Exception) {
                _uiState.value = RunDetailUiState(isLoading = false, error = e.message ?: "加载失败")
            }
        }
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
                label = "动态跑力"
            )
        )

        // 3. 平均配速（始终显示）
        metrics.add(
            RunMetricItem(
                value = formatPace(record.averageSpeed),
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
                    unit = "TL"
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

        // 10. 垂直步幅比 - 暂不实现（无字段）

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
        if (paceMinPerKm <= 0) return "-"
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return "${minutes}'${seconds.toString().padStart(2, '0')}\""
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
            return RunDetailViewModel(workoutId, repository, fitDownloadRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
