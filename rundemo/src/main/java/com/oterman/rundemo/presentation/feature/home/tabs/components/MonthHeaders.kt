package com.oterman.rundemo.presentation.feature.home.tabs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.MonthRangeData
import com.oterman.rundemo.presentation.feature.statistics.year.components.MiniMonthHeatmapGrid

/**
 * 热力图模式月份头部
 * 对应 iOS HeatmapMonthView
 *
 * 布局: 左侧显示总距离+日期，右侧显示迷你热力日历
 */
@Composable
fun HeatmapMonthHeader(
    monthData: MonthRangeData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 左侧信息区域
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            // 总距离
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = monthData.getFormattedDistance(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "公里",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 格式化日期 (2025.10)
            Text(
                text = "${monthData.year}.${String.format("%02d", monthData.month)}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 右侧热力图区域 - 固定宽度避免过大
        Box(
            modifier = Modifier
                .width(120.dp)
                .padding(start = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            MiniMonthHeatmapGrid(
                dailyRecords = monthData.dailyRecords,
                cellSpacing = 5.dp,  // DataTab场景：较大间距
                maxCellSize = 12.dp  // DataTab场景：限制格子大小
            )
        }
    }
}

/**
 * 简单模式月份头部
 * 对应 iOS SimpleRunMonthView
 *
 * 布局: 左侧显示月份+年份，右侧显示总距离+跑步次数+时长
 */
@Composable
fun SimpleMonthHeader(
    monthData: MonthRangeData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 左侧: 月份 + 年份
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "${monthData.month}月",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${monthData.year}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 右侧: 总距离 + 跑步次数 + 时长
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = monthData.getFormattedDistance(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "公里",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "跑步${monthData.runCount}次，${monthData.getFormattedDuration()}小时",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
