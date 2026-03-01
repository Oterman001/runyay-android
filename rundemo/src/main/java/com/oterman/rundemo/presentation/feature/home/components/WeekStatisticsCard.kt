package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.domain.model.WeekStatistics
import com.oterman.rundemo.presentation.feature.statistics.components.DayTrajectoryCell
import com.oterman.rundemo.ui.theme.RunOrange
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.RunYayFontFamily
import com.oterman.rundemo.ui.theme.RunYayFontFamily4
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Week statistics card with 7-day grid
 * Matches iOS LatestWeekRunView
 */
@Composable
fun WeekStatisticsCard(
    stats: WeekStatistics,
    modifier: Modifier = Modifier,
    showTrajectoryMode: Boolean = false,
    trajectoryDataMap: Map<String, List<TrackPoint>> = emptyMap(),
    onToggleTrajectoryMode: () -> Unit = {},
    onDayClick: (DayRunData) -> Unit = {},
    onClick: () -> Unit = {}
) {
    StatisticsCard(
        modifier = modifier.clickable { onClick() }
    ) {
        // Title with toggle button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "本周",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = onToggleTrajectoryMode,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (showTrajectoryMode) {
                        Icons.Default.RemoveRedEye
                    } else {
                        Icons.Outlined.Route
                    },
                    contentDescription = if (showTrajectoryMode) "切换到距离显示" else "切换到轨迹显示",
                    tint = RunTheme.colorScheme.blue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

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
                    color = RunTheme.colorScheme.orange ,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = RunYayFontFamily4,
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    text = "公里",
                    color = SecondaryTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.alignByBaseline().padding(start = 2.dp)
                )
            }

            // Duration
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${stats.formattedHours}",
                    fontSize = 22.sp,
                    color = RunTheme.colorScheme.orange,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = RunYayFontFamily4,
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    text = "小时",
                    color = SecondaryTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.alignByBaseline().padding(start = 2.dp)
                )
                Text(
                    text = "${stats.formattedMinutes}",
                    fontSize = 22.sp,
                    color = RunTheme.colorScheme.orange,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = RunYayFontFamily4,
                    modifier = Modifier.alignByBaseline().padding(start = 4.dp)
                )
                Text(
                    text = "分钟",
                    color = SecondaryTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.alignByBaseline().padding(start = 2.dp)
                )
            }
        }

        // 7-day grid with Crossfade animation
        Crossfade(
            targetState = showTrajectoryMode,
            animationSpec = tween(durationMillis = 300),
            label = "week_card_trajectory_crossfade"
        ) { trajectoryMode ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stats.dailyRecords.forEach { dayData ->
                    if (trajectoryMode) {
                        val workoutId = dayData.workoutIds.firstOrNull()
                        val trackPoints = workoutId?.let { trajectoryDataMap[it] }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = dayData.hasRun) { onDayClick(dayData) }
                        ) {
                            DayTrajectoryCell(
                                workoutId = workoutId,
                                trackPoints = trackPoints,
                                isFuture = dayData.isFuture,
                                isIndoor = dayData.isIndoor,
                                totalDistanceKm = dayData.totalDistance
                            )
                            Text(
                                text = dayData.dayOfWeek,
                                fontSize = 11.sp,
                                color = if (dayData.isToday) RunTheme.colorScheme.blue else SecondaryTextColor,
                                fontWeight = if (dayData.isToday) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    } else {
                        DayCell(
                            dayData = dayData,
                            onClick = { onDayClick(dayData) },
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
