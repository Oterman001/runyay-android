package com.oterman.rundemo.presentation.feature.statistics.year.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.data.repository.RunDataRepository

/**
 * Content renderer for trajectory wall display.
 * Renders loading / empty / grid states without any card or header chrome.
 */
@Composable
fun TrajectoryWallGrid(
    trajectoryWorkoutIds: List<String>,
    itemsPerRow: Int,
    isLoading: Boolean,
    repository: RunDataRepository,
    distanceMap: Map<String, Double> = emptyMap(),
    onWorkoutClick: (workoutId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Text(
                        text = "正在加载轨迹数据...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }

        trajectoryWorkoutIds.isEmpty() -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "本年度暂无跑步轨迹",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }

        else -> {
            val chunkedIds = trajectoryWorkoutIds.chunked(itemsPerRow)

            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (rowIds in chunkedIds) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (workoutId in rowIds) {
                            TrajectoryWallCell(
                                workoutId = workoutId,
                                repository = repository,
                                totalDistanceKm = distanceMap[workoutId] ?: 0.0,
                                onClick = { onWorkoutClick(workoutId) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(itemsPerRow - rowIds.size) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
