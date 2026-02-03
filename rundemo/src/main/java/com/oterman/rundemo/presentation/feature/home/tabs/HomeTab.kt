package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.presentation.feature.home.components.PeriodStatisticsCard
import com.oterman.rundemo.presentation.feature.home.components.TotalRunVdotCard
import com.oterman.rundemo.presentation.feature.home.components.WeekStatisticsCard

/**
 * Home tab content with iOS-style NavigationTitle effect
 * Large title collapses to small title when scrolling
 * Corresponds to iOS Tab1Page
 */
@Composable
fun HomeTabContent(
    viewModel: HomeTabViewModel = viewModel(
        factory = HomeTabViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
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

        // Loading indicator
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Main content
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // Large title header (iOS NavigationTitle style)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, top = 48.dp, bottom = 8.dp)
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

            // Total Run + VDOT Card
            item {
                TotalRunVdotCard(
                    stats = uiState.totalStats,
                    modifier = Modifier.padding(bottom = 10.dp),
                    onClick = { /* Navigate to all records */ }
                )
            }

            // Year + Month Cards (side by side)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PeriodStatisticsCard(
                        title = "今年",
                        stats = uiState.yearStats,
                        modifier = Modifier.weight(1f),
                        onClick = { /* Navigate to year stats */ }
                    )
                    PeriodStatisticsCard(
                        title = "本月",
                        stats = uiState.monthStats,
                        modifier = Modifier.weight(1f),
                        onClick = { /* Navigate to month stats */ }
                    )
                }
            }

            // Week Card
            item {
                WeekStatisticsCard(
                    stats = uiState.weekStats,
                    onDayClick = { dayData ->
                        /* Navigate to day detail */
                    },
                    onClick = { /* Navigate to week stats */ }
                )
            }




        }
    }
}
