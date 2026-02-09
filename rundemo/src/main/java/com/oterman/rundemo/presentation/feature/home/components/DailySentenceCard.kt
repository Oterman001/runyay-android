package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Daily sentence card (minimal design)
 * Corresponds to iOS DailySentenceView
 */
@Composable
fun DailySentenceCard(
    sentence: String,
    modifier: Modifier = Modifier
) {
    StatisticsCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 40.dp)
        ) {
            Text(
                text = sentence,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
