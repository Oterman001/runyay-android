package com.oterman.rundemo.presentation.feature.onboarding.physio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.HearRateZoneSettings
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.repository.UserRepository
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class PhysioSetupViewModel(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PhysioSetupViewModel"
    }

    val settings: MutableStateFlow<HearRateZoneSettings> =
        MutableStateFlow(preferencesManager.getHearRateZoneSettings())

    val isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val serverError: MutableStateFlow<String?> = MutableStateFlow(null)
    val setupComplete: MutableStateFlow<Boolean> = MutableStateFlow(false)

    fun skipServerSync() {
        setupComplete.value = true
    }

    fun retryServerSync() {
        serverError.value = null
        isLoading.value = true
        viewModelScope.launch {
            userRepository.saveBasicInfo(settings.value)
                .onSuccess {
                    isLoading.value = false
                    setupComplete.value = true
                }
                .onFailure { e ->
                    RLog.e(TAG, "重试保存生理参数到服务端失败: ${e.message}")
                    isLoading.value = false
                    serverError.value = "保存失败，请稍后重试"
                }
        }
    }

    fun onGenderChanged(isMale: Boolean) {
        settings.value = settings.value.copy(isMale = isMale)
    }

    fun onBirthdayChanged(date: Date) {
        val age = calculateAge(date)
        val calculatedMaxHR = (220 - age).coerceIn(120, 239)
        settings.value = settings.value.copy(
            birthdayMillis = date.time,
            maxHeartRate = calculatedMaxHR
        )
    }

    fun onMaxHRChanged(hr: Int) {
        settings.value = settings.value.copy(maxHeartRate = hr)
    }

    fun onRestHRChanged(hr: Int) {
        settings.value = settings.value.copy(restingHeartRate = hr)
    }

    fun completeSetup() {
        val current = settings.value
        preferencesManager.saveHearRateZoneSettings(current)
        preferencesManager.markPhysioSetupCompleted()
        isLoading.value = true
        viewModelScope.launch {
            userRepository.saveBasicInfo(current)
                .onSuccess {
                    isLoading.value = false
                    setupComplete.value = true
                }
                .onFailure { e ->
                    RLog.e(TAG, "保存生理参数到服务端失败: ${e.message}")
                    isLoading.value = false
                    serverError.value = "保存失败，请稍后重试"
                }
        }
    }

    fun skipSetup() {
        preferencesManager.markPhysioSetupCompleted()
    }

    private fun calculateAge(birthday: Date): Int {
        val now = Calendar.getInstance()
        val birth = Calendar.getInstance().apply { time = birthday }
        var age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
        return age.coerceAtLeast(0)
    }
}
