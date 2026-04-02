package com.oterman.rundemo.presentation.feature.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.data.repository.RunningShoeRepository
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.repository.HealthRepository
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.domain.model.IntervalType
import com.oterman.rundemo.domain.model.MergedRunSegment
import com.oterman.rundemo.domain.model.RunSegment
import com.oterman.rundemo.presentation.feature.rundetail.PerformTagType
import com.oterman.rundemo.presentation.feature.rundetail.RunMetricItem
import com.oterman.rundemo.presentation.feature.rundetail.RunPerformanceTag
import com.oterman.rundemo.presentation.feature.share.components.*
import com.oterman.rundemo.util.DeviceNameUtils
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 分享页面 ViewModel
 * 负责加载数据、管理分享状态、生成分享图片
 */
class ShareViewModel(
    private val workoutId: String,
    private val repository: RunDataRepository,
    private val sharePreferences: SharePreferences,
    private val healthRepository: HealthRepository,
    private val avatarManager: com.oterman.rundemo.data.repository.AvatarManager,
    private val preferencesManager: PreferencesManager,
    private val shoeRepository: RunningShoeRepository,
    private val isPrivacyMode: Boolean = false
) : ViewModel() {

    companion object {
        private const val TAG = "ShareViewModel"
    }

    private val _uiState = MutableStateFlow(ShareUiState(isPrivacyMode = isPrivacyMode))
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    init {
        loadSavedPreferences()
        loadData()
        loadAvatar()
    }

    private fun loadAvatar() {
        val userId = preferencesManager.getUserId() ?: return
        if (!preferencesManager.isUserLoggedIn()) return

        val userName = preferencesManager.getUserName()
        if (!userName.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(userName = userName)
        }

        viewModelScope.launch {
            avatarManager.getAvatarUrl(userId).onSuccess { url ->
                _uiState.value = _uiState.value.copy(avatarUrl = url)
            }.onFailure {
                RLog.e(TAG, "头像加载失败: ${it.message}")
            }
        }
    }

    /**
     * 从持久化加载用户偏好
     */
    private fun loadSavedPreferences() {
        val savedMetrics = sharePreferences.getSelectedMetrics()
        val savedCards = sharePreferences.getEnabledCards()
        val showDate = sharePreferences.getShowDate()
        val showNickname = sharePreferences.getShowNickname()
        val brandText = com.oterman.rundemo.presentation.feature.share.components.getRandomBrandText()
        val customDevice = sharePreferences.getCustomDeviceName()

        _uiState.value = _uiState.value.copy(
            selectedMetrics = savedMetrics ?: ShareUiState.defaultShortMetrics,
            enabledCards = savedCards,
            showDate = showDate,
            showNickname = showNickname,
            brandText = brandText,
            customDeviceName = customDevice
        )
    }

    /**
     * 加载跑步数据（与 RunDetailViewModel 类似）
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
                val trackPoints = repository.getTrackPoints(workoutId)
                val segments = repository.getKilometerSegments(workoutId)
                val trainingSegments = repository.getTrainingSegments(workoutId)
                val mergedTrainingSegments = createMergedSegmentData(trainingSegments)
                val metrics = buildMetricsList(record)
                val isOutdoor = trackPoints.isNotEmpty()  // 与 RunDetailViewModel 保持一致：有 GPS 轨迹即为户外

                // 图表数据
                val heartRateSeries = repository.getHeartRateSeries(workoutId)
                val speedSeries = repository.getSpeedSeries(workoutId)
                val cadenceSeries = repository.getCadenceSeries(workoutId)
                val powerSeries = repository.getPowerSeries(workoutId)
                val strideLengthSeries = repository.getStrideLengthSeries(workoutId)
                val verticalOscillationSeries = repository.getVerticalOscillationSeries(workoutId)
                val contactTimeSeries = repository.getContactTimeSeries(workoutId)
                val altitudeSeries = repository.getAltitudeSeries(workoutId)

                // 区间数据
                val heartRate7Zones = repository.getHeartRate7Zones(workoutId)
                val heartRate5Zones = repository.getHeartRate5Zones(workoutId)
                val speedZones = repository.getSpeedZones(workoutId)

                // VO2Max
                val runDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(record.startTime))
                val vo2Max = healthRepository.getVo2MaxForDate(runDateStr)
                val previousVo2Max = if (vo2Max != null) {
                    healthRepository.getPreviousVo2Max(runDateStr)
                } else null

                // 根据数据可用性构建可选指标列表
                val availableMetrics = buildAvailableMetrics(record)

                // 关联跑鞋
                val linkedShoe = record.shoeId?.let { shoeRepository.getShoe(it) }

                // 从缓存获取地图截图
                val mapSnapshot = ShareDataCache.takeMapSnapshot()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    record = record,
                    trackPoints = trackPoints,
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
                    segments = segments,
                    trainingSegments = trainingSegments,
                    mergedTrainingSegments = mergedTrainingSegments,
                    vo2Max = vo2Max,
                    previousVo2Max = previousVo2Max,
                    mapSnapshot = mapSnapshot,
                    availableMetrics = availableMetrics,
                    linkedShoe = linkedShoe
                )
            } catch (e: Exception) {
                RLog.e(TAG, "加载数据失败: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // ==================== 模式切换 ====================

    fun setShareMode(mode: ShareMode) {
        _uiState.value = _uiState.value.copy(shareMode = mode)
    }

    // ==================== 短图编辑 ====================

    fun updateSelectedMetrics(metrics: List<ShareMetricType>) {
        _uiState.value = _uiState.value.copy(selectedMetrics = metrics)
        sharePreferences.saveSelectedMetrics(metrics)
    }

    // ==================== 长图编辑 ====================

    fun toggleCard(cardType: ShareCardType, enabled: Boolean) {
        if (cardType == ShareCardType.HEADER) return // HEADER 始终显示
        val newCards = _uiState.value.enabledCards.toMutableMap()
        newCards[cardType] = enabled
        _uiState.value = _uiState.value.copy(enabledCards = newCards)
        sharePreferences.saveEnabledCards(newCards)
    }

    /**
     * 获取有数据的卡片列表（用于长图编辑面板）
     */
    fun getAvailableCards(): List<ShareCardType> {
        val state = _uiState.value
        return ShareCardType.entries.filter { cardType ->
            when (cardType) {
                ShareCardType.HEADER -> true
                ShareCardType.VO2MAX -> state.vo2Max != null && state.vo2Max > 0
                ShareCardType.TRAINING_EFFECT -> {
                    val record = state.record
                    record != null && (record.trainingEffect > 0 || record.anaerobicTrainingEffect > 0)
                }
                ShareCardType.KM_SEGMENTS -> state.segments.isNotEmpty()
                ShareCardType.TRAINING_SEGMENTS -> state.trainingSegments.isNotEmpty()
                ShareCardType.HEART_RATE -> state.heartRateSeries.isNotEmpty()
                ShareCardType.PACE -> state.speedSeries.isNotEmpty()
                ShareCardType.ALTITUDE -> state.altitudeSeries.isNotEmpty()
                ShareCardType.STRIDE_LENGTH -> state.strideLengthSeries.isNotEmpty()
                ShareCardType.CADENCE -> state.cadenceSeries.isNotEmpty()
                ShareCardType.CONTACT_TIME -> state.contactTimeSeries.isNotEmpty()
                ShareCardType.VERTICAL_OSCILLATION -> state.verticalOscillationSeries.isNotEmpty()
                ShareCardType.POWER -> state.powerSeries.isNotEmpty()
                ShareCardType.LINKED_SHOE -> state.linkedShoe != null
            }
        }
    }

    // ==================== 共用编辑 ====================

    fun updateShowDate(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDate = show)
        sharePreferences.saveShowDate(show)
    }

    fun updateShowNickname(show: Boolean) {
        _uiState.value = _uiState.value.copy(showNickname = show)
        sharePreferences.saveShowNickname(show)
    }

    fun updateHeartRateZoneMode(show7: Boolean) {
        _uiState.value = _uiState.value.copy(heartRateZone7Selected = show7)
    }

    fun updateDeviceName(name: String) {
        _uiState.value = _uiState.value.copy(customDeviceName = name.ifBlank { null })
        sharePreferences.saveCustomDeviceName(name.ifBlank { null })
    }

    fun updateBrandText(text: String) {
        val resolvedText = text.ifBlank {
            com.oterman.rundemo.presentation.feature.share.components.getRandomBrandText()
        }
        _uiState.value = _uiState.value.copy(brandText = resolvedText)
        // 不持久化自定义文案，下次进入时仍从内置文案随机选择
    }

    fun showEditSheet() {
        _uiState.value = _uiState.value.copy(showEditSheet = true)
    }

    fun hideEditSheet() {
        _uiState.value = _uiState.value.copy(showEditSheet = false)
    }

    // ==================== 分享图片生成 ====================

    private suspend fun renderShareBitmap(
        context: Context,
        state: ShareUiState,
        record: RunRecordEntity,
        darkTheme: Boolean
    ): Bitmap? {
        val widthPx = context.resources.displayMetrics.widthPixels
        return withContext(Dispatchers.Main) {
            when (state.shareMode) {
                ShareMode.SHORT -> ShareImageGenerator.renderToBitmap(context, widthPx, darkTheme) {
                    ShortSharePreview(
                        record = record,
                        mapSnapshot = state.mapSnapshot,
                        selectedMetrics = state.selectedMetrics,
                        showDate = state.showDate,
                        deviceName = state.customDeviceName ?: DeviceNameUtils.resolveDisplayName(record),
                        brandText = state.brandText,
                        avatarUrl = state.avatarUrl,
                        userName = state.userName,
                        showNickname = state.showNickname,
                        isPrivacyMode = state.isPrivacyMode,
                        trackPoints = state.trackPoints,
                        isIndoor = !state.isOutdoor
                    )
                }
                ShareMode.LONG -> ShareImageGenerator.renderToBitmap(context, widthPx, darkTheme) {
                    LongSharePreview(
                        record = record,
                        mapSnapshot = state.mapSnapshot,
                        metrics = state.metrics,
                        enabledCards = state.enabledCards,
                        segments = state.segments,
                        trainingSegments = state.trainingSegments,
                        mergedTrainingSegments = state.mergedTrainingSegments,
                        heartRateSeries = state.heartRateSeries,
                        speedSeries = state.speedSeries,
                        cadenceSeries = state.cadenceSeries,
                        powerSeries = state.powerSeries,
                        strideLengthSeries = state.strideLengthSeries,
                        verticalOscillationSeries = state.verticalOscillationSeries,
                        contactTimeSeries = state.contactTimeSeries,
                        altitudeSeries = state.altitudeSeries,
                        heartRate7Zones = state.heartRate7Zones,
                        heartRate5Zones = state.heartRate5Zones,
                        speedZones = state.speedZones,
                        vo2Max = state.vo2Max,
                        previousVo2Max = state.previousVo2Max,
                        showDate = state.showDate,
                        deviceName = state.customDeviceName ?: DeviceNameUtils.resolveDisplayName(record),
                        brandText = state.brandText,
                        avatarUrl = state.avatarUrl,
                        userName = state.userName,
                        showNickname = state.showNickname,
                        linkedShoe = state.linkedShoe,
                        isPrivacyMode = state.isPrivacyMode,
                        trackPoints = state.trackPoints,
                        heartRateZone7Selected = state.heartRateZone7Selected,
                        isIndoor = !state.isOutdoor
                    )
                }
                ShareMode.CUSTOM -> null
            }
        }
    }

    private fun buildGalleryFileName(state: ShareUiState): String {
        val idPart = workoutId.take(8).ifEmpty { "unknown" }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
        val modeSuffix = when (state.shareMode) {
            ShareMode.SHORT -> "short"
            ShareMode.LONG -> "long"
            else -> "other"
        }
        return "run_share_${idPart}_${ts}_${modeSuffix}.jpg"
    }

    fun generateAndShare(context: Context, darkTheme: Boolean = false) {
        val state = _uiState.value
        val record = state.record ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, shareError = null)

            try {
                val bitmap = renderShareBitmap(context, state, record, darkTheme)

                if (bitmap == null) {
                    _uiState.value = _uiState.value.copy(isGenerating = false)
                    return@launch
                }

                // 创建分享 Intent
                val shareIntent = ShareImageGenerator.createShareIntent(context, bitmap)
                context.startActivity(Intent.createChooser(shareIntent, "分享跑步记录"))

                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    generatedBitmap = bitmap
                )
            } catch (e: Exception) {
                RLog.e(TAG, "生成分享图片失败: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    shareError = "生成图片失败: ${e.message}"
                )
            }
        }
    }

    fun generateAndSave(context: Context, darkTheme: Boolean = false) {
        val state = _uiState.value
        val record = state.record ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                saveError = null,
                saveSuccess = false
            )

            try {
                if (state.shareMode == ShareMode.CUSTOM) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveError = "当前模式不支持保存图片"
                    )
                    return@launch
                }

                val bitmap = renderShareBitmap(context, state, record, darkTheme)
                if (bitmap == null) {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    return@launch
                }

                val fileName = buildGalleryFileName(state)
                val result = withContext(Dispatchers.IO) {
                    ShareImageGenerator.saveToGallery(context, bitmap, fileName)
                }

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            saveSuccess = true,
                            generatedBitmap = bitmap
                        )
                    },
                    onFailure = { e ->
                        RLog.e(TAG, "保存到相册失败: ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            saveError = e.message ?: "保存失败"
                        )
                    }
                )
            } catch (e: Exception) {
                RLog.e(TAG, "保存到相册失败: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveError = "保存失败: ${e.message}"
                )
            }
        }
    }

    fun clearSaveState() {
        _uiState.value = _uiState.value.copy(saveSuccess = false, saveError = null)
    }

    // ==================== 辅助方法 ====================

    private fun buildAvailableMetrics(record: com.oterman.rundemo.data.local.entity.RunRecordEntity): List<ShareMetricType> {
        val list = mutableListOf<ShareMetricType>()
        list.add(ShareMetricType.DISTANCE)
        list.add(ShareMetricType.DURATION)
        list.add(ShareMetricType.VDOT)
        list.add(ShareMetricType.PACE)
        if (record.trainingLoad > 0) list.add(ShareMetricType.TRAINING_LOAD)
        list.add(ShareMetricType.AVG_HEART_RATE)
        list.add(ShareMetricType.MAX_HEART_RATE)
        if (record.averageStrideLength > 0) list.add(ShareMetricType.AVG_STRIDE_LENGTH)
        if (record.averageCadence > 0) list.add(ShareMetricType.AVG_CADENCE)
        list.add(ShareMetricType.ELEVATION)
        if (record.averageStrideLength > 0 && record.averageVerticalOscillation > 0) {
            list.add(ShareMetricType.VERTICAL_STRIDE_RATIO)
        }
        if (record.totalCalories > 0) list.add(ShareMetricType.CALORIES)
        if (record.averagePower > 0) list.add(ShareMetricType.AVG_POWER)
        return list
    }

    private fun buildMetricsList(record: com.oterman.rundemo.data.local.entity.RunRecordEntity): List<RunMetricItem> {
        val metrics = mutableListOf<RunMetricItem>()

        metrics.add(RunMetricItem(value = formatDuration(record.activeDuration), label = "运动时间"))

        val vdotValue = if (record.vdot > 0) String.format("%.1f", record.vdot) else "-"
        metrics.add(RunMetricItem(value = vdotValue, label = "动态跑力", isVdot = true))

        val paceValue = if (record.averageSpeed > 0) record.averageSpeed
        else if (record.totalDistance > 0 && record.activeDuration > 0)
            record.activeDuration / record.totalDistance else 0.0
        metrics.add(RunMetricItem(value = formatPace(paceValue), label = "平均配速", unit = "/km"))

        if (record.trainingLoad > 0) {
            metrics.add(RunMetricItem(
                value = String.format("%.0f", record.trainingLoad), label = "运动负荷", unit = "TL",
                tag = getTrainingLoadTag(record.trainingLoad)
            ))
        }

        metrics.add(RunMetricItem(
            value = if (record.averageHeartRate > 0) record.averageHeartRate.toInt().toString() else "-",
            label = "平均心率", unit = "bpm"
        ))
        metrics.add(RunMetricItem(
            value = if (record.maxHeartRate > 0) record.maxHeartRate.toInt().toString() else "-",
            label = "最大心率", unit = "bpm"
        ))

        if (record.averageStrideLength > 0) {
            metrics.add(RunMetricItem(value = String.format("%.0f", record.averageStrideLength), label = "平均步幅", unit = "cm"))
        }
        if (record.averageCadence > 0) {
            metrics.add(RunMetricItem(value = record.averageCadence.toInt().toString(), label = "平均步频", unit = "/min"))
        }

        metrics.add(RunMetricItem(
            value = if (record.elevationAscended > 0) String.format("%.0f", record.elevationAscended) else "-",
            label = "累计上升", unit = "m"
        ))

        if (record.averageStrideLength > 0 && record.averageVerticalOscillation > 0) {
            val ratioPercent = record.averageVerticalOscillation / record.averageStrideLength * 100
            metrics.add(RunMetricItem(
                value = String.format("%.1f", ratioPercent), label = "垂直步幅比", unit = "%",
                tag = getVerticalStrideRatioTag(ratioPercent)
            ))
        }

        if (record.totalCalories > 0) {
            metrics.add(RunMetricItem(value = String.format("%.0f", record.totalCalories), label = "消耗能量", unit = "kcal"))
        }
        if (record.averagePower > 0) {
            metrics.add(RunMetricItem(value = record.averagePower.toInt().toString(), label = "平均功率", unit = "W"))
        }

        return metrics
    }

    private fun createMergedSegmentData(segments: List<RunSegment>): List<MergedRunSegment> {
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

        val allMerged = mutableListOf<MergedRunSegment>()
        for (group in groups) {
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

    private fun canMerge(segments: List<RunSegment>): Boolean {
        val first = segments.firstOrNull() ?: return false
        val wktStepIndex = first.wktStepIndex ?: return false
        if (wktStepIndex == 0) return false
        return segments.all { it.wktStepIndex == wktStepIndex && it.intervalType == first.intervalType }
    }

    private fun shouldContinueGroup(current: RunSegment, previous: RunSegment): Boolean {
        if (current.seq != previous.seq + 1) return false
        if (current.wktStepIndex != previous.wktStepIndex) return false
        if (current.intervalType != previous.intervalType) return false
        val wktStepIndex = current.wktStepIndex ?: return false
        return wktStepIndex != 0
    }

    private fun formatDuration(durationMinutes: Double): String {
        if (durationMinutes <= 0) return "-"
        val totalSeconds = (durationMinutes * 60).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%d:%02d", minutes, seconds)
    }

    private fun formatPace(paceMinPerKm: Double): String {
        if (paceMinPerKm <= 0 || paceMinPerKm > 30) return "-"
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return "${minutes}'${seconds.toString().padStart(2, '0')}\""
    }

    private fun getTrainingLoadTag(trainingLoad: Double): RunPerformanceTag {
        return when {
            trainingLoad < 50 -> RunPerformanceTag("很低", 0xFF90CAF9, PerformTagType.TRAINING_LOAD)
            trainingLoad < 120 -> RunPerformanceTag("较低", 0xFF4CAF50, PerformTagType.TRAINING_LOAD)
            trainingLoad < 250 -> RunPerformanceTag("中等", 0xFFFFC107, PerformTagType.TRAINING_LOAD)
            trainingLoad < 400 -> RunPerformanceTag("高", 0xFFFF9800, PerformTagType.TRAINING_LOAD)
            else -> RunPerformanceTag("很高", 0xFFF44336, PerformTagType.TRAINING_LOAD)
        }
    }

    private fun getVerticalStrideRatioTag(ratioPercent: Double): RunPerformanceTag {
        return when {
            ratioPercent < 6 -> RunPerformanceTag("凌波鸭", 0xFF90CAF9, PerformTagType.STRIDE_RATIO)
            ratioPercent < 8 -> RunPerformanceTag("踏浪鸭", 0xFF4CAF50, PerformTagType.STRIDE_RATIO)
            ratioPercent < 10 -> RunPerformanceTag("轻羽鸭", 0xFFFFC107, PerformTagType.STRIDE_RATIO)
            ratioPercent < 12 -> RunPerformanceTag("稳健鸭", 0xFFFF9800, PerformTagType.STRIDE_RATIO)
            else -> RunPerformanceTag("蓄力鸭", 0xFFF44336, PerformTagType.STRIDE_RATIO)
        }
    }
}

/**
 * ShareViewModel Factory
 */
class ShareViewModelFactory(
    private val context: Context,
    private val workoutId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShareViewModel::class.java)) {
            val database = RunDatabase.getInstance(context)
            val repository = RunDataRepositoryImpl.getInstance(database)
            val sharePreferences = SharePreferences(context)
            val preferencesManager = PreferencesManager(context)
            val dataSourcePreferences = DataSourcePreferences(context)
            val dataSourceRepository = DataSourceRepository(dataSourcePreferences, preferencesManager)
            val healthRepository = HealthRepository(database.dailyHealthDao(), dataSourceRepository, preferencesManager)
            val avatarManager = com.oterman.rundemo.data.repository.AvatarManager.getInstance(context)
            val shoeRepository = RunningShoeRepository(context)
            val isPrivacyMode = com.oterman.rundemo.presentation.feature.rundetail.components.RunMapPreferences.getPrivacyMode(context)
            return ShareViewModel(workoutId, repository, sharePreferences, healthRepository, avatarManager, preferencesManager, shoeRepository, isPrivacyMode) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
