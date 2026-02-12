package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants
import com.oterman.rundemo.presentation.feature.rundetail.RunMetricItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 合并的 Header + DataGrid 卡片
 * 对标 iOS V3HeaderCardView，将距离/时间信息和数据网格合并在一张卡片内
 */
@Composable
fun RunDetailHeaderDataCard(
    distance: Double,
    startTime: Long,
    endTime: Long,
    duration: Double,
    deviceName: String?,
    isOutdoor: Boolean,
    metrics: List<RunMetricItem>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp)
    ) {
        // 主卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = RunDetailLayoutConstants.HeaderInvasionOffset.dp),
            shape = RoundedCornerShape(RunDetailLayoutConstants.HeaderCardRadius.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = RunDetailLayoutConstants.HeaderShadowElevation.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(RunDetailLayoutConstants.HeaderCardPadding.dp)
            ) {
                // ========== Header 部分 ==========

                // 第一行：日期时间
                Text(
                    text = formatHeaderStartEndTime(startTime, endTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 第二行：距离（大字）
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = String.format("%.2f", distance),
                        fontSize = RunDetailLayoutConstants.DistanceFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "km",
                        fontSize = RunDetailLayoutConstants.DistanceUnitFontSize.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 第三行：时长 + 运动类型
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatHeaderDuration(duration),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isOutdoor) "户外跑" else "室内跑",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 第四行：设备信息（可选）
                deviceName?.let { device ->
                    if (device.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Watch,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = device,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ========== 分隔线 ==========
                if (metrics.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // ========== DataGrid 部分 ==========
                    val chunkedMetrics = metrics.chunked(3)
                    chunkedMetrics.forEachIndexed { index, rowMetrics ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowMetrics.forEach { metric ->
                                MergedMetricCell(
                                    metric = metric,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowMetrics.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        if (index < chunkedMetrics.size - 1) {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }

        // 头像（右上角，向上偏移露出一半）
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(
                    x = (-RunDetailLayoutConstants.AvatarTrailingPadding).dp,
                    y = RunDetailLayoutConstants.AvatarVerticalOffset.dp
                )
        ) {
            HeaderAvatarPlaceholder()
        }
    }
}

/**
 * 单个指标单元格
 */
@Composable
private fun MergedMetricCell(
    metric: RunMetricItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = metric.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            metric.unit?.let { unit ->
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = metric.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 头像占位符
 */
@Composable
private fun HeaderAvatarPlaceholder() {
    Box(
        modifier = Modifier
            .size(RunDetailLayoutConstants.AvatarSize.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(
                width = RunDetailLayoutConstants.AvatarBorderWidth.dp,
                color = Color.White,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "跑",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private fun formatHeaderStartEndTime(startTime: Long, endTime: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startDate = Date(startTime)
    val endDate = Date(endTime)
    return "${dateFormat.format(startDate)} ${timeFormat.format(startDate)} - ${timeFormat.format(endDate)}"
}

private fun formatHeaderDuration(durationMinutes: Double): String {
    if (durationMinutes <= 0) return "-"
    val totalSeconds = (durationMinutes * 60).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

