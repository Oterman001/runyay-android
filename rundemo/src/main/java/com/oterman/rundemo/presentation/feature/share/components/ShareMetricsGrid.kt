package com.oterman.rundemo.presentation.feature.share.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.presentation.feature.share.ShareMetricType
import com.oterman.rundemo.ui.theme.RunYayFontFamily4

/**
 * 短图指标网格（2x3 或动态列数）
 * 显示用户选择的指标数据
 */
@Composable
fun ShareMetricsGrid(
    record: RunRecordEntity,
    selectedMetrics: List<ShareMetricType>,
    modifier: Modifier = Modifier
) {
    val columns = 3
    val chunked = selectedMetrics.chunked(columns)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        chunked.forEachIndexed { index, rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowMetrics.forEach { metricType ->
                    ShareMetricCell(
                        metricType = metricType,
                        record = record,
                        modifier = Modifier.weight(1f)
                    )
                }
                // 填充空位
                repeat(columns - rowMetrics.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            if (index < chunked.size - 1) {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ShareMetricCell(
    metricType: ShareMetricType,
    record: RunRecordEntity,
    modifier: Modifier = Modifier
) {
    val (value, unit) = getMetricValueAndUnit(metricType, record)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = RunYayFontFamily4
            )
            unit?.let {
                Text(
                    text = " $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = metricType.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 根据指标类型从 RunRecordEntity 中提取值
 */
private fun getMetricValueAndUnit(
    type: ShareMetricType,
    record: RunRecordEntity
): Pair<String, String?> {
    return when (type) {
        ShareMetricType.DURATION -> {
            val totalSeconds = (record.activeDuration * 60).toInt()
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            val value = if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
            else String.format("%d:%02d", minutes, seconds)
            value to null
        }
        ShareMetricType.VDOT -> {
            val value = if (record.vdot > 0) String.format("%.1f", record.vdot) else "-"
            value to null
        }
        ShareMetricType.PACE -> {
            val pace = if (record.averageSpeed > 0) record.averageSpeed
            else if (record.totalDistance > 0 && record.activeDuration > 0)
                record.activeDuration / record.totalDistance else 0.0
            val value = if (pace > 0 && pace <= 30) {
                val m = pace.toInt()
                val s = ((pace - m) * 60).toInt()
                "${m}'${s.toString().padStart(2, '0')}\""
            } else "-"
            value to type.unit
        }
        ShareMetricType.TRAINING_LOAD -> {
            val value = if (record.trainingLoad > 0) String.format("%.0f", record.trainingLoad) else "-"
            value to type.unit
        }
        ShareMetricType.AVG_HEART_RATE -> {
            val value = if (record.averageHeartRate > 0) record.averageHeartRate.toInt().toString() else "-"
            value to type.unit
        }
        ShareMetricType.MAX_HEART_RATE -> {
            val value = if (record.maxHeartRate > 0) record.maxHeartRate.toInt().toString() else "-"
            value to type.unit
        }
        ShareMetricType.AVG_STRIDE_LENGTH -> {
            val value = if (record.averageStrideLength > 0) String.format("%.0f", record.averageStrideLength) else "-"
            value to type.unit
        }
        ShareMetricType.AVG_CADENCE -> {
            val value = if (record.averageCadence > 0) record.averageCadence.toInt().toString() else "-"
            value to type.unit
        }
        ShareMetricType.ELEVATION -> {
            val value = if (record.elevationAscended > 0) String.format("%.0f", record.elevationAscended) else "-"
            value to type.unit
        }
        ShareMetricType.VERTICAL_STRIDE_RATIO -> {
            val value = if (record.averageStrideLength > 0 && record.averageVerticalOscillation > 0) {
                String.format("%.1f", record.averageVerticalOscillation / record.averageStrideLength * 100)
            } else "-"
            value to type.unit
        }
        ShareMetricType.CALORIES -> {
            val value = if (record.totalCalories > 0) String.format("%.0f", record.totalCalories) else "-"
            value to type.unit
        }
        ShareMetricType.AVG_POWER -> {
            val value = if (record.averagePower > 0) record.averagePower.toInt().toString() else "-"
            value to type.unit
        }
        ShareMetricType.DISTANCE -> {
            String.format("%.2f", record.totalDistance) to type.unit
        }
    }
}
