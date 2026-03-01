package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.RunOrange
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Goal progress view showing dual progress bars
 * - Goal achievement progress (blue)
 * - Time elapsed progress (orange)
 *
 * Matches iOS CurYearTotalView/CurMonthTotalView progress section
 */
@Composable
fun GoalProgressView(
    goalProgress: Float,
    goalProgressText: String,
    timeProgress: Float,
    timeProgressText: String,
    goalLabel: String = "目标完成",
    timeLabel: String = "时间已过",
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Goal progress bar
        ProgressBarWithLabel(
            label = goalLabel,
            progressText = goalProgressText,
            progress = goalProgress,
            progressColor = RunTheme.colorScheme.blue
        )

        // Time elapsed progress bar
        ProgressBarWithLabel(
            label = timeLabel,
            progressText = timeProgressText,
            progress = timeProgress,
            progressColor = RunOrange
        )
    }
}

@Composable
private fun ProgressBarWithLabel(
    label: String,
    progressText: String,
    progress: Float,
    progressColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Label row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = SecondaryTextColor,
                fontSize = 11.sp
            )
            Text(
                text = progressText,
                color = progressColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(progressColor)
            )
        }
    }
}

/**
 * Set goal button shown when no goal is configured
 */
@Composable
fun SetGoalButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RunTheme.colorScheme.orange.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = RunTheme.colorScheme.orange,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalProgressViewPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        GoalProgressView(
            goalProgress = 0.45f,
            goalProgressText = "45.0%",
            timeProgress = 0.75f,
            timeProgressText = "75.0%"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SetGoalButtonPreview() {
    SetGoalButton(text = "设置年度目标")
}
