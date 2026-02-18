package com.oterman.rundemo.presentation.feature.statistics.year.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.MonthRangeData
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Year summary header: "年跑步 X 次，Y.Z 公里"
 */
@Composable
internal fun YearSummaryHeader(
    runCount: Int,
    totalDistance: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "年跑步",
            fontSize = 14.sp,
            color = SecondaryTextColor
        )
        Text(
            text = "$runCount",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "次，",
            fontSize = 14.sp,
            color = SecondaryTextColor
        )
        Text(
            text = String.format("%.1f", totalDistance),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = RunTheme.colorScheme.blue
        )
        Text(
            text = "公里",
            fontSize = 14.sp,
            color = SecondaryTextColor
        )
    }
}

/**
 * 3x4 grid of mini month views
 */
@Composable
internal fun YearMonthsGrid(
    monthRangeDataList: List<MonthRangeData>,
    onMonthClick: (MonthRangeData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (rowIndex in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (colIndex in 0 until 3) {
                    val monthIndex = rowIndex * 3 + colIndex
                    if (monthIndex < monthRangeDataList.size) {
                        MiniMonthView(
                            monthRangeData = monthRangeDataList[monthIndex],
                            onClick = { onMonthClick(monthRangeDataList[monthIndex]) },
                            modifier = Modifier.weight(1f),
                            cellSpacing = 3.dp,
                            maxCellSize = 30.dp
                        )
                    }
                }
            }
        }
    }
}
