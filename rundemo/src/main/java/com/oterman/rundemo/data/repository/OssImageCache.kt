package com.oterman.rundemo.data.repository

import android.content.Context

/**
 * 通用 OSS 图片 URL 缓存（内存 + SharedPreferences 磁盘）
 */
class OssImageCache(
    context: Context,
    prefsName: String,
    private val maxCacheAgeMs: Long
) {
    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    @Volatile private var memCache: Map<String, String> = emptyMap()
    @Volatile private var memTimes: Map<String, Long> = emptyMap()

    fun getCachedUrl(id: String): String? {
        // 1. memory
        memCache[id]?.let { url ->
            if (isCacheValid(id, memTimes[id] ?: 0L)) return url
        }
        // 2. disk
        val diskUrl = prefs.getString("url_$id", null)
        val diskTime = prefs.getLong("time_$id", 0L)
        if (diskUrl != null && isCacheValid(id, diskTime)) {
            memCache = memCache + (id to diskUrl)
            memTimes = memTimes + (id to diskTime)
            return diskUrl
        }
        return null
    }

    fun setCachedUrl(id: String, url: String) {
        val now = System.currentTimeMillis()
        memCache = memCache + (id to url)
        memTimes = memTimes + (id to now)
        prefs.edit()
            .putString("url_$id", url)
            .putLong("time_$id", now)
            .apply()
    }

    fun isCacheValid(id: String): Boolean {
        val time = memTimes[id] ?: prefs.getLong("time_$id", 0L)
        return isCacheValid(id, time)
    }

    fun clearCache(id: String) {
        memCache = memCache - id
        memTimes = memTimes - id
        prefs.edit().remove("url_$id").remove("time_$id").apply()
    }

    fun clearAll() {
        memCache = emptyMap()
        memTimes = emptyMap()
        prefs.edit().clear().apply()
    }

    private fun isCacheValid(id: String, fetchTime: Long): Boolean {
        if (maxCacheAgeMs == Long.MAX_VALUE) return fetchTime > 0L
        return System.currentTimeMillis() - fetchTime < maxCacheAgeMs
    }
}
