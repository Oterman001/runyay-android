package com.oterman.rundemo.presentation.feature.rundetail

import android.net.Uri
import com.oterman.rundemo.domain.model.DataSourcePlatform
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.oterman.rundemo.presentation.components.EditInclusiveLevelDialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.presentation.feature.rundetail.components.AltitudeChartCard
import com.oterman.rundemo.presentation.feature.rundetail.components.CadenceChartCard
import com.oterman.rundemo.presentation.feature.rundetail.components.ContactTimeChartCard
import com.oterman.rundemo.presentation.feature.rundetail.components.HeartRateChartCard
import com.oterman.rundemo.presentation.feature.rundetail.components.PaceChartCard
import com.oterman.rundemo.presentation.feature.rundetail.components.PowerChartCard
import com.oterman.rundemo.data.map.MapRendererFactory
import com.oterman.rundemo.domain.map.MapCameraState
import com.oterman.rundemo.presentation.feature.rundetail.components.RunDetailHeaderDataCard
import com.oterman.rundemo.presentation.feature.rundetail.components.RunDetailMapSection
import com.oterman.rundemo.presentation.feature.rundetail.components.RunDetailSegmentTable
import com.oterman.rundemo.presentation.feature.rundetail.components.RunDetailTrainingSegmentTable
import com.oterman.rundemo.presentation.feature.rundetail.components.RunDetailWeatherOverlay
import com.oterman.rundemo.presentation.feature.rundetail.components.StrideLengthChartCard
import com.oterman.rundemo.presentation.feature.rundetail.components.TrainingEffectCard
import com.oterman.rundemo.presentation.feature.rundetail.components.RunDetailShoeCard
import com.oterman.rundemo.presentation.feature.rundetail.components.ShoeSelectionSheet
import com.oterman.rundemo.presentation.feature.rundetail.components.VO2MaxCard
import com.oterman.rundemo.presentation.feature.rundetail.components.VerticalOscillationChartCard
import com.oterman.rundemo.presentation.feature.share.ShareActivity
import com.oterman.rundemo.presentation.feature.share.ShareDataCache
import com.oterman.rundemo.util.AppleWatchDeviceUtils
import com.oterman.rundemo.BuildConfig

/**
 * 跑步详情页面
 * 对标 iOS RunRecordDetailPageV3
 *
 * 页面内容顺序：
 * 1. 地图区域 + 天气覆盖层
 * 2. Header + DataGrid 合并卡片（含VDOT/跑力，最多12项指标）
 * 3. 最大摄氧量卡片（条件显示，来自daily_health表）
 * 4. 训练效果卡片（条件显示）
 * 5. 公里分段表格
 * 6. 训练分段表格（条件显示）
 * 7. 心率图表 + 心率区间（单卡，5/7区间切换）
 * 8. 配速图表 + 配速区间（单卡）
 * 9. 海拔图表（条件显示）
 * 10. 步幅图表（条件显示）
 * 11. 步频图表（条件显示）
 * 12. 触地时间图表（条件显示）
 * 13. 垂直振幅图表（条件显示）
 * 14. 功率图表（条件显示）
 * 15. 关联跑鞋卡片
 * 16. 数据来源标签
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailScreen(
    workoutId: String,
    onNavigateBack: () -> Unit,
    viewModel: RunDetailViewModel = viewModel(
        factory = RunDetailViewModelFactory(LocalContext.current, workoutId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 持有 MapView 引用用于分享时截图（View 类型，供应商无关）
    var mapViewRef by remember { mutableStateOf<android.view.View?>(null) }

    // 右上角三点菜单展开状态
    var showMenu by remember { mutableStateOf(false) }

    // SAF文件选择器 - 使用 */* 确保文件名后缀被保留
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri?.let {
            uiState.downloadedFitData?.let { data ->
                saveFitFile(context, uri, data)
                viewModel.clearDownloadState()
            }
        } ?: viewModel.clearDownloadState()
    }

    // 下载完成后触发文件选择
    LaunchedEffect(uiState.downloadSuccess, uiState.downloadedFitData) {
        if (uiState.downloadSuccess && uiState.downloadedFitData != null) {
            createDocumentLauncher.launch(viewModel.getDefaultFileName())
        }
    }

    // 分享准备完成后跳转 ShareActivity
    LaunchedEffect(uiState.shareDataReady) {
        if (uiState.shareDataReady) {
            val record = uiState.record
            if (record != null) {
                context.startActivity(ShareActivity.createIntent(context, record.workoutId))
            }
            viewModel.clearShareState()
        }
    }

    // 显示错误消息
    LaunchedEffect(uiState.downloadError) {
        uiState.downloadError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearDownloadState()
        }
    }

    // 删除成功后返回上一页
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            viewModel.clearDeleteState()
            onNavigateBack()
        }
    }

    // 删除失败显示 Snackbar
    LaunchedEffect(uiState.deleteError) {
        uiState.deleteError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearDeleteState()
        }
    }

    // 更新成功反馈
    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            snackbarHostState.showSnackbar("保存成功")
            viewModel.clearUpdateState()
        }
    }

    // 更新失败反馈
    LaunchedEffect(uiState.updateError) {
        uiState.updateError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearUpdateState()
        }
    }

    // 根据滚动位置决定导航栏透明度
    val scrollOffset by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (lazyListState.firstVisibleItemScrollOffset / 300f).coerceIn(0f, 1f)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = scrollOffset > 0.5f,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text("跑步详情")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = if (scrollOffset < 0.5f) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // 三点菜单按钮
                    if (uiState.record != null && !uiState.isLoading) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多操作",
                                tint = if (scrollOffset < 0.5f) Color.White
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {

                            if (BuildConfig.DEBUG) {
                                // 修正距离
                                DropdownMenuItem(
                                    text = { Text("修正距离") },
                                    onClick = { showMenu = false; viewModel.showEditDistanceDialog() }
                                )
                                // 分享
                                DropdownMenuItem(
                                    text = {
                                        if (uiState.isPreparingShare) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("分享")
                                        }
                                    },
                                    enabled = !uiState.isPreparingShare,
                                    onClick = {
                                        showMenu = false
                                        val mv = mapViewRef
                                        if (mv != null && uiState.isOutdoor) {
                                            viewModel.setPreparingShare(true)
                                            val provider =
                                                com.oterman.rundemo.presentation.feature.rundetail.components.RunMapPreferences.getMapProvider(context)
                                            val renderer = MapRendererFactory.getRenderer(provider)
                                            renderer.snapshot(mv) { bitmap ->
                                                bitmap?.let { ShareDataCache.putMapSnapshot(it) }
                                                viewModel.prepareShareData()
                                            }
                                        } else {
                                            viewModel.prepareShareData()
                                        }
                                    }
                                )
                            }
                            // 下载FIT
                            if (uiState.canDownloadFit) {
                                DropdownMenuItem(
                                    text = {
                                        if (uiState.isDownloading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("下载FIT")
                                        }
                                    },
                                    enabled = !uiState.isDownloading,
                                    onClick = {
                                        showMenu = false
                                        viewModel.downloadFitFile()
                                    }
                                )
                            }

                            val isManualImport = uiState.record?.datasource == DataSourcePlatform.MANUAL.code
                            if (BuildConfig.DEBUG || isManualImport) {

                                // 删除记录
                                DropdownMenuItem(
                                    text = { Text("删除记录", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.showDeleteConfirmation()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = scrollOffset),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // 删除确认对话框
        if (uiState.showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteDialog() },
                title = { Text("删除跑步记录") },
                text = { Text("确认删除该跑步记录？此操作不可撤销，将同时删除云端数据。") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteRunRecord() }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDeleteDialog() }) { Text("取消") }
                }
            )
        }

        // 修正距离对话框
        if (uiState.showEditDistanceDialog) {
            val record = uiState.record
            val baseline = if (record != null && record.originDistance > 0) record.originDistance else record?.totalDistance ?: 0.0
            AlertDialog(
                onDismissRequest = { viewModel.dismissEditDistanceDialog() },
                title = { Text("修正距离") },
                text = {
                    Column {
                        Text(
                            "当前距离：${String.format("%.2f", record?.totalDistance ?: 0.0)} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "原始距离：${String.format("%.2f", baseline)} km（允许 ±10%）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.editDistanceInput,
                            onValueChange = { viewModel.onEditDistanceInputChanged(it) },
                            label = { Text("新距离 (km)") },
                            isError = uiState.editDistanceError != null,
                            supportingText = uiState.editDistanceError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                },
                confirmButton = { TextButton(onClick = { viewModel.confirmEditDistance() }) { Text("确认") } },
                dismissButton = { TextButton(onClick = { viewModel.dismissEditDistanceDialog() }) { Text("取消") } }
            )
        }

        // 跑鞋选择 BottomSheet
        if (uiState.showShoeSelector) {
            ShoeSelectionSheet(
                shoes = uiState.availableShoes,
                currentShoeId = uiState.record?.shoeId,
                onSelect = { viewModel.changeShoe(it) },
                onDismiss = { viewModel.dismissShoeSelector() }
            )
        }

        // 修改 inclusiveLevel 对话框
        if (uiState.showEditInclusiveLevelDialog) {
            EditInclusiveLevelDialog(
                currentLevel = uiState.record?.inclusiveLevel ?: 1,
                onDismiss = { viewModel.dismissEditInclusiveLevelDialog() },
                onConfirm = { viewModel.confirmEditInclusiveLevel(it) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    // 加载状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    // 错误状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "加载失败",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                uiState.record != null -> {
                    // 成功状态 - 显示详情内容
                    val record = uiState.record!!

                    // 保存地图相机状态，在 LazyColumn 外部 remember，
                    // 避免地图 item 被 dispose 后相机状态丢失
                    var savedCameraState by remember { mutableStateOf<MapCameraState?>(null) }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // ==================== 1. 地图区域 + 天气 ====================
                        item {
                            Box {
                                RunDetailMapSection(
                                    trackPoints = uiState.trackPoints,
                                    isOutdoor = uiState.isOutdoor,
                                    actualDistanceKm = record.totalDistance,
                                    savedCameraState = savedCameraState,
                                    onCameraChanged = { savedCameraState = it },
                                    onMapViewReady = { mapViewRef = it }
                                )

                                // 天气覆盖层（左下角）
                                if (record.weatherTemperature != 0.0 || record.weatherHumidity > 0) {
                                    RunDetailWeatherOverlay(
                                        temperature = record.weatherTemperature,
                                        humidity = record.weatherHumidity,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(start = 12.dp, bottom = 25.dp)
                                    )
                                }
                            }
                        }

                        // ==================== 2. Header + DataGrid 合并卡片 ====================
                        item {
                            RunDetailHeaderDataCard(
                                distance = record.totalDistance,
                                startTime = record.startTime,
                                endTime = record.endTime,
                                duration = record.activeDuration,
                                deviceName = when {
                                    record.datasource == "HK" && record.deviceInfo?.contains("apple watch", ignoreCase = true) == true ->
                                        AppleWatchDeviceUtils.getModelName(record.deviceVersion)

                                    record.datasource == DataSourcePlatform.MANUAL.code ->
                                        if (record.deviceInfo.isNullOrBlank()) "Manual" else "Manual-${record.deviceInfo}"

                                    else -> record.deviceInfo
                                },
                                isOutdoor = uiState.isOutdoor,
                                metrics = uiState.metrics,
                                avatarUrl = uiState.avatarUrl,
                                isLoadingAvatar = uiState.isLoadingAvatar,
                                userName = uiState.userName,
                                inclusiveLevel = record.inclusiveLevel,
                                onInclusiveLevelClick = {
                                    viewModel.showEditInclusiveLevelDialog()
                                },
                                modifier = Modifier
                                    .layout { measurable, constraints ->
                                        val placeable = measurable.measure(constraints)
                                        val invasionPx = RunDetailLayoutConstants.HeaderInvasionOffset.dp
                                            .roundToPx().let { kotlin.math.abs(it) }
                                        layout(placeable.width, placeable.height - invasionPx) {
                                            placeable.placeRelative(0, -invasionPx)
                                        }
                                    }
                            )
                        }

                        // 间距
                        item {
                            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                        }

                        // ==================== 3. 最大摄氧量卡片 ====================
                        val vo2Max = uiState.vo2Max
                        if (vo2Max != null && vo2Max > 0) {
                            item {
                                VO2MaxCard(
                                    vo2Max = vo2Max,
                                    previousVo2Max = uiState.previousVo2Max
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                            }
                        }

                        // ==================== 4. 训练效果卡片 ====================
                        if (record.trainingEffect > 0 || record.anaerobicTrainingEffect > 0) {
                            item {
                                TrainingEffectCard(
                                    aerobicEffect = record.trainingEffect,
                                    anaerobicEffect = record.anaerobicTrainingEffect
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                            }
                        }

                        // ==================== 5. 公里分段表格 ====================
                        if (uiState.segments.isNotEmpty()) {
                            item {
                                RunDetailSegmentTable(
                                    segments = uiState.segments
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                            }
                        }

                        // ==================== 6. 训练分段表格 ====================
                        if (uiState.trainingSegments.isNotEmpty()) {
                            item {
                                RunDetailTrainingSegmentTable(
                                    segments = uiState.trainingSegments,
                                    mergedSegments = uiState.mergedTrainingSegments,
                                    expandedSegmentIds = uiState.expandedSegmentIds,
                                    onToggleExpansion = { viewModel.toggleSegmentExpansion(it) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                            }
                        }

                        // ==================== 7. 心率图表 + 区间（单卡 + 5/7区间切换） ====================
                        if (uiState.heartRateSeries.isNotEmpty()) {
                            item {
                                HeartRateChartCard(
                                    heartRateSeries = uiState.heartRateSeries,
                                    heartRate7Zones = uiState.heartRate7Zones,
                                    heartRate5Zones = uiState.heartRate5Zones,
                                    avgHeartRate = record.averageHeartRate,
                                    maxHeartRate = record.maxHeartRate,
                                    minHeartRate = record.minHeartRate
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                            }
                        }

                        // ==================== 8. 配速图表 + 区间（单卡） ====================
                        if (uiState.speedSeries.isNotEmpty()) {
                            item {
                                PaceChartCard(
                                    speedSeries = uiState.speedSeries,
                                    speedZones = uiState.speedZones,
                                    avgSpeed = record.averageSpeed,
                                    maxSpeed = record.maxSpeed
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                            }
                        }

                        // ==================== 9. 海拔图表 ====================
                        if (uiState.altitudeSeries.isNotEmpty()) {
                            item {
                                AltitudeChartCard(
                                    altitudeSeries = uiState.altitudeSeries,
                                    elevationAscended = record.elevationAscended
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                            }
                        }

                        // ==================== 10. 步幅图表 ====================
                        if (uiState.strideLengthSeries.isNotEmpty()) {
                            item {
                                StrideLengthChartCard(
                                    strideLengthSeries = uiState.strideLengthSeries,
                                    avgStrideLength = record.averageStrideLength
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                            }
                        }

                        // ==================== 11. 步频图表 ====================
                        if (uiState.cadenceSeries.isNotEmpty()) {
                            item {
                                CadenceChartCard(
                                    cadenceSeries = uiState.cadenceSeries,
                                    avgCadence = record.averageCadence
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                            }
                        }

                        // ==================== 12. 触地时间图表 ====================
                        if (uiState.contactTimeSeries.isNotEmpty()) {
                            item {
                                ContactTimeChartCard(
                                    contactTimeSeries = uiState.contactTimeSeries,
                                    avgContactTime = record.averageContactTime
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                            }
                        }

                        // ==================== 13. 垂直振幅图表 ====================
                        if (uiState.verticalOscillationSeries.isNotEmpty()) {
                            item {
                                VerticalOscillationChartCard(
                                    verticalOscillationSeries = uiState.verticalOscillationSeries,
                                    avgVerticalOscillation = record.averageVerticalOscillation
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                            }
                        }

                        // ==================== 14. 功率图表 ====================
                        if (uiState.powerSeries.isNotEmpty()) {
                            item {
                                PowerChartCard(
                                    powerSeries = uiState.powerSeries,
                                    avgPower = record.averagePower
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                            }
                        }

                        // ==================== 15. 关联跑鞋 ====================
                        item {
                            RunDetailShoeCard(
                                shoe = uiState.linkedShoe,
                                onClick = { viewModel.showShoeSelector() },
                                onRemove = { viewModel.removeShoe() }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                        }

                        // ==================== 16. 数据来源标签 ====================
                        item {
                            DataSourceLabel(
                                datasource = record.datasource
                            )
                        }

                        // 底部留白
                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }

            // 删除中全屏遮罩
            if (uiState.isDeleting) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/**
 * 数据来源标签
 * 底部居中显示数据来源信息
 */
@Composable
private fun DataSourceLabel(
    datasource: String?,
    modifier: Modifier = Modifier
) {
    val sourceText = if (datasource == null) {
        "本地数据"
    } else {
        val platform = DataSourcePlatform.fromCode(datasource)
        if (platform != null) "数据来源: ${platform.displayNameEn}(${platform.displayName})"
        else "数据来源: $datasource"
    }

    Text(
        text = sourceText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        fontSize = 12.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 32.dp)
    )
}

/**
 * 保存FIT文件到用户选择的位置
 */
private fun saveFitFile(context: android.content.Context, uri: Uri, data: ByteArray) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(data)
        }
    } catch (e: Exception) {
        // 错误处理 - 静默失败，因为用户已经选择了位置
    }
}
