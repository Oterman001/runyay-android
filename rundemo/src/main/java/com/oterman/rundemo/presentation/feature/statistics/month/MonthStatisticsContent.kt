package com.oterman.rundemo.presentation.feature.statistics.month

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
import com.oterman.rundemo.presentation.feature.statistics.month.components.MonthBarChart
import com.oterman.rundemo.presentation.feature.statistics.month.components.MonthCalendarCard
import com.oterman.rundemo.presentation.feature.statistics.month.components.MonthDetailTable
import com.oterman.rundemo.presentation.feature.statistics.month.components.MonthNavigationHeader
import com.oterman.rundemo.presentation.feature.statistics.week.components.HeartRateZonePlaceholder
import com.oterman.rundemo.presentation.feature.statistics.week.components.SpeedZonePlaceholder
import com.oterman.rundemo.presentation.feature.statistics.week.components.StatisticCardsGrid

/**
 * Main content for Month Statistics tab
 * Displays month navigation, calendar grid, stats cards, charts, and daily sentence
 */
@Composable
fun MonthStatisticsContent(
    onDayClick: (DayRunData) -> Unit = {},
    viewModel: MonthStatisticsViewModel = viewModel(
        factory = MonthStatisticsViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Month navigation header
        item {
            MonthNavigationHeader(
                monthYearDisplay = uiState.monthYearDisplay,
                canGoNext = uiState.canGoNext,
                onPreviousClick = viewModel::goToPreviousMonth,
                onNextClick = viewModel::goToNextMonth,
                onDateDoubleClick = viewModel::goToCurrentMonth
            )
        }

        // 2. Month calendar grid (summary + weekday labels + day grid)
        item {
            MonthCalendarCard(
                runCount = uiState.monthStats.runCount,
                totalDistance = uiState.monthStats.totalDistance,
                dailyRecords = uiState.monthStats.dailyRecords,
                onDayClick = onDayClick
            )
        }

        // 3. Four statistics cards (2x2 grid)
        item {
            StatisticCardsGrid(
                totalDistance = uiState.monthStats.totalDistance,
                totalDurationMinutes = uiState.monthStats.totalDurationMinutes,
                avgPace = uiState.monthStats.avgPace,
                totalElevation = uiState.monthStats.totalElevation
            )
        }

        // 4. Heart rate zone placeholder
        item {
            HeartRateZonePlaceholder()
        }

        // 5. Speed zone placeholder
        item {
            SpeedZonePlaceholder()
        }

        // 6. Bar chart (only show if there's data)
        if (uiState.monthStats.totalDistance > 0) {
            item {
                MonthBarChart(
                    dailyRecords = uiState.monthStats.dailyRecords
                )
            }
        }

        // 7. Detail table (only show if there's data)
        if (uiState.monthStats.totalDistance > 0) {
            item {
                MonthDetailTable(
                    dailyRecords = uiState.monthStats.dailyRecords,
                    monthStats = uiState.monthStats
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
private fun MonthStatisticsContentPreview() {
    // Preview would require mock ViewModel
}
