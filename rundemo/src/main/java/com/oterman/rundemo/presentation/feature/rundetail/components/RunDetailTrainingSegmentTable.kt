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
import com.oterman.rundemo.domain.model.IntervalType
import com.oterman.rundemo.domain.model.RunSegment
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants

private enum class TrainingFilter(val label: String) {
    ALL("全部"),
    WORK("训练"),
    RECOVERY("恢复")
}

/**
 * 训练分段表格
 * 对标 iOS MergedSegmentGridView
 * 6列: 序号, 距离, 耗时, 配速, 心率, 步频
 * 带筛选按钮（全部/训练/恢复）
 */
@Composable
fun RunDetailTrainingSegmentTable(
    segments: List<RunSegment>,
    modifier: Modifier = Modifier
) {
    if (segments.isEmpty()) return

    var selectedFilter by remember { mutableStateOf(TrainingFilter.ALL) }

    val filteredSegments = when (selectedFilter) {
        TrainingFilter.ALL -> segments
        TrainingFilter.WORK -> segments.filter { it.intervalType == IntervalType.WORK }
        TrainingFilter.RECOVERY -> segments.filter { it.intervalType == IntervalType.RECOVERY }
    }

    AppCard(
        modifier = modifier
            .padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RunDetailLayoutConstants.HeaderCardPadding.dp)
        ) {
            // 标题
            Text(
                text = "训练分段",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = RunDetailLayoutConstants.HeaderCardPadding.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 筛选按钮行
            FilterButtonsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                modifier = Modifier.padding(horizontal = RunDetailLayoutConstants.HeaderCardPadding.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 表头
            TrainingSegmentTableHeader(
                modifier = Modifier.padding(horizontal = RunDetailLayoutConstants.HeaderCardPadding.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 分段数据行
            filteredSegments.forEachIndexed { index, segment ->
                TrainingSegmentRow(
                    segment = segment,
                    index = index
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun FilterButtonsRow(
    selectedFilter: TrainingFilter,
    onFilterSelected: (TrainingFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start
    ) {
        TrainingFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        if (isSelected) Color(0xFF1E88E5).copy(alpha = 0.3f)
                        else Color.LightGray.copy(alpha = 0.3f)
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 15.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF1E88E5)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun TrainingSegmentTableHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        val headers = listOf("序号", "距离", "耗时", "配速", "心率", "步频")
        headers.forEach { header ->
            Text(
                text = header,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrainingSegmentRow(
    segment: RunSegment,
    index: Int
) {
    val bgColor = if (index % 2 == 0) {
        Color.LightGray.copy(alpha = 0.1f)
    } else {
        Color.Gray.copy(alpha = 0.01f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = RunDetailLayoutConstants.HeaderCardPadding.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 序号 + interval type tag
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IntervalTypeTag(intervalType = segment.intervalType)
            }
        }

        // 距离
        Text(
            text = segment.getFormattedDistance(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 耗时
        Text(
            text = segment.getFormattedDuration(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 配速
        Text(
            text = segment.getFormattedSpeed(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 心率
        Text(
            text = if (segment.averageHeartRate > 0) "${segment.averageHeartRate.toInt()}" else "-",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 步频
        Text(
            text = if (segment.averageCadence > 0) "${segment.averageCadence.toInt()}" else "-",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 间歇类型标签（小彩色标签）
 */
@Composable
private fun IntervalTypeTag(
    intervalType: IntervalType
) {
    val textColor = when (intervalType) {
        IntervalType.WARMUP -> Color(0xFFE65100)
        IntervalType.WORK -> Color(0xFF1565C0)
        IntervalType.RECOVERY -> Color(0xFF2E7D32)
        IntervalType.COOLDOWN -> Color(0xFF6A1B9A)
        IntervalType.UNKNOWN -> Color(0xFF616161)
    }

    Text(
        text = intervalType.displayName,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = textColor
    )
}
