package com.oterman.rundemo.presentation.feature.settings.goal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.domain.model.GoalSettings
import com.oterman.rundemo.domain.model.GoalType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for RunGoalSetPage
 */
data class RunGoalSetUiState(
    val goalEnabled: Boolean = false,
    val goalType: GoalType = GoalType.DISTANCE,
    val monthDistanceInput: String = "",
    val yearDistanceInput: String = "",
    val monthDurationInput: String = "",
    val yearDurationInput: String = "",
    val showSaveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = true
) {
    // Validation ranges
    companion object {
        const val MIN_MONTH_DISTANCE = 10.0
        const val MAX_MONTH_DISTANCE = 1000.0
        const val MIN_YEAR_DISTANCE = 100.0
        const val MAX_YEAR_DISTANCE = 12000.0
        const val MIN_MONTH_DURATION = 5.0
        const val MAX_MONTH_DURATION = 200.0
        const val MIN_YEAR_DURATION = 50.0
        const val MAX_YEAR_DURATION = 2000.0
    }
}

/**
 * ViewModel for RunGoalSetPage
 * Handles goal settings input, validation, and persistence
 */
class RunGoalSetViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunGoalSetUiState())
    val uiState: StateFlow<RunGoalSetUiState> = _uiState.asStateFlow()

    init {
        loadGoalSettings()
    }

    /**
     * Load existing goal settings
     */
    private fun loadGoalSettings() {
        viewModelScope.launch {
            val settings = preferencesManager.getGoalSettings()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    goalEnabled = settings.goalEnabled,
                    goalType = settings.goalType,
                    monthDistanceInput = if (settings.monthDistanceGoal > 0)
                        settings.monthDistanceGoal.toInt().toString() else "",
                    yearDistanceInput = if (settings.yearDistanceGoal > 0)
                        settings.yearDistanceGoal.toInt().toString() else "",
                    monthDurationInput = if (settings.monthDurationGoal > 0)
                        settings.monthDurationGoal.toInt().toString() else "",
                    yearDurationInput = if (settings.yearDurationGoal > 0)
                        settings.yearDurationGoal.toInt().toString() else ""
                )
            }
        }
    }

    /**
     * Toggle goal enabled state
     */
    fun setGoalEnabled(enabled: Boolean) {
        _uiState.update { it.copy(goalEnabled = enabled) }
    }

    /**
     * Set goal type (distance or duration)
     */
    fun setGoalType(type: GoalType) {
        _uiState.update { it.copy(goalType = type) }
    }

    /**
     * Update month distance input
     */
    fun setMonthDistanceInput(value: String) {
        _uiState.update { it.copy(monthDistanceInput = value) }
    }

    /**
     * Update year distance input
     */
    fun setYearDistanceInput(value: String) {
        _uiState.update { it.copy(yearDistanceInput = value) }
    }

    /**
     * Update month duration input
     */
    fun setMonthDurationInput(value: String) {
        _uiState.update { it.copy(monthDurationInput = value) }
    }

    /**
     * Update year duration input
     */
    fun setYearDurationInput(value: String) {
        _uiState.update { it.copy(yearDurationInput = value) }
    }

    /**
     * Auto-fill year goal from month goal (month * 12)
     */
    fun autoFillYearFromMonth() {
        val state = _uiState.value
        if (state.goalType == GoalType.DISTANCE) {
            val monthGoal = state.monthDistanceInput.toDoubleOrNull() ?: return
            val yearGoal = (monthGoal * 12).toInt()
            _uiState.update { it.copy(yearDistanceInput = yearGoal.toString()) }
        } else {
            val monthGoal = state.monthDurationInput.toDoubleOrNull() ?: return
            val yearGoal = (monthGoal * 12).toInt()
            _uiState.update { it.copy(yearDurationInput = yearGoal.toString()) }
        }
    }

    /**
     * Auto-fill month goal from year goal (year / 12)
     */
    fun autoFillMonthFromYear() {
        val state = _uiState.value
        if (state.goalType == GoalType.DISTANCE) {
            val yearGoal = state.yearDistanceInput.toDoubleOrNull() ?: return
            val monthGoal = (yearGoal / 12).toInt()
            _uiState.update { it.copy(monthDistanceInput = monthGoal.toString()) }
        } else {
            val yearGoal = state.yearDurationInput.toDoubleOrNull() ?: return
            val monthGoal = (yearGoal / 12).toInt()
            _uiState.update { it.copy(monthDurationInput = monthGoal.toString()) }
        }
    }

    /**
     * Validate and save goal settings
     */
    fun saveGoalSettings(): Boolean {
        val state = _uiState.value

        // If goals disabled, just save and return
        if (!state.goalEnabled) {
            saveToPreferences(state)
            _uiState.update { it.copy(showSaveSuccess = true, errorMessage = null) }
            return true
        }

        // Validate based on goal type
        val validationError = if (state.goalType == GoalType.DISTANCE) {
            validateDistanceGoals(state)
        } else {
            validateDurationGoals(state)
        }

        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return false
        }

        // Auto-fill if needed
        autoFillIfNeeded()

        // Save
        saveToPreferences(_uiState.value)
        _uiState.update { it.copy(showSaveSuccess = true, errorMessage = null) }
        return true
    }

    private fun validateDistanceGoals(state: RunGoalSetUiState): String? {
        val monthGoal = state.monthDistanceInput.toDoubleOrNull()
        val yearGoal = state.yearDistanceInput.toDoubleOrNull()

        // At least one goal should be set
        if (monthGoal == null && yearGoal == null) {
            return "请设置月度或年度距离目标"
        }

        // Validate month goal if set
        if (monthGoal != null) {
            if (monthGoal < RunGoalSetUiState.MIN_MONTH_DISTANCE ||
                monthGoal > RunGoalSetUiState.MAX_MONTH_DISTANCE) {
                return "月度距离目标应在${RunGoalSetUiState.MIN_MONTH_DISTANCE.toInt()}-${RunGoalSetUiState.MAX_MONTH_DISTANCE.toInt()}公里之间"
            }
        }

        // Validate year goal if set
        if (yearGoal != null) {
            if (yearGoal < RunGoalSetUiState.MIN_YEAR_DISTANCE ||
                yearGoal > RunGoalSetUiState.MAX_YEAR_DISTANCE) {
                return "年度距离目标应在${RunGoalSetUiState.MIN_YEAR_DISTANCE.toInt()}-${RunGoalSetUiState.MAX_YEAR_DISTANCE.toInt()}公里之间"
            }
        }

        return null
    }

    private fun validateDurationGoals(state: RunGoalSetUiState): String? {
        val monthGoal = state.monthDurationInput.toDoubleOrNull()
        val yearGoal = state.yearDurationInput.toDoubleOrNull()

        // At least one goal should be set
        if (monthGoal == null && yearGoal == null) {
            return "请设置月度或年度时长目标"
        }

        // Validate month goal if set
        if (monthGoal != null) {
            if (monthGoal < RunGoalSetUiState.MIN_MONTH_DURATION ||
                monthGoal > RunGoalSetUiState.MAX_MONTH_DURATION) {
                return "月度时长目标应在${RunGoalSetUiState.MIN_MONTH_DURATION.toInt()}-${RunGoalSetUiState.MAX_MONTH_DURATION.toInt()}小时之间"
            }
        }

        // Validate year goal if set
        if (yearGoal != null) {
            if (yearGoal < RunGoalSetUiState.MIN_YEAR_DURATION ||
                yearGoal > RunGoalSetUiState.MAX_YEAR_DURATION) {
                return "年度时长目标应在${RunGoalSetUiState.MIN_YEAR_DURATION.toInt()}-${RunGoalSetUiState.MAX_YEAR_DURATION.toInt()}小时之间"
            }
        }

        return null
    }

    private fun autoFillIfNeeded() {
        val state = _uiState.value

        if (state.goalType == GoalType.DISTANCE) {
            val monthGoal = state.monthDistanceInput.toDoubleOrNull()
            val yearGoal = state.yearDistanceInput.toDoubleOrNull()

            when {
                monthGoal != null && yearGoal == null -> {
                    _uiState.update { it.copy(yearDistanceInput = (monthGoal * 12).toInt().toString()) }
                }
                yearGoal != null && monthGoal == null -> {
                    _uiState.update { it.copy(monthDistanceInput = (yearGoal / 12).toInt().toString()) }
                }
            }
        } else {
            val monthGoal = state.monthDurationInput.toDoubleOrNull()
            val yearGoal = state.yearDurationInput.toDoubleOrNull()

            when {
                monthGoal != null && yearGoal == null -> {
                    _uiState.update { it.copy(yearDurationInput = (monthGoal * 12).toInt().toString()) }
                }
                yearGoal != null && monthGoal == null -> {
                    _uiState.update { it.copy(monthDurationInput = (yearGoal / 12).toInt().toString()) }
                }
            }
        }
    }

    private fun saveToPreferences(state: RunGoalSetUiState) {
        val settings = GoalSettings(
            goalEnabled = state.goalEnabled,
            goalType = state.goalType,
            yearDistanceGoal = state.yearDistanceInput.toDoubleOrNull() ?: 0.0,
            monthDistanceGoal = state.monthDistanceInput.toDoubleOrNull() ?: 0.0,
            yearDurationGoal = state.yearDurationInput.toDoubleOrNull() ?: 0.0,
            monthDurationGoal = state.monthDurationInput.toDoubleOrNull() ?: 0.0
        )
        preferencesManager.saveGoalSettings(settings)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Clear success flag
     */
    fun clearSuccessFlag() {
        _uiState.update { it.copy(showSaveSuccess = false) }
    }
}

/**
 * Factory for RunGoalSetViewModel
 */
class RunGoalSetViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunGoalSetViewModel::class.java)) {
            val preferencesManager = PreferencesManager(context)
            return RunGoalSetViewModel(preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
