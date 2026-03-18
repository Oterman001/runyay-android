package com.oterman.rundemo.presentation.feature.runningshoes.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.repository.RunningShoeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RunningShoeDetailViewModel(
    private val context: Context,
    private val shoeId: String,
    private val repository: RunningShoeRepository = RunningShoeRepository(context)
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunningShoeDetailUiState())
    val uiState: StateFlow<RunningShoeDetailUiState> = _uiState.asStateFlow()

    init {
        loadShoe()
    }

    fun loadShoe() {
        viewModelScope.launch {
            // 首次加载才显示 loading，后续刷新静默更新
            if (_uiState.value.shoe == null) {
                _uiState.update { it.copy(isLoading = true) }
            }
            val shoe = repository.getShoe(shoeId)
            val count = repository.getLinkedRecordsCount(shoeId)
            _uiState.update {
                it.copy(
                    shoe = shoe,
                    linkedRecordsCount = count,
                    isLoading = false,
                    errorMessage = if (shoe == null) "跑鞋不存在" else null
                )
            }
        }
    }

    fun retireShoe() {
        viewModelScope.launch {
            repository.retireShoe(shoeId).onSuccess {
                loadShoe()
                _uiState.update { it.copy(toastMessage = "已退役") }
            }.onFailure { e ->
                _uiState.update { it.copy(toastMessage = "操作失败: ${e.message}") }
            }
        }
    }

    fun reactivateShoe() {
        viewModelScope.launch {
            repository.reactivateShoe(shoeId).onSuccess {
                loadShoe()
                _uiState.update { it.copy(toastMessage = "已恢复使用") }
            }.onFailure { e ->
                _uiState.update { it.copy(toastMessage = "操作失败: ${e.message}") }
            }
        }
    }

    fun deleteShoe() {
        viewModelScope.launch {
            repository.deleteShoe(shoeId).onSuccess {
                repository.deleteShoeFromServer(shoeId)
                _uiState.update { it.copy(navigateBack = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(toastMessage = "删除失败: ${e.message}") }
            }
        }
    }

    fun setAsDefault() {
        viewModelScope.launch {
            repository.setDefaultShoe(shoeId).onSuccess {
                loadShoe()
                _uiState.update { it.copy(toastMessage = "已设为默认跑鞋") }
            }
        }
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun uploadImage(uri: android.net.Uri) {
        viewModelScope.launch {
            repository.uploadImage(shoeId, uri).onSuccess {
                loadShoe()
                _uiState.update { it.copy(toastMessage = "图片已更新") }
            }.onFailure { e ->
                _uiState.update { it.copy(toastMessage = "图片上传失败: ${e.message}") }
            }
        }
    }
}

class RunningShoeDetailViewModelFactory(
    private val context: Context,
    private val shoeId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunningShoeDetailViewModel::class.java)) {
            return RunningShoeDetailViewModel(context, shoeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
