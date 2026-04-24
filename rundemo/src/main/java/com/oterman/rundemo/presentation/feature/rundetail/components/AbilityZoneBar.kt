package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.domain.model.AbilityZoneType
import com.oterman.rundemo.ui.theme.RunTheme

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
    modifier: Modifier = Modifier,
    showValueRange: Boolean = true
) {
    if (zones.isEmpty()) return

    val totalDuration = zones.sumOf { it.duration }
    if (totalDuration <= 0) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        zones.forEach { zone ->
            val percentage = if (totalDuration > 0) zone.duration / totalDuration else 0.0
            val zoneColor = getZoneColor(zone)
            ZoneBarRow(
                zone = zone,
                percentage = percentage.toFloat(),
                color = zoneColor,
                showValueRange = showValueRange
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun ZoneBarRow(
    zone: AbilityZone,
    percentage: Float,
    color: Color,
    showValueRange: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(35.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Zone description + speed range (85dp fixed)
        Column(
            modifier = Modifier.width(85.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = zone.getZoneDescription(),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.5.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showValueRange) {
                Text(
                    text = zone.getFormattedSpeedRange(),
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    maxLines = 1
                )
            }
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

        // Right: Percentage (primary) + duration (secondary), 65dp fixed
        Column(
            modifier = Modifier.width(65.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format("%.1f%%", percentage * 100),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.End,
                maxLines = 1
            )
            Text(
                text = zone.getFormattedDuration(),
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
    }
}

/**
 * 根据区间类型和索引获取对应颜色
 */
@Composable
private fun getZoneColor(zone: AbilityZone): Color {
    val colors = RunTheme.colorScheme
    return when (zone.zoneType) {
        AbilityZoneType.HEART_RATE_7 -> {
            val z7 = colors.zone7Colors
            z7.getOrElse(zone.zoneIndex - 1) { colors.neutral }
        }
        AbilityZoneType.HEART_RATE_5,
        AbilityZoneType.SPEED -> {
            val z5 = colors.zoneColors
            z5.getOrElse(zone.zoneIndex - 1) { colors.neutral }
        }
    }
}
