package com.oterman.rundemo.data.network.api

import com.oterman.rundemo.data.network.dto.request.BaseRequest
import com.oterman.rundemo.data.network.dto.request.DeleteAuthorizationDto
import com.oterman.rundemo.data.network.dto.request.EmptyRequestDto
import com.oterman.rundemo.data.network.dto.request.UpdateScopeDto
import com.oterman.rundemo.data.network.dto.response.BaseResponse
import com.oterman.rundemo.data.network.dto.response.McpAuthorizationResponseData
import retrofit2.http.Body
import retrofit2.http.POST

interface McpAuthorizationApi {
    @POST("api/mcp/authorizations")
    suspend fun fetchConnections(
        @Body request: BaseRequest<EmptyRequestDto>
    ): BaseResponse<McpAuthorizationResponseData>

    @POST("api/mcp/authorizations/usage")
    suspend fun fetchUsage(
        @Body request: BaseRequest<EmptyRequestDto>
    ): BaseResponse<McpAuthorizationResponseData>

    @POST("api/mcp/scopes")
    suspend fun fetchScopes(
        @Body request: BaseRequest<EmptyRequestDto>
    ): BaseResponse<McpAuthorizationResponseData>

    @POST("api/mcp/authorizations/delete")
    suspend fun revokeConnection(
        @Body request: BaseRequest<DeleteAuthorizationDto>
    ): BaseResponse<McpAuthorizationResponseData>

    @POST("api/mcp/authorizations/update-scope")
    suspend fun updateScope(
        @Body request: BaseRequest<UpdateScopeDto>
    ): BaseResponse<McpAuthorizationResponseData>
}
