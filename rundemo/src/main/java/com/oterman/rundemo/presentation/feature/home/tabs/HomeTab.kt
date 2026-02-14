package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.presentation.feature.home.components.AllPBAbilityCard
import com.oterman.rundemo.presentation.feature.home.components.RotatingSyncIcon
import com.oterman.rundemo.presentation.feature.home.components.AllPBSpeedCard
import com.oterman.rundemo.presentation.feature.home.components.DailySentenceCard
import com.oterman.rundemo.presentation.feature.home.components.DayRunRecordSelectDialog
import com.oterman.rundemo.presentation.feature.home.components.LatestRunRecordCard
import com.oterman.rundemo.presentation.feature.home.components.NextRaceCard
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
    ),
    showSyncIcon: Boolean = false,
    isSyncing: Boolean = false,
    onSyncIconClick: () -> Unit = {},
    onSetGoalClick: () -> Unit = {},
    onNavigateToRunDetail: (workoutId: String) -> Unit = {},
    onNavigateToRunStatistics: (tab: String) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshGoalSettings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val backgroundColor = MaterialTheme.colorScheme.background

    // State for multi-record selection dialog
    var showRecordSelectDialog by remember { mutableStateOf(false) }
    var selectedDayData by remember { mutableStateOf<DayRunData?>(null) }

    // Calculate collapse progress based on scroll offset
    val collapseProgress by remember {
        derivedStateOf {
            (scrollState.value / 200f).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Collapsed header (small title) - appears when scrolled
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .zIndex(1f)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .alpha(collapseProgress),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "首页",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (showSyncIcon) {
                RotatingSyncIcon(isRotating = isSyncing, onClick = onSyncIconClick)
            }
        }

        // Loading indicator
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // Large title header (iOS NavigationTitle style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 48.dp, bottom = 8.dp)
                    .graphicsLayer {
                        val scale = 1f - (collapseProgress * 0.15f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - collapseProgress
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "首页",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 0.sp
                )
                AnimatedVisibility(
                    visible = showSyncIcon,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    RotatingSyncIcon(isRotating = isSyncing, onClick = onSyncIconClick)
                }
            }

            // Total Run + VDOT Card
            TotalRunVdotCard(
                stats = uiState.totalStats,
                modifier = Modifier.padding(bottom = 10.dp),
                onClick = { onNavigateToRunStatistics("total") }
            )

            // Year + Month Cards (side by side)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PeriodStatisticsCard(
                    title = "今年",
                    stats = uiState.yearStats,
                    goalSettings = uiState.goalSettings,
                    isYearCard = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToRunStatistics("year") },
                    onSetGoalClick = onSetGoalClick
                )
                PeriodStatisticsCard(
                    title = "本月",
                    stats = uiState.monthStats,
                    goalSettings = uiState.goalSettings,
                    isYearCard = false,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToRunStatistics("month") },
                    onSetGoalClick = onSetGoalClick
                )
            }

            // Week Card
            WeekStatisticsCard(
                stats = uiState.weekStats,
                modifier = Modifier.padding(bottom = 10.dp),
                onDayClick = { dayData ->
                    when {
                        dayData.runCount == 1 && dayData.workoutIds.isNotEmpty() -> {
                            // Single record: navigate directly
                            onNavigateToRunDetail(dayData.workoutIds.first())
                        }
                        dayData.runCount > 1 -> {
                            // Multiple records: show selection dialog
                            selectedDayData = dayData
                            showRecordSelectDialog = true
                        }
                        // No records: do nothing
                    }
                },
                onClick = { onNavigateToRunStatistics("week") }
            )

            // Card 1: Latest Run Record
            uiState.latestRunRecord?.let { record ->
                LatestRunRecordCard(
                    record = record,
                    modifier = Modifier.padding(bottom = 10.dp),
                    onClick = { onNavigateToRunDetail(record.workoutId) }
                )
            }

            // Card 3: Next Race
            NextRaceCard(
                race = uiState.nextRace,
                modifier = Modifier.padding(bottom = 10.dp),
                onClick = { /* Navigate to race list or add race */ }
            )

            // Card 2: Max Data (PB Ability)
            if (uiState.pbAbilityList.isNotEmpty()) {
                AllPBAbilityCard(
                    pbList = uiState.pbAbilityList,
                    modifier = Modifier.padding(bottom = 10.dp),
                    onItemClick = { item ->
                        item.workoutId?.let { workoutId ->
                            onNavigateToRunDetail(workoutId)
                        }
                    }
                )
            }

            // Card 5: PB Speed Records
            if (uiState.pbSpeedList.isNotEmpty()) {
                AllPBSpeedCard(
                    pbList = uiState.pbSpeedList,
                    modifier = Modifier.padding(bottom = 10.dp),
                    onItemClick = { item ->
                        /* Navigate to record detail if workoutId exists */
                    }
                )
            }

            // Card 4: Daily Sentence
            if (uiState.dailySentence.isNotEmpty()) {
                DailySentenceCard(
                    sentence = uiState.dailySentence,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }
    }

    // Multi-record selection dialog
    if (showRecordSelectDialog && selectedDayData != null) {
        DayRunRecordSelectDialog(
            dayData = selectedDayData!!,
            onRecordSelected = { workoutId ->
                showRecordSelectDialog = false
                selectedDayData = null
                onNavigateToRunDetail(workoutId)
            },
            onDismiss = {
                showRecordSelectDialog = false
                selectedDayData = null
            }
        )
    }
}
