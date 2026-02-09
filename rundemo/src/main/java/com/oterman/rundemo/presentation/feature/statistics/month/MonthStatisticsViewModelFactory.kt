package com.oterman.rundemo.presentation.feature.statistics.month

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl

/**
 * Factory for creating MonthStatisticsViewModel
 */
class MonthStatisticsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MonthStatisticsViewModel::class.java)) {
            val database = RunDatabase.getInstance(context)
            val repository = RunDataRepositoryImpl(database)
            val preferencesManager = PreferencesManager(context)
            return MonthStatisticsViewModel(repository, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
