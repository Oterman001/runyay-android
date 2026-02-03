package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.TotalRunStatistics
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Total run distance + VDOT card
 * Matches iOS TopTotalRunView2
 */
@Composable
fun TotalRunVdotCard(
    stats: TotalRunStatistics,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    StatisticsCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left - Total Distance
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = String.format("%.0f", stats.totalDistance),
                        color = RunBlue,
                        fontSize = 45.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "公里",
                        color = SecondaryTextColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
                    )
                }
                Text(
                    text = "累计里程",
                    color = SecondaryTextColor,
                    fontSize = 12.sp
                )
            }

            // Right - VDOT
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = String.format("%.0f", stats.overallVdot),
                        color = RunBlue,
                        fontSize = 45.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "跑力",
                        color = SecondaryTextColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
                    )
                }
                Text(
                    text = "VDOT",
                    color = SecondaryTextColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TotalRunVdotCardPreview() {
    TotalRunVdotCard(
        stats = TotalRunStatistics(
            totalDistance = 1234.5,
            overallVdot = 52.0
        )
    )
}
