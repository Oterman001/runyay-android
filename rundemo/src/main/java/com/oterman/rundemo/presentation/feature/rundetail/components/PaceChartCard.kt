package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.domain.model.ChartDataPoint
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants
import com.oterman.rundemo.ui.theme.RunTheme

/**
 * 配速图表卡片（合并版）
 * 将配速折线图 + 配速区间条形图合并到同一张卡片内
 * 对标 iOS SpeedChartAndZoneView
 *
 * 注意：配速 Y 轴反转（值越小=越快，显示在上方）
 */
@Composable
fun PaceChartCard(
    speedSeries: List<ChartDataPoint>,
    speedZones: List<AbilityZone>,
    avgSpeed: Double,
    maxSpeed: Double,
    modifier: Modifier = Modifier
) {
    if (speedSeries.isEmpty()) return

    // 过滤掉异常值（配速 > 20min/km 或 <= 0 视为异常）
    val filteredSeries = speedSeries.filter { it.value in 0.1..20.0 }
    if (filteredSeries.isEmpty()) return

    AppCard(
        modifier = modifier
            .padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RunDetailLayoutConstants.HeaderCardPadding.dp)
        ) {
            // ========== 配速折线图标题行 ==========
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏃 配速(min/km)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                // 平均/最快配速摘要
                Text(
                    text = "均${formatPaceValue(avgSpeed)}  最快${formatPaceValue(maxSpeed)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ========== 配速折线图（嵌入式，Y轴反转） ==========
            RunDataLineChartContent(
                dataPoints = filteredSeries,
                lineColor = RunTheme.colorScheme.chartPaceLine,
                avgValue = avgSpeed,
                invertYAxis = true,
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

            // ========== 分隔线 + 配速区间 ==========
            if (speedZones.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                AbilityZoneBar(zones = speedZones)
            }
        }
    }
}

/**
 * 格式化配速值 (min/km -> 5'30")
 */
private fun formatPaceValue(paceMinPerKm: Double): String {
    if (paceMinPerKm <= 0) return "-"
    val minutes = paceMinPerKm.toInt()
    val seconds = ((paceMinPerKm - minutes) * 60).toInt()
    return "${minutes}'${seconds.toString().padStart(2, '0')}\""
}
