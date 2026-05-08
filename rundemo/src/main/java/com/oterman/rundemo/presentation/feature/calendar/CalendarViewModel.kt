package com.oterman.rundemo.presentation.feature.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val daysWithRuns: Set<LocalDate> = emptySet(),
    val selectedDate: LocalDate? = null,
    val selectedDateRecords: List<RunRecordEntity> = emptyList(),
    val isLoading: Boolean = false
)

class CalendarViewModel(
    private val dao: RunRecordDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonth(YearMonth.now())
    }

    fun onMonthChanged(month: YearMonth) {
        _uiState.update { it.copy(currentMonth = month, selectedDate = null, selectedDateRecords = emptyList()) }
        loadMonth(month)
    }

    fun onDateSelected(date: LocalDate) {
        val records = _uiState.value.selectedDateRecords
        if (_uiState.value.selectedDate == date) {
            _uiState.update { it.copy(selectedDate = null, selectedDateRecords = emptyList()) }
            return
        }
        _uiState.update { it.copy(selectedDate = date) }
        loadRecordsForDate(date)
    }

    private fun loadMonth(month: YearMonth) {
        val userId = preferencesManager.getUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val zoneId = ZoneId.systemDefault()
            val startMs = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endMs = month.atEndOfMonth().atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()

            val records = dao.getRecordsByMonthForUser(userId, startMs, endMs)
            val days = records.map { record ->
                record.startTime.toLocalDate(zoneId)
            }.toSet()

            _uiState.update { it.copy(daysWithRuns = days, isLoading = false) }
        }
    }

    private fun loadRecordsForDate(date: LocalDate) {
        val userId = preferencesManager.getUserId() ?: return
        viewModelScope.launch {
            val zoneId = ZoneId.systemDefault()
            val startMs = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endMs = date.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()
            val records = dao.getRecordsByMonthForUser(userId, startMs, endMs)
            _uiState.update { it.copy(selectedDateRecords = records) }
        }
    }

    private fun Long.toLocalDate(zoneId: ZoneId): LocalDate =
        java.time.Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
}

class CalendarViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            val dao = RunDatabase.getInstance(context).runRecordDao()
            val prefs = PreferencesManager(context)
            return CalendarViewModel(dao, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
