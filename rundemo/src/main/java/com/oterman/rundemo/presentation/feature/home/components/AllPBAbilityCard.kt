package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.PBAbilityInfo

/**
 * Max data card (VDOT, Max Distance, Max Duration)
 * Corresponds to iOS AllPBAbilityView
 */
@Composable
fun AllPBAbilityCard(
    pbList: List<PBAbilityInfo>,
    modifier: Modifier = Modifier,
    onItemClick: (PBAbilityInfo) -> Unit = {}
) {
    StatisticsCard(modifier = modifier) {
        Column {
            // Title
            Text(
                text = "最大数据",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PB List
            pbList.forEachIndexed { index, item ->
                ItemPbSingleRow(
                    description = item.itemKey.description,
                    value = item.itemMaxValue ?: "--",
                    date = item.itemDate ?: "",
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
