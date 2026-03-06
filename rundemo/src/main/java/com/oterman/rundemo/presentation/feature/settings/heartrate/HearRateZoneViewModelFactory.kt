package com.oterman.rundemo.presentation.feature.settings.heartrate

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.PreferencesManager

class HearRateZoneViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val preferencesManager = PreferencesManager(context)
    private val dataSourcePreferences = DataSourcePreferences(context)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HearRateZoneViewModel(preferencesManager, dataSourcePreferences) as T
    }
}
