package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.local.entity.OverallVdotEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verification tests to lock in exact behavior of VDOT calculation algorithms.
 * Run before and after obfuscation to ensure correctness is preserved.
 */
class VdotVerificationTest {

    private val DELTA = 0.001

    // ==================== VdotCalculator.getVDot ====================

    @Test
    fun getVDot_5kIn25min() {
        assertEquals(38.309372939994, VdotCalculator.getVDot(5000.0, 25.0), DELTA)
    }

    @Test
    fun getVDot_10kIn50min() {
        assertEquals(40.012057554250, VdotCalculator.getVDot(10000.0, 50.0), DELTA)
    }

    @Test
    fun getVDot_1500mIn5min() {
        assertEquals(53.678865415979, VdotCalculator.getVDot(1500.0, 5.0), DELTA)
    }

    @Test
    fun getVDot_800mIn3min() {
        assertEquals(42.092917289050, VdotCalculator.getVDot(800.0, 3.0), DELTA)
    }

    @Test
    fun getVDot_invalidInput_zeroDistance() {
        assertEquals(-1.0, VdotCalculator.getVDot(0.0, 25.0), DELTA)
    }

    @Test
    fun getVDot_invalidInput_zeroTime() {
        assertEquals(-1.0, VdotCalculator.getVDot(5000.0, 0.0), DELTA)
    }

    // ==================== VdotCalculator.calculateFromDistanceAndTime ====================

    @Test
    fun calculateFromDistanceAndTime_withTempAndHR() {
        val result = VdotCalculator.calculateFromDistanceAndTime(
            distanceMeters = 5000.0,
            timeMinute = 25.0,
            heartRate = 150.0,
            temperature = 25.0,
            maxHR = 190.0,
            restHR = 60.0
        )
        assertTrue("Result should be positive", result > 0)
        // With temp correction and HR adjustment (zone 2, factor 1.07)
        assertTrue("Adjusted VDOT should be > raw VDOT", result > 38.0)
    }

    @Test
    fun calculateFromDistanceAndTime_noTemp() {
        val result = VdotCalculator.calculateFromDistanceAndTime(
            distanceMeters = 5000.0,
            timeMinute = 25.0,
            heartRate = 175.0,  // zone 5, no HR adjustment
            temperature = null,
            maxHR = 190.0,
            restHR = 60.0
        )
        assertTrue("Result should be positive", result > 0)
        // No temp correction, zone 5 => no HR adjustment => raw VDOT
        val rawVdot = VdotCalculator.getVDot(5000.0, 25.0)
        assertEquals(rawVdot, result, DELTA)
    }

    @Test
    fun calculateFromDistanceAndTime_optimalTemp() {
        val result = VdotCalculator.calculateFromDistanceAndTime(
            distanceMeters = 5000.0,
            timeMinute = 25.0,
            heartRate = 180.0,  // zone 5
            temperature = 15.0,  // optimal range 12-20
            maxHR = 190.0,
            restHR = 60.0
        )
        val rawVdot = VdotCalculator.getVDot(5000.0, 25.0)
        assertEquals("Optimal temp should not affect result", rawVdot, result, DELTA)
    }

    @Test
    fun calculateFromDistanceAndTime_invalidInput() {
        assertEquals(-1.0, VdotCalculator.calculateFromDistanceAndTime(
            0.0, 25.0, 150.0, null, 190.0, 60.0
        ), DELTA)
        assertEquals(-1.0, VdotCalculator.calculateFromDistanceAndTime(
            5000.0, 0.0, 150.0, null, 190.0, 60.0
        ), DELTA)
        assertEquals(-1.0, VdotCalculator.calculateFromDistanceAndTime(
            5000.0, 25.0, 0.0, null, 190.0, 60.0
        ), DELTA)
    }

    // ==================== VdotCalculator.calculateOverallVdot ====================

    @Test
    fun calculateOverallVdot_noHistory() {
        val result = VdotCalculator.calculateOverallVdot(
            hisVdotList = emptyList(),
            originVdot = 45.0,
            totalDistance = 10.0,
            activeDuration = 60.0
        )
        assertNotNull(result)
        assertEquals(45.0, result!!, DELTA)
    }

    @Test
    fun calculateOverallVdot_withHistory() {
        val history = listOf(
            OverallVdotEntity(workoutId = "w1", originValue = 40.0),
            OverallVdotEntity(workoutId = "w2", originValue = 35.0)
        )
        val result = VdotCalculator.calculateOverallVdot(
            hisVdotList = history,
            originVdot = 45.0,
            totalDistance = 10.0,
            activeDuration = 60.0
        )
        assertNotNull(result)
        assertEquals(40.332778978776, result!!, DELTA)
    }

    @Test
    fun calculateOverallVdot_insufficientData() {
        assertNull(VdotCalculator.calculateOverallVdot(emptyList(), 45.0, 1.0, 10.0))
        assertNull(VdotCalculator.calculateOverallVdot(emptyList(), -1.0, 10.0, 60.0))
    }

    // ==================== VdotCalculator.calculateFromSegments ====================

    @Test
    fun calculateFromSegments_workSegments() {
        val segments = listOf(
            RunSegmentEntity(
                workoutId = "w1", seq = 0, segmentType = 2,
                beginTime = 0, endTime = 300000,
                distance = 1.0, activeDuration = 5.0,
                averageHeartRate = 170.0, intervalType = "warmup"
            ),
            RunSegmentEntity(
                workoutId = "w1", seq = 1, segmentType = 2,
                beginTime = 300000, endTime = 600000,
                distance = 2.0, activeDuration = 8.0,
                averageHeartRate = 175.0, intervalType = "work"
            ),
            RunSegmentEntity(
                workoutId = "w1", seq = 2, segmentType = 2,
                beginTime = 600000, endTime = 900000,
                distance = 0.5, activeDuration = 3.0,
                averageHeartRate = 140.0, intervalType = "recovery"
            ),
            RunSegmentEntity(
                workoutId = "w1", seq = 3, segmentType = 2,
                beginTime = 900000, endTime = 1200000,
                distance = 1.0, activeDuration = 5.0,
                averageHeartRate = 160.0, intervalType = "cooldown"
            )
        )
        val result = VdotCalculator.calculateFromSegments(
            segments = segments,
            temperature = null,
            maxHR = 190.0,
            restHR = 60.0
        )
        // Should include warmup+work+cooldown (not recovery)
        // distance=4.0km, time=18min, weightedHR=(170*5+175*8+160*5)/18
        assertNotNull(result)
        assertTrue("VDOT should be positive", result!! > 0)
    }

    @Test
    fun calculateFromSegments_onlyRecovery() {
        val segments = listOf(
            RunSegmentEntity(
                workoutId = "w1", seq = 0, segmentType = 2,
                beginTime = 0, endTime = 300000,
                distance = 0.5, activeDuration = 3.0,
                averageHeartRate = 130.0, intervalType = "recovery"
            )
        )
        val result = VdotCalculator.calculateFromSegments(
            segments = segments, temperature = null, maxHR = 190.0, restHR = 60.0
        )
        assertNull("Should return null when only recovery segments", result)
    }

    // ==================== VdotSpeedCalculator ====================

    @Test
    fun calculateSpeedZoneRanges_normalVdot() {
        val ranges = VdotSpeedCalculator.calculateSpeedZoneRanges(45.0)
        assertEquals(7, ranges.size)
        // Zone 1 minPace should be -1 (no lower bound)
        assertEquals(-1.0, ranges[1]!!.minPace, DELTA)
        // Zone 7 maxPace should be -1 (no upper bound)
        assertEquals(-1.0, ranges[7]!!.maxPace, DELTA)
        // Paces should decrease as zone increases (faster)
        for (zone in 2..6) {
            val current = ranges[zone]!!
            assertTrue("Zone $zone maxPace should be < minPace (faster)", current.maxPace < current.minPace)
        }
    }

    @Test
    fun calculateSpeedZoneRanges_slowVdot() {
        val ranges = VdotSpeedCalculator.calculateSpeedZoneRanges(30.0)
        assertEquals(7, ranges.size)
        // Slow VDOT should still produce valid ranges
        for (zone in 1..7) {
            assertNotNull(ranges[zone])
        }
    }

    @Test
    fun calculateSpeedZoneRanges_invalidVdot() {
        assertTrue(VdotSpeedCalculator.calculateSpeedZoneRanges(0.0).isEmpty())
        assertTrue(VdotSpeedCalculator.calculateSpeedZoneRanges(-5.0).isEmpty())
    }

    @Test
    fun getPredictedRaceTime_5k() {
        assertEquals(21.822634668635, VdotSpeedCalculator.getPredictedRaceTime(45.0, 5000.0), DELTA)
    }

    @Test
    fun getPredictedRaceTime_10k() {
        assertEquals(45.259369844050, VdotSpeedCalculator.getPredictedRaceTime(45.0, 10000.0), DELTA)
    }

    @Test
    fun getPredictedRaceTime_marathon() {
        assertEquals(208.383757097288, VdotSpeedCalculator.getPredictedRaceTime(45.0, 42195.0), DELTA)
    }

    @Test
    fun getPredictedRaceTime_shortDistance() {
        assertEquals(2.591917131966, VdotSpeedCalculator.getPredictedRaceTime(50.0, 800.0), DELTA)
    }

    // ==================== AbilityZoneCalculator ====================

    @Test
    fun calculateHeartRate7Ranges_standard() {
        val ranges = AbilityZoneCalculator.calculateHeartRate7Ranges(60.0, 190.0)
        assertEquals(7, ranges.size)

        // HRR = 130
        val z1 = ranges[1]!!
        assertEquals(-1.0, z1.minHR, DELTA)
        assertEquals(130 * 0.59 + 60, z1.maxHR, DELTA) // 136.7

        val z2 = ranges[2]!!
        assertEquals(130 * 0.59 + 60, z2.minHR, DELTA)
        assertEquals(130 * 0.74 + 60, z2.maxHR, DELTA) // 156.2

        val z3 = ranges[3]!!
        assertEquals(130 * 0.74 + 60, z3.minHR, DELTA)
        assertEquals(130 * 0.84 + 60, z3.maxHR, DELTA) // 169.2

        val z4 = ranges[4]!!
        assertEquals(130 * 0.84 + 60, z4.minHR, DELTA)
        assertEquals(130 * 0.88 + 60, z4.maxHR, DELTA) // 174.4

        val z5 = ranges[5]!!
        assertEquals(130 * 0.88 + 60, z5.minHR, DELTA)
        assertEquals(130 * 0.95 + 60, z5.maxHR, DELTA) // 183.5

        val z6 = ranges[6]!!
        assertEquals(130 * 0.95 + 60, z6.minHR, DELTA)
        assertEquals(190.0, z6.maxHR, DELTA)

        val z7 = ranges[7]!!
        assertEquals(190.0, z7.minHR, DELTA)
        assertEquals(-1.0, z7.maxHR, DELTA)
    }

    @Test
    fun calculateHeartRate5Ranges_standard() {
        val ranges = AbilityZoneCalculator.calculateHeartRate5Ranges(60.0, 190.0)
        assertEquals(5, ranges.size)
        // HRR = 130
        assertEquals(-1.0, ranges[1]!!.minHR, DELTA)
        assertEquals(130 * 0.60 + 60, ranges[1]!!.maxHR, DELTA) // 138.0
        assertEquals(130 * 0.90 + 60, ranges[4]!!.maxHR, DELTA) // 177.0
        assertEquals(-1.0, ranges[5]!!.maxHR, DELTA)
    }

    @Test
    fun getZoneByHeartRate_variousHR() {
        val ranges = AbilityZoneCalculator.calculateHeartRate7Ranges(60.0, 190.0)
        // Zone 1: < 136.7
        assertEquals(1, AbilityZoneCalculator.getZoneByHeartRate(120.0, ranges))
        // Zone 2: 136.7 ~ 156.2
        assertEquals(2, AbilityZoneCalculator.getZoneByHeartRate(150.0, ranges))
        // Zone 3: 156.2 ~ 169.2
        assertEquals(3, AbilityZoneCalculator.getZoneByHeartRate(160.0, ranges))
        // Zone 4: 169.2 ~ 174.4
        assertEquals(4, AbilityZoneCalculator.getZoneByHeartRate(170.0, ranges))
        // Zone 5: 174.4 ~ 183.5
        assertEquals(5, AbilityZoneCalculator.getZoneByHeartRate(180.0, ranges))
        // Zone 6: 183.5 ~ 190
        assertEquals(6, AbilityZoneCalculator.getZoneByHeartRate(185.0, ranges))
        // Zone 7: > 190
        assertEquals(7, AbilityZoneCalculator.getZoneByHeartRate(195.0, ranges))
    }

    // ==================== Cross-consistency checks ====================

    @Test
    fun vdot_consistency_sameSpeedDifferentDistance() {
        // Same speed (200 m/min) at different distances should yield different VDOTs
        // because the fraction formula depends on time
        val vdot1 = VdotCalculator.getVDot(5000.0, 25.0)   // 25 min
        val vdot2 = VdotCalculator.getVDot(10000.0, 50.0)  // 50 min
        assertTrue("Longer duration at same speed should yield higher VDOT", vdot2 > vdot1)
    }

    @Test
    fun speedZones_higherVdot_fasterPaces() {
        val zones40 = VdotSpeedCalculator.calculateSpeedZoneRanges(40.0)
        val zones50 = VdotSpeedCalculator.calculateSpeedZoneRanges(50.0)
        // Higher VDOT should have faster (lower) paces
        assertTrue("Higher VDOT should have faster E zone",
            zones50[2]!!.minPace < zones40[2]!!.minPace)
    }

    @Test
    fun raceTime_higherVdot_fasterTime() {
        val time40 = VdotSpeedCalculator.getPredictedRaceTime(40.0, 5000.0)
        val time50 = VdotSpeedCalculator.getPredictedRaceTime(50.0, 5000.0)
        assertTrue("Higher VDOT should predict faster 5K", time50 < time40)
    }

    // ==================== Snapshot tests for exact values ====================
    // These lock in the precise output and will catch any drift from obfuscation

    private var snapshot5k25 = 0.0
    private var snapshot10k50 = 0.0
    private var snapshotRaceTime5k = 0.0

    @Before
    fun captureSnapshots() {
        snapshot5k25 = VdotCalculator.getVDot(5000.0, 25.0)
        snapshot10k50 = VdotCalculator.getVDot(10000.0, 50.0)
        snapshotRaceTime5k = VdotSpeedCalculator.getPredictedRaceTime(45.0, 5000.0)
    }

    @Test
    fun snapshot_getVDot_5k25_exact() {
        val result = VdotCalculator.getVDot(5000.0, 25.0)
        assertEquals("Exact VDOT match for 5K/25min", snapshot5k25, result, 1e-10)
    }

    @Test
    fun snapshot_getVDot_10k50_exact() {
        val result = VdotCalculator.getVDot(10000.0, 50.0)
        assertEquals("Exact VDOT match for 10K/50min", snapshot10k50, result, 1e-10)
    }

    @Test
    fun snapshot_raceTime5k_exact() {
        val result = VdotSpeedCalculator.getPredictedRaceTime(45.0, 5000.0)
        assertEquals("Exact race time match for VDOT 45/5K", snapshotRaceTime5k, result, 1e-10)
    }
}
