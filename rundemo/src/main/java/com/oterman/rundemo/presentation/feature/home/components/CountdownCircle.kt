package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Countdown circle component for race card
 * Corresponds to iOS MyRaceCardView countdown circle
 */
@Composable
fun CountdownCircle(
    mainText: String,
    subText: String,
    color: Color,
    size: Dp = 60.dp,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        // Circle border
        Canvas(modifier = Modifier.size(size)) {
            drawCircle(
                color = color.copy(alpha = 0.2f),
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // Text content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = mainText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            if (subText.isNotEmpty()) {
                Text(
                    text = subText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            }
        }
    }
}
