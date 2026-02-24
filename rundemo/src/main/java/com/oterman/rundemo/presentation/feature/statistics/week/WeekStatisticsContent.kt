package com.oterman.rundemo.presentation.feature.statistics.week

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.presentation.feature.home.components.DailySentenceCard
import com.oterman.rundemo.presentation.feature.statistics.week.components.HeartRateZoneCard
import com.oterman.rundemo.presentation.feature.statistics.week.components.SpeedZoneCard
import com.oterman.rundemo.presentation.feature.statistics.week.components.StatisticCardsGrid
import com.oterman.rundemo.presentation.feature.statistics.week.components.WeekBarChart
import com.oterman.rundemo.presentation.feature.statistics.week.components.WeekDetailTable
import com.oterman.rundemo.presentation.feature.statistics.week.components.WeekNavigationHeader
import com.oterman.rundemo.presentation.feature.statistics.week.components.WeekSummaryAndGrid

/**
 * Main content for Week Statistics tab
 * Displays week navigation, day grid, stats cards, charts, and daily sentence
 */
@Composable
fun WeekStatisticsContent(
    onDayClick: (DayRunData) -> Unit = {},
    viewModel: WeekStatisticsViewModel = viewModel(
        factory = WeekStatisticsViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Week navigation header
        item {
            WeekNavigationHeader(
                dateRange = uiState.weekDateRange,
                canGoNext = uiState.canGoNext,
                onPreviousClick = viewModel::goToPreviousWeek,
                onNextClick = viewModel::goToNextWeek,
                onDateDoubleClick = viewModel::goToCurrentWeek
            )
        }

        // 2. Week summary + 7-day grid
        item {
            val showTrajectoryMode by viewModel.showTrajectoryMode.collectAsState()
            val trajectoryDataMap by viewModel.trajectoryDataMap.collectAsState()
            
            WeekSummaryAndGrid(
                runCount = uiState.weekStats.runCount,
                totalDistance = uiState.weekStats.totalDistance,
                dailyRecords = uiState.weekStats.dailyRecords,
                showTrajectoryMode = showTrajectoryMode,
                trajectoryDataMap = trajectoryDataMap,
                onDayClick = onDayClick,
                onToggleTrajectoryMode = { viewModel.toggleTrajectoryMode() }
            )
        }

        // 3. Four statistics cards (2x2 grid)
        item {
            StatisticCardsGrid(
                totalDistance = uiState.weekStats.totalDistance,
                totalDurationMinutes = uiState.weekStats.totalDurationMinutes,
                avgPace = uiState.weekStats.avgPace,
                totalElevation = uiState.weekStats.totalElevation
            )
        }

        // 4. Heart rate zone distribution
        if (uiState.heartRate7Zones.isNotEmpty() || uiState.heartRate5Zones.isNotEmpty()) {
            item {
                HeartRateZoneCard(
                    heartRate7Zones = uiState.heartRate7Zones,
                    heartRate5Zones = uiState.heartRate5Zones
                )
            }
        }

        // 5. Speed zone distribution
        if (uiState.speedZones.isNotEmpty()) {
            item {
                SpeedZoneCard(
                    speedZones = uiState.speedZones
                )
            }
        }

        // 6. Bar chart (only show if there's data)
        if (uiState.weekStats.totalDistance > 0) {
            item {
                WeekBarChart(
                    dailyRecords = uiState.weekStats.dailyRecords
                )
            }
        }

        // 7. Detail table (only show if there's data)
        if (uiState.weekStats.totalDistance > 0) {
            item {
                WeekDetailTable(
                    dailyRecords = uiState.weekStats.dailyRecords,
                    weekStats = uiState.weekStats
                )
            }
        }

        // 8. Daily sentence
        if (uiState.dailySentence.isNotEmpty()) {
            item {
                DailySentenceCard(
                    sentence = uiState.dailySentence
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeekStatisticsContentPreview() {
    // Preview would require mock ViewModel
}
