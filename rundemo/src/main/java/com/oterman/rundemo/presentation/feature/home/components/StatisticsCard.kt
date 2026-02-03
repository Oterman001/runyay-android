package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.ui.theme.CardBgDark
import com.oterman.rundemo.ui.theme.CardBgLight

/**
 * Base card container for statistics cards
 * Matches iOS dashboardCardStyle():
 * - 10dp corner radius
 * - 16dp padding
 * - Light gray shadow in light mode only
 */
@Composable
fun StatisticsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(10.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!isDark) {
                    Modifier.shadow(
                        elevation = 5.dp,
                        shape = cardShape,
                        ambientColor = Color.Gray.copy(alpha = 0.1f),
                        spotColor = Color.Gray.copy(alpha = 0.1f)
                    )
                } else {
                    Modifier
                }
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) CardBgDark else CardBgLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
