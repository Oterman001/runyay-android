package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.components.MetricTagChip
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants
import kotlin.math.abs

/**
 * VO2Max 展示卡片
 * 对标iOS VO2MaxCardView
 * 左侧显示数值+等级tag，右侧显示与上次的变化量
 */
@Composable
fun VO2MaxCard(
    vo2Max: Double,
    previousVo2Max: Double?,
    modifier: Modifier = Modifier
) {
    if (vo2Max <= 0) return

    AppCard(
        modifier = modifier
            .padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RunDetailLayoutConstants.HeaderCardPadding.dp)
        ) {
            // 标题
            Text(
                text = "最大摄氧量",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 数据行：左侧数值+tag，右侧delta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：数值 + 等级tag
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%.2f", vo2Max),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val grade = getVo2MaxGrade(vo2Max)
                    MetricTagChip(
                        text = grade.label,
                        color = grade.color
                    )
                }

                // 右侧：delta显示
                DeltaDisplay(previousVo2Max = previousVo2Max, currentVo2Max = vo2Max)
            }
        }
    }
}

/**
 * 变化量显示
 */
@Composable
private fun DeltaDisplay(
    previousVo2Max: Double?,
    currentVo2Max: Double
) {
    if (previousVo2Max == null) {
        Text(
            text = "首次记录",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        val change = currentVo2Max - previousVo2Max
        val (arrow, color) = when {
            change > 0 -> "↑" to Color(0xFF4CAF50)
            change < 0 -> "↓" to Color(0xFFF44336)
            else -> "→" to MaterialTheme.colorScheme.onSurfaceVariant
        }
        val text = if (change == 0.0) {
            "$arrow 无变化"
        } else {
            "$arrow ${String.format("%.2f", abs(change))}"
        }

        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

/**
 * VO2Max等级评价（简化版，男性30-39 ACSM标准）
 */
private data class Vo2MaxGrade(val label: String, val color: Color)

private fun getVo2MaxGrade(vo2Max: Double): Vo2MaxGrade {
    return when {
        vo2Max >= 52.0 -> Vo2MaxGrade("优秀", Color(0xFF4CAF50))
        vo2Max >= 43.0 -> Vo2MaxGrade("良好", Color(0xFF2196F3))
        vo2Max >= 34.0 -> Vo2MaxGrade("一般", Color(0xFFFF9800))
        else -> Vo2MaxGrade("待提升", Color(0xFFF44336))
    }
}
