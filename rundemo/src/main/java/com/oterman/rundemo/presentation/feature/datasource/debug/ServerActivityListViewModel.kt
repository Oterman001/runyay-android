package com.oterman.rundemo.presentation.feature.datasource.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.repository.RunDataRemoteRepository
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ServerActivityListViewModel(
    private val platform: DataSourcePlatform,
    private val remoteRepository: RunDataRemoteRepository,
    private val dataSourcePreferences: DataSourcePreferences
) : ViewModel() {

    companion object {
        private const val TAG = "ServerActivityListVM"
        private const val PAGE_SIZE = 15
        private const val DEBUG_LAST_SYNC_TIME = "20000101000000000"
    }

    private val _uiState = MutableStateFlow(ServerActivityListUiState(platform = platform))
    val uiState: StateFlow<ServerActivityListUiState> = _uiState.asStateFlow()

    init {
        loadFirstPage()
    }

    fun loadFirstPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val result = remoteRepository.getActivityFileList(
                    platformCode = platform.code,
                    pageNum = 1,
                    pageSize = PAGE_SIZE,
                    lastSyncTime = DEBUG_LAST_SYNC_TIME
                )
                result.fold(
                    onSuccess = { files ->
                        _uiState.update {
                            it.copy(
                                items = files,
                                isLoading = false,
                                currentPage = 1,
                                hasMorePages = files.size >= PAGE_SIZE
                            )
                        }
                        RLog.i(TAG, "首页加载成功: ${files.size}条")
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isLoading = false, message = "加载失败: ${e.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, message = "加载异常: ${e.message}")
                }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMorePages) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val nextPage = state.currentPage + 1
                val result = remoteRepository.getActivityFileList(
                    platformCode = platform.code,
                    pageNum = nextPage,
                    pageSize = PAGE_SIZE,
                    lastSyncTime = DEBUG_LAST_SYNC_TIME
                )
                result.fold(
                    onSuccess = { files ->
                        _uiState.update {
                            it.copy(
                                items = it.items + files,
                                isLoadingMore = false,
                                currentPage = nextPage,
                                hasMorePages = files.size >= PAGE_SIZE
                            )
                        }
                        RLog.i(TAG, "加载更多成功: ${files.size}条, page=$nextPage")
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isLoadingMore = false, message = "加载更多失败: ${e.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoadingMore = false, message = "加载更多异常: ${e.message}")
                }
            }
        }
    }

    fun toggleExpand(itemId: Int) {
        _uiState.update { state ->
            val newSet = state.expandedItemIds.toMutableSet()
            if (itemId in newSet) newSet.remove(itemId) else newSet.add(itemId)
            state.copy(expandedItemIds = newSet)
        }
    }

    fun refresh() {
        _uiState.update {
            it.copy(items = emptyList(), currentPage = 0, hasMorePages = true, expandedItemIds = emptySet())
        }
        loadFirstPage()
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
