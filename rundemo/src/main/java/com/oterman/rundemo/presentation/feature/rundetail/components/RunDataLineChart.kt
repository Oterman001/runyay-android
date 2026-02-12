package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.ChartDataPoint
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants

/**
 * 通用跑步数据折线图组件
 * 基于 Compose Canvas 实现，展示时序数据折线图
 *
 * 特性：
 * - 折线图 + 填充渐变
 * - 平均线（虚线）
 * - X/Y 轴标签
 * - 支持配速模式（Y 轴反转）
 */
@Composable
fun RunDataLineChart(
    title: String,
    dataPoints: List<ChartDataPoint>,
    lineColor: Color,
    unit: String = "",
    avgValue: Double? = null,
    maxValue: Double? = null,
    minValue: Double? = null,
    invertYAxis: Boolean = false,
    chartHeight: Int = 180,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return

    val calculatedMin = minValue ?: dataPoints.minOf { it.value }
    val calculatedMax = maxValue ?: dataPoints.maxOf { it.value }
    val calculatedAvg = avgValue ?: dataPoints.map { it.value }.average()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp),
        shape = RoundedCornerShape(RunDetailLayoutConstants.HeaderCardRadius.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RunDetailLayoutConstants.HeaderCardPadding.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                // 平均值标签
                if (calculatedAvg > 0) {
                    Text(
                        text = "平均 ${formatChartValue(calculatedAvg, invertYAxis)} $unit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 极值信息
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (calculatedMax > 0) {
                    Text(
                        text = "最大 ${formatChartValue(calculatedMax, invertYAxis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (calculatedMin > 0) {
                    Text(
                        text = "最小 ${formatChartValue(calculatedMin, invertYAxis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 折线图区域
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight.dp)
            ) {
                drawLineChart(
                    dataPoints = dataPoints,
                    lineColor = lineColor,
                    avgValue = calculatedAvg,
                    minValue = calculatedMin,
                    maxValue = calculatedMax,
                    invertYAxis = invertYAxis,
                    gridColor = surfaceVariant,
                    avgLineColor = onSurfaceVariant
                )
            }

            // X 轴时间标签
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                val totalSeconds = if (dataPoints.isNotEmpty()) {
                    dataPoints.last().timeOffset - dataPoints.first().timeOffset
                } else 0

                Text(
                    text = "0:00",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatTimeLabel(totalSeconds / 2),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatTimeLabel(totalSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * 绘制折线图
 */
private fun DrawScope.drawLineChart(
    dataPoints: List<ChartDataPoint>,
    lineColor: Color,
    avgValue: Double,
    minValue: Double,
    maxValue: Double,
    invertYAxis: Boolean,
    gridColor: Color,
    avgLineColor: Color
) {
    if (dataPoints.size < 2) return

    val chartWidth = size.width
    val chartHeight = size.height
    val padding = 0f

    val effectiveWidth = chartWidth - padding * 2
    val effectiveHeight = chartHeight - padding * 2

    val range = maxValue - minValue
    val safeRange = if (range == 0.0) 1.0 else range

    val timeMin = dataPoints.first().timeOffset
    val timeMax = dataPoints.last().timeOffset
    val timeRange = (timeMax - timeMin).toFloat()
    val safeTimeRange = if (timeRange == 0f) 1f else timeRange

    // 绘制水平网格线（3条）
    for (i in 0..3) {
        val y = padding + effectiveHeight * (i / 3f)
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(padding + effectiveWidth, y),
            strokeWidth = 1f
        )
    }

    // 计算数据点坐标
    fun getX(timeOffset: Int): Float {
        return padding + ((timeOffset - timeMin) / safeTimeRange) * effectiveWidth
    }

    fun getY(value: Double): Float {
        val normalizedValue = ((value - minValue) / safeRange).toFloat().coerceIn(0f, 1f)
        return if (invertYAxis) {
            padding + normalizedValue * effectiveHeight
        } else {
            padding + (1f - normalizedValue) * effectiveHeight
        }
    }

    // 构建折线路径
    val linePath = Path()
    val fillPath = Path()

    val firstX = getX(dataPoints.first().timeOffset)
    val firstY = getY(dataPoints.first().value)
    linePath.moveTo(firstX, firstY)
    fillPath.moveTo(firstX, chartHeight)
    fillPath.lineTo(firstX, firstY)

    for (i in 1 until dataPoints.size) {
        val x = getX(dataPoints[i].timeOffset)
        val y = getY(dataPoints[i].value)
        linePath.lineTo(x, y)
        fillPath.lineTo(x, y)
    }

    // 关闭填充路径
    val lastX = getX(dataPoints.last().timeOffset)
    fillPath.lineTo(lastX, chartHeight)
    fillPath.close()

    // 绘制渐变填充
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                lineColor.copy(alpha = 0.3f),
                lineColor.copy(alpha = 0.05f)
            )
        )
    )

    // 绘制折线
    drawPath(
        path = linePath,
        color = lineColor,
        style = Stroke(
            width = 2f,
            cap = StrokeCap.Round
        )
    )

    // 绘制平均线（虚线）
    if (avgValue in minValue..maxValue) {
        val avgY = getY(avgValue)
        drawLine(
            color = avgLineColor.copy(alpha = 0.6f),
            start = Offset(padding, avgY),
            end = Offset(padding + effectiveWidth, avgY),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
        )
    }
}

/**
 * 格式化图表数值
 */
private fun formatChartValue(value: Double, isPace: Boolean): String {
    return if (isPace) {
        // 配速格式 min'sec"
        val minutes = value.toInt()
        val seconds = ((value - minutes) * 60).toInt()
        "${minutes}'${seconds.toString().padStart(2, '0')}\""
    } else {
        if (value == value.toLong().toDouble()) {
            value.toInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}

/**
 * 格式化时间标签
 */
private fun formatTimeLabel(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val mins = minutes % 60
        String.format("%d:%02d:%02d", hours, mins, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

