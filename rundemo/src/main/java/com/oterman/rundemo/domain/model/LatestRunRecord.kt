package com.oterman.rundemo.domain.model

/**
 * Latest run record display model
 * Corresponds to iOS LatestRunRecordView data
 */
data class LatestRunRecord(
    val workoutId: String,
    val runDate: String,           // "2月8日 周六"
    val startEndTime: String,      // "06:30-07:15"
    val totalDistance: Double,     // km
    val duration: String,          // "45'30\""
    val avgPace: String,           // "4'20\""
    val deviceName: String,        // "Garmin FR965"
    val isVerified: Boolean        // true = green dot, false = gray dot
) {
    /**
     * Get formatted distance string
     */
    fun getFormattedDistance(): String {
        return String.format("%.2f", totalDistance)
    }
}
