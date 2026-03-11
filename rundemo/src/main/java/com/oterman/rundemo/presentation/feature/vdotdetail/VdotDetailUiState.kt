package com.oterman.rundemo.presentation.feature.vdotdetail

/**
 * VDOT详情页数据模型
 */

data class VdotTrendPoint(
    val dateMillis: Long,
    val smoothedValue: Double,
    val rawValue: Double
)

enum class TimePeriod(val label: String, val months: Int) {
    ONE_MONTH("1月", 1),
    THREE_MONTHS("3月", 3),
    SIX_MONTHS("6月", 6),
    ONE_YEAR("1年", 12),
    ALL("全部", 0)
}

data class PredictedRaceTime(
    val label: String,
    val distanceMeters: Double,
    val timeMinutes: Double
)

data class TrainingPaceZone(
    val name: String,
    val description: String,
    val paceDisplay: String
)

data class VdotDetailUiState(
    val isLoading: Boolean = true,
    val currentVdot: Double = 0.0,
    val currentOriginVdot: Double = 0.0,
    val lastUpdateDate: Long = 0L,
    val selectedPeriod: TimePeriod = TimePeriod.THREE_MONTHS,
    val trendPoints: List<VdotTrendPoint> = emptyList(),
    val predictedRaceTimes: List<PredictedRaceTime> = emptyList(),
    val trainingPaces: List<TrainingPaceZone> = emptyList()
)
