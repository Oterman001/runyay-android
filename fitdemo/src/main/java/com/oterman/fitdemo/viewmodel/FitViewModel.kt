package com.oterman.fitdemo.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
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
class FitViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = FitFileRepository(application.applicationContext)
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "FitViewModel"
    }
    
    /**
     * 解析FIT文件
     */
    fun parseFitFile(uri: Uri) {
        Log.d(TAG, "开始解析FIT文件: $uri")
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            Log.d(TAG, "状态更新为: Loading")
            
            try {
                val result = withContext(Dispatchers.IO) {
                    Log.d(TAG, "在IO线程解析文件...")
                    repository.parseFitFile(uri)
                }
                
                result.fold(
                    onSuccess = { data ->
                        Log.d(TAG, "解析成功: ${data}")
                        _uiState.value = UiState.Success(data)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "解析失败", error)
                        _uiState.value = UiState.Error(error.message ?: "未知错误")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "解析过程发生异常", e)
                _uiState.value = UiState.Error(e.message ?: "解析过程发生异常")
            }
        }
    }
    
    /**
     * 重置状态
     */
    fun resetState() {
        Log.d(TAG, "重置状态")
        _uiState.value = UiState.Idle
    }
}

