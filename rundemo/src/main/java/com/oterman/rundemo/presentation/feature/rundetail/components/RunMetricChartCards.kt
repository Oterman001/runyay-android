package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.oterman.rundemo.domain.model.ChartDataPoint

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

