package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.oterman.rundemo.domain.model.WeekStatistics
import com.oterman.rundemo.ui.theme.RunOrange
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Week statistics card with 7-day grid
 * Matches iOS LatestWeekRunView
 */
@Composable
fun WeekStatisticsCard(
    stats: WeekStatistics,
    modifier: Modifier = Modifier,
    onDayClick: (DayRunData) -> Unit = {},
    onClick: () -> Unit = {}
) {
    StatisticsCard(
        modifier = modifier.clickable { onClick() }
    ) {
        // Title
        Text(
            text = "本周",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Distance
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format("%.1f", stats.totalDistance),
                    color = RunOrange,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "公里",
                    color = SecondaryTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                )
            }

            // Duration
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${stats.formattedHours}",
                    color = RunOrange,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "小时",
                    color = SecondaryTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                )
                Text(
                    text = "${stats.formattedMinutes}",
                    color = RunOrange,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Text(
                    text = "分钟",
                    color = SecondaryTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                )
            }
        }

        // 7-day grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stats.dailyRecords.forEach { dayData ->
                DayCell(
                    dayData = dayData,
                    onClick = { onDayClick(dayData) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeekStatisticsCardPreview() {
    val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
    val dailyRecords = weekDays.mapIndexed { index, day ->
        DayRunData(
            dayOfWeek = day,
            totalDistance = if (index % 2 == 0) (index + 1) * 1.5 else 0.0,
            runCount = if (index % 2 == 0) 1 else 0,
            isToday = index == 3,
            isFuture = index > 4
        )
    }

    WeekStatisticsCard(
        stats = WeekStatistics(
            totalDistance = 25.5,
            totalDurationMinutes = 185.0,
            dailyRecords = dailyRecords
        )
    )
}
