package com.oterman.rundemo.presentation.feature.statistics.week.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.presentation.feature.home.components.DayCell
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.presentation.feature.statistics.components.DayTrajectoryCell
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
 * Supports trajectory mode
 */
@Composable
fun WeekDayGrid(
    dailyRecords: List<DayRunData>,
    showTrajectoryMode: Boolean,
    trajectoryDataMap: Map<String, List<TrackPoint>>,
    onDayClick: (DayRunData) -> Unit,
    modifier: Modifier = Modifier
) {
    StatisticsCard(modifier = modifier) {
        Crossfade(
            targetState = showTrajectoryMode,
            animationSpec = tween(durationMillis = 300),
            label = "trajectory_mode_crossfade"
        ) { trajectoryMode ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dailyRecords.forEach { dayData ->
                    if (trajectoryMode) {
                        // 轨迹模式：显示轨迹缩略图
                        val workoutId = dayData.workoutIds.firstOrNull()
                        val trackPoints = workoutId?.let { trajectoryDataMap[it] }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = dayData.hasRun) { onDayClick(dayData) }
                        ) {
                            DayTrajectoryCell(
                                workoutId = workoutId,
                                trackPoints = trackPoints,
                                isFuture = dayData.isFuture
                            )
                        }
                    } else {
                        // 距离模式：显示距离热力图
                        DayCell(
                            dayData = dayData,
                            modifier = Modifier.weight(1f),
                            onClick = { onDayClick(dayData) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Combined week summary and day grid component with trajectory mode support
 */
@Composable
fun WeekSummaryAndGrid(
    runCount: Int,
    totalDistance: Double,
    dailyRecords: List<DayRunData>,
    showTrajectoryMode: Boolean,
    trajectoryDataMap: Map<String, List<TrackPoint>>,
    onDayClick: (DayRunData) -> Unit,
    onToggleTrajectoryMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    StatisticsCard(modifier = modifier) {
        Column {
            // Header with toggle button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Summary text
                val summaryText = if (runCount > 0) {
                    "本周累计跑步 $runCount 次，总计 ${String.format("%.1f", totalDistance)} 公里"
                } else {
                    "本周还没有跑步记录"
                }
                
                Text(
                    text = summaryText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                
                // Toggle button
                IconButton(
                    onClick = onToggleTrajectoryMode,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = if (showTrajectoryMode) {
                            Icons.Default.RemoveRedEye
                        } else {
                            Icons.Default.Timeline
                        },
                        contentDescription = if (showTrajectoryMode) {
                            "切换到距离显示"
                        } else {
                            "切换到轨迹显示"
                        },
                        tint = RunBlue
                    )
                }
            }
            
            // Day grid with animation
            Crossfade(
                targetState = showTrajectoryMode,
                animationSpec = tween(durationMillis = 300),
                label = "trajectory_mode_crossfade"
            ) { trajectoryMode ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dailyRecords.forEach { dayData ->
                        if (trajectoryMode) {
                            // 轨迹模式：显示轨迹缩略图
                            val workoutId = dayData.workoutIds.firstOrNull()
                            val trackPoints = workoutId?.let { trajectoryDataMap[it] }
                            
                            DayTrajectoryCell(
                                workoutId = workoutId,
                                trackPoints = trackPoints,
                                isFuture = dayData.isFuture,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // 距离模式：显示距离热力图
                            DayCell(
                                dayData = dayData,
                                modifier = Modifier.weight(1f),
                                onClick = { onDayClick(dayData) }
                            )
                        }
                    }
                }
            }
        }
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
        showTrajectoryMode = false,
        trajectoryDataMap = emptyMap(),
        onDayClick = {},
        onToggleTrajectoryMode = {}
    )
}
