package com.oterman.rundemo.presentation.feature.settings.heartrate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.fit.AbilityZoneCalculator
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.HearRateZoneSettings
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.domain.model.DataSourcePlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class HearRateZoneViewModel(
    private val preferencesManager: PreferencesManager,
    private val dataSourcePreferences: DataSourcePreferences
) : ViewModel() {

    val settings: MutableStateFlow<HearRateZoneSettings> =
        MutableStateFlow(preferencesManager.getHearRateZoneSettings())

    val zoneRanges: StateFlow<Map<Int, AbilityZoneCalculator.HeartRateRange>> = settings
        .map { s ->
            AbilityZoneCalculator.calculateHeartRate7Ranges(
                restHR = s.restingHeartRate.toDouble(),
                maxHR = s.maxHeartRate.toDouble()
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val saveSuccess: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val boundPlatforms: List<DataSourcePlatform> = DataSourcePlatform.getEnabledPlatforms()
        .filter { dataSourcePreferences.isPlatformBound(it) }

    fun onMaxHRChanged(hr: Int) {
        settings.value = settings.value.copy(maxHeartRate = hr)
    }

    fun onRestHRChanged(hr: Int) {
        settings.value = settings.value.copy(restingHeartRate = hr)
    }

    fun onBirthdayChanged(date: Date) {
        val age = calculateAge(date)
        val calculatedMaxHR = (220 - age).coerceIn(120, 239)
        settings.value = settings.value.copy(
            birthdayMillis = date.time,
            maxHeartRate = calculatedMaxHR
        )
    }

    fun onAutoSyncToggled(enabled: Boolean) {
        settings.value = settings.value.copy(isAutoSyncEnabled = enabled)
    }

    fun onPreferredPlatformChanged(platform: DataSourcePlatform?) {
        settings.value = settings.value.copy(preferredPlatform = platform?.code)
    }

    fun save() {
        viewModelScope.launch {
            preferencesManager.saveHearRateZoneSettings(settings.value)
            saveSuccess.value = true
        }
    }

    fun clearSaveSuccess() {
        saveSuccess.value = false
    }

    private fun calculateAge(birthday: Date): Int {
        val now = Calendar.getInstance()
        val birth = Calendar.getInstance().apply { time = birthday }
        var age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
        return age.coerceAtLeast(0)
    }
}
