package com.oterman.rundemo.presentation.feature.trainplan.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.BlockType
import com.oterman.rundemo.domain.model.TrainBlock

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
    val blockLabel = when (block.blockType) {
        BlockType.WARMUP -> "热身"
        BlockType.MAIN -> "训练"
        BlockType.COOLDOWN -> "冷却"
    }
    val blockColor = when (block.blockType) {
        BlockType.WARMUP -> MaterialTheme.colorScheme.tertiary
        BlockType.MAIN -> MaterialTheme.colorScheme.primary
        BlockType.COOLDOWN -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, blockColor.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(
            containerColor = blockColor.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = blockLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = blockColor
                    )
                    if (block.blockType == BlockType.MAIN && onLoopCountChange != null) {
                        Spacer(Modifier.width(12.dp))
                        IconButton(
                            onClick = { onLoopCountChange(-1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, "减少", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${block.loopCnt}x",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { onLoopCountChange(1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, "增加", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                IconButton(onClick = onRemoveBlock, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "删除",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Steps
            block.stepList.forEachIndexed { index, step ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = blockColor.copy(alpha = 0.15f)
                    )
                }
                TrainStepRow(
                    step = step,
                    onClick = { onStepClick(index) },
                    onRemove = { onRemoveStep(index) }
                )
            }

            // Add step button
            Spacer(Modifier.height(4.dp))
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
