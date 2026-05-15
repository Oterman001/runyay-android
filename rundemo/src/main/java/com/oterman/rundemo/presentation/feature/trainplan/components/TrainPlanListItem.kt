package com.oterman.rundemo.presentation.feature.trainplan.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.R
import com.oterman.rundemo.domain.model.TrainPlan
import com.oterman.rundemo.domain.model.TrainPlanSummary
import com.oterman.rundemo.domain.model.TrainWholeType
import com.oterman.rundemo.presentation.feature.trainplan.formatDistance
import com.oterman.rundemo.presentation.feature.trainplan.formatDuration
import com.oterman.rundemo.presentation.feature.trainplan.formatPace
import com.oterman.rundemo.presentation.feature.trainplan.distanceMeters
import com.oterman.rundemo.presentation.feature.trainplan.goalText
import com.oterman.rundemo.presentation.feature.trainplan.totalDistanceMeters
import com.oterman.rundemo.presentation.feature.trainplan.totalDurationSeconds
import com.oterman.rundemo.ui.theme.RunTheme

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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
        color = RunTheme.colorScheme.cardBg,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = accent
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelChip(text = formatTypeLabel(plan.trainWholeType), accent = accent)
                        LabelChip(text = plan.locationType?.let(::formatLocationLabel) ?: "场地待定")
                        LabelChip(text = plan.hardLevel?.let { "强度 $it" } ?: "强度待定")
                    }
                }
                Spacer(Modifier.width(10.dp))
                StatusPill(isFinished = plan.finishFlag == "Y")
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            Spacer(Modifier.height(12.dp))

            if (isLoadingDetail) {
                LoadingMetrics()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    metrics.forEach { metric ->
                        MetricBlock(
                            metric = metric,
                            accent = accent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            val description = detail?.description?.takeIf { it.isNotBlank() }
            if (description != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LabelChip(
    text: String,
    accent: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = accent,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

@Composable
private fun StatusPill(isFinished: Boolean) {
    val bg = if (isFinished) RunTheme.colorScheme.successContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isFinished) RunTheme.colorScheme.success else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isFinished) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = fg
            )
        }
        Text(
            text = if (isFinished) "已完成" else "未完成",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1
        )
    }
}

@Composable
private fun MetricBlock(
    metric: PlanMetric,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = metric.icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = accent
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
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

@Composable
private fun LoadingMetrics() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                )
            }
        }
    }
}

private data class PlanMetric(
    val label: String,
    val value: String,
    val iconRes: Int
)

private val PlanMetric.icon: Painter
    @Composable get() = painterResource(iconRes)

private fun formatTypeLabel(type: TrainWholeType): String = when (type) {
    TrainWholeType.SELF_DEFINE -> "自定义训练"
    TrainWholeType.DISTANCE -> "距离目标"
    TrainWholeType.TIME -> "时间目标"
    TrainWholeType.CALORIES -> "卡路里目标"
    TrainWholeType.PACER -> "配速目标"
}

private fun formatLocationLabel(value: String): String = when {
    value.equals("outdoor", ignoreCase = true) -> "室外"
    value.equals("indoor", ignoreCase = true) -> "室内"
    else -> "场地待定"
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
    val fallbackGoal = detail?.let(::buildGoalSummaryText) ?: "--"
    return when (plan.trainWholeType) {
        TrainWholeType.DISTANCE -> listOf(
            PlanMetric("目标", detail?.distanceGoalStep?.goalText() ?: fallbackGoal, R.drawable.ic_goal_distance),
            PlanMetric("预计", detail?.blockList?.totalDurationSeconds()?.takeIf { it > 0 }?.let { formatDuration(it) } ?: "--", R.drawable.ic_goal_time),
            PlanMetric("类型", "距离", R.drawable.ic_step_training)
        )
        TrainWholeType.TIME -> listOf(
            PlanMetric("目标", detail?.timeGoalStep?.goalText() ?: fallbackGoal, R.drawable.ic_goal_time),
            PlanMetric("预计", detail?.blockList?.totalDistanceMeters()?.takeIf { it > 0 }?.let { formatDistance(it) } ?: "--", R.drawable.ic_goal_distance),
            PlanMetric("类型", "时间", R.drawable.ic_step_training)
        )
        TrainWholeType.CALORIES -> listOf(
            PlanMetric("目标", detail?.calGoalStep?.goalText() ?: fallbackGoal, R.drawable.ic_goal_calories),
            PlanMetric("类型", "卡路里", R.drawable.ic_goal_calories),
            PlanMetric("状态", if (plan.finishFlag == "Y") "完成" else "待训练", R.drawable.ic_step_training)
        )
        TrainWholeType.PACER -> {
            val step = detail?.pacerGoalStep
            listOf(
                PlanMetric("距离", step?.distanceMeters()?.takeIf { it > 0 }?.let { formatDistance(it) } ?: fallbackGoal, R.drawable.ic_goal_distance),
                PlanMetric("时间", step?.timeGoalSeconds?.takeIf { it > 0 }?.let { formatDuration(it) } ?: "--", R.drawable.ic_goal_time),
                PlanMetric("配速", step?.minPace?.let { formatPace(it) } ?: "--", R.drawable.ic_intensity_pace)
            )
        }
        TrainWholeType.SELF_DEFINE -> {
            val blocks = detail?.blockList.orEmpty()
            val distance = blocks.totalDistanceMeters()
            val duration = blocks.totalDurationSeconds()
            listOf(
                PlanMetric("距离", distance.takeIf { it > 0 }?.let { formatDistance(it) } ?: fallbackGoal, R.drawable.ic_goal_distance),
                PlanMetric("时长", duration.takeIf { it > 0 }?.let { formatDuration(it) } ?: "--", R.drawable.ic_goal_time),
                PlanMetric("结构", "${blocks.size} 组", R.drawable.ic_step_training)
            )
        }
    }
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
