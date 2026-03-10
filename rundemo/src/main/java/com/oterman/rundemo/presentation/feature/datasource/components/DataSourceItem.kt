package com.oterman.rundemo.presentation.feature.datasource.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.DataSourceInfo
import com.oterman.rundemo.presentation.components.AppCard

/**
 * 数据源列表项组件
 */
@Composable
fun DataSourceItem(
    dataSourceInfo: DataSourceInfo,
    isEditMode: Boolean = false,
    isLoading: Boolean = false,
    showPriority: Boolean = true,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isEnabled = dataSourceInfo.platform.isEnabled

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isLoading) { onClick() }
            .alpha(if (isEnabled) 1f else 0.6f),
        color = if (isDragging) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shadowElevation = if (isDragging) 8.dp else 0.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 优先级徽章
            if (showPriority && dataSourceInfo.platform.supportsSorting) {
                PriorityBadge(priority = dataSourceInfo.priority)
                Spacer(modifier = Modifier.width(12.dp))
            }

            // 图标
            Image(
                painter = painterResource(id = dataSourceInfo.platform.iconResId),
                contentDescription = dataSourceInfo.platform.displayName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 标题和状态
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dataSourceInfo.platform.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )

                // 在编辑模式下也显示授权状态（手动导入和苹果健康不显示）
                val hideAuthStatus = dataSourceInfo.platform == com.oterman.rundemo.domain.model.DataSourcePlatform.MANUAL ||
                        dataSourceInfo.platform == com.oterman.rundemo.domain.model.DataSourcePlatform.APPLE_HEALTH
                if (!hideAuthStatus && (!isEditMode || dataSourceInfo.platform.supportsSorting)) {
                    Text(
                        text = if (dataSourceInfo.isAuthorized) "已授权" else "未授权",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dataSourceInfo.isAuthorized) {
                            Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // 右侧内容
            if (isEditMode) {
                // 编辑模式：显示拖拽手柄，绑定拖拽手势
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "拖拽排序",
                    tint = if (isDragging) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .size(24.dp)
                        .then(dragHandleModifier)
                )
            } else {
                // 正常模式：显示加载或箭头
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (isEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * 优先级徽章
 */
@Composable
private fun PriorityBadge(priority: Int) {
    val gradient = when (priority) {
        1 -> Brush.linearGradient(
            colors = listOf(Color(0xFF2196F3), Color(0xFF00BCD4))
        )

        2 -> Brush.linearGradient(
            colors = listOf(Color(0xFF00BCD4), Color(0xFF2196F3).copy(alpha = 0.7f))
        )

        else -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF2196F3).copy(alpha = 0.7f),
                Color(0xFF2196F3).copy(alpha = 0.5f)
            )
        )
    }

    Box(
        modifier = Modifier
            .size(28.dp)
            .background(gradient, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = priority.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

