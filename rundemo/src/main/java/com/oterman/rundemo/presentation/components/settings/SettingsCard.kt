package com.oterman.rundemo.presentation.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.oterman.rundemo.presentation.components.AppCard

/**
 * Card container for settings groups
 * Corresponds to iOS SettingsGroup with dashboardCardStyle
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AppCard(modifier = modifier) {
        Column {
            content()
        }
    }
}
