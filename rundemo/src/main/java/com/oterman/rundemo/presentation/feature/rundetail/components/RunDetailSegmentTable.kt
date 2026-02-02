package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.RunSegment
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants

/**
 * 公里分段表格
 * 显示每公里的配速、心率、步频等数据
 */
@Composable
fun RunDetailSegmentTable(
    segments: List<RunSegment>,
    modifier: Modifier = Modifier
) {
    if (segments.isEmpty()) return

    Card(
        modifier = modifier
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
            // 标题
            Text(
                text = "公里配速",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 表头
            SegmentTableHeader()

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // 分段数据行
            segments.forEachIndexed { index, segment ->
                SegmentTableRow(
                    segment = segment,
                    isFastest = segment.isFastest
                )

                if (index < segments.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * 表头
 */
@Composable
private fun SegmentTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TableHeaderCell(text = "公里", modifier = Modifier.weight(1f))
        TableHeaderCell(text = "配速", modifier = Modifier.weight(1f))
        TableHeaderCell(text = "心率", modifier = Modifier.weight(1f))
        TableHeaderCell(text = "步频", modifier = Modifier.weight(1f))
    }
}

/**
 * 表头单元格
 */
@Composable
private fun TableHeaderCell(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

/**
 * 分段数据行
 */
@Composable
private fun SegmentTableRow(
    segment: RunSegment,
    isFastest: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFastest) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 公里数
        TableDataCell(
            text = "${segment.seq + 1}",
            modifier = Modifier.weight(1f),
            isFastest = isFastest
        )

        // 配速
        TableDataCell(
            text = segment.getFormattedSpeed(),
            modifier = Modifier.weight(1f),
            isFastest = isFastest
        )

        // 心率
        TableDataCell(
            text = if (segment.averageHeartRate > 0) "${segment.averageHeartRate.toInt()}" else "-",
            modifier = Modifier.weight(1f),
            isFastest = isFastest
        )

        // 步频
        TableDataCell(
            text = if (segment.averageCadence > 0) "${segment.averageCadence.toInt()}" else "-",
            modifier = Modifier.weight(1f),
            isFastest = isFastest
        )
    }
}

/**
 * 数据单元格
 */
@Composable
private fun TableDataCell(
    text: String,
    modifier: Modifier = Modifier,
    isFastest: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (isFastest) FontWeight.SemiBold else FontWeight.Normal,
        color = if (isFastest) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}
