package com.oterman.rundemo.presentation.feature.statistics.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.mock.MockDataProvider
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.domain.model.DayRunRecordInfo
import com.oterman.rundemo.domain.model.MonthStatistics
import com.oterman.rundemo.util.Logger
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
 * ViewModel for Month Statistics view
 * Handles month navigation and data loading
 */
class MonthStatisticsViewModel(
    private val repository: RunDataRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "MonthStatisticsVM"
    }

    private val _uiState = MutableStateFlow(MonthViewUiState())
    val uiState: StateFlow<MonthViewUiState> = _uiState.asStateFlow()

    // Current month start (1st of the month)
    private var currentMonthStart: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    init {
        loadMonthData()
        loadDailySentence()
    }

    /**
     * Navigate to previous month
     */
    fun goToPreviousMonth() {
        Logger.d(TAG, "Navigating to previous month")
        currentMonthStart.add(Calendar.MONTH, -1)
        loadMonthData()
    }

    /**
     * Navigate to next month (if allowed)
     */
    fun goToNextMonth() {
        if (_uiState.value.canGoNext) {
            Logger.d(TAG, "Navigating to next month")
            currentMonthStart.add(Calendar.MONTH, 1)
            loadMonthData()
        }
    }

    /**
     * Jump to current month (on double tap)
     */
    fun goToCurrentMonth() {
        Logger.d(TAG, "Jumping to current month")
        currentMonthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        loadMonthData()
    }

    /**
     * Jump to specific month
     * Called when user clicks a month in year view
     */
    fun goToSpecificMonth(year: Int, month: Int) {
        Logger.d(TAG, "Jumping to specific month: $year -- $month")
        currentMonthStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Calendar.MONTH is 0-based
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        loadMonthData()
    }

    /**
     * Refresh current month data
     */
    fun refresh() {
        loadMonthData()
        loadDailySentence()
    }

    private fun loadMonthData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val monthStart = currentMonthStart.timeInMillis
                val monthEndCal = currentMonthStart.clone() as Calendar
                monthEndCal.add(Calendar.MONTH, 1)
                val monthEnd = monthEndCal.timeInMillis

                // Format month display
                val monthYearDisplay = formatMonthYear(currentMonthStart)

                // Check if can navigate to next month
                val today = Calendar.getInstance()
                val currentMonthCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val canGoNext = currentMonthStart.before(currentMonthCal)

                Logger.d(TAG, "Loading month data: $monthYearDisplay")

                // Get records for this month
                val records = repository.getRunRecordsByTimeRange(monthStart, monthEnd)

                // Calculate month statistics
                val monthStats = calculateMonthStats(records, currentMonthStart.clone() as Calendar)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        monthYearDisplay = monthYearDisplay,
                        monthStats = monthStats,
                        canGoNext = canGoNext,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to load month data", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load month data"
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
     * Format month year like "2024年11月"
     */
    private fun formatMonthYear(cal: Calendar): String {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return "${year}年${month}月"
    }

    /**
     * Calculate month statistics from records
     */
    private fun calculateMonthStats(
        records: List<RunRecordEntity>,
        monthStart: Calendar
    ): MonthStatistics {
        val today = Calendar.getInstance()
        val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
        val dailyRecords = mutableListOf<DayRunData>()

        // Total stats
        var totalDistance = 0.0
        var totalDurationMinutes = 0.0
        var totalElevation = 0.0
        var runCount = 0

        // Calculate the first day of week offset for the month (Monday = 0, Sunday = 6)
        val firstDayOfWeek = getFirstDayOfWeekOffset(monthStart)

        // Add placeholder cells for days before the 1st
        repeat(firstDayOfWeek) {
            dailyRecords.add(DayRunData(isPlaceholder = true))
        }

        // Get days in month
        val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Build daily records for each day of the month
        for (day in 1..daysInMonth) {
            val dayCal = monthStart.clone() as Calendar
            dayCal.set(Calendar.DAY_OF_MONTH, day)
            val dayStart = dayCal.timeInMillis
            dayCal.add(Calendar.DAY_OF_MONTH, 1)
            val dayEnd = dayCal.timeInMillis

            val dayRecords = records.filter { it.startTime in dayStart until dayEnd }

            dayCal.timeInMillis = dayStart
            val isToday = isSameDay(dayCal, today)
            val isFuture = dayCal.after(today) && !isToday

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
                    isIndoor = dayRecords.any { it.outdoor == 0 },
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

        // Calculate average pace
        val avgPace = if (totalDistance > 0 && totalDurationMinutes > 0) {
            val avgSpeed = totalDistance / (totalDurationMinutes / 60.0)
            formatPace(avgSpeed)
        } else {
            "--'--\""
        }

        return MonthStatistics(
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
