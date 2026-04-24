package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.IntervalType
import com.oterman.rundemo.domain.model.MergedRunSegment
import com.oterman.rundemo.domain.model.RunSegment
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants
import com.oterman.rundemo.ui.theme.RunTheme

private enum class TrainingFilter(val label: String) {
    ALL("全部"),
    WORK("训练"),
    RECOVERY("恢复")
}

@Composable
private fun getIntervalColor(intervalType: IntervalType): Color {
    val colors = RunTheme.colorScheme
    return when (intervalType) {
        IntervalType.WARMUP -> colors.segmentWarmup
        IntervalType.WORK -> colors.segmentWork
        IntervalType.RECOVERY -> colors.segmentRecovery
        IntervalType.COOLDOWN -> colors.segmentCooldown
        IntervalType.UNKNOWN -> colors.segmentUnknown
    }
}

/**
 * 训练分段表格
 * 对标 iOS MergedSegmentGridView
 * 7列: #, 类型, 距离, 耗时, 配速, 心率, 步频
 * 带筛选按钮（全部/训练/恢复）+ 合并行展开折叠
 */
@Composable
fun RunDetailTrainingSegmentTable(
    segments: List<RunSegment>,
    mergedSegments: List<MergedRunSegment>,
    expandedSegmentIds: Set<String>,
    onToggleExpansion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (segments.isEmpty()) return

    var selectedFilter by remember { mutableStateOf(TrainingFilter.ALL) }

    val filteredMergedSegments = when (selectedFilter) {
        TrainingFilter.ALL -> mergedSegments
        TrainingFilter.WORK -> mergedSegments.filter { it.intervalType == IntervalType.WORK }
        TrainingFilter.RECOVERY -> mergedSegments.filter { it.intervalType == IntervalType.RECOVERY }
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
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 筛选按钮行
            FilterButtonsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 表头
            TrainingSegmentTableHeader(
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 合并分段数据行
            filteredMergedSegments.forEachIndexed { index, mergedSegment ->
                MergedSegmentRow(
                    mergedSegment = mergedSegment,
                    index = index,
                    isExpanded = expandedSegmentIds.contains(mergedSegment.id),
                    onToggle = { onToggleExpansion(mergedSegment.id) }
                )

                // 展开的子分段行
                if (expandedSegmentIds.contains(mergedSegment.id) && mergedSegment.isMerged) {
                    mergedSegment.subSegments.forEachIndexed { subIndex, subSegment ->
                        SubSegmentRow(
                            segment = subSegment,
                            subIndex = subIndex,
                            parentDisplayName = mergedSegment.getDisplayName()
                        )
                    }
                }
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
                        if (isSelected) RunTheme.colorScheme.chartPaceLine.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 15.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) RunTheme.colorScheme.chartPaceLine
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
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // # 列（窄）
        Text(
            text = "#",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.5f)
        )
        // 类型, 距离, 耗时, 配速, 心率, 步频
        listOf("类型", "距离", "耗时", "配速", "心率", "步频").forEach { header ->
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
private fun MergedSegmentRow(
    mergedSegment: MergedRunSegment,
    index: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val bgColor = if (index % 2 == 0) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .then(
                if (mergedSegment.isMerged) Modifier.clickable { onToggle() }
                else Modifier
            )
            .padding(horizontal = 4.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // # 列 - 序号 + chevron
        Box(
            modifier = Modifier.weight(0.5f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (mergedSegment.isMerged) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown
                    else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 8.dp),
                    tint = RunTheme.colorScheme.chartPaceLine
                )
            }
        }

        // 类型列
        Text(
            text = mergedSegment.getDisplayName(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = getIntervalColor(mergedSegment.intervalType),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // 距离
        Text(
            text = mergedSegment.getFormattedDistance(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 耗时
        Text(
            text = mergedSegment.getFormattedDuration(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 配速
        Text(
            text = mergedSegment.getFormattedSpeed(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 心率
        Text(
            text = mergedSegment.getFormattedHeartRate(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 步频
        Text(
            text = mergedSegment.getFormattedCadence(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SubSegmentRow(
    segment: RunSegment,
    subIndex: Int,
    parentDisplayName: String
) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
    val textAlpha = 0.8f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 4.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // # 列 - 缩进 + 子序号
        Box(
            modifier = Modifier.weight(0.5f),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "${subIndex + 1}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha),
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        // 类型列
        Text(
            text = parentDisplayName,
            fontSize = 11.sp,
            color = getIntervalColor(segment.intervalType).copy(alpha = textAlpha),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // 距离
        Text(
            text = segment.getFormattedDistance(),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 耗时
        Text(
            text = segment.getFormattedDuration(),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 配速 - 使用 getComputedPace()
        Text(
            text = segment.getComputedPace(),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 心率
        Text(
            text = if (segment.averageHeartRate > 0) "${segment.averageHeartRate.toInt()}" else "-",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 步频
        Text(
            text = if (segment.averageCadence > 0) "${segment.averageCadence.toInt()}" else "-",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
    }
}
