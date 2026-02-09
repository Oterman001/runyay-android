package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.PBSpeedInfo
import com.oterman.rundemo.ui.theme.RunOrange

/**
 * PB speed records card (1km, 3km, 5km, 10km, half, full marathon)
 * Corresponds to iOS AllPBSpeedView
 */
@Composable
fun AllPBSpeedCard(
    pbList: List<PBSpeedInfo>,
    modifier: Modifier = Modifier,
    onItemClick: (PBSpeedInfo) -> Unit = {}
) {
    StatisticsCard(modifier = modifier) {
        Column {
            // Header: Medal icon + "PB记录"
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = RunOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PB记录",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // PB Speed List
            pbList.forEachIndexed { index, item ->
                ItemPbSingleRow(
                    description = item.pbKey.description,
                    value = item.pbTimeValue ?: "--",
                    date = item.pbDate ?: "",
                    onClick = if (item.workoutId != null) {
                        { onItemClick(item) }
                    } else null
                )

                // Divider between items (not after last)
                if (index < pbList.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
