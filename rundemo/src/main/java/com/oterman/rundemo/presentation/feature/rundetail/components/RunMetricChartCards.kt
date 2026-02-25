package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.ChartDataPoint
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants

private val AltitudeLineColor = Color(0xFF8D6E63)

/**
 * 海拔折线图卡片
 * 展示海拔折线图 + 累计爬升/下降 + 平均/最高/最低海拔
 * 对标 iOS createAltitudeLineChart
 */
@Composable
fun AltitudeChartCard(
    altitudeSeries: List<ChartDataPoint>,
    elevationAscended: Double = 0.0,
    modifier: Modifier = Modifier
) {
    if (altitudeSeries.isEmpty()) return

    val minAltitude = remember(altitudeSeries) { altitudeSeries.minOf { it.value } }
    val maxAltitude = remember(altitudeSeries) { altitudeSeries.maxOf { it.value } }
    val avgAltitude = remember(altitudeSeries) { altitudeSeries.map { it.value }.average() }
    val elevationDescended = remember(altitudeSeries) {
        var descent = 0.0
        for (i in 1 until altitudeSeries.size) {
            val diff = altitudeSeries[i - 1].value - altitudeSeries[i].value
            if (diff > 0) descent += diff
        }
        descent
    }

    AppCard(
        modifier = modifier
            .padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RunDetailLayoutConstants.HeaderCardPadding.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⛰ 海拔(m)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "↑${formatAltitudeValue(elevationAscended)}m  ↓${formatAltitudeValue(elevationDescended)}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "均${formatAltitudeValue(avgAltitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "最高${formatAltitudeValue(maxAltitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "最低${formatAltitudeValue(minAltitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            RunDataLineChartContent(
                dataPoints = altitudeSeries,
                lineColor = AltitudeLineColor,
                avgValue = avgAltitude,
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
        }
    }
}

private fun formatAltitudeValue(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value)
    }
}

/**
 * 步幅折线图卡片
 * 对标iOS步幅图表
 */
@Composable
fun StrideLengthChartCard(
    strideLengthSeries: List<ChartDataPoint>,
    avgStrideLength: Double,
    modifier: Modifier = Modifier
) {
    if (strideLengthSeries.isEmpty()) return

    RunDataLineChart(
        title = "步幅",
        dataPoints = strideLengthSeries,
        lineColor = Color(0xFF7E57C2),
        unit = "cm",
        avgValue = avgStrideLength,
        modifier = modifier
    )
}

/**
 * 步频散点/折线图卡片
 * 对标iOS步频图表
 */
@Composable
fun CadenceChartCard(
    cadenceSeries: List<ChartDataPoint>,
    avgCadence: Double,
    modifier: Modifier = Modifier
) {
    if (cadenceSeries.isEmpty()) return

    RunDataLineChart(
        title = "步频",
        dataPoints = cadenceSeries,
        lineColor = Color(0xFF26A69A),
        unit = "spm",
        avgValue = avgCadence,
        modifier = modifier
    )
}

/**
 * 触地时间折线图卡片
 * 对标iOS触地时间图表
 */
@Composable
fun ContactTimeChartCard(
    contactTimeSeries: List<ChartDataPoint>,
    avgContactTime: Double,
    modifier: Modifier = Modifier
) {
    if (contactTimeSeries.isEmpty()) return

    RunDataLineChart(
        title = "触地时间",
        dataPoints = contactTimeSeries,
        lineColor = Color(0xFFFF7043),
        unit = "ms",
        avgValue = avgContactTime,
        modifier = modifier
    )
}

/**
 * 垂直振幅折线图卡片
 * 对标iOS垂直振幅图表
 */
@Composable
fun VerticalOscillationChartCard(
    verticalOscillationSeries: List<ChartDataPoint>,
    avgVerticalOscillation: Double,
    modifier: Modifier = Modifier
) {
    if (verticalOscillationSeries.isEmpty()) return

    RunDataLineChart(
        title = "垂直振幅",
        dataPoints = verticalOscillationSeries,
        lineColor = Color(0xFF5C6BC0),
        unit = "cm",
        avgValue = avgVerticalOscillation,
        modifier = modifier
    )
}

/**
 * 功率折线图卡片
 * 对标iOS功率图表
 */
@Composable
fun PowerChartCard(
    powerSeries: List<ChartDataPoint>,
    avgPower: Double,
    modifier: Modifier = Modifier
) {
    if (powerSeries.isEmpty()) return

    RunDataLineChart(
        title = "功率",
        dataPoints = powerSeries,
        lineColor = Color(0xFFEF5350),
        unit = "W",
        avgValue = avgPower,
        modifier = modifier
    )
}

