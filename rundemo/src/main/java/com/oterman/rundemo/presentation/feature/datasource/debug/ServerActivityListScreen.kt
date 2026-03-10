package com.oterman.rundemo.presentation.feature.datasource.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.data.network.dto.RunSummaryBasicInfoDto
import com.oterman.rundemo.domain.model.UnifiedFileInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerActivityListScreen(
    viewModel: ServerActivityListViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "服务端活动列表 - ${uiState.platform.displayName}",
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
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.items.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.items.isEmpty() && !uiState.isLoading) {
                Text(
                    text = "暂无数据",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    items(uiState.items, key = { it.id }) { item ->
                        ActivityItemCard(
                            item = item,
                            isExpanded = item.id in uiState.expandedItemIds,
                            onToggleExpand = { viewModel.toggleExpand(item.id) },
                            snackbarHostState = snackbarHostState
                        )
                    }

                    // Pagination sentinel
                    item {
                        if (uiState.hasMorePages) {
                            LaunchedEffect(Unit) {
                                viewModel.loadNextPage()
                            }
                            if (uiState.isLoadingMore) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "没有更多数据",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ActivityItemCard(
    item: UnifiedFileInfo,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: id, dataDate, platformCode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "#${item.id}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item.platformCode,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            FieldRow("dataDate", item.dataDate)
            FieldRow("summaryId", item.summaryId)
            FieldRow("deviceName", item.deviceName)

            // ossUrl
            if (!item.ossUrl.isNullOrEmpty()) {
                UrlFieldRow("ossUrl", item.ossUrl, context, snackbarHostState)
            }

            // fitUrl
            if (!item.fitUrl.isNullOrEmpty()) {
                UrlFieldRow("fitUrl", item.fitUrl, context, snackbarHostState)
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            // RunSummary section
            if (item.hasRunSummary) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "基础信息 (runSummary)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    item.runSummary?.let { summary ->
                        RunSummaryContent(summary)
                    }
                }
            } else {
                Text(
                    text = "无基础信息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UrlFieldRow(
    label: String,
    url: String,
    context: Context,
    snackbarHostState: SnackbarHostState
) {
    val truncated = if (url.length > 50) {
        url.take(30) + "..." + url.takeLast(20)
    } else {
        url
    }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = truncated,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(label, url))
                scope.launch { snackbarHostState.showSnackbar("已复制") }
            },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "复制",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RunSummaryContent(summary: RunSummaryBasicInfoDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 4.dp)
    ) {
        // 基本信息
        SummarySection("基本信息") {
            SummaryField("activityType", summary.activityType)
            SummaryField("activityName", summary.activityName)
        }

        // 时间
        SummarySection("时间") {
            summary.startTimeInSeconds?.let { SummaryField("startTimeInSeconds", it.toString()) }
            summary.startTimeOffsetInSeconds?.let { SummaryField("startTimeOffsetInSeconds", it.toString()) }
            summary.durationInSeconds?.let { SummaryField("durationInSeconds", it.toString()) }
            summary.activeDuration?.let { SummaryField("activeDuration", it.toString()) }
        }

        // 距离配速
        SummarySection("距离配速") {
            summary.distanceInMeters?.let { SummaryField("distanceInMeters", it.toString()) }
            summary.averagePace?.let { SummaryField("averagePace", it.toString()) }
            summary.maxPace?.let { SummaryField("maxPace", it.toString()) }
        }

        // 心率
        SummarySection("心率") {
            summary.averageHeartRate?.let { SummaryField("averageHeartRate", it.toString()) }
            summary.maxHeartRate?.let { SummaryField("maxHeartRate", it.toString()) }
            summary.minHeartRate?.let { SummaryField("minHeartRate", it.toString()) }
            summary.vo2Max?.let { SummaryField("vo2Max", it.toString()) }
            summary.restingHeartRate?.let { SummaryField("restingHeartRate", it.toString()) }
        }

        // 功率
        SummarySection("功率") {
            summary.averagePower?.let { SummaryField("averagePower", it.toString()) }
            summary.maxPower?.let { SummaryField("maxPower", it.toString()) }
        }

        // 步频步幅
        SummarySection("步频步幅") {
            summary.averageCadence?.let { SummaryField("averageCadence", it.toString()) }
            summary.averageStrideLength?.let { SummaryField("averageStrideLength", it.toString()) }
        }

        // 跑步动态
        SummarySection("跑步动态") {
            summary.averageVerticalOscillation?.let { SummaryField("averageVerticalOscillation", it.toString()) }
            summary.averageContactTime?.let { SummaryField("averageContactTime", it.toString()) }
        }

        // 消耗
        SummarySection("消耗") {
            summary.activeKilocalories?.let { SummaryField("activeKilocalories", it.toString()) }
            summary.totalStepCount?.let { SummaryField("totalStepCount", it.toString()) }
            summary.totalElevationGain?.let { SummaryField("totalElevationGain", it.toString()) }
        }

        // 训练
        SummarySection("训练") {
            summary.vdot?.let { SummaryField("vdot", it.toString()) }
            summary.overallVdot?.let { SummaryField("overallVdot", it.toString()) }
            summary.trainingEffect?.let { SummaryField("trainingEffect", it.toString()) }
            summary.anaerobicTrainingEffect?.let { SummaryField("anaerobicTrainingEffect", it.toString()) }
            summary.trainingLoad?.let { SummaryField("trainingLoad", it.toString()) }
        }

        // 环境
        SummarySection("环境") {
            summary.weatherTemperature?.let { SummaryField("weatherTemperature", it.toString()) }
            summary.weatherHumidity?.let { SummaryField("weatherHumidity", it.toString()) }
            summary.outdoor?.let { SummaryField("outdoor", if (it == 0) "户外" else "室内") }
        }

        // 设备
        SummarySection("设备") {
            SummaryField("deviceInfo", summary.deviceInfo)
            SummaryField("deviceVersion", summary.deviceVersion)
            SummaryField("datasource", summary.datasource)
            SummaryField("originId", summary.originId)
        }

        // 其他
        SummarySection("其他") {
            SummaryField("note", summary.note)
            SummaryField("address", summary.address)
            SummaryField("userId", summary.userId)
            summary.feelingLevel?.let { SummaryField("feelingLevel", it.toString()) }
            summary.inclusiveLevel?.let { SummaryField("inclusiveLevel", it.toString()) }
            summary.trajectoryStatus?.let { SummaryField("trajectoryStatus", it.toString()) }
            summary.uploadStatus?.let { SummaryField("uploadStatus", it.toString()) }
            SummaryField("trainPlanId", summary.trainPlanId)
            SummaryField("shoeId", summary.shoeId)
            SummaryField("linkedRaceRecordId", summary.linkedRaceRecordId)
        }
    }
}

@Composable
private fun SummarySection(title: String, content: @Composable () -> Unit) {
    // Use a column to collect children, only render if there's content
    // Since we can't conditionally skip in Compose easily, we always render the section
    // but the SummaryField composable only renders non-null values
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
        )
        content()
    }
}

@Composable
private fun SummaryField(label: String, value: String?) {
    if (value.isNullOrEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(160.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
