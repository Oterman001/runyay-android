package com.oterman.rundemo.presentation.feature.trainplan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.BlockType
import com.oterman.rundemo.domain.model.TrainBlock
import com.oterman.rundemo.presentation.feature.trainplan.displayName
import com.oterman.rundemo.ui.theme.RunTheme

@Composable
fun TrainBlockCard(
    block: TrainBlock,
    blockIndex: Int,
    onRemoveBlock: () -> Unit,
    onAddStep: () -> Unit,
    onStepClick: (stepIndex: Int) -> Unit,
    onRemoveStep: (stepIndex: Int) -> Unit,
    onLoopCountChange: ((delta: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = blockAccent(block)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(RunTheme.colorScheme.cardBg, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = blockIcon(block),
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
                if (block.blockType == BlockType.MAIN && onLoopCountChange != null) {
                    Spacer(Modifier.width(14.dp))
                    LoopCounter(
                        loopCnt = block.loopCnt,
                        accent = accent,
                        onLoopCountChange = onLoopCountChange
                    )
                }
            }
            IconButton(onClick = onRemoveBlock, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除分段",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        block.stepList.forEachIndexed { index, step ->
            TrainStepRow(
                step = step,
                onClick = { onStepClick(index) },
                onRemove = { onRemoveStep(index) }
            )
        }

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
    block.blockType == BlockType.WARMUP -> RunTheme.colorScheme.orange
    block.blockType == BlockType.COOLDOWN -> MaterialTheme.colorScheme.tertiary
    block.stepList.firstOrNull()?.purpose.equals("RECOVERY", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
    block.loopCnt > 1 -> MaterialTheme.colorScheme.secondary
    else -> RunTheme.colorScheme.blue
}

private fun blockTitle(block: TrainBlock): String = when {
    block.blockType == BlockType.MAIN && block.loopCnt > 1 -> "循环"
    block.blockType == BlockType.MAIN && block.stepList.firstOrNull()?.purpose.equals("RECOVERY", ignoreCase = true) -> "恢复"
    else -> block.blockType.displayName(block.loopCnt)
}

private fun blockIcon(block: TrainBlock) = when {
    block.blockType == BlockType.WARMUP -> Icons.Outlined.KeyboardDoubleArrowUp
    block.blockType == BlockType.COOLDOWN -> Icons.Outlined.KeyboardDoubleArrowDown
    block.loopCnt > 1 -> Icons.Outlined.Cached
    block.stepList.firstOrNull()?.purpose.equals("RECOVERY", ignoreCase = true) -> Icons.Outlined.KeyboardDoubleArrowDown
    else -> Icons.Outlined.KeyboardDoubleArrowUp
}
