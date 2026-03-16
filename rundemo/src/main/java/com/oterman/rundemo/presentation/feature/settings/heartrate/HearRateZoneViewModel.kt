package com.oterman.rundemo.presentation.feature.settings.heartrate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.fit.AbilityZoneCalculator
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.HearRateZoneSettings
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.repository.UserRepository
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HearRateZoneViewModel(
    private val preferencesManager: PreferencesManager,
    private val dataSourcePreferences: DataSourcePreferences,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "HearRateZoneViewModel"
    }

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
    val serverError: MutableStateFlow<String?> = MutableStateFlow(null)
    fun clearServerError() { serverError.value = null }

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
            val current = settings.value
            preferencesManager.saveHearRateZoneSettings(current)
            saveSuccess.value = true
            val birthDate = if (current.birthdayMillis != 0L) {
                SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(current.birthdayMillis))
            } else null
            userRepository.updateBasicInfo(
                gender = if (current.isMale) "M" else "F",
                birthDate = birthDate,
                maxHR = current.maxHeartRate,
                restHR = current.restingHeartRate

            ).onFailure {
                RLog.w(TAG, "update心率区间设置失败，尝试save: ${it.message}")
                userRepository.saveBasicInfo(current).onFailure { e ->
                    RLog.e(TAG, "save心率区间设置也失败: ${e.message}")
                    serverError.value = "保存数据到服务器失败，请稍后重试"
                }
            }
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
