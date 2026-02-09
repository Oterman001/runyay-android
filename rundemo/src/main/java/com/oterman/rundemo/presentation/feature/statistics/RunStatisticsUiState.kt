package com.oterman.rundemo.presentation.feature.statistics

/**
 * 跑步统计页面的UI状态
 */
data class RunStatisticsUiState(
    val selectedTab: RunStatisticTab = RunStatisticTab.WEEK,
    val isLoading: Boolean = false
)
