package com.oterman.rundemo.presentation.feature.statistics.week.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.presentation.feature.home.components.DayCell
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.ui.theme.RunBlue

/**
 * Week summary header showing run count and total distance
 */
@Composable
fun WeekSummaryHeader(
    runCount: Int,
    totalDistance: Double,
    modifier: Modifier = Modifier
) {
    val summaryText = if (runCount > 0) {
        "本周累计跑步 $runCount 次，总计 ${String.format("%.1f", totalDistance)} 公里"
    } else {
        "本周还没有跑步记录"
    }

    Text(
        text = summaryText,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

/**
 * 7-day grid for week view, reusing DayCell component
 */
@Composable
fun WeekDayGrid(
    dailyRecords: List<DayRunData>,
    onDayClick: (DayRunData) -> Unit,
    modifier: Modifier = Modifier
) {
    StatisticsCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dailyRecords.forEach { dayData ->
                DayCell(
                    dayData = dayData,
                    modifier = Modifier.weight(1f),
                    onClick = { onDayClick(dayData) }
                )
            }
        }
    }
}

/**
 * Combined week summary and day grid component
 */
@Composable
fun WeekSummaryAndGrid(
    runCount: Int,
    totalDistance: Double,
    dailyRecords: List<DayRunData>,
    onDayClick: (DayRunData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        WeekSummaryHeader(
            runCount = runCount,
            totalDistance = totalDistance,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        WeekDayGrid(
            dailyRecords = dailyRecords,
            onDayClick = onDayClick
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WeekDayGridPreview() {
    val mockDays = listOf(
        DayRunData(dayOfWeek = "一", totalDistance = 5.2, runCount = 1),
        DayRunData(dayOfWeek = "二", totalDistance = 0.0, runCount = 0),
        DayRunData(dayOfWeek = "三", totalDistance = 10.5, runCount = 2),
        DayRunData(dayOfWeek = "四", totalDistance = 0.0, runCount = 0),
        DayRunData(dayOfWeek = "五", totalDistance = 8.3, runCount = 1),
        DayRunData(dayOfWeek = "六", totalDistance = 0.0, runCount = 0, isToday = true),
        DayRunData(dayOfWeek = "日", totalDistance = 0.0, runCount = 0, isFuture = true)
    )
    WeekSummaryAndGrid(
        runCount = 4,
        totalDistance = 24.0,
        dailyRecords = mockDays,
        onDayClick = {}
    )
}
