package com.oterman.rundemo.presentation.feature.datasource.records

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.DataSourcePlatform
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

    val title = if (isManual) "手动导入记录" else "${uiState.platform.displayName} 的数据"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold
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
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
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
                    if (isManual) {
                        item {
                            ConflictNoteBar(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    item {
                        Text(
                            text = "共 ${uiState.records.size} 条记录" + if (isManual) "（左滑可删除）" else "",
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
                            if (isManual) {
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.requestDelete(record.workoutId)
                                            true
                                        } else false
                                    },
                                    positionalThreshold = { it * 0.4f }
                                )

                                val isPendingDelete = uiState.pendingDeleteWorkoutId == record.workoutId
                                LaunchedEffect(isPendingDelete) {
                                    if (!isPendingDelete && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                        dismissState.reset()
                                    }
                                }

                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = true,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            targetValue = when (dismissState.targetValue) {
                                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            label = "swipe_bg"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(color),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除",
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(end = 24.dp)
                                            )
                                        }
                                    }
                                ) {
                                    val trackPoints = viewModel.getCachedTrackPoints(record.workoutId)
                                    val isLoading = viewModel.isTrackPointsLoading(record.workoutId)
                                    RunRecordItem(
                                        record = record,
                                        trackPoints = trackPoints,
                                        isTrackPointsLoading = isLoading,
                                        onClick = { onNavigateToRunDetail(record.workoutId) }
                                    )
                                }
                            } else {
                                val trackPoints = viewModel.getCachedTrackPoints(record.workoutId)
                                val isLoading = viewModel.isTrackPointsLoading(record.workoutId)
                                RunRecordItem(
                                    record = record,
                                    trackPoints = trackPoints,
                                    isTrackPointsLoading = isLoading,
                                    onClick = { onNavigateToRunDetail(record.workoutId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // MANUAL 平台删除确认弹窗
    if (isManual && uiState.pendingDeleteWorkoutId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("删除记录") },
            text = { Text("确定要删除这条手动导入的记录吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    colors = androidx.compose.material3.ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("取消")
                }
            }
        )
    }
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
