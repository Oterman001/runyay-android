package com.oterman.rundemo.service.sync.model

import com.oterman.rundemo.domain.model.ImportedRunSummary

/**
 * 分页同步结果
 * 表示单次分页请求的同步结果
 */
data class PageSyncResult(
    /** 是否成功 */
    val success: Boolean,

    /** 该页导入的记录数 */
    val importedCount: Int,

    /** 该页总文件数 */
    val totalFilesInPage: Int,

    /** 该页跳过的文件数（已存在） */
    val skippedCount: Int,

    /** 该页失败的文件数 */
    val failedCount: Int,

    /** 导入的记录摘要列表 */
    val importedRecords: List<ImportedRunSummary>,

    /** 该页最新的dataDate（用于更新同步时间戳） */
    val latestDataDate: String? = null,

    /** 该页最早的失败dataDate（用于下次重试） */
    val earliestFailedDataDate: String? = null,

    /** 是否还有下一页 */
    val hasMorePages: Boolean,

    /** 当前页码 */
    val pageNum: Int,

    /** 错误信息列表 */
    val errors: List<String> = emptyList()
) {
    /** 该页是否有失败 */
    val hasErrors: Boolean
        get() = failedCount > 0 || errors.isNotEmpty()

    /** 该页处理的总数 */
    val processedCount: Int
        get() = importedCount + skippedCount + failedCount

    companion object {
        /**
         * 创建成功的分页结果
         */
        fun success(
            importedCount: Int,
            totalFilesInPage: Int,
            skippedCount: Int,
            importedRecords: List<ImportedRunSummary>,
            latestDataDate: String?,
            hasMorePages: Boolean,
            pageNum: Int
        ): PageSyncResult {
            return PageSyncResult(
                success = true,
                importedCount = importedCount,
                totalFilesInPage = totalFilesInPage,
                skippedCount = skippedCount,
                failedCount = 0,
                importedRecords = importedRecords,
                latestDataDate = latestDataDate,
                earliestFailedDataDate = null,
                hasMorePages = hasMorePages,
                pageNum = pageNum
            )
        }

        /**
         * 创建失败的分页结果
         */
        fun failure(
            error: String,
            pageNum: Int
        ): PageSyncResult {
            return PageSyncResult(
                success = false,
                importedCount = 0,
                totalFilesInPage = 0,
                skippedCount = 0,
                failedCount = 0,
                importedRecords = emptyList(),
                latestDataDate = null,
                earliestFailedDataDate = null,
                hasMorePages = false,
                pageNum = pageNum,
                errors = listOf(error)
            )
        }

        /**
         * 创建空结果（没有数据）
         */
        fun empty(pageNum: Int): PageSyncResult {
            return PageSyncResult(
                success = true,
                importedCount = 0,
                totalFilesInPage = 0,
                skippedCount = 0,
                failedCount = 0,
                importedRecords = emptyList(),
                latestDataDate = null,
                earliestFailedDataDate = null,
                hasMorePages = false,
                pageNum = pageNum
            )
        }
    }
}

/**
 * 时间戳追踪器
 * 用于在同步过程中追踪成功和失败的时间戳
 */
class TimestampTracker {
    /** 最新的成功时间戳 */
    private var latestSuccessTimestamp: String? = null

    /** 最早的失败时间戳 */
    private var earliestFailedTimestamp: String? = null

    /**
     * 记录成功的时间戳
     * 保留最大（最晚）的成功时间戳
     */
    fun recordSuccess(timestamp: String) {
        val current = latestSuccessTimestamp
        if (current == null || timestamp > current) {
            latestSuccessTimestamp = timestamp
        }
    }

    /**
     * 记录失败的时间戳
     * 保留最小（最早）的失败时间戳
     */
    fun recordFailure(timestamp: String) {
        val current = earliestFailedTimestamp
        if (current == null || timestamp < current) {
            earliestFailedTimestamp = timestamp
        }
    }

    /**
     * 获取下次同步应使用的时间戳
     * - 如果有失败，返回最早的失败时间戳（用于重试）
     * - 如果没有失败，返回最新的成功时间戳
     */
    fun getNextSyncTimestamp(): String? {
        return earliestFailedTimestamp ?: latestSuccessTimestamp
    }

    /**
     * 是否有错误
     */
    fun hasErrors(): Boolean {
        return earliestFailedTimestamp != null
    }

    /**
     * 获取最新成功时间戳
     */
    fun getLatestSuccessTimestamp(): String? = latestSuccessTimestamp

    /**
     * 获取最早失败时间戳
     */
    fun getEarliestFailedTimestamp(): String? = earliestFailedTimestamp

    /**
     * 重置追踪器
     */
    fun reset() {
        latestSuccessTimestamp = null
        earliestFailedTimestamp = null
    }
}
