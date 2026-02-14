package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants
import com.oterman.rundemo.presentation.feature.rundetail.RunMetricItem

/**
 * 跑步详情数据网格
 * 3列布局显示各项指标
 */
@Composable
fun RunDetailDataGrid(
    metrics: List<RunMetricItem>,
    modifier: Modifier = Modifier
) {
    if (metrics.isEmpty()) return

    AppCard(
        modifier = modifier
            .padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RunDetailLayoutConstants.HeaderCardPadding.dp)
        ) {
            // 将指标分成每行3个
            val chunkedMetrics = metrics.chunked(3)

            chunkedMetrics.forEachIndexed { index, rowMetrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowMetrics.forEach { metric ->
                        RunMetricCell(
                            metric = metric,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 如果这一行不满3个，添加空白占位
                    repeat(3 - rowMetrics.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // 行间距
                if (index < chunkedMetrics.size - 1) {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * 单个指标单元格
 */
@Composable
private fun RunMetricCell(
    metric: RunMetricItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 数值 + 单位
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = metric.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            metric.unit?.let { unit ->
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 标签
        Text(
            text = metric.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
