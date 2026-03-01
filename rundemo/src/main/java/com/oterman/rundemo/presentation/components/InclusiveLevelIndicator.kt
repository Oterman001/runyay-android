package com.oterman.rundemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.oterman.rundemo.ui.theme.RunTheme

/**
 * inclusiveLevel 圆形颜色标识
 * 0 → 灰色, 1 → 绿色, 2 → 主题蓝
 */
@Composable
fun InclusiveLevelIndicator(
    inclusiveLevel: Int,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp
) {
    val color = when (inclusiveLevel) {
        0 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        1 -> Color(0xFF4CAF50)
        else -> RunTheme.colorScheme.blue
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}
