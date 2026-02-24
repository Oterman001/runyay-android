package com.oterman.rundemo.presentation.feature.statistics.total

import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.domain.model.AllTimeTotalStatistics
import com.oterman.rundemo.domain.model.TotalChartDisplayMode

/**
 * UI state for Total Statistics view
 */
data class TotalViewUiState(
    val isLoading: Boolean = false,
    val totalStats: AllTimeTotalStatistics = AllTimeTotalStatistics(),
    val chartDisplayMode: TotalChartDisplayMode = TotalChartDisplayMode.DISTANCE,
    val dailySentence: String = "",
    val error: String? = null,
    // Zone distribution data
    val heartRate7Zones: List<AbilityZone> = emptyList(),
    val heartRate5Zones: List<AbilityZone> = emptyList(),
    val speedZones: List<AbilityZone> = emptyList()
)
