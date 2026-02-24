package com.oterman.rundemo.presentation.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.repository.DataSourceRepository

/**
 * BindingGuideViewModel工厂类
 */
class BindingGuideViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BindingGuideViewModel::class.java)) {
            val dataSourcePreferences = DataSourcePreferences(context)
            val preferencesManager = PreferencesManager(context)
            val repository = DataSourceRepository(dataSourcePreferences, preferencesManager)

            return BindingGuideViewModel(repository, dataSourcePreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
