package com.oterman.rundemo.presentation.feature.vdotdetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.feature.vdotdetail.PredictedRaceTime
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.SecondaryTextColor
import com.oterman.rundemo.util.FitDataConverter

@Composable
fun PredictedRaceTimesCard(
    raceTimes: List<PredictedRaceTime>,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "预测成绩",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 2x2 grid
            if (raceTimes.size >= 4) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RaceTimeItem(raceTimes[0], Modifier.weight(1f))
                    RaceTimeItem(raceTimes[1], Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RaceTimeItem(raceTimes[2], Modifier.weight(1f))
                    RaceTimeItem(raceTimes[3], Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RaceTimeItem(
    raceTime: PredictedRaceTime,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = raceTime.label,
            color = SecondaryTextColor,
            fontSize = 12.sp
        )
        Text(
            text = FitDataConverter.formatDuration(raceTime.timeMinutes),
            color = RunTheme.colorScheme.blue,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
