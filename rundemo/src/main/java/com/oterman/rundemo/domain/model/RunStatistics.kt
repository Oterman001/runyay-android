package com.oterman.rundemo.domain.model

import com.oterman.rundemo.data.local.entity.RunRecordEntity
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
     * Get formatted distance progress percentage (uncapped, can exceed 100%)
     */
    fun getDistanceProgressPercent(): String {
        return if (distanceGoal > 0) {
            String.format("%.1f%%", (totalDistance / distanceGoal) * 100)
        } else "0.0%"
    }

    /**
     * Get formatted duration progress percentage (uncapped, can exceed 100%)
     */
    fun getDurationProgressPercent(): String {
        return if (durationGoal > 0) {
            String.format("%.1f%%", (totalDuration / durationGoal) * 100)
        } else "0.0%"
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
    val dailyRecords: List<DayRunData> = emptyList(),
    // Additional fields for week statistics view
    val runCount: Int = 0,                      // Number of runs this week
    val avgPace: String = "--'--\"",            // Average pace formatted
    val totalElevation: Double = 0.0            // Total elevation gain (meters)
) {
    val formattedHours: Int get() = (totalDurationMinutes / 60).toInt()
    val formattedMinutes: Int get() = (totalDurationMinutes % 60).toInt()
}

/**
 * Monthly statistics with daily breakdown
 */
data class MonthStatistics(
    val totalDistance: Double = 0.0,           // km
    val totalDurationMinutes: Double = 0.0,    // minutes
    val runCount: Int = 0,                     // Number of runs this month
    val avgPace: String = "--'--\"",           // Average pace formatted
    val totalElevation: Double = 0.0,          // Total elevation gain (meters)
    val dailyRecords: List<DayRunData> = emptyList()  // Contains placeholders for calendar layout
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
    val startTime: String,          // formatted like "06:30"
    val deviceInfo: String? = null   // device name
)

/**
 * Single day run data for week/month grid
 */
data class DayRunData(
    val date: Date = Date(),
    val dayOfWeek: String = "",             // "一", "二", etc.
    val dayOfMonth: Int = 0,                // 1-31, day of month for calendar display
    val totalDistance: Double = 0.0,
    val runCount: Int = 0,
    val isToday: Boolean = false,
    val isFuture: Boolean = false,
    val isIndoor: Boolean = false,          // For different cell color
    val isPlaceholder: Boolean = false,     // For month calendar empty cells before 1st
    val workoutIds: List<String> = emptyList(),  // All workout IDs for this day
    val recordInfos: List<DayRunRecordInfo> = emptyList(),  // Record details for multi-select dialog
    // Additional fields for week/month detail table
    val totalDurationMinutes: Double = 0.0,     // Duration in minutes
    val avgPace: String = "--'--\"",            // Average pace formatted
    val totalElevation: Double = 0.0            // Total elevation gain (meters)
) {
    val hasRun: Boolean get() = totalDistance > 0 && !isPlaceholder

    fun getFormattedDistance(): String {
        return when {
            isPlaceholder -> ""
            isFuture -> ""
            totalDistance > 0 -> String.format("%.1f", totalDistance)
            else -> ""
        }
    }

    fun getFormattedDuration(): String {
        if (isPlaceholder || totalDurationMinutes <= 0) return "--"
        val hours = (totalDurationMinutes / 60).toInt()
        val mins = (totalDurationMinutes % 60).toInt()
        return if (hours > 0) {
            "${hours}h${mins}'"
        } else {
            "${mins}'"
        }
    }

    fun getFormattedElevation(): String {
        if (!hasRun || totalElevation <= 0) return "-"
        return String.format("%.0f", totalElevation)
    }
}

/**
 * Month range statistics data (for year view's monthly summary)
 */
data class MonthRangeData(
    val year: Int = 0,
    val month: Int = 0,                       // 1-12
    val totalDistance: Double = 0.0,          // km
    val totalDurationMinutes: Double = 0.0,   // minutes
    val runCount: Int = 0,
    val avgPace: String = "--'--\"",
    val totalElevation: Double = 0.0,
    val dailyRecords: List<DayRunData> = emptyList()  // Daily data for heatmap (with placeholders)
) {
    val date: Date get() {
        val cal = java.util.Calendar.getInstance()
        cal.set(year, month - 1, 1)
        return cal.time
    }

    fun getFormattedDistance(): String {
        return if (totalDistance >= 1000) {
            totalDistance.toInt().toString()
        } else {
            String.format("%.1f", totalDistance)
        }
    }

    fun getFormattedDuration(): String {
        val hours = totalDurationMinutes / 60.0
        return String.format("%.1f", hours)
    }
}

/**
 * Year statistics data
 */
data class YearStatistics(
    val year: Int = 0,
    val totalDistance: Double = 0.0,           // km
    val totalDurationMinutes: Double = 0.0,    // minutes
    val runCount: Int = 0,
    val avgPace: String = "--'--\"",
    val totalElevation: Double = 0.0,
    val monthRangeDataList: List<MonthRangeData> = emptyList(),  // 12 months data
    val maxMonthDistance: Double = 0.0         // Max month distance (for bar chart Y-axis)
) {
    val formattedHours: Int get() = (totalDurationMinutes / 60).toInt()
    val formattedMinutes: Int get() = (totalDurationMinutes % 60).toInt()

    // Calculate average month distance (current year by elapsed months, past years by 12)
    fun getAverageMonthDistance(isCurYear: Boolean, curMonth: Int): Double {
        val monthCount = if (isCurYear) curMonth else 12
        return if (monthCount > 0) totalDistance / monthCount else 0.0
    }
}

/**
 * Yearly statistics data (for Total view's per-year breakdown)
 */
data class YearlyStatistic(
    val year: Int = 0,
    val totalDistance: Double = 0.0,          // km
    val totalDurationMinutes: Double = 0.0,   // minutes
    val avgPace: String = "--'--\"",          // min/km formatted
    val totalElevation: Double = 0.0,         // meters
    val runCount: Int = 0,
    val totalEnergy: Double = 0.0             // kcal
) {
    fun getFormattedDistance(): String {
        return if (totalDistance >= 1000) {
            totalDistance.toInt().toString()
        } else {
            String.format("%.1f", totalDistance)
        }
    }

    fun getFormattedDuration(): String {
        val hours = totalDurationMinutes / 60.0
        return String.format("%.1f", hours)
    }

    fun getFormattedElevation(): String {
        return String.format("%.0f", totalElevation)
    }
}

/**
 * All-time total statistics (aggregated across all years)
 */
data class AllTimeTotalStatistics(
    val totalDistance: Double = 0.0,           // km
    val totalDurationMinutes: Double = 0.0,    // minutes
    val avgPace: String = "--'--\"",           // min/km formatted
    val totalElevation: Double = 0.0,          // meters
    val runCount: Int = 0,
    val yearlyStatistics: List<YearlyStatistic> = emptyList(),
    val maxYearDistance: Double = 0.0,         // Max year distance (for chart Y-axis)
    val maxYearDuration: Double = 0.0          // Max year duration in minutes (for chart Y-axis)
) {
    val formattedHours: Int get() = (totalDurationMinutes / 60).toInt()
    val formattedMinutes: Int get() = (totalDurationMinutes % 60).toInt()
}

/**
 * Chart display mode for Total view
 */
enum class TotalChartDisplayMode {
    DISTANCE,
    DURATION;

    val title: String
        get() = when (this) {
            DISTANCE -> "距离"
            DURATION -> "时长"
        }

    val unit: String
        get() = when (this) {
            DISTANCE -> "公里"
            DURATION -> "小时"
        }
}

/**
 * DataTab 显示模式
 * 对应 iOS AllRunRecordPage 的两种视图模式
 */
enum class DataTabDisplayMode {
    HEATMAP,  // 热力图模式 - 显示迷你月度热力日历
    SIMPLE;   // 简单文本模式 - 显示文字统计信息

    fun toggle(): DataTabDisplayMode = when (this) {
        HEATMAP -> SIMPLE
        SIMPLE -> HEATMAP
    }
}

/**
 * Streak statistics for consecutive running days and weeks
 */
data class StreakStats(
    val currentDayStreak: Int = 0,
    val currentWeekStreak: Int = 0,
    val bestDayStreak: Int = 0,
    val bestWeekStreak: Int = 0
)

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
    val latestRunRecordEntity: RunRecordEntity? = null,
    val pbAbilityList: List<PBAbilityInfo> = emptyList(),
    val pbSpeedList: List<PBSpeedInfo> = emptyList(),
    val nextRace: NextRaceInfo? = null,
    val dailySentence: String = "",
    val nextTrainPlanSummary: TrainPlanSummary? = null,
    val nextTrainPlanDetail: TrainPlan? = null,

    val streakStats: StreakStats = StreakStats(),

    val error: String? = null
)
