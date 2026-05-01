package com.oterman.rundemo.presentation.feature.share

import android.graphics.Bitmap
import android.net.Uri
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.domain.model.ChartDataPoint
import com.oterman.rundemo.domain.model.MergedRunSegment
import com.oterman.rundemo.domain.model.RunSegment
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.domain.model.RunningShoe
import com.oterman.rundemo.presentation.feature.rundetail.RunMetricItem

/**
 * 分享模式
 */
enum class ShareMode(val displayName: String) {
    TEMPLATE("模板"),
    SHORT("短图"),
    LONG("长图")
}

/**
 * 模板分享占位定义
 */
data class ShareTemplateSpec(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val isAvailable: Boolean = false,
    val supportsBackgroundReplace: Boolean = false
)

/**
 * 短图可选指标类型
 */
enum class ShareMetricType(val displayName: String, val unit: String?) {
    DURATION("运动时间", null),
    VDOT("动态跑力", null),
    PACE("平均配速", "/km"),
    TRAINING_LOAD("运动负荷", "TL"),
    AVG_HEART_RATE("平均心率", "bpm"),
    MAX_HEART_RATE("最大心率", "bpm"),
    AVG_STRIDE_LENGTH("平均步幅", "cm"),
    AVG_CADENCE("平均步频", "/min"),
    ELEVATION("累计上升", "m"),
    VERTICAL_STRIDE_RATIO("垂直步幅比", "%"),
    CALORIES("消耗能量", "kcal"),
    AVG_POWER("平均功率", "W"),
    DISTANCE("距离", "km")
}

/**
 * 长图中的卡片类型
 */
enum class ShareCardType(val displayName: String) {
    HEADER("基本信息"),
    VO2MAX("最大摄氧量"),
    TRAINING_EFFECT("训练效果"),
    KM_SEGMENTS("公里分段"),
    TRAINING_SEGMENTS("训练分段"),
    HEART_RATE("心率"),
    PACE("配速"),
    ALTITUDE("海拔"),
    STRIDE_LENGTH("步幅"),
    CADENCE("步频"),
    CONTACT_TIME("触地时间"),
    VERTICAL_OSCILLATION("垂直振幅"),
    POWER("功率"),
    LINKED_SHOE("关联跑鞋")
}

/**
 * 分享页面UI状态
 */
data class ShareUiState(
    // 当前模式
    val shareMode: ShareMode = ShareMode.TEMPLATE,

    // 模板分享
    val templates: List<ShareTemplateSpec> = defaultTemplates,
    val selectedTemplateId: String = defaultTemplates.first().id,

    // 数据加载
    val isLoading: Boolean = true,
    val error: String? = null,

    // 跑步数据（从Room加载）
    val record: RunRecordEntity? = null,
    val trackPoints: List<TrackPoint> = emptyList(),
    val metrics: List<RunMetricItem> = emptyList(),
    val isOutdoor: Boolean = true,

    // 图表数据
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

    // 分段
    val segments: List<RunSegment> = emptyList(),
    val trainingSegments: List<RunSegment> = emptyList(),
    val mergedTrainingSegments: List<MergedRunSegment> = emptyList(),

    // 公里分段视图状态（从详情页同步）
    val segmentBarChartMode: Boolean = false,
    val segmentBarChartMetricIndex: Int = 0,
    val segmentBarChartGroupSize: Int = 1,

    // VO2Max
    val vo2Max: Double? = null,
    val previousVo2Max: Double? = null,

    // 地图截图
    val mapSnapshot: Bitmap? = null,

    // 隐私模式
    val isPrivacyMode: Boolean = false,

    // 头像
    val avatarUrl: String? = null,
    val userName: String? = null,

    // 关联跑鞋
    val linkedShoe: RunningShoe? = null,

    // 短图编辑设置
    val selectedMetrics: List<ShareMetricType> = defaultShortMetrics,
    val availableMetrics: List<ShareMetricType> = emptyList(),

    // 长图编辑设置
    val enabledCards: Map<ShareCardType, Boolean> = ShareCardType.entries.associateWith { true },

    // 共用编辑设置
    val showDate: Boolean = true,
    val showNickname: Boolean = true,
    val customDeviceName: String? = null,
    val brandText: String = "",
    val heartRateZone7Selected: Boolean = true,

    // 编辑面板
    val showEditSheet: Boolean = false,

    // 图片生成
    val isGenerating: Boolean = false,
    val generatedBitmap: Bitmap? = null,
    val shareError: String? = null,

    // 保存到相册
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val savedImageUri: Uri? = null
) {
    companion object {
        val defaultTemplates = listOf(
            ShareTemplateSpec(
                id = "highlight",
                name = "高光模板",
                description = "突出距离、配速、跑力等核心成绩",
                tags = listOf("可选指标", "成绩高光", "即将设计")
            ),
            ShareTemplateSpec(
                id = "route",
                name = "路线模板",
                description = "突出地图轨迹和运动地点",
                tags = listOf("可换背景", "轨迹优先", "即将设计"),
                supportsBackgroundReplace = true
            ),
            ShareTemplateSpec(
                id = "data",
                name = "数据模板",
                description = "适合训练复盘和多指标摘要",
                tags = listOf("数据复盘", "可选模块", "即将设计")
            ),
            ShareTemplateSpec(
                id = "poster",
                name = "海报模板",
                description = "固定背景的社交传播氛围图",
                tags = listOf("固定背景", "海报风格", "即将设计")
            )
        )

        val defaultShortMetrics = listOf(
            ShareMetricType.DURATION,
            ShareMetricType.VDOT,
            ShareMetricType.PACE,
            ShareMetricType.TRAINING_LOAD,
            ShareMetricType.AVG_HEART_RATE,
            ShareMetricType.MAX_HEART_RATE,
            ShareMetricType.AVG_STRIDE_LENGTH,
            ShareMetricType.AVG_CADENCE,
            ShareMetricType.ELEVATION
        )
    }
}
