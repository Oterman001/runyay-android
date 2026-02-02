package com.oterman.rundemo.presentation.feature.rundetail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.presentation.feature.rundetail.components.RunDetailDataGrid
import com.oterman.rundemo.presentation.feature.rundetail.components.RunDetailHeaderCard
import com.oterman.rundemo.presentation.feature.rundetail.components.RunDetailMapSection
import com.oterman.rundemo.presentation.feature.rundetail.components.RunDetailSegmentTable

/**
 * 跑步详情页面
 * 用户友好的详情展示，包含地图、头部卡片、数据网格和分段表格
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

    // 显示错误消息
    LaunchedEffect(uiState.downloadError) {
        uiState.downloadError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearDownloadState()
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
                    // 下载按钮（仅当可以下载时显示）
                    if (uiState.canDownloadFit) {
                        IconButton(
                            onClick = { viewModel.downloadFitFile() },
                            enabled = !uiState.isDownloading
                        ) {
                            if (uiState.isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = if (scrollOffset < 0.5f) Color.White
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "下载FIT文件",
                                    tint = if (scrollOffset < 0.5f) Color.White
                                           else MaterialTheme.colorScheme.onSurface
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

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 地图区域（占据60%屏幕高度）
                        item {
                            RunDetailMapSection(
                                trackPoints = uiState.trackPoints,
                                isOutdoor = uiState.isOutdoor
                            )
                        }

                        // 头部卡片（向上侵入地图区域）
                        item {
                            RunDetailHeaderCard(
                                distance = record.totalDistance,
                                startTime = record.startTime,
                                endTime = record.endTime,
                                duration = record.activeDuration,
                                deviceName = record.deviceVersion,
                                isOutdoor = uiState.isOutdoor
                            )
                        }

                        // 间距
                        item {
                            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                        }

                        // 数据网格
                        item {
                            RunDetailDataGrid(
                                metrics = uiState.metrics
                            )
                        }

                        // 间距
                        item {
                            Spacer(modifier = Modifier.height(RunDetailLayoutConstants.CardSpacing.dp))
                        }

                        // 公里分段表格
                        if (uiState.segments.isNotEmpty()) {
                            item {
                                RunDetailSegmentTable(
                                    segments = uiState.segments
                                )
                            }
                        }

                        // 底部留白
                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }
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
