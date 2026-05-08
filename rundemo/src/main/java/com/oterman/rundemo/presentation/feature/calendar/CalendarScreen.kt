package com.oterman.rundemo.presentation.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.oterman.rundemo.presentation.feature.home.components.RunRecordItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit = {},
    viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    val calendarState = rememberCalendarState(
        startMonth = YearMonth.now().minusMonths(24),
        endMonth = YearMonth.now().plusMonths(6),
        firstVisibleMonth = uiState.currentMonth,
        firstDayOfWeek = firstDayOfWeekFromLocale()
    )

    // Scroll calendar to the month when navigation buttons are tapped
    LaunchedEffect(uiState.currentMonth) {
        calendarState.animateScrollToMonth(uiState.currentMonth)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
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
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    userScrollEnabled = false,
                    dayContent = { day ->
                        DayCell(
                            day = day,
                            hasRun = day.position == DayPosition.MonthDate &&
                                    uiState.daysWithRuns.contains(day.date),
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

            when {
                uiState.selectedDate != null && uiState.selectedDateRecords.isNotEmpty() -> {
                    items(uiState.selectedDateRecords) { record ->
                        RunRecordItem(
                            record = record,
                            trackPoints = null,
                            onClick = {}
                        )
                    }
                }
                uiState.selectedDate != null -> {
                    item {
                        EmptyHint(text = "当天没有跑步记录")
                    }
                }
                else -> {
                    item {
                        EmptyHint(text = "点击日期查看当天记录")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
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
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isCurrentMonth = day.position == DayPosition.MonthDate
    val isToday = day.date == LocalDate.now()
    val primaryColor = MaterialTheme.colorScheme.primary

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
        if (hasRun) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else primaryColor)
            )
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
