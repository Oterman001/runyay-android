package com.oterman.rundemo.presentation.feature.statistics.year

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.domain.model.MonthRangeData
import com.oterman.rundemo.presentation.feature.home.components.DailySentenceCard
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.presentation.feature.statistics.week.components.HeartRateZoneCard
import com.oterman.rundemo.presentation.feature.statistics.week.components.SpeedZoneCard
import com.oterman.rundemo.presentation.feature.statistics.week.components.StatisticCardsGrid
import com.oterman.rundemo.presentation.feature.statistics.year.components.TrajectorySettingsSheet
import com.oterman.rundemo.presentation.feature.statistics.year.components.TrajectoryWallGrid
import com.oterman.rundemo.presentation.feature.statistics.year.components.YearBarChart
import com.oterman.rundemo.presentation.feature.statistics.year.components.YearDetailTable
import com.oterman.rundemo.presentation.feature.statistics.year.components.YearMonthsGrid
import com.oterman.rundemo.presentation.feature.statistics.year.components.YearNavigationHeader
import com.oterman.rundemo.presentation.feature.statistics.year.components.YearSummaryHeader

/**
 * Main content for Year Statistics tab
 * Displays year navigation, 12-month heatmap grid or trajectory wall, stats cards, charts, and daily sentence
 */
@Composable
fun YearStatisticsContent(
    onMonthClick: (MonthRangeData) -> Unit = {},
    onWorkoutClick: (workoutId: String) -> Unit = {},
    viewModel: YearStatisticsViewModel = viewModel(
        factory = YearStatisticsViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val repository = remember {
        RunDataRepositoryImpl.getInstance(RunDatabase.getInstance(context))
    }

    val isDark = isSystemInDarkTheme()
    val dividerColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Year navigation header (prev / year / next)
        YearNavigationHeader(
            yearDisplay = uiState.yearDisplay,
            canGoNext = uiState.canGoNext,
            onPreviousClick = viewModel::goToPreviousYear,
            onNextClick = viewModel::goToNextYear,
            onDateDoubleClick = viewModel::goToCurrentYear
        )

        // 2. Unified card: header + buttons + content (heatmap or trajectory)
        StatisticsCard {
            Column {
                // Header row with summary text and buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    YearSummaryHeader(
                        runCount = uiState.yearStats.runCount,
                        totalDistance = uiState.yearStats.totalDistance
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (uiState.showTrajectoryMode) {
                        IconButton(
                            onClick = viewModel::toggleSettingsSheet,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "轨迹墙设置",
                                tint = RunTheme.colorScheme.blue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = viewModel::toggleTrajectoryMode,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.showTrajectoryMode)
                                Icons.Default.RemoveRedEye
                            else
                                Icons.Outlined.Route,
                            contentDescription = if (uiState.showTrajectoryMode) "切换到月份显示" else "切换到轨迹墙显示",
                            tint = RunTheme.colorScheme.blue
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = dividerColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Conditional content
                if (uiState.showTrajectoryMode) {
                    TrajectoryWallGrid(
                        trajectoryWorkoutIds = uiState.trajectoryWorkoutIds,
                        itemsPerRow = uiState.itemsPerRow,
                        isLoading = uiState.isLoadingTrajectory,
                        repository = repository,
                        distanceMap = uiState.trajectoryDistanceMap,
                        onWorkoutClick = onWorkoutClick
                    )
                } else {
                    YearMonthsGrid(
                        monthRangeDataList = uiState.yearStats.monthRangeDataList,
                        onMonthClick = onMonthClick
                    )
                }
            }
        }

        // 3. Statistics cards (always visible)
        StatisticCardsGrid(
            totalDistance = uiState.yearStats.totalDistance,
            totalDurationMinutes = uiState.yearStats.totalDurationMinutes,
            avgPace = uiState.yearStats.avgPace,
            totalElevation = uiState.yearStats.totalElevation
        )

        // 4. Heart rate zone distribution
        if (uiState.heartRate7Zones.isNotEmpty() || uiState.heartRate5Zones.isNotEmpty()) {
            HeartRateZoneCard(
                heartRate7Zones = uiState.heartRate7Zones,
                heartRate5Zones = uiState.heartRate5Zones
            )
        }

        // 5. Speed zone distribution
        if (uiState.speedZones.isNotEmpty()) {
            SpeedZoneCard(speedZones = uiState.speedZones)
        }

        // 6. Year bar chart (only show if there's data)
        if (uiState.yearStats.totalDistance > 0) {
            YearBarChart(
                monthRangeDataList = uiState.yearStats.monthRangeDataList,
                maxMonthDistance = uiState.yearStats.maxMonthDistance,
                avgMonthDistance = uiState.yearStats.getAverageMonthDistance(
                    viewModel.isCurYear(),
                    viewModel.getCurMonth()
                )
            )
        }

        // 7. Year detail table (only show if there's data)
        if (uiState.yearStats.totalDistance > 0) {
            YearDetailTable(
                monthRangeDataList = uiState.yearStats.monthRangeDataList,
                yearStats = uiState.yearStats,
                isCurYear = viewModel.isCurYear(),
                curMonth = viewModel.getCurMonth()
            )
        }

        // 8. Daily sentence
        if (uiState.dailySentence.isNotEmpty()) {
            DailySentenceCard(
                sentence = uiState.dailySentence
            )
        }
    }

    // Settings bottom sheet
    if (uiState.showSettingsSheet) {
        TrajectorySettingsSheet(
            currentItemsPerRow = uiState.itemsPerRow,
            onItemsPerRowSelected = viewModel::setItemsPerRow,
            onDismiss = viewModel::dismissSettingsSheet
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun YearStatisticsContentPreview() {
    // Preview would require mock ViewModel
}
