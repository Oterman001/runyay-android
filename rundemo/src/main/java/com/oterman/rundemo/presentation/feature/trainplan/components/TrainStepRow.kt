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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.R
import com.oterman.rundemo.domain.model.IntensityType
import com.oterman.rundemo.domain.model.TrainGoalType
import com.oterman.rundemo.domain.model.TrainStep
import com.oterman.rundemo.presentation.feature.trainplan.canMoveLikeIos
import com.oterman.rundemo.presentation.feature.trainplan.canRemoveLikeIos
import com.oterman.rundemo.presentation.feature.trainplan.displayName
import com.oterman.rundemo.presentation.feature.trainplan.goalText
import com.oterman.rundemo.presentation.feature.trainplan.intensityText
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.StepCooldownColor
import com.oterman.rundemo.ui.theme.StepRecoveryColor
import com.oterman.rundemo.ui.theme.StepTrainingColor
import com.oterman.rundemo.ui.theme.StepWarmupColor

@Composable
fun TrainStepRow(
    step: TrainStep,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    isEditMode: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = stepAccent(step)
    val isTrainingOrRecovery = step.warmupFlag != "Y" && step.cooldownFlag != "Y"
    // 训练/恢复图标略大，视觉上更粗重；热身/放松图标保持标准大小
    val purposeIconSize = if (isTrainingOrRecovery) 18.dp else 16.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RunTheme.colorScheme.cardBg, RoundedCornerShape(12.dp))
            .clickable(enabled = isEditMode, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 目的图标：添加 top padding 使其与第一行文字视觉对齐
        // 第一行右侧有 28dp 的 IconButton，文字在其内部垂直居中，偏移约 6dp
        Icon(
            painter = stepPurposePainter(step),
            contentDescription = null,
            modifier = Modifier
                .size(purposeIconSize)
                .padding(top = 6.dp),
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
                // 拖动图标仅作为视觉提示，实际手势由外层 modifier 承载
                if (isEditMode && step.canMoveLikeIos()) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "拖动",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
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
                    // 目标靠左
                    MetricText(
                        painter = goalPainter(step.goalType),
                        value = step.goalText(),
                        tint = accent
                    )
                    // 强度靠右，与上方删除/拖动按钮右对齐
                    MetricText(
                        painter = intensityPainter(step.intensityType),
                        value = step.intensityText() ?: "自由练",
                        tint = accent,
                        showIcon = step.intensityType != null,
                        muted = step.intensityType == null
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricText(
    painter: Painter,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    muted: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon) {
            Icon(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else tint
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
    }
}

private fun stepAccent(step: TrainStep): Color = when {
    step.warmupFlag == "Y" -> StepWarmupColor
    step.cooldownFlag == "Y" -> StepCooldownColor
    step.purpose.equals("RECOVERY", ignoreCase = true) -> StepRecoveryColor
    else -> StepTrainingColor
}

@Composable
private fun stepPurposePainter(step: TrainStep): Painter = painterResource(
    when {
        step.warmupFlag == "Y" -> R.drawable.ic_step_warmup
        step.cooldownFlag == "Y" -> R.drawable.ic_step_cooldown
        step.purpose.equals("RECOVERY", ignoreCase = true) -> R.drawable.ic_step_recovery
        else -> R.drawable.ic_step_training
    }
)

@Composable
private fun goalPainter(type: TrainGoalType): Painter = painterResource(
    when (type) {
        TrainGoalType.DISTANCE, TrainGoalType.OPEN -> R.drawable.ic_goal_distance
        TrainGoalType.TIME -> R.drawable.ic_goal_time
        TrainGoalType.CALORIES -> R.drawable.ic_goal_calories
        TrainGoalType.PACER -> R.drawable.ic_intensity_pace
    }
)

@Composable
private fun intensityPainter(type: IntensityType?): Painter = painterResource(
    when (type) {
        IntensityType.HEART_RATE -> R.drawable.ic_intensity_heartrate
        IntensityType.SPEED -> R.drawable.ic_intensity_pace
        IntensityType.NONE -> R.drawable.ic_step_training
        null -> R.drawable.ic_step_training
    }
)
