package com.oterman.rundemo.presentation.feature.datasource.debug

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.repository.RunDataRemoteRepository
import com.oterman.rundemo.domain.model.DataSourcePlatform

class ServerActivityListViewModelFactory(
    private val context: Context,
    private val platform: DataSourcePlatform
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ServerActivityListViewModel::class.java)) {
            val preferencesManager = PreferencesManager(context)
            val dataSourcePreferences = DataSourcePreferences(context)
            val remoteRepository = RunDataRemoteRepository(preferencesManager)
            return ServerActivityListViewModel(platform, remoteRepository, dataSourcePreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
