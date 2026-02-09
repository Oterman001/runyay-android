package com.oterman.rundemo.domain.model

import androidx.compose.ui.graphics.Color
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
     * Get countdown circle color based on days remaining
     * Red: <= 7 days
     * Orange: <= 30 days
     * Green: > 30 days
     * Gray: already passed
     */
    fun getCountdownColor(): Color {
        val days = getDaysRemaining()
        return when {
            days < 0 -> Color(0xFF8E8E93)      // Gray - passed
            days <= 7 -> Color(0xFFFF3B30)    // Red - within a week
            days <= 30 -> Color(0xFFFF9500)   // Orange - within a month
            else -> Color(0xFF34C759)          // Green - more than a month
        }
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
