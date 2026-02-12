package com.oterman.rundemo.presentation.feature.statistics.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.mock.MockDataProvider
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.domain.model.DayRunRecordInfo
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.domain.model.WeekStatistics
import com.oterman.rundemo.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ViewModel for Week Statistics view
 * Handles week navigation and data loading
 */
class WeekStatisticsViewModel(
    private val repository: RunDataRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "WeekStatisticsVM"
    }

    private val _uiState = MutableStateFlow(WeekViewUiState())
    val uiState: StateFlow<WeekViewUiState> = _uiState.asStateFlow()

    // Current week start (Monday)
    private var currentWeekStart: Calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Trajectory mode state
    private val _showTrajectoryMode = MutableStateFlow(false)
    val showTrajectoryMode: StateFlow<Boolean> = _showTrajectoryMode.asStateFlow()

    // Trajectory data cache (workoutId -> TrackPoints)
    private val _trajectoryDataMap = MutableStateFlow<Map<String, List<TrackPoint>>>(emptyMap())
    val trajectoryDataMap: StateFlow<Map<String, List<TrackPoint>>> = _trajectoryDataMap.asStateFlow()

    init {
        loadWeekData()
        loadDailySentence()
    }

    /**
     * Navigate to previous week
     */
    fun goToPreviousWeek() {
        Logger.d(TAG, "Navigating to previous week")
        currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
        // Clear trajectory data to avoid showing stale data
        if (_showTrajectoryMode.value) {
            _trajectoryDataMap.value = emptyMap()
        }
        loadWeekData()
        // Note: preloadTrajectories() is called in loadWeekData() after data is ready
    }

    /**
     * Navigate to next week (if allowed)
     */
    fun goToNextWeek() {
        if (_uiState.value.canGoNext) {
            Logger.d(TAG, "Navigating to next week")
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1)
            // Clear trajectory data to avoid showing stale data
            if (_showTrajectoryMode.value) {
                _trajectoryDataMap.value = emptyMap()
            }
            loadWeekData()
            // Note: preloadTrajectories() is called in loadWeekData() after data is ready
        }
    }

    /**
     * Jump to current week
     */
    fun goToCurrentWeek() {
        Logger.d(TAG, "Jumping to current week")
        currentWeekStart = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Clear trajectory data to avoid showing stale data
        if (_showTrajectoryMode.value) {
            _trajectoryDataMap.value = emptyMap()
        }
        loadWeekData()
        // Note: preloadTrajectories() is called in loadWeekData() after data is ready
    }

    /**
     * Refresh current week data
     */
    fun refresh() {
        // Clear trajectory data to avoid showing stale data
        if (_showTrajectoryMode.value) {
            _trajectoryDataMap.value = emptyMap()
        }
        loadWeekData()
        loadDailySentence()
        // Note: preloadTrajectories() is called in loadWeekData() after data is ready
    }

    /**
     * Toggle trajectory display mode
     */
    fun toggleTrajectoryMode() {
        val newMode = !_showTrajectoryMode.value
        Logger.d(TAG, "Toggling trajectory mode: ${_showTrajectoryMode.value} -> $newMode")
        _showTrajectoryMode.value = newMode
        
        // Preload trajectories when switching to trajectory mode
        if (newMode) {
            preloadTrajectories()
        }
    }

    /**
     * Preload trajectory data for current week
     */
    fun preloadTrajectories() {
        viewModelScope.launch {
            try {
                val dailyRecords = _uiState.value.weekStats.dailyRecords
                val allWorkoutIds = dailyRecords.flatMap { it.workoutIds }
                Logger.d(TAG, "preloadTrajectories: weekDateRange=${_uiState.value.weekDateRange}, workoutIds=$allWorkoutIds")
                val trajectoryMap = mutableMapOf<String, List<TrackPoint>>()
                
                // Load track points for each workout in the week
                dailyRecords.forEach { dayData ->
                    dayData.workoutIds.forEach { workoutId ->
                        try {
                            val trackPoints = repository.getTrackPoints(workoutId)
                            if (trackPoints.isNotEmpty()) {
                                trajectoryMap[workoutId] = trackPoints
                            }
                        } catch (e: Exception) {
                            Logger.e(TAG, "Failed to load track points for $workoutId", e)
                        }
                    }
                }
                
                _trajectoryDataMap.value = trajectoryMap
                Logger.d(TAG, "Preloaded ${trajectoryMap.size} trajectories")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to preload trajectories", e)
            }
        }
    }

    private fun loadWeekData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val weekStart = currentWeekStart.timeInMillis
                val weekEndCal = currentWeekStart.clone() as Calendar
                weekEndCal.add(Calendar.DAY_OF_WEEK, 6)
                val weekEnd = weekEndCal.timeInMillis

                // Format date range
                val dateRange = formatWeekDateRange(currentWeekStart, weekEndCal)

                // Check if can navigate to next week
                val today = Calendar.getInstance()
                val canGoNext = weekEndCal.before(today)

                Logger.d(TAG, "Loading week data: $dateRange")

                // Get records for this week
                val weekEndForQuery = weekEndCal.clone() as Calendar
                weekEndForQuery.add(Calendar.DAY_OF_WEEK, 1) // Include the last day
                val records = repository.getRunRecordsByTimeRange(
                    weekStart,
                    weekEndForQuery.timeInMillis
                )

                // Calculate week statistics
                val weekStats = calculateWeekStats(records, currentWeekStart.clone() as Calendar)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        weekDateRange = dateRange,
                        weekStats = weekStats,
                        canGoNext = canGoNext,
                        error = null
                    )
                }

                // Load trajectories after data is ready (if in trajectory mode)
                if (_showTrajectoryMode.value) {
                    Logger.d(TAG, "loadWeekData completed: dateRange=$dateRange, workoutIds=${weekStats.dailyRecords.flatMap { it.workoutIds }}, calling preloadTrajectories")
                    preloadTrajectories()
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to load week data", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load week data"
                    )
                }
            }
        }
    }

    private fun loadDailySentence() {
        val sentence = MockDataProvider.getRandomDailySentence()
        _uiState.update { it.copy(dailySentence = sentence) }
    }

    /**
     * Format week date range like "2024年11月16日-11月22日"
     */
    private fun formatWeekDateRange(start: Calendar, end: Calendar): String {
        val startYear = start.get(Calendar.YEAR)
        val startMonth = start.get(Calendar.MONTH) + 1
        val startDay = start.get(Calendar.DAY_OF_MONTH)
        val endMonth = end.get(Calendar.MONTH) + 1
        val endDay = end.get(Calendar.DAY_OF_MONTH)

        return if (startMonth == endMonth) {
            "${startYear}年${startMonth}月${startDay}日-${endDay}日"
        } else {
            "${startYear}年${startMonth}月${startDay}日-${endMonth}月${endDay}日"
        }
    }

    /**
     * Calculate week statistics from records
     */
    private fun calculateWeekStats(
        records: List<RunRecordEntity>,
        weekStart: Calendar
    ): WeekStatistics {
        val today = Calendar.getInstance()
        val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
        val dailyRecords = mutableListOf<DayRunData>()

        // Total stats
        var totalDistance = 0.0
        var totalDurationMinutes = 0.0
        var totalElevation = 0.0
        var runCount = 0

        // Build daily records for 7 days
        for (i in 0..6) {
            val dayCal = weekStart.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_WEEK, i)
            val dayStart = dayCal.timeInMillis
            dayCal.add(Calendar.DAY_OF_WEEK, 1)
            val dayEnd = dayCal.timeInMillis

            val dayRecords = records.filter { it.startTime in dayStart until dayEnd }

            dayCal.timeInMillis = dayStart
            val isToday = isSameDay(dayCal, today)
            val isFuture = dayCal.after(today) && !isToday

            // Calculate day stats
            val dayDistance = dayRecords.sumOf { it.totalDistance }
            val dayDuration = dayRecords.sumOf { it.activeDuration }
            val dayPace = if (dayDistance > 0) formatPace(dayRecords.sumOf { it.averageSpeed * it.activeDuration } / dayDuration) else "--'--\""

            // Build record infos for multi-select dialog
            val recordInfos = dayRecords.map { record ->
                DayRunRecordInfo(
                    workoutId = record.workoutId,
                    distance = record.totalDistance,
                    duration = formatDuration(record.activeDuration),
                    startTime = formatTime(record.startTime)
                )
            }

            dailyRecords.add(
                DayRunData(
                    date = Date(dayStart),
                    dayOfWeek = weekDays[i],
                    totalDistance = dayDistance,
                    runCount = dayRecords.size,
                    isToday = isToday,
                    isFuture = isFuture,
                    isIndoor = dayRecords.any { it.outdoor == 0 },
                    workoutIds = dayRecords.map { it.workoutId },
                    recordInfos = recordInfos,
                    totalDurationMinutes = dayDuration,
                    avgPace = dayPace
                )
            )

            // Accumulate totals
            if (!isFuture) {
                totalDistance += dayDistance
                totalDurationMinutes += dayDuration
                runCount += dayRecords.size
                totalElevation += dayRecords.sumOf { it.elevationAscended }
            }
        }

        // Calculate average pace
        val avgPace = if (totalDistance > 0 && totalDurationMinutes > 0) {
            val avgSpeed = totalDistance / (totalDurationMinutes / 60.0)
            formatPace(avgSpeed)
        } else {
            "--'--\""
        }

        return WeekStatistics(
            totalDistance = totalDistance,
            totalDurationMinutes = totalDurationMinutes,
            dailyRecords = dailyRecords,
            runCount = runCount,
            avgPace = avgPace,
            totalElevation = totalElevation
        )
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Format duration in minutes to "45'30\"" format
     */
    private fun formatDuration(minutes: Double): String {
        val totalSeconds = (minutes * 60).toInt()
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        return "${mins}'${String.format("%02d", secs)}\""
    }

    /**
     * Format speed (km/h) to pace "4'20\"" format
     */
    private fun formatPace(speedKmh: Double): String {
        if (speedKmh <= 0) return "--'--\""
        val paceMinPerKm = 60.0 / speedKmh
        val mins = paceMinPerKm.toInt()
        val secs = ((paceMinPerKm - mins) * 60).toInt()
        return "${mins}'${String.format("%02d", secs)}\""
    }

    /**
     * Format time to "HH:mm"
     */
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
