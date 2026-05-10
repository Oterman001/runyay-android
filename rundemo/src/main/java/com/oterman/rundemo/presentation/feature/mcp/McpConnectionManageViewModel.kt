package com.oterman.rundemo.presentation.feature.mcp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.dto.response.McpConnectionDto
import com.oterman.rundemo.data.network.dto.response.McpScopeUtils
import com.oterman.rundemo.data.network.dto.response.OAuth2UserAuthorizationDto
import com.oterman.rundemo.data.repository.McpAuthorizationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class McpConnectionManageViewModel(
    private val repository: McpAuthorizationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(McpConnectionManageUiState())
    val uiState: StateFlow<McpConnectionManageUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load(isRefresh: Boolean = false) {
        _uiState.update {
            it.copy(
                isLoading = !isRefresh && it.connections.isEmpty() && it.usages.isEmpty(),
                isRefreshing = isRefresh,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val connectionsDeferred = async { repository.fetchConnections() }
            val usageDeferred = async { repository.fetchUsage() }
            val partnersDeferred = async { repository.fetchPartnerAuthorizations() }
            val scopesDeferred = async { repository.fetchScopes() }

            val connectionsResult = connectionsDeferred.await()
            val usageResult = usageDeferred.await()
            val partnersResult = partnersDeferred.await()
            val scopesResult = scopesDeferred.await()

            val fatalError = connectionsResult.exceptionOrNull() ?: usageResult.exceptionOrNull()
            _uiState.update { current ->
                current.copy(
                    connections = connectionsResult.getOrDefault(current.connections).filter { it.isActive },
                    usages = usageResult.getOrDefault(current.usages),
                    partnerAuthorizations = partnersResult.getOrDefault(emptyList()).filter { it.isActive },
                    supportedScopes = scopesResult.getOrDefault(current.supportedScopes)
                        .filter { it.scopeCode.isNotEmpty() },
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = fatalError?.message
                )
            }
        }
    }

    fun selectTab(tab: AuthorizationManageTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun showUsageDetail() {
        _uiState.update { it.copy(showUsageDetail = true) }
    }

    fun dismissUsageDetail() {
        _uiState.update { it.copy(showUsageDetail = false) }
    }

    fun selectConnection(connection: McpConnectionDto) {
        _uiState.update { it.copy(selectedConnection = connection) }
        loadScopes(force = true)
    }

    fun dismissConnectionSheet() {
        _uiState.update { it.copy(selectedConnection = null) }
    }

    fun updateScope(connection: McpConnectionDto, scopeCodes: Set<String>, connectionName: String) {
        val current = _uiState.value
        val scope = McpScopeUtils.scopeString(scopeCodes, current.supportedScopes)
        val normalizedName = connectionName.trim().take(50)

        viewModelScope.launch {
            repository.updateScope(
                authorizationId = connection.id,
                scope = scope,
                connectionName = normalizedName
            ).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "连接已更新", selectedConnection = null) }
                load(isRefresh = true)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message, selectedConnection = null) }
            }
        }
    }

    fun prepareRevoke(connection: McpConnectionDto) {
        _uiState.update {
            it.copy(
                pendingRevokeConnection = connection,
                selectedConnection = null
            )
        }
    }

    fun dismissRevokeDialog() {
        _uiState.update { it.copy(pendingRevokeConnection = null) }
    }

    fun confirmRevoke() {
        val connection = _uiState.value.pendingRevokeConnection ?: return
        viewModelScope.launch {
            repository.revokeConnection(connection.id)
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            connections = current.connections.filterNot { it.id == connection.id },
                            pendingRevokeConnection = null,
                            snackbarMessage = "连接已吊销"
                        )
                    }
                    load(isRefresh = true)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message, pendingRevokeConnection = null)
                    }
                }
        }
    }

    fun preparePartnerRevoke(item: OAuth2UserAuthorizationDto) {
        _uiState.update { it.copy(pendingPartnerRevokeItem = item) }
    }

    fun dismissPartnerRevokeDialog() {
        _uiState.update { it.copy(pendingPartnerRevokeItem = null) }
    }

    fun confirmPartnerRevoke() {
        val item = _uiState.value.pendingPartnerRevokeItem ?: return
        val clientId = item.clientId ?: return
        viewModelScope.launch {
            repository.revokePartnerAuthorization(clientId)
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            partnerAuthorizations = current.partnerAuthorizations.filterNot { it.clientId == clientId },
                            pendingPartnerRevokeItem = null,
                            snackbarMessage = "合作方授权已取消"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message, pendingPartnerRevokeItem = null)
                    }
                }
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun loadScopes(force: Boolean = false) {
        if (!force && _uiState.value.supportedScopes.isNotEmpty()) return

        _uiState.update {
            it.copy(
                isLoadingScopes = true,
                scopeErrorMessage = null
            )
        }
        viewModelScope.launch {
            repository.fetchScopes()
                .onSuccess { scopes ->
                    _uiState.update {
                        it.copy(
                            supportedScopes = scopes.filter { scope -> scope.scopeCode.isNotEmpty() },
                            isLoadingScopes = false,
                            scopeErrorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            supportedScopes = emptyList(),
                            isLoadingScopes = false,
                            scopeErrorMessage = error.message ?: "权限列表加载失败"
                        )
                    }
                }
        }
    }
}

class McpConnectionManageViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(McpConnectionManageViewModel::class.java)) {
            val preferencesManager = PreferencesManager(context)
            return McpConnectionManageViewModel(
                McpAuthorizationRepository(preferencesManager)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class AuthorizationSummaryViewModel(
    private val repository: McpAuthorizationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthorizationSummaryUiState())
    val uiState: StateFlow<AuthorizationSummaryUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val connectionsDeferred = async { repository.fetchConnections() }
            val partnersDeferred = async { repository.fetchPartnerAuthorizations() }
            val connections = connectionsDeferred.await().getOrNull()
            val partners = partnersDeferred.await().getOrNull()
            _uiState.update {
                it.copy(
                    mcpConnectionCount = connections?.count { item -> item.isActive },
                    partnerAuthorizationCount = partners?.count { item -> item.isActive },
                    isLoading = false
                )
            }
        }
    }
}

class AuthorizationSummaryViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthorizationSummaryViewModel::class.java)) {
            val preferencesManager = PreferencesManager(context)
            return AuthorizationSummaryViewModel(
                McpAuthorizationRepository(preferencesManager)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
