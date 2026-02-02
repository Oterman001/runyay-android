package com.oterman.rundemo.presentation.feature.datasource.debug

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.domain.model.DataSourcePlatform

/**
 * DataSourceDebugViewModel工厂类
 */
class DataSourceDebugViewModelFactory(
    private val context: Context,
    private val platform: DataSourcePlatform
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataSourceDebugViewModel::class.java)) {
            val dataSourcePreferences = DataSourcePreferences(context)
            val runRecordDao = RunDatabase.getInstance(context).runRecordDao()

            return DataSourceDebugViewModel(platform, dataSourcePreferences, runRecordDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
