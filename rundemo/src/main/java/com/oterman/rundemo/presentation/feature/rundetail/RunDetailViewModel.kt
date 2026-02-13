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

                // 加载区间数据
                val heartRateZones = repository.getHeartRate5Zones(workoutId)
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
                    heartRateZones = heartRateZones,
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
     */
    private fun buildMetricsList(record: com.oterman.rundemo.data.local.entity.RunRecordEntity): List<RunMetricItem> {
        val metrics = mutableListOf<RunMetricItem>()

        // 时长
        metrics.add(
            RunMetricItem(
                value = formatDuration(record.activeDuration),
                label = "时长"
            )
        )

        // 配速
        metrics.add(
            RunMetricItem(
                value = formatPace(record.averageSpeed),
                label = "配速",
                unit = "/km"
            )
        )

        // 心率
        if (record.averageHeartRate > 0) {
            metrics.add(
                RunMetricItem(
                    value = record.averageHeartRate.toInt().toString(),
                    label = "心率",
                    unit = "bpm"
                )
            )
        }

        // 步频
        if (record.averageCadence > 0) {
            metrics.add(
                RunMetricItem(
                    value = record.averageCadence.toInt().toString(),
                    label = "步频",
                    unit = "spm"
                )
            )
        }

        // 步幅
        if (record.averageStrideLength > 0) {
            metrics.add(
                RunMetricItem(
                    value = String.format("%.2f", record.averageStrideLength),
                    label = "步幅",
                    unit = "m"
                )
            )
        }

        // 功率
        if (record.averagePower > 0) {
            metrics.add(
                RunMetricItem(
                    value = record.averagePower.toInt().toString(),
                    label = "功率",
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
