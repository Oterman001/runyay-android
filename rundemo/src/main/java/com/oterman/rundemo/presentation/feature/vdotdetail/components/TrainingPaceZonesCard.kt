package com.oterman.rundemo.presentation.feature.vdotdetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.feature.vdotdetail.TrainingPaceZone
import com.oterman.rundemo.ui.theme.RunTheme

@Composable
fun TrainingPaceZonesCard(
    paces: List<TrainingPaceZone>,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "训练配速",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            paces.forEachIndexed { index, zone ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = zone.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = zone.paceDisplay,
                        color = RunTheme.colorScheme.blue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (index < paces.lastIndex) {
                    HorizontalDivider(
                        color = RunTheme.colorScheme.divider,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}
