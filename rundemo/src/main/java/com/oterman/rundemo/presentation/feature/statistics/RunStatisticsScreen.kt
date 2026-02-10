package com.oterman.rundemo.presentation.feature.statistics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.domain.model.MonthRangeData
import com.oterman.rundemo.presentation.feature.home.components.DayRunRecordSelectDialog
import com.oterman.rundemo.presentation.feature.statistics.month.MonthStatisticsContent
import com.oterman.rundemo.presentation.feature.statistics.total.TotalStatisticsContent
import com.oterman.rundemo.presentation.feature.statistics.month.MonthStatisticsViewModel
import com.oterman.rundemo.presentation.feature.statistics.month.MonthStatisticsViewModelFactory
import com.oterman.rundemo.presentation.feature.statistics.week.WeekStatisticsContent
import com.oterman.rundemo.presentation.feature.statistics.year.YearStatisticsContent
import kotlinx.coroutines.launch

/**
 * 跑步统计页面
 * 包含周/月/年/总四个tab，支持滑动切换
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunStatisticsScreen(
    initialTab: RunStatisticTab = RunStatisticTab.WEEK,
    onNavigateBack: () -> Unit = {},
    onNavigateToRunDetail: (workoutId: String) -> Unit = {},
    viewModel: RunStatisticsViewModel = viewModel(
        factory = RunStatisticsViewModelFactory(initialTab)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // State for multi-record selection dialog
    var showRecordSelectDialog by remember { mutableStateOf(false) }
    var selectedDayData by remember { mutableStateOf<DayRunData?>(null) }

    // State for month navigation from year view
    var pendingMonthNavigation by remember { mutableStateOf<MonthRangeData?>(null) }

    // Month ViewModel instance (shared across recompositions)
    val monthViewModel: MonthStatisticsViewModel = viewModel(
        factory = MonthStatisticsViewModelFactory(context)
    )

    // HorizontalPager state
    val pagerState = rememberPagerState(
        initialPage = initialTab.ordinal,
        pageCount = { RunStatisticTab.entries.size }
    )

    // Sync pager with ViewModel state
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.selectTabByIndex(page)
        }
    }

    // Sync ViewModel state with pager
    LaunchedEffect(uiState.selectedTab) {
        if (pagerState.currentPage != uiState.selectedTab.ordinal) {
            pagerState.animateScrollToPage(uiState.selectedTab.ordinal)
        }
    }

    // Handle month navigation from year view
    LaunchedEffect(pendingMonthNavigation) {
        pendingMonthNavigation?.let { monthData ->
            monthViewModel.goToSpecificMonth(monthData.year, monthData.month)
            pendingMonthNavigation = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("跑步统计") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Selector
            StatisticsTabSelector(
                selectedTab = uiState.selectedTab,
                onTabSelected = { tab ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(tab.ordinal)
                    }
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
            )

            // Content Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (RunStatisticTab.entries[page]) {
                    RunStatisticTab.WEEK -> WeekStatisticsContent(
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
                        }
                    )
                    RunStatisticTab.MONTH -> MonthStatisticsContent(
                        viewModel = monthViewModel,
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
                        }
                    )
                    RunStatisticTab.YEAR -> YearStatisticsContent(
                        onMonthClick = { monthData ->
                            // Switch to MONTH tab and navigate to the clicked month
                            pendingMonthNavigation = monthData
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(RunStatisticTab.MONTH.ordinal)
                            }
                        }
                    )
                    RunStatisticTab.TOTAL -> TotalStatisticsContent()
                }
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

/**
 * Tab选择器
 * 水平排列的4个tab按钮，选中时有背景色和缩放效果
 */
@Composable
private fun StatisticsTabSelector(
    selectedTab: RunStatisticTab,
    onTabSelected: (RunStatisticTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RunStatisticTab.entries.forEach { tab ->
            TabButton(
                label = tab.label,
                isSelected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 单个Tab按钮
 */
@Composable
private fun TabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.surface
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 250),
        label = "tabBackground"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "tabScale"
    )

    Box(
        modifier = modifier
            .padding(4.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

