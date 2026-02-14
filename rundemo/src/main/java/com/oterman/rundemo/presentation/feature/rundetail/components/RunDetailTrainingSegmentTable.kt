package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * 训练分段表格
 * 展示间歇训练等自定义分段数据
 * 对标 iOS SegSelfDefineSimpleViewNew 的简版样式
 *
 * 每行显示：分段名称/类型标签、距离、配速、心率
 */
@Composable
fun RunDetailTrainingSegmentTable(
    segments: List<RunSegment>,
    modifier: Modifier = Modifier
) {
    if (segments.isEmpty()) return

    AppCard(
        modifier = modifier
            .padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RunDetailLayoutConstants.HeaderCardPadding.dp)
        ) {
            // 标题
            Text(
                text = "训练分段",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 表头
            TrainingSegmentTableHeader()

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // 分段数据行
            segments.forEachIndexed { index, segment ->
                TrainingSegmentRow(segment = segment)

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
 * 训练分段表头
 */
@Composable
private fun TrainingSegmentTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TrainingHeaderCell(text = "分段", modifier = Modifier.weight(1.2f))
        TrainingHeaderCell(text = "距离", modifier = Modifier.weight(1f))
        TrainingHeaderCell(text = "配速", modifier = Modifier.weight(1f))
        TrainingHeaderCell(text = "心率", modifier = Modifier.weight(0.8f))
    }
}

@Composable
private fun TrainingHeaderCell(
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
 * 训练分段数据行
 */
@Composable
private fun TrainingSegmentRow(
    segment: RunSegment
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分段名称 + 类型标签
        Row(
            modifier = Modifier.weight(1.2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // 间歇类型标签 (彩色小标签)
            IntervalTypeTag(intervalType = segment.intervalType)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = segment.getSegmentName(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        // 距离
        Text(
            text = segment.getFormattedDistance(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 配速
        Text(
            text = segment.getFormattedSpeed(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 心率
        Text(
            text = if (segment.averageHeartRate > 0) "${segment.averageHeartRate.toInt()}" else "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.8f)
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
    val (bgColor, textColor) = when (intervalType) {
        IntervalType.WARMUP -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        IntervalType.WORK -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        IntervalType.RECOVERY -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        IntervalType.COOLDOWN -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
        IntervalType.UNKNOWN -> Color(0xFFF5F5F5) to Color(0xFF616161)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = intervalType.displayName,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}

