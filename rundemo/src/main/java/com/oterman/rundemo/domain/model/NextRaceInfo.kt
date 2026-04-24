package com.oterman.rundemo.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Next race event data model
 * Corresponds to iOS MyRaceRecordDTO
 */
data class NextRaceInfo(
    val id: String,
    val raceName: String,          // "2025 北京马拉松"
    val raceDate: Long,            // timestamp
    val raceType: RaceDistanceType
) {
    /**
     * Calculate days remaining until race
     * Positive = days until race
     * Negative = days since race
     * Zero = race is today
     */
    fun getDaysRemaining(): Int {
        val calendar = Calendar.getInstance()
        val today = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val raceCalendar = Calendar.getInstance().apply {
            timeInMillis = raceDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val raceDay = raceCalendar.timeInMillis

        val diffMs = raceDay - today
        return (diffMs / (1000 * 60 * 60 * 24)).toInt()
    }

    /**
     * Get formatted race date string
     */
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(raceDate))
    }

    /**
     * Get countdown display text
     */
    fun getCountdownText(): Pair<String, String> {
        val days = getDaysRemaining()
        return when {
            days == 0 -> Pair("今天", "")
            days > 0 -> Pair(days.toString(), "天")
            else -> Pair(abs(days).toString(), "天前")
        }
    }
}
