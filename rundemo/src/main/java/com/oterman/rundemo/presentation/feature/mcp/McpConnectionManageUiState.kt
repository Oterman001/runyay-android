package com.oterman.rundemo.presentation.feature.mcp

import com.oterman.rundemo.data.network.dto.response.McpConnectionDto
import com.oterman.rundemo.data.network.dto.response.McpScopeDescDto
import com.oterman.rundemo.data.network.dto.response.McpToolUsageDto
import com.oterman.rundemo.data.network.dto.response.OAuth2UserAuthorizationDto

enum class AuthorizationManageTab(val title: String) {
    AI_CONNECTION("AI 连接"),
    PARTNER_AUTHORIZATION("合作方授权")
}

data class McpConnectionManageUiState(
    val connections: List<McpConnectionDto> = emptyList(),
    val usages: List<McpToolUsageDto> = emptyList(),
    val supportedScopes: List<McpScopeDescDto> = emptyList(),
    val isLoadingScopes: Boolean = false,
    val scopeErrorMessage: String? = null,
    val partnerAuthorizations: List<OAuth2UserAuthorizationDto> = emptyList(),
    val selectedTab: AuthorizationManageTab = AuthorizationManageTab.AI_CONNECTION,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val selectedConnection: McpConnectionDto? = null,
    val pendingRevokeConnection: McpConnectionDto? = null,
    val pendingPartnerRevokeItem: OAuth2UserAuthorizationDto? = null,
    val showUsageDetail: Boolean = false
) {
    val activeConnectionCount: Int
        get() = connections.count { it.isActive }

    val todayCount: Int
        get() = usages.sumOf { it.todayCount ?: 0 }

    val monthCount: Int
        get() = usages.sumOf { it.monthCount ?: 0 }

    val todayLimit: Int
        get() = mergedLimit(usages.map { it.todayLimit ?: 0 })

    val monthLimit: Int
        get() = mergedLimit(usages.map { it.monthLimit ?: 0 })

    private fun mergedLimit(limits: List<Int>): Int {
        if (limits.contains(0)) return 0
        return limits.sum()
    }
}

data class AuthorizationSummaryUiState(
    val mcpConnectionCount: Int? = null,
    val partnerAuthorizationCount: Int? = null,
    val isLoading: Boolean = false
)
