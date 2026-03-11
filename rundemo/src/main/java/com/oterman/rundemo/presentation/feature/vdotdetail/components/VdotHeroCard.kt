package com.oterman.rundemo.presentation.feature.vdotdetail.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.RunYayFontFamily
import com.oterman.rundemo.ui.theme.SecondaryTextColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VdotHeroCard(
    currentVdot: Double,
    originVdot: Double,
    lastUpdateDate: Long,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left - smoothed VDOT
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (currentVdot > 0) String.format("%.1f", currentVdot) else "--",
                        color = RunTheme.colorScheme.blue,
                        fontSize = 55.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = RunYayFontFamily
                    )
                    Text(
                        text = "综合跑力",
                        color = SecondaryTextColor,
                        fontSize = 12.sp
                    )
                }

                // Right - raw VDOT
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (originVdot > 0) String.format("%.1f", originVdot) else "--",
                        color = RunTheme.colorScheme.orange,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = RunYayFontFamily
                    )
                    Text(
                        text = "动态跑力",
                        color = SecondaryTextColor,
                        fontSize = 12.sp
                    )
                }
            }

            // Last update date
            if (lastUpdateDate > 0) {
                val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    .format(Date(lastUpdateDate))
                Text(
                    text = "最近更新: $dateStr",
                    color = SecondaryTextColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
