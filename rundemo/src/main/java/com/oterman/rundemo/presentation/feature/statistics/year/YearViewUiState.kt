package com.oterman.rundemo.presentation.feature.statistics.year

import com.oterman.rundemo.domain.model.YearStatistics

/**
 * UI state for Year Statistics view
 */
data class YearViewUiState(
    val isLoading: Boolean = false,
    val yearDisplay: String = "",              // "2024年"
    val yearStats: YearStatistics = YearStatistics(),
    val canGoNext: Boolean = false,            // Whether can navigate to next year (disabled if current year)
    val dailySentence: String = "",
    val error: String? = null
)
