package com.oterman.rundemo.presentation.feature.runningshoes.detail

import com.oterman.rundemo.domain.model.RunningShoe

data class RunningShoeDetailUiState(
    val shoe: RunningShoe? = null,
    val linkedRecordsCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val navigateBack: Boolean = false,
    val toastMessage: String? = null,
    val showDeleteDialog: Boolean = false
)
