package com.oterman.rundemo.domain.model

/**
 * 图表数据点领域模型
 * 用于展示心率、配速、步频等时序数据图表
 */
data class ChartDataPoint(
    val timeOffset: Int,        // 距离开始时间的秒数（X轴）
    val value: Double,          // 数据值（Y轴）
    val isPauseData: Boolean = false  // 是否为暂停期间的数据（用于图表断开显示）
)

/**
 * 图表数据类型枚举
 */
enum class ChartDataType(val value: Int, val displayName: String, val unit: String) {
    HEART_RATE(1, "心率", "bpm"),
    POWER(2, "功率", "W"),
    VERTICAL_OSCILLATION(3, "垂直振幅", "cm"),
    CONTACT_TIME(4, "触地时间", "ms"),
    SPEED(5, "配速", "min/km"),
    STRIDE_LENGTH(6, "步幅", "cm"),
    CADENCE(7, "步频", "spm"),
    DISTANCE(8, "距离", "m");
    
    companion object {
        fun fromValue(value: Int): ChartDataType? {
            return entries.find { it.value == value }
        }
    }
}

/**
 * 移动平均平滑，用于消除传感器噪声毛刺。
 * 不改变 timeOffset，只平滑 value；avgValue/maxValue/minValue 保持原始统计。
 */
fun List<ChartDataPoint>.smoothed(windowSize: Int): List<ChartDataPoint> {
    if (size <= windowSize) return this
    val half = windowSize / 2
    return mapIndexed { i, point ->
        val from = maxOf(0, i - half)
        val to = minOf(size - 1, i + half)
        val avg = subList(from, to + 1).map { it.value }.average()
        point.copy(value = avg)
    }
}

/**
 * 图表数据系列
 */
data class ChartSeries(
    val type: ChartDataType,
    val points: List<ChartDataPoint>,
    val minValue: Double,
    val maxValue: Double,
    val avgValue: Double
)

