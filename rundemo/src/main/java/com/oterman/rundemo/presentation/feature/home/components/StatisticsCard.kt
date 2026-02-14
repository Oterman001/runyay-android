package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.presentation.components.AppCard

/**
 * Base card container for statistics cards
 * Matches iOS dashboardCardStyle():
 * - 10dp corner radius
 * - 12dp padding
 * - Light gray shadow in light mode only
 */
@Composable
fun StatisticsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            content = content
        )
    }
}
