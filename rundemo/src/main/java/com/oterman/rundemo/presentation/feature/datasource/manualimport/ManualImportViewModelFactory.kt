package com.oterman.rundemo.presentation.feature.datasource.manualimport

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oterman.rundemo.data.fit.FitImportService
import com.oterman.rundemo.data.gpx.GpxImportService
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl

class ManualImportViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManualImportViewModel::class.java)) {
            val repository = RunDataRepositoryImpl.getInstance(RunDatabase.getInstance(context))
            val fitImportService = FitImportService(context)
            val gpxImportService = GpxImportService(context)
            return ManualImportViewModel(context, repository, fitImportService, gpxImportService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
