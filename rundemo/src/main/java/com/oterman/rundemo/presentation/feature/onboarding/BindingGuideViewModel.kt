package com.oterman.rundemo.presentation.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 绑定引导页ViewModel
 */
class BindingGuideViewModel(
    private val repository: DataSourceRepository,
    private val dataSourcePreferences: DataSourcePreferences
) : ViewModel() {

    companion object {
        private const val TAG = "BindingGuideVM"
    }

    private val _uiState = MutableStateFlow(BindingGuideUiState())
    val uiState: StateFlow<BindingGuideUiState> = _uiState.asStateFlow()

    private var isInitialLoadDone = false

    init {
        checkAndLoadPlatforms()
    }

    private fun checkAndLoadPlatforms() {
        viewModelScope.launch {
            // 1. 检查是否已完成引导
            if (dataSourcePreferences.isBindingGuideCompleted()) {
                RLog.d(TAG, "引导已完成，直接跳转Home")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCheckingComplete = false,
                        shouldNavigateToHome = true
                    )
                }
                isInitialLoadDone = true
                return@launch
            }

            // 2. 加载本地缓存的平台状态（仅展示需要 OAuth 授权的平台，隐藏苹果健康和手动导入）
            val localPlatforms = repository.getAllDataSourceInfos()
                .filter { it.platform.isEnabled && it.platform.requiresOAuthBinding }
            _uiState.update {
                it.copy(platforms = localPlatforms, isCheckingComplete = false)
            }

            // 3. 从服务器刷新绑定状态
            repository.queryPlatformStatus()
                .onSuccess {
                    val updatedPlatforms = repository.getAllDataSourceInfos()
                        .filter { it.platform.isEnabled && it.platform.requiresOAuthBinding }
                    // APPLE_HEALTH 在 Android 上无需 OAuth 授权（isPlatformBound 硬编码返回 true），
                    // MANUAL 为手动导入，不代表用户已绑定运动平台，两者均不纳入"已绑定"判断。
                    // updatedPlatforms 已按 requiresOAuthBinding 过滤，直接检查 isAuthorized 即可。
                    val hasAnyBound = updatedPlatforms.any { it.isAuthorized }

                    if (hasAnyBound) {
                        // 已有绑定，自动标记完成并跳转
                        RLog.d(TAG, "检测到已有绑定平台，自动跳过引导")
                        dataSourcePreferences.setBindingGuideCompleted(true)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                platforms = updatedPlatforms,
                                shouldNavigateToHome = true
                            )
                        }
                    } else {
                        // 无绑定，显示引导页
                        _uiState.update {
                            it.copy(isLoading = false, platforms = updatedPlatforms)
                        }
                    }
                }
                .onFailure { error ->
                    RLog.e(TAG, "刷新平台状态失败", error)
                    _uiState.update { it.copy(isLoading = false) }
                }

            isInitialLoadDone = true
        }
    }

    /**
     * 从详情页返回后刷新绑定状态
     */
    fun refreshAfterBinding() {
        if (!isInitialLoadDone) return
        viewModelScope.launch {
            val updatedPlatforms = repository.getAllDataSourceInfos()
                .filter { it.platform.isEnabled && it.platform.requiresOAuthBinding }
            _uiState.update { it.copy(platforms = updatedPlatforms) }

            // 同时从服务器确认
            repository.queryPlatformStatus()
                .onSuccess {
                    val serverPlatforms = repository.getAllDataSourceInfos()
                        .filter { it.platform.isEnabled && it.platform.requiresOAuthBinding }
                    _uiState.update { it.copy(platforms = serverPlatforms) }
                }
        }
    }

    /**
     * 标记引导已完成
     */
    fun markGuideCompleted() {
        dataSourcePreferences.setBindingGuideCompleted(true)
    }
}
