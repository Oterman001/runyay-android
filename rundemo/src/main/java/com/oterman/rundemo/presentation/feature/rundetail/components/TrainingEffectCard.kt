package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants

/**
 * 训练效果卡片
 * 对标iOS TrainingEffectCardView
 * 显示有氧训练效果和无氧训练效果，各带一个进度环
 */
@Composable
fun TrainingEffectCard(
    aerobicEffect: Double,
    anaerobicEffect: Double,
    modifier: Modifier = Modifier
) {
    if (aerobicEffect <= 0 && anaerobicEffect <= 0) return

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
                text = "训练效果",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 有氧训练效果
                if (aerobicEffect > 0) {
                    TrainingEffectItem(
                        label = "有氧",
                        value = aerobicEffect,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 无氧训练效果
                if (anaerobicEffect > 0) {
                    TrainingEffectItem(
                        label = "无氧",
                        value = anaerobicEffect,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingEffectItem(
    label: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 进度环
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            TrainingEffectRing(
                progress = (value / 5.0f).toFloat().coerceIn(0f, 1f),
                color = color,
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = String.format("%.1f", value),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = getTrainingEffectDescription(value),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TrainingEffectRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val strokeWidth = 8.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val topLeft = Offset(
            (size.width - radius * 2) / 2,
            (size.height - radius * 2) / 2
        )

        // 背景轨道
        drawArc(
            color = trackColor,
            startAngle = -225f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 进度弧
        drawArc(
            color = color,
            startAngle = -225f,
            sweepAngle = 270f * progress,
            useCenter = false,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

private fun getTrainingEffectDescription(value: Double): String {
    return when {
        value < 1.0 -> "无效果"
        value < 2.0 -> "轻微"
        value < 3.0 -> "维持"
        value < 4.0 -> "提升"
        value < 5.0 -> "高度提升"
        else -> "过量"
    }
}

