package com.oterman.rundemo.presentation.feature.statistics.month.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.ui.theme.NoDataBg
import com.oterman.rundemo.ui.theme.NoDataBgDark
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Month calendar header showing run count and total distance
 */
@Composable
fun MonthCalendarHeader(
    runCount: Int,
    totalDistance: Double,
    modifier: Modifier = Modifier
) {
    val summaryText = if (runCount > 0) {
        "本月累计跑步 $runCount 次，总计 ${String.format("%.1f", totalDistance)} 公里"
    } else {
        "本月还没有跑步记录"
    }

    Text(
        text = summaryText,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * Weekday labels row (一 二 三 四 五 六 日)
 */
@Composable
fun WeekdayLabels(
    modifier: Modifier = Modifier
) {
    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekdays.forEach { day ->
            Text(
                text = day,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SecondaryTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Month calendar grid with 7 columns
 * Contains placeholder cells for days before the 1st and day cells with heatmap
 */
@Composable
fun MonthCalendarGrid(
    dailyRecords: List<DayRunData>,
    onDayClick: (DayRunData) -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate row count (7 columns)
    val rowCount = (dailyRecords.size + 6) / 7

    Column(modifier = modifier) {
        for (rowIndex in 0 until rowCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (colIndex in 0 until 7) {
                    val index = rowIndex * 7 + colIndex
                    if (index < dailyRecords.size) {
                        val dayData = dailyRecords[index]
                        MonthDayCell(
                            dayData = dayData,
                            onClick = { if (dayData.hasRun) onDayClick(dayData) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Empty cell for remaining slots
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (rowIndex < rowCount - 1) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/**
 * Single day cell in month calendar
 * Shows day number on top and heatmap block below
 */
@Composable
private fun MonthDayCell(
    dayData: DayRunData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val cellShape = RoundedCornerShape(6.dp)
    val fullColorThreshold = 5.0

    if (dayData.isPlaceholder) {
        // Empty placeholder cell
        Box(
            modifier = modifier
                .aspectRatio(0.8f)
                .padding(2.dp)
        )
        return
    }

    // Calculate background color based on distance
    val backgroundColor = when {
        dayData.isFuture -> Color.Transparent
        dayData.totalDistance <= 0 -> if (isDark) NoDataBgDark else NoDataBg
        dayData.totalDistance >= fullColorThreshold -> {
            if (dayData.isIndoor) Color(0xFF8B5CF6) else RunBlue
        }
        else -> {
            val intensity = (dayData.totalDistance / fullColorThreshold).coerceIn(0.0, 1.0)
            val baseColor = if (dayData.isIndoor) Color(0xFF8B5CF6) else RunBlue
            val minAlpha = if (isDark) 0.3f else 0.2f
            baseColor.copy(alpha = (minAlpha + intensity * (1f - minAlpha)).toFloat())
        }
    }

    val distanceTextColor = when {
        dayData.totalDistance > 0 -> Color.White
        dayData.isFuture -> SecondaryTextColor
        else -> SecondaryTextColor
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(2.dp)
            .clickable(enabled = dayData.hasRun) { onClick() }
    ) {
        // Day number with today highlight
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .then(
                    if (dayData.isToday) {
                        Modifier
                            .background(RunBlue, CircleShape)
                    } else {
                        Modifier
                    }
                )
        ) {
            Text(
                text = dayData.dayOfMonth.toString(),
                fontSize = 10.sp,
                fontWeight = if (dayData.isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = if (dayData.isToday) Color.White else SecondaryTextColor,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Outer Box without clip - allows badge to overflow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            // Inner Box with clip - rounded corner heatmap background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(cellShape)
                    .background(backgroundColor)
                    .then(
                        if (dayData.isFuture) {
                            Modifier.border(
                                width = 1.dp,
                                color = RunBlue.copy(alpha = 0.4f),
                                shape = cellShape
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Show distance if has run
                if (dayData.totalDistance > 0) {
                    Text(
                        text = dayData.getFormattedDistance(),
                        color = distanceTextColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Badge for multiple runs - outside clip, matching DayCell style
            if (dayData.runCount >= 2) {
                val badgeSize = if (dayData.runCount >= 10) 16.dp else 14.dp
                val badgeFontSize = if (dayData.runCount >= 10) 8.sp else 9.sp
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .size(badgeSize)
                        .background(Color(0xFFE0E0E0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${dayData.runCount}",
                        color = Color.Red,
                        fontSize = badgeFontSize,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = badgeFontSize
                    )
                }
            }
        }
    }
}

/**
 * Complete month calendar card with header, weekday labels, divider, and grid
 */
@Composable
fun MonthCalendarCard(
    runCount: Int,
    totalDistance: Double,
    dailyRecords: List<DayRunData>,
    onDayClick: (DayRunData) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val dividerColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)

    StatisticsCard(modifier = modifier) {
        Column {
            MonthCalendarHeader(
                runCount = runCount,
                totalDistance = totalDistance
            )

            Spacer(modifier = Modifier.height(12.dp))

            WeekdayLabels()

            HorizontalDivider(
                thickness = 0.5.dp,
                color = dividerColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            MonthCalendarGrid(
                dailyRecords = dailyRecords,
                onDayClick = onDayClick
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthCalendarCardPreview() {
    // Mock November 2024 data (November 1st is Friday -> offset 4)
    val mockDays = buildList {
        // Placeholders for Mon-Thu (4 days)
        repeat(4) {
            add(DayRunData(isPlaceholder = true))
        }
        // Days 1-30
        for (day in 1..30) {
            add(
                DayRunData(
                    dayOfMonth = day,
                    totalDistance = if (day % 3 == 0) (day % 10).toDouble() else 0.0,
                    runCount = if (day == 15) 2 else if (day % 3 == 0) 1 else 0,
                    isToday = day == 20,
                    isFuture = day > 20
                )
            )
        }
    }

    MonthCalendarCard(
        runCount = 5,
        totalDistance = 42.5,
        dailyRecords = mockDays,
        onDayClick = {}
    )
}
