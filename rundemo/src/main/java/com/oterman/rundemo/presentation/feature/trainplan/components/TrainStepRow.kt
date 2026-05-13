package com.oterman.rundemo.presentation.feature.trainplan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.IntensityType
import com.oterman.rundemo.domain.model.TrainGoalType
import com.oterman.rundemo.domain.model.TrainStep
import com.oterman.rundemo.presentation.feature.trainplan.canMoveLikeIos
import com.oterman.rundemo.presentation.feature.trainplan.canRemoveLikeIos
import com.oterman.rundemo.presentation.feature.trainplan.displayName
import com.oterman.rundemo.presentation.feature.trainplan.goalText
import com.oterman.rundemo.presentation.feature.trainplan.intensityText
import com.oterman.rundemo.ui.theme.RunTheme

@Composable
fun TrainStepRow(
    step: TrainStep,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    isEditMode: Boolean,
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val accent = when {
        step.warmupFlag == "Y" -> RunTheme.colorScheme.orange
        step.cooldownFlag == "Y" -> MaterialTheme.colorScheme.tertiary
        step.purpose.equals("RECOVERY", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
        else -> RunTheme.colorScheme.blue
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RunTheme.colorScheme.cardBg, RoundedCornerShape(12.dp))
            .clickable(enabled = isEditMode, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = stepIcon(step),
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .background(accent.copy(alpha = 0.12f), CircleShape)
                .padding(5.dp),
            tint = accent
        )
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = step.displayName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                if (isEditMode && step.canMoveLikeIos()) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "拖动",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = dragHandleModifier.size(18.dp)
                    )
                }
                if (isEditMode && step.canRemoveLikeIos()) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (step.skipStatus == 1) {
                Text(
                    text = "跳过",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricText(
                        icon = goalIcon(step.goalType),
                        value = step.goalText(),
                        tint = accent,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    MetricText(
                        icon = intensityIcon(step.intensityType),
                        value = step.intensityText() ?: "自由练",
                        tint = accent,
                        muted = step.intensityType == null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricText(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
    muted: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else tint
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
    }
}

private fun stepIcon(step: TrainStep) = when {
    step.intensityType == IntensityType.SPEED -> Icons.Outlined.Speed
    step.goalType == TrainGoalType.TIME -> Icons.Outlined.Timer
    step.goalType == TrainGoalType.CALORIES -> Icons.Outlined.FavoriteBorder
    else -> Icons.Outlined.DirectionsRun
}

private fun goalIcon(type: TrainGoalType) = when (type) {
    TrainGoalType.DISTANCE -> Icons.Outlined.DirectionsRun
    TrainGoalType.TIME -> Icons.Outlined.Timer
    TrainGoalType.CALORIES -> Icons.Outlined.FavoriteBorder
    TrainGoalType.PACER -> Icons.Outlined.Speed
    TrainGoalType.OPEN -> Icons.Outlined.DirectionsRun
}

private fun intensityIcon(type: IntensityType?) = when (type) {
    IntensityType.HEART_RATE -> Icons.Outlined.FavoriteBorder
    IntensityType.SPEED -> Icons.Outlined.Speed
    null -> Icons.Outlined.DirectionsRun
}
