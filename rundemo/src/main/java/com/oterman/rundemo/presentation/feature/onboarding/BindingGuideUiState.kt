package com.oterman.rundemo.presentation.feature.onboarding

import com.oterman.rundemo.domain.model.DataSourceInfo

/**
 * 绑定引导页UI状态
 */
data class BindingGuideUiState(
    val isLoading: Boolean = true,
    val platforms: List<DataSourceInfo> = emptyList(),
    val isCheckingComplete: Boolean = true,
    val shouldNavigateToHome: Boolean = false
) {
    val hasAnyBound: Boolean
        get() = platforms.any { it.isAuthorized }
}
