package com.oterman.rundemo.presentation.feature.share

import android.graphics.Bitmap

/**
 * Activity间传递地图截图的内存缓存单例
 *
 * 地图截图 Bitmap 较大，不适合通过 Intent extras 传递。
 * 使用内存缓存在 RunDetailActivity 和 ShareActivity 之间传递。
 * ShareActivity 取出后即清除，避免内存泄漏。
 */
object ShareDataCache {

    private var mapSnapshot: Bitmap? = null

    /**
     * 存入地图截图（由 RunDetailActivity 调用）
     */
    fun putMapSnapshot(bitmap: Bitmap) {
        mapSnapshot = bitmap
    }

    /**
     * 取出地图截图（由 ShareActivity 调用，取后清除）
     */
    fun takeMapSnapshot(): Bitmap? {
        val snapshot = mapSnapshot
        mapSnapshot = null
        return snapshot
    }

    /**
     * 清除缓存
     */
    fun clear() {
        mapSnapshot?.recycle()
        mapSnapshot = null
    }
}
