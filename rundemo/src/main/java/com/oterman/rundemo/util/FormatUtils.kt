package com.oterman.rundemo.util

/**
 * Formatting utilities for running statistics display
 * Matches iOS formatting patterns
 */
object FormatUtils {

    /**
     * Format distance intelligently
     * >= 100km: no decimals
     * < 100km: 1 decimal
     */
    fun formatDistance(distance: Double): String = when {
        distance >= 100 -> String.format("%.0f", distance)
        else -> String.format("%.1f", distance)
    }

    /**
     * Format pace in min'sec" format (e.g., 5'30")
     * @param paceMinPerKm pace in minutes per kilometer
     */
    fun formatPace(paceMinPerKm: Double): String {
        if (paceMinPerKm <= 0 || paceMinPerKm > 30) return "--"
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return "$minutes'${String.format("%02d", seconds)}\""
    }

    /**
     * Format calories (no decimals)
     */
    fun formatCalories(calories: Double): String {
        return if (calories > 0) String.format("%.0f", calories) else "--"
    }

    /**
     * Format elevation in meters (no decimals)
     */
    fun formatElevation(meters: Double): String {
        return if (meters > 0) String.format("%.0f", meters) else "--"
    }

    /**
     * Format duration intelligently
     * >= 100 hours: no decimals
     * < 100 hours: 1 decimal
     */
    fun formatDuration(hours: Double): String = when {
        hours >= 100 -> String.format("%.0f", hours)
        else -> String.format("%.1f", hours)
    }

    /**
     * Format duration in hours and minutes separately
     * Returns Pair(hours, minutes)
     */
    fun formatDurationHoursMinutes(totalMinutes: Double): Pair<Int, Int> {
        val hours = (totalMinutes / 60).toInt()
        val minutes = (totalMinutes % 60).toInt()
        return Pair(hours, minutes)
    }
}
