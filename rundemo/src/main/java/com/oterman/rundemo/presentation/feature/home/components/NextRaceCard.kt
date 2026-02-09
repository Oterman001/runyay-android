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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.NextRaceInfo
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Next race event card
 * Corresponds to iOS MyRaceCardView
 */
@Composable
fun NextRaceCard(
    race: NextRaceInfo?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    StatisticsCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Column {
            // Header Row: Flag icon + "下次赛事"
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = RunBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "下次赛事",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 6.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (race != null) {
                // Race info row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left: Race name, type badge, date
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Race name
                        Text(
                            text = race.raceName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Type badge + Date
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Type badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(race.raceType.themeColor)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = race.raceType.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Date
                            Text(
                                text = race.getFormattedDate(),
                                fontSize = 14.sp,
                                color = SecondaryTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right: Countdown circle
                    val countdownText = race.getCountdownText()
                    CountdownCircle(
                        mainText = countdownText.first,
                        subText = countdownText.second,
                        color = race.getCountdownColor(),
                        size = 60.dp
                    )
                }
            } else {
                // Empty state
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "无比赛信息，快去添加吧",
                        fontSize = 14.sp,
                        color = SecondaryTextColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = RunBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
