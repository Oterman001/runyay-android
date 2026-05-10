package com.oterman.rundemo.data.network.dto.response

import com.google.gson.annotations.SerializedName

data class McpAuthorizationResponseData(
    @SerializedName("McpConnectionDto")
    val mcpConnectionDto: List<McpConnectionDto>? = null,

    @SerializedName("ToolUsageDto")
    val toolUsageDto: List<McpToolUsageDto>? = null,

    @SerializedName("McpToolUsageDto")
    val mcpToolUsageDto: List<McpToolUsageDto>? = null,

    @SerializedName("ScopeDescDto")
    val scopeDescDto: List<McpScopeDescDto>? = null,

    @SerializedName("McpScopeDescDto")
    val mcpScopeDescDto: List<McpScopeDescDto>? = null
)

data class OAuth2AuthorizationResponseData(
    @SerializedName("OAuth2UserAuthorizationDto")
    val oauth2UserAuthorizationDto: List<OAuth2UserAuthorizationDto>? = null
)

data class McpConnectionDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("connectionName")
    val connectionName: String? = null,

    @SerializedName("clientName")
    val clientName: String? = null,

    @SerializedName("scope")
    val scope: String? = null,

    @SerializedName("scopeDesc")
    val scopeDesc: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("lastUsedAt")
    val lastUsedAt: String? = null
) {
    val displayName: String
        get() = connectionName.trimmedOrNull()
            ?: clientName.trimmedOrNull()
            ?: "未命名连接"

    val clientDisplayName: String?
        get() = clientName.trimmedOrNull()

    val activeScopeCodes: Set<String>
        get() = McpScopeUtils.scopeCodes(scope)

    val activeScopes: List<McpPermissionScope>
        get() = McpPermissionScope.scopes(scope)

    val permissionSummary: String
        get() = scopeDesc.trimmedOrNull()
            ?: activeScopes.map { it.displayName }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
            ?: "未授予权限"

    val isActive: Boolean
        get() = status == "A" || status == "AUTHORIZED"
}

data class McpToolUsageDto(
    @SerializedName("toolName")
    val toolName: String? = null,

    @SerializedName("toolDesc")
    val toolDesc: String? = null,

    @SerializedName("scope")
    val scope: String? = null,

    @SerializedName("scopeDesc")
    val scopeDesc: String? = null,

    @SerializedName("todayCount")
    val todayCount: Int? = null,

    @SerializedName("todayLimit")
    val todayLimit: Int? = null,

    @SerializedName("monthCount")
    val monthCount: Int? = null,

    @SerializedName("monthLimit")
    val monthLimit: Int? = null
) {
    val id: String
        get() = toolName ?: toolDesc ?: hashCode().toString()

    val usageTitle: String
        get() = toolDesc.trimmedOrNull() ?: "工具"
}

data class McpScopeDescDto(
    @SerializedName("scope")
    val scope: String? = null,

    @SerializedName("scopeDesc")
    val scopeDesc: String? = null
) {
    val scopeCode: String
        get() = scope?.trim() ?: ""

    val displayName: String
        get() = scopeDesc.trimmedOrNull()
            ?: McpPermissionScope.entries.firstOrNull { it.rawValue == scopeCode }?.displayName
            ?: "未知权限"
}

data class OAuth2UserAuthorizationDto(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("userId")
    val userId: String? = null,

    @SerializedName("clientId")
    val clientId: String? = null,

    @SerializedName("clientName")
    val clientName: String? = null,

    @SerializedName("openid")
    val openid: String? = null,

    @SerializedName("scope")
    val scope: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("authorizeTime")
    val authorizeTime: String? = null,

    @SerializedName("revokeTime")
    val revokeTime: String? = null,

    @SerializedName("createTime")
    val createTime: String? = null,

    @SerializedName("updateTime")
    val updateTime: String? = null
) {
    val stableId: String
        get() = id?.toString() ?: clientId ?: hashCode().toString()

    val isActive: Boolean
        get() = status == "A" || status == "AUTHORIZED"
}

enum class McpPermissionScope(val rawValue: String, val displayName: String, val description: String) {
    RunRead("run:read", "读取跑步数据", "查看跑步记录、摘要和分段"),
    RunWrite("run:write", "写入跑步数据", "写入或更新跑步相关数据"),
    PlanRead("plan:read", "读取训练计划", "查看训练计划和课程安排"),
    PlanWrite("plan:write", "写入训练计划", "推送训练计划到账号");

    fun asScopeOption(): McpScopeDescDto = McpScopeDescDto(scope = rawValue, scopeDesc = displayName)

    companion object {
        fun scopes(rawScope: String?): List<McpPermissionScope> {
            val rawValues = McpScopeUtils.scopeCodes(rawScope)
            return entries.filter { rawValues.contains(it.rawValue) }
        }

        val fallbackOptions: List<McpScopeDescDto>
            get() = entries.map { it.asScopeOption() }
    }
}

object McpScopeUtils {
    fun scopeCodes(rawScope: String?): Set<String> {
        return rawScope.orEmpty()
            .split(' ', ',', '，')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    fun scopeString(scopeCodes: Set<String>, orderedBy: List<McpScopeDescDto>): String {
        val orderedCodes = orderedBy
            .map { it.scopeCode }
            .filter { it.isNotEmpty() && scopeCodes.contains(it) }
            .toMutableList()
        val appendedCodes = scopeCodes
            .filter { !orderedCodes.contains(it) }
            .sorted()
        orderedCodes.addAll(appendedCodes)
        return orderedCodes.joinToString(" ")
    }
}

private fun String?.trimmedOrNull(): String? {
    val value = this?.trim()
    return value?.takeIf { it.isNotEmpty() }
}
