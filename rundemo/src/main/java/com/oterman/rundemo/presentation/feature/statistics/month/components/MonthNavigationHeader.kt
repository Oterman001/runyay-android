package com.oterman.rundemo.presentation.feature.statistics.month.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Month navigation header with previous/next buttons and month year display
 * Double-tap on date to jump to current month
 */
@Composable
fun MonthNavigationHeader(
    monthYearDisplay: String,
    canGoNext: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onDateDoubleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous month button
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "上一月",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Month year display (double tap to go to current month)
        Text(
            text = monthYearDisplay,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDateDoubleClick() }
                )
            }
        )

        // Next month button
        IconButton(
            onClick = onNextClick,
            enabled = canGoNext,
            modifier = Modifier
                .size(44.dp)
                .alpha(if (canGoNext) 1f else 0.3f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "下一月",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthNavigationHeaderPreview() {
    MonthNavigationHeader(
        monthYearDisplay = "2024年11月",
        canGoNext = false,
        onPreviousClick = {},
        onNextClick = {},
        onDateDoubleClick = {}
    )
}
