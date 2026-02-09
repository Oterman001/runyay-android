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
    val totalDuration: Double = 0.0,        // hours
    // Goal-related fields
    val distanceGoal: Double = 0.0,         // km goal
    val durationGoal: Double = 0.0,         // hours goal
    val timeProgress: Float = 0f            // Time elapsed progress (0-1)
) {
    /**
     * Calculate distance goal progress (0-1)
     */
    fun getDistanceProgress(): Float {
        return if (distanceGoal > 0) {
            (totalDistance / distanceGoal).coerceIn(0.0, 1.0).toFloat()
        } else 0f
    }

    /**
     * Calculate duration goal progress (0-1)
     */
    fun getDurationProgress(): Float {
        return if (durationGoal > 0) {
            (totalDuration / durationGoal).coerceIn(0.0, 1.0).toFloat()
        } else 0f
    }

    /**
     * Get formatted distance progress percentage
     */
    fun getDistanceProgressPercent(): String {
        return String.format("%.1f%%", getDistanceProgress() * 100)
    }

    /**
     * Get formatted duration progress percentage
     */
    fun getDurationProgressPercent(): String {
        return String.format("%.1f%%", getDurationProgress() * 100)
    }

    /**
     * Get formatted time progress percentage
     */
    fun getTimeProgressPercent(): String {
        return String.format("%.1f%%", timeProgress * 100)
    }
}

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
 * Simple run record info for day selection dialog
 */
data class DayRunRecordInfo(
    val workoutId: String,
    val distance: Double,           // km
    val duration: String,           // formatted like "45'30\""
    val startTime: String           // formatted like "06:30"
)

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
    val isIndoor: Boolean = false,          // For different cell color
    val workoutIds: List<String> = emptyList(),  // All workout IDs for this day
    val recordInfos: List<DayRunRecordInfo> = emptyList()  // Record details for multi-select dialog
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
    val goalSettings: GoalSettings = GoalSettings(),

    // New fields for 5 cards
    val latestRunRecord: LatestRunRecord? = null,
    val pbAbilityList: List<PBAbilityInfo> = emptyList(),
    val pbSpeedList: List<PBSpeedInfo> = emptyList(),
    val nextRace: NextRaceInfo? = null,
    val dailySentence: String = "",

    val error: String? = null
)
