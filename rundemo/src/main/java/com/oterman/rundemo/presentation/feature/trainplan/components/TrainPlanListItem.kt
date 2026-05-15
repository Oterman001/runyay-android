package com.oterman.rundemo.presentation.feature.trainplan.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.TrainPlan
import com.oterman.rundemo.domain.model.TrainPlanSummary
import com.oterman.rundemo.domain.model.TrainWholeType
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.presentation.feature.trainplan.formatDistance
import com.oterman.rundemo.presentation.feature.trainplan.formatDuration
import com.oterman.rundemo.presentation.feature.trainplan.distanceMeters
import com.oterman.rundemo.presentation.feature.trainplan.goalText
import com.oterman.rundemo.presentation.feature.trainplan.totalDistanceMeters
import com.oterman.rundemo.presentation.feature.trainplan.totalDurationSeconds

@Composable
fun TrainPlanListItem(
    plan: TrainPlanSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: TrainPlan? = null,
    isLoadingDetail: Boolean = false
) {
    val accent = plan.trainWholeType.accentColor()
    val metrics = buildPlanMetrics(plan, detail)

    StatisticsCard(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatTypeLabel(plan.trainWholeType),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(12.dp))

            if (isLoadingDetail) {
                LoadingMetrics()
            } else {
                CompactMetrics(metrics = metrics)
            }
        }
    }
}

@Composable
private fun CompactMetrics(metrics: List<PlanMetric>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        metrics.forEach { metric ->
            MetricText(metric = metric)
        }
    }
}

@Composable
private fun LoadingMetrics() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) {
            Column(
                modifier = Modifier
                    .width(52.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                )
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                )
            }
        }
    }
}

@Composable
private fun MetricText(metric: PlanMetric) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = metric.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = metric.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class PlanMetric(
    val label: String,
    val value: String
)

private fun formatTypeLabel(type: TrainWholeType): String = when (type) {
    TrainWholeType.SELF_DEFINE -> "自定义训练"
    TrainWholeType.DISTANCE -> "距离目标"
    TrainWholeType.TIME -> "时间目标"
    TrainWholeType.CALORIES -> "卡路里目标"
    TrainWholeType.PACER -> "配速目标"
}

@Composable
private fun TrainWholeType.accentColor(): Color = when (this) {
    TrainWholeType.SELF_DEFINE -> MaterialTheme.colorScheme.primary
    TrainWholeType.DISTANCE -> Color(0xFF2E7D32)
    TrainWholeType.TIME -> Color(0xFF1E88E5)
    TrainWholeType.CALORIES -> Color(0xFFE65100)
    TrainWholeType.PACER -> Color(0xFF7E57C2)
}

private fun buildPlanMetrics(plan: TrainPlanSummary, detail: TrainPlan?): List<PlanMetric> {
    if (detail == null) return listOf(PlanMetric("目标", "--"))

    return when (plan.trainWholeType) {
        TrainWholeType.DISTANCE -> listOfNotNull(
            detail.distanceGoalStep?.goalText()?.let { PlanMetric("距离", it) }
                ?: detail.blockList.totalDistanceMeters().takeIf { it > 0 }?.let { PlanMetric("距离", formatDistance(it)) },
            detail.blockList.totalDurationSeconds().takeIf { it > 0 }?.let { PlanMetric("时长", formatDuration(it)) }
        ).take(2)
        TrainWholeType.TIME -> listOfNotNull(
            detail.timeGoalStep?.goalText()?.let { PlanMetric("时长", it) }
                ?: detail.blockList.totalDurationSeconds().takeIf { it > 0 }?.let { PlanMetric("时长", formatDuration(it)) },
            detail.blockList.totalDistanceMeters().takeIf { it > 0 }?.let { PlanMetric("距离", formatDistance(it)) }
        ).take(2)
        TrainWholeType.CALORIES -> listOfNotNull(
            detail.calGoalStep?.goalText()?.let { PlanMetric("卡路里", it) }
        )
        TrainWholeType.PACER -> {
            val step = detail.pacerGoalStep
            listOfNotNull(
                step?.distanceMeters()?.takeIf { it > 0 }?.let { PlanMetric("距离", formatDistance(it)) },
                step?.timeGoalSeconds?.takeIf { it > 0 }?.let { PlanMetric("时长", formatDuration(it)) }
            )
        }
        TrainWholeType.SELF_DEFINE -> {
            val distance = detail.blockList.totalDistanceMeters()
            val duration = detail.blockList.totalDurationSeconds()
            listOfNotNull(
                distance.takeIf { it > 0 }?.let { PlanMetric("距离", formatDistance(it)) },
                duration.takeIf { it > 0 }?.let { PlanMetric("时长", formatDuration(it)) }
            )
        }
    }.ifEmpty { listOf(PlanMetric("目标", buildGoalSummaryText(detail) ?: "--")) }
}

private fun buildGoalSummaryText(detail: TrainPlan): String? = when (detail.trainWholeType) {
    TrainWholeType.DISTANCE -> detail.distanceGoalStep?.goalText()
        ?: detail.blockList.totalDistanceMeters().takeIf { it > 0 }?.let { formatDistance(it) }
    TrainWholeType.TIME -> detail.timeGoalStep?.goalText()
        ?: detail.blockList.totalDurationSeconds().takeIf { it > 0 }?.let { formatDuration(it) }
    TrainWholeType.CALORIES -> detail.calGoalStep?.goalText()
    TrainWholeType.PACER -> detail.pacerGoalStep?.goalText()
    TrainWholeType.SELF_DEFINE -> {
        val meters = detail.blockList.totalDistanceMeters()
        val secs = detail.blockList.totalDurationSeconds()
        when {
            meters > 0 -> formatDistance(meters)
            secs > 0 -> formatDuration(secs)
            else -> null
        }
    }
}
