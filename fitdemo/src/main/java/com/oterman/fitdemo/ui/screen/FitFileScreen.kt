package com.oterman.fitdemo.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oterman.fitdemo.data.model.FitSummaryData
import com.oterman.fitdemo.data.model.UiState
import com.oterman.fitdemo.ui.components.InfoCard
import com.oterman.fitdemo.ui.components.InfoRow
import com.oterman.fitdemo.ui.components.LapDataTable
import com.oterman.fitdemo.ui.components.SectionHeader
import com.oterman.fitdemo.ui.theme.ComopseDemoHubTheme

/**
 * FIT文件解析主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitFileScreen(
    uiState: UiState,
    onSelectFile: () -> Unit,
    onShowMap: (FitSummaryData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FIT文件解析") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is UiState.Idle -> IdleContent(onSelectFile)
                is UiState.Loading -> LoadingContent()
                is UiState.Success -> SuccessContent(uiState.data, onSelectFile, onShowMap)
                is UiState.Error -> ErrorContent(uiState.message, onSelectFile)
            }
        }
    }
}

/**
 * 空闲状态内容
 */
@Composable
private fun IdleContent(onSelectFile: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "请选择一个FIT文件进行解析",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onSelectFile) {
                Text("选择FIT文件")
            }
        }
    }
}

/**
 * 加载中状态内容
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "正在解析FIT文件...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 成功状态内容
 */
@Composable
private fun SuccessContent(
    data: FitSummaryData,
    onSelectFile: () -> Unit,
    onShowMap: (FitSummaryData) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 选择新文件按钮
        item {
            Button(
                onClick = onSelectFile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("选择其他文件")
            }
        }
        
        // 查看地图按钮（仅当有GPS数据时显示）
        if (data.trackPoints.isNotEmpty()) {
            item {
                Button(
                    onClick = { onShowMap(data) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("查看地图轨迹 (${data.trackPoints.size} 个轨迹点)")
                }
            }
        }
        
        // 文件信息
        data.fileInfo?.let { fileInfo ->
            item {
                InfoCard {
                    SectionHeader("文件信息")
                    InfoRow("文件类型", fileInfo.type)
                    InfoRow("制造商", fileInfo.manufacturer)
                    InfoRow("产品", fileInfo.product)
                    InfoRow("序列号", fileInfo.serialNumber)
                    InfoRow("创建时间", fileInfo.timeCreated)
                    InfoRow("文件编号", fileInfo.number?.toString())
                }
            }
        }
        
        // 会话摘要
        data.sessionSummary?.let { session ->
            item {
                InfoCard {
                    SectionHeader("会话摘要")
                    InfoRow("运动类型", session.sport)
                    InfoRow("子类型", session.subSport)
                    InfoRow("开始时间", session.startTime)
                    InfoRow("总用时", session.totalElapsedTime)
                    InfoRow("计时时间", session.totalTimerTime)
                    InfoRow("总距离", session.totalDistance)
                    InfoRow("总卡路里", session.totalCalories)
                    InfoRow("平均速度", session.avgSpeed)
                    InfoRow("最大速度", session.maxSpeed)
                    InfoRow("平均配速", session.avgPace)
                    InfoRow("最快配速", session.maxPace)
                    InfoRow("平均心率", session.avgHeartRate)
                    InfoRow("最大心率", session.maxHeartRate)
                    InfoRow("平均步频", session.avgCadence)
                    InfoRow("最大步频", session.maxCadence)
                    InfoRow("总上升", session.totalAscent)
                    InfoRow("总下降", session.totalDescent)
                    InfoRow("平均步幅", session.avgStride)
                }
            }
        }
        
        // 轨迹信息
        item {
            InfoCard {
                SectionHeader("轨迹信息")
                InfoRow("记录点数量", data.trackInfo.totalRecords.toString())
                InfoRow("包含GPS数据", if (data.trackInfo.hasGpsData) "是" else "否")
                InfoRow("包含心率数据", if (data.trackInfo.hasHeartRateData) "是" else "否")
                InfoRow("包含步频数据", if (data.trackInfo.hasCadenceData) "是" else "否")
            }
        }
        
        // 区间数据 - 使用表格展示
        if (data.laps.isNotEmpty()) {
            item {
                InfoCard {
                    LapDataTable(laps = data.laps)
                }
            }
        }
        
        // 设备信息
        data.deviceInfo?.let { device ->
            item {
                InfoCard {
                    SectionHeader("设备信息")
                    InfoRow("制造商", device.manufacturer)
                    InfoRow("产品", device.product)
                    InfoRow("序列号", device.serialNumber)
                    InfoRow("设备类型", device.deviceType)
                    InfoRow("硬件版本", device.hardwareVersion)
                    InfoRow("软件版本", device.softwareVersion)
                }
            }
        }
        
        // 底部空白
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 错误状态内容
 */
@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "解析失败",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text("重新选择文件")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FitFileScreenIdlePreview() {
    ComopseDemoHubTheme {
        FitFileScreen(
            uiState = UiState.Idle,
            onSelectFile = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FitFileScreenLoadingPreview() {
    ComopseDemoHubTheme {
        FitFileScreen(
            uiState = UiState.Loading,
            onSelectFile = {}
        )
    }
}

