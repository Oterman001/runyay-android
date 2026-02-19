package com.oterman.rundemo.presentation.feature.debug.allrecords

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AllRunRecordsUiState(
    val allRecords: List<RunRecordEntity> = emptyList(),
    val filteredRecords: List<RunRecordEntity> = emptyList(),
    val datasources: List<String> = emptyList(),
    val selectedDatasource: String? = null,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isDeleting: Boolean = false
)

class AllRunRecordsViewModel(context: Context) : ViewModel() {

    private val repository = RunDataRepositoryImpl.getInstance(
        RunDatabase.getInstance(context.applicationContext)
    )

    private val _selectedDatasource = MutableStateFlow<String?>(null)
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isSelectionMode = MutableStateFlow(false)
    private val _isDeleting = MutableStateFlow(false)
    private val _datasources = MutableStateFlow<List<String>>(emptyList())

    val uiState: StateFlow<AllRunRecordsUiState> = combine(
        repository.getAllRunRecords(),
        _selectedDatasource,
        _selectedIds,
        _isSelectionMode,
        _isDeleting
    ) { allRecords, datasource, selectedIds, selectionMode, deleting ->
        val filtered = if (datasource == null) {
            allRecords
        } else {
            allRecords.filter { it.datasource == datasource }
        }
        AllRunRecordsUiState(
            allRecords = allRecords,
            filteredRecords = filtered,
            datasources = _datasources.value,
            selectedDatasource = datasource,
            selectedIds = selectedIds,
            isSelectionMode = selectionMode,
            isDeleting = deleting
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AllRunRecordsUiState())

    init {
        loadDatasources()
    }

    private fun loadDatasources() {
        viewModelScope.launch {
            _datasources.value = repository.getAllDatasources()
        }
    }

    fun selectDatasource(datasource: String?) {
        _selectedDatasource.value = datasource
        _selectedIds.value = emptySet()
    }

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedIds.value = emptySet()
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleSelect(workoutId: String) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (contains(workoutId)) remove(workoutId) else add(workoutId)
        }
    }

    fun selectAll() {
        val currentFiltered = uiState.value.filteredRecords
        _selectedIds.value = currentFiltered.map { it.workoutId }.toSet()
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected(onComplete: (Int) -> Unit) {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _isDeleting.value = true
            try {
                repository.deleteRunRecords(ids)
                _selectedIds.value = emptySet()
                loadDatasources()
                onComplete(ids.size)
            } finally {
                _isDeleting.value = false
            }
        }
    }
}

class AllRunRecordsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AllRunRecordsViewModel::class.java)) {
            return AllRunRecordsViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
