package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/**
 * Home tab content with iOS-style NavigationTitle effect
 * Large title collapses to small title when scrolling
 * Corresponds to iOS Tab1Page
 */
@Composable
fun HomeTabContent() {
    val lazyListState = rememberLazyListState()
    val backgroundColor = MaterialTheme.colorScheme.background

    // Calculate collapse progress based on scroll offset
    val collapseProgress by remember {
        derivedStateOf {
            val firstItemIndex = lazyListState.firstVisibleItemIndex
            val firstItemOffset = lazyListState.firstVisibleItemScrollOffset

            if (firstItemIndex > 0) {
                1f
            } else {
                (firstItemOffset / 200f).coerceIn(0f, 1f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Collapsed header (small title) - appears when scrolled
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .zIndex(1f)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .alpha(collapseProgress)
        ) {
            Text(
                text = "首页",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Main content
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            // Large title header (iOS NavigationTitle style)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 8.dp)
                        .graphicsLayer {
                            val scale = 1f - (collapseProgress * 0.15f)
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - collapseProgress
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        }
                ) {
                    Text(
                        text = "首页",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 0.sp
                    )
                }
            }

            // Placeholder items
            items(20) { index ->
                HomeListItem(index)
            }
        }
    }
}

/**
 * Home list item component
 */
@Composable
private fun HomeListItem(index: Int) {
    ListItem(
        headlineContent = {
            Text("活动记录 ${index + 1}")
        },
        supportingContent = {
            Text("这是第 ${index + 1} 条活动记录")
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.DirectionsRun,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}
