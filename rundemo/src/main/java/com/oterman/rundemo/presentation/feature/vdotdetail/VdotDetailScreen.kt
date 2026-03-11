package com.oterman.rundemo.presentation.feature.vdotdetail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.presentation.feature.vdotdetail.components.PredictedRaceTimesCard
import com.oterman.rundemo.presentation.feature.vdotdetail.components.TrainingPaceZonesCard
import com.oterman.rundemo.presentation.feature.vdotdetail.components.VdotHeroCard
import com.oterman.rundemo.presentation.feature.vdotdetail.components.VdotTrendChart
import com.oterman.rundemo.ui.theme.RunTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VdotDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: VdotDetailViewModel = viewModel(
        factory = VdotDetailViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("跑力详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // A. Hero card
            item {
                VdotHeroCard(
                    currentVdot = uiState.currentVdot,
                    originVdot = uiState.currentOriginVdot,
                    lastUpdateDate = uiState.lastUpdateDate
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // B. Period selector
            item {
                val periods = TimePeriod.entries
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillParentMaxWidth()
                ) {
                    periods.forEachIndexed { index, period ->
                        SegmentedButton(
                            selected = uiState.selectedPeriod == period,
                            onClick = { viewModel.onPeriodChanged(period) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = periods.size
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = RunTheme.colorScheme.blue.copy(alpha = 0.12f),
                                activeContentColor = RunTheme.colorScheme.blue
                            )
                        ) {
                            Text(text = period.label)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // C. Trend chart
            item {
                VdotTrendChart(
                    points = uiState.trendPoints
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // D. Predicted race times
            if (uiState.predictedRaceTimes.isNotEmpty()) {
                item {
                    PredictedRaceTimesCard(
                        raceTimes = uiState.predictedRaceTimes
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // E. Training paces
            if (uiState.trainingPaces.isNotEmpty()) {
                item {
                    TrainingPaceZonesCard(
                        paces = uiState.trainingPaces
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
