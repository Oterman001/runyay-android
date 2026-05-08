package com.oterman.rundemo.presentation.feature.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.network.dto.response.toDomain
import com.oterman.rundemo.data.repository.TrainPlanRepository
import com.oterman.rundemo.domain.model.TrainPlanSummary
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val daysWithRuns: Set<LocalDate> = emptySet(),
    val daysWithPlans: Set<LocalDate> = emptySet(),
    val selectedDate: LocalDate? = null,
    val selectedDateRecords: List<RunRecordEntity> = emptyList(),
    val selectedDatePlans: List<TrainPlanSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isDeletingPlan: Boolean = false
)

class CalendarViewModel(
    private val dao: RunRecordDao,
    private val preferencesManager: PreferencesManager,
    private val trainPlanRepository: TrainPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Cache all plans for current month to avoid re-fetching per date
    private var monthPlans: List<TrainPlanSummary> = emptyList()

    init {
        loadMonth(YearMonth.now())
    }

    fun onMonthChanged(month: YearMonth) {
        if (month == _uiState.value.currentMonth) return
        _uiState.update { it.copy(currentMonth = month, selectedDate = null, selectedDateRecords = emptyList(), selectedDatePlans = emptyList()) }
        loadMonth(month)
    }

    fun onDateSelected(date: LocalDate) {
        if (_uiState.value.selectedDate == date) {
            _uiState.update { it.copy(selectedDate = null, selectedDateRecords = emptyList(), selectedDatePlans = emptyList()) }
            return
        }
        _uiState.update { it.copy(selectedDate = date) }
        loadRecordsForDate(date)
        loadPlansForDate(date)
    }

    fun deletePlan(planId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingPlan = true) }
            val result = trainPlanRepository.deletePlans(listOf(planId))
            result.onSuccess {
                // Refresh current month
                loadPlansForMonth(_uiState.value.currentMonth)
                // Refresh selected date
                _uiState.value.selectedDate?.let { loadPlansForDate(it) }
            }.onFailure { e ->
                RLog.e("CalendarVM", "deletePlan failed", e)
            }
            _uiState.update { it.copy(isDeletingPlan = false) }
        }
    }

    fun refreshPlans() {
        loadPlansForMonth(_uiState.value.currentMonth)
        _uiState.value.selectedDate?.let { loadPlansForDate(it) }
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
        loadPlansForMonth(month)
    }

    private fun loadPlansForMonth(month: YearMonth) {
        viewModelScope.launch {
            val startDate = month.atDay(1).format(dateFormatter)
            val endDate = month.atEndOfMonth().format(dateFormatter)
            val result = trainPlanRepository.listPlans(
                startDate = startDate,
                endDate = endDate,
                pageSize = 100
            )
            result.onSuccess { data ->
                val plans = data.records?.map { it.toDomain() } ?: emptyList()
                monthPlans = plans
                val planDays = plans.mapNotNull { plan ->
                    plan.scheduledDate?.let {
                        try { LocalDate.parse(it, dateFormatter) } catch (_: Exception) { null }
                    }
                }.toSet()
                _uiState.update { it.copy(daysWithPlans = planDays) }
            }.onFailure {
                RLog.w("CalendarVM", "loadPlansForMonth failed: ${it.message}")
            }
        }
    }

    private fun loadPlansForDate(date: LocalDate) {
        val dateStr = date.format(dateFormatter)
        val plans = monthPlans.filter { it.scheduledDate == dateStr }
        _uiState.update { it.copy(selectedDatePlans = plans) }
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
            val trainPlanRepo = TrainPlanRepository(prefs)
            return CalendarViewModel(dao, prefs, trainPlanRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
