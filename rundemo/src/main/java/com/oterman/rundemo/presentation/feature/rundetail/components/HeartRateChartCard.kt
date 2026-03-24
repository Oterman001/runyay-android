package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.domain.model.ChartDataPoint
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants

/**
 * 心率图表卡片（合并版）
 * 将折线图 + 心率区间条形图合并到同一张卡片内
 * 支持 7区间 / 5区间 切换
 * 对标 iOS HeartRateAndZoneView
 */
@Composable
fun HeartRateChartCard(
    heartRateSeries: List<ChartDataPoint>,
    heartRate7Zones: List<AbilityZone>,
    heartRate5Zones: List<AbilityZone>,
    avgHeartRate: Double,
    maxHeartRate: Double,
    minHeartRate: Double,
    initialShow7Zone: Boolean = true,
    onZoneChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (heartRateSeries.isEmpty()) return

    // 区间显示模式: true = 7区间, false = 5区间
    var show7Zone by remember { mutableStateOf(initialShow7Zone) }
    val displayedZones = if (show7Zone) heartRate7Zones else heartRate5Zones
    val hasAnyZones = heartRate7Zones.isNotEmpty() || heartRate5Zones.isNotEmpty()

    AppCard(
        modifier = modifier
            .padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RunDetailLayoutConstants.HeaderCardPadding.dp)
        ) {
            // ========== 心率折线图标题行 ==========
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❤️ 心率(bpm)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                // 平均/最大/最小 心率摘要
                Text(
                    text = "均${avgHeartRate.toInt()}  最高${maxHeartRate.toInt()}  最低${minHeartRate.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ========== 心率折线图（嵌入式，不含外层Card） ==========
            RunDataLineChartContent(
                dataPoints = heartRateSeries,
                lineColor = Color(0xFFE53935),
                avgValue = avgHeartRate,
                chartHeight = 160
            )

//            Text(
//                text = "长按查看数值 · 双指缩放",
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
//                textAlign = TextAlign.Center,
//                fontSize = 10.sp,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(top = 4.dp)
//            )

            // ========== 分隔线 + 区间部分 ==========
            if (hasAnyZones) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // 5区间/7区间 切换按钮
                if (heartRate7Zones.isNotEmpty() && heartRate5Zones.isNotEmpty()) {
                    HeartRateZoneToggle(
                        show7Zone = show7Zone,
                        onToggle = {
                            show7Zone = it
                            onZoneChanged?.invoke(it)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 区间条形图
                if (displayedZones.isNotEmpty()) {
                    AbilityZoneBar(zones = displayedZones)
                }
            }
        }
    }
}

/**
 * 心率区间 5/7 切换按钮
 * 对标 iOS Picker("", selection: $runDetailVm.heartRateZoneDisplayMode)
 */
@Composable
private fun HeartRateZoneToggle(
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
                if (selected) MaterialTheme.colorScheme.primary
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
