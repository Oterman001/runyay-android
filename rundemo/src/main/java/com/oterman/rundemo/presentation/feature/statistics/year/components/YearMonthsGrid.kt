package com.oterman.rundemo.presentation.feature.statistics.year.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.domain.model.MonthRangeData
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Year 12-month heatmap grid card (3x4 layout)
 */
@Composable
fun YearMonthsGridCard(
    runCount: Int,
    totalDistance: Double,
    monthRangeDataList: List<MonthRangeData>,
    onMonthClick: (MonthRangeData) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val dividerColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)

    StatisticsCard(modifier = modifier) {
        Column {
            // Header row
            YearSummaryHeader(
                runCount = runCount,
                totalDistance = totalDistance
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color = dividerColor,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // 3x4 grid
            YearMonthsGrid(
                monthRangeDataList = monthRangeDataList,
                onMonthClick = onMonthClick
            )
        }
    }
}

/**
 * Year summary header: "年跑步 X 次，Y.Z 公里"
 */
@Composable
private fun YearSummaryHeader(
    runCount: Int,
    totalDistance: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "年跑步",
            fontSize = 14.sp,
            color = SecondaryTextColor
        )
        Text(
            text = "$runCount",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "次，",
            fontSize = 14.sp,
            color = SecondaryTextColor
        )
        Text(
            text = String.format("%.1f", totalDistance),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = RunBlue
        )
        Text(
            text = "公里",
            fontSize = 14.sp,
            color = SecondaryTextColor
        )
    }
}

/**
 * 3x4 grid of mini month views
 */
@Composable
private fun YearMonthsGrid(
    monthRangeDataList: List<MonthRangeData>,
    onMonthClick: (MonthRangeData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (rowIndex in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (colIndex in 0 until 3) {
                    val monthIndex = rowIndex * 3 + colIndex
                    if (monthIndex < monthRangeDataList.size) {
                        MiniMonthView(
                            monthRangeData = monthRangeDataList[monthIndex],
                            onClick = { onMonthClick(monthRangeDataList[monthIndex]) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun YearMonthsGridCardPreview() {
    val mockMonths = (1..12).map { month ->
        MonthRangeData(
            year = 2024,
            month = month,
            totalDistance = (month * 10).toDouble(),
            runCount = month * 2,
            dailyRecords = (1..28).map { day ->
                DayRunData(
                    dayOfMonth = day,
                    totalDistance = if (day % 3 == 0) (day % 5 + 1).toDouble() else 0.0
                )
            }
        )
    }

    YearMonthsGridCard(
        runCount = 120,
        totalDistance = 780.5,
        monthRangeDataList = mockMonths,
        onMonthClick = {}
    )
}
