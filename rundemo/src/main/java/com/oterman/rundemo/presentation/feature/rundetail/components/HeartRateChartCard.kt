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
 * 心率图表卡片
 * 组合折线图 + 心率区间条形图
 * 对标iOS HeartRateAndZoneView
 */
@Composable
fun HeartRateChartCard(
    heartRateSeries: List<ChartDataPoint>,
    heartRateZones: List<AbilityZone>,
    avgHeartRate: Double,
    maxHeartRate: Double,
    minHeartRate: Double,
    modifier: Modifier = Modifier
) {
    if (heartRateSeries.isEmpty()) return

    // 心率折线图
    RunDataLineChart(
        title = "心率",
        dataPoints = heartRateSeries,
        lineColor = Color(0xFFE53935),
        unit = "bpm",
        avgValue = avgHeartRate,
        maxValue = maxHeartRate,
        minValue = minHeartRate,
        modifier = modifier
    )

    // 心率区间条形图
    if (heartRateZones.isNotEmpty()) {
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
                    text = "心率区间",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                AbilityZoneBar(
                    zones = heartRateZones,
                    title = ""
                )
            }
        }
    }
}

