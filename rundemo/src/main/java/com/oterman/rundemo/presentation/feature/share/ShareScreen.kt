package com.oterman.rundemo.presentation.feature.share

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.presentation.feature.share.components.LongShareEditSheet
import com.oterman.rundemo.presentation.feature.share.components.LongSharePreview
import com.oterman.rundemo.presentation.feature.share.components.ShareBottomBar
import com.oterman.rundemo.presentation.feature.share.components.ShortShareEditSheet
import com.oterman.rundemo.presentation.feature.share.components.ShortSharePreview
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.util.DeviceNameUtils

/**
 * 分享主页面
 * 顶部模式切换 + 中间预览区 + 底部固定「编辑 + 分享」按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    workoutId: String,
    onNavigateBack: () -> Unit,
    viewModel: ShareViewModel = viewModel(
        factory = ShareViewModelFactory(LocalContext.current, workoutId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 错误提示
    LaunchedEffect(uiState.shareError) {
        uiState.shareError?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (uiState.record != null) {
                        ShareModeSelector(
                            currentMode = uiState.shareMode,
                            onModeSelected = { viewModel.setShareMode(it) }
                        )
                    } else {
                        Text("分享跑步")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        bottomBar = {
            ShareBottomBar(
                isGenerating = uiState.isGenerating,
                onEditClick = { viewModel.showEditSheet() },
                onShareClick = { viewModel.generateAndShare(context) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "加载失败",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                uiState.record != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 预览区域（可滚动）
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.background)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp)
                        ) {
                            when (uiState.shareMode) {
                                ShareMode.SHORT -> {
                                    ShortSharePreview(
                                        record = uiState.record!!,
                                        mapSnapshot = uiState.mapSnapshot,
                                        selectedMetrics = uiState.selectedMetrics,
                                        showDate = uiState.showDate,
                                        deviceName = uiState.customDeviceName
                                            ?: DeviceNameUtils.resolveDisplayName(uiState.record!!),
                                        brandText = uiState.brandText,
                                        avatarUrl = uiState.avatarUrl,
                                        userName = uiState.userName
                                    )
                                }

                                ShareMode.LONG -> {
                                    LongSharePreview(
                                        record = uiState.record!!,
                                        mapSnapshot = uiState.mapSnapshot,
                                        metrics = uiState.metrics,
                                        enabledCards = uiState.enabledCards,
                                        segments = uiState.segments,
                                        trainingSegments = uiState.trainingSegments,
                                        mergedTrainingSegments = uiState.mergedTrainingSegments,
                                        heartRateSeries = uiState.heartRateSeries,
                                        speedSeries = uiState.speedSeries,
                                        cadenceSeries = uiState.cadenceSeries,
                                        powerSeries = uiState.powerSeries,
                                        strideLengthSeries = uiState.strideLengthSeries,
                                        verticalOscillationSeries = uiState.verticalOscillationSeries,
                                        contactTimeSeries = uiState.contactTimeSeries,
                                        altitudeSeries = uiState.altitudeSeries,
                                        heartRate7Zones = uiState.heartRate7Zones,
                                        heartRate5Zones = uiState.heartRate5Zones,
                                        speedZones = uiState.speedZones,
                                        vo2Max = uiState.vo2Max,
                                        previousVo2Max = uiState.previousVo2Max,
                                        showDate = uiState.showDate,
                                        deviceName = uiState.customDeviceName
                                            ?: DeviceNameUtils.resolveDisplayName(uiState.record!!),
                                        brandText = uiState.brandText,
                                        avatarUrl = uiState.avatarUrl
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 编辑面板
    if (uiState.showEditSheet && uiState.record != null) {
        when (uiState.shareMode) {
            ShareMode.SHORT -> {
                ShortShareEditSheet(
                    selectedMetrics = uiState.selectedMetrics,
                    availableMetrics = uiState.availableMetrics,
                    deviceName = uiState.customDeviceName
                        ?: DeviceNameUtils.resolveDisplayName(uiState.record!!) ?: "",
                    showDate = uiState.showDate,
                    brandText = uiState.brandText,
                    onMetricsChanged = { viewModel.updateSelectedMetrics(it) },
                    onDeviceNameChanged = { viewModel.updateDeviceName(it) },
                    onShowDateChanged = { viewModel.updateShowDate(it) },
                    onBrandTextChanged = { viewModel.updateBrandText(it) },
                    onDismiss = { viewModel.hideEditSheet() }
                )
            }

            ShareMode.LONG -> {
                LongShareEditSheet(
                    enabledCards = uiState.enabledCards,
                    availableCards = viewModel.getAvailableCards(),
                    deviceName = uiState.customDeviceName
                        ?: DeviceNameUtils.resolveDisplayName(uiState.record!!) ?: "",
                    showDate = uiState.showDate,
                    brandText = uiState.brandText,
                    onCardToggle = { type, enabled -> viewModel.toggleCard(type, enabled) },
                    onDeviceNameChanged = { viewModel.updateDeviceName(it) },
                    onShowDateChanged = { viewModel.updateShowDate(it) },
                    onBrandTextChanged = { viewModel.updateBrandText(it) },
                    onDismiss = { viewModel.hideEditSheet() }
                )
            }
        }
    }
}

/**
 * 模式选择器（短图/长图切换），用于 TopAppBar title 区域
 */
@Composable
private fun ShareModeSelector(
    currentMode: ShareMode,
    onModeSelected: (ShareMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = ShareMode.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = currentMode == mode,
                onClick = { onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = RunBlue.copy(alpha = 0.15f),
                    activeContentColor = RunBlue,
                    activeBorderColor = RunBlue
                ),
                label = { Text(mode.displayName) }
            )
        }
    }
}
