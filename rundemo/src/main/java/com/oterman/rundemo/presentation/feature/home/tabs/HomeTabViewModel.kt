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
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.domain.model.GoalSettings
import com.oterman.rundemo.domain.model.GoalType
import com.oterman.rundemo.domain.model.HomeTabUiState
import com.oterman.rundemo.domain.model.PeriodStatistics
import com.oterman.rundemo.domain.model.TotalRunStatistics
import com.oterman.rundemo.domain.model.WeekStatistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * HomeTab ViewModel
 * Manages running statistics state for the home tab
 */
class HomeTabViewModel(
    private val repository: RunDataRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeTabUiState())
    val uiState: StateFlow<HomeTabUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    /**
     * Load all statistics
     */
    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val allRecords = repository.getAllRunRecords().first()
                val goalSettings = preferencesManager.getGoalSettings()

                val totalStats = calculateTotalStatistics(allRecords)
                val yearStats = calculateYearStatistics(allRecords, goalSettings)
                val monthStats = calculateMonthStatistics(allRecords, goalSettings)
                val weekStats = calculateWeekStatistics(allRecords)

                _uiState.value = HomeTabUiState(
                    isLoading = false,
                    totalStats = totalStats,
                    yearStats = yearStats,
                    monthStats = monthStats,
                    weekStats = weekStats,
                    goalSettings = goalSettings,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load statistics"
                )
            }
        }
    }

    /**
     * Refresh statistics
     */
    fun refresh() {
        loadStatistics()
    }

    private fun calculateTotalStatistics(allRecords: List<RunRecordEntity>): TotalRunStatistics {
        return TotalRunStatistics(
            totalDistance = allRecords.sumOf { it.totalDistance },
            totalDuration = allRecords.sumOf { it.activeDuration } / 60.0, // Convert to hours
            totalRuns = allRecords.size,
            overallVdot = allRecords.maxOfOrNull { maxOf(it.vdot, it.overallVdot) } ?: 0.0
        )
    }

    private fun calculateYearStatistics(
        allRecords: List<RunRecordEntity>,
        goalSettings: GoalSettings
    ): PeriodStatistics {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val yearStart = calendar.timeInMillis

        calendar.set(currentYear + 1, Calendar.JANUARY, 1, 0, 0, 0)
        val yearEnd = calendar.timeInMillis

        val yearRecords = allRecords.filter { it.startTime in yearStart until yearEnd }

        // Calculate time progress (how much of the year has passed)
        val timeProgress = calculateYearTimeProgress()

        return PeriodStatistics(
            runCount = yearRecords.size,
            totalDistance = yearRecords.sumOf { it.totalDistance },
            totalDuration = yearRecords.sumOf { it.activeDuration } / 60.0,
            distanceGoal = goalSettings.yearDistanceGoal,
            durationGoal = goalSettings.yearDurationGoal,
            timeProgress = timeProgress
        )
    }

    private fun calculateMonthStatistics(
        allRecords: List<RunRecordEntity>,
        goalSettings: GoalSettings
    ): PeriodStatistics {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis

        val monthRecords = allRecords.filter { it.startTime in monthStart until monthEnd }

        // Calculate time progress (how much of the month has passed)
        val timeProgress = calculateMonthTimeProgress()

        return PeriodStatistics(
            runCount = monthRecords.size,
            totalDistance = monthRecords.sumOf { it.totalDistance },
            totalDuration = monthRecords.sumOf { it.activeDuration } / 60.0,
            distanceGoal = goalSettings.monthDistanceGoal,
            durationGoal = goalSettings.monthDurationGoal,
            timeProgress = timeProgress
        )
    }

    private fun calculateWeekStatistics(allRecords: List<RunRecordEntity>): WeekStatistics {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()

        // Set to Monday of current week
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis

        // End of week (next Monday)
        val weekEndCal = calendar.clone() as Calendar
        weekEndCal.add(Calendar.DAY_OF_WEEK, 7)
        val weekEnd = weekEndCal.timeInMillis

        val weekRecords = allRecords.filter { it.startTime in weekStart until weekEnd }

        // Build daily records
        val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
        val dailyRecords = mutableListOf<DayRunData>()

        for (i in 0..6) {
            val dayCal = Calendar.getInstance()
            dayCal.timeInMillis = weekStart
            dayCal.add(Calendar.DAY_OF_WEEK, i)
            val dayStart = dayCal.timeInMillis
            dayCal.add(Calendar.DAY_OF_WEEK, 1)
            val dayEnd = dayCal.timeInMillis

            val dayRecords = weekRecords.filter { it.startTime in dayStart until dayEnd }

            dayCal.timeInMillis = dayStart
            val isToday = isSameDay(dayCal, today)
            val isFuture = dayCal.after(today) && !isToday

            dailyRecords.add(
                DayRunData(
                    date = Date(dayStart),
                    dayOfWeek = weekDays[i],
                    totalDistance = dayRecords.sumOf { it.totalDistance },
                    runCount = dayRecords.size,
                    isToday = isToday,
                    isFuture = isFuture,
                    isIndoor = dayRecords.any { it.outdoor == 1 }
                )
            )
        }

        return WeekStatistics(
            totalDistance = weekRecords.sumOf { it.totalDistance },
            totalDurationMinutes = weekRecords.sumOf { it.activeDuration },
            dailyRecords = dailyRecords
        )
    }

    /**
     * Calculate how much of the current year has passed (0-1)
     */
    private fun calculateYearTimeProgress(): Float {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        // Year start
        calendar.set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val yearStart = calendar.timeInMillis

        // Year end
        calendar.set(currentYear + 1, Calendar.JANUARY, 1, 0, 0, 0)
        val yearEnd = calendar.timeInMillis

        val totalDuration = yearEnd - yearStart
        val elapsed = now - yearStart

        return (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
    }

    /**
     * Calculate how much of the current month has passed (0-1)
     */
    private fun calculateMonthTimeProgress(): Float {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        // Month start
        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis

        // Month end
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis

        val totalDuration = monthEnd - monthStart
        val elapsed = now - monthStart

        return (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

/**
 * HomeTabViewModel Factory
 */
class HomeTabViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeTabViewModel::class.java)) {
            val database = RunDatabase.getInstance(context)
            val repository = RunDataRepositoryImpl(database)
            val preferencesManager = PreferencesManager(context)
            return HomeTabViewModel(repository, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
