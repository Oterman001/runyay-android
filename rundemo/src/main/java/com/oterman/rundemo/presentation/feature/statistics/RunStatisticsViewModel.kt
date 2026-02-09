package com.oterman.rundemo.presentation.feature.statistics

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 跑步统计页面的ViewModel
 */
class RunStatisticsViewModel(
    initialTab: RunStatisticTab = RunStatisticTab.WEEK
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunStatisticsUiState(selectedTab = initialTab))
    val uiState: StateFlow<RunStatisticsUiState> = _uiState.asStateFlow()

    /**
     * 选择Tab
     */
    fun selectTab(tab: RunStatisticTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /**
     * 根据页面索引选择Tab
     */
    fun selectTabByIndex(index: Int) {
        val tab = RunStatisticTab.entries.getOrElse(index) { RunStatisticTab.WEEK }
        selectTab(tab)
    }
}
