package com.oterman.rundemo.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
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
 * Unified card container used across all pages.
 * - 10dp corner radius
 * - Light-mode only shadow (5dp gray)
 * - CardBgLight / CardBgDark background
 * - 0dp Material elevation
 * - No internal padding (callers handle their own)
 */
@Composable
fun AppCard(
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}
