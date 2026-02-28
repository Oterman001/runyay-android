package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.presentation.components.trajectory.BlendedTrajectoryThumbnail
import com.oterman.rundemo.presentation.feature.home.tabs.formatDateCompact
import com.oterman.rundemo.presentation.feature.home.tabs.formatDuration
import com.oterman.rundemo.presentation.feature.home.tabs.formatPace
import com.oterman.rundemo.ui.theme.RunYayFontFamily
import com.oterman.rundemo.ui.theme.RunYayFontFamily4

/**
 * 跑步记录列表项（Distance Hero + Blended Trajectory）
 * - 距离放大显示在左侧作为视觉焦点
 * - 轨迹缩略图透明背景融入卡片右侧
 * - 顶行：日期时段 + 设备信息
 * - 底行：时长 + 配速
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RunRecordItem(
    record: RunRecordEntity,
    trackPoints: List<TrackPoint>?,
    isTrackPointsLoading: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val isOutdoor = record.outdoor == 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 22.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // 第一行：日期时段 + 设备信息（独立占满宽度，不与轨迹图重叠）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDateCompact(record.startTime, record.endTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                record.deviceInfo?.let { device ->
                    if (device.isNotBlank()) {
                        Text(
                            text = device,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 第二+三行区域：左侧文字 + 右侧轨迹缩略图（Row 水平布局，互不遮挡）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：Hero 距离 + 时长配速
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%.2f", record.totalDistance),
                            fontSize = 33.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = RunYayFontFamily
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "公里",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = formatDuration(record.activeDuration),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = RunYayFontFamily4
                        )
                        Text(
                            text = formatPace(record.averageSpeed),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = RunYayFontFamily4
                        )
                    }
                }

                // 右侧：轨迹缩略图（透明融合，仅占第二三行高度）
                if (isOutdoor) {
                    BlendedTrajectoryThumbnail(
                        trackPoints = trackPoints,
                        isLoading = isTrackPointsLoading,
                        isOutdoor = true,
                        width = 75.dp,
                        height = 75.dp
                    )
                } else {
                    // todo 室内跑
                }
            }
        }
    }
}