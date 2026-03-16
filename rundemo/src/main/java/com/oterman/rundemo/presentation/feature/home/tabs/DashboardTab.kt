package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.oterman.rundemo.domain.model.DashboardCardId
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.presentation.feature.home.components.AllPBAbilityCard
import com.oterman.rundemo.presentation.feature.home.components.DashboardCardEditSheet
import com.oterman.rundemo.presentation.feature.home.components.RotatingSyncIcon
import com.oterman.rundemo.presentation.feature.home.components.AllPBSpeedCard
import com.oterman.rundemo.presentation.feature.home.components.DailySentenceCard
import com.oterman.rundemo.presentation.feature.home.components.DayRunRecordSelectDialog
import com.oterman.rundemo.presentation.feature.home.components.NextRaceCard
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.presentation.components.EditInclusiveLevelDialog
import com.oterman.rundemo.presentation.feature.home.components.RunRecordItem
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.presentation.feature.home.components.PeriodStatisticsCard
import com.oterman.rundemo.presentation.feature.home.components.TotalRunVdotCard
import com.oterman.rundemo.presentation.feature.home.components.WeekStatisticsCard

/**
 * Home tab content with iOS-style NavigationTitle effect
 * Large title collapses to small title when scrolling
 * Corresponds to iOS Tab1Page
 */
@Composable
fun DashboardTabContent(
    viewModel: DashboardTabViewModel = viewModel(
        factory = HomeTabViewModelFactory(LocalContext.current)
    ),
    showSyncIcon: Boolean = false,
    isSyncing: Boolean = false,
    onSyncIconClick: () -> Unit = {},
    onSetGoalClick: () -> Unit = {},
    onNavigateToRunDetail: (workoutId: String) -> Unit = {},
    onNavigateToRunStatistics: (tab: String) -> Unit = {},
    onNavigateToVdotDetail: () -> Unit = {},
    onSwitchToDataTab: () -> Unit = {}
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
    val trackPointsVersion by viewModel.trackPointsVersion.collectAsState()
    val showTrajectoryMode by viewModel.showTrajectoryMode.collectAsState()
    val trajectoryDataMap by viewModel.trajectoryDataMap.collectAsState()
    val dashboardCards by viewModel.dashboardCards.collectAsState()
    val scrollState = rememberScrollState()
    val backgroundColor = MaterialTheme.colorScheme.background

    // State for multi-record selection dialog
    var showRecordSelectDialog by remember { mutableStateOf(false) }
    var selectedDayData by remember { mutableStateOf<DayRunData?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var pendingInclusiveLevelRecord by remember { mutableStateOf<RunRecordEntity?>(null) }

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
                text = "仪表盘",
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
                    text = "仪表盘",
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

            // Dynamic card rendering based on user configuration
            dashboardCards.forEach { card ->
                if (card.visible) {
                    when (card.id) {
                        DashboardCardId.TOTAL_VDOT -> {
                            TotalRunVdotCard(
                                stats = uiState.totalStats,
                                modifier = Modifier.padding(bottom = 10.dp, top = 10.dp),
                                onDistanceClick = { onSwitchToDataTab() },
                                onVdotClick = { onNavigateToVdotDetail() }
                            )
                        }
                        DashboardCardId.YEAR_MONTH -> {
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
                        }
                        DashboardCardId.WEEK -> {
                            WeekStatisticsCard(
                                stats = uiState.weekStats,
                                modifier = Modifier.padding(bottom = 10.dp),
                                showTrajectoryMode = showTrajectoryMode,
                                trajectoryDataMap = trajectoryDataMap,
                                onToggleTrajectoryMode = { viewModel.toggleTrajectoryMode() },
                                onDayClick = { dayData ->
                                    when {
                                        dayData.runCount == 1 && dayData.workoutIds.isNotEmpty() -> {
                                            onNavigateToRunDetail(dayData.workoutIds.first())
                                        }
                                        dayData.runCount > 1 -> {
                                            selectedDayData = dayData
                                            showRecordSelectDialog = true
                                        }
                                    }
                                },
                                onClick = { onNavigateToRunStatistics("week") }
                            )
                        }
                        DashboardCardId.LATEST_RUN -> {
                            uiState.latestRunRecordEntity?.let { record ->
                                key(record.workoutId, trackPointsVersion) {
                                    val trackPoints = viewModel.getCachedTrackPoints(record.workoutId)
                                    val isLoading = viewModel.isTrackPointsLoading(record.workoutId)
                                    RunRecordItem(
                                        record = record,
                                        trackPoints = trackPoints,
                                        isTrackPointsLoading = isLoading,
                                        primaryColor = MaterialTheme.colorScheme.onSurface,
                                        onClick = { onNavigateToRunDetail(record.workoutId) },
                                        onInclusiveLevelClick = { pendingInclusiveLevelRecord = record },
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )
                                }
                            }
                        }
                        DashboardCardId.PB_ABILITY -> {
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
                        }
                        DashboardCardId.PB_SPEED -> {
                            if (uiState.pbSpeedList.isNotEmpty()) {
                                AllPBSpeedCard(
                                    pbList = uiState.pbSpeedList,
                                    modifier = Modifier.padding(bottom = 10.dp),
                                    onItemClick = { item ->
                                        item.workoutId?.let { workoutId ->
                                            onNavigateToRunDetail(workoutId)
                                        }
                                    }
                                )
                            }
                        }
                        DashboardCardId.DAILY_SENTENCE -> {
                            if (uiState.dailySentence.isNotEmpty()) {
                                DailySentenceCard(
                                    sentence = uiState.dailySentence,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Edit button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { showEditSheet = true },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "编辑仪表盘",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "编辑仪表盘",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // InclusiveLevel edit dialog for latest run card
    pendingInclusiveLevelRecord?.let { rec ->
        EditInclusiveLevelDialog(
            currentLevel = rec.inclusiveLevel,
            onDismiss = { pendingInclusiveLevelRecord = null },
            onConfirm = { viewModel.updateInclusiveLevel(rec, it); pendingInclusiveLevelRecord = null }
        )
    }

    // Dashboard card edit sheet
    if (showEditSheet) {
        DashboardCardEditSheet(
            cards = dashboardCards,
            onSave = { updatedCards ->
                viewModel.saveDashboardCards(updatedCards)
                showEditSheet = false
            },
            onDismiss = { showEditSheet = false }
        )
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
