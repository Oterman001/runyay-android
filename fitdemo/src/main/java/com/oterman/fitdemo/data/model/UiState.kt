package com.oterman.fitdemo.data.model

/**
 * UI状态封装
 */
sealed class UiState {
    /**
     * 空闲状态 - 初始状态
     */
    data object Idle : UiState()
    
    /**
     * 加载中 - 正在解析FIT文件
     */
    data object Loading : UiState()
    
    /**
     * 成功状态 - 解析成功，包含数据
     */
    data class Success(val data: FitSummaryData) : UiState()
    
    /**
     * 错误状态 - 解析失败
     */
    data class Error(val message: String) : UiState()
}

