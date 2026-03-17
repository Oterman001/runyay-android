package com.oterman.rundemo.presentation.feature.runningshoes.batchlink

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.repository.RunningShoeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BatchLinkUiState(
    val unlinkedRecords: List<RunRecordEntity> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isLinking: Boolean = false,
    val linkSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val isAllSelected: Boolean
        get() = unlinkedRecords.isNotEmpty() && selectedIds.size == unlinkedRecords.size
}

class BatchLinkViewModel(
    private val context: Context,
    private val shoeId: String,
    private val repository: RunningShoeRepository = RunningShoeRepository(context)
) : ViewModel() {

    private val _uiState = MutableStateFlow(BatchLinkUiState())
    val uiState: StateFlow<BatchLinkUiState> = _uiState.asStateFlow()

    init {
        loadUnlinkedRecords()
    }

    fun loadUnlinkedRecords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val records = repository.getUnlinkedRecords()
            _uiState.update { it.copy(unlinkedRecords = records, isLoading = false) }
        }
    }

    fun toggleSelection(recordId: String) {
        _uiState.update { state ->
            val newSelected = state.selectedIds.toMutableSet()
            if (newSelected.contains(recordId)) {
                newSelected.remove(recordId)
            } else {
                newSelected.add(recordId)
            }
            state.copy(selectedIds = newSelected)
        }
    }

    fun toggleSelectAll() {
        _uiState.update { state ->
            if (state.isAllSelected) {
                state.copy(selectedIds = emptySet())
            } else {
                state.copy(selectedIds = state.unlinkedRecords.map { it.workoutId }.toSet())
            }
        }
    }

    fun confirmLink() {
        val selectedIds = _uiState.value.selectedIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLinking = true) }
            repository.linkRecords(shoeId, selectedIds).onSuccess {
                _uiState.update { it.copy(isLinking = false, linkSuccess = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLinking = false, errorMessage = "关联失败: ${e.message}") }
            }
        }
    }
}

class BatchLinkViewModelFactory(
    private val context: Context,
    private val shoeId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BatchLinkViewModel::class.java)) {
            return BatchLinkViewModel(context, shoeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
