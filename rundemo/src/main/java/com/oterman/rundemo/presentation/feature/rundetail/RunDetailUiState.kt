package com.oterman.rundemo.presentation.feature.rundetail

import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.domain.model.ChartDataPoint
import com.oterman.rundemo.domain.model.MergedRunSegment
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

    // 图表时序数据
    val heartRateSeries: List<ChartDataPoint> = emptyList(),
    val speedSeries: List<ChartDataPoint> = emptyList(),
    val cadenceSeries: List<ChartDataPoint> = emptyList(),
    val powerSeries: List<ChartDataPoint> = emptyList(),
    val strideLengthSeries: List<ChartDataPoint> = emptyList(),
    val verticalOscillationSeries: List<ChartDataPoint> = emptyList(),
    val contactTimeSeries: List<ChartDataPoint> = emptyList(),
    val altitudeSeries: List<ChartDataPoint> = emptyList(),

    // 区间数据
    val heartRate7Zones: List<AbilityZone> = emptyList(),
    val heartRate5Zones: List<AbilityZone> = emptyList(),
    val speedZones: List<AbilityZone> = emptyList(),

    // 训练分段
    val trainingSegments: List<RunSegment> = emptyList(),
    val mergedTrainingSegments: List<MergedRunSegment> = emptyList(),
    val expandedSegmentIds: Set<String> = emptySet(),

    // 头像
    val avatarUrl: String? = null,
    val isLoadingAvatar: Boolean = false,

    // VO2Max（来自daily_health表）
    val vo2Max: Double? = null,
    val previousVo2Max: Double? = null,

    // FIT下载状态
    val isDownloading: Boolean = false,
    val downloadedFitData: ByteArray? = null,
    val downloadError: String? = null,
    val downloadSuccess: Boolean = false,

    // 分享状态
    val isPreparingShare: Boolean = false,
    val shareDataReady: Boolean = false
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
        if (heartRateSeries != other.heartRateSeries) return false
        if (speedSeries != other.speedSeries) return false
        if (cadenceSeries != other.cadenceSeries) return false
        if (powerSeries != other.powerSeries) return false
        if (strideLengthSeries != other.strideLengthSeries) return false
        if (verticalOscillationSeries != other.verticalOscillationSeries) return false
        if (contactTimeSeries != other.contactTimeSeries) return false
        if (altitudeSeries != other.altitudeSeries) return false
        if (heartRate7Zones != other.heartRate7Zones) return false
        if (heartRate5Zones != other.heartRate5Zones) return false
        if (speedZones != other.speedZones) return false
        if (trainingSegments != other.trainingSegments) return false
        if (mergedTrainingSegments != other.mergedTrainingSegments) return false
        if (expandedSegmentIds != other.expandedSegmentIds) return false
        if (avatarUrl != other.avatarUrl) return false
        if (isLoadingAvatar != other.isLoadingAvatar) return false
        if (vo2Max != other.vo2Max) return false
        if (previousVo2Max != other.previousVo2Max) return false
        if (isDownloading != other.isDownloading) return false
        if (downloadedFitData != null) {
            if (other.downloadedFitData == null) return false
            if (!downloadedFitData.contentEquals(other.downloadedFitData)) return false
        } else if (other.downloadedFitData != null) return false
        if (downloadError != other.downloadError) return false
        if (downloadSuccess != other.downloadSuccess) return false
        if (isPreparingShare != other.isPreparingShare) return false
        if (shareDataReady != other.shareDataReady) return false

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
        result = 31 * result + heartRateSeries.hashCode()
        result = 31 * result + speedSeries.hashCode()
        result = 31 * result + cadenceSeries.hashCode()
        result = 31 * result + powerSeries.hashCode()
        result = 31 * result + strideLengthSeries.hashCode()
        result = 31 * result + verticalOscillationSeries.hashCode()
        result = 31 * result + contactTimeSeries.hashCode()
        result = 31 * result + altitudeSeries.hashCode()
        result = 31 * result + heartRate7Zones.hashCode()
        result = 31 * result + heartRate5Zones.hashCode()
        result = 31 * result + speedZones.hashCode()
        result = 31 * result + trainingSegments.hashCode()
        result = 31 * result + mergedTrainingSegments.hashCode()
        result = 31 * result + expandedSegmentIds.hashCode()
        result = 31 * result + (avatarUrl?.hashCode() ?: 0)
        result = 31 * result + isLoadingAvatar.hashCode()
        result = 31 * result + (vo2Max?.hashCode() ?: 0)
        result = 31 * result + (previousVo2Max?.hashCode() ?: 0)
        result = 31 * result + isDownloading.hashCode()
        result = 31 * result + (downloadedFitData?.contentHashCode() ?: 0)
        result = 31 * result + (downloadError?.hashCode() ?: 0)
        result = 31 * result + downloadSuccess.hashCode()
        result = 31 * result + isPreparingShare.hashCode()
        result = 31 * result + shareDataReady.hashCode()
        return result
    }
}

/**
 * 数据网格中的指标项
 */
data class RunMetricItem(
    val value: String,
    val label: String,
    val unit: String? = null,
    val tag: RunPerformanceTag? = null,
    val isVdot: Boolean = false
)

/**
 * 性能标签（运动负荷、垂直步幅比等级）
 */
data class RunPerformanceTag(
    val tagName: String,
    val tagColor: Long,
    val tagType: PerformTagType
)

enum class PerformTagType {
    TRAINING_LOAD,
    STRIDE_RATIO
}

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
    val HeaderCardMargin = 12 // dp
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
