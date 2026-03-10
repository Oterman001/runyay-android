package com.oterman.rundemo.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.util.TimestampUtils

/**
 * 数据源本地偏好存储
 * 管理数据源授权状态、优先级和同步时间戳
 */
class DataSourcePreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "data_source_prefs"

        // 授权状态
        private const val KEY_GARMIN_CHINA_BOUND = "garmin_china_bound"
        private const val KEY_GARMIN_GLOBAL_BOUND = "garmin_global_bound"
        private const val KEY_COROS_BOUND = "coros_bound"

        // 同步时间戳（分开存储各平台）
        private const val KEY_GARMIN_CHINA_LAST_SYNC_TIME = "garmin_china_last_sync_time"
        private const val KEY_GARMIN_GLOBAL_LAST_SYNC_TIME = "garmin_global_last_sync_time"
        private const val KEY_COROS_LAST_SYNC_TIME = "coros_last_sync_time"
        private const val KEY_APPLE_HEALTH_LAST_SYNC_TIME = "apple_health_last_sync_time"

        // 旧版时间戳key（用于迁移）
        @Deprecated("Use KEY_GARMIN_CHINA_LAST_SYNC_TIME instead")
        private const val KEY_GARMIN_LAST_SYNC_TIME = "garmin_last_sync_time"

        // 绑定引导
        private const val KEY_BINDING_GUIDE_COMPLETED = "binding_guide_completed"

        // 数据源优先级
        private const val KEY_DATA_SOURCE_ORDER = "data_source_order"

        // 默认同步开始时间 (格式: yyyyMMddHHmmssSSS，17位)
        const val DEFAULT_SYNC_START_TIME = TimestampUtils.DEFAULT_SYNC_START_TIME
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val gson = Gson()
    
    // ============ 授权状态 ============
    
    /**
     * 设置平台绑定状态
     */
    fun setPlatformBound(platform: DataSourcePlatform, isBound: Boolean) {
        val key = when (platform) {
            DataSourcePlatform.GARMIN_CHINA -> KEY_GARMIN_CHINA_BOUND
            DataSourcePlatform.GARMIN_GLOBAL -> KEY_GARMIN_GLOBAL_BOUND
            DataSourcePlatform.COROS -> KEY_COROS_BOUND
            else -> return
        }
        prefs.edit().putBoolean(key, isBound).apply()
    }
    
    /**
     * 获取平台绑定状态
     * 苹果健康（HK）无需绑定，始终返回 true
     */
    fun isPlatformBound(platform: DataSourcePlatform): Boolean {
        if (platform == DataSourcePlatform.APPLE_HEALTH) return true
        val key = when (platform) {
            DataSourcePlatform.GARMIN_CHINA -> KEY_GARMIN_CHINA_BOUND
            DataSourcePlatform.GARMIN_GLOBAL -> KEY_GARMIN_GLOBAL_BOUND
            DataSourcePlatform.COROS -> KEY_COROS_BOUND
            else -> return false
        }
        return prefs.getBoolean(key, false)
    }
    
    /**
     * 更新多个平台的绑定状态
     */
    fun updatePlatformStatus(statusMap: Map<DataSourcePlatform, Boolean>) {
        val editor = prefs.edit()
        statusMap.forEach { (platform, isBound) ->
            val key = when (platform) {
                DataSourcePlatform.GARMIN_CHINA -> KEY_GARMIN_CHINA_BOUND
                DataSourcePlatform.GARMIN_GLOBAL -> KEY_GARMIN_GLOBAL_BOUND
                DataSourcePlatform.COROS -> KEY_COROS_BOUND
                else -> return@forEach
            }
            editor.putBoolean(key, isBound)
        }
        editor.apply()
    }
    
    // ============ 同步时间戳 ============

    /**
     * 获取佳明中国上次同步时间戳（17位格式）
     */
    fun getGarminChinaLastSyncTime(): String {
        // 优先获取新key，如果没有则尝试迁移旧key
        val newValue = prefs.getString(KEY_GARMIN_CHINA_LAST_SYNC_TIME, null)
        if (newValue != null) {
            return TimestampUtils.normalizeTimestamp(newValue)
        }

        // 尝试从旧key迁移
        @Suppress("DEPRECATION")
        val oldValue = prefs.getString(KEY_GARMIN_LAST_SYNC_TIME, null)
        if (oldValue != null) {
            val migrated = TimestampUtils.migrateOldTimestamp(oldValue)
            setGarminChinaLastSyncTime(migrated)
            return migrated
        }

        return DEFAULT_SYNC_START_TIME
    }

    /**
     * 设置佳明中国上次同步时间戳（17位格式）
     */
    fun setGarminChinaLastSyncTime(timestamp: String) {
        val normalized = TimestampUtils.normalizeTimestamp(timestamp)
        prefs.edit().putString(KEY_GARMIN_CHINA_LAST_SYNC_TIME, normalized).apply()
    }

    /**
     * 清除佳明中国同步时间戳
     */
    fun clearGarminChinaSyncTime() {
        prefs.edit().remove(KEY_GARMIN_CHINA_LAST_SYNC_TIME).apply()
    }

    /**
     * 获取佳明国际上次同步时间戳（17位格式）
     */
    fun getGarminGlobalLastSyncTime(): String {
        val value = prefs.getString(KEY_GARMIN_GLOBAL_LAST_SYNC_TIME, null)
        return if (value != null) {
            TimestampUtils.normalizeTimestamp(value)
        } else {
            DEFAULT_SYNC_START_TIME
        }
    }

    /**
     * 设置佳明国际上次同步时间戳（17位格式）
     */
    fun setGarminGlobalLastSyncTime(timestamp: String) {
        val normalized = TimestampUtils.normalizeTimestamp(timestamp)
        prefs.edit().putString(KEY_GARMIN_GLOBAL_LAST_SYNC_TIME, normalized).apply()
    }

    /**
     * 清除佳明国际同步时间戳
     */
    fun clearGarminGlobalSyncTime() {
        prefs.edit().remove(KEY_GARMIN_GLOBAL_LAST_SYNC_TIME).apply()
    }

    /**
     * 获取佳明上次同步时间戳（兼容旧接口，默认返回中国版）
     * @deprecated 使用 getGarminChinaLastSyncTime() 或 getGarminGlobalLastSyncTime()
     */
    @Deprecated("Use getGarminChinaLastSyncTime() or getGarminGlobalLastSyncTime()")
    fun getGarminLastSyncTime(): String {
        return getGarminChinaLastSyncTime()
    }

    /**
     * 设置佳明上次同步时间戳（兼容旧接口，默认设置中国版）
     * @deprecated 使用 setGarminChinaLastSyncTime() 或 setGarminGlobalLastSyncTime()
     */
    @Deprecated("Use setGarminChinaLastSyncTime() or setGarminGlobalLastSyncTime()")
    fun setGarminLastSyncTime(timestamp: String) {
        setGarminChinaLastSyncTime(timestamp)
    }

    /**
     * 清除佳明同步时间戳（兼容旧接口，清除中国版）
     * @deprecated 使用 clearGarminChinaSyncTime() 或 clearGarminGlobalSyncTime()
     */
    @Deprecated("Use clearGarminChinaSyncTime() or clearGarminGlobalSyncTime()")
    fun clearGarminSyncTime() {
        clearGarminChinaSyncTime()
    }

    /**
     * 获取高驰上次同步时间戳（17位格式）
     */
    fun getCorosLastSyncTime(): String {
        val value = prefs.getString(KEY_COROS_LAST_SYNC_TIME, null)
        return if (value != null) {
            TimestampUtils.normalizeTimestamp(value)
        } else {
            DEFAULT_SYNC_START_TIME
        }
    }

    /**
     * 设置高驰上次同步时间戳（17位格式）
     */
    fun setCorosLastSyncTime(timestamp: String) {
        val normalized = TimestampUtils.normalizeTimestamp(timestamp)
        prefs.edit().putString(KEY_COROS_LAST_SYNC_TIME, normalized).apply()
    }

    /**
     * 清除高驰同步时间戳
     */
    fun clearCorosSyncTime() {
        prefs.edit().remove(KEY_COROS_LAST_SYNC_TIME).apply()
    }

    /**
     * 获取苹果健康上次同步时间戳（17位格式）
     */
    fun getAppleHealthLastSyncTime(): String {
        val value = prefs.getString(KEY_APPLE_HEALTH_LAST_SYNC_TIME, null)
        return if (value != null) {
            TimestampUtils.normalizeTimestamp(value)
        } else {
            DEFAULT_SYNC_START_TIME
        }
    }

    /**
     * 设置苹果健康上次同步时间戳（17位格式）
     */
    fun setAppleHealthLastSyncTime(timestamp: String) {
        val normalized = TimestampUtils.normalizeTimestamp(timestamp)
        prefs.edit().putString(KEY_APPLE_HEALTH_LAST_SYNC_TIME, normalized).apply()
    }

    /**
     * 清除苹果健康同步时间戳
     */
    fun clearAppleHealthSyncTime() {
        prefs.edit().remove(KEY_APPLE_HEALTH_LAST_SYNC_TIME).apply()
    }

    /**
     * 获取指定平台的同步时间戳
     */
    fun getLastSyncTime(platform: DataSourcePlatform): String {
        return when (platform) {
            DataSourcePlatform.GARMIN_CHINA -> getGarminChinaLastSyncTime()
            DataSourcePlatform.GARMIN_GLOBAL -> getGarminGlobalLastSyncTime()
            DataSourcePlatform.COROS -> getCorosLastSyncTime()
            DataSourcePlatform.APPLE_HEALTH -> getAppleHealthLastSyncTime()
            else -> DEFAULT_SYNC_START_TIME
        }
    }

    /**
     * 设置指定平台的同步时间戳
     */
    fun setLastSyncTime(platform: DataSourcePlatform, timestamp: String) {
        when (platform) {
            DataSourcePlatform.GARMIN_CHINA -> setGarminChinaLastSyncTime(timestamp)
            DataSourcePlatform.GARMIN_GLOBAL -> setGarminGlobalLastSyncTime(timestamp)
            DataSourcePlatform.COROS -> setCorosLastSyncTime(timestamp)
            DataSourcePlatform.APPLE_HEALTH -> setAppleHealthLastSyncTime(timestamp)
            else -> {}
        }
    }

    /**
     * 清除指定平台的同步时间戳
     */
    fun clearSyncTime(platform: DataSourcePlatform) {
        when (platform) {
            DataSourcePlatform.GARMIN_CHINA -> clearGarminChinaSyncTime()
            DataSourcePlatform.GARMIN_GLOBAL -> clearGarminGlobalSyncTime()
            DataSourcePlatform.COROS -> clearCorosSyncTime()
            DataSourcePlatform.APPLE_HEALTH -> clearAppleHealthSyncTime()
            else -> {}
        }
    }
    
    // ============ 绑定引导 ============

    fun isBindingGuideCompleted(): Boolean = prefs.getBoolean(KEY_BINDING_GUIDE_COMPLETED, false)

    fun setBindingGuideCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_BINDING_GUIDE_COMPLETED, completed).apply()
    }

    // ============ 数据源优先级 ============
    
    /**
     * 获取数据源优先级排序
     * 返回格式: Map<平台code, 优先级数字>
     */
    fun getDataSourceOrder(): Map<String, Int> {
        val json = prefs.getString(KEY_DATA_SOURCE_ORDER, null)
        if (json.isNullOrEmpty()) {
            return getDefaultOrder()
        }
        
        return try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            getDefaultOrder()
        }
    }
    
    /**
     * 保存数据源优先级排序
     */
    fun saveDataSourceOrder(order: Map<String, Int>) {
        val json = gson.toJson(order)
        prefs.edit().putString(KEY_DATA_SOURCE_ORDER, json).apply()
    }
    
    /**
     * 获取默认排序
     */
    private fun getDefaultOrder(): Map<String, Int> {
        return mapOf(
            DataSourcePlatform.GARMIN_CHINA.code to 1,
            DataSourcePlatform.GARMIN_GLOBAL.code to 2,
            DataSourcePlatform.COROS.code to 3,
            DataSourcePlatform.APPLE_HEALTH.code to 4,
            DataSourcePlatform.MANUAL.code to 5
        )
    }
    
    /**
     * 重置为默认排序
     */
    fun resetToDefaultOrder() {
        saveDataSourceOrder(getDefaultOrder())
    }
    
    /**
     * 获取指定平台的优先级
     */
    fun getPriorityNumber(platform: DataSourcePlatform): Int {
        val order = getDataSourceOrder()
        return order[platform.code] ?: 999
    }
    
    // ============ 清理 ============
    
    /**
     * 清除所有数据源相关数据
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

