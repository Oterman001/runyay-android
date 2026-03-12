package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.oterman.rundemo.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.components.InclusiveLevelIndicator
import com.oterman.rundemo.presentation.components.trajectory.BlendedTrajectoryThumbnail
import com.oterman.rundemo.presentation.feature.home.tabs.formatDateWithWeekday
import com.oterman.rundemo.presentation.feature.home.tabs.formatDuration
import com.oterman.rundemo.presentation.feature.home.tabs.formatPace
import com.oterman.rundemo.ui.theme.RunTheme
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
    primaryColor: Color = Color.Unspecified,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onInclusiveLevelClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
) {
    val isOutdoor = record.outdoor == 0
    val distanceColor = if (primaryColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else primaryColor

    AppCard(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // 第一行：跑步图标 + 日期时间周几 + 设备信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsRun,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = RunTheme.colorScheme.blue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDateWithWeekday(record.startTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = if (onInclusiveLevelClick != null)
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onInclusiveLevelClick() }
                    else Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val platformName = DataSourcePlatform.fromCode(record.datasource ?: "")?.displayNameEn
                    if (!platformName.isNullOrBlank()) {
                        Text(
                            text = platformName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    InclusiveLevelIndicator(inclusiveLevel = record.inclusiveLevel)
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
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = distanceColor,
                            fontFamily = RunYayFontFamily
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "公里",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .alignByBaseline()
//                                .padding(bottom = 4.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDuration(record.activeDuration),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = RunYayFontFamily4
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                }

                // 右侧：轨迹缩略图（透明融合，仅占第二三行高度）
                if (isOutdoor) {
                    BlendedTrajectoryThumbnail(
                        trackPoints = trackPoints,
                        isLoading = isTrackPointsLoading,
                        isOutdoor = true,
                        totalDistance = record.totalDistance,
                        width = 75.dp,
                        height = 75.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.figure_run_treadmill),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}