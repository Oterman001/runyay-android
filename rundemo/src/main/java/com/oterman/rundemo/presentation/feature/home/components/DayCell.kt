package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.ui.theme.DayCellBadgeBg
import com.oterman.rundemo.ui.theme.DayCellBadgeText
import com.oterman.rundemo.ui.theme.NoDataBg
import com.oterman.rundemo.ui.theme.NoDataBgDark
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Shared day heatmap box for week and month views.
 * Renders a colored square based on run distance with optional multi-run badge.
 *
 * All colors use centralized theme constants from Color.kt for easy global adjustment.
 *
 * @param dayData The day's run data
 * @param maxCellSize Maximum cell width/height to constrain size on wide screens.
 *        Use Dp.Unspecified (default) to let parent control sizing (e.g. via weight).
 * @param fullColorThreshold Distance (km) at which the cell reaches full color intensity
 * @param cornerRadius Corner radius of the heatmap square
 * @param fontSize Font size of the distance text inside the cell
 * @param borderWidth Border width for future-day cells
 * @param badgeSize Badge size for multi-run indicator (count < 10)
 * @param badgeLargeSize Badge size for multi-run indicator (count >= 10)
 * @param badgeFontSize Badge font size (count < 10)
 * @param badgeLargeFontSize Badge font size (count >= 10)
 * @param badgeOffsetX Horizontal offset for badge position
 * @param badgeOffsetY Vertical offset for badge position
 */
@Composable
fun DayHeatmapBox(
    dayData: DayRunData,
    modifier: Modifier = Modifier,
    maxCellSize: Dp = 64.dp,
    fullColorThreshold: Double = 5.0,
    cornerRadius: Dp = 8.dp,
    fontSize: TextUnit = 11.sp,
    borderWidth: Dp = 1.5.dp,
    badgeSize: Dp = 16.dp,
    badgeLargeSize: Dp = 18.dp,
    badgeFontSize: TextUnit = 10.sp,
    badgeLargeFontSize: TextUnit = 9.sp,
    badgeOffsetX: Dp = 4.dp,
    badgeOffsetY: Dp = (-4).dp
) {
    val isDark = RunTheme.isDark
    val cellShape = RoundedCornerShape(cornerRadius)
    val futureBorderColor = RunTheme.colorScheme.dayCellActive.copy(alpha = 0.4f)

    // Unified color calculation - all use RunTheme.colorScheme.dayCellActive (RunTheme.colorScheme.blue), no indoor/outdoor distinction
    val backgroundColor = when {
        dayData.isFuture -> Color.Transparent
        dayData.totalDistance <= 0 -> if (isDark) NoDataBgDark else NoDataBg
        dayData.totalDistance >= fullColorThreshold -> RunTheme.colorScheme.dayCellActive
        else -> {
            val intensity = (dayData.totalDistance / fullColorThreshold).coerceIn(0.0, 1.0)
            val minAlpha = if (isDark) 0.3f else 0.2f
            RunTheme.colorScheme.dayCellActive.copy(alpha = (minAlpha + intensity * (1f - minAlpha)).toFloat())
        }
    }

    val textColor = when {
        dayData.totalDistance > 0 -> Color.White
        else -> SecondaryTextColor
    }

    // Outer Box - allows badge to overflow. Apply max size constraint only when specified.
    Box(
        modifier = modifier
            .then(
                if (maxCellSize != Dp.Unspecified) {
                    Modifier.sizeIn(maxWidth = maxCellSize, maxHeight = maxCellSize)
                } else {
                    Modifier
                }
            )
            .aspectRatio(1f)
    ) {
        // Inner Box with clip - rounded corner heatmap background
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(cellShape)
                .background(backgroundColor)
                .then(
                    if (dayData.isFuture) {
                        Modifier.drawBehind {
                            val strokeWidth = borderWidth.toPx()
                            val dashLength = 4.dp.toPx()
                            val gapLength = 3.dp.toPx()
                            drawRoundRect(
                                color = futureBorderColor,
                                cornerRadius = CornerRadius(cornerRadius.toPx()),
                                style = Stroke(
                                    width = strokeWidth,
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(dashLength, gapLength),
                                        phase = 0f
                                    )
                                )
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayData.getFormattedDistance(),
                color = textColor,
                fontSize = fontSize,
                fontWeight = if (dayData.isToday) FontWeight.SemiBold else FontWeight.Normal
            )
        }

        // Badge for multiple runs - outside clip
        if (dayData.runCount >= 2) {
            val actualBadgeSize = if (dayData.runCount >= 10) badgeLargeSize else badgeSize
            val actualBadgeFontSize = if (dayData.runCount >= 10) badgeLargeFontSize else badgeFontSize
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = badgeOffsetX, y = badgeOffsetY)
                    .size(actualBadgeSize)
                    .background(DayCellBadgeBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${dayData.runCount}",
                    color = DayCellBadgeText,
                    fontSize = actualBadgeFontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = actualBadgeFontSize
                )
            }
        }
    }
}

/**
 * Day cell for week grid with weekday label below the heatmap box.
 * Used in HomeTab's WeekStatisticsCard and WeekStatisticsContent's WeekSummaryAndGrid.
 * Matches iOS DayBigCellView.
 */
@Composable
fun DayCell(
    dayData: DayRunData,
    modifier: Modifier = Modifier,
    fullColorThreshold: Double = 5.0,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(enabled = dayData.hasRun) { onClick() }
    ) {
        DayHeatmapBox(
            dayData = dayData,
            fullColorThreshold = fullColorThreshold
        )

        // Weekday label - uses RunTheme.colorScheme.blue for today, not MaterialTheme.colorScheme.primary
        Text(
            text = dayData.dayOfWeek,
            fontSize = 11.sp,
            color = if (dayData.isToday) RunTheme.colorScheme.blue else SecondaryTextColor,
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

@Preview(showBackground = true)
@Composable
private fun DayHeatmapBoxPreview() {
    DayHeatmapBox(
        dayData = DayRunData(
            dayOfWeek = "三",
            totalDistance = 3.5,
            runCount = 2,
            isToday = true,
            isFuture = false
        )
    )
}
