package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants

/**
 * VO2Max / VDOT 展示卡片
 * 对标iOS的VO2Max展示区域
 */
@Composable
fun VO2MaxCard(
    vdot: Double,
    overallVdot: Double,
    modifier: Modifier = Modifier
) {
    if (vdot <= 0 && overallVdot <= 0) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp),
        shape = RoundedCornerShape(RunDetailLayoutConstants.HeaderCardRadius.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RunDetailLayoutConstants.HeaderCardPadding.dp)
        ) {
            Text(
                text = "VDOT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 本次VDOT
                if (vdot > 0) {
                    VdotValueItem(
                        label = "本次",
                        value = vdot,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Overall VDOT
                if (overallVdot > 0) {
                    VdotValueItem(
                        label = "综合",
                        value = overallVdot,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // VDOT 等级指示条
            if (vdot > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                VdotLevelBar(vdot = vdot)
            }
        }
    }
}

@Composable
private fun VdotValueItem(
    label: String,
    value: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format("%.1f", value),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * VDOT等级指示条
 * 30-85的范围，显示当前值的位置
 */
@Composable
private fun VdotLevelBar(
    vdot: Double,
    modifier: Modifier = Modifier
) {
    val minVdot = 30.0
    val maxVdot = 85.0
    val progress = ((vdot - minVdot) / (maxVdot - minVdot)).toFloat().coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        // 等级标签行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "初级",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "优秀",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "精英",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 渐变进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF66BB6A),
                            Color(0xFF42A5F5),
                            Color(0xFFAB47BC)
                        )
                    )
                )
        ) {
            // 当前值指示器
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(3.dp)
                        .height(12.dp)
                        .background(
                            Color.White,
                            RoundedCornerShape(1.5.dp)
                        )
                )
            }
        }
    }
}

