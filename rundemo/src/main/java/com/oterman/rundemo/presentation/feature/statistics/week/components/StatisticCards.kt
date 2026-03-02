package com.oterman.rundemo.presentation.feature.statistics.week.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * 2x2 grid of statistic cards
 */
@Composable
fun StatisticCardsGrid(
    totalDistance: Double,
    totalDurationMinutes: Double,
    avgPace: String,
    totalElevation: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Distance | Duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SimpleStatCard(
                icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                title = "总距离",
                value = String.format("%.1f", totalDistance),
                unit = "公里",
                modifier = Modifier.weight(1f)
            )
            SimpleStatCard(
                icon = Icons.Outlined.Schedule,
                title = "总时长",
                value = formatDurationValue(totalDurationMinutes),
                unit = "",
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Pace | Elevation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SimpleStatCard(
                icon = Icons.Outlined.Speed,
                title = "平均配速",
                value = avgPace,
                unit = "/公里",
                modifier = Modifier.weight(1f)
            )
            SimpleStatCard(
                icon = Icons.Outlined.Terrain,
                title = "累计上升",
                value = String.format("%.0f", totalElevation),
                unit = "米",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Single statistic card with icon, title, value and unit
 */
@Composable
private fun SimpleStatCard(
    icon: ImageVector,
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    val isDark = RunTheme.isDark

    StatisticsCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(4.dp)
        ) {
            // Icon and title row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = RunTheme.colorScheme.blue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = SecondaryTextColor,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Value and unit
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        color = SecondaryTextColor,
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Format duration in minutes to display string
 */
private fun formatDurationValue(minutes: Double): String {
    val hours = (minutes / 60).toInt()
    val mins = (minutes % 60).toInt()
    return if (hours > 0) {
        "${hours}h${mins}'"
    } else {
        "${mins}'"
    }
}

@Preview(showBackground = true)
@Composable
private fun StatisticCardsGridPreview() {
    StatisticCardsGrid(
        totalDistance = 42.5,
        totalDurationMinutes = 265.0,
        avgPace = "5'30\"",
        totalElevation = 320.0
    )
}
