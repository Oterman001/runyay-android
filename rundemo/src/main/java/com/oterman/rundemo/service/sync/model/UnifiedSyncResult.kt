package com.oterman.rundemo.service.sync.model

import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.SyncResult

/**
 * 统一同步结果
 * 包含多个平台的同步结果汇总
 */
data class UnifiedSyncResult(
    /** 是否整体成功（所有平台都成功） */
    val overallSuccess: Boolean,

    /** 各平台的同步结果 */
    val platformResults: Map<DataSourcePlatform, SyncResult>,

    /** 总导入记录数 */
    val totalImportedCount: Int,

    /** 成功的平台数 */
    val successfulPlatforms: Int,

    /** 失败的平台数 */
    val failedPlatforms: Int,

    /** 汇总错误信息 */
    val errors: List<String> = emptyList(),

    /** 同步开始时间 */
    val startTime: Long = System.currentTimeMillis(),

    /** 同步结束时间 */
    val endTime: Long = System.currentTimeMillis()
) {
    /** 同步耗时（毫秒） */
    val durationMs: Long
        get() = endTime - startTime

    /** 是否有任何平台成功 */
    val hasAnySuccess: Boolean
        get() = successfulPlatforms > 0

    /** 是否有任何平台失败 */
    val hasAnyFailure: Boolean
        get() = failedPlatforms > 0

    /** 获取指定平台的结果 */
    fun getResultForPlatform(platform: DataSourcePlatform): SyncResult? {
        return platformResults[platform]
    }

    /** 获取成功的平台列表 */
    fun getSuccessfulPlatformList(): List<DataSourcePlatform> {
        return platformResults.filter { it.value.success }.keys.toList()
    }

    /** 获取失败的平台列表 */
    fun getFailedPlatformList(): List<DataSourcePlatform> {
        return platformResults.filter { !it.value.success }.keys.toList()
    }

    companion object {
        /**
         * 从多个平台结果创建统一结果
         */
        fun fromPlatformResults(
            results: Map<DataSourcePlatform, SyncResult>,
            startTime: Long = System.currentTimeMillis()
        ): UnifiedSyncResult {
            val successCount = results.count { it.value.success }
            val failCount = results.count { !it.value.success }
            val totalImported = results.values.sumOf { it.importedCount }
            val errors = results.values
                .mapNotNull { it.error }
                .filter { it.isNotBlank() }

            return UnifiedSyncResult(
                overallSuccess = failCount == 0,
                platformResults = results,
                totalImportedCount = totalImported,
                successfulPlatforms = successCount,
                failedPlatforms = failCount,
                errors = errors,
                startTime = startTime,
                endTime = System.currentTimeMillis()
            )
        }

        /**
         * 创建空结果（无平台需要同步）
         */
        fun empty(): UnifiedSyncResult {
            return UnifiedSyncResult(
                overallSuccess = true,
                platformResults = emptyMap(),
                totalImportedCount = 0,
                successfulPlatforms = 0,
                failedPlatforms = 0
            )
        }

        /**
         * 创建单平台结果
         */
        fun fromSinglePlatform(result: SyncResult): UnifiedSyncResult {
            val platform = result.platform ?: return empty()
            return fromPlatformResults(mapOf(platform to result))
        }
    }
}

/**
 * 同步通知
 * 用于通知UI同步状态变化
 */
sealed class SyncNotification {
    /** 平台同步开始 */
    data class PlatformStarted(val platform: DataSourcePlatform) : SyncNotification()

    /** 平台同步进度 */
    data class PlatformProgress(
        val platform: DataSourcePlatform,
        val current: Int,
        val total: Int,
        val message: String
    ) : SyncNotification()

    /** 单条记录导入 */
    data class RecordImported(
        val platform: DataSourcePlatform,
        val originId: String,
        val displayText: String
    ) : SyncNotification()

    /** 平台同步完成 */
    data class PlatformCompleted(
        val platform: DataSourcePlatform,
        val result: SyncResult
    ) : SyncNotification()

    /** 平台同步失败 */
    data class PlatformFailed(
        val platform: DataSourcePlatform,
        val error: String
    ) : SyncNotification()

    /** 回填请求成功 */
    data class BackfillCompleted(val platform: DataSourcePlatform) : SyncNotification()

    /** 统一同步完成 */
    data class UnifiedCompleted(val result: UnifiedSyncResult) : SyncNotification()

    /** 统一同步失败 */
    data class UnifiedFailed(val error: String) : SyncNotification()
}
