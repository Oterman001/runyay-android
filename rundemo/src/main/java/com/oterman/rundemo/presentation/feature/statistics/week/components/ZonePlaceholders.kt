package com.oterman.rundemo.presentation.feature.statistics.week.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.presentation.feature.rundetail.components.AbilityZoneBar
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Placeholder card for heart rate zone distribution (used by Month/Year/Total pages)
 */
@Composable
fun HeartRateZonePlaceholder(
    modifier: Modifier = Modifier
) {
    ZonePlaceholderCard(
        title = "心率区间分布",
        modifier = modifier
    )
}

/**
 * Placeholder card for speed/pace zone distribution (used by Month/Year/Total pages)
 */
@Composable
fun SpeedZonePlaceholder(
    modifier: Modifier = Modifier
) {
    ZonePlaceholderCard(
        title = "配速区间分布",
        modifier = modifier
    )
}

/**
 * Generic placeholder card for zone distributions
 */
@Composable
private fun ZonePlaceholderCard(
    title: String,
    modifier: Modifier = Modifier
) {
    StatisticsCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "待实现",
                tint = SecondaryTextColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "$title (待实现)",
                fontSize = 14.sp,
                color = SecondaryTextColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * 心率区间分布卡片
 * 对标 iOS RunWeekView 中的心率区间部分
 * 支持 7区间 / 5区间 切换
 */
@Composable
fun HeartRateZoneCard(
    heartRate7Zones: List<AbilityZone>,
    heartRate5Zones: List<AbilityZone>,
    modifier: Modifier = Modifier
) {
    val hasAnyZones = heartRate7Zones.isNotEmpty() || heartRate5Zones.isNotEmpty()
    if (!hasAnyZones) return

    var show7Zone by remember { mutableStateOf(true) }
    val displayedZones = if (show7Zone) heartRate7Zones else heartRate5Zones

    StatisticsCard(modifier = modifier) {
        // 标题行 + 切换器
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "心率区间分布",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            // 5/7区间切换
            if (heartRate7Zones.isNotEmpty() && heartRate5Zones.isNotEmpty()) {
                ZoneToggle(
                    show7Zone = show7Zone,
                    onToggle = { show7Zone = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 区间条形图
        if (displayedZones.isNotEmpty()) {
            AbilityZoneBar(zones = displayedZones)
        }
    }
}

/**
 * 配速区间分布卡片
 * 对标 iOS RunWeekView 中的配速区间部分
 */
@Composable
fun SpeedZoneCard(
    speedZones: List<AbilityZone>,
    modifier: Modifier = Modifier
) {
    if (speedZones.isEmpty()) return

    StatisticsCard(modifier = modifier) {
        // 标题行
        Text(
            text = "配速区间分布",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 区间条形图
        AbilityZoneBar(zones = speedZones)
    }
}

/**
 * 5/7 区间切换按钮
 */
@Composable
private fun ZoneToggle(
    show7Zone: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp)
    ) {
        ToggleChip(
            text = "7区间",
            selected = show7Zone,
            onClick = { onToggle(true) }
        )
        ToggleChip(
            text = "5区间",
            selected = !show7Zone,
            onClick = { onToggle(false) }
        )
    }
}

@Composable
private fun ToggleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(
                if (selected) RunTheme.colorScheme.blue
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}
