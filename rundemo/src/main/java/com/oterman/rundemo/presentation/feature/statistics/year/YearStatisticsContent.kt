package com.oterman.rundemo.presentation.feature.statistics.year

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.domain.model.MonthRangeData
import com.oterman.rundemo.presentation.feature.home.components.DailySentenceCard
import com.oterman.rundemo.presentation.feature.statistics.week.components.HeartRateZonePlaceholder
import com.oterman.rundemo.presentation.feature.statistics.week.components.SpeedZonePlaceholder
import com.oterman.rundemo.presentation.feature.statistics.week.components.StatisticCardsGrid
import com.oterman.rundemo.presentation.feature.statistics.year.components.TrajectorySettingsSheet
import com.oterman.rundemo.presentation.feature.statistics.year.components.TrajectoryWallGrid
import com.oterman.rundemo.presentation.feature.statistics.year.components.YearBarChart
import com.oterman.rundemo.presentation.feature.statistics.year.components.YearDetailTable
import com.oterman.rundemo.presentation.feature.statistics.year.components.YearMonthsGridCard
import com.oterman.rundemo.presentation.feature.statistics.year.components.YearNavigationHeader

/**
 * Main content for Year Statistics tab
 * Displays year navigation, 12-month heatmap grid or trajectory wall, stats cards, charts, and daily sentence
 */
@Composable
fun YearStatisticsContent(
    onMonthClick: (MonthRangeData) -> Unit = {},
    viewModel: YearStatisticsViewModel = viewModel(
        factory = YearStatisticsViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val repository = remember {
        RunDataRepositoryImpl.getInstance(RunDatabase.getInstance(context))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Year navigation header with trajectory mode toggle
        YearNavigationHeader(
            yearDisplay = uiState.yearDisplay,
            canGoNext = uiState.canGoNext,
            showTrajectoryMode = uiState.showTrajectoryMode,
            onPreviousClick = viewModel::goToPreviousYear,
            onNextClick = viewModel::goToNextYear,
            onDateDoubleClick = viewModel::goToCurrentYear,
            onToggleMode = viewModel::toggleTrajectoryMode
        )

        if (uiState.showTrajectoryMode) {
            // Trajectory wall mode
            TrajectoryWallGrid(
                trajectoryWorkoutIds = uiState.trajectoryWorkoutIds,
                itemsPerRow = uiState.itemsPerRow,
                isLoading = uiState.isLoadingTrajectory,
                repository = repository,
                onSettingsClick = viewModel::toggleSettingsSheet
            )

            // Daily sentence
            if (uiState.dailySentence.isNotEmpty()) {
                DailySentenceCard(
                    sentence = uiState.dailySentence
                )
            }
        } else {
            // Heatmap mode (default)

            // 2. 12-month heatmap grid
            YearMonthsGridCard(
                runCount = uiState.yearStats.runCount,
                totalDistance = uiState.yearStats.totalDistance,
                monthRangeDataList = uiState.yearStats.monthRangeDataList,
                onMonthClick = onMonthClick
            )

            // 3. Four statistics cards (2x2 grid) - reuse week component
            StatisticCardsGrid(
                totalDistance = uiState.yearStats.totalDistance,
                totalDurationMinutes = uiState.yearStats.totalDurationMinutes,
                avgPace = uiState.yearStats.avgPace,
                totalElevation = uiState.yearStats.totalElevation
            )

            // 4. Heart rate zone placeholder - reuse week component
            HeartRateZonePlaceholder()

            // 5. Speed zone placeholder - reuse week component
            SpeedZonePlaceholder()

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

            // 8. Daily sentence - reuse home component
            if (uiState.dailySentence.isNotEmpty()) {
                DailySentenceCard(
                    sentence = uiState.dailySentence
                )
            }
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
