package com.oterman.rundemo.presentation.feature.runningshoes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.repository.RunningShoeRepository
import com.oterman.rundemo.domain.model.ShoeSortType
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RunningShoesViewModel(
    private val context: Context,
    private val repository: RunningShoeRepository = RunningShoeRepository(context),
    private val preferencesManager: PreferencesManager = PreferencesManager(context)
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunningShoesUiState())
    val uiState: StateFlow<RunningShoesUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun initialize() {
        observeShoes()
        viewModelScope.launch {
            try {
                repository.pullFromServer()
            } catch (e: Exception) {
                RLog.e("RunningShoesVM", "pullFromServer failed", e)
            }
            try {
                repository.syncToServer()
            } catch (e: Exception) {
                RLog.e("RunningShoesVM", "syncToServer failed", e)
            }
            try {
                repository.retryPendingImageUploads()
            } catch (e: Exception) {
                RLog.e("RunningShoesVM", "retryPendingImageUploads failed", e)
            }
        }
    }

    private fun observeShoes() {
        viewModelScope.launch {
            repository.getActiveShoes().collect { shoes ->
                val sorted = repository.sortShoes(shoes, _uiState.value.sortType)
                _uiState.update { it.copy(activeShoes = sorted, isLoading = false) }
            }
        }
        viewModelScope.launch {
            repository.getRetiredShoes().collect { shoes ->
                val sorted = repository.sortShoes(shoes, _uiState.value.sortType)
                _uiState.update { it.copy(retiredShoes = sorted) }
            }
        }
    }

    fun searchShoes(keyword: String) {
        _uiState.update { it.copy(searchKeyword = keyword) }
        searchJob?.cancel()
        if (keyword.isBlank()) {
            observeShoes()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            val results = repository.searchShoes(keyword)
            _uiState.update {
                it.copy(
                    activeShoes = results.filter { shoe -> shoe.isActive },
                    retiredShoes = results.filter { shoe -> !shoe.isActive }
                )
            }
        }
    }

    fun setSortType(sortType: ShoeSortType) {
        _uiState.update {
            it.copy(
                sortType = sortType,
                activeShoes = repository.sortShoes(it.activeShoes, sortType),
                retiredShoes = repository.sortShoes(it.retiredShoes, sortType)
            )
        }
    }

    fun setDefaultShoe(shoeId: String) {
        viewModelScope.launch {
            repository.setDefaultShoe(shoeId).onSuccess {
                _uiState.update { it.copy(toastMessage = "已设为默认跑鞋") }
            }.onFailure { e ->
                _uiState.update { it.copy(toastMessage = "设置失败: ${e.message}") }
            }
        }
    }

    fun deleteShoe(shoeId: String) {
        viewModelScope.launch {
            repository.deleteShoe(shoeId).onSuccess {
                _uiState.update { it.copy(toastMessage = "已删除") }
                // Also delete from server
                repository.deleteShoeFromServer(shoeId)
            }.onFailure { e ->
                _uiState.update { it.copy(toastMessage = "删除失败: ${e.message}") }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun isLoggedIn(): Boolean = preferencesManager.getUserToken() != null
}

class RunningShoesViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunningShoesViewModel::class.java)) {
            return RunningShoesViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
