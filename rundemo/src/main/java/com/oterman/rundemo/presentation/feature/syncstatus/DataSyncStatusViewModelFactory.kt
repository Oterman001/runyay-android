package com.oterman.rundemo.presentation.feature.syncstatus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oterman.rundemo.service.sync.UnifiedDataSyncManager

class DataSyncStatusViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataSyncStatusViewModel::class.java)) {
            val syncManager = UnifiedDataSyncManager.getInstance(context)
            return DataSyncStatusViewModel(syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
