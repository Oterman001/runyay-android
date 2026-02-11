package com.oterman.rundemo.presentation.feature.home.tabs.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.DataTabDisplayMode
import com.oterman.rundemo.domain.model.MonthRangeData

/**
 * 月份折叠区块
 * 对应 iOS 的 DisclosureGroup
 */
@Composable
fun MonthSection(
    monthData: MonthRangeData,
    isExpanded: Boolean,
    displayMode: DataTabDisplayMode,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    // 展开箭头旋转动画
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Column(modifier = modifier) {
        // 月份头部(可点击展开/折叠)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 根据显示模式选择不同的头部组件
            Box(modifier = Modifier.weight(1f)) {
                when (displayMode) {
                    DataTabDisplayMode.HEATMAP -> HeatmapMonthHeader(monthData = monthData)
                    DataTabDisplayMode.SIMPLE -> SimpleMonthHeader(monthData = monthData)
                }
            }

            // 展开/折叠箭头
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "收起" else "展开",
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotationAngle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 可折叠的内容区域
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                content()
            }
        }

        // 分隔线
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}
