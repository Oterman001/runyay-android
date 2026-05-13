package com.oterman.rundemo.presentation.feature.trainplan.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.R
import com.oterman.rundemo.domain.model.BlockType
import com.oterman.rundemo.domain.model.TrainBlock
import com.oterman.rundemo.presentation.feature.trainplan.displayName
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.StepCooldownColor
import com.oterman.rundemo.ui.theme.StepRecoveryColor
import com.oterman.rundemo.ui.theme.StepTrainingColor
import com.oterman.rundemo.ui.theme.StepWarmupColor

@Composable
fun TrainBlockCard(
    block: TrainBlock,
    blockIndex: Int,
    onRemoveBlock: () -> Unit,
    onAddStep: () -> Unit,
    onStepClick: (stepIndex: Int) -> Unit,
    onRemoveStep: (stepIndex: Int) -> Unit,
    isEditMode: Boolean,
    dragHandleModifier: Modifier = Modifier,
    onLoopCountChange: ((delta: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = blockAccent(block)
    val isSimpleSingleStep = block.stepList.size == 1 && block.loopCnt <= 1

    if (isSimpleSingleStep) {
        block.stepList.forEachIndexed { index, step ->
            TrainStepRow(
                step = step,
                onClick = { onStepClick(index) },
                onRemove = { onRemoveStep(index) },
                isEditMode = isEditMode,
                dragHandleModifier = dragHandleModifier,
                modifier = modifier.padding(horizontal = 20.dp)
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .dashedBorder(accent.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BlockHeader(
            block = block,
            accent = accent,
            onRemoveBlock = onRemoveBlock,
            isEditMode = isEditMode,
            onLoopCountChange = if (isEditMode && block.blockType == BlockType.MAIN) onLoopCountChange else null
        )

        block.stepList.forEachIndexed { index, step ->
            TrainStepRow(
                step = step,
                onClick = { onStepClick(index) },
                onRemove = { onRemoveStep(index) },
                isEditMode = isEditMode,
                dragHandleModifier = dragHandleModifier
            )
        }

        if (isEditMode) {
            TextButton(
                onClick = onAddStep,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加步骤")
            }
        }
    }
}

private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    val strokeWidth = 1.5.dp.toPx()
    drawRoundRect(
        color = color,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx()),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()), 0f)
        )
    )
}

@Composable
private fun BlockHeader(
    block: TrainBlock,
    accent: Color,
    onRemoveBlock: () -> Unit,
    isEditMode: Boolean,
    onLoopCountChange: ((delta: Int) -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onLoopCountChange != null) {
                LoopCounter(
                    loopCnt = block.loopCnt,
                    accent = accent,
                    onLoopCountChange = onLoopCountChange
                )
            } else {
                Icon(
                    painter = blockPainter(block),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = blockTitle(block),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (isEditMode && block.blockType == BlockType.MAIN) {
            IconButton(onClick = onRemoveBlock, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除分段",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LoopCounter(
    loopCnt: Int,
    accent: Color,
    onLoopCountChange: (delta: Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(accent.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .padding(horizontal = 4.dp)
    ) {
        IconButton(onClick = { onLoopCountChange(-1) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "减少循环", modifier = Modifier.size(16.dp), tint = accent)
        }
        Text(
            text = "${loopCnt.coerceAtLeast(1)}x",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = accent
        )
        IconButton(onClick = { onLoopCountChange(1) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Add, contentDescription = "增加循环", modifier = Modifier.size(16.dp), tint = accent)
        }
    }
}

@Composable
private fun blockAccent(block: TrainBlock): Color = when {
    block.blockType == BlockType.WARMUP -> StepWarmupColor
    block.blockType == BlockType.COOLDOWN -> StepCooldownColor
    block.stepList.firstOrNull()?.purpose.equals("RECOVERY", ignoreCase = true) -> StepRecoveryColor
    block.loopCnt > 1 -> StepTrainingColor
    else -> StepTrainingColor
}

private fun blockTitle(block: TrainBlock): String = when {
    block.blockType == BlockType.MAIN && block.loopCnt > 1 -> "循环"
    block.blockType == BlockType.MAIN && block.stepList.firstOrNull()?.purpose.equals("RECOVERY", ignoreCase = true) -> "恢复"
    else -> block.blockType.displayName(block.loopCnt)
}

@Composable
private fun blockPainter(block: TrainBlock) = painterResource(
    when {
        block.blockType == BlockType.WARMUP -> R.drawable.ic_step_warmup
        block.blockType == BlockType.COOLDOWN -> R.drawable.ic_step_cooldown
        block.stepList.firstOrNull()?.purpose.equals("RECOVERY", ignoreCase = true) -> R.drawable.ic_step_recovery
        else -> R.drawable.ic_step_training
    }
)
