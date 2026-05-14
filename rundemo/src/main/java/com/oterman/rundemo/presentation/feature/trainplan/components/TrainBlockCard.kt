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
import androidx.compose.material.icons.filled.DragHandle
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
import com.oterman.rundemo.presentation.feature.trainplan.canMoveLikeIos
import com.oterman.rundemo.presentation.feature.trainplan.displayName
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.StepCooldownColor
import com.oterman.rundemo.ui.theme.StepTrainingColor
import com.oterman.rundemo.ui.theme.StepRecoveryColor
import com.oterman.rundemo.ui.theme.StepTrainingColor
import com.oterman.rundemo.ui.theme.StepWarmupColor
import androidx.compose.runtime.key
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun TrainBlockCard(
    block: TrainBlock,
    blockIndex: Int,
    onRemoveBlock: () -> Unit,
    onAddStep: () -> Unit,
    onStepClick: (stepIndex: Int) -> Unit,
    onRemoveStep: (stepIndex: Int) -> Unit,
    onMoveStep: ((fromIndex: Int, toIndex: Int) -> Unit)? = null,
    isEditMode: Boolean,
    blockDragHandleModifier: Modifier = Modifier,
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
                // 单 Step Block：整行是 Block 级拖动热区
                modifier = if (isEditMode && step.canMoveLikeIos()) {
                    modifier.padding(horizontal = 20.dp).then(blockDragHandleModifier)
                } else {
                    modifier.padding(horizontal = 20.dp)
                }
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
            blockDragHandleModifier = blockDragHandleModifier,
            onLoopCountChange = if (isEditMode && block.blockType == BlockType.MAIN) onLoopCountChange else null
        )

        if (isEditMode && onMoveStep != null && block.stepList.size > 1) {
            ReorderableColumn(
                list = block.stepList,
                onSettle = { fromIndex, toIndex -> onMoveStep(fromIndex, toIndex) },
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) { index, step, _ ->
                key(step.stepId ?: "step_${block.blockId}_$index") {
                    TrainStepRow(
                        step = step,
                        onClick = { onStepClick(index) },
                        onRemove = { onRemoveStep(index) },
                        isEditMode = isEditMode,
                        // 整行是 Step 级拖动热区
                        modifier = if (step.canMoveLikeIos()) Modifier.longPressDraggableHandle() else Modifier
                    )
                }
            }
        } else {
            block.stepList.forEachIndexed { index, step ->
                TrainStepRow(
                    step = step,
                    onClick = { onStepClick(index) },
                    onRemove = { onRemoveStep(index) },
                    isEditMode = isEditMode
                )
            }
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
    blockDragHandleModifier: Modifier = Modifier,
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
        // 右侧操作区：多 Step 时显示 Block 拖动手柄，MAIN Block 显示删除
        // padding(end=14dp) 使按钮与 TrainStepRow 内 padding(14dp) 对齐
        Row(
            modifier = Modifier.padding(end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode && block.stepList.size > 1 && block.blockType == BlockType.MAIN) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "拖动整段",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = blockDragHandleModifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            if (isEditMode && block.blockType == BlockType.MAIN) {
                IconButton(onClick = onRemoveBlock, modifier = Modifier.size(28.dp)) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "重复 ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${loopCnt.coerceAtLeast(1)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = RunTheme.colorScheme.blue
            )
            Text(
                text = " 次",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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
    block.stepList.size > 1 -> RunTheme.colorScheme.blue
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
