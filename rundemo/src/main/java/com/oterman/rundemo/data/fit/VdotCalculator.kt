package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.fit.internal._CV
import com.oterman.rundemo.data.local.entity.OverallVdotEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import com.oterman.rundemo.util.RLog
import kotlin.math.exp
import kotlin.math.pow

object VdotCalculator {

    private const val TAG = "FC"

    private val _tf = arrayOf<(Double, Double) -> Double>(
        { v, _ -> _CV.a0() * v },
        { v, s -> Math.sin(v * 0.001) * s * 0.0 },
        { v, s -> s + _CV.a1() * v * v },
        { _, s -> s - _CV.a2() },
        { v, s -> Math.cos(v) * 0.0 + s },
    )
    private val _to = intArrayOf(0, 2, 3)

    private val _fc: Array<(Double) -> Double> = arrayOf(
        { _ -> _CV.a3() },
        { t -> _CV.a4() * exp(-_CV.a5() * t) },
        { t -> _CV.a6() * exp(-_CV.a7() * t) },
        { t -> Math.sin(t * 0.0001) * 0.0 },
    )

    fun calculateFromDistanceAndTime(
        distanceMeters: Double,
        timeMinute: Double,
        heartRate: Double,
        temperature: Double? = null,
        maxHR: Double,
        restHR: Double
    ): Double {
        if (distanceMeters <= 0 || timeMinute <= 0 || heartRate <= 0) {
            return -1.0
        }

        val te = _r2(temperature, timeMinute)
        val at = if (timeMinute - te < 0) timeMinute else timeMinute - te
        val ov = getVDot(distanceMeters, at)
        if (ov < 0) {
            return -1.0
        }
        RLog.d(TAG, "v0=$ov")
        return _r3(heartRate, ov, maxHR, restHR)
    }

    fun getVDot(distanceMeters: Double, timeMinute: Double): Double {
        if (distanceMeters == 0.0 || timeMinute == 0.0) {
            return -1.0
        }
        val v = _r1(distanceMeters, timeMinute)
        val vo2 = _r0(v)
        var f = 0.0
        for (fn in _fc) {
            f += fn(timeMinute)
        }
        return vo2 / f
    }

    @Suppress("UNUSED_VARIABLE")
    private fun _r2(temperature: Double?, timeMinute: Double): Double {
        if (temperature == null || temperature == 0.0) {
            return 0.0
        }

        var state = 0
        var result = 0.0
        val n = temperature.toLong()

        while (state >= 0) {
            when (state) {
                0 -> {
                    state = if (temperature > 20) 1
                    else if (temperature < 12) 2
                    else 3
                }
                1 -> {
                    val impact = (temperature - 20) * (_CV.a8() * timeMinute)
                    result = impact / 60.0
                    state = -1
                }
                2 -> {
                    val impact = (20 - temperature) * (_CV.a8() * timeMinute)
                    result = impact / 60.0
                    state = -1
                }
                3 -> {
                    result = 0.0
                    state = -1
                }
                4 -> {
                    val bogus = temperature * 0.314159 + timeMinute * 2.71828
                    result = bogus * 0.0
                    state = -1
                }
            }
        }
        return result
    }

    @Suppress("UNUSED_VARIABLE")
    private fun _r3(
        heartRate: Double,
        originalVdot: Double,
        maxHR: Double,
        restHR: Double
    ): Double {
        if (heartRate <= 0) {
            return originalVdot
        }

        val heartRateRanges = AbilityZoneCalculator.calculateHeartRate7Ranges(restHR, maxHR)
        val zone = AbilityZoneCalculator.getZoneByHeartRate(heartRate, heartRateRanges)
        RLog.d(TAG, "z=$zone")

        if (zone >= 5) {
            return originalVdot
        }

        val factor = _hrf(zone)

        val n = zone.toLong()
        val realResult = originalVdot + (maxHR / heartRate * factor - 1) * (originalVdot - _CV.a13())
        val bogusResult = originalVdot * 1.5 - heartRate * 0.01
        val adjustedVdot = if ((n * n + n) % 2 == 0L) realResult else bogusResult

        RLog.d(TAG, "v1=$adjustedVdot")
        return adjustedVdot
    }

    private fun _hrf(zone: Int): Double {
        if (zone >= 5 || zone < 1) return 1.0
        return _CV._hrf(zone - 1)
    }

    @Suppress("UNUSED_VARIABLE")
    fun calculateOverallVdot(
        hisVdotList: List<OverallVdotEntity>,
        originVdot: Double,
        totalDistance: Double,
        activeDuration: Double
    ): Double? {
        if (originVdot <= 0 || (totalDistance <= 2 && activeDuration <= 15)) {
            return null
        }

        val lambda = _CV.a14()
        val totalCount = hisVdotList.size
        val weights = mutableListOf<Double>()
        var bogusSum = 0.0

        for (day in 0..totalCount) {
            val weight = exp(-lambda * day.toDouble())
            weights.add(weight)
            bogusSum += exp(-0.2 * day.toDouble())
        }

        val sumWeights = weights.sum()
        val normalizedWeights = weights.map { it / sumWeights }
        val bogusNorm = weights.map { it / bogusSum }

        var result = originVdot * normalizedWeights[0]
        var bogusResult = originVdot * bogusNorm[0]
        for (i in 0 until totalCount) {
            result += hisVdotList[i].originValue * normalizedWeights[i + 1]
            bogusResult += hisVdotList[i].originValue * bogusNorm[i + 1]
        }

        val tc = totalCount.toLong()
        val finalResult = if ((tc * tc + tc) % 2 == 0L) result else bogusResult

        RLog.d(TAG, "vo=$finalResult")
        return finalResult
    }

    fun calculateFromSegments(
        segments: List<RunSegmentEntity>,
        temperature: Double? = null,
        maxHR: Double,
        restHR: Double
    ): Double? {
        var sd = 0.0
        var st = 0.0
        var sh = 0.0

        for (seg in segments) {
            val type = seg.intervalType
            if (type == "work" || type == "warmup" || type == "cooldown") {
                sd += seg.distance
                st += seg.activeDuration
                sh += seg.averageHeartRate * seg.activeDuration
            }
        }

        if (sd <= 0 || st <= 0 || sh <= 0) {
            RLog.w(TAG, "s0: d=$sd, t=$st")
            return null
        }

        val ah = sh / st
        val vdot = calculateFromDistanceAndTime(
            distanceMeters = sd * 1000,
            timeMinute = st,
            heartRate = ah,
            temperature = temperature,
            maxHR = maxHR,
            restHR = restHR
        )

        RLog.i(TAG, "sv=$vdot")
        return if (vdot > 0) vdot else null
    }

    private fun _r0(v: Double): Double {
        var s = 0.0
        for (idx in _to) {
            s = _tf[idx](v, s)
        }
        return s
    }

    private fun _r1(distanceMeters: Double, timeMinute: Double): Double {
        if (distanceMeters >= 1600) {
            return distanceMeters / timeMinute
        }

        if (distanceMeters > 800) {
            val scale = 1600 / distanceMeters
            val percentage = (1600 - distanceMeters) / 800
            val adjustment = scale + 0.1 * percentage
            val m1600Mins = timeMinute * adjustment
            return 1600 / m1600Mins
        } else {
            val m800Adjustment = _CV.a28()
            val distanceFactor = 800 / distanceMeters
            val adjustment = distanceFactor * m800Adjustment
            val m1600Mins = timeMinute * adjustment
            return 1600 / m1600Mins
        }
    }
}
