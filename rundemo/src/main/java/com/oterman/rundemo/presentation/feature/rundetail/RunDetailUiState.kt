package com.oterman.rundemo.presentation.feature.rundetail

import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.domain.model.RunSegment
import com.oterman.rundemo.domain.model.TrackPoint

/**
 * 跑步详情页面UI状态
 */
data class RunDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val record: RunRecordEntity? = null,
    val trackPoints: List<TrackPoint> = emptyList(),
    val segments: List<RunSegment> = emptyList(),
    val metrics: List<RunMetricItem> = emptyList(),
    val isOutdoor: Boolean = true,
    // FIT下载状态
    val isDownloading: Boolean = false,
    val downloadedFitData: ByteArray? = null,
    val downloadError: String? = null,
    val downloadSuccess: Boolean = false
) {
    /**
     * 是否可以下载FIT文件（需要有originId和datasource）
     */
    val canDownloadFit: Boolean
        get() = record?.originId != null && record.datasource != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RunDetailUiState

        if (isLoading != other.isLoading) return false
        if (error != other.error) return false
        if (record != other.record) return false
        if (trackPoints != other.trackPoints) return false
        if (segments != other.segments) return false
        if (metrics != other.metrics) return false
        if (isOutdoor != other.isOutdoor) return false
        if (isDownloading != other.isDownloading) return false
        if (downloadedFitData != null) {
            if (other.downloadedFitData == null) return false
            if (!downloadedFitData.contentEquals(other.downloadedFitData)) return false
        } else if (other.downloadedFitData != null) return false
        if (downloadError != other.downloadError) return false
        if (downloadSuccess != other.downloadSuccess) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + (record?.hashCode() ?: 0)
        result = 31 * result + trackPoints.hashCode()
        result = 31 * result + segments.hashCode()
        result = 31 * result + metrics.hashCode()
        result = 31 * result + isOutdoor.hashCode()
        result = 31 * result + isDownloading.hashCode()
        result = 31 * result + (downloadedFitData?.contentHashCode() ?: 0)
        result = 31 * result + (downloadError?.hashCode() ?: 0)
        result = 31 * result + downloadSuccess.hashCode()
        return result
    }
}

/**
 * 数据网格中的指标项
 */
data class RunMetricItem(
    val value: String,
    val label: String,
    val unit: String? = null
)

/**
 * 布局常量 - 匹配iOS V3LayoutConstants
 */
object RunDetailLayoutConstants {
    // 地图相关
    const val MapHeightRatio = 0.60f
    val MapGradientHeight = 60 // dp

    // Header卡片
    val HeaderInvasionOffset = -25 // dp，卡片向上侵入地图的偏移
    val HeaderCardRadius = 16 // dp
    val HeaderCardPadding = 20 // dp
    val HeaderCardMargin = 16 // dp
    val HeaderShadowElevation = 4 // dp

    // 头像
    val AvatarSize = 50 // dp
    val AvatarVerticalOffset = -22 // dp
    val AvatarTrailingPadding = 30 // dp
    val AvatarBorderWidth = 2 // dp

    // 间距
    val CardSpacing = 12 // dp
    val SectionPadding = 10 // dp

    // 字体大小
    val DistanceFontSize = 48 // sp
    val DistanceUnitFontSize = 18 // sp
    val TimeTypeFontSize = 14 // sp
}
