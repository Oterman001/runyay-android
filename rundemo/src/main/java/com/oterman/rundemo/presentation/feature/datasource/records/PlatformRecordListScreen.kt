package com.oterman.rundemo.presentation.feature.datasource.records

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.presentation.components.EditInclusiveLevelDialog
import com.oterman.rundemo.presentation.feature.home.components.RunRecordItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformRecordListScreen(
    viewModel: PlatformRecordListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRunDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val trackPointsVersion by viewModel.trackPointsVersion.collectAsState()
    val isManual = uiState.platform == DataSourcePlatform.MANUAL
    var pendingInclusiveLevelRecord by remember { mutableStateOf<RunRecordEntity?>(null) }

    val selectedCount = uiState.selectedWorkoutIds.size

    // 选择模式下拦截返回键退出选择模式
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.exitSelectionMode()
    }

    val topBarTitle = when {
        uiState.isSelectionMode && selectedCount > 0 -> "已选 $selectedCount 项"
        uiState.isSelectionMode -> "选择记录"
        isManual -> "手动导入记录"
        else -> "${uiState.platform.displayName} 的数据"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "取消选择"
                            )
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                },
                actions = {
                    if (isManual && uiState.records.isNotEmpty()) {
                        if (uiState.isSelectionMode) {
                            // 全选按钮
                            val allSelected = selectedCount == uiState.records.size
                            IconButton(onClick = {
                                if (allSelected) viewModel.exitSelectionMode().also { viewModel.enterSelectionMode() }
                                else viewModel.selectAll()
                            }) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = if (allSelected) "取消全选" else "全选"
                                )
                            }
                            // 删除按钮
                            TextButton(
                                onClick = { viewModel.requestBatchDelete() },
                                enabled = selectedCount > 0 && !uiState.isBatchDeleting
                            ) {
                                Text(
                                    text = if (selectedCount > 0) "删除($selectedCount)" else "删除",
                                    color = if (selectedCount > 0 && !uiState.isBatchDeleting)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        } else {
                            TextButton(onClick = { viewModel.enterSelectionMode() }) {
                                Text("选择")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading || uiState.isBatchDeleting -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        if (uiState.isBatchDeleting) {
                            Text(
                                text = "正在删除，请稍候...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }

            uiState.records.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无数据",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // MANUAL 平台专属：冲突说明紧凑提示条
                    if (isManual && !uiState.isSelectionMode) {
                        item {
                            ConflictNoteBar(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    item {
                        Text(
                            text = "共 ${uiState.records.size} 条记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    items(
                        items = uiState.records,
                        key = { it.workoutId }
                    ) { record ->
                        key(record.workoutId, trackPointsVersion) {
                            if (uiState.isSelectionMode) {
                                val isSelected = record.workoutId in uiState.selectedWorkoutIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleSelection(record.workoutId) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleSelection(record.workoutId) },
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                    Box(modifier = Modifier.weight(1f)) {
                                        val trackPoints = viewModel.getCachedTrackPoints(record.workoutId)
                                        val isLoading = viewModel.isTrackPointsLoading(record.workoutId)
                                        RunRecordItem(
                                            record = record,
                                            trackPoints = trackPoints,
                                            isTrackPointsLoading = isLoading,
                                            onClick = { viewModel.toggleSelection(record.workoutId) },
                                            onInclusiveLevelClick = null
                                        )
                                    }
                                }
                            } else {
                                val trackPoints = viewModel.getCachedTrackPoints(record.workoutId)
                                val isLoading = viewModel.isTrackPointsLoading(record.workoutId)
                                RunRecordItem(
                                    record = record,
                                    trackPoints = trackPoints,
                                    isTrackPointsLoading = isLoading,
                                    onClick = { onNavigateToRunDetail(record.workoutId) },
                                    onInclusiveLevelClick = { pendingInclusiveLevelRecord = record }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // EditInclusiveLevelDialog
    pendingInclusiveLevelRecord?.let { record ->
        EditInclusiveLevelDialog(
            currentLevel = record.inclusiveLevel,
            onDismiss = { pendingInclusiveLevelRecord = null },
            onConfirm = { newLevel ->
                viewModel.updateInclusiveLevel(record, newLevel)
                pendingInclusiveLevelRecord = null
            }
        )
    }

    // 批量删除确认对话框
    if (uiState.showBatchDeleteConfirm) {
        BatchDeleteConfirmDialog(
            count = selectedCount,
            onDismiss = { viewModel.dismissBatchDeleteConfirm() },
            onConfirm = { viewModel.confirmBatchDelete() }
        )
    }
}

@Composable
private fun BatchDeleteConfirmDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "确认删除")
        },
        text = {
            Text(text = "确定要删除选中的 $count 条记录吗？\n删除后将同步至服务器，此操作不可撤销。")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "删除",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * MANUAL 平台冲突说明紧凑提示条（可展开/折叠）
 */
@Composable
private fun ConflictNoteBar(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 1.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "数据时段重叠说明",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (expanded) {
                Text(
                    text = "若手动导入的记录与其他数据源的记录存在时间重叠，系统将按数据源优先级规则处理冲突。排序靠后的记录可能不会被纳入统计与分析。可在数据源管理中调整数据源优先级。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else {
                Text(
                    text = "时段重叠时，部分记录可能不参与统计",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "折叠" else "展开",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
