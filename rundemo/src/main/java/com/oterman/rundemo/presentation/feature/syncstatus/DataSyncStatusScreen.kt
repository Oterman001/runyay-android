package com.oterman.rundemo.presentation.feature.syncstatus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.presentation.feature.home.components.StatisticsCard
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.service.sync.model.UnifiedSyncResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSyncStatusScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DataSyncStatusViewModel = viewModel(
        factory = DataSyncStatusViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "数据同步",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 同步状态头部
            SyncStatusHeaderCard(syncStatus = uiState.syncStatus)

            // 前台保持提示横幅（仅同步中显示）
            AnimatedVisibility(
                visible = uiState.syncStatus == SyncStatusType.SYNCING,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                KeepInForegroundBanner()
            }

            // 导入记录列表（有记录时显示）
            if (uiState.importedRecords.isNotEmpty()) {
                ImportedRecordsSection(records = uiState.importedRecords)
            }

            // 同步结果摘要（同步完成后显示）
            if (uiState.syncStatus == SyncStatusType.COMPLETED && uiState.syncResult != null) {
                SyncResultSummaryCard(result = uiState.syncResult!!)
            }
        }
    }
}

@Composable
private fun SyncStatusHeaderCard(syncStatus: SyncStatusType) {
    val isSyncing = syncStatus == SyncStatusType.SYNCING
    val infiniteTransition = rememberInfiniteTransition(label = "syncStatusRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncStatusAngle"
    )

    StatisticsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSyncing) Icons.Filled.Sync else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (isSyncing) RunTheme.colorScheme.blue else Color(0xFF34C759),
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        rotationZ = if (isSyncing) rotation else 0f
                    }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when (syncStatus) {
                    SyncStatusType.PREPARING -> "准备同步"
                    SyncStatusType.SYNCING -> "数据同步中"
                    SyncStatusType.COMPLETED -> "同步完成"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun KeepInForegroundBanner() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFFF3E0) // orange.opacity(0.1) equivalent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.FreeBreakfast,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "同步进行中，请保持应用在前台",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "建议您稍作休息，喝杯咖啡，数据马上就好",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ImportedRecordsSection(records: List<ImportedRecordItem>) {
    val listState = rememberLazyListState()

    // 自动滚动到最新记录
    LaunchedEffect(records.size) {
        if (records.isNotEmpty()) {
            listState.animateScrollToItem(records.size - 1)
        }
    }

    StatisticsCard {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Assignment,
                    contentDescription = null,
                    tint = RunTheme.colorScheme.blue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "导入记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "${records.size} 条",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 滚动记录列表（固定高度200dp）
        LazyColumn(
            state = listState,
            modifier = Modifier.height(200.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(records, key = { "${it.originId}_${records.indexOf(it)}" }) { record ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF34C759),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.displayText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 提示信息
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = RunTheme.colorScheme.blue,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "实时显示导入的运动记录",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SyncResultSummaryCard(result: UnifiedSyncResult) {
    val durationSeconds = result.durationMs / 1000.0
    val totalDataSources = result.successfulPlatforms + result.failedPlatforms

    StatisticsCard {
        // 标题
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.InsertChart,
                contentDescription = null,
                tint = RunTheme.colorScheme.blue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "同步摘要",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 摘要行
        SummaryRow(
            icon = Icons.Filled.Timer,
            title = "同步耗时",
            value = String.format("%.1f秒", durationSeconds),
            tintColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        SummaryRow(
            icon = Icons.Filled.Storage,
            title = "数据源",
            value = "$totalDataSources 个",
            tintColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        SummaryRow(
            icon = Icons.Filled.CheckCircle,
            title = "成功",
            value = "${result.successfulPlatforms} 个",
            tintColor = Color(0xFF34C759)
        )
        if (result.failedPlatforms > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            SummaryRow(
                icon = Icons.Filled.Close,
                title = "失败",
                value = "${result.failedPlatforms} 个",
                tintColor = Color(0xFFFF3B30)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        SummaryRow(
            icon = Icons.Filled.DirectionsRun,
            title = "导入记录",
            value = "${result.totalImportedCount} 条",
            tintColor = RunTheme.colorScheme.blue
        )
    }
}

@Composable
private fun SummaryRow(
    icon: ImageVector,
    title: String,
    value: String,
    tintColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = tintColor
        )
    }
}
