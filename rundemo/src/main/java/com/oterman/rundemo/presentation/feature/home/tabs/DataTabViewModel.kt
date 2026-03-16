package com.oterman.rundemo.presentation.feature.home.tabs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.domain.model.DataTabDisplayMode
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.domain.model.MonthRangeData
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.data.fit.VdotRecalculationService
import com.oterman.rundemo.data.network.dto.request.toUpdateRequest
import com.oterman.rundemo.service.sync.UnifiedDataSyncManager
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

/**
 * DataTab UI状态
 * 支持按月分组和折叠展开
 */
data class DataTabUiState(
    val isLoading: Boolean = true,
    val monthGroups: List<MonthRangeData> = emptyList(),    // 按月分组数据
    val expandedMonths: Set<String> = emptySet(),            // 展开的月份ID集合 ("2025-2")
    val displayMode: DataTabDisplayMode = DataTabDisplayMode.HEATMAP,  // 显示模式
    val error: String? = null,
    val runRecords: List<RunRecordEntity> = emptyList(),     // 保留原有字段兼容
    val totalDistance: Double = 0.0,      // 总跑步距离 (km)
    val totalRunCount: Int = 0,           // 总跑步次数
    val totalDuration: Double = 0.0,      // 总跑步时长 (分钟)
    val selectedInclusiveLevels: Set<Int> = emptySet(),     // 筛选的统计分析级别，空=全部
    val selectedDatasources: Set<String> = emptySet(),      // 筛选的数据来源，空=全部
    val availableDatasources: List<String> = emptyList(),   // 从数据中提取的去重数据来源
    val isFilterActive: Boolean = false                     // 是否有活跃过滤
)

/**
 * DataTab ViewModel
 * 管理跑步记录列表的状态，支持按月分组和折叠展开
 */
class DataTabViewModel(
    private val repository: RunDataRepository,
    private val preferencesManager: PreferencesManager,
    private val syncManager: UnifiedDataSyncManager
) : ViewModel() {

    private val vdotRecalculationService = VdotRecalculationService(repository)

    private val _uiState = MutableStateFlow(DataTabUiState())
    val uiState: StateFlow<DataTabUiState> = _uiState.asStateFlow()

    // 保存未过滤的完整记录列表
    private var allRecords: List<RunRecordEntity> = emptyList()

    // 轨迹点版本号，用于触发UI重组
    private val _trackPointsVersion = MutableStateFlow(0L)
    val trackPointsVersion: StateFlow<Long> = _trackPointsVersion.asStateFlow()

    // 轨迹点缓存 (workoutId -> TrackPoints)
    private val trackPointsCache = ConcurrentHashMap<String, List<TrackPoint>>()

    // 正在加载的轨迹点workoutId
    private val loadingTrackPoints = ConcurrentHashMap.newKeySet<String>()

    init {
        loadDisplayModePreference()
        loadRunRecords()
    }

    /**
     * 加载显示模式偏好设置
     */
    private fun loadDisplayModePreference() {
        val useHeatmap = preferencesManager.getDataTabDisplayMode()
        _uiState.value = _uiState.value.copy(
            displayMode = if (useHeatmap) DataTabDisplayMode.HEATMAP else DataTabDisplayMode.SIMPLE
        )
    }

    /**
     * 切换显示模式
     */
    fun toggleDisplayMode() {
        val newMode = _uiState.value.displayMode.toggle()
        _uiState.value = _uiState.value.copy(displayMode = newMode)
        preferencesManager.saveDataTabDisplayMode(newMode == DataTabDisplayMode.HEATMAP)
    }

    /**
     * 切换月份展开/折叠状态
     */
    fun toggleMonthExpanded(monthId: String) {
        val currentExpanded = _uiState.value.expandedMonths
        val newExpanded = if (currentExpanded.contains(monthId)) {
            currentExpanded - monthId
        } else {
            currentExpanded + monthId
        }
        _uiState.value = _uiState.value.copy(expandedMonths = newExpanded)
    }

    /**
     * 检查月份是否展开
     */
    fun isMonthExpanded(monthId: String): Boolean {
        return _uiState.value.expandedMonths.contains(monthId)
    }

    /**
     * 加载跑步记录列表并按月分组
     */
    private fun loadRunRecords() {
        viewModelScope.launch {
            repository.getAllRunRecords()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
                .collect { records ->
                    // 保存完整记录
                    allRecords = records

                    // 提取去重的数据来源
                    val datasources = records.mapNotNull { it.datasource }
                        .distinct().sorted()

                    // 保存当前展开状态和过滤状态
                    val currentExpanded = _uiState.value.expandedMonths
                    val currentInclusiveLevels = _uiState.value.selectedInclusiveLevels
                    val currentDatasources = _uiState.value.selectedDatasources

                    // 应用过滤
                    val filteredRecords = applyFilterToRecords(records, currentInclusiveLevels, currentDatasources)

                    // 按月分组
                    val monthGroups = groupRecordsByMonth(filteredRecords)

                    // 计算总统计数据（过滤掉 inclusiveLevel == 0 的不纳入统计记录）
                    val statsRecords = filteredRecords.filter { it.inclusiveLevel != 0 }
                    val totalDistance = statsRecords.sumOf { it.totalDistance }
                    val totalRunCount = statsRecords.size
                    val totalDuration = statsRecords.sumOf { it.activeDuration }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        monthGroups = monthGroups,
                        runRecords = filteredRecords,
                        expandedMonths = currentExpanded,
                        error = null,
                        totalDistance = totalDistance,
                        totalRunCount = totalRunCount,
                        totalDuration = totalDuration,
                        availableDatasources = datasources
                    )
                }
        }
    }

    /**
     * 将跑步记录按月份分组
     * 从当前月份遍历到最早有数据的月份，中间空月份也生成
     * 返回按时间降序排列的月份分组列表
     */
    private fun groupRecordsByMonth(records: List<RunRecordEntity>): List<MonthRangeData> {
        if (records.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()

        // 1. 找到最早记录的年月
        val earliestRecord = records.minByOrNull { it.startTime } ?: return emptyList()
        calendar.timeInMillis = earliestRecord.startTime
        val earliestYear = calendar.get(Calendar.YEAR)
        val earliestMonth = calendar.get(Calendar.MONTH) + 1

        // 2. 获取当前年月
        val today = Calendar.getInstance()
        val currentYear = today.get(Calendar.YEAR)
        val currentMonth = today.get(Calendar.MONTH) + 1

        // 3. 按年月分组实际记录
        val recordsByMonth = records.groupBy { record ->
            calendar.timeInMillis = record.startTime
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}"
        }

        // 4. 从当前月份遍历到最早月份，生成完整列表
        val result = mutableListOf<MonthRangeData>()
        var year = currentYear
        var month = currentMonth

        while (year > earliestYear || (year == earliestYear && month >= earliestMonth)) {
            val key = "$year-$month"
            val monthRecords = recordsByMonth[key] ?: emptyList()

            // 计算统计数据（过滤掉 inclusiveLevel == 0 的不纳入统计记录）
            val statsRecords = monthRecords.filter { it.inclusiveLevel != 0 }
            val totalDistance = statsRecords.sumOf { it.totalDistance }
            val totalDuration = statsRecords.sumOf { it.activeDuration }
            val avgPace = if (totalDistance > 0) {
                formatPace(totalDuration / totalDistance)
            } else {
                "--'--\""
            }

            // 生成每日数据(用于热力图)
            val dailyRecords = buildDailyRecordsForMonth(year, month, monthRecords)

            result.add(MonthRangeData(
                year = year,
                month = month,
                totalDistance = totalDistance,
                totalDurationMinutes = totalDuration,
                runCount = statsRecords.size,
                avgPace = avgPace,
                dailyRecords = dailyRecords
            ))

            // 上一个月
            month--
            if (month < 1) {
                month = 12
                year--
            }
        }

        return result  // 已经是降序排列（从当前月到最早月）
    }

    /**
     * 构建某月的每日数据(包含占位符，用于热力图显示)
     */
    private fun buildDailyRecordsForMonth(
        year: Int,
        month: Int,
        records: List<RunRecordEntity>
    ): List<DayRunData> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)

        // 获取该月第一天是周几 (周日=1, 周一=2, ...)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // 转换为周一=0的索引 (周一开始的日历)
        val placeholderCount = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

        // 获取该月天数
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 按日期分组记录
        val recordsByDay = records.groupBy { record ->
            calendar.timeInMillis = record.startTime
            calendar.get(Calendar.DAY_OF_MONTH)
        }

        val today = Calendar.getInstance()
        val isCurrentMonth = year == today.get(Calendar.YEAR) &&
                             month == today.get(Calendar.MONTH) + 1
        val todayDay = today.get(Calendar.DAY_OF_MONTH)

        val result = mutableListOf<DayRunData>()

        // 添加占位符
        repeat(placeholderCount) {
            result.add(DayRunData(isPlaceholder = true))
        }

        // 添加每日数据
        for (day in 1..daysInMonth) {
            val dayRecords = recordsByDay[day] ?: emptyList()
            val isFuture = isCurrentMonth && day > todayDay
            val dayStatsRecords = dayRecords.filter { it.inclusiveLevel != 0 }

            result.add(DayRunData(
                dayOfMonth = day,
                totalDistance = dayStatsRecords.sumOf { it.totalDistance },
                runCount = dayStatsRecords.size,
                isToday = isCurrentMonth && day == todayDay,
                isFuture = isFuture,
                workoutIds = dayRecords.map { it.workoutId }
            ))
        }

        return result
    }

    /**
     * 格式化配速
     */
    private fun formatPace(paceMinPerKm: Double): String {
        if (paceMinPerKm <= 0 || paceMinPerKm.isNaN() || paceMinPerKm.isInfinite()) {
            return "--'--\""
        }
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return "${minutes}'${seconds.toString().padStart(2, '0')}\""
    }

    /**
     * 对记录列表应用过滤条件
     */
    private fun applyFilterToRecords(
        records: List<RunRecordEntity>,
        inclusiveLevels: Set<Int>,
        datasources: Set<String>
    ): List<RunRecordEntity> {
        var result = records
        if (inclusiveLevels.isNotEmpty()) {
            result = result.filter { it.inclusiveLevel in inclusiveLevels }
        }
        if (datasources.isNotEmpty()) {
            result = result.filter { it.datasource in datasources }
        }
        return result
    }

    /**
     * 应用过滤条件
     */
    fun applyFilter(inclusiveLevels: Set<Int>, datasources: Set<String>) {
        val isActive = inclusiveLevels.isNotEmpty() || datasources.isNotEmpty()
        val filteredRecords = applyFilterToRecords(allRecords, inclusiveLevels, datasources)
        val monthGroups = groupRecordsByMonth(filteredRecords)
        val statsRecords = filteredRecords.filter { it.inclusiveLevel != 0 }

        _uiState.value = _uiState.value.copy(
            selectedInclusiveLevels = inclusiveLevels,
            selectedDatasources = datasources,
            isFilterActive = isActive,
            runRecords = filteredRecords,
            monthGroups = monthGroups,
            totalDistance = statsRecords.sumOf { it.totalDistance },
            totalRunCount = statsRecords.size,
            totalDuration = statsRecords.sumOf { it.activeDuration }
        )
    }

    /**
     * 清除过滤条件
     */
    fun clearFilter() {
        applyFilter(emptySet(), emptySet())
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        loadRunRecords()
    }

    /**
     * 获取月份内的跑步记录
     */
    fun getRecordsForMonth(year: Int, month: Int): List<RunRecordEntity> {
        val calendar = Calendar.getInstance()
        return _uiState.value.runRecords.filter { record ->
            calendar.timeInMillis = record.startTime
            calendar.get(Calendar.YEAR) == year &&
            calendar.get(Calendar.MONTH) + 1 == month
        }.sortedByDescending { it.startTime }
    }

    /**
     * 获取缓存的轨迹点
     * 如果缓存中没有，返回null并异步加载
     */
    fun getCachedTrackPoints(workoutId: String): List<TrackPoint>? {
        // 先检查缓存
        trackPointsCache[workoutId]?.let { return it }

        // 如果正在加载，返回null
        if (loadingTrackPoints.contains(workoutId)) {
            return null
        }

        // 异步加载
        loadTrackPoints(workoutId)
        return null
    }

    /**
     * 异步加载轨迹点
     */
    private fun loadTrackPoints(workoutId: String) {
        if (!loadingTrackPoints.add(workoutId)) {
            return // 已经在加载
        }

        viewModelScope.launch {
            try {
                val trackPoints = repository.getTrackPoints(workoutId)
                trackPointsCache[workoutId] = trackPoints
                // 加载完成后递增版本号，触发 UI 重组
                _trackPointsVersion.value++
            } catch (e: Exception) {
                // 加载失败，缓存空列表
                trackPointsCache[workoutId] = emptyList()
                _trackPointsVersion.value++
            } finally {
                loadingTrackPoints.remove(workoutId)
            }
        }
    }

    /**
     * 检查轨迹点是否正在加载
     */
    fun isTrackPointsLoading(workoutId: String): Boolean {
        return loadingTrackPoints.contains(workoutId)
    }

    /**
     * 清除轨迹点缓存
     */
    fun clearTrackPointsCache() {
        trackPointsCache.clear()
    }

    /**
     * 更新跑步记录的统计分析级别
     */
    fun updateInclusiveLevel(record: RunRecordEntity, newLevel: Int) {
        viewModelScope.launch {
            val updatedRecord = record.copy(inclusiveLevel = newLevel, uploadStatus = 0)
            try {
                repository.updateRunRecord(updatedRecord)
            } catch (_: Exception) { return@launch }

            // 同步到服务器
            if (updatedRecord.originId != null) {
                try {
                    val request = updatedRecord.toUpdateRequest()
                    val result = syncManager.updateRunSummary(request)
                    if (result.isSuccess) {
                        repository.updateRunRecord(updatedRecord.copy(uploadStatus = 2))
                    } else {
                        RLog.w("DataTabViewModel", "同步服务器失败: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    RLog.w("DataTabViewModel", "同步服务器异常: ${e.message}")
                }
            }

            // VDOT级联重算
            try {
                vdotRecalculationService.onInclusiveLevelChanged(record.workoutId, newLevel)
            } catch (e: Exception) {
                RLog.w("DataTabViewModel", "VDOT级联重算失败: ${e.message}")
            }
        }
    }
}

/**
 * DataTabViewModel Factory
 */
class DataTabViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataTabViewModel::class.java)) {
            val database = RunDatabase.getInstance(context)
            val repository = RunDataRepositoryImpl.getInstance(database)
            val preferencesManager = PreferencesManager(context)
            val syncManager = UnifiedDataSyncManager.getInstance(context)
            return DataTabViewModel(repository, preferencesManager, syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
