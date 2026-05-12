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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.TrainPlan
import com.oterman.rundemo.domain.model.TrainPlanSummary
import com.oterman.rundemo.domain.model.TrainWholeType
import com.oterman.rundemo.presentation.feature.trainplan.formatDistance
import com.oterman.rundemo.presentation.feature.trainplan.formatDuration
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plan.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTypeLabel(plan.trainWholeType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (isLoadingDetail) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    )
                } else if (detail != null) {
                    val goalText = buildGoalSummaryText(detail)
                    if (goalText != null) {
                        Text(
                            text = goalText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        if (plan.finishFlag == "Y") {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "已完成",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatTypeLabel(type: TrainWholeType): String = when (type) {
    TrainWholeType.SELF_DEFINE -> "自定义训练"
    TrainWholeType.DISTANCE -> "距离目标"
    TrainWholeType.TIME -> "时间目标"
    TrainWholeType.CALORIES -> "卡路里目标"
    TrainWholeType.PACER -> "配速目标"
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
