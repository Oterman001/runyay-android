package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.domain.model.DataTabDisplayMode
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.presentation.components.trajectory.TrajectoryThumbnail
import com.oterman.rundemo.presentation.feature.home.tabs.components.MonthSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data tab content with iOS-style NavigationTitle effect
 * Large title collapses to small title when scrolling
 * Supports monthly grouping with expand/collapse
 * Corresponds to iOS AllRunRecordPage
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DataTabContent(
    onRecordClick: (workoutId: String) -> Unit = {},
    onRecordLongClick: (workoutId: String) -> Unit = {},
    viewModel: DataTabViewModel = viewModel(
        factory = DataTabViewModelFactory(LocalContext.current)
    )
) {
    val hapticFeedback = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    val backgroundColor = MaterialTheme.colorScheme.background

    // Calculate collapse progress based on scroll offset
    val collapseProgress by remember {
        derivedStateOf {
            val firstItemIndex = lazyListState.firstVisibleItemIndex
            val firstItemOffset = lazyListState.firstVisibleItemScrollOffset

            if (firstItemIndex > 0) {
                1f
            } else {
                // Collapse over 80dp of scrolling
                (firstItemOffset / 200f).coerceIn(0f, 1f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Fixed header: 固定高度48dp，与其他Tab一致
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(backgroundColor)
                .zIndex(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 折叠后的小标题（滚动时渐显）
                Text(
                    text = "数据",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.alpha(collapseProgress)
                )

                // 模式切换按钮（始终显示，紧凑尺寸）
                DisplayModeToggleButton(
                    displayMode = uiState.displayMode,
                    onClick = { viewModel.toggleDisplayMode() },
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Main content
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            // Large title header (iOS NavigationTitle style)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 8.dp)
                        .graphicsLayer {
                            // Scale and fade out the large title as we scroll
                            val scale = 1f - (collapseProgress * 0.15f)
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - collapseProgress
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                ) {
                    Text(
                        text = "数据",
                        fontSize = 34.sp,  // iOS large title size
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 0.sp
                    )
                }
            }

            // Top statistics section (参考iOS AllRunTopView)
            item {
                AllRunTopView(
                    totalDistance = uiState.totalDistance,
                    totalRunCount = uiState.totalRunCount,
                    totalDuration = uiState.totalDuration,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // Loading state
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Error state
            uiState.error?.let { error ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Empty state
            if (!uiState.isLoading && uiState.error == null && uiState.monthGroups.isEmpty()) {
                item {
                    EmptyStateView()
                }
            }

            // Monthly grouped list
            uiState.monthGroups.forEach { monthData ->
                val monthId = "${monthData.year}-${monthData.month}"
                val isExpanded = viewModel.isMonthExpanded(monthId)

                // Month section with header
                item(key = "${monthId}_section") {
                    MonthSection(
                        monthData = monthData,
                        isExpanded = isExpanded,
                        displayMode = uiState.displayMode,
                        onToggleExpand = { viewModel.toggleMonthExpanded(monthId) },
                        content = {
                            // Records within this month (rendered when expanded)
                            val monthRecords = viewModel.getRecordsForMonth(monthData.year, monthData.month)
                            // 观察版本号，版本号变化时触发重组
                            val trackPointsVersion by viewModel.trackPointsVersion.collectAsState()
                            Column {
                                monthRecords.forEach { record ->
                                    // 使用 key 确保版本号变化时重组
                                    key(record.workoutId, trackPointsVersion) {
                                        // 获取缓存的轨迹点
                                        val trackPoints = viewModel.getCachedTrackPoints(record.workoutId)
                                        val isLoading = viewModel.isTrackPointsLoading(record.workoutId)
                                        RunRecordItem(
                                            record = record,
                                            trackPoints = trackPoints,
                                            isTrackPointsLoading = isLoading,
                                            onClick = { onRecordClick(record.workoutId) },
                                            onLongClick = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onRecordLongClick(record.workoutId)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 显示模式切换按钮
 */
@Composable
private fun DisplayModeToggleButton(
    displayMode: DataTabDisplayMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = when (displayMode) {
                DataTabDisplayMode.HEATMAP -> Icons.Default.List
                DataTabDisplayMode.SIMPLE -> Icons.Default.CalendarViewMonth
            },
            contentDescription = "切换显示模式",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyStateView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.DirectionsRun,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无跑步记录",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请在「我的」页面导入FIT文件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 跑步记录列表项
 * 参考iOS TrajectoryRunRecordView布局:
 * - 第一行: 日期时间 + 来源标签
 * - 第二行: 轨迹缩略图(80dp) + 数据区域
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RunRecordItem(
    record: RunRecordEntity,
    trackPoints: List<TrackPoint>?,
    isTrackPointsLoading: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    // 判断是否为室外跑 (outdoor=0表示室外，outdoor=1表示室内)
    val isOutdoor = record.outdoor == 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 第一行：日期时间 + 来源标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 日期时间（参考iOS: MM月dd日 HH:mm-HH:mm）
                Text(
                    text = formatDateCompact(record.startTime, record.endTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 来源标识
                record.datasource?.let { source ->
                    if (source.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = formatDatasource(source),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 第二行：轨迹缩略图 + 数据区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：轨迹缩略图
                TrajectoryThumbnail(
                    workoutId = record.workoutId,
                    trackPoints = trackPoints,
                    isLoading = isTrackPointsLoading,
                    isOutdoor = isOutdoor,
                    size = 80.dp,
                    cornerRadius = 8.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 右侧：数据区域
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 距离（大字）- 参考iOS 22号字体
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = String.format("%.2f", record.totalDistance),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "公里",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 时长 / 配速（参考iOS 13号字体）
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = formatDuration(record.activeDuration),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatPace(record.averageSpeed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 设备来源
                    record.deviceInfo?.let { device ->
                        if (device.isNotBlank()) {
                            Text(
                                text = device,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 格式化日期显示（完整格式）
 */
private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("yyyy年M月d日 EEEE HH:mm", Locale.CHINESE)
    return format.format(date)
}

/**
 * 格式化日期显示（紧凑格式）
 * 参考iOS: MM月dd日 HH:mm-HH:mm
 */
private fun formatDateCompact(startTime: Long, endTime: Long): String {
    val startDate = Date(startTime)
    val endDate = Date(endTime)
    val dateFormat = SimpleDateFormat("M月d日", Locale.CHINESE)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINESE)
    return "${dateFormat.format(startDate)} ${timeFormat.format(startDate)}-${timeFormat.format(endDate)}"
}

/**
 * 格式化配速 (min/km -> 5'30")
 */
private fun formatPace(paceMinPerKm: Double): String {
    if (paceMinPerKm <= 0) return "-"
    val minutes = paceMinPerKm.toInt()
    val seconds = ((paceMinPerKm - minutes) * 60).toInt()
    return "${minutes}'${seconds.toString().padStart(2, '0')}\""
}

/**
 * 格式化时长 (分钟 -> 1:05:30)
 */
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

/**
 * 格式化数据来源显示
 */
private fun formatDatasource(source: String): String {
    return when (source.uppercase()) {
        "GCN", "GARMIN" -> "Garmin"
        "COROS" -> "COROS"
        "GGB" -> "佳明国行"
        "FIT" -> "FIT文件"
        else -> source
    }
}

/**
 * 顶部统计信息视图
 * 参考iOS AllRunTopView实现
 */
@Composable
private fun AllRunTopView(
    totalDistance: Double,
    totalRunCount: Int,
    totalDuration: Double,  // 分钟
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AllRunItemView(
            value = String.format("%.1f", totalDistance),
            unit = "公里",
            description = "累计跑量"
        )
        AllRunItemView(
            value = totalRunCount.toString(),
            unit = "次",
            description = "总次数"
        )
        AllRunItemView(
            value = String.format("%.1f", totalDuration / 60),
            unit = "小时",
            description = "总耗时"
        )
    }
}

/**
 * 统计项视图
 * 参考iOS AllRunItemView实现
 */
@Composable
private fun AllRunItemView(
    value: String,
    unit: String,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = RunTheme.colorScheme.blue
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
