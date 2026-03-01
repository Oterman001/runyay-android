package com.oterman.rundemo.domain.trajectory

import android.content.Context
import android.graphics.Bitmap
import com.oterman.rundemo.data.trajectory.TrajectoryThumbnailCache
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.presentation.components.trajectory.TrajectoryRenderer
import com.oterman.rundemo.presentation.components.trajectory.getCacheKeySuffix
import com.oterman.rundemo.presentation.components.trajectory.getTrackColor
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 轨迹缩略图管理器
 * 负责缩略图的获取、生成、缓存管理
 * 参考iOS TrajectoryDisplayManager设计
 *
 * 特性：
 * - 分层缓存（内存+磁盘）
 * - 任务去重（防止同一轨迹重复生成）
 * - 主题适配（亮色/暗色分别缓存）
 */
class TrajectoryThumbnailManager private constructor(
    private val context: Context
) {
    companion object {
        // 默认缩略图尺寸（dp转px后的值，会在运行时计算）
        const val DEFAULT_SIZE_DP = 80

        @Volatile
        private var instance: TrajectoryThumbnailManager? = null

        fun getInstance(context: Context): TrajectoryThumbnailManager {
            return instance ?: synchronized(this) {
                instance ?: TrajectoryThumbnailManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    // 缓存管理器
    private val cache = TrajectoryThumbnailCache.getInstance(context)

    // 正在生成的任务（用于任务去重）
    private val generatingTasks = ConcurrentHashMap<String, Deferred<Bitmap?>>()

    // 任务锁
    private val taskMutex = Mutex()

    /**
     * 获取缩略图
     *
     * @param workoutId 运动记录ID
     * @param trackPoints 轨迹点列表（可为null，会返回NoTrajectory状态）
     * @param sizePx 缩略图尺寸（像素）
     * @param isDark 是否为暗色主题
     * @return 缩略图状态
     */
    suspend fun getThumbnail(
        workoutId: String,
        trackPoints: List<TrackPoint>?,
        sizePx: Int,
        isDark: Boolean,
        totalDistanceKm: Double = 0.0
    ): ThumbnailState {
        // 无轨迹数据
        if (trackPoints == null || trackPoints.isEmpty()) {
            return ThumbnailState.NoTrajectory
        }

        // 过滤有效点
        val validPoints = trackPoints.filter { it.isValidCoordinate() }
        if (validPoints.size < 2) {
            return ThumbnailState.NoTrajectory
        }

        val cacheKey = cache.getCacheKey(workoutId, sizePx, isDark) +
            getCacheKeySuffix(totalDistanceKm)
        val trackColorOverride = getTrackColor(totalDistanceKm, isDark)

        // 1. 检查缓存
        cache.get(cacheKey)?.let { bitmap ->
            return ThumbnailState.Cached(bitmap)
        }

        // 2. 检查是否有正在进行的生成任务
        generatingTasks[cacheKey]?.let { existingTask ->
            return try {
                val bitmap = existingTask.await()
                if (bitmap != null) {
                    ThumbnailState.Cached(bitmap)
                } else {
                    ThumbnailState.Failed("生成失败")
                }
            } catch (e: Exception) {
                ThumbnailState.Failed(e.message ?: "生成失败")
            }
        }

        // 3. 启动新的生成任务
        return generateThumbnail(cacheKey, validPoints, sizePx, isDark, trackColorOverride)
    }

    /**
     * 生成缩略图（带任务去重）
     */
    private suspend fun generateThumbnail(
        cacheKey: String,
        trackPoints: List<TrackPoint>,
        sizePx: Int,
        isDark: Boolean,
        trackColorOverride: Int? = null
    ): ThumbnailState = coroutineScope {
        // 使用锁确保只有一个任务被创建
        val task = taskMutex.withLock {
            // 双重检查
            generatingTasks[cacheKey]?.let { return@withLock it }

            // 创建新任务
            val newTask = async(Dispatchers.Default) {
                try {
                    TrajectoryRenderer.render(trackPoints, sizePx, isDark, trackColorOverride)?.also { bitmap ->
                        // 保存到缓存
                        cache.put(cacheKey, bitmap)
                    }
                } catch (e: Exception) {
                    null
                } finally {
                    // 任务完成后移除
                    generatingTasks.remove(cacheKey)
                }
            }

            generatingTasks[cacheKey] = newTask
            newTask
        }

        // 等待任务完成
        try {
            val bitmap = task.await()
            if (bitmap != null) {
                ThumbnailState.Cached(bitmap)
            } else {
                ThumbnailState.Failed("渲染失败")
            }
        } catch (e: Exception) {
            ThumbnailState.Failed(e.message ?: "生成失败")
        }
    }

    /**
     * 预加载多个缩略图
     *
     * @param workoutDataList 包含workoutId和轨迹点的列表
     * @param sizePx 缩略图尺寸
     * @param isDark 是否为暗色主题
     */
    suspend fun preload(
        workoutDataList: List<Pair<String, List<TrackPoint>>>,
        sizePx: Int,
        isDark: Boolean
    ) = withContext(Dispatchers.Default) {
        workoutDataList.forEach { (workoutId, trackPoints) ->
            getThumbnail(workoutId, trackPoints, sizePx, isDark)
        }
    }

    /**
     * 清除指定记录的缓存
     */
    suspend fun clearCache(workoutId: String) {
        cache.clearCache(workoutId)
    }

    /**
     * 清理过期缓存
     */
    suspend fun cleanup() {
        cache.cleanup()
    }

    /**
     * 清除所有缓存
     */
    suspend fun clearAll() {
        cache.clearAll()
        generatingTasks.clear()
    }
}
