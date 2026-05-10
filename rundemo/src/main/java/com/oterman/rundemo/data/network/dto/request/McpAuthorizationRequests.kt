package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

data class EmptyRequestDto(
    @SerializedName("empty")
    val empty: String = ""
)

data class DeleteAuthorizationDto(
    @SerializedName("authorizationId")
    val authorizationId: Long
)

data class UpdateScopeDto(
    @SerializedName("authorizationId")
    val authorizationId: Long,

    @SerializedName("scope")
    val scope: String,

    @SerializedName("connectionName")
    val connectionName: String?
)

data class OAuth2AuthorizationListRequestDto(
    @SerializedName("userId")
    val userId: String
)

data class OAuth2RevokeAuthorizationRequestDto(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("clientId")
    val clientId: String
)
