package com.oterman.rundemo.presentation.feature.datasource.records

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.domain.model.DataSourcePlatform

class PlatformRecordListViewModelFactory(
    private val context: Context,
    private val platform: DataSourcePlatform
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlatformRecordListViewModel::class.java)) {
            val repository = RunDataRepositoryImpl.getInstance(RunDatabase.getInstance(context))
            return PlatformRecordListViewModel(platform, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
