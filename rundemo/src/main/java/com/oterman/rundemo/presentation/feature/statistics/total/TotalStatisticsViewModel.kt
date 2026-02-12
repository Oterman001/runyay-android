package com.oterman.rundemo.presentation.feature.statistics.total

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.mock.MockDataProvider
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.domain.model.AllTimeTotalStatistics
import com.oterman.rundemo.domain.model.TotalChartDisplayMode
import com.oterman.rundemo.domain.model.YearlyStatistic
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel for Total Statistics view
 * Handles loading all-time statistics and yearly breakdown
 */
class TotalStatisticsViewModel(
    private val repository: RunDataRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "TotalStatisticsVM"
    }

    private val _uiState = MutableStateFlow(TotalViewUiState())
    val uiState: StateFlow<TotalViewUiState> = _uiState.asStateFlow()

    init {
        loadTotalData()
        loadDailySentence()
    }

    /**
     * Toggle chart display mode (distance <-> duration)
     */
    fun toggleChartDisplayMode() {
        _uiState.update { state ->
            val newMode = when (state.chartDisplayMode) {
                TotalChartDisplayMode.DISTANCE -> TotalChartDisplayMode.DURATION
                TotalChartDisplayMode.DURATION -> TotalChartDisplayMode.DISTANCE
            }
            state.copy(chartDisplayMode = newMode)
        }
    }

    /**
     * Refresh data
     */
    fun refresh() {
        loadTotalData()
        loadDailySentence()
    }

    private fun loadTotalData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                RLog.d(TAG, "Loading total statistics data")

                // Get all records from the beginning of time
                val allRecords = repository.getAllRunRecords().first()

                // Group records by year
                val recordsByYear = groupRecordsByYear(allRecords)

                // Calculate yearly statistics
                val yearlyStats = mutableListOf<YearlyStatistic>()
                var totalDistance = 0.0
                var totalDurationMinutes = 0.0
                var totalElevation = 0.0
                var runCount = 0
                var maxYearDistance = 0.0
                var maxYearDuration = 0.0

                for ((year, records) in recordsByYear.toSortedMap()) {
                    val yearStat = calculateYearlyStatistic(year, records)
                    yearlyStats.add(yearStat)

                    totalDistance += yearStat.totalDistance
                    totalDurationMinutes += yearStat.totalDurationMinutes
                    totalElevation += yearStat.totalElevation
                    runCount += yearStat.runCount

                    maxYearDistance = maxOf(maxYearDistance, yearStat.totalDistance)
                    maxYearDuration = maxOf(maxYearDuration, yearStat.totalDurationMinutes)
                }

                // Calculate overall average pace
                val avgPace = if (totalDistance > 0 && totalDurationMinutes > 0) {
                    formatPace(totalDistance / (totalDurationMinutes / 60.0))
                } else {
                    "--'--\""
                }

                val totalStats = AllTimeTotalStatistics(
                    totalDistance = totalDistance,
                    totalDurationMinutes = totalDurationMinutes,
                    avgPace = avgPace,
                    totalElevation = totalElevation,
                    runCount = runCount,
                    yearlyStatistics = yearlyStats,
                    maxYearDistance = maxYearDistance,
                    maxYearDuration = maxYearDuration
                )

                RLog.d(TAG, "Total stats loaded: $runCount runs, ${String.format("%.1f", totalDistance)} km, ${yearlyStats.size} years")

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalStats = totalStats,
                        error = null
                    )
                }
            } catch (e: Exception) {
                RLog.e(TAG, "Failed to load total data", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load total data"
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
     * Group records by year
     */
    private fun groupRecordsByYear(records: List<RunRecordEntity>): Map<Int, List<RunRecordEntity>> {
        val calendar = Calendar.getInstance()
        return records.groupBy { record ->
            calendar.timeInMillis = record.startTime
            calendar.get(Calendar.YEAR)
        }
    }

    /**
     * Calculate yearly statistic from records
     */
    private fun calculateYearlyStatistic(year: Int, records: List<RunRecordEntity>): YearlyStatistic {
        val totalDistance = records.sumOf { it.totalDistance }
        val totalDurationMinutes = records.sumOf { it.activeDuration }
        val totalElevation = records.sumOf { it.elevationAscended }
        val runCount = records.size
        val totalEnergy = records.sumOf { it.totalCalories }

        // Calculate average pace
        val avgPace = if (totalDistance > 0 && totalDurationMinutes > 0) {
            val avgSpeed = totalDistance / (totalDurationMinutes / 60.0)
            formatPace(avgSpeed)
        } else {
            "--'--\""
        }

        return YearlyStatistic(
            year = year,
            totalDistance = totalDistance,
            totalDurationMinutes = totalDurationMinutes,
            avgPace = avgPace,
            totalElevation = totalElevation,
            runCount = runCount,
            totalEnergy = totalEnergy
        )
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
}
