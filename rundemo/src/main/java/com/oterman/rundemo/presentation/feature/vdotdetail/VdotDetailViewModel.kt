package com.oterman.rundemo.presentation.feature.vdotdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.fit.VdotSpeedCalculator
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.util.FitDataConverter
import com.oterman.rundemo.util.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class VdotDetailViewModel(
    private val repository: RunDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VdotDetailUiState())
    val uiState: StateFlow<VdotDetailUiState> = _uiState.asStateFlow()

    init {
        loadLatestVdot()
        loadTrendData(TimePeriod.THREE_MONTHS)
    }

    fun onPeriodChanged(period: TimePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadTrendData(period)
    }

    private fun loadLatestVdot() {
        viewModelScope.launch {
            val latest = repository.getLatestVdot() ?: return@launch
            val vdot = latest.value
            val predictedTimes = calculatePredictedRaceTimes(vdot)
            val trainingPaces = calculateTrainingPaces(vdot)

            _uiState.update {
                it.copy(
                    currentVdot = vdot,
                    currentOriginVdot = latest.originValue,
                    lastUpdateDate = latest.date,
                    predictedRaceTimes = predictedTimes,
                    trainingPaces = trainingPaces,
                    isLoading = false
                )
            }
        }
    }

    private fun loadTrendData(period: TimePeriod) {
        viewModelScope.launch {
            val endDate = System.currentTimeMillis()
            val startDate = if (period == TimePeriod.ALL) {
                0L
            } else {
                Calendar.getInstance().apply {
                    add(Calendar.MONTH, -period.months)
                }.timeInMillis
            }

            val vdots = repository.getVdotsByDateRange(startDate, endDate)
            // Repository returns DESC, reverse to ASC
            val points = vdots.reversed().map { entity ->
                VdotTrendPoint(
                    dateMillis = entity.date,
                    smoothedValue = entity.value,
                    rawValue = entity.originValue
                )
            }
            _uiState.update { it.copy(trendPoints = points) }
        }
    }

    private fun calculatePredictedRaceTimes(vdot: Double): List<PredictedRaceTime> {
        if (vdot <= 0) return emptyList()
        return listOf(
            PredictedRaceTime("5K", 5000.0, VdotSpeedCalculator.getPredictedRaceTime(vdot, 5000.0)),
            PredictedRaceTime("10K", 10000.0, VdotSpeedCalculator.getPredictedRaceTime(vdot, 10000.0)),
            PredictedRaceTime("半马", 21097.5, VdotSpeedCalculator.getPredictedRaceTime(vdot, 21097.5)),
            PredictedRaceTime("全马", 42195.0, VdotSpeedCalculator.getPredictedRaceTime(vdot, 42195.0))
        )
    }

    private fun calculateTrainingPaces(vdot: Double): List<TrainingPaceZone> {
        if (vdot <= 0) return emptyList()
        val distance = 1000.0

        val easyRange = VdotSpeedCalculator.getEasyPaceRange(vdot, distance)
        val easySlow = easyRange["slow"] ?: return emptyList()
        val easyFast = easyRange["fast"] ?: return emptyList()
        val marathonPace = VdotSpeedCalculator.getMarathonPaceByVo2(vdot, distance)
        val thresholdPace = VdotSpeedCalculator.getThresholdPace(vdot, distance)
        val intervalPace = VdotSpeedCalculator.getIntervalPace(vdot, distance)
        val repetitionPace = VdotSpeedCalculator.getRepetitionPace(vdot, distance)

        return listOf(
            TrainingPaceZone("E 轻松跑", "Easy", "${FormatUtils.formatPace(easySlow)} - ${FormatUtils.formatPace(easyFast)}"),
            TrainingPaceZone("M 马拉松", "Marathon", FormatUtils.formatPace(marathonPace)),
            TrainingPaceZone("T 乳酸阈值", "Threshold", FormatUtils.formatPace(thresholdPace)),
            TrainingPaceZone("I 间歇", "Interval", FormatUtils.formatPace(intervalPace)),
            TrainingPaceZone("R 冲刺", "Repetition", FormatUtils.formatPace(repetitionPace))
        )
    }
}
