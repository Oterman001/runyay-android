package com.oterman.rundemo.presentation.feature.datasource.debug

import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.UnifiedFileInfo

data class ServerActivityListUiState(
    val platform: DataSourcePlatform,
    val items: List<UnifiedFileInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val currentPage: Int = 0,
    val message: String? = null,
    val expandedItemIds: Set<Int> = emptySet()
)
