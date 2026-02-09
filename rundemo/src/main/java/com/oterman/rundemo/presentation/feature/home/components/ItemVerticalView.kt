package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * Vertical stat item component
 * Used in LatestRunRecordCard for distance/duration/pace display
 * Corresponds to iOS ItemVerticalView
 */
@Composable
fun ItemVerticalView(
    itemValue: String,
    itemDesc: String,
    valueColor: Color = RunBlue,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = itemValue,
            color = valueColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = itemDesc,
            color = SecondaryTextColor,
            fontSize = 10.sp
        )
    }
}
