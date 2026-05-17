package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Watch
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.components.EditInclusiveLevelDialog
import com.oterman.rundemo.presentation.components.trajectory.BlendedTrajectoryThumbnail
import com.oterman.rundemo.presentation.feature.home.components.RunRecordItem
import com.oterman.rundemo.presentation.feature.home.tabs.components.MonthSection
import com.oterman.rundemo.ui.theme.RunYayFontFamily
import com.oterman.rundemo.ui.theme.RunYayFontFamily4
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    onNavigateToDataSourceManage: () -> Unit = {},
    viewModel: DataTabViewModel = viewModel(
        factory = DataTabViewModelFactory(LocalContext.current)
    )
) {
    val hapticFeedback = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    val backgroundColor = MaterialTheme.colorScheme.background
    var pendingInclusiveLevelRecord by remember { mutableStateOf<RunRecordEntity?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

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

    pendingInclusiveLevelRecord?.let { rec ->
        EditInclusiveLevelDialog(
            currentLevel = rec.inclusiveLevel,
            onDismiss = { pendingInclusiveLevelRecord = null },
            onConfirm = { viewModel.updateInclusiveLevel(rec, it); pendingInclusiveLevelRecord = null }
        )
    }

    if (showFilterSheet) {
        DataTabFilterSheet(
            selectedInclusiveLevels = uiState.selectedInclusiveLevels,
            selectedDatasources = uiState.selectedDatasources,
            availableDatasources = uiState.availableDatasources,
            hideEmptyMonths = uiState.hideEmptyMonths,
            selectedOutdoorTypes = uiState.selectedOutdoorTypes,
            onDismiss = { showFilterSheet = false },
            onApply = { levels, datasources, hideEmpty, outdoorTypes ->
                viewModel.applyFilter(levels, datasources, hideEmpty, outdoorTypes)
                showFilterSheet = false
            }
        )
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
                    text = "跑步记录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.alpha(collapseProgress)
                )

                Row {
                    // 过滤按钮
                    FilterButton(
                        isActive = uiState.isFilterActive,
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.size(40.dp)
                    )

                    // 模式切换按钮（始终显示，紧凑尺寸）
                    DisplayModeToggleButton(
                        displayMode = uiState.displayMode,
                        onClick = { viewModel.toggleDisplayMode() },
                        modifier = Modifier.size(40.dp)
                    )
                }
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
                        text = "跑步记录",
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
                    EmptyStateView(onBindDevice = onNavigateToDataSourceManage)
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
                                            },
                                            onInclusiveLevelClick = { pendingInclusiveLevelRecord = record },
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
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
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 过滤按钮（带激活角标）
 */
@Composable
private fun FilterButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Outlined.FilterAlt,
                contentDescription = "筛选",
                tint = if (isActive) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface
            )
        }
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyStateView(onBindDevice: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp)
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
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(
//                text = "绑定 Garmin、COROS 等设备自动同步跑步数据",
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
//            )
            Spacer(modifier = Modifier.height(24.dp))

            // 引导绑定按钮
            AppCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
//                    .background(
////                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
//                        shape = RoundedCornerShape(12.dp)
//                    )
                    .clickable(onClick = onBindDevice)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Watch,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "连接数据源",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "绑定运动手表，自动同步数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "前往绑定",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            }
        }
    }
}



/**
 * 格式化日期显示（完整格式）
 */
fun formatDate(timestamp: Long, activityTimeZone: String? = null): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("yyyy年M月d日 EEEE HH:mm", Locale.CHINESE).withActivityTimeZone(activityTimeZone)
    return format.format(date)
}

/**
 * 格式化日期显示（紧凑格式）
 * 参考iOS: MM月dd日 HH:mm-HH:mm
 */
fun formatDateCompact(startTime: Long, endTime: Long, activityTimeZone: String? = null): String {
    val startDate = Date(startTime)
    val endDate = Date(endTime)
    val dateFormat = SimpleDateFormat("M月d日", Locale.CHINESE).withActivityTimeZone(activityTimeZone)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINESE).withActivityTimeZone(activityTimeZone)
    return "${dateFormat.format(startDate)} ${timeFormat.format(startDate)}-${timeFormat.format(endDate)}"
}

/**
 * 格式化日期显示（带周几）
 * 示例: 2月28日 08:30 周六
 */
fun formatDateWithWeekday(startTime: Long, activityTimeZone: String? = null): String {
    val date = Date(startTime)
    val dateTimeFormat = SimpleDateFormat("M月d日 HH:mm", Locale.CHINESE).withActivityTimeZone(activityTimeZone)
    val weekdayFormat = SimpleDateFormat("EEEE", Locale.CHINESE).withActivityTimeZone(activityTimeZone)
    val weekdayFull = weekdayFormat.format(date) // e.g. 星期六
    val weekdayShort = weekdayFull.replace("星期", "周") // e.g. 周六
    return "${dateTimeFormat.format(date)} $weekdayShort"
}

private fun SimpleDateFormat.withActivityTimeZone(activityTimeZone: String?): SimpleDateFormat {
    if (!activityTimeZone.isNullOrBlank()) {
        timeZone = TimeZone.getTimeZone(activityTimeZone)
    }
    return this
}

/**
 * 格式化配速 (min/km -> 5'30")
 */
fun formatPace(paceMinPerKm: Double): String {
    if (paceMinPerKm <= 0) return "-"
    val minutes = paceMinPerKm.toInt()
    val seconds = ((paceMinPerKm - minutes) * 60).toInt()
    return "${minutes}'${seconds.toString().padStart(2, '0')}\""
}

/**
 * 格式化时长 (分钟 -> 1:05:30)
 */
fun formatDuration(durationMinutes: Double): String {
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
            value = if (totalDistance >= 10000) String.format("%.0f", totalDistance)
                    else String.format("%.1f", totalDistance),
            unit = "公里",
            description = "累计跑量"
        )
        AllRunItemView(
            value = totalRunCount.toString(),
            unit = "次",
            description = "总次数"
        )
        AllRunItemView(
            value = if (totalDuration / 60 >= 1000) String.format("%.0f", totalDuration / 60)
                    else String.format("%.1f", totalDuration / 60),
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
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = RunTheme.colorScheme.blue,
                fontFamily = RunYayFontFamily
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
