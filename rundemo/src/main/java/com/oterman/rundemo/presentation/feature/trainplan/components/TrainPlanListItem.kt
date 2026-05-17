package com.oterman.rundemo.presentation.feature.trainplan.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.SendToMobile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.R
import com.oterman.rundemo.domain.model.TrainPlan
import com.oterman.rundemo.domain.model.TrainPlanSummary
import com.oterman.rundemo.domain.model.TrainWholeType
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.sentDevicePlatforms
import com.oterman.rundemo.domain.model.sourceLabel
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.presentation.feature.trainplan.distanceMeters
import com.oterman.rundemo.presentation.feature.trainplan.estimateDistance
import com.oterman.rundemo.presentation.feature.trainplan.estimateSelfDefine
import com.oterman.rundemo.presentation.feature.trainplan.estimateTime
import com.oterman.rundemo.presentation.feature.trainplan.formatDistance
import com.oterman.rundemo.presentation.feature.trainplan.formatDurationColon
import com.oterman.rundemo.presentation.feature.trainplan.goalText
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.StepTrainingColor

private val MetricCaloriesColor = Color(0xFFE53935)

@Composable
fun TrainPlanListItem(
    plan: TrainPlanSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: TrainPlan? = null
) {
    val metrics = buildPlanMetrics(plan, detail)
    val dimTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val sentPlatforms = detail?.sentDevicePlatforms() ?: plan.sentDevicePlatforms()
    val sourceLabel = detail?.sourceLabel() ?: plan.sourceLabel()

    StatisticsCard(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Title row: name + checkmark icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = plan.name,
                    modifier = Modifier.weight(1f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (plan.finishFlag == "Y") Color(0xFF34C759) else dimTint
                )
            }

            TrainPlanSourceTag(label = sourceLabel)

            TrainPlanSentPlatformTags(platforms = sentPlatforms)

            if (metrics.isNotEmpty()) {
                MetricsRow(metrics = metrics)
            }
        }
    }
}

@Composable
fun TrainPlanSourceTag(
    label: String?,
    modifier: Modifier = Modifier
) {
    if (label.isNullOrBlank()) return
    val tint = RunTheme.colorScheme.blue
    Row(
        modifier = modifier
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(tint.copy(alpha = 0.16f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AI",
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TrainPlanSentPlatformTags(
    platforms: List<DataSourcePlatform>,
    modifier: Modifier = Modifier,
    showEmptyState: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (platforms.isEmpty()) {
            if (showEmptyState) {
                SentPlatformTag(
                    label = "未发送到设备",
                    platform = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            platforms.forEach { platform ->
                SentPlatformTag(
                    label = platform.displayName,
                    platform = platform,
                    tint = when (platform) {
                        DataSourcePlatform.GARMIN_GLOBAL -> Color(0xFF00A9E0)
                        DataSourcePlatform.COROS -> StepTrainingColor
                        else -> RunTheme.colorScheme.blue
                    }
                )
            }
        }
    }
}

@Composable
private fun SentPlatformTag(
    label: String,
    platform: DataSourcePlatform?,
    tint: Color
) {
    Row(
        modifier = Modifier
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (platform == null) {
            Icon(
                imageVector = Icons.Outlined.SendToMobile,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = tint
            )
        } else {
            Image(
                painter = painterResource(platform.iconResId),
                contentDescription = null,
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
private fun MetricsRow(metrics: List<PlanMetric>) {
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        metrics.forEach { metric ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(metric.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = metric.tint
                )
                Text(
                    text = metric.value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}


private data class PlanMetric(
    val iconRes: Int,
    val tint: Color,
    val value: String
)

private fun buildPlanMetrics(plan: TrainPlanSummary, detail: TrainPlan?): List<PlanMetric> {
    if (detail == null) return emptyList()

    return when (plan.trainWholeType) {
        TrainWholeType.DISTANCE -> {
            val distM = detail.distanceGoalStep?.distanceMeters()
                ?: estimateSelfDefine(detail.blockList, vdot = null).distanceMeters ?: 0.0
            val estimate = estimateDistance(distM, vdot = null)
            listOfNotNull(
                distM.takeIf { it > 0 }?.let { distanceMetric(formatDistance(it)) },
                estimate.durationSeconds?.let { timeMetric(formatDurationColon(it)) }
            ).take(2)
        }
        TrainWholeType.TIME -> {
            val durSec = detail.timeGoalStep?.timeGoalSeconds
                ?: estimateSelfDefine(detail.blockList, vdot = null).durationSeconds ?: 0
            val estimate = estimateTime(durSec, vdot = null)
            listOfNotNull(
                durSec.takeIf { it > 0 }?.let { timeMetric(formatDurationColon(it)) },
                estimate.distanceMeters?.let { distanceMetric(formatDistance(it)) }
            ).take(2)
        }
        TrainWholeType.CALORIES -> listOfNotNull(
            detail.calGoalStep?.goalText()?.let { caloriesMetric(it) }
        )
        TrainWholeType.PACER -> {
            val step = detail.pacerGoalStep
            listOfNotNull(
                step?.distanceMeters()?.takeIf { it > 0 }?.let { distanceMetric(formatDistance(it)) },
                step?.timeGoalSeconds?.takeIf { it > 0 }?.let { timeMetric(formatDurationColon(it)) }
            )
        }
        TrainWholeType.SELF_DEFINE -> {
            val estimate = estimateSelfDefine(detail.blockList, vdot = null)
            listOfNotNull(
                estimate.distanceMeters?.let { distanceMetric(formatDistance(it)) },
                estimate.durationSeconds?.let { timeMetric(formatDurationColon(it)) }
            )
        }
    }
}

private fun distanceMetric(value: String) =
    PlanMetric(iconRes = R.drawable.ic_goal_distance, tint = Color(0xFF1AA9F8), value = value)

private fun timeMetric(value: String) =
    PlanMetric(iconRes = R.drawable.ic_goal_time, tint = StepTrainingColor, value = value)

private fun caloriesMetric(value: String) =
    PlanMetric(iconRes = R.drawable.ic_goal_calories, tint = MetricCaloriesColor, value = value)
