package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
 * 对标iOS HeartRateAndZoneView / SpeedChartAndZoneView 中的区间展示部分
 */
@Composable
fun AbilityZoneBar(
    zones: List<AbilityZone>,
    title: String = "区间分布",
    modifier: Modifier = Modifier
) {
    if (zones.isEmpty()) return

    val totalDuration = zones.sumOf { it.duration }
    if (totalDuration <= 0) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        zones.forEach { zone ->
            val percentage = if (totalDuration > 0) zone.duration / totalDuration else 0.0
            ZoneBarRow(
                zone = zone,
                percentage = percentage.toFloat(),
                color = getZoneColor(zone)
            )
            Spacer(modifier = Modifier.height(4.dp))
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 区间名称
        Text(
            text = zone.getZoneName(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.width(6.dp))

        // 进度条
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = percentage.coerceIn(0f, 1f))
                    .height(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 百分比
        Text(
            text = String.format("%.0f%%", percentage * 100),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.width(4.dp))

        // 时长
        Text(
            text = zone.getFormattedDuration(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End,
            fontSize = 11.sp
        )
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

