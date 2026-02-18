package com.oterman.rundemo.presentation.feature.statistics.total.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.TotalChartDisplayMode
import com.oterman.rundemo.domain.model.YearlyStatistic
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Bar chart showing yearly statistics (distance or duration)
 * X-axis: Years
 * Y-axis: Distance (km) or Duration (hours)
 * With toggle button to switch between distance and duration
 */
@Composable
fun TotalYearBarChart(
    yearlyStatistics: List<YearlyStatistic>,
    chartDisplayMode: TotalChartDisplayMode,
    maxYearDistance: Double,
    maxYearDuration: Double,
    onToggleDisplayMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val textMeasurer = rememberTextMeasurer()

    // Calculate max value based on display mode
    val effectiveMaxValue = remember(chartDisplayMode, maxYearDistance, maxYearDuration) {
        when (chartDisplayMode) {
            TotalChartDisplayMode.DISTANCE -> maxYearDistance.coerceAtLeast(1.0)
            TotalChartDisplayMode.DURATION -> (maxYearDuration / 60.0).coerceAtLeast(1.0) // Convert to hours
        }
    }

    // Calculate average value
    val avgValue = remember(yearlyStatistics, chartDisplayMode) {
        if (yearlyStatistics.isEmpty()) 0.0
        else {
            val sum = yearlyStatistics.sumOf {
                when (chartDisplayMode) {
                    TotalChartDisplayMode.DISTANCE -> it.totalDistance
                    TotalChartDisplayMode.DURATION -> it.totalDurationMinutes / 60.0
                }
            }
            sum / yearlyStatistics.size
        }
    }

    // Colors
    val barGradient = Brush.verticalGradient(
        colors = listOf(
            RunTheme.colorScheme.blue,
            RunTheme.colorScheme.blue.copy(alpha = 0.6f)
        )
    )
    val bgBarColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    val textColor = SecondaryTextColor
    val gridLineColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    val toggleBgColor = RunTheme.colorScheme.blue.copy(alpha = 0.1f)

    StatisticsCard(modifier = modifier) {
        Column {
            // Title row with toggle button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "按年统计(${chartDisplayMode.unit})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Toggle button
                Text(
                    text = if (chartDisplayMode == TotalChartDisplayMode.DISTANCE) "切换到时长" else "切换到距离",
                    fontSize = 12.sp,
                    color = RunTheme.colorScheme.blue,
                    modifier = Modifier
                        .background(toggleBgColor, RoundedCornerShape(8.dp))
                        .clickable { onToggleDisplayMode() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            if (yearlyStatistics.isEmpty()) {
                // Empty state
                Text(
                    text = "暂无数据",
                    fontSize = 14.sp,
                    color = SecondaryTextColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 80.dp)
                        .align(Alignment.CenterHorizontally)
                )
            } else {
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
                    val yValues = listOf(0.0, avgValue, effectiveMaxValue)
                    yValues.forEachIndexed { _, value ->
                        val y = barAreaBottom - (value / effectiveMaxValue * barAreaHeight).toFloat()

                        // Grid line
                        drawLine(
                            color = gridLineColor,
                            start = Offset(leftPadding, y),
                            end = Offset(canvasWidth - rightPadding, y),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Y-axis label
                        val labelText = if (value > 0) String.format("%.0f", value) else "0"
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

                    // Draw bars for each year
                    val totalYears = yearlyStatistics.size
                    val barSpacing = 8.dp.toPx()
                    val maxBarWidth = 40.dp.toPx()
                    val calculatedBarWidth = (barAreaWidth - barSpacing * (totalYears - 1)) / totalYears
                    val barWidth = minOf(calculatedBarWidth, maxBarWidth)
                    val totalBarsWidth = barWidth * totalYears + barSpacing * (totalYears - 1)
                    val startX = leftPadding + (barAreaWidth - totalBarsWidth) / 2

                    yearlyStatistics.forEachIndexed { index, yearStat ->
                        val x = startX + index * (barWidth + barSpacing)

                        // Background bar (max reference)
                        drawRoundRect(
                            color = bgBarColor,
                            topLeft = Offset(x, barAreaTop),
                            size = Size(barWidth, barAreaHeight),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )

                        // Data bar
                        val value = when (chartDisplayMode) {
                            TotalChartDisplayMode.DISTANCE -> yearStat.totalDistance
                            TotalChartDisplayMode.DURATION -> yearStat.totalDurationMinutes / 60.0
                        }
                        if (value > 0) {
                            val barHeight = (value / effectiveMaxValue * barAreaHeight).toFloat()
                            drawRoundRect(
                                brush = barGradient,
                                topLeft = Offset(x, barAreaBottom - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(3.dp.toPx())
                            )
                        }

                        // X-axis label (year)
                        val yearLabel = yearStat.year.toString()
                        val textResult = textMeasurer.measure(
                            text = yearLabel,
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = textColor
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
private fun TotalYearBarChartPreview() {
    val mockYears = listOf(
        YearlyStatistic(year = 2021, totalDistance = 520.0, totalDurationMinutes = 2800.0, runCount = 52),
        YearlyStatistic(year = 2022, totalDistance = 680.0, totalDurationMinutes = 3600.0, runCount = 68),
        YearlyStatistic(year = 2023, totalDistance = 890.0, totalDurationMinutes = 4800.0, runCount = 89),
        YearlyStatistic(year = 2024, totalDistance = 450.0, totalDurationMinutes = 2400.0, runCount = 45)
    )

    TotalYearBarChart(
        yearlyStatistics = mockYears,
        chartDisplayMode = TotalChartDisplayMode.DISTANCE,
        maxYearDistance = 890.0,
        maxYearDuration = 4800.0,
        onToggleDisplayMode = {}
    )
}
