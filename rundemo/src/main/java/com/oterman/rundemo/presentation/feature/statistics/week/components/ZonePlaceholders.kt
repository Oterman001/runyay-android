package com.oterman.rundemo.presentation.feature.statistics.week.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Placeholder card for heart rate zone distribution (to be implemented)
 */
@Composable
fun HeartRateZonePlaceholder(
    modifier: Modifier = Modifier
) {
    ZonePlaceholderCard(
        title = "心率区间分布",
        modifier = modifier
    )
}

/**
 * Placeholder card for speed/pace zone distribution (to be implemented)
 */
@Composable
fun SpeedZonePlaceholder(
    modifier: Modifier = Modifier
) {
    ZonePlaceholderCard(
        title = "配速区间分布",
        modifier = modifier
    )
}

/**
 * Generic placeholder card for zone distributions
 */
@Composable
private fun ZonePlaceholderCard(
    title: String,
    modifier: Modifier = Modifier
) {
    StatisticsCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "待实现",
                tint = SecondaryTextColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "$title (待实现)",
                fontSize = 14.sp,
                color = SecondaryTextColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HeartRateZonePlaceholderPreview() {
    HeartRateZonePlaceholder()
}

@Preview(showBackground = true)
@Composable
private fun SpeedZonePlaceholderPreview() {
    SpeedZonePlaceholder()
}
