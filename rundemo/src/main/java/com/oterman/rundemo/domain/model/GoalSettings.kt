package com.oterman.rundemo.domain.model

/**
 * Goal type enum - either distance-based or duration-based goals
 */
enum class GoalType {
    DISTANCE,  // Distance goal (km)
    DURATION   // Duration goal (hours)
}

/**
 * Running goal settings
 * Supports both distance and duration goals for month and year periods
 */
data class GoalSettings(
    val goalEnabled: Boolean = false,
    val goalType: GoalType = GoalType.DISTANCE,
    val yearDistanceGoal: Double = 0.0,     // km
    val monthDistanceGoal: Double = 0.0,    // km
    val yearDurationGoal: Double = 0.0,     // hours
    val monthDurationGoal: Double = 0.0     // hours
) {
    /**
     * Check if there's an active goal based on current goal type
     */
    fun hasActiveYearGoal(): Boolean {
        return goalEnabled && when (goalType) {
            GoalType.DISTANCE -> yearDistanceGoal > 0
            GoalType.DURATION -> yearDurationGoal > 0
        }
    }

    fun hasActiveMonthGoal(): Boolean {
        return goalEnabled && when (goalType) {
            GoalType.DISTANCE -> monthDistanceGoal > 0
            GoalType.DURATION -> monthDurationGoal > 0
        }
    }

    /**
     * Get the active year goal value
     */
    fun getActiveYearGoal(): Double {
        return when (goalType) {
            GoalType.DISTANCE -> yearDistanceGoal
            GoalType.DURATION -> yearDurationGoal
        }
    }

    /**
     * Get the active month goal value
     */
    fun getActiveMonthGoal(): Double {
        return when (goalType) {
            GoalType.DISTANCE -> monthDistanceGoal
            GoalType.DURATION -> monthDurationGoal
        }
    }
}
