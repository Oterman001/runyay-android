package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 跑步详情页头部卡片
 * 显示距离、时间、设备信息和用户头像
 * 与iOS V3HeaderCardView对应
 */
@Composable
fun RunDetailHeaderCard(
    distance: Double,
    startTime: Long,
    endTime: Long,
    duration: Double,
    deviceName: String?,
    isOutdoor: Boolean,
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
                // 第一行：日期时间
                Text(
                    text = formatStartEndTime(startTime, endTime),
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
                        text = formatDuration(duration),
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
            AvatarPlaceholder()
        }
    }
}

/**
 * 头像占位符
 */
@Composable
private fun AvatarPlaceholder() {
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

/**
 * 格式化开始结束时间
 */
private fun formatStartEndTime(startTime: Long, endTime: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val startDate = Date(startTime)
    val endDate = Date(endTime)

    return "${dateFormat.format(startDate)} ${timeFormat.format(startDate)} - ${timeFormat.format(endDate)}"
}

/**
 * 格式化时长
 */
private fun formatDuration(durationMinutes: Double): String {
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
