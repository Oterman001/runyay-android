package com.oterman.rundemo.presentation.feature.statistics.week

import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.domain.model.WeekStatistics

/**
 * UI state for Week Statistics view
 */
data class WeekViewUiState(
    val isLoading: Boolean = false,
    val weekDateRange: String = "",             // "2024年11月16日-11月22日"
    val weekStats: WeekStatistics = WeekStatistics(),
    val canGoNext: Boolean = false,             // Whether can navigate to next week
    val dailySentence: String = "",
    val error: String? = null,
    // 区间分布数据
    val heartRate7Zones: List<AbilityZone> = emptyList(),
    val heartRate5Zones: List<AbilityZone> = emptyList(),
    val speedZones: List<AbilityZone> = emptyList()
)
