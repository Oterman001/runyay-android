package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.ui.theme.RunTheme

/**
 * 地图左下角天气信息覆盖层
 * 对标iOS V3WeatherView
 */
@Composable
fun RunDetailWeatherOverlay(
    temperature: Double,
    humidity: Double,
    modifier: Modifier = Modifier
) {
    // 只有温度或湿度有效时才显示
    if (temperature == 0.0 && humidity == 0.0) return

    Row(
        modifier = modifier
            .background(
                color = Color.Transparent
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (temperature != 0.0) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Thermostat,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = RunTheme.colorScheme.orange
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${temperature.toInt()}°C",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color =RunTheme.colorScheme.orange
                )
            }
        }

        if (humidity > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint =  RunTheme.colorScheme.blue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${humidity.toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = RunTheme.colorScheme.blue
                )
            }
        }
    }
}

