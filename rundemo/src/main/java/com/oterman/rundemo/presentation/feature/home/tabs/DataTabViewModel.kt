package com.oterman.rundemo.presentation.feature.home.tabs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * DataTab UI状态
 */
data class DataTabUiState(
    val isLoading: Boolean = true,
    val runRecords: List<RunRecordEntity> = emptyList(),
    val error: String? = null
)

/**
 * DataTab ViewModel
 * 管理跑步记录列表的状态
 */
class DataTabViewModel(
    private val repository: RunDataRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DataTabUiState())
    val uiState: StateFlow<DataTabUiState> = _uiState.asStateFlow()
    
    init {
        loadRunRecords()
    }
    
    /**
     * 加载跑步记录列表
     */
    private fun loadRunRecords() {
        viewModelScope.launch {
            repository.getAllRunRecords()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
                .collect { records ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        runRecords = records,
                        error = null
                    )
                }
        }
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        loadRunRecords()
    }
}

/**
 * DataTabViewModel Factory
 */
class DataTabViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataTabViewModel::class.java)) {
            val database = RunDatabase.getInstance(context)
            val repository = RunDataRepositoryImpl(database)
            return DataTabViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

