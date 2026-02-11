package com.oterman.rundemo.domain.trajectory

import android.graphics.Bitmap

/**
 * 轨迹缩略图状态
 * 参考iOS ThumbnailState实现
 */
sealed class ThumbnailState {
    /**
     * 已缓存，可直接显示
     */
    data class Cached(val bitmap: Bitmap) : ThumbnailState()

    /**
     * 正在生成中
     */
    data object Generating : ThumbnailState()

    /**
     * 未开始生成
     */
    data object NotStarted : ThumbnailState()

    /**
     * 生成失败
     */
    data class Failed(val reason: String) : ThumbnailState()

    /**
     * 无轨迹（室内跑等）
     */
    data object NoTrajectory : ThumbnailState()
}
