package com.oterman.rundemo.presentation.feature.datasource

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.BuildConfig
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.presentation.feature.datasource.components.DataSourceItem
import com.oterman.rundemo.presentation.feature.home.FitImportResult
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * 数据源管理页面
 * 对应iOS的DataSourceManagePage
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSourceManageScreen(
    viewModel: DataSourceManageViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (DataSourcePlatform) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToHkDebug: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveDataSource(from.index, to.index)
    }

    // 单个FIT文件选择器
    val singleFitLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importSingleFitFile(it) }
    }

    // 批量FIT文件选择器
    val batchFitLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importBatchFitFiles(uris)
        }
    }

    // 每次屏幕显示时刷新数据源状态（处理从详情页授权返回的情况）
    LaunchedEffect(Unit) {
        viewModel.refreshDataSources()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "数据源管理",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isEditingOrder) {
                            viewModel.cancelEditingOrder()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (uiState.isEditingOrder) {
                        TextButton(onClick = { viewModel.saveOrder() }) {
                            Text(
                                text = "保存",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 加载状态
            if (uiState.isLoading && uiState.dataSources.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.isEditingOrder) {
                        // 编辑模式：仅显示可排序数据源，包裹在 ReorderableItem 中支持拖拽
                        itemsIndexed(
                            items = uiState.sortableDataSources,
                            key = { _, item -> item.platform.code }
                        ) { _, dataSourceInfo ->
                            ReorderableItem(reorderState, key = dataSourceInfo.platform.code) { isDragging ->
                                DataSourceItem(
                                    dataSourceInfo = dataSourceInfo,
                                    isEditMode = true,
                                    showPriority = true,
                                    isDragging = isDragging,
                                    dragHandleModifier = Modifier.draggableHandle()
                                )
                            }
                        }

                        item {
                            PriorityExplanationCard()
                        }
                    } else {
                        // 正常模式：显示全部数据源
                        itemsIndexed(
                            items = uiState.displayDataSources,
                            key = { _, item -> item.platform.code }
                        ) { _, dataSourceInfo ->
                            DataSourceItem(
                                dataSourceInfo = dataSourceInfo,
                                isEditMode = false,
                                showPriority = dataSourceInfo.platform.supportsSorting,
                                onClick = {
                                    when {
                                        viewModel.isComingSoonDataSource(dataSourceInfo.platform) -> {
                                            viewModel.showComingSoonDialog()
                                        }
                                        viewModel.isManualImportDataSource(dataSourceInfo.platform) -> {
                                            viewModel.showManualImportDialog()
                                        }
                                        else -> {
                                            onNavigateToDetail(dataSourceInfo.platform)
                                        }
                                    }
                                }
                            )
                        }

                        // Debug 专属：HK - 苹果健康调试入口
                        if (BuildConfig.DEBUG) {
                            item(key = "hk_debug_entry") {
                                HkDebugEntry(onClick = onNavigateToHkDebug)
                            }
                        }
                    }
                }

                // 底部按钮区域（非编辑模式）
                AnimatedVisibility(
                    visible = !uiState.isEditingOrder,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    BottomButtonSection(
                        onEditOrderClick = { viewModel.startEditingOrder() }
                    )
                }
            }
        }
    }

    // 手动导入选择弹窗
    if (uiState.showManualImportDialog) {
        ManualImportDialog(
            onSingleImport = {
                viewModel.dismissManualImportDialog()
                singleFitLauncher.launch(arrayOf("*/*"))
            },
            onBatchImport = {
                viewModel.dismissManualImportDialog()
                batchFitLauncher.launch(arrayOf("*/*"))
            },
            onDismiss = { viewModel.dismissManualImportDialog() }
        )
    }

    // 导入进度提示
    if (uiState.isImportingFit) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("导入中") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(uiState.importProgress ?: "导入中...")
                }
            },
            confirmButton = {}
        )
    }

    // 即将支持弹窗
    if (uiState.showComingSoonDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissComingSoonDialog() },
            title = { Text("即将支持") },
            text = { Text("该功能即将上线，敬请期待") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissComingSoonDialog() }) {
                    Text("确定")
                }
            }
        )
    }

    // 需要登录弹窗
    if (uiState.showLoginRequiredDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLoginRequiredDialog() },
            title = { Text("需要登录") },
            text = { Text("该功能需要登录后才能使用，请先登录您的账户") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissLoginRequiredDialog()
                    onNavigateToLogin()
                }) {
                    Text("去登录")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLoginRequiredDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    // 导入结果弹窗
    if (uiState.showImportResultDialog) {
        ManageScreenFitImportResultDialog(
            result = uiState.fitImportResult,
            onDismiss = { viewModel.dismissImportResultDialog() }
        )
    }
}

/**
 * 手动导入选择弹窗
 */
@Composable
private fun ManualImportDialog(
    onSingleImport: () -> Unit,
    onBatchImport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手动导入") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "请选择导入方式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 单个导入
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSingleImport),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "导入单个FIT文件",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "选择一个FIT文件进行导入",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // 批量导入
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onBatchImport),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "批量导入FIT文件",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "选择多个FIT文件一次性导入",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 导入结果弹窗（数据源管理页面用）
 */
@Composable
private fun ManageScreenFitImportResultDialog(
    result: FitImportResult?,
    onDismiss: () -> Unit
) {
    val (title, message) = when (result) {
        is FitImportResult.Success -> {
            if (result.distance == 0.0 && result.duration == 0.0) {
                // 批量导入的汇总结果
                "导入完成" to "批量导入已完成"
            } else {
                "导入成功" to "已成功导入跑步记录\n距离：${String.format("%.2f", result.distance)} 公里\n时长：${String.format("%.1f", result.duration)} 分钟"
            }
        }
        is FitImportResult.AlreadyExists -> {
            "文件已存在" to "所有文件之前已导入过，无需重复导入"
        }
        is FitImportResult.Error -> {
            "导入失败" to result.message
        }
        is FitImportResult.UploadFailed -> {
            "上传失败" to result.message
        }
        is FitImportResult.ConflictFound -> return
        null -> return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

/**
 * Debug 专属 HK 苹果健康调试入口
 */
@Composable
private fun HkDebugEntry(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HK - 苹果健康（调试）",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Debug 专属入口",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 优先级说明卡片
 */
@Composable
private fun PriorityExplanationCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "数据源优先级",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "长按拖动调整顺序",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "排在前面的数据源优先级更高。当多个数据源有冲突的运动数据时，会优先采用排序靠前的数据源。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 底部按钮区域
 */
@Composable
private fun BottomButtonSection(
    onEditOrderClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onEditOrderClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "调整数据源优先级",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = "碰到问题？联系我们",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(0.7f)
        )
    }
}
