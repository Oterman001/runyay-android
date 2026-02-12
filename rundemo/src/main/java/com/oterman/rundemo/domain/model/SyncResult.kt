package com.oterman.rundemo.domain.model

import java.util.Date

/**
 * 数据同步结果
 * 对应iOS的GarminSyncResult/CorosSyncResult
 */
data class SyncResult(
    val success: Boolean,
    val importedCount: Int,
    val error: String? = null,
    val platform: DataSourcePlatform? = null
)

/**
 * 同步状态
 */
data class SyncStatus(
    val isAuthorized: Boolean,
    val lastSyncTimestamp: String,
    val localFileCount: Int = 0,
    val isSyncing: Boolean = false
)

/**
 * 导入的运动记录摘要
 * 用于实时显示导入进度
 */
data class ImportedRunSummary(
    val originId: String,
    val platformCode: String,
    val runDate: Date,
    val distance: Double,
    val duration: Double = 0.0,
    val displayText: String
)

/**
 * 同步时间范围选项
 */
enum class SyncTimeRange(
    val displayName: String,
    val days: Int
) {
    ONE_WEEK("最近一周", 7),
    ONE_MONTH("近1月", 30),
    TWO_MONTHS("近2月", 60),
    THREE_MONTHS("近3月", 90),
    SIX_MONTHS("近半年", 180),
    ONE_YEAR("近1年", 365);

    companion object {
        val allOptions: List<SyncTimeRange> = entries

        /**
         * 获取平台的默认同步时间范围
         */
        fun getDefaultForPlatform(platform: DataSourcePlatform): SyncTimeRange {
            return when (platform) {
                DataSourcePlatform.GARMIN_CHINA -> ONE_YEAR
                DataSourcePlatform.GARMIN_GLOBAL -> ONE_MONTH  // 国际版限制1月
                DataSourcePlatform.COROS -> ONE_YEAR
                else -> ONE_MONTH
            }
        }

        /**
         * 获取平台支持的时间范围选项
         */
        fun getOptionsForPlatform(platform: DataSourcePlatform): List<SyncTimeRange> {
            return when (platform) {
                DataSourcePlatform.GARMIN_GLOBAL -> listOf(ONE_WEEK, ONE_MONTH)  // 国际版限制
                else -> allOptions
            }
        }
    }
}

/**
 * 平台绑定状态
 */
enum class PlatformBindStatus(val code: String, val displayName: String) {
    BOUND("A", "已绑定"),       // "A" = Active/Authorized
    UNBOUND("U", "未绑定"),     // "U" = Unbound
    EXPIRED("E", "已过期"),     // "E" = Expired
    UNKNOWN("-1", "未知");

    val isBound: Boolean
        get() = this == BOUND

    companion object {
        fun fromCode(code: String): PlatformBindStatus {
            return entries.find { it.code == code } ?: UNKNOWN
        }
    }
}

/**
 * 文件信息
 * 用于FIT文件下载
 */
data class FileInfo(
    val id: Int,
    val platformCode: String,
    val summaryId: String,
    val dataDate: String,
    val deviceName: String,
    val ossUrl: String? = null,
    val fitUrl: String? = null
) {
    val fileName: String
        get() = summaryId

    val hasOssUrl: Boolean
        get() = !ossUrl.isNullOrEmpty()

    val hasFitUrl: Boolean
        get() = !fitUrl.isNullOrEmpty()
}

