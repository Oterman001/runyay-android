package com.oterman.rundemo.presentation.feature.datasource

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oterman.rundemo.data.fit.FitImportService
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.repository.DataSourceRepository

/**
 * DataSourceManageViewModel工厂类
 */
class DataSourceManageViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataSourceManageViewModel::class.java)) {
            val dataSourcePreferences = DataSourcePreferences(context)
            val preferencesManager = PreferencesManager(context)
            val repository = DataSourceRepository(dataSourcePreferences, preferencesManager)
            val fitImportService = FitImportService(context)

            return DataSourceManageViewModel(repository, fitImportService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
