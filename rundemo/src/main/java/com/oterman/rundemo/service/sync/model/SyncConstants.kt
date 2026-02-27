package com.oterman.rundemo.service.sync.model

import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.SyncTimeRange

/**
 * 同步相关常量定义
 * 统一管理同步模块的常量和默认值
 */
object SyncConstants {

    // ============ 时间戳默认值 ============

    /** 默认同步开始时间（17位格式） */
    const val DEFAULT_SYNC_START_TIME = "20251001000000000"

    /** 佳明中国默认同步开始时间 */
    const val GARMIN_CHINA_DEFAULT_START = DEFAULT_SYNC_START_TIME

    /** 佳明国际默认同步开始时间 */
    const val GARMIN_GLOBAL_DEFAULT_START = DEFAULT_SYNC_START_TIME

    /** 高驰默认同步开始时间 */
    const val COROS_DEFAULT_START = DEFAULT_SYNC_START_TIME

    // ============ 分页配置 ============

    /** 默认每页大小 */
    const val DEFAULT_PAGE_SIZE = 15

    /** 最大每页大小 */
    const val MAX_PAGE_SIZE = 50

    /** 最大并发解析数 */
    const val MAX_CONCURRENT_PARSE = 3

    // ============ 频率限制 ============

    /** 手动同步最小间隔（秒） */
    const val MANUAL_SYNC_COOLDOWN_SECONDS = 10

    /** 回填请求后延迟启动同步的时间（毫秒） */
    const val BACKFILL_DELAY_MS = 5000L

    /** 授权状态缓存有效期（秒） */
    const val AUTH_STATUS_CACHE_TTL_SECONDS = 60

    // ============ 通知名称 ============

    /** 同步开始通知 */
    const val NOTIFICATION_SYNC_STARTED = "sync_started"

    /** 同步进度通知 */
    const val NOTIFICATION_SYNC_PROGRESS = "sync_progress"

    /** 单条记录导入通知 */
    const val NOTIFICATION_RECORD_IMPORTED = "record_imported"

    /** 同步完成通知 */
    const val NOTIFICATION_SYNC_COMPLETED = "sync_completed"

    /** 同步失败通知 */
    const val NOTIFICATION_SYNC_FAILED = "sync_failed"

    // ============ 平台特定配置 ============

    /**
     * 获取平台的默认同步时间范围
     */
    fun getDefaultTimeRange(platform: DataSourcePlatform): SyncTimeRange {
        return when (platform) {
            DataSourcePlatform.GARMIN_CHINA -> SyncTimeRange.ONE_YEAR
            DataSourcePlatform.GARMIN_GLOBAL -> SyncTimeRange.ONE_MONTH  // 国际版限制1月
            DataSourcePlatform.COROS -> SyncTimeRange.ONE_YEAR
            else -> SyncTimeRange.ONE_MONTH
        }
    }

    /**
     * 获取平台的默认同步开始时间
     */
    fun getDefaultStartTime(platform: DataSourcePlatform): String {
        return when (platform) {
            DataSourcePlatform.GARMIN_CHINA -> GARMIN_CHINA_DEFAULT_START
            DataSourcePlatform.GARMIN_GLOBAL -> GARMIN_GLOBAL_DEFAULT_START
            DataSourcePlatform.COROS -> COROS_DEFAULT_START
            else -> DEFAULT_SYNC_START_TIME
        }
    }

    /**
     * 获取平台的最大时间范围（天数）
     */
    fun getMaxDays(platform: DataSourcePlatform): Int {
        return when (platform) {
            DataSourcePlatform.GARMIN_CHINA -> 365
            DataSourcePlatform.GARMIN_GLOBAL -> 30  // 国际版限制30天
            DataSourcePlatform.COROS -> 365
            else -> 30
        }
    }

    // ============ DTO名称 ============

    object DtoNames {
        // 佳明
        const val GARMIN_BIND_REQUEST = "GarminBindRequest"
        const val GARMIN_CALLBACK_DTO = "GarminCallBackDto"
        const val GARMIN_UNBIND_REQUEST = "GarminUnbindRequest"
        const val GARMIN_FILE_LIST_REQUEST = "GarminFileListRequest"
        const val GARMIN_FILE_DOWNLOAD_REQUEST = "GarminFileDownloadRequest"
        const val GARMIN_BACKFILL_REQUEST = "GarminBackfillRequest"

        // 高驰
        const val COROS_BIND_REQUEST = "CorosBindRequest"
        const val COROS_CALLBACK_REQUEST = "CorosCallbackRequest"
        const val COROS_UNBIND_REQUEST = "CorosUnbindRequest"
        const val COROS_FILE_LIST_REQUEST = "CorosFileListRequest"
        const val COROS_SPORT_DETAIL_REQUEST = "CorosSportDetailRequest"
        const val COROS_BACKFILL_REQUEST = "CorosBackfillRequest"

        // 健康数据
        const val HEALTH_QUERY_REQUEST = "GarminHealthQueryRequest"
    }
}
