package com.oterman.rundemo.presentation.feature.statistics.year

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.mock.MockDataProvider
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.domain.model.DayRunRecordInfo
import com.oterman.rundemo.domain.model.MonthRangeData
import com.oterman.rundemo.domain.model.YearStatistics
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ViewModel for Year Statistics view
 * Handles year navigation and data loading
 */
class YearStatisticsViewModel(
    private val repository: RunDataRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "YearStatisticsVM"
    }

    private val _uiState = MutableStateFlow(YearViewUiState())
    val uiState: StateFlow<YearViewUiState> = _uiState.asStateFlow()

    // Current year being displayed
    private var currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)

    init {
        // Load saved items per row preference
        val savedItemsPerRow = preferencesManager.getTrajectoryItemsPerRow()
        _uiState.update { it.copy(itemsPerRow = savedItemsPerRow) }

        loadYearData()
        loadDailySentence()
    }

    /**
     * Navigate to previous year
     */
    fun goToPreviousYear() {
        RLog.d(TAG, "Navigating to previous year")
        currentYear -= 1
        loadYearData()
        if (_uiState.value.showTrajectoryMode) {
            loadTrajectoryDataForYear()
        }
    }

    /**
     * Navigate to next year (if allowed)
     */
    fun goToNextYear() {
        if (_uiState.value.canGoNext) {
            RLog.d(TAG, "Navigating to next year")
            currentYear += 1
            loadYearData()
            if (_uiState.value.showTrajectoryMode) {
                loadTrajectoryDataForYear()
            }
        }
    }

    /**
     * Jump to current year (on double tap)
     */
    fun goToCurrentYear() {
        RLog.d(TAG, "Jumping to current year")
        currentYear = Calendar.getInstance().get(Calendar.YEAR)
        loadYearData()
        if (_uiState.value.showTrajectoryMode) {
            loadTrajectoryDataForYear()
        }
    }

    /**
     * Check if currently showing current year
     */
    fun isCurYear(): Boolean {
        return currentYear == Calendar.getInstance().get(Calendar.YEAR)
    }

    /**
     * Get current month (1-12)
     */
    fun getCurMonth(): Int {
        return Calendar.getInstance().get(Calendar.MONTH) + 1
    }

    /**
     * Refresh current year data
     */
    fun refresh() {
        loadYearData()
        loadDailySentence()
        if (_uiState.value.showTrajectoryMode) {
            loadTrajectoryDataForYear()
        }
    }

    /**
     * Toggle between heatmap and trajectory wall modes
     */
    fun toggleTrajectoryMode() {
        val entering = !_uiState.value.showTrajectoryMode
        _uiState.update { it.copy(showTrajectoryMode = entering) }
        if (entering) {
            loadTrajectoryDataForYear()
        }
    }

    /**
     * Load trajectory data for the current year
     * Filters outdoor runs with distance >= 0.5km, trajectoryStatus != 2, inclusiveLevel != 0
     */
    fun loadTrajectoryDataForYear() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTrajectory = true) }
            try {
                val workoutIds = mutableListOf<Pair<String, Long>>()
                val distanceMap = mutableMapOf<String, Double>()

                for (month in 1..12) {
                    val monthStart = getMonthStart(currentYear, month)
                    val monthEnd = getMonthEnd(currentYear, month)
                    val records = repository.getRunRecordsByTimeRange(monthStart, monthEnd)

                    for (record in records) {
                        if (record.outdoor == 0 &&
                            record.totalDistance >= 0.5 &&
                            record.trajectoryStatus != 2 &&
                            record.inclusiveLevel != 0
                        ) {
                            workoutIds.add(record.workoutId to record.startTime)
                            distanceMap[record.workoutId] = record.totalDistance
                        }
                    }
                }

                // Sort by startTime ascending
                workoutIds.sortBy { it.second }

                _uiState.update {
                    it.copy(
                        trajectoryWorkoutIds = workoutIds.map { pair -> pair.first },
                        trajectoryDistanceMap = distanceMap,
                        isLoadingTrajectory = false
                    )
                }
                RLog.d(TAG, "Loaded ${workoutIds.size} trajectory workout IDs for year $currentYear")
            } catch (e: Exception) {
                RLog.e(TAG, "Failed to load trajectory data", e)
                _uiState.update { it.copy(isLoadingTrajectory = false) }
            }
        }
    }

    /**
     * Set items per row and persist to preferences
     */
    fun setItemsPerRow(count: Int) {
        if (count in 3..10) {
            _uiState.update { it.copy(itemsPerRow = count) }
            preferencesManager.saveTrajectoryItemsPerRow(count)
        }
    }

    /**
     * Toggle settings bottom sheet visibility
     */
    fun toggleSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = !it.showSettingsSheet) }
    }

    /**
     * Dismiss settings bottom sheet
     */
    fun dismissSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = false) }
    }

    private fun loadYearData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val today = Calendar.getInstance()
                val todayYear = today.get(Calendar.YEAR)
                val todayMonth = today.get(Calendar.MONTH) + 1

                // Check if can navigate to next year
                val canGoNext = currentYear < todayYear

                RLog.d(TAG, "Loading year data: ${currentYear}年")

                val monthRangeDataList = mutableListOf<MonthRangeData>()
                var maxMonthDistance = 0.0
                var totalDistance = 0.0
                var totalDurationMinutes = 0.0
                var totalElevation = 0.0
                var runCount = 0

                // Iterate through 12 months
                for (month in 1..12) {
                    val monthStart = getMonthStart(currentYear, month)
                    val monthEnd = getMonthEnd(currentYear, month)

                    val records = repository.getRunRecordsByTimeRange(monthStart, monthEnd)
                    val monthData = calculateMonthRangeData(records, currentYear, month)

                    monthRangeDataList.add(monthData)
                    maxMonthDistance = maxOf(maxMonthDistance, monthData.totalDistance)

                    // Only accumulate past months (or current month if showing current year)
                    val isFutureMonth = currentYear == todayYear && month > todayMonth
                    if (!isFutureMonth) {
                        totalDistance += monthData.totalDistance
                        totalDurationMinutes += monthData.totalDurationMinutes
                        totalElevation += monthData.totalElevation
                        runCount += monthData.runCount
                    }
                }

                // Calculate average pace
                val avgPace = if (totalDistance > 0 && totalDurationMinutes > 0) {
                    val avgSpeed = totalDistance / (totalDurationMinutes / 60.0)
                    formatPace(avgSpeed)
                } else {
                    "--'--\""
                }

                // Build YearStatistics
                val yearStats = YearStatistics(
                    year = currentYear,
                    totalDistance = totalDistance,
                    totalDurationMinutes = totalDurationMinutes,
                    runCount = runCount,
                    avgPace = avgPace,
                    totalElevation = totalElevation,
                    monthRangeDataList = monthRangeDataList,
                    maxMonthDistance = maxMonthDistance
                )

                // Load zone distribution data
                val allWorkoutIds = monthRangeDataList.flatMap { month ->
                    month.dailyRecords.filter { !it.isPlaceholder }.flatMap { it.workoutIds }
                }
                val hr7Zones = repository.getAggregatedHeartRate7Zones(allWorkoutIds)
                val hr5Zones = repository.getAggregatedHeartRate5Zones(allWorkoutIds)
                val speedZones = repository.getAggregatedSpeedZones(allWorkoutIds)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        yearDisplay = "${currentYear}年",
                        yearStats = yearStats,
                        canGoNext = canGoNext,
                        heartRate7Zones = hr7Zones,
                        heartRate5Zones = hr5Zones,
                        speedZones = speedZones,
                        error = null
                    )
                }
            } catch (e: Exception) {
                RLog.e(TAG, "Failed to load year data", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load year data"
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
     * Get month start timestamp
     */
    private fun getMonthStart(year: Int, month: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Get month end timestamp (exclusive, first day of next month)
     */
    private fun getMonthEnd(year: Int, month: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, 1)
        return cal.timeInMillis
    }

    /**
     * Calculate MonthRangeData from records for a specific month
     */
    private fun calculateMonthRangeData(
        records: List<RunRecordEntity>,
        year: Int,
        month: Int
    ): MonthRangeData {
        val today = Calendar.getInstance()
        val todayYear = today.get(Calendar.YEAR)
        val todayMonth = today.get(Calendar.MONTH) + 1
        val todayDay = today.get(Calendar.DAY_OF_MONTH)

        val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
        val dailyRecords = mutableListOf<DayRunData>()

        // Month start calendar
        val monthStart = Calendar.getInstance()
        monthStart.set(year, month - 1, 1, 0, 0, 0)
        monthStart.set(Calendar.MILLISECOND, 0)

        // Calculate the first day of week offset (Monday = 0, Sunday = 6)
        val firstDayOfWeek = getFirstDayOfWeekOffset(monthStart)

        // Add placeholder cells for days before the 1st
        repeat(firstDayOfWeek) {
            dailyRecords.add(DayRunData(isPlaceholder = true))
        }

        // Get days in month
        val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Total stats for this month
        var totalDistance = 0.0
        var totalDurationMinutes = 0.0
        var totalElevation = 0.0
        var runCount = 0

        // Build daily records for each day of the month
        for (day in 1..daysInMonth) {
            val dayCal = monthStart.clone() as Calendar
            dayCal.set(Calendar.DAY_OF_MONTH, day)
            val dayStart = dayCal.timeInMillis
            dayCal.add(Calendar.DAY_OF_MONTH, 1)
            val dayEnd = dayCal.timeInMillis

            val dayRecords = records.filter { it.startTime in dayStart until dayEnd }

            dayCal.timeInMillis = dayStart
            val isToday = year == todayYear && month == todayMonth && day == todayDay
            val isFuture = if (year > todayYear) {
                true
            } else if (year == todayYear && month > todayMonth) {
                true
            } else if (year == todayYear && month == todayMonth && day > todayDay) {
                true
            } else {
                false
            }

            // Get day of week (Monday = 0, Sunday = 6)
            val dayOfWeekIndex = (dayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val dayOfWeek = weekDays[dayOfWeekIndex]

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
                    dayOfWeek = dayOfWeek,
                    dayOfMonth = day,
                    totalDistance = dayDistance,
                    runCount = dayRecords.size,
                    isToday = isToday,
                    isFuture = isFuture,
                    isIndoor = dayRecords.any { it.outdoor == 1 },
                    isPlaceholder = false,
                    workoutIds = dayRecords.map { it.workoutId },
                    recordInfos = recordInfos,
                    totalDurationMinutes = dayDuration,
                    avgPace = dayPace
                )
            )

            // Accumulate totals (only for past/today)
            if (!isFuture) {
                totalDistance += dayDistance
                totalDurationMinutes += dayDuration
                runCount += dayRecords.size
                totalElevation += dayRecords.sumOf { it.elevationAscended }
            }
        }

        // Calculate average pace for this month
        val avgPace = if (totalDistance > 0 && totalDurationMinutes > 0) {
            val avgSpeed = totalDistance / (totalDurationMinutes / 60.0)
            formatPace(avgSpeed)
        } else {
            "--'--\""
        }

        return MonthRangeData(
            year = year,
            month = month,
            totalDistance = totalDistance,
            totalDurationMinutes = totalDurationMinutes,
            runCount = runCount,
            avgPace = avgPace,
            totalElevation = totalElevation,
            dailyRecords = dailyRecords
        )
    }

    /**
     * Get offset for the first day of the month (Monday = 0, Sunday = 6)
     */
    private fun getFirstDayOfWeekOffset(monthStart: Calendar): Int {
        val firstDayOfMonth = monthStart.get(Calendar.DAY_OF_WEEK)
        // Convert from Calendar.DAY_OF_WEEK (Sunday=1...Saturday=7) to (Monday=0...Sunday=6)
        return (firstDayOfMonth + 5) % 7
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
