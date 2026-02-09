package com.oterman.rundemo.data.mock

import com.oterman.rundemo.domain.model.LatestRunRecord
import com.oterman.rundemo.domain.model.NextRaceInfo
import com.oterman.rundemo.domain.model.PBAbilityInfo
import com.oterman.rundemo.domain.model.PBAbilityKey
import com.oterman.rundemo.domain.model.PBSpeedInfo
import com.oterman.rundemo.domain.model.PBSpeedKey
import com.oterman.rundemo.domain.model.RaceDistanceType
import java.util.Calendar

/**
 * Provides mock data for cards that don't have real data yet
 */
object MockDataProvider {

    /**
     * Get mock latest run record (for testing when no real data)
     */
    fun getMockLatestRunRecord(): LatestRunRecord {
        return LatestRunRecord(
            workoutId = "mock-001",
            runDate = "2月8日 周六",
            startEndTime = "06:30-07:15",
            totalDistance = 10.52,
            duration = "45'30\"",
            avgPace = "4'20\"",
            deviceName = "Garmin FR965",
            isVerified = true
        )
    }

    /**
     * Get mock PB ability list (VDOT uses mock, distance/duration can use real)
     */
    fun getMockPBAbilityList(): List<PBAbilityInfo> {
        return listOf(
            PBAbilityInfo(
                itemKey = PBAbilityKey.MAX_VDOT,
                itemMaxValue = "52.5",
                itemDate = "2024-10-15",
                workoutId = "mock-002"
            ),
            PBAbilityInfo(
                itemKey = PBAbilityKey.MAX_DISTANCE,
                itemMaxValue = "42.52",
                itemDate = "2024-11-11",
                workoutId = "mock-003"
            ),
            PBAbilityInfo(
                itemKey = PBAbilityKey.MAX_DURATION,
                itemMaxValue = "4h30'20\"",
                itemDate = "2024-11-11",
                workoutId = "mock-003"
            )
        )
    }

    /**
     * Get mock PB speed list
     */
    fun getMockPBSpeedList(): List<PBSpeedInfo> {
        return listOf(
            PBSpeedInfo(
                pbKey = PBSpeedKey.KM_1,
                pbTimeValue = "3'45\"",
                pbDate = "2024-09-10",
                workoutId = "mock-004"
            ),
            PBSpeedInfo(
                pbKey = PBSpeedKey.KM_3,
                pbTimeValue = "12'30\"",
                pbDate = "2024-09-10",
                workoutId = "mock-004"
            ),
            PBSpeedInfo(
                pbKey = PBSpeedKey.KM_5,
                pbTimeValue = "21'15\"",
                pbDate = "2024-08-20",
                workoutId = "mock-005"
            ),
            PBSpeedInfo(
                pbKey = PBSpeedKey.KM_10,
                pbTimeValue = "45'30\"",
                pbDate = "2024-10-15",
                workoutId = "mock-002"
            ),
            PBSpeedInfo(
                pbKey = PBSpeedKey.KM_HALF_MARATHON,
                pbTimeValue = "1h38'20\"",
                pbDate = "2024-11-03",
                workoutId = "mock-006"
            ),
            PBSpeedInfo(
                pbKey = PBSpeedKey.KM_MARATHON,
                pbTimeValue = "3h28'45\"",
                pbDate = "2024-11-11",
                workoutId = "mock-003"
            )
        )
    }

    /**
     * Get mock next race info
     */
    fun getMockNextRace(): NextRaceInfo {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 25) // 25 days from now

        return NextRaceInfo(
            id = "race-001",
            raceName = "2025 北京马拉松",
            raceDate = calendar.timeInMillis,
            raceType = RaceDistanceType.MARATHON
        )
    }

    /**
     * Get random daily sentence
     */
    fun getRandomDailySentence(): String {
        return DailySentences.getRandomSentence()
    }
}
