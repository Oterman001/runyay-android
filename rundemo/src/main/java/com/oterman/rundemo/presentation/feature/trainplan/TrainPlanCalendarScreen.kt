package com.oterman.rundemo.presentation.feature.trainplan

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToInt
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.oterman.rundemo.presentation.feature.home.components.RunRecordItem
import com.oterman.rundemo.presentation.feature.trainplan.components.TrainPlanListItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainPlanCalendarScreen(
    onBack: () -> Unit = {},
    onAddPlan: (String?) -> Unit = {},
    onEditPlan: (String) -> Unit = {},
    viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    val calendarState = rememberCalendarState(
        startMonth = YearMonth.now().minusMonths(24),
        endMonth = YearMonth.now().plusMonths(6),
        firstVisibleMonth = uiState.currentMonth,
        firstDayOfWeek = firstDayOfWeekFromLocale()
    )

    // Sync calendar position when nav arrows change the month
    LaunchedEffect(uiState.currentMonth) {
        calendarState.animateScrollToMonth(uiState.currentMonth)
    }

    // Refresh detail cache when returning from edit screen
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        snapshotFlow { lifecycleOwner.lifecycle.currentState >= Lifecycle.State.RESUMED }
            .distinctUntilChanged()
            .drop(1)
            .filter { it }
            .collect { viewModel.refreshCurrentDateDetails() }
    }

    // Detect swipe-based month changes and notify ViewModel
    LaunchedEffect(calendarState) {
        snapshotFlow { calendarState.firstVisibleMonth.yearMonth }
            .distinctUntilChanged()
            .collectLatest { month -> viewModel.onMonthChanged(month) }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissActionDialog,
            title = { Text("删除训练计划") },
            text = { Text("确认删除该训练计划？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeletePlan) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissActionDialog) { Text("取消") }
            }
        )
    }

    // Copy date picker
    if (uiState.showCopyDatePicker) {
        val initialMs = uiState.selectedDate
            ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMs)
        DatePickerDialog(
            onDismissRequest = viewModel::onDismissActionDialog,
            confirmButton = {
                TextButton(onClick = {
                    val ms = datePickerState.selectedDateMillis
                    if (ms != null) {
                        val date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.confirmCopyPlan(date)
                    }
                }) { Text("复制到此日期") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissActionDialog) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState, headline = { Text("选择复制目标日期", modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)) })
        }
    }

    // Move date picker
    if (uiState.showMoveDatePicker) {
        val initialMs = uiState.selectedDate
            ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMs)
        DatePickerDialog(
            onDismissRequest = viewModel::onDismissActionDialog,
            confirmButton = {
                TextButton(onClick = {
                    val ms = datePickerState.selectedDateMillis
                    if (ms != null) {
                        val date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.confirmMovePlan(date)
                    }
                }) { Text("移动到此日期") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissActionDialog) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState, headline = { Text("选择移动目标日期", modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)) })
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("训练日历", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val dateStr = uiState.selectedDate?.format(dateFormatter)
                    onAddPlan(dateStr)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增训练")
            }
        }
    ) { innerPadding ->
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refreshCurrentMonth,
            state = pullState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = uiState.isRefreshing,
                    state = pullState
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                MonthNavHeader(
                    currentMonth = uiState.currentMonth,
                    onPrevMonth = { viewModel.onMonthChanged(uiState.currentMonth.minusMonths(1)) },
                    onNextMonth = { viewModel.onMonthChanged(uiState.currentMonth.plusMonths(1)) }
                )
            }

            item {
                WeekDayLabels(firstDayOfWeek = firstDayOfWeekFromLocale())
            }

            item {
                HorizontalCalendar(
                    state = calendarState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    dayContent = { day ->
                        DayCell(
                            day = day,
                            hasRun = day.position == DayPosition.MonthDate &&
                                    uiState.daysWithRuns.contains(day.date),
                            hasPlan = day.position == DayPosition.MonthDate &&
                                    uiState.daysWithPlans.contains(day.date),
                            isSelected = day.date == uiState.selectedDate,
                            onClick = {
                                if (day.position == DayPosition.MonthDate) {
                                    viewModel.onDateSelected(day.date)
                                }
                            }
                        )
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }
            item { Spacer(modifier = Modifier.height(8.dp)) }

            if (uiState.selectedDate != null) {
                // Run records
                if (uiState.selectedDateRecords.isNotEmpty()) {
                    items(uiState.selectedDateRecords) { record ->
                        RunRecordItem(
                            record = record,
                            trackPoints = null,
                            onClick = {}
                        )
                    }
                }

                // Plan items
                if (uiState.selectedDatePlans.isNotEmpty()) {
                    items(uiState.selectedDatePlans, key = { it.planId }) { plan ->
                        SwipeRevealPlanItem(
                            onCopy = { viewModel.onCopyPlanRequest(plan.planId) },
                            onMove = { viewModel.onMovePlanRequest(plan.planId) },
                            onDelete = { viewModel.onDeletePlanRequest(plan.planId) }
                        ) {
                            TrainPlanListItem(
                                plan = plan,
                                onClick = { onEditPlan(plan.planId) },
                                detail = uiState.selectedDateDetails[plan.planId]
                            )
                        }
                    }
                }

                if (uiState.isLoadingPlans && uiState.selectedDatePlans.isEmpty()) {
                    item {
                        LoadingHint(text = "正在加载训练计划")
                    }
                } else if (uiState.planLoadError != null && uiState.selectedDatePlans.isEmpty()) {
                    item {
                        PlanLoadError(
                            message = uiState.planLoadError ?: "训练计划加载失败",
                            onRetry = viewModel::retryLoadPlans
                        )
                    }
                } else if (uiState.selectedDateRecords.isEmpty() && uiState.selectedDatePlans.isEmpty()) {
                    item {
                        EmptyHint(text = "当天没有记录")
                    }
                }
            } else {
                item {
                    EmptyHint(text = "点击日期查看当天记录")
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
        }
    }
}

@Composable
private fun SwipeRevealPlanItem(
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val actionWidthDp = 180.dp
    val density = LocalDensity.current
    val actionWidthPx = with(density) { actionWidthDp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // height(IntrinsicSize.Min): Box height = content intrinsic height so fillMaxHeight children resolve correctly
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clipToBounds()
    ) {
        // Action buttons — width fixed to 180dp, height fills the Box via fillMaxHeight
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(actionWidthDp)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF2196F3))
                    .clickable {
                        scope.launch { offsetX.animateTo(0f) }
                        onCopy()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制", tint = Color.White, modifier = Modifier.size(22.dp))
                    Text("复制", color = Color.White, fontSize = 11.sp)
                }
            }
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFFF9800))
                    .clickable {
                        scope.launch { offsetX.animateTo(0f) }
                        onMove()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "移动", tint = Color.White, modifier = Modifier.size(22.dp))
                    Text("移动", color = Color.White, fontSize = 11.sp)
                }
            }
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error)
                    .clickable {
                        scope.launch { offsetX.animateTo(0f) }
                        onDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.White, modifier = Modifier.size(22.dp))
                    Text("删除", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        // Foreground — solid background covers transparent card padding, preventing button colors from bleeding through
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < -(actionWidthPx / 2)) {
                                    offsetX.animateTo(-actionWidthPx)
                                } else {
                                    offsetX.animateTo(0f)
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val newOffset = (offsetX.value + dragAmount).coerceIn(-actionWidthPx, 0f)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
private fun MonthNavHeader(
    currentMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
        }
        Text(
            text = "${currentMonth.year}年 ${
                currentMonth.month.getDisplayName(TextStyle.FULL, Locale.CHINESE)
            }",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月")
        }
    }
}

@Composable
private fun WeekDayLabels(firstDayOfWeek: DayOfWeek) {
    val orderedDays = (0 until 7).map { firstDayOfWeek.plus(it.toLong()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        orderedDays.forEach { dow ->
            Text(
                text = dow.getDisplayName(TextStyle.NARROW, Locale.CHINESE),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun DayCell(
    day: CalendarDay,
    hasRun: Boolean,
    hasPlan: Boolean = false,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isCurrentMonth = day.position == DayPosition.MonthDate
    val isToday = day.date == LocalDate.now()
    val primaryColor = MaterialTheme.colorScheme.primary
    val planColor = Color(0xFF2196F3) // Blue for plans

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(if (isSelected) Modifier.background(primaryColor) else Modifier)
            .clickable(enabled = isCurrentMonth, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> Color.White
                !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                isToday -> primaryColor
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        if (hasRun || hasPlan) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                if (hasRun) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else primaryColor)
                    )
                }
                if (hasRun && hasPlan) {
                    Spacer(modifier = Modifier.width(2.dp))
                }
                if (hasPlan) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else planColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun LoadingHint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun PlanLoadError(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}
