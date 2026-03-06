package com.oterman.rundemo.presentation.feature.onboarding.physio

import androidx.lifecycle.ViewModel
import com.oterman.rundemo.data.local.HearRateZoneSettings
import com.oterman.rundemo.data.local.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar
import java.util.Date

class PhysioSetupViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    val settings: MutableStateFlow<HearRateZoneSettings> =
        MutableStateFlow(preferencesManager.getHearRateZoneSettings())

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
        preferencesManager.saveHearRateZoneSettings(settings.value)
        preferencesManager.markPhysioSetupCompleted()
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
