package com.oterman.rundemo.presentation.feature.datasource.debug

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.domain.model.DataSourcePlatform

/**
 * DataSourceRecordListViewModel工厂类
 */
class DataSourceRecordListViewModelFactory(
    private val context: Context,
    private val platform: DataSourcePlatform
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataSourceRecordListViewModel::class.java)) {
            val dataSourcePreferences = DataSourcePreferences(context)
            val repository = RunDataRepositoryImpl.getInstance(RunDatabase.getInstance(context))

            return DataSourceRecordListViewModel(platform, repository, dataSourcePreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
