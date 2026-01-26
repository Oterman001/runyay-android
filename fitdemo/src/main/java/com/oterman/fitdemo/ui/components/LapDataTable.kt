package com.oterman.fitdemo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oterman.fitdemo.data.model.LapData

/**
 * 区间数据表格组件
 */
@Composable
fun LapDataTable(
    laps: List<LapData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 表格标题
        SectionHeader("区间数据")
        Text(
            text = "共 ${laps.size} 个区间",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 可横向滚动的表格
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                )
        ) {
            Column {
                // 表头
                TableHeader()
                
                // 数据行
                laps.forEach { lap ->
                    TableRow(lap)
                }
            }
        }
    }
}

/**
 * 表头
 */
@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        TableCell("区间", 50.dp, isHeader = true)
        TableCell("距离", 90.dp, isHeader = true)
        TableCell("用时", 80.dp, isHeader = true)
        TableCell("配速", 90.dp, isHeader = true)
        TableCell("心率", 60.dp, isHeader = true)
        TableCell("步频", 60.dp, isHeader = true)
        TableCell("卡路里", 70.dp, isHeader = true)
    }
}

/**
 * 数据行
 */
@Composable
private fun TableRow(lap: LapData) {
    Row(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        TableCell(lap.lapNumber.toString(), 50.dp)
        TableCell(lap.totalDistance ?: "-", 90.dp)
        TableCell(lap.totalElapsedTime ?: "-", 80.dp)
        TableCell(lap.avgPace ?: "-", 90.dp)
        TableCell(lap.avgHeartRate ?: "-", 60.dp)
        TableCell(lap.avgCadence ?: "-", 60.dp)
        TableCell(lap.totalCalories ?: "-", 70.dp)
    }
}

/**
 * 表格单元格
 */
@Composable
private fun TableCell(
    text: String,
    width: Dp,
    isHeader: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(width)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if (isHeader) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.bodySmall
            },
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            color = if (isHeader) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            maxLines = if (isHeader) 1 else 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

