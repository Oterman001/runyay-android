package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.LatestRunRecord
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Latest run record card
 * Corresponds to iOS LatestRunRecordView
 */
@Composable
fun LatestRunRecordCard(
    record: LatestRunRecord,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    StatisticsCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Column {
            // Row 1: Date + Start/End Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsRun,
                    contentDescription = null,
                    tint = RunTheme.colorScheme.blue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = record.runDate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 6.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = record.startEndTime,
                    fontSize = 12.sp,
                    color = SecondaryTextColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 2: Stats (Distance, Duration, Pace)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ItemVerticalView(
                    itemValue = record.getFormattedDistance(),
                    itemDesc = "总距离(公里)"
                )
                ItemVerticalView(
                    itemValue = record.duration,
                    itemDesc = "总时长"
                )
                ItemVerticalView(
                    itemValue = record.avgPace,
                    itemDesc = "平均配速"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Device + Verification Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.deviceName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (record.isVerified) Color(0xFF34C759)
                            else Color(0xFF8E8E93)
                        )
                )
            }
        }
    }
}
