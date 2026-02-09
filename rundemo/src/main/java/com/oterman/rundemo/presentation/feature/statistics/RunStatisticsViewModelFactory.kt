package com.oterman.rundemo.presentation.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * RunStatisticsViewModel的工厂类
 */
class RunStatisticsViewModelFactory(
    private val initialTab: RunStatisticTab
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunStatisticsViewModel::class.java)) {
            return RunStatisticsViewModel(initialTab) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
