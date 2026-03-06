package com.oterman.rundemo.presentation.feature.onboarding.physio

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oterman.rundemo.data.local.PreferencesManager

class PhysioSetupViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val preferencesManager = PreferencesManager(context)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PhysioSetupViewModel(preferencesManager) as T
    }
}
