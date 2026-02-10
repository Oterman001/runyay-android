package com.oterman.rundemo.presentation.feature.statistics.year.components

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
import com.oterman.rundemo.domain.model.MonthRangeData
import com.oterman.rundemo.domain.model.YearStatistics
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Year detail table showing monthly breakdown
 * Headers: 月份 | 总距离(km) | 总时长(h) | 平均配速
 * Data rows for 12 months
 * Summary row with averages in blue
 */
@Composable
fun YearDetailTable(
    monthRangeDataList: List<MonthRangeData>,
    yearStats: YearStatistics,
    isCurYear: Boolean,
    curMonth: Int,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val alternateRowColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF5F5F7)
    val headerBgColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)

    // Filter months with data for display (only show months with runs)
    val displayMonths = remember(monthRangeDataList) {
        monthRangeDataList.filter { it.runCount > 0 }
    }

    // Calculate averages
    val avgDistance = remember(yearStats, isCurYear, curMonth) {
        val monthCount = if (isCurYear) curMonth else 12
        if (monthCount > 0 && yearStats.runCount > 0) {
            yearStats.totalDistance / displayMonths.size.coerceAtLeast(1)
        } else 0.0
    }

    val avgDuration = remember(yearStats, displayMonths) {
        if (displayMonths.isNotEmpty()) {
            yearStats.totalDurationMinutes / displayMonths.size
        } else 0.0
    }

    StatisticsCard(modifier = modifier) {
        Column {
            // Title
            Text(
                text = "月份详情",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Table header
            TableRow(
                col1 = "月份",
                col2 = "总距离(km)",
                col3 = "总时长(h)",
                col4 = "平均配速",
                isHeader = true,
                backgroundColor = headerBgColor
            )

            HorizontalDivider(thickness = 0.5.dp, color = headerBgColor)

            // Data rows (only months with runs)
            displayMonths.forEachIndexed { index, monthData ->
                val bgColor = if (index % 2 == 1) alternateRowColor else Color.Transparent

                TableRow(
                    col1 = "${monthData.month}月",
                    col2 = monthData.getFormattedDistance(),
                    col3 = monthData.getFormattedDuration(),
                    col4 = monthData.avgPace,
                    isHeader = false,
                    backgroundColor = bgColor
                )
            }

            if (displayMonths.isNotEmpty()) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = RunBlue.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Summary row (averages)
                TableRow(
                    col1 = "平均",
                    col2 = String.format("%.1f", avgDistance),
                    col3 = formatAverageDuration(avgDuration),
                    col4 = yearStats.avgPace,
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
    isHeader: Boolean,
    backgroundColor: Color,
    isSummary: Boolean = false
) {
    val textColor = when {
        isSummary -> RunBlue
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
            .padding(vertical = 10.dp, horizontal = 4.dp),
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
            modifier = Modifier.weight(1.2f)
        )
    }
}

/**
 * Format average duration in minutes to "X.Xh" format
 */
private fun formatAverageDuration(minutes: Double): String {
    if (minutes <= 0) return "-"
    val hours = minutes / 60.0
    return String.format("%.1f", hours)
}

@Preview(showBackground = true)
@Composable
private fun YearDetailTablePreview() {
    val mockMonths = listOf(
        MonthRangeData(month = 1, totalDistance = 85.2, totalDurationMinutes = 480.0, runCount = 8, avgPace = "5'38\""),
        MonthRangeData(month = 2, totalDistance = 92.5, totalDurationMinutes = 520.0, runCount = 10, avgPace = "5'37\""),
        MonthRangeData(month = 3, totalDistance = 105.0, totalDurationMinutes = 600.0, runCount = 12, avgPace = "5'43\""),
        MonthRangeData(month = 4, totalDistance = 78.3, totalDurationMinutes = 420.0, runCount = 7, avgPace = "5'22\""),
    )
    val yearStats = YearStatistics(
        totalDistance = 361.0,
        totalDurationMinutes = 2020.0,
        runCount = 37,
        avgPace = "5'35\""
    )

    YearDetailTable(
        monthRangeDataList = mockMonths,
        yearStats = yearStats,
        isCurYear = true,
        curMonth = 4
    )
}
