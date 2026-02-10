package com.oterman.rundemo.presentation.feature.statistics.year

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
import com.oterman.rundemo.domain.model.MonthRangeData
import com.oterman.rundemo.presentation.feature.home.components.DailySentenceCard
import com.oterman.rundemo.presentation.feature.statistics.week.components.HeartRateZonePlaceholder
import com.oterman.rundemo.presentation.feature.statistics.week.components.SpeedZonePlaceholder
import com.oterman.rundemo.presentation.feature.statistics.week.components.StatisticCardsGrid
import com.oterman.rundemo.presentation.feature.statistics.year.components.YearBarChart
import com.oterman.rundemo.presentation.feature.statistics.year.components.YearDetailTable
import com.oterman.rundemo.presentation.feature.statistics.year.components.YearMonthsGridCard
import com.oterman.rundemo.presentation.feature.statistics.year.components.YearNavigationHeader

/**
 * Main content for Year Statistics tab
 * Displays year navigation, 12-month heatmap grid, stats cards, charts, and daily sentence
 */
@Composable
fun YearStatisticsContent(
    onMonthClick: (MonthRangeData) -> Unit = {},
    viewModel: YearStatisticsViewModel = viewModel(
        factory = YearStatisticsViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Year navigation header
        item {
            YearNavigationHeader(
                yearDisplay = uiState.yearDisplay,
                canGoNext = uiState.canGoNext,
                onPreviousClick = viewModel::goToPreviousYear,
                onNextClick = viewModel::goToNextYear,
                onDateDoubleClick = viewModel::goToCurrentYear
            )
        }

        // 2. 12-month heatmap grid
        item {
            YearMonthsGridCard(
                runCount = uiState.yearStats.runCount,
                totalDistance = uiState.yearStats.totalDistance,
                monthRangeDataList = uiState.yearStats.monthRangeDataList,
                onMonthClick = onMonthClick
            )
        }

        // 3. Four statistics cards (2x2 grid) - reuse week component
        item {
            StatisticCardsGrid(
                totalDistance = uiState.yearStats.totalDistance,
                totalDurationMinutes = uiState.yearStats.totalDurationMinutes,
                avgPace = uiState.yearStats.avgPace,
                totalElevation = uiState.yearStats.totalElevation
            )
        }

        // 4. Heart rate zone placeholder - reuse week component
        item {
            HeartRateZonePlaceholder()
        }

        // 5. Speed zone placeholder - reuse week component
        item {
            SpeedZonePlaceholder()
        }

        // 6. Year bar chart (only show if there's data)
        if (uiState.yearStats.totalDistance > 0) {
            item {
                YearBarChart(
                    monthRangeDataList = uiState.yearStats.monthRangeDataList,
                    maxMonthDistance = uiState.yearStats.maxMonthDistance,
                    avgMonthDistance = uiState.yearStats.getAverageMonthDistance(
                        viewModel.isCurYear(),
                        viewModel.getCurMonth()
                    )
                )
            }
        }

        // 7. Year detail table (only show if there's data)
        if (uiState.yearStats.totalDistance > 0) {
            item {
                YearDetailTable(
                    monthRangeDataList = uiState.yearStats.monthRangeDataList,
                    yearStats = uiState.yearStats,
                    isCurYear = viewModel.isCurYear(),
                    curMonth = viewModel.getCurMonth()
                )
            }
        }

        // 8. Daily sentence - reuse home component
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
private fun YearStatisticsContentPreview() {
    // Preview would require mock ViewModel
}
