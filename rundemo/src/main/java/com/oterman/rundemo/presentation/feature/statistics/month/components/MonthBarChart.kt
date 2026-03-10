package com.oterman.rundemo.presentation.feature.statistics.month.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Monthly bar chart showing daily distance
 * X-axis: Day of month (1, 5, 10, 15, 20, 25, 30)
 * Y-axis: Distance in km
 * Gray background bars for all days, blue gradient bars for past days with data
 */
@Composable
fun MonthBarChart(
    dailyRecords: List<DayRunData>,
    modifier: Modifier = Modifier
) {
    val isDark = RunTheme.isDark
    val textMeasurer = rememberTextMeasurer()

    // Filter out placeholder cells and get actual day data
    val actualDays = remember(dailyRecords) {
        dailyRecords.filter { !it.isPlaceholder }
    }

    // Calculate max distance for Y-axis scaling
    val maxDistance = remember(actualDays) {
        actualDays.maxOfOrNull { it.totalDistance }?.coerceAtLeast(1.0) ?: 1.0
    }

    // Calculate average distance (only past days with runs)
    val avgDistance = remember(actualDays) {
        val pastDaysWithRuns = actualDays.filter { !it.isFuture && it.totalDistance > 0 }
        if (pastDaysWithRuns.isNotEmpty()) {
            pastDaysWithRuns.sumOf { it.totalDistance } / pastDaysWithRuns.size
        } else 0.0
    }

    // Colors
    val barGradient = Brush.verticalGradient(
        colors = listOf(
            RunTheme.colorScheme.blue,
            RunTheme.colorScheme.blue.copy(alpha = 0.6f)
        )
    )
    val todayLabelColor = RunTheme.colorScheme.blue
    val bgBarColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    val textColor = SecondaryTextColor
    val gridLineColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)

    // X-axis labels to show
    val xLabels = listOf(1, 5, 10, 15, 20, 25, 30)

    StatisticsCard(modifier = modifier) {
        Column {
            // Title
            Text(
                text = "按日统计(km)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barAreaTop = 20.dp.toPx()
                val barAreaBottom = canvasHeight - 30.dp.toPx()
                val barAreaHeight = barAreaBottom - barAreaTop
                val leftPadding = 40.dp.toPx()
                val rightPadding = 16.dp.toPx()
                val barAreaWidth = canvasWidth - leftPadding - rightPadding

                // Draw Y-axis labels and grid lines
                val yValues = listOf(0.0, avgDistance)
                yValues.forEachIndexed { index, value ->
                    val y = barAreaBottom - (value / maxDistance * barAreaHeight).toFloat()

                    // Grid line
                    drawLine(
                        color = gridLineColor,
                        start = Offset(leftPadding, y),
                        end = Offset(canvasWidth - rightPadding, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(7f, 5f), // 10px实线，10px间隔
                            phase = 0f
                        )
                    )

                    // Y-axis label
                    val labelText = if (value > 0) String.format("%.1f", value) else "0"
                    val textResult = textMeasurer.measure(
                        text = labelText,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = textColor
                        )
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset(
                            x = leftPadding - textResult.size.width - 4.dp.toPx(),
                            y = y - textResult.size.height / 2
                        )
                    )
                }

                // Draw bars for each day
                val totalDays = actualDays.size
                if (totalDays == 0) return@Canvas

                val barSpacing = 2.dp.toPx()
                val barWidth = (barAreaWidth - barSpacing * (totalDays - 1)) / totalDays

                actualDays.forEachIndexed { index, dayData ->
                    val x = leftPadding + index * (barWidth + barSpacing)

                    // Background bar (max reference)
                    drawRoundRect(
                        color = bgBarColor,
                        topLeft = Offset(x, barAreaTop),
                        size = Size(barWidth, barAreaHeight),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )

                    // Data bar (only for past days with data)
                    if (!dayData.isFuture && dayData.totalDistance > 0) {
                        val barHeight = (dayData.totalDistance / maxDistance * barAreaHeight).toFloat()
                        drawRoundRect(
                            brush = barGradient,
                            topLeft = Offset(x, barAreaBottom - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }

                // Draw X-axis labels
                xLabels.forEach { dayNum ->
                    if (dayNum <= totalDays) {
                        val dayIndex = dayNum - 1
                        val x = leftPadding + dayIndex * (barWidth + barSpacing)

                        val dayData = actualDays.getOrNull(dayIndex)
                        val isToday = dayData?.isToday == true

                        val textResult = textMeasurer.measure(
                            text = dayNum.toString(),
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = if (isToday) todayLabelColor else textColor,
                                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal
                            )
                        )
                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset(
                                x = x + barWidth / 2 - textResult.size.width / 2,
                                y = barAreaBottom + 8.dp.toPx()
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthBarChartPreview() {
    val mockDays = (1..30).map { day ->
        DayRunData(
            dayOfMonth = day,
            totalDistance = if (day % 3 == 0) (day % 10 + 2).toDouble() else 0.0,
            isToday = day == 20,
            isFuture = day > 20
        )
    }
    MonthBarChart(dailyRecords = mockDays)
}
