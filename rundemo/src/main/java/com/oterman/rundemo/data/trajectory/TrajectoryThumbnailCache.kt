package com.oterman.rundemo.data.trajectory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 轨迹缩略图缓存管理
 * 实现分层缓存：内存 (LruCache) + 磁盘 (PNG文件)
 * 参考iOS TrajectoryDisplayManager的缓存设计
 */
class TrajectoryThumbnailCache(context: Context) {

    companion object {
        // 内存缓存配置
        private const val MAX_MEMORY_CACHE_SIZE = 50  // 最多缓存50张缩略图

        // 磁盘缓存配置
        private const val CACHE_DIR_NAME = "trajectory_thumbnails"
        private const val CACHE_EXPIRY_DAYS = 30L

        // 单例
        @Volatile
        private var instance: TrajectoryThumbnailCache? = null

        fun getInstance(context: Context): TrajectoryThumbnailCache {
            return instance ?: synchronized(this) {
                instance ?: TrajectoryThumbnailCache(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    // 内存缓存
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(MAX_MEMORY_CACHE_SIZE) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // 每个条目计数为1，简单计数策略
            return 1
        }
    }

    // 磁盘缓存目录
    private val diskCacheDir: File = File(context.cacheDir, CACHE_DIR_NAME).apply {
        if (!exists()) mkdirs()
    }

    // 磁盘操作锁
    private val diskMutex = Mutex()

    /**
     * 生成缓存键
     * 格式: "{workoutId}_{size}_{isDark}"
     */
    fun getCacheKey(workoutId: String, size: Int, isDark: Boolean): String {
        val themeFlag = if (isDark) "dark" else "light"
        return "${workoutId}_${size}_$themeFlag"
    }

    /**
     * 获取缓存的缩略图
     * 先查内存，再查磁盘
     */
    suspend fun get(key: String): Bitmap? {
        // 1. 先查内存缓存
        memoryCache.get(key)?.let { return it }

        // 2. 查磁盘缓存
        return withContext(Dispatchers.IO) {
            diskMutex.withLock {
                val file = getCacheFile(key)
                if (file.exists()) {
                    try {
                        BitmapFactory.decodeFile(file.absolutePath)?.also { bitmap ->
                            // 放入内存缓存
                            memoryCache.put(key, bitmap)
                        }
                    } catch (e: Exception) {
                        // 文件损坏，删除
                        file.delete()
                        null
                    }
                } else {
                    null
                }
            }
        }
    }

    /**
     * 保存缩略图到缓存
     * 同时保存到内存和磁盘
     */
    suspend fun put(key: String, bitmap: Bitmap) {
        // 1. 保存到内存缓存
        memoryCache.put(key, bitmap)

        // 2. 异步保存到磁盘
        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                try {
                    val file = getCacheFile(key)
                    FileOutputStream(file).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    }
                } catch (e: Exception) {
                    // 忽略磁盘写入失败
                }
            }
        }
    }

    /**
     * 清除指定记录的缓存
     */
    suspend fun clearCache(workoutId: String) {
        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                // 清除内存缓存中该workoutId的所有条目
                val keysToRemove = mutableListOf<String>()
                val snapshot = memoryCache.snapshot()
                snapshot.keys.forEach { key ->
                    if (key.startsWith("${workoutId}_")) {
                        keysToRemove.add(key)
                    }
                }
                keysToRemove.forEach { memoryCache.remove(it) }

                // 清除磁盘缓存
                diskCacheDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("${workoutId}_")) {
                        file.delete()
                    }
                }
            }
        }
    }

    /**
     * 清理过期缓存
     */
    suspend fun cleanup() {
        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                val expiryTime = System.currentTimeMillis() - CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000
                diskCacheDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < expiryTime) {
                        file.delete()
                    }
                }
            }
        }
    }

    /**
     * 清除所有缓存
     */
    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                memoryCache.evictAll()
                diskCacheDir.listFiles()?.forEach { it.delete() }
            }
        }
    }

    /**
     * 获取缓存文件
     */
    private fun getCacheFile(key: String): File {
        return File(diskCacheDir, "$key.png")
    }
}
