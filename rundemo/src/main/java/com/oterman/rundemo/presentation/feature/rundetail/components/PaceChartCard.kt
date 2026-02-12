package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.AbilityZone
import com.oterman.rundemo.domain.model.ChartDataPoint
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants

/**
 * 配速图表卡片
 * 组合折线图 + 配速区间条形图
 * 对标iOS SpeedChartAndZoneView
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

    val minPace = filteredSeries.minOf { it.value }
    val maxPace = filteredSeries.maxOf { it.value }

    // 配速折线图（Y轴反转）
    RunDataLineChart(
        title = "配速",
        dataPoints = filteredSeries,
        lineColor = Color(0xFF1E88E5),
        unit = "/km",
        avgValue = avgSpeed,
        maxValue = maxPace,
        minValue = minPace,
        invertYAxis = true,
        modifier = modifier
    )

    // 配速区间条形图
    if (speedZones.isNotEmpty()) {
        Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp),
            shape = RoundedCornerShape(RunDetailLayoutConstants.HeaderCardRadius.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RunDetailLayoutConstants.HeaderCardPadding.dp)
            ) {
                Text(
                    text = "配速区间",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                AbilityZoneBar(
                    zones = speedZones,
                    title = ""
                )
            }
        }
    }
}

