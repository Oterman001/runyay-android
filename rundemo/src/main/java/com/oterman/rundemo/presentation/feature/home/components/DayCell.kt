package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.ui.theme.NoDataBg
import com.oterman.rundemo.ui.theme.NoDataBgDark
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Day cell for week grid
 * Matches iOS DayBigCellView
 */
@Composable
fun DayCell(
    dayData: DayRunData,
    modifier: Modifier = Modifier,
    fullColorThreshold: Double = 5.0,  // km for full color
    onClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val cellShape = RoundedCornerShape(8.dp)

    // Calculate background color based on distance
    val backgroundColor = when {
        dayData.isFuture -> Color.Transparent
        dayData.totalDistance <= 0 -> if (isDark) NoDataBgDark else NoDataBg
        dayData.totalDistance >= fullColorThreshold -> {
            if (dayData.isIndoor) Color(0xFF8B5CF6) else RunBlue
        }
        else -> {
            val intensity = (dayData.totalDistance / fullColorThreshold).coerceIn(0.0, 1.0)
            val baseColor = if (dayData.isIndoor) Color(0xFF8B5CF6) else RunBlue
            val minAlpha = if (isDark) 0.3f else 0.2f
            baseColor.copy(alpha = (minAlpha + intensity * (1f - minAlpha)).toFloat())
        }
    }

    val textColor = when {
        dayData.totalDistance > 0 -> Color.White
        dayData.isFuture -> SecondaryTextColor
        else -> SecondaryTextColor
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(enabled = dayData.hasRun) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(cellShape)
                .background(backgroundColor)
                .then(
                    if (dayData.isFuture) {
                        Modifier.border(
                            width = 1.5.dp,
                            color = RunBlue.copy(alpha = 0.4f),
                            shape = cellShape
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayData.getFormattedDistance(),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (dayData.isToday) FontWeight.SemiBold else FontWeight.Normal
            )

            // Badge for multiple runs
            if (dayData.runCount >= 2) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(14.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${dayData.runCount}",
                        color = Color.Red,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Weekday label
        Text(
            text = dayData.dayOfWeek,
            fontSize = 11.sp,
            color = if (dayData.isToday) MaterialTheme.colorScheme.primary else SecondaryTextColor,
            fontWeight = if (dayData.isToday) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DayCellPreview() {
    DayCell(
        dayData = DayRunData(
            dayOfWeek = "一",
            totalDistance = 5.2,
            runCount = 1,
            isToday = false,
            isFuture = false
        )
    )
}
