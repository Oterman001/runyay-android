package com.oterman.rundemo.data.repository

import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.RequestBuilder
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.network.api.McpAuthorizationApi
import com.oterman.rundemo.data.network.api.OAuth2AuthorizationApi
import com.oterman.rundemo.data.network.dto.request.DeleteAuthorizationDto
import com.oterman.rundemo.data.network.dto.request.EmptyRequestDto
import com.oterman.rundemo.data.network.dto.request.OAuth2AuthorizationListRequestDto
import com.oterman.rundemo.data.network.dto.request.OAuth2RevokeAuthorizationRequestDto
import com.oterman.rundemo.data.network.dto.request.UpdateScopeDto
import com.oterman.rundemo.data.network.dto.response.McpConnectionDto
import com.oterman.rundemo.data.network.dto.response.McpScopeDescDto
import com.oterman.rundemo.data.network.dto.response.McpToolUsageDto
import com.oterman.rundemo.data.network.dto.response.OAuth2UserAuthorizationDto
import com.oterman.rundemo.util.RLog

class McpAuthorizationRepository(
    private val preferencesManager: PreferencesManager,
    private val mcpApi: McpAuthorizationApi = RetrofitClient.mcpAuthorizationApi,
    private val oauth2Api: OAuth2AuthorizationApi = RetrofitClient.oauth2AuthorizationApi
) {
    companion object {
        private const val TAG = "McpAuthorizationRepo"
    }

    suspend fun fetchConnections(): Result<List<McpConnectionDto>> = runCatching {
        val response = mcpApi.fetchConnections(emptyRequest())
        if (!response.isSuccess()) throw Exception(response.msg)
        response.data?.mcpConnectionDto.orEmpty()
    }.onFailure {
        RLog.e(TAG, "查询 MCP 连接列表失败", it)
    }

    suspend fun fetchUsage(): Result<List<McpToolUsageDto>> = runCatching {
        val response = mcpApi.fetchUsage(emptyRequest())
        if (!response.isSuccess()) throw Exception(response.msg)
        response.data?.toolUsageDto ?: response.data?.mcpToolUsageDto.orEmpty()
    }.onFailure {
        RLog.e(TAG, "查询 MCP 工具用量失败", it)
    }

    suspend fun fetchScopes(): Result<List<McpScopeDescDto>> = runCatching {
        RLog.i(TAG, "开始查询 MCP 权限列表: /api/mcp/scopes")
        val response = mcpApi.fetchScopes(emptyRequest())
        if (!response.isSuccess()) throw Exception(response.msg)
        val scopes = response.data?.scopeDescDto ?: response.data?.mcpScopeDescDto.orEmpty()
        scopes.filter { it.scopeCode.isNotEmpty() }
    }.onFailure {
        RLog.e(TAG, "查询 MCP 权限列表失败", it)
    }

    suspend fun revokeConnection(authorizationId: Long): Result<Unit> = runCatching {
        val request = RequestBuilder.createRequest(
            dtoName = "DeleteAuthorizationDto",
            data = DeleteAuthorizationDto(authorizationId),
            preferencesManager = preferencesManager
        )
        val response = mcpApi.revokeConnection(request)
        if (!response.isSuccess()) throw Exception(response.msg)
    }.onFailure {
        RLog.e(TAG, "吊销 MCP 连接失败", it)
    }

    suspend fun updateScope(
        authorizationId: Long,
        scope: String,
        connectionName: String?
    ): Result<Unit> = runCatching {
        val request = RequestBuilder.createRequest(
            dtoName = "UpdateScopeDto",
            data = UpdateScopeDto(
                authorizationId = authorizationId,
                scope = scope,
                connectionName = connectionName
            ),
            preferencesManager = preferencesManager
        )
        val response = mcpApi.updateScope(request)
        if (!response.isSuccess()) throw Exception(response.msg)
    }.onFailure {
        RLog.e(TAG, "修改 MCP 连接权限失败", it)
    }

    suspend fun fetchPartnerAuthorizations(): Result<List<OAuth2UserAuthorizationDto>> = runCatching {
        val userId = preferencesManager.getUserId().orEmpty()
        if (userId.isBlank()) throw Exception("获取用户ID失败")
        val request = RequestBuilder.createRequest(
            dtoName = "OAuth2UserAuthorizationListRequestDto",
            data = OAuth2AuthorizationListRequestDto(userId),
            preferencesManager = preferencesManager
        )
        val response = oauth2Api.fetchAuthorizations(request)
        if (!response.isSuccess()) throw Exception(response.msg)
        response.data?.oauth2UserAuthorizationDto.orEmpty()
    }.onFailure {
        RLog.e(TAG, "查询合作方授权列表失败", it)
    }

    suspend fun revokePartnerAuthorization(clientId: String): Result<Unit> = runCatching {
        val userId = preferencesManager.getUserId().orEmpty()
        if (userId.isBlank()) throw Exception("获取用户ID失败")
        val request = RequestBuilder.createRequest(
            dtoName = "OAuth2RevokeAuthorizationRequestDto",
            data = OAuth2RevokeAuthorizationRequestDto(userId = userId, clientId = clientId),
            preferencesManager = preferencesManager
        )
        val response = oauth2Api.revokeAuthorization(request)
        if (!response.isSuccess()) throw Exception(response.msg)
    }.onFailure {
        RLog.e(TAG, "取消合作方授权失败", it)
    }

    private fun emptyRequest() = RequestBuilder.createRequest(
        dtoName = "EmptyRequestDTO",
        data = EmptyRequestDto(),
        preferencesManager = preferencesManager
    )
}
