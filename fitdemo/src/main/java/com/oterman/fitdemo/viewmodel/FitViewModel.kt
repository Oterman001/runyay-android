package com.oterman.fitdemo.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.fitdemo.data.model.UiState
import com.oterman.fitdemo.data.repository.FitFileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * FIT文件解析ViewModel
 */
class FitViewModel(context: Context) : ViewModel() {
    
    private val repository = FitFileRepository(context)
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    /**
     * 解析FIT文件
     */
    fun parseFitFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.parseFitFile(uri)
                }
                
                result.fold(
                    onSuccess = { data ->
                        _uiState.value = UiState.Success(data)
                    },
                    onFailure = { error ->
                        _uiState.value = UiState.Error(error.message ?: "未知错误")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "解析过程发生异常")
            }
        }
    }
    
    /**
     * 重置状态
     */
    fun resetState() {
        _uiState.value = UiState.Idle
    }
}

