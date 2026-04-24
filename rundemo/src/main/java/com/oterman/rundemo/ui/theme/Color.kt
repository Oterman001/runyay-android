package com.oterman.rundemo.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─── 品牌色 Brand Colors ──────────────────────────────────────────────────────
val RunBlue = Color(0xFF1AA9F8)
val RunBlueGradient1 = Color(0xFF2BCCFC)
val RunBlueGradient2 = Color(0xFF53E4FC)
val RunBlueLiner = Color(0xFF38CCFF)
val RunOrange = Color(0xFFFF9500)
val RunGold = Color(0xFFFFD040)

// ─── 表面/背景色 Surface ───────────────────────────────────────────────────────
val RunLiteBlue1Light = Color(0xFFEFF6FF)
val RunLiteBlue1Dark = Color(0xFF1B1C1E)
val CardBgLight = Color(0xFFFFFFFF)
val CardBgDark = Color(0xFF1C1C1E)
val NoDataBg = Color(0xFFF2F2F7)
val NoDataBgDark = Color(0xFF2C2C2E)
val DividerLight = Color(0xFFE5E5EA)
val DividerDark = Color(0xFF3A3A3C)

// ─── 文字色 Text ──────────────────────────────────────────────────────────────
val SecondaryTextColor = Color(0xFF8E8E93)

// ─── 状态色 Status Colors ─────────────────────────────────────────────────────
val StatusSuccess = Color(0xFF34C759)
val StatusSuccessContainer = Color(0xFFE8F8ED)
val StatusSuccessContainerDark = Color(0xFF1A3A24)
val StatusWarning = Color(0xFFFF9500)
val StatusWarningContainer = Color(0xFFFFF3E0)
val StatusDestructive = Color(0xFFFF3B30)
val StatusDestructiveContainer = Color(0xFFFFEBEE)
val StatusDestructiveContainerDark = Color(0xFF3A1A1A)
val StatusInfo = RunBlue
val StatusNeutral = Color(0xFF8E8E93)

// ─── 日历格 DayCell ───────────────────────────────────────────────────────────
val DayCellBadgeBg = Color(0xFFE0E0E0)
val DayCellBadgeText = Color(0xFFFF3B30)

// ─── 图表折线色 Chart Line Colors ─────────────────────────────────────────────
val ChartPaceLine = Color(0xFF1E88E5)
val ChartHeartRateLine = Color(0xFFE53935)
val ChartAltitudeLine = Color(0xFF8D6E63)
val ChartStrideLine = Color(0xFF7E57C2)
val ChartCadenceLine = Color(0xFF26A69A)
val ChartContactTimeLine = Color(0xFFFF7043)
val ChartVerticalOscLine = Color(0xFF5C6BC0)
val ChartPowerLine = Color(0xFFEF5350)

// ─── 区间色 Zone Colors (5级: 蓝→绿→黄→橙→红) ──────────────────────────────
val Zone1Color = Color(0xFF90CAF9)   // 蓝 - 轻松/恢复
val Zone2Color = Color(0xFF4CAF50)   // 绿 - 有氧
val Zone3Color = Color(0xFFFFC107)   // 黄 - 马拉松/有氧发展
val Zone4Color = Color(0xFFFF9800)   // 橙 - 无氧阈
val Zone5Color = Color(0xFFF44336)   // 红 - 最大摄氧量

// ─── 7区间扩展色 ──────────────────────────────────────────────────────────────
val Zone6Color = Color(0xFFFF5722)   // 深橙
val Zone7Color = Color(0xFFD32F2F)   // 深红

// ─── 训练间歇类型色 Training Segment Colors ───────────────────────────────────
val SegmentWarmup = Color(0xFFE65100)
val SegmentWork = Color(0xFFEEB23C)
val SegmentRecovery = Color(0xFF2E7D32)
val SegmentCooldown = Color(0xFF6A1B9A)
val SegmentUnknown = Color(0xFF616161)

// ─── 跑鞋磨损色 Wear Colors ───────────────────────────────────────────────────
val WearLow = Color(0xFF43A047)
val WearMedium = Color(0xFFFDD835)
val WearHigh = Color(0xFFFB8C00)
val WearCritical = Color(0xFFE53935)

// ─── VO2Max / 性能等级色 Performance Grade Colors ─────────────────────────────
val GradeExcellent = Color(0xFF4CAF50)
val GradeGood = Color(0xFF2196F3)
val GradeAverage = Color(0xFFFF9800)
val GradePoor = Color(0xFFF44336)

// ─── 赛事类型标签色 Race Distance Colors ─────────────────────────────────────
val RaceColorMarathon = Color(0xFFFF3B30)
val RaceColorHalfMarathon = RunBlue
val RaceColorTenK = Color(0xFF34C759)
val RaceColorFiveK = Color(0xFFFF9500)
val RaceColorOther = Color(0xFF8E8E93)

// ─── 表格辅助色 Table Colors ──────────────────────────────────────────────────
val TableAlternateRowLight = Color(0xFFF5F5F7)
val TableAlternateRowDark = Color(0xFF2C2C2E)
val TableHeaderLight = Color(0xFFE5E5EA)
val TableHeaderDark = Color(0xFF3A3A3C)

// ─── 欢迎页背景 Welcome Screen ────────────────────────────────────────────────
val WelcomeDeepNavy = Color(0xFF0A1628)
val WelcomeMidNavy = Color(0xFF0F2440)

// ─────────────────────────────────────────────────────────────────────────────
// RunColorScheme — 语义化主题槽位，通过 RunTheme.colorScheme 访问
// ─────────────────────────────────────────────────────────────────────────────
@Immutable
data class RunColorScheme(
    // 品牌色
    val blue: Color,
    val blueGradient1: Color,
    val blueGradient2: Color,
    val blueLiner: Color,
    val liteBlue1: Color,
    val orange: Color,
    // 表面/背景
    val secondaryText: Color,
    val cardBg: Color,
    val noDataBg: Color,
    val divider: Color,
    // 日历格
    val dayCellActive: Color,
    val dayCellBadgeBg: Color,
    val dayCellBadgeText: Color,
    // 状态色
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val destructive: Color,
    val destructiveContainer: Color,
    val info: Color,
    val neutral: Color,
    // 图表折线色
    val chartPaceLine: Color,
    val chartHeartRateLine: Color,
    val chartAltitudeLine: Color,
    val chartStrideLine: Color,
    val chartCadenceLine: Color,
    val chartContactTimeLine: Color,
    val chartVerticalOscLine: Color,
    val chartPowerLine: Color,
    // 区间色 (index 0-4 = zone1-5)
    val zoneColors: List<Color>,
    // 7区间完整列表 (index 0-6 = zone1-7)
    val zone7Colors: List<Color>,
    // 训练间歇类型色
    val segmentWarmup: Color,
    val segmentWork: Color,
    val segmentRecovery: Color,
    val segmentCooldown: Color,
    val segmentUnknown: Color,
    // 跑鞋磨损色
    val wearLow: Color,
    val wearMedium: Color,
    val wearHigh: Color,
    val wearCritical: Color,
    // VO2Max / 性能等级
    val gradeExcellent: Color,
    val gradeGood: Color,
    val gradeAverage: Color,
    val gradePoor: Color,
    // 赛事类型标签色
    val raceColorMarathon: Color,
    val raceColorHalfMarathon: Color,
    val raceColorTenK: Color,
    val raceColorFiveK: Color,
    val raceColorOther: Color,
    // 表格辅助色
    val tableAlternateRow: Color,
    val tableHeader: Color,
    // 欢迎页背景
    val welcomeGradientStart: Color,
    val welcomeGradientMid: Color,
)

val LightRunColorScheme = RunColorScheme(
    blue = RunBlue,
    blueGradient1 = RunBlueGradient1,
    blueGradient2 = RunBlueGradient2,
    blueLiner = RunBlueLiner,
    liteBlue1 = RunLiteBlue1Light,
    orange = RunOrange,
    secondaryText = SecondaryTextColor,
    cardBg = CardBgLight,
    noDataBg = NoDataBg,
    divider = DividerLight,
    dayCellActive = RunBlue,
    dayCellBadgeBg = DayCellBadgeBg,
    dayCellBadgeText = DayCellBadgeText,
    success = StatusSuccess,
    successContainer = StatusSuccessContainer,
    warning = StatusWarning,
    warningContainer = StatusWarningContainer,
    destructive = StatusDestructive,
    destructiveContainer = StatusDestructiveContainer,
    info = StatusInfo,
    neutral = StatusNeutral,
    chartPaceLine = ChartPaceLine,
    chartHeartRateLine = ChartHeartRateLine,
    chartAltitudeLine = ChartAltitudeLine,
    chartStrideLine = ChartStrideLine,
    chartCadenceLine = ChartCadenceLine,
    chartContactTimeLine = ChartContactTimeLine,
    chartVerticalOscLine = ChartVerticalOscLine,
    chartPowerLine = ChartPowerLine,
    zoneColors = listOf(Zone1Color, Zone2Color, Zone3Color, Zone4Color, Zone5Color),
    zone7Colors = listOf(Zone1Color, Color(0xFF64B5F6), Zone2Color, Zone3Color, Zone4Color, Zone6Color, Zone5Color),
    segmentWarmup = SegmentWarmup,
    segmentWork = SegmentWork,
    segmentRecovery = SegmentRecovery,
    segmentCooldown = SegmentCooldown,
    segmentUnknown = SegmentUnknown,
    wearLow = WearLow,
    wearMedium = WearMedium,
    wearHigh = WearHigh,
    wearCritical = WearCritical,
    gradeExcellent = GradeExcellent,
    gradeGood = GradeGood,
    gradeAverage = GradeAverage,
    gradePoor = GradePoor,
    raceColorMarathon = RaceColorMarathon,
    raceColorHalfMarathon = RaceColorHalfMarathon,
    raceColorTenK = RaceColorTenK,
    raceColorFiveK = RaceColorFiveK,
    raceColorOther = RaceColorOther,
    tableAlternateRow = TableAlternateRowLight,
    tableHeader = TableHeaderLight,
    welcomeGradientStart = WelcomeDeepNavy,
    welcomeGradientMid = WelcomeMidNavy,
)

val DarkRunColorScheme = RunColorScheme(
    blue = RunBlue,
    blueGradient1 = RunBlueGradient1,
    blueGradient2 = RunBlueGradient2,
    blueLiner = RunBlueLiner,
    liteBlue1 = RunLiteBlue1Dark,
    orange = RunOrange,
    secondaryText = SecondaryTextColor,
    cardBg = CardBgDark,
    noDataBg = NoDataBgDark,
    divider = DividerDark,
    dayCellActive = RunBlue,
    dayCellBadgeBg = DayCellBadgeBg,
    dayCellBadgeText = DayCellBadgeText,
    success = StatusSuccess,
    successContainer = StatusSuccessContainerDark,
    warning = StatusWarning,
    warningContainer = Color(0xFF3A2800),
    destructive = StatusDestructive,
    destructiveContainer = StatusDestructiveContainerDark,
    info = StatusInfo,
    neutral = StatusNeutral,
    chartPaceLine = ChartPaceLine,
    chartHeartRateLine = ChartHeartRateLine,
    chartAltitudeLine = ChartAltitudeLine,
    chartStrideLine = ChartStrideLine,
    chartCadenceLine = ChartCadenceLine,
    chartContactTimeLine = ChartContactTimeLine,
    chartVerticalOscLine = ChartVerticalOscLine,
    chartPowerLine = ChartPowerLine,
    zoneColors = listOf(Zone1Color, Zone2Color, Zone3Color, Zone4Color, Zone5Color),
    zone7Colors = listOf(Zone1Color, Color(0xFF64B5F6), Zone2Color, Zone3Color, Zone4Color, Zone6Color, Zone5Color),
    segmentWarmup = SegmentWarmup,
    segmentWork = SegmentWork,
    segmentRecovery = SegmentRecovery,
    segmentCooldown = SegmentCooldown,
    segmentUnknown = SegmentUnknown,
    wearLow = WearLow,
    wearMedium = WearMedium,
    wearHigh = WearHigh,
    wearCritical = WearCritical,
    gradeExcellent = GradeExcellent,
    gradeGood = GradeGood,
    gradeAverage = GradeAverage,
    gradePoor = GradePoor,
    raceColorMarathon = RaceColorMarathon,
    raceColorHalfMarathon = RaceColorHalfMarathon,
    raceColorTenK = RaceColorTenK,
    raceColorFiveK = RaceColorFiveK,
    raceColorOther = RaceColorOther,
    tableAlternateRow = TableAlternateRowDark,
    tableHeader = TableHeaderDark,
    welcomeGradientStart = WelcomeDeepNavy,
    welcomeGradientMid = WelcomeMidNavy,
)

val LocalRunColorScheme = staticCompositionLocalOf { LightRunColorScheme }

// ─────────────────────────────────────────────────────────────────────────────
// 语义辅助函数 Semantic Helper Functions
// ─────────────────────────────────────────────────────────────────────────────

/** 根据跑鞋磨损百分比返回对应颜色 */
fun RunColorScheme.wearColorFor(percentage: Double): androidx.compose.ui.graphics.Color = when {
    percentage < 60 -> wearLow
    percentage < 80 -> wearMedium
    percentage < 90 -> wearHigh
    else -> wearCritical
}

/** 根据赛前天数返回倒计时颜色 */
fun RunColorScheme.countdownColorFor(daysRemaining: Int): androidx.compose.ui.graphics.Color = when {
    daysRemaining < 0 -> neutral
    daysRemaining <= 7 -> destructive
    daysRemaining <= 30 -> warning
    else -> success
}

/** 根据赛事距离类型返回标签颜色 */
fun RunColorScheme.raceColorFor(type: com.oterman.rundemo.domain.model.RaceDistanceType): androidx.compose.ui.graphics.Color = when (type) {
    com.oterman.rundemo.domain.model.RaceDistanceType.MARATHON -> raceColorMarathon
    com.oterman.rundemo.domain.model.RaceDistanceType.HALF_MARATHON -> raceColorHalfMarathon
    com.oterman.rundemo.domain.model.RaceDistanceType.TEN_K -> raceColorTenK
    com.oterman.rundemo.domain.model.RaceDistanceType.FIVE_K -> raceColorFiveK
    com.oterman.rundemo.domain.model.RaceDistanceType.OTHER -> raceColorOther
}
