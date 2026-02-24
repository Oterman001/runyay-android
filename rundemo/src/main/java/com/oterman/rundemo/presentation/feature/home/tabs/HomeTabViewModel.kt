package com.oterman.rundemo.presentation.feature.home.tabs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.data.mock.MockDataProvider
import com.oterman.rundemo.domain.model.DayRunData
import com.oterman.rundemo.domain.model.DayRunRecordInfo
import com.oterman.rundemo.domain.model.GoalSettings
import com.oterman.rundemo.domain.model.GoalType
import com.oterman.rundemo.domain.model.HomeTabUiState
import com.oterman.rundemo.domain.model.LatestRunRecord
import com.oterman.rundemo.data.local.entity.PBRecordEntity
import com.oterman.rundemo.data.local.entity.OverallVdotEntity
import com.oterman.rundemo.domain.model.PBAbilityInfo
import com.oterman.rundemo.domain.model.PBAbilityKey
import com.oterman.rundemo.domain.model.PBSpeedInfo
import com.oterman.rundemo.domain.model.PBSpeedKey
import com.oterman.rundemo.domain.model.PeriodStatistics
import com.oterman.rundemo.domain.model.TotalRunStatistics
import com.oterman.rundemo.domain.model.WeekStatistics
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * HomeTab ViewModel
 * Manages running statistics state for the home tab
 */
class HomeTabViewModel(
    private val repository: RunDataRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeTabUiState())
    val uiState: StateFlow<HomeTabUiState> = _uiState.asStateFlow()

    private var latestRecords: List<RunRecordEntity> = emptyList()

    init {
        observeRunRecords()
    }

    /**
     * Continuously observe run records from database.
     * Automatically refreshes UI when records change (e.g. after sync).
     */
    private fun observeRunRecords() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getAllRunRecords()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load statistics"
                    )
                }
                .collect { allRecords ->
                    latestRecords = allRecords
                    val goalSettings = preferencesManager.getGoalSettings()

                    val totalStats = calculateTotalStatistics(allRecords)
                    val yearStats = calculateYearStatistics(allRecords, goalSettings)
                    val monthStats = calculateMonthStatistics(allRecords, goalSettings)
                    val weekStats = calculateWeekStatistics(allRecords)

                    // New calculations for 5 cards
                    val latestRecord = calculateLatestRunRecord(allRecords)

                    // 从 pb_record 表读取真实 PB 数据
                    val abilityPBs = repository.getAllPBByType("Ability")
                    val speedPBs = repository.getAllPBByType("Speed")
                    val latestVdot = repository.getLatestVdot()

                    val pbAbilityList = calculatePBAbilityList(allRecords, abilityPBs, latestVdot)
                    val pbSpeedList = calculatePBSpeedList(speedPBs)
                    val nextRace = MockDataProvider.getMockNextRace()
                    val dailySentence = MockDataProvider.getRandomDailySentence()

                    _uiState.value = HomeTabUiState(
                        isLoading = false,
                        totalStats = totalStats,
                        yearStats = yearStats,
                        monthStats = monthStats,
                        weekStats = weekStats,
                        goalSettings = goalSettings,
                        latestRunRecord = latestRecord,
                        pbAbilityList = pbAbilityList,
                        pbSpeedList = pbSpeedList,
                        nextRace = nextRace,
                        dailySentence = dailySentence,
                        error = null
                    )
                }
        }
    }

    /**
     * Refresh goal settings from SharedPreferences and recalculate period stats.
     * Called when HomeTab resumes (e.g. returning from goal settings page).
     */
    fun refreshGoalSettings() {
        if (latestRecords.isEmpty()) return
        val goalSettings = preferencesManager.getGoalSettings()
        val yearStats = calculateYearStatistics(latestRecords, goalSettings)
        val monthStats = calculateMonthStatistics(latestRecords, goalSettings)
        _uiState.value = _uiState.value.copy(
            goalSettings = goalSettings,
            yearStats = yearStats,
            monthStats = monthStats
        )
    }

    private fun calculateTotalStatistics(allRecords: List<RunRecordEntity>): TotalRunStatistics {
        return TotalRunStatistics(
            totalDistance = allRecords.sumOf { it.totalDistance },
            totalDuration = allRecords.sumOf { it.activeDuration } / 60.0, // Convert to hours
            totalRuns = allRecords.size,
            overallVdot = allRecords.maxOfOrNull { maxOf(it.vdot, it.overallVdot) } ?: 0.0
        )
    }

    private fun calculateYearStatistics(
        allRecords: List<RunRecordEntity>,
        goalSettings: GoalSettings
    ): PeriodStatistics {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val yearStart = calendar.timeInMillis

        calendar.set(currentYear + 1, Calendar.JANUARY, 1, 0, 0, 0)
        val yearEnd = calendar.timeInMillis

        val yearRecords = allRecords.filter { it.startTime in yearStart until yearEnd }

        // Calculate time progress (how much of the year has passed)
        val timeProgress = calculateYearTimeProgress()

        return PeriodStatistics(
            runCount = yearRecords.size,
            totalDistance = yearRecords.sumOf { it.totalDistance },
            totalDuration = yearRecords.sumOf { it.activeDuration } / 60.0,
            distanceGoal = goalSettings.yearDistanceGoal,
            durationGoal = goalSettings.yearDurationGoal,
            timeProgress = timeProgress
        )
    }

    private fun calculateMonthStatistics(
        allRecords: List<RunRecordEntity>,
        goalSettings: GoalSettings
    ): PeriodStatistics {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis

        val monthRecords = allRecords.filter { it.startTime in monthStart until monthEnd }

        // Calculate time progress (how much of the month has passed)
        val timeProgress = calculateMonthTimeProgress()

        return PeriodStatistics(
            runCount = monthRecords.size,
            totalDistance = monthRecords.sumOf { it.totalDistance },
            totalDuration = monthRecords.sumOf { it.activeDuration } / 60.0,
            distanceGoal = goalSettings.monthDistanceGoal,
            durationGoal = goalSettings.monthDurationGoal,
            timeProgress = timeProgress
        )
    }

    private fun calculateWeekStatistics(allRecords: List<RunRecordEntity>): WeekStatistics {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()

        // Set to Monday of current week
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis

        // End of week (next Monday)
        val weekEndCal = calendar.clone() as Calendar
        weekEndCal.add(Calendar.DAY_OF_WEEK, 7)
        val weekEnd = weekEndCal.timeInMillis

        val weekRecords = allRecords.filter { it.startTime in weekStart until weekEnd }

        // Build daily records
        val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
        val dailyRecords = mutableListOf<DayRunData>()

        for (i in 0..6) {
            val dayCal = Calendar.getInstance()
            dayCal.timeInMillis = weekStart
            dayCal.add(Calendar.DAY_OF_WEEK, i)
            val dayStart = dayCal.timeInMillis
            dayCal.add(Calendar.DAY_OF_WEEK, 1)
            val dayEnd = dayCal.timeInMillis

            val dayRecords = weekRecords.filter { it.startTime in dayStart until dayEnd }

            dayCal.timeInMillis = dayStart
            val isToday = isSameDay(dayCal, today)
            val isFuture = dayCal.after(today) && !isToday

            // Build record infos for multi-select dialog
            val recordInfos = dayRecords.map { record ->
                DayRunRecordInfo(
                    workoutId = record.workoutId,
                    distance = record.totalDistance,
                    duration = formatDuration(record.activeDuration),
                    startTime = formatTime(record.startTime)
                )
            }

            dailyRecords.add(
                DayRunData(
                    date = Date(dayStart),
                    dayOfWeek = weekDays[i],
                    totalDistance = dayRecords.sumOf { it.totalDistance },
                    runCount = dayRecords.size,
                    isToday = isToday,
                    isFuture = isFuture,
                    isIndoor = dayRecords.any { it.outdoor == 1 },
                    workoutIds = dayRecords.map { it.workoutId },
                    recordInfos = recordInfos
                )
            )
        }

        return WeekStatistics(
            totalDistance = weekRecords.sumOf { it.totalDistance },
            totalDurationMinutes = weekRecords.sumOf { it.activeDuration },
            dailyRecords = dailyRecords
        )
    }

    /**
     * Calculate how much of the current year has passed (0-1)
     */
    private fun calculateYearTimeProgress(): Float {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        // Year start
        calendar.set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val yearStart = calendar.timeInMillis

        // Year end
        calendar.set(currentYear + 1, Calendar.JANUARY, 1, 0, 0, 0)
        val yearEnd = calendar.timeInMillis

        val totalDuration = yearEnd - yearStart
        val elapsed = now - yearStart

        return (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
    }

    /**
     * Calculate how much of the current month has passed (0-1)
     */
    private fun calculateMonthTimeProgress(): Float {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        // Month start
        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis

        // Month end
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis

        val totalDuration = monthEnd - monthStart
        val elapsed = now - monthStart

        return (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Calculate latest run record from all records
     */
    private fun calculateLatestRunRecord(allRecords: List<RunRecordEntity>): LatestRunRecord? {
        val latest = allRecords.maxByOrNull { it.startTime } ?: return null
        return LatestRunRecord(
            workoutId = latest.workoutId,
            runDate = formatRunDate(latest.startTime),
            startEndTime = formatStartEndTime(latest.startTime, latest.endTime),
            totalDistance = latest.totalDistance,
            duration = formatDuration(latest.activeDuration),
            avgPace = formatPace(latest.averageSpeed),
            deviceName = latest.deviceInfo ?: "Unknown",
            isVerified = latest.inclusiveLevel >= 1
        )
    }

    /**
     * 计算最大数据列表（最大跑力、最远距离、最长时间）
     * 优先从 pb_record / overall_vdot 表读取，无数据时回退到 run_record 计算
     */
    private fun calculatePBAbilityList(
        allRecords: List<RunRecordEntity>,
        abilityPBs: List<PBRecordEntity>,
        latestVdot: OverallVdotEntity?
    ): List<PBAbilityInfo> {
        // 最大跑力：优先 overall_vdot 表，回退到 run_record 中最大 vdot
        val vdotValue = latestVdot?.value
            ?: allRecords.maxOfOrNull { maxOf(it.vdot, it.overallVdot) }
                ?.takeIf { it > 0 }
        val maxVdot = PBAbilityInfo(
            itemKey = PBAbilityKey.MAX_VDOT,
            itemMaxValue = vdotValue?.let { String.format("%.1f", it) },
            itemDate = latestVdot?.let { formatDate(it.date) },
            workoutId = latestVdot?.workoutId
        )

        // 最远距离：优先 pb_record (type=Ability, subType=maxDistance)，回退到 run_record
        val distancePB = abilityPBs.find { it.subType == "maxDistance" }
        val maxDistance = if (distancePB != null) {
            PBAbilityInfo(
                itemKey = PBAbilityKey.MAX_DISTANCE,
                itemMaxValue = String.format("%.2f", distancePB.value),
                itemDate = formatDate(distancePB.completeTime),
                workoutId = distancePB.workoutId
            )
        } else {
            val fallback = allRecords.maxByOrNull { it.totalDistance }
            if (fallback != null && fallback.totalDistance > 0) {
                PBAbilityInfo(
                    itemKey = PBAbilityKey.MAX_DISTANCE,
                    itemMaxValue = String.format("%.2f", fallback.totalDistance),
                    itemDate = formatDate(fallback.startTime),
                    workoutId = fallback.workoutId
                )
            } else {
                PBAbilityInfo(itemKey = PBAbilityKey.MAX_DISTANCE, itemMaxValue = null, itemDate = null)
            }
        }

        // 最长时间：优先 pb_record (type=Ability, subType=maxDuration)，回退到 run_record
        val durationPB = abilityPBs.find { it.subType == "maxDuration" }
        val maxDuration = if (durationPB != null) {
            PBAbilityInfo(
                itemKey = PBAbilityKey.MAX_DURATION,
                itemMaxValue = formatDurationLong(durationPB.value),
                itemDate = formatDate(durationPB.completeTime),
                workoutId = durationPB.workoutId
            )
        } else {
            val fallback = allRecords.maxByOrNull { it.activeDuration }
            if (fallback != null && fallback.activeDuration > 0) {
                PBAbilityInfo(
                    itemKey = PBAbilityKey.MAX_DURATION,
                    itemMaxValue = formatDurationLong(fallback.activeDuration),
                    itemDate = formatDate(fallback.startTime),
                    workoutId = fallback.workoutId
                )
            } else {
                PBAbilityInfo(itemKey = PBAbilityKey.MAX_DURATION, itemMaxValue = null, itemDate = null)
            }
        }

        return listOf(maxVdot, maxDistance, maxDuration)
    }

    /**
     * 从 pb_record 表（type=Speed）读取各距离 PB 并映射到 PBSpeedInfo 列表
     * subType 对应关系：1k / 3k / 5k / 10k / 21k / 42k
     */
    private fun calculatePBSpeedList(speedPBs: List<PBRecordEntity>): List<PBSpeedInfo> {
        val subTypeMap = mapOf(
            PBSpeedKey.KM_1 to "1k",
            PBSpeedKey.KM_3 to "3k",
            PBSpeedKey.KM_5 to "5k",
            PBSpeedKey.KM_10 to "10k",
            PBSpeedKey.KM_HALF_MARATHON to "21k",
            PBSpeedKey.KM_MARATHON to "42k"
        )
        return PBSpeedKey.entries.map { key ->
            val subType = subTypeMap[key] ?: ""
            val entity = speedPBs.find { it.subType == subType }
            PBSpeedInfo(
                pbKey = key,
                pbTimeValue = entity?.let { formatDuration(it.value) },
                pbDate = entity?.let { formatDate(it.completeTime) },
                workoutId = entity?.workoutId
            )
        }
    }

    /**
     * Format run date like "2月8日 周六"
     */
    private fun formatRunDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val weekDay = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "周日"
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> ""
        }
        return "${month}月${day}日 $weekDay"
    }

    /**
     * Format start and end time like "06:30-07:15"
     */
    private fun formatStartEndTime(startTime: Long, endTime: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return "${sdf.format(Date(startTime))}-${sdf.format(Date(endTime))}"
    }

    /**
     * Format duration in minutes to "45'30\"" format
     */
    private fun formatDuration(minutes: Double): String {
        val totalSeconds = (minutes * 60).toInt()
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        return "${mins}'${String.format("%02d", secs)}\""
    }

    /**
     * Format duration in minutes to "4h30'20\"" format for long durations
     */
    private fun formatDurationLong(minutes: Double): String {
        val totalSeconds = (minutes * 60).toInt()
        val hours = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        return if (hours > 0) {
            "${hours}h${mins}'${String.format("%02d", secs)}\""
        } else {
            "${mins}'${String.format("%02d", secs)}\""
        }
    }

    /**
     * Format average speed (km/h) to pace "4'20\"" format
     */
    private fun formatPace(speedKmh: Double): String {
        if (speedKmh <= 0) return "--"
        val paceMinPerKm = 60.0 / speedKmh
        val mins = paceMinPerKm.toInt()
        val secs = ((paceMinPerKm - mins) * 60).toInt()
        return "${mins}'${String.format("%02d", secs)}\""
    }

    /**
     * Format date to "yyyy-MM-dd"
     */
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Format time to "HH:mm"
     */
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

/**
 * HomeTabViewModel Factory
 */
class HomeTabViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeTabViewModel::class.java)) {
            val database = RunDatabase.getInstance(context)
            val repository = RunDataRepositoryImpl.getInstance(database)
            val preferencesManager = PreferencesManager(context)
            return HomeTabViewModel(repository, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
