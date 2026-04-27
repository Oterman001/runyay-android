package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.StreakStats
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.RunYayFontFamily4
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Streak cards showing consecutive running days / weeks.
 * Designed as half-width pair (Row + weight(1f)), matching YEAR_MONTH layout.
 */
@Composable
fun StreakDayCard(
    streakStats: StreakStats,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    StreakStatCard(
        title = "连续天数",
        currentValue = streakStats.currentDayStreak,
        bestValue = streakStats.bestDayStreak,
        unit = "天",
        accentColor = RunTheme.colorScheme.blue,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
fun StreakWeekCard(
    streakStats: StreakStats,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    StreakStatCard(
        title = "连续周数",
        currentValue = streakStats.currentWeekStreak,
        bestValue = streakStats.bestWeekStreak,
        unit = "周",
        accentColor = RunTheme.colorScheme.orange,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
private fun StreakStatCard(
    title: String,
    currentValue: Int,
    bestValue: Int,
    unit: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    StatisticsCard(modifier = modifier.clickable { onClick() }) {
        // Title
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Current streak value — centered with number + unit baseline-aligned
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Text(
                text = currentValue.toString(),
                fontSize = 38.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                fontFamily = RunYayFontFamily4,
                maxLines = 1,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = unit,
                fontSize = 14.sp,
                color = SecondaryTextColor,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = 3.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Best streak record — centered, baseline-aligned, symmetric spacing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Text(
                text = "最长",
                fontSize = 12.sp,
                color = SecondaryTextColor,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = bestValue.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = RunYayFontFamily4,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(horizontal = 4.dp)
            )
            Text(
                text = unit,
                fontSize = 12.sp,
                color = SecondaryTextColor,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}
