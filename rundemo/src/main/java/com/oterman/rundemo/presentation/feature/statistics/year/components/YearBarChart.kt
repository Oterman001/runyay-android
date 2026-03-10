package com.oterman.rundemo.presentation.feature.statistics.year.components

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
import com.oterman.rundemo.domain.model.MonthRangeData
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Year bar chart showing monthly distance
 * X-axis: 1~12 months
 * Y-axis: Distance in km (0, average, max)
 * Gray background bars for reference, blue gradient bars for actual distance
 */
@Composable
fun YearBarChart(
    monthRangeDataList: List<MonthRangeData>,
    maxMonthDistance: Double,
    avgMonthDistance: Double,
    modifier: Modifier = Modifier
) {
    val isDark = RunTheme.isDark
    val textMeasurer = rememberTextMeasurer()

    // Ensure max distance is at least 1 to avoid division by zero
    val effectiveMaxDistance = remember(maxMonthDistance) {
        maxMonthDistance.coerceAtLeast(1.0)
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

    StatisticsCard(modifier = modifier) {
        Column {
            // Title
            Text(
                text = "按月统计(km)",
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
                val yValues = listOf(0.0, avgMonthDistance)
                yValues.forEachIndexed { index, value ->
                    val y = barAreaBottom - (value / effectiveMaxDistance * barAreaHeight).toFloat()


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

                // Draw bars for each month (12 months)
                val totalMonths = 12
                val barSpacing = 6.dp.toPx()
                val barWidth = (barAreaWidth - barSpacing * (totalMonths - 1)) / totalMonths

                for (monthIndex in 0 until totalMonths) {
                    val x = leftPadding + monthIndex * (barWidth + barSpacing)

                    // Background bar (max reference)
                    drawRoundRect(
                        color = bgBarColor,
                        topLeft = Offset(x, barAreaTop),
                        size = Size(barWidth, barAreaHeight),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )

                    // Data bar
                    val monthData = monthRangeDataList.getOrNull(monthIndex)
                    if (monthData != null && monthData.totalDistance > 0) {
                        val barHeight = (monthData.totalDistance / effectiveMaxDistance * barAreaHeight).toFloat()
                        drawRoundRect(
                            brush = barGradient,
                            topLeft = Offset(x, barAreaBottom - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                    }
                }

                // Draw X-axis labels (1-12)
                for (monthIndex in 0 until totalMonths) {
                    val x = leftPadding + monthIndex * (barWidth + barSpacing)
                    val monthNum = monthIndex + 1

                    val textResult = textMeasurer.measure(
                        text = monthNum.toString(),
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

@Preview(showBackground = true)
@Composable
private fun YearBarChartPreview() {
    val mockMonths = (1..12).map { month ->
        MonthRangeData(
            year = 2024,
            month = month,
            totalDistance = if (month <= 6) (month * 15 + 20).toDouble() else 0.0,
            runCount = month * 2
        )
    }

    YearBarChart(
        monthRangeDataList = mockMonths,
        maxMonthDistance = 110.0,
        avgMonthDistance = 55.0
    )
}
