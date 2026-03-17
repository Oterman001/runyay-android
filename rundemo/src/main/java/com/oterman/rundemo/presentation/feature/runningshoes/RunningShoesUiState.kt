package com.oterman.rundemo.presentation.feature.runningshoes

import com.oterman.rundemo.domain.model.RunningShoe
import com.oterman.rundemo.domain.model.ShoeSortType

data class RunningShoesUiState(
    val activeShoes: List<RunningShoe> = emptyList(),
    val retiredShoes: List<RunningShoe> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchKeyword: String = "",
    val sortType: ShoeSortType = ShoeSortType.updateTime,
    val toastMessage: String? = null,
    val selectedTabIndex: Int = 0
)
