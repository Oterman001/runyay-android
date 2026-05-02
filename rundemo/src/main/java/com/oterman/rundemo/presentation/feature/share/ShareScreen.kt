package com.oterman.rundemo.presentation.feature.share

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import com.oterman.rundemo.presentation.feature.share.components.ShareTemplateGallery
import com.oterman.rundemo.presentation.feature.share.components.ShortShareEditSheet
import com.oterman.rundemo.presentation.feature.share.components.ShortSharePreview
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.BuildConfig
import com.oterman.rundemo.util.DeviceNameUtils

/**
 * 分享主页面
 * 顶部模式切换 + 中间预览区 + 底部固定「编辑 + 分享」按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    workoutId: String,
    segmentBarChartMode: Boolean = false,
    segmentMetricIndex: Int = 0,
    segmentGroupSize: Int = 1,
    onNavigateBack: () -> Unit,
    viewModel: ShareViewModel = viewModel(
        factory = ShareViewModelFactory(LocalContext.current, workoutId, segmentBarChartMode, segmentMetricIndex, segmentGroupSize)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val snackbarHostState = remember { SnackbarHostState() }

    // 错误提示
    LaunchedEffect(uiState.shareError) {
        uiState.shareError?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            val result = snackbarHostState.showSnackbar(
                message = "图片已保存到相册",
                actionLabel = "查看",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                uiState.savedImageUri?.let { uri ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                }
            }
            viewModel.clearSaveState()
        }
    }

    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearSaveState()
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
            val isTemplateMode = uiState.shareMode == ShareMode.TEMPLATE
            ShareBottomBar(
                isGenerating = uiState.isGenerating,
                isSaving = uiState.isSaving,
                canEdit = !isTemplateMode,
                canExport = !isTemplateMode,
                onEditClick = { viewModel.showEditSheet() },
                onSaveClick = { viewModel.generateAndSave(context, isDark) },
                onShareClick = { viewModel.generateAndShare(context, isDark) }
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
                                ShareMode.TEMPLATE -> {
                                    ShareTemplateGallery(
                                        templates = uiState.templates,
                                        selectedTemplateId = uiState.selectedTemplateId,
                                        onTemplateSelected = { viewModel.selectTemplate(it) }
                                    )
                                }

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
                                        userId = uiState.userId,
                                        userName = uiState.userName,
                                        showNickname = uiState.showNickname,
                                        isPrivacyMode = uiState.isPrivacyMode,
                                        trackPoints = uiState.trackPoints,
                                        isIndoor = !uiState.isOutdoor
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
                                        avatarUrl = uiState.avatarUrl,
                                        userId = uiState.userId,
                                        userName = uiState.userName,
                                        showNickname = uiState.showNickname,
                                        linkedShoe = uiState.linkedShoe,
                                        isPrivacyMode = uiState.isPrivacyMode,
                                        trackPoints = uiState.trackPoints,
                                        heartRateZone7Selected = uiState.heartRateZone7Selected,
                                        onHeartRateZoneChanged = { viewModel.updateHeartRateZoneMode(it) },
                                        isIndoor = !uiState.isOutdoor,
                                        segmentBarChartMode = uiState.segmentBarChartMode,
                                        segmentMetricIndex = uiState.segmentBarChartMetricIndex,
                                        segmentGroupSize = uiState.segmentBarChartGroupSize,
                                        onSegmentBarChartModeChanged = { viewModel.updateSegmentBarChartMode(it) },
                                        onSegmentMetricIndexChanged = { viewModel.updateSegmentBarChartMetricIndex(it) }
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
                    showNickname = uiState.showNickname,
                    brandText = uiState.brandText,
                    onMetricsChanged = { viewModel.updateSelectedMetrics(it) },
                    onDeviceNameChanged = { viewModel.updateDeviceName(it) },
                    onShowDateChanged = { viewModel.updateShowDate(it) },
                    onShowNicknameChanged = { viewModel.updateShowNickname(it) },
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
                    showNickname = uiState.showNickname,
                    brandText = uiState.brandText,
                    onCardToggle = { type, enabled -> viewModel.toggleCard(type, enabled) },
                    onDeviceNameChanged = { viewModel.updateDeviceName(it) },
                    onShowDateChanged = { viewModel.updateShowDate(it) },
                    onShowNicknameChanged = { viewModel.updateShowNickname(it) },
                    onBrandTextChanged = { viewModel.updateBrandText(it) },
                    onDismiss = { viewModel.hideEditSheet() }
                )
            }

            ShareMode.TEMPLATE -> Unit
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
    val modes = if (BuildConfig.DEBUG) ShareMode.entries
                else ShareMode.entries.filter { it != ShareMode.TEMPLATE }
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
                icon = {},
                label = { Text(mode.displayName) }
            )
        }
    }
}
