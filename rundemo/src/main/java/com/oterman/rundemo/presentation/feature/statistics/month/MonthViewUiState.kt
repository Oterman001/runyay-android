package com.oterman.rundemo.presentation.feature.statistics.month

import com.oterman.rundemo.domain.model.MonthStatistics

/**
 * UI state for Month Statistics view
 */
data class MonthViewUiState(
    val isLoading: Boolean = false,
    val monthYearDisplay: String = "",          // "2024年11月"
    val monthStats: MonthStatistics = MonthStatistics(),
    val canGoNext: Boolean = false,             // Whether can navigate to next month (disabled if current month)
    val dailySentence: String = "",
    val error: String? = null
)
