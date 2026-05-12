package com.oterman.rundemo.presentation.feature.trainplan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.repository.TrainPlanRepository
import com.oterman.rundemo.domain.model.TrainPlan
import com.oterman.rundemo.domain.model.TrainPlanSummary
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
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
    val isLoadingPlans: Boolean = false,
    val planLoadError: String? = null,
    val isDeletingPlan: Boolean = false,
    val selectedDateDetails: Map<String, TrainPlan> = emptyMap(),
    val isLoadingDetails: Boolean = false
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
    private var monthDetailCache: MutableMap<String, TrainPlan> = mutableMapOf()
    private var detailLoadJob: Job? = null

    init {
        loadMonth(YearMonth.now())
    }

    fun onMonthChanged(month: YearMonth) {
        if (month == _uiState.value.currentMonth) return
        detailLoadJob?.cancel()
        monthDetailCache.clear()
        _uiState.update {
            it.copy(
                currentMonth = month,
                selectedDate = null,
                selectedDateRecords = emptyList(),
                selectedDatePlans = emptyList(),
                selectedDateDetails = emptyMap(),
                isLoadingDetails = false
            )
        }
        loadMonth(month)
    }

    fun onDateSelected(date: LocalDate) {
        if (_uiState.value.selectedDate == date) {
            detailLoadJob?.cancel()
            _uiState.update {
                it.copy(
                    selectedDate = null,
                    selectedDateRecords = emptyList(),
                    selectedDatePlans = emptyList(),
                    selectedDateDetails = emptyMap()
                )
            }
            return
        }
        _uiState.update { it.copy(selectedDate = date) }
        loadRecordsForDate(date)
        loadPlansForDate(date)
        loadPlanDetailsForDate(date)
    }

    fun deletePlan(planId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingPlan = true) }
            val result = trainPlanRepository.deletePlans(listOf(planId))
            result.onSuccess {
                // Refresh current month
                loadPlansForMonth(_uiState.value.currentMonth)
            }.onFailure { e ->
                RLog.e("CalendarVM", "deletePlan failed", e)
            }
            _uiState.update { it.copy(isDeletingPlan = false) }
        }
    }

    fun refreshPlans(force: Boolean = true) {
        loadPlansForMonth(_uiState.value.currentMonth)
    }

    fun retryLoadPlans() {
        refreshPlans()
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
            _uiState.update { it.copy(isLoadingPlans = true, planLoadError = null) }
            val result = trainPlanRepository.listPlanSummaries(
                startDate = month.atDay(1),
                endDate = month.atEndOfMonth()
            )
            result.onSuccess { plans ->
                monthPlans = plans
                val validIds = plans.map { it.planId }.toSet()
                monthDetailCache.keys.retainAll(validIds)
                val planDays = plans.mapNotNull { plan ->
                    plan.scheduledDate?.let {
                        parsePlanDate(it)
                    }
                }.toSet()
                _uiState.update { state ->
                    state.copy(
                        daysWithPlans = planDays,
                        selectedDatePlans = state.selectedDate?.let { loadPlansForDateValue(it) } ?: emptyList(),
                        isLoadingPlans = false,
                        planLoadError = null
                    )
                }
            }.onFailure { e ->
                RLog.w("CalendarVM", "loadPlansForMonth failed: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoadingPlans = false,
                        planLoadError = e.message ?: "训练计划加载失败"
                    )
                }
            }
        }
    }

    private fun loadPlansForDate(date: LocalDate) {
        val plans = loadPlansForDateValue(date)
        _uiState.update { it.copy(selectedDatePlans = plans) }
    }

    private fun loadPlanDetailsForDate(date: LocalDate) {
        detailLoadJob?.cancel()
        val plansForDate = loadPlansForDateValue(date)
        if (plansForDate.isEmpty()) return

        val cachedDetails = plansForDate
            .mapNotNull { summary -> monthDetailCache[summary.planId]?.let { summary.planId to it } }
            .toMap()
        if (cachedDetails.isNotEmpty()) {
            _uiState.update { it.copy(selectedDateDetails = cachedDetails) }
        }

        val uncachedIds = plansForDate.map { it.planId }.filter { !monthDetailCache.containsKey(it) }
        if (uncachedIds.isEmpty()) return

        detailLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetails = true) }
            val deferreds = uncachedIds.map { planId ->
                async { planId to trainPlanRepository.getPlanDetail(planId) }
            }
            for (deferred in deferreds) {
                val (planId, result) = deferred.await()
                result.onSuccess { detail ->
                    monthDetailCache[planId] = detail
                    if (_uiState.value.selectedDate == date) {
                        _uiState.update { state ->
                            state.copy(selectedDateDetails = state.selectedDateDetails + (planId to detail))
                        }
                    }
                }.onFailure { e ->
                    RLog.w("CalendarVM", "loadDetail failed for $planId: ${e.message}")
                }
            }
            _uiState.update { it.copy(isLoadingDetails = false) }
        }
    }

    private fun loadPlansForDateValue(date: LocalDate): List<TrainPlanSummary> {
        return monthPlans.filter { plan ->
            plan.scheduledDate?.let { parsePlanDate(it) } == date
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
        Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

    private fun parsePlanDate(value: String): LocalDate? {
        return runCatching {
            if (value.contains("-")) {
                LocalDate.parse(value, dateFormatter)
            } else {
                LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
            }
        }.getOrNull()
    }
}

class CalendarViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            val db = RunDatabase.getInstance(context)
            val prefs = PreferencesManager(context)
            val trainPlanRepo = TrainPlanRepository(prefs, localDao = db.trainPlanDao())
            return CalendarViewModel(db.runRecordDao(), prefs, trainPlanRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
