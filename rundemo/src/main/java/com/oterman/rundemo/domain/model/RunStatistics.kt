package com.oterman.rundemo.domain.model

import java.util.Date

/**
 * Total run statistics (all time)
 */
data class TotalRunStatistics(
    val totalDistance: Double = 0.0,        // Total km
    val totalDuration: Double = 0.0,        // Total hours
    val totalRuns: Int = 0,
    val overallVdot: Double = 0.0
)

/**
 * Period-based statistics (year/month)
 */
data class PeriodStatistics(
    val runCount: Int = 0,
    val totalDistance: Double = 0.0,        // km
    val totalDuration: Double = 0.0         // hours
)

/**
 * Weekly statistics with daily breakdown
 */
data class WeekStatistics(
    val totalDistance: Double = 0.0,        // km
    val totalDurationMinutes: Double = 0.0, // minutes
    val dailyRecords: List<DayRunData> = emptyList()
) {
    val formattedHours: Int get() = (totalDurationMinutes / 60).toInt()
    val formattedMinutes: Int get() = (totalDurationMinutes % 60).toInt()
}

/**
 * Single day run data for week grid
 */
data class DayRunData(
    val date: Date = Date(),
    val dayOfWeek: String = "",             // "一", "二", etc.
    val totalDistance: Double = 0.0,
    val runCount: Int = 0,
    val isToday: Boolean = false,
    val isFuture: Boolean = false,
    val isIndoor: Boolean = false           // For different cell color
) {
    val hasRun: Boolean get() = totalDistance > 0

    fun getFormattedDistance(): String {
        return when {
            isFuture -> ""
            totalDistance > 0 -> String.format("%.1f", totalDistance)
            else -> ""
        }
    }
}

/**
 * Combined HomeTab UI state
 */
data class HomeTabUiState(
    val isLoading: Boolean = true,
    val totalStats: TotalRunStatistics = TotalRunStatistics(),
    val yearStats: PeriodStatistics = PeriodStatistics(),
    val monthStats: PeriodStatistics = PeriodStatistics(),
    val weekStats: WeekStatistics = WeekStatistics(),
    val error: String? = null
)
