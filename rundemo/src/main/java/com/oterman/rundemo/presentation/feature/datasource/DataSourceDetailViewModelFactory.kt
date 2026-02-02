package com.oterman.rundemo.presentation.feature.datasource

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.service.sync.UnifiedDataSyncManager

/**
 * DataSourceDetailViewModel工厂类
 */
class DataSourceDetailViewModelFactory(
    private val context: Context,
    private val platform: DataSourcePlatform
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataSourceDetailViewModel::class.java)) {
            val dataSourcePreferences = DataSourcePreferences(context)
            val preferencesManager = PreferencesManager(context)
            val repository = DataSourceRepository(dataSourcePreferences, preferencesManager)
            val syncManager = UnifiedDataSyncManager.getInstance(context)

            return DataSourceDetailViewModel(platform, repository, syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

