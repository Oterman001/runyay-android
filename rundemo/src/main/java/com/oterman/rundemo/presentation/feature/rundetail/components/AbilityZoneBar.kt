package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.domain.model.AbilityZoneType

/**
 * 心率/配速区间水平条形图
 * 对标iOS AbilityZoneItemView 的两行布局
 *
 * 布局:
 * ┌──────────────────────────────────────────────────────┐
 * │ 轻松跑(E)    │  ████████░░░░░░░  │ 33.1%            │
 * │ 5:47~6:21    │                    │ 1:23:45          │
 * └──────────────────────────────────────────────────────┘
 */
@Composable
fun AbilityZoneBar(
    zones: List<AbilityZone>,
    modifier: Modifier = Modifier
) {
    if (zones.isEmpty()) return

    val totalDuration = zones.sumOf { it.duration }
    if (totalDuration <= 0) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        zones.forEach { zone ->
            val percentage = if (totalDuration > 0) zone.duration / totalDuration else 0.0
            ZoneBarRow(
                zone = zone,
                percentage = percentage.toFloat(),
                color = getZoneColor(zone)
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun ZoneBarRow(
    zone: AbilityZone,
    percentage: Float,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(35.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Zone description + speed range (85dp fixed)
        Column(
            modifier = Modifier.width(85.dp)
        ) {
            Text(
                text = zone.getZoneDescription(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                maxLines = 1
            )
            Text(
                text = zone.getFormattedSpeedRange(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Middle: Progress bar (capsule shape, 13dp height)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(13.dp)
                .clip(RoundedCornerShape(6.5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = percentage.coerceIn(0f, 1f))
                    .height(13.dp)
                    .clip(RoundedCornerShape(6.5.dp))
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Right: Percentage + duration (55dp fixed)
        Column(
            modifier = Modifier.width(55.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = String.format("%.1f%%", percentage * 100),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                textAlign = TextAlign.End
            )
            Text(
                text = zone.getFormattedDuration(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * 根据区间类型和索引获取对应颜色
 */
private fun getZoneColor(zone: AbilityZone): Color {
    return when (zone.zoneType) {
        AbilityZoneType.HEART_RATE_7 -> when (zone.zoneIndex) {
            1 -> Color(0xFF90CAF9)  // 蓝色极淡
            2 -> Color(0xFF64B5F6)  // 蓝色
            3 -> Color(0xFF4CAF50)  // 绿色
            4 -> Color(0xFFFFC107)  // 黄色
            5 -> Color(0xFFFF9800)  // 橙色
            6 -> Color(0xFFFF5722)  // 深橙
            7 -> Color(0xFFF44336)  // 红色
            else -> Color.Gray
        }
        AbilityZoneType.HEART_RATE_5 -> when (zone.zoneIndex) {
            1 -> Color(0xFF90CAF9)  // 蓝色
            2 -> Color(0xFF4CAF50)  // 绿色
            3 -> Color(0xFFFFC107)  // 黄色
            4 -> Color(0xFFFF9800)  // 橙色
            5 -> Color(0xFFF44336)  // 红色
            else -> Color.Gray
        }
        AbilityZoneType.SPEED -> when (zone.zoneIndex) {
            1 -> Color(0xFF90CAF9)  // E - 轻松跑
            2 -> Color(0xFF4CAF50)  // M - 马拉松配速
            3 -> Color(0xFFFFC107)  // T - 乳酸阈值
            4 -> Color(0xFFFF9800)  // I - 间歇
            5 -> Color(0xFFF44336)  // R - 重复
            else -> Color.Gray
        }
    }
}
