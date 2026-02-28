package com.oterman.rundemo.presentation.feature.statistics.total.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.AllTimeTotalStatistics
import com.oterman.rundemo.domain.model.YearlyStatistic
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Year detail table showing yearly breakdown
 * Headers: 年份 | 距离(km) | 时长(h) | 配速 | 爬升(m)
 * Data rows for each year (sorted by year descending)
 * Summary row with totals in blue
 */
@Composable
fun TotalYearDetailTable(
    yearlyStatistics: List<YearlyStatistic>,
    totalStats: AllTimeTotalStatistics,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val alternateRowColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF5F5F7)
    val headerBgColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)

    // Sort years by descending order for display
    val displayYears = remember(yearlyStatistics) {
        yearlyStatistics.sortedByDescending { it.year }
    }

    AppCard(modifier = modifier) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            // Title
            Text(
                text = "年度数据明细",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            )

            if (displayYears.isEmpty()) {
                // Empty state
                Text(
                    text = "暂无数据",
                    fontSize = 14.sp,
                    color = SecondaryTextColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                // Table header
                TableRow(
                    col1 = "年份",
                    col2 = "距离(km)",
                    col3 = "时长(h)",
                    col4 = "配速",
                    col5 = "爬升(m)",
                    isHeader = true,
                    backgroundColor = headerBgColor
                )

                HorizontalDivider(thickness = 0.5.dp, color = headerBgColor)

                // Data rows (years in descending order)
                displayYears.forEachIndexed { index, yearStat ->
                    val bgColor = if (index % 2 == 1) alternateRowColor else Color.Transparent

                    TableRow(
                        col1 = "${yearStat.year}",
                        col2 = yearStat.getFormattedDistance(),
                        col3 = yearStat.getFormattedDuration(),
                        col4 = yearStat.avgPace,
                        col5 = yearStat.getFormattedElevation(),
                        isHeader = false,
                        backgroundColor = bgColor
                    )
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = RunTheme.colorScheme.blue.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Summary row (totals)
                TableRow(
                    col1 = "合计",
                    col2 = formatTotalDistance(totalStats.totalDistance),
                    col3 = formatTotalDuration(totalStats.totalDurationMinutes),
                    col4 = totalStats.avgPace,
                    col5 = String.format("%.0f", totalStats.totalElevation),
                    isHeader = false,
                    backgroundColor = Color.Transparent,
                    isSummary = true
                )
            }
        }
    }
}

/**
 * Single row in the table
 */
@Composable
private fun TableRow(
    col1: String,
    col2: String,
    col3: String,
    col4: String,
    col5: String,
    isHeader: Boolean,
    backgroundColor: Color,
    isSummary: Boolean = false
) {
    val textColor = when {
        isSummary -> RunTheme.colorScheme.blue
        isHeader -> SecondaryTextColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    val fontWeight = when {
        isHeader || isSummary -> FontWeight.Medium
        else -> FontWeight.Normal
    }

    val fontSize = if (isHeader) 12.sp else 13.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = col1,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.8f)
        )
        Text(
            text = col2,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = col3,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = col4,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = col5,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Format total distance
 */
private fun formatTotalDistance(distance: Double): String {
    return if (distance >= 1000) {
        distance.toInt().toString()
    } else {
        String.format("%.1f", distance)
    }
}

/**
 * Format total duration in minutes to hours string
 */
private fun formatTotalDuration(minutes: Double): String {
    val hours = minutes / 60.0
    return String.format("%.1f", hours)
}

@Preview(showBackground = true)
@Composable
private fun TotalYearDetailTablePreview() {
    val mockYears = listOf(
        YearlyStatistic(year = 2024, totalDistance = 450.0, totalDurationMinutes = 2400.0, runCount = 45, avgPace = "5'20\"", totalElevation = 3200.0),
        YearlyStatistic(year = 2023, totalDistance = 890.0, totalDurationMinutes = 4800.0, runCount = 89, avgPace = "5'23\"", totalElevation = 6500.0),
        YearlyStatistic(year = 2022, totalDistance = 680.0, totalDurationMinutes = 3600.0, runCount = 68, avgPace = "5'18\"", totalElevation = 4800.0),
        YearlyStatistic(year = 2021, totalDistance = 520.0, totalDurationMinutes = 2800.0, runCount = 52, avgPace = "5'23\"", totalElevation = 3600.0)
    )
    val totalStats = AllTimeTotalStatistics(
        totalDistance = 2540.0,
        totalDurationMinutes = 13600.0,
        avgPace = "5'21\"",
        totalElevation = 18100.0,
        runCount = 254,
        yearlyStatistics = mockYears
    )

    TotalYearDetailTable(
        yearlyStatistics = mockYears,
        totalStats = totalStats
    )
}
