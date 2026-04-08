package com.oterman.rundemo.presentation.feature.rundetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.data.local.entity.RunAbilityZoneEntity
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.local.entity.RunSamplePointEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 跑步记录调试详情页
 * 展示跑步记录的完整数据信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailDebugScreen(
    workoutId: String,
    onNavigateBack: () -> Unit,
    viewModel: RunDetailDebugViewModel = viewModel(
        factory = RunDetailDebugViewModelFactory(LocalContext.current, workoutId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val uploadActionState by viewModel.uploadActionState.collectAsState()
    val forceUploadState by viewModel.forceUploadState.collectAsState()
    val vo2MaxUpdateState by viewModel.vo2MaxUpdateState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("跑步详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is RunDetailDebugUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is RunDetailDebugUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            is RunDetailDebugUiState.Success -> {
                RunDetailContent(
                    data = state.data,
                    uploadActionState = uploadActionState,
                    onUpload = { viewModel.uploadRecord(state.data.record) },
                    forceUploadState = forceUploadState,
                    onForceUpload = { viewModel.forceUploadRecord(state.data.record) },
                    vo2MaxUpdateState = vo2MaxUpdateState,
                    onUpdateVo2Max = { vo2Max, summaryId ->
                        viewModel.updateVo2MaxToServer(vo2Max, summaryId)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

/**
 * 详情页主内容
 */
@Composable
private fun RunDetailContent(
    data: RunDetailFullData,
    uploadActionState: UploadActionState,
    onUpload: () -> Unit,
    forceUploadState: UploadActionState,
    onForceUpload: () -> Unit,
    vo2MaxUpdateState: Vo2MaxUpdateState,
    onUpdateVo2Max: (Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 摘要卡片
        item {
            SummaryCard(record = data.record)
        }
        
        // 基础信息
        item {
            DataSection(
                title = "基础信息",
                initiallyExpanded = true
            ) {
                BasicInfoContent(record = data.record)
            }
        }
        
        // 配速心率数据
        item {
            DataSection(
                title = "配速心率",
                initiallyExpanded = true
            ) {
                PaceHeartRateContent(record = data.record)
            }
        }
        
        // 步频步幅数据
        item {
            DataSection(
                title = "步频步幅",
                initiallyExpanded = false
            ) {
                CadenceStrideContent(record = data.record)
            }
        }
        
        // 训练效果
        item {
            DataSection(
                title = "训练效果",
                initiallyExpanded = false
            ) {
                TrainingEffectContent(record = data.record, vo2Max = data.vo2Max)
            }
        }
        
        // 设备与来源
        item {
            DataSection(
                title = "设备与来源",
                initiallyExpanded = false
            ) {
                DeviceSourceContent(record = data.record)
            }
        }

        // 手动上传操作
        item {
            UploadActionCard(
                record = data.record,
                uploadActionState = uploadActionState,
                onUpload = onUpload,
                forceUploadState = forceUploadState,
                onForceUpload = onForceUpload,
                vo2Max = data.vo2Max,
                vo2MaxUpdateState = vo2MaxUpdateState,
                onUpdateVo2Max = onUpdateVo2Max
            )
        }

        // 公里分段
        if (data.segments.isNotEmpty()) {
            item {
                DataSection(
                    title = "分段数据 (${data.segments.size}段)",
                    initiallyExpanded = false
                ) {
                    SegmentsContent(segments = data.segments)
                }
            }
        }
        
        // 能力区间
        if (data.zones.isNotEmpty()) {
            item {
                DataSection(
                    title = "能力区间 (${data.zones.size}个)",
                    initiallyExpanded = false
                ) {
                    ZonesContent(zones = data.zones)
                }
            }
        }
        
        // 采样点统计
        item {
            DataSection(
                title = "采样点数据",
                initiallyExpanded = false
            ) {
                SamplePointsContent(
                    samplePoints = data.samplePoints,
                    samplePointCount = data.samplePointCount,
                    trackPointCount = data.trackPointCount
                )
            }
        }
        
        // 底部间距
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 摘要卡片
 */
@Composable
private fun SummaryCard(record: RunRecordEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 日期时间
            Text(
                text = formatDate(record.startTime),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 距离（大字）
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format("%.2f", record.totalDistance),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "公里",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 四个关键指标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryMetric(
                    icon = Icons.Default.Timer,
                    label = "时长",
                    value = formatDuration(record.activeDuration)
                )
                SummaryMetric(
                    icon = Icons.Default.Speed,
                    label = "配速",
                    value = formatPace(record.averageSpeed)
                )
                SummaryMetric(
                    icon = Icons.Default.FavoriteBorder,
                    label = "心率",
                    value = if (record.averageHeartRate > 0) "${record.averageHeartRate.toInt()}" else "-"
                )
                SummaryMetric(
                    icon = Icons.Default.DirectionsRun,
                    label = "步频",
                    value = if (record.averageCadence > 0) "${record.averageCadence.toInt()}" else "-"
                )
            }
        }
    }
}

/**
 * 摘要指标项
 */
@Composable
private fun SummaryMetric(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
    }
}

/**
 * 可展开的数据区块
 */
@Composable
private fun DataSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // 标题栏（可点击展开/收起）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 内容区域（动画展开/收起）
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    content()
                }
            }
        }
    }
}

/**
 * 数据行
 */
@Composable
private fun DataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 基础信息内容
 */
@Composable
private fun BasicInfoContent(record: RunRecordEntity) {
    Column {
        DataRow("workoutId", record.workoutId)
        DataRow("开始时间", formatDateTime(record.startTime))
        DataRow("结束时间", formatDateTime(record.endTime))
        DataRow("总时长", "${String.format("%.2f", record.duration)} 分钟")
        DataRow("运动时长", "${String.format("%.2f", record.activeDuration)} 分钟")
        DataRow("总距离", "${String.format("%.3f", record.totalDistance)} 公里")
        DataRow("原始距离", "${String.format("%.3f", record.originDistance)} 公里")
        DataRow("室内/室外", if (record.outdoor == 0) "室外" else "室内")
        DataRow("地点", record.address ?: "-")
        DataRow("备注", record.note ?: "-")
    }
}

/**
 * 配速心率内容
 */
@Composable
private fun PaceHeartRateContent(record: RunRecordEntity) {
    Column {
        DataRow("平均配速", formatPace(record.averageSpeed))
        DataRow("最快配速", formatPace(record.maxSpeed))
        DataRow("平均心率", "${record.averageHeartRate.toInt()} bpm")
        DataRow("最高心率", "${record.maxHeartRate.toInt()} bpm")
        DataRow("最低心率", "${record.minHeartRate.toInt()} bpm")
        DataRow("平均功率", "${record.averagePower.toInt()} W")
        DataRow("最大功率", "${record.maxPower.toInt()} W")
    }
}

/**
 * 步频步幅内容
 */
@Composable
private fun CadenceStrideContent(record: RunRecordEntity) {
    Column {
        DataRow("平均步频", "${record.averageCadence.toInt()} spm")
        DataRow("平均步幅", "${String.format("%.1f", record.averageStrideLength)} cm")
        DataRow("垂直振幅", "${String.format("%.1f", record.averageVerticalOscillation)} cm")
        DataRow("触地时间", "${String.format("%.0f", record.averageContactTime)} ms")
        DataRow("总步数", "${record.totalStepCount.toInt()}")
        DataRow("累计爬升", "${String.format("%.0f", record.elevationAscended)} 米")
        DataRow("总卡路里", "${String.format("%.0f", record.totalCalories)} kcal")
    }
}

/**
 * 训练效果内容
 */
@Composable
private fun TrainingEffectContent(record: RunRecordEntity, vo2Max: Double? = null) {
    Column {
        DataRow("VDOT", String.format("%.1f", record.vdot))
        DataRow("整体VDOT", String.format("%.1f", record.overallVdot))
        DataRow("有氧训练效果", String.format("%.1f", record.trainingEffect))
        DataRow("无氧训练效果", String.format("%.1f", record.anaerobicTrainingEffect))
        DataRow("训练负荷", String.format("%.0f", record.trainingLoad))
        DataRow("感受等级", "${record.feelingLevel}")
        DataRow("最大摄氧量", vo2Max?.let { String.format("%.1f", it) } ?: "-")
    }
}

/**
 * 设备与来源内容
 */
@Composable
private fun DeviceSourceContent(record: RunRecordEntity) {
    Column {
        DataRow("设备品牌", record.deviceInfo ?: "-")
        DataRow("设备型号", record.deviceVersion ?: "-")
        DataRow("数据来源", record.datasource ?: "-")
        DataRow("原始ID", record.originId ?: "-")
        DataRow("数据优先级", "${record.inclusiveLevel}")
        DataRow("轨迹状态", when (record.trajectoryStatus) {
            0 -> "未知"
            1 -> "存在"
            2 -> "不存在"
            else -> "${record.trajectoryStatus}"
        })
        DataRow("上传状态", when (record.uploadStatus) {
            0 -> "未上传"
            1 -> "上传中"
            2 -> "成功"
            3 -> "失败"
            else -> "${record.uploadStatus}"
        })
        DataRow("关联训练计划", record.trainPlanId ?: "-")
        DataRow("关联跑鞋", record.shoeId ?: "-")
        DataRow("关联赛事", record.linkedRaceRecordId ?: "-")
    }
}

/**
 * 手动上传操作卡片
 */
@Composable
private fun UploadActionCard(
    record: RunRecordEntity,
    uploadActionState: UploadActionState,
    onUpload: () -> Unit,
    forceUploadState: UploadActionState = UploadActionState.Idle,
    onForceUpload: () -> Unit = {},
    vo2Max: Double? = null,
    vo2MaxUpdateState: Vo2MaxUpdateState = Vo2MaxUpdateState.Idle,
    onUpdateVo2Max: (Double, String) -> Unit = { _, _ -> }
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "上传操作",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

            val statusText = when (record.uploadStatus) {
                0 -> "未上传"
                1 -> "上传中"
                2 -> "已上传"
                3 -> "上传失败"
                else -> "${record.uploadStatus}"
            }
            DataRow("上传状态", statusText)

            if (uploadActionState is UploadActionState.Error) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "错误：${uploadActionState.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (record.uploadStatus != 2) {
                Spacer(modifier = Modifier.height(12.dp))
                val isLoading = uploadActionState is UploadActionState.Loading
                Button(
                    onClick = onUpload,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isLoading) "上传中..." else "手动上传")
                }
            }

            // 强制上传（始终显示）
            Spacer(modifier = Modifier.height(8.dp))
            if (forceUploadState is UploadActionState.Error) {
                Text(
                    text = "强制上传失败：${forceUploadState.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
            } else if (forceUploadState is UploadActionState.Success) {
                Text(
                    text = "强制上传成功",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            val isForceLoading = forceUploadState is UploadActionState.Loading
            Button(
                onClick = onForceUpload,
                enabled = !isForceLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isForceLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isForceLoading) "上传中..." else "强制上传摘要")
            }

            // VO2Max 更新按钮：仅当 vo2Max 有值且有 originId 时显示
            if (vo2Max != null && record.originId != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                DataRow("最大摄氧量", String.format("%.1f", vo2Max))

                when (vo2MaxUpdateState) {
                    is Vo2MaxUpdateState.Error -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "更新失败：${vo2MaxUpdateState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is Vo2MaxUpdateState.Success -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "VO2Max 已更新到服务器",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(8.dp))
                val isVo2MaxLoading = vo2MaxUpdateState is Vo2MaxUpdateState.Loading
                Button(
                    onClick = { onUpdateVo2Max(vo2Max, record.originId) },
                    enabled = !isVo2MaxLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isVo2MaxLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isVo2MaxLoading) "更新中..." else "更新 VO2Max 到服务器")
                }
            }
        }
    }
}

/**
 * 分段数据内容
 */
@Composable
private fun SegmentsContent(segments: List<RunSegmentEntity>) {
    Column {
        segments.forEachIndexed { index, segment ->
            if (index > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            Text(
                text = "第 ${segment.seq + 1} 段 (${if (segment.segmentType == 1) "公里" else "训练"})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            DataRow("距离", "${String.format("%.3f", segment.distance)} km")
            DataRow("配速", formatPace(segment.averageSpeed))
            DataRow("心率", "${segment.averageHeartRate.toInt()} bpm")
            DataRow("步频", "${segment.averageCadence.toInt()} spm")
            DataRow("时长", "${String.format("%.1f", segment.activeDuration)} 分钟")
            segment.intervalType?.let {
                DataRow("类型", it)
            }
        }
    }
}

/**
 * 能力区间内容
 */
@Composable
private fun ZonesContent(zones: List<RunAbilityZoneEntity>) {
    Column {
        zones.forEachIndexed { index, zone ->
            if (index > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            Text(
                text = "区间 ${zone.zoneIndex + 1} (类型: ${zone.zoneType})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            DataRow("时长", "${String.format("%.1f", zone.duration)} 分钟")
            DataRow("最小值", String.format("%.1f", zone.minValue))
            DataRow("最大值", String.format("%.1f", zone.maxValue))
        }
    }
}

/**
 * 采样点内容
 */
@Composable
private fun SamplePointsContent(
    samplePoints: List<RunSamplePointEntity>,
    samplePointCount: Int,
    trackPointCount: Int
) {
    Column {
        DataRow("采样点总数", "$samplePointCount 个")
        DataRow("GPS轨迹点数", "$trackPointCount 个")
        
        if (samplePoints.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "采样点列表 (显示前50个)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 表头
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text("序号", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("时间(s)", Modifier.width(50.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("心率", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("配速", Modifier.width(50.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("步频", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("GPS", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
            
            // 采样点行（只显示前50个）
            samplePoints.take(50).forEach { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("${point.sequence}", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall)
                    Text("${point.timeOffset}", Modifier.width(50.dp), style = MaterialTheme.typography.labelSmall)
                    Text("${point.heartRate ?: "-"}", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall)
                    Text(point.speed?.let { formatPace(it) } ?: "-", Modifier.width(50.dp), style = MaterialTheme.typography.labelSmall)
                    Text("${point.cadence ?: "-"}", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall)
                    Text(
                        if (point.latitude != null) "✓" else "-",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.End
                    )
                }
            }
            
            if (samplePoints.size > 50) {
                Text(
                    text = "... 还有 ${samplePoints.size - 50} 个采样点",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ==================== 工具函数 ====================

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE)
    return format.format(date)
}

private fun formatDateTime(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return format.format(date)
}

private fun formatPace(paceMinPerKm: Double): String {
    if (paceMinPerKm <= 0) return "-"
    val minutes = paceMinPerKm.toInt()
    val seconds = ((paceMinPerKm - minutes) * 60).toInt()
    return "${minutes}'${seconds.toString().padStart(2, '0')}\""
}

private fun formatDuration(durationMinutes: Double): String {
    if (durationMinutes <= 0) return "-"
    val totalSeconds = (durationMinutes * 60).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

