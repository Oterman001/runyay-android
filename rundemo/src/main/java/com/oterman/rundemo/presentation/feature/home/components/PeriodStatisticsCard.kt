package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.GoalSettings
import com.oterman.rundemo.domain.model.GoalType
import com.oterman.rundemo.domain.model.PeriodStatistics
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Year/Month statistics card with goal support
 * Matches iOS CurYearTotalView / CurMonthTotalView
 *
 * Features:
 * - Shows distance and duration stats
 * - When goal enabled: shows goal value, progress bars
 * - When no goal: shows "Set Goal" button
 * - Dynamic coloring based on goal type
 */
@Composable
fun PeriodStatisticsCard(
    title: String,
    stats: PeriodStatistics,
    goalSettings: GoalSettings = GoalSettings(),
    isYearCard: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onSetGoalClick: () -> Unit = {}
) {
    val hasActiveGoal = if (isYearCard) {
        goalSettings.hasActiveYearGoal()
    } else {
        goalSettings.hasActiveMonthGoal()
    }

    StatisticsCard(
        modifier = modifier.clickable { onClick() }
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Run count
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = "跑步",
                    color = SecondaryTextColor,
                    fontSize = 12.sp
                )
                Text(
                    text = "${stats.runCount}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Text(
                    text = "次",
                    color = SecondaryTextColor,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stats display - order and size depends on goal type
        if (goalSettings.goalEnabled && goalSettings.goalType == GoalType.DURATION) {
            // Duration goal: show duration first (larger), then distance
            DurationRow(
                duration = stats.totalDuration,
                goal = stats.durationGoal,
                isPrimary = true,
                showGoal = hasActiveGoal
            )
            Spacer(modifier = Modifier.height(4.dp))
            DistanceRow(
                distance = stats.totalDistance,
                goal = stats.distanceGoal,
                isPrimary = false,
                showGoal = false
            )
        } else {
            // Distance goal or no goal: show distance first (larger)
            DistanceRow(
                distance = stats.totalDistance,
                goal = stats.distanceGoal,
                isPrimary = true,
                showGoal = hasActiveGoal && goalSettings.goalType == GoalType.DISTANCE
            )
            Spacer(modifier = Modifier.height(4.dp))
            DurationRow(
                duration = stats.totalDuration,
                goal = stats.durationGoal,
                isPrimary = false,
                showGoal = false
            )
        }

        // Goal progress section
        if (goalSettings.goalEnabled && hasActiveGoal) {
            // Show progress bars
            val goalProgress = if (goalSettings.goalType == GoalType.DISTANCE) {
                stats.getDistanceProgress()
            } else {
                stats.getDurationProgress()
            }
            val goalProgressText = if (goalSettings.goalType == GoalType.DISTANCE) {
                stats.getDistanceProgressPercent()
            } else {
                stats.getDurationProgressPercent()
            }

            GoalProgressView(
                goalProgress = goalProgress,
                goalProgressText = goalProgressText,
                timeProgress = stats.timeProgress,
                timeProgressText = stats.getTimeProgressPercent(),
                onClick = onSetGoalClick
            )
        } else if (!goalSettings.goalEnabled) {
            // Show "Set Goal" button
            Spacer(modifier = Modifier.height(10.dp))
            SetGoalButton(
                text = if (isYearCard) "设置年度目标" else "设置月度目标",
                onClick = onSetGoalClick
            )
        }
    }
}

@Composable
private fun DistanceRow(
    distance: Double,
    goal: Double,
    isPrimary: Boolean,
    showGoal: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isPrimary) RunBlue else MaterialTheme.colorScheme.onSurface
    val baseFontSize = if (isPrimary) 32.sp else 22.sp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val maxWidthPx = constraints.maxWidth.toFloat()

        // Estimate text width to avoid line wrapping
        val distanceText = String.format("%.1f", distance)
        val goalText = if (showGoal && goal > 0) " / ${goal.toInt()}" else ""
        val unitText = "公里"
        val totalChars = distanceText.length + goalText.length + unitText.length

        // Adjust font size based on content length
        val adjustedFontSize = with(density) {
            val estimatedWidth = baseFontSize.toPx() * totalChars * 0.6f
            if (estimatedWidth > maxWidthPx * 0.95f) {
                (baseFontSize.value * maxWidthPx * 0.95f / estimatedWidth).sp
            } else {
                baseFontSize
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = distanceText,
                color = textColor,
                fontSize = adjustedFontSize,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alignByBaseline()
            )

            if (showGoal && goal > 0) {
                Text(
                    text = " / ",
                    color = SecondaryTextColor,
                    fontSize = 14.sp,
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    text = "${goal.toInt()}",
                    color = SecondaryTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alignByBaseline()
                )
            }

            Text(
                text = unitText,
                color = SecondaryTextColor,
                fontSize = 12.sp,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = 2.dp)
            )
        }
    }
}

@Composable
private fun DurationRow(
    duration: Double,
    goal: Double,
    isPrimary: Boolean,
    showGoal: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isPrimary) RunBlue else MaterialTheme.colorScheme.onSurface
    val baseFontSize = if (isPrimary) 32.sp else 22.sp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val maxWidthPx = constraints.maxWidth.toFloat()

        // Estimate text width to avoid line wrapping
        val durationText = String.format("%.1f", duration)
        val goalText = if (showGoal && goal > 0) " / ${goal.toInt()}" else ""
        val unitText = "小时"
        val totalChars = durationText.length + goalText.length + unitText.length

        // Adjust font size based on content length
        val adjustedFontSize = with(density) {
            val estimatedWidth = baseFontSize.toPx() * totalChars * 0.6f
            if (estimatedWidth > maxWidthPx * 0.95f) {
                (baseFontSize.value * maxWidthPx * 0.95f / estimatedWidth).sp
            } else {
                baseFontSize
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = durationText,
                color = textColor,
                fontSize = adjustedFontSize,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alignByBaseline()
            )

            if (showGoal && goal > 0) {
                Text(
                    text = " / ",
                    color = SecondaryTextColor,
                    fontSize = 14.sp,
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    text = "${goal.toInt()}",
                    color = SecondaryTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alignByBaseline()
                )
            }

            Text(
                text = unitText,
                color = SecondaryTextColor,
                fontSize = 12.sp,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = 2.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PeriodStatisticsCardPreview() {
    PeriodStatisticsCard(
        title = "今年",
        stats = PeriodStatistics(
            runCount = 42,
            totalDistance = 356.8,
            totalDuration = 45.5
        )
    )
}

@Preview(showBackground = true, name = "With Distance Goal")
@Composable
private fun PeriodStatisticsCardWithDistanceGoalPreview() {
    PeriodStatisticsCard(
        title = "今年",
        stats = PeriodStatistics(
            runCount = 42,
            totalDistance = 356.8,
            totalDuration = 45.5,
            distanceGoal = 1000.0,
            timeProgress = 0.75f
        ),
        goalSettings = GoalSettings(
            goalEnabled = true,
            goalType = GoalType.DISTANCE,
            yearDistanceGoal = 1000.0
        )
    )
}

@Preview(showBackground = true, name = "With Duration Goal")
@Composable
private fun PeriodStatisticsCardWithDurationGoalPreview() {
    PeriodStatisticsCard(
        title = "本月",
        stats = PeriodStatistics(
            runCount = 8,
            totalDistance = 45.2,
            totalDuration = 6.5,
            durationGoal = 20.0,
            timeProgress = 0.6f
        ),
        goalSettings = GoalSettings(
            goalEnabled = true,
            goalType = GoalType.DURATION,
            monthDurationGoal = 20.0
        ),
        isYearCard = false
    )
}
