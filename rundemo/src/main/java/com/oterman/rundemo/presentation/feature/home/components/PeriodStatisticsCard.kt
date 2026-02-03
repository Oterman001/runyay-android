package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.PeriodStatistics
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Year/Month statistics card
 * Matches iOS CurYearTotalView / CurMonthTotalView
 */
@Composable
fun PeriodStatisticsCard(
    title: String,
    stats: PeriodStatistics,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
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

        Spacer(modifier = Modifier.height(15.dp))

        // Distance row
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Text(
                text = String.format("%.1f", stats.totalDistance),
                color = RunBlue,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "公里",
                color = SecondaryTextColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Duration row
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Text(
                text = String.format("%.1f", stats.totalDuration),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "小时",
                color = SecondaryTextColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
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
