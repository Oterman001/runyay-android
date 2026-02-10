package com.oterman.rundemo.presentation.feature.statistics.year.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.domain.model.MonthRangeData
import com.oterman.rundemo.ui.theme.NoDataBg
import com.oterman.rundemo.ui.theme.NoDataBgDark
import com.oterman.rundemo.ui.theme.RunBlue

/**
 * Mini month heatmap view for year grid (3x4 layout)
 * Corresponds to iOS MiniMonthView
 */
@Composable
fun MiniMonthView(
    monthRangeData: MonthRangeData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        // Month title
        Text(
            text = "${monthRangeData.month}月",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // 7x6 heatmap grid (7 columns, up to 6 rows)
        MiniMonthHeatmapGrid(
            dailyRecords = monthRangeData.dailyRecords
        )
    }
}

/**
 * Heatmap grid for mini month view
 */
@Composable
private fun MiniMonthHeatmapGrid(
    dailyRecords: List<DayRunData>,
    modifier: Modifier = Modifier
) {
    val cellSize = 10.dp
    val cellSpacing = 2.dp
    val cellRadius = 3.dp

    // Calculate row count (7 columns)
    val rowCount = (dailyRecords.size + 6) / 7

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(cellSpacing)
    ) {
        for (rowIndex in 0 until rowCount) {
            Row(horizontalArrangement = Arrangement.spacedBy(cellSpacing)) {
                for (colIndex in 0 until 7) {
                    val index = rowIndex * 7 + colIndex
                    if (index < dailyRecords.size) {
                        val dayData = dailyRecords[index]
                        MiniDayCell(
                            dayData = dayData,
                            cellSize = cellSize,
                            cellRadius = cellRadius
                        )
                    } else {
                        Spacer(modifier = Modifier.size(cellSize))
                    }
                }
            }
        }
    }
}

/**
 * Single day cell in mini heatmap
 */
@Composable
private fun MiniDayCell(
    dayData: DayRunData,
    cellSize: Dp,
    cellRadius: Dp
) {
    val isDark = isSystemInDarkTheme()
    val fullColorThreshold = 5.0

    val backgroundColor = when {
        dayData.isPlaceholder -> Color.Transparent
        dayData.isFuture -> Color.Transparent
        dayData.totalDistance <= 0 -> if (isDark) NoDataBgDark else NoDataBg
        dayData.totalDistance >= fullColorThreshold -> RunBlue
        else -> {
            val intensity = (dayData.totalDistance / fullColorThreshold).coerceIn(0.0, 1.0)
            val minAlpha = if (isDark) 0.3f else 0.2f
            RunBlue.copy(alpha = (minAlpha + intensity * (1f - minAlpha)).toFloat())
        }
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .clip(RoundedCornerShape(cellRadius))
            .background(backgroundColor)
    )
}

@Preview(showBackground = true)
@Composable
private fun MiniMonthViewPreview() {
    // Mock January data (Jan 1st = Monday, 31 days)
    val mockDays = buildList {
        // No placeholder needed for Jan 2024 (1st is Monday)
        for (day in 1..31) {
            add(
                DayRunData(
                    dayOfMonth = day,
                    totalDistance = if (day % 3 == 0) (day % 10 + 1).toDouble() else 0.0,
                    isToday = day == 15,
                    isFuture = day > 15
                )
            )
        }
    }

    MiniMonthView(
        monthRangeData = MonthRangeData(
            year = 2024,
            month = 1,
            totalDistance = 42.5,
            runCount = 10,
            dailyRecords = mockDays
        ),
        onClick = {}
    )
}
