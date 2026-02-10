package com.oterman.rundemo.presentation.feature.statistics.total

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
import com.oterman.rundemo.presentation.feature.home.components.DailySentenceCard
import com.oterman.rundemo.presentation.feature.statistics.total.components.TotalYearBarChart
import com.oterman.rundemo.presentation.feature.statistics.total.components.TotalYearDetailTable
import com.oterman.rundemo.presentation.feature.statistics.week.components.HeartRateZonePlaceholder
import com.oterman.rundemo.presentation.feature.statistics.week.components.SpeedZonePlaceholder
import com.oterman.rundemo.presentation.feature.statistics.week.components.StatisticCardsGrid

/**
 * Main content for Total Statistics tab (总视图)
 * Displays all-time statistics with yearly breakdown
 */
@Composable
fun TotalStatisticsContent(
    viewModel: TotalStatisticsViewModel = viewModel(
        factory = TotalStatisticsViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Four statistics cards (2x2 grid) - total stats
        item {
            StatisticCardsGrid(
                totalDistance = uiState.totalStats.totalDistance,
                totalDurationMinutes = uiState.totalStats.totalDurationMinutes,
                avgPace = uiState.totalStats.avgPace,
                totalElevation = uiState.totalStats.totalElevation
            )
        }

        // 2. Heart rate zone placeholder
        item {
            HeartRateZonePlaceholder()
        }

        // 3. Speed zone placeholder
        item {
            SpeedZonePlaceholder()
        }

        // 4. Year bar chart (only show if there's data)
        if (uiState.totalStats.yearlyStatistics.isNotEmpty()) {
            item {
                TotalYearBarChart(
                    yearlyStatistics = uiState.totalStats.yearlyStatistics,
                    chartDisplayMode = uiState.chartDisplayMode,
                    maxYearDistance = uiState.totalStats.maxYearDistance,
                    maxYearDuration = uiState.totalStats.maxYearDuration,
                    onToggleDisplayMode = viewModel::toggleChartDisplayMode
                )
            }
        }

        // 5. Year detail table (only show if there's data)
        if (uiState.totalStats.yearlyStatistics.isNotEmpty()) {
            item {
                TotalYearDetailTable(
                    yearlyStatistics = uiState.totalStats.yearlyStatistics,
                    totalStats = uiState.totalStats
                )
            }
        }

        // 6. Daily sentence
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
private fun TotalStatisticsContentPreview() {
    // Preview would require mock ViewModel
}
