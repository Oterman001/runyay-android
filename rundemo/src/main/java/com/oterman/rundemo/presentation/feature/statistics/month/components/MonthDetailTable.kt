package com.oterman.rundemo.presentation.feature.statistics.month.components

import androidx.compose.foundation.background
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
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.domain.model.MonthStatistics
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Month detail table showing daily breakdown
 * Headers: 日期 | 距离(km) | 时长(h) | 平均配速
 * Data rows for past days with runs only
 * Summary row with averages in blue
 */
@Composable
fun MonthDetailTable(
    dailyRecords: List<DayRunData>,
    monthStats: MonthStatistics,
    modifier: Modifier = Modifier
) {
    val isDark = RunTheme.isDark
    val alternateRowColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF5F5F7)
    val headerBgColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)

    // Filter out placeholders, future days, and days without runs
    val displayRecords = remember(dailyRecords) {
        dailyRecords.filter { !it.isPlaceholder && !it.isFuture && it.hasRun }
    }

    AppCard(modifier = modifier) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            // Title
            Text(
                text = "每日详情",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            )

            // Table header
            TableRow(
                col1 = "日期",
                col2 = "距离(km)",
                col3 = "时长",
                col4 = "平均配速",
                isHeader = true,
                backgroundColor = headerBgColor
            )

            HorizontalDivider(thickness = 0.5.dp, color = headerBgColor)

            // Data rows
            displayRecords.forEachIndexed { index, dayData ->
                val bgColor = if (index % 2 == 1) alternateRowColor else Color.Transparent

                TableRow(
                    col1 = "${dayData.dayOfMonth}日",
                    col2 = dayData.getFormattedDistance(),
                    col3 = dayData.getFormattedDuration(),
                    col4 = dayData.avgPace,
                    isHeader = false,
                    backgroundColor = bgColor,
                    isToday = dayData.isToday
                )
            }

            if (displayRecords.isNotEmpty()) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = RunTheme.colorScheme.blue.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Summary row (averages)
                TableRow(
                    col1 = "平均",
                    col2 = String.format("%.1f", monthStats.totalDistance / maxOf(monthStats.runCount, 1)),
                    col3 = formatAverageDuration(monthStats.totalDurationMinutes, monthStats.runCount),
                    col4 = monthStats.avgPace,
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
    isToday: Boolean = false,
    isSummary: Boolean = false
) {
    val textColor = when {
        isSummary -> RunTheme.colorScheme.blue
        isHeader -> SecondaryTextColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    val fontWeight = when {
        isHeader || isSummary -> FontWeight.Medium
        isToday -> FontWeight.SemiBold
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
            color = if (isToday) RunTheme.colorScheme.blue else textColor,
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
 * Format average duration
 */
private fun formatAverageDuration(totalMinutes: Double, runCount: Int): String {
    if (runCount <= 0) return "-"
    val avgMinutes = totalMinutes / runCount
    val hours = (avgMinutes / 60).toInt()
    val mins = (avgMinutes % 60).toInt()
    return if (hours > 0) {
        "${hours}h${mins}'"
    } else {
        "${mins}'"
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthDetailTablePreview() {
    val mockDays = listOf(
        DayRunData(dayOfMonth = 3, totalDistance = 5.2, runCount = 1, totalDurationMinutes = 32.0, avgPace = "6'09\""),
        DayRunData(dayOfMonth = 6, totalDistance = 10.5, runCount = 1, totalDurationMinutes = 58.0, avgPace = "5'31\""),
        DayRunData(dayOfMonth = 9, totalDistance = 8.3, runCount = 1, totalDurationMinutes = 45.0, avgPace = "5'25\""),
        DayRunData(dayOfMonth = 12, totalDistance = 6.0, runCount = 1, totalDurationMinutes = 35.0, avgPace = "5'50\"", isToday = true),
    )
    val monthStats = MonthStatistics(
        totalDistance = 30.0,
        totalDurationMinutes = 170.0,
        runCount = 4,
        avgPace = "5'40\""
    )
    MonthDetailTable(
        dailyRecords = mockDays,
        monthStats = monthStats
    )
}
