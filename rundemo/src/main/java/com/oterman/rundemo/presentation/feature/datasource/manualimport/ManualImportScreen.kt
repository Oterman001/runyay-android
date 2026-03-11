package com.oterman.rundemo.presentation.feature.datasource.manualimport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualImportScreen(
    viewModel: ManualImportViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRecordList: () -> Unit,
    onNavigateToRunDetail: (String) -> Unit,
    onNavigateToDebug: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // 从记录列表返回时刷新计数
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadRecords()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val fitLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFitFiles(uris)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val clickTimestamps = remember { mutableListOf<Long>() }
                    Text(
                        text = "手动导入",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            val now = System.currentTimeMillis()
                            clickTimestamps.add(now)
                            // 只保留最近500ms内的点击
                            clickTimestamps.removeAll { now - it > 500L }
                            if (clickTimestamps.size >= 5) {
                                clickTimestamps.clear()
                                onNavigateToDebug()
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 导入入口卡片
            item {
                ImportEntryCard(
                    isImporting = uiState.isImporting,
                    onImportClick = { fitLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // 数据冲突说明 Banner
            item {
                ConflictInfoBanner(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 查看已导入记录入口
            item {
                ViewRecordsEntry(
                    recordCount = uiState.records.size,
                    isLoading = uiState.isLoadingRecords,
                    onClick = onNavigateToRecordList,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }

    // 导入进度弹窗（不可关闭）
    if (uiState.isImporting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("导入中") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(uiState.importProgress ?: "正在处理...")
                }
            },
            confirmButton = {}
        )
    }

    // 导入结果弹窗
    uiState.importResult?.let { result ->
        ImportResultDialog(
            result = result,
            onDismiss = { viewModel.dismissResult() },
            onViewDetail = { workoutId ->
                viewModel.dismissResult()
                onNavigateToRunDetail(workoutId)
            }
        )
    }
}

@Composable
private fun ImportEntryCard(
    isImporting: Boolean,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "导入 FIT 文件",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "支持同时选择多个文件批量导入",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = onImportClick,
                enabled = !isImporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isImporting) "导入中..." else "选择文件",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 数据冲突说明 Banner（可展开/折叠）
 */
@Composable
private fun ConflictInfoBanner(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "关于数据时段重叠",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "折叠" else "展开",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "若手动导入的记录与其他数据源（如佳明、高驰等）的记录存在时间重叠，系统将按数据源优先级规则自动处理冲突。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "排序靠后的记录可能不会被纳入统计与分析，但数据仍会保留。可在数据源管理页面中调整各数据源的优先级顺序。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "时段重叠时，部分记录可能不参与统计与分析",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 26.dp)
            )
        }
    }
}

/**
 * 查看已导入记录入口卡片
 */
@Composable
private fun ViewRecordsEntry(
    recordCount: Int,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "查看已导入记录",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when {
                        isLoading -> "加载中..."
                        recordCount == 0 -> "暂无记录"
                        else -> "共 $recordCount 条记录"
                    },
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

@Composable
private fun ImportResultDialog(
    result: ManualImportResult,
    onDismiss: () -> Unit,
    onViewDetail: (String) -> Unit
) {
    when (result) {
        is ManualImportResult.SingleSuccess -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("导入成功") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("已成功导入跑步记录")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "距离：${String.format("%.2f", result.distance / 1000.0)} 公里",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "时长：${String.format("%.1f", result.duration / 60.0)} 分钟",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    if (result.workoutId.isNotEmpty()) {
                        TextButton(onClick = { onViewDetail(result.workoutId) }) {
                            Text("查看详情")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            )
        }

        is ManualImportResult.SingleAlreadyExists -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("文件已存在") },
                text = { Text("该文件之前已导入过，无需重复导入。") },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("确定") }
                }
            )
        }

        is ManualImportResult.SingleError -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("导入失败") },
                text = { Text(result.message) },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("确定") }
                }
            )
        }

        is ManualImportResult.BatchComplete -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("批量导入完成") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (result.successCount > 0) {
                            Text(
                                text = "成功导入：${result.successCount} 个",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (result.skipCount > 0) {
                            Text(
                                text = "已存在跳过：${result.skipCount} 个",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (result.failures.isNotEmpty()) {
                            Text(
                                text = "导入失败：${result.failures.size} 个",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            result.failures.forEach { (fileName, reason) ->
                                Text(
                                    text = "· $fileName：$reason",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        if (result.successCount == 0 && result.skipCount == 0 && result.failures.isEmpty()) {
                            Text(
                                text = "没有文件被处理，请检查所选文件。",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("确定") }
                }
            )
        }
    }
}
