package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.fit.internal._CV
import kotlin.math.exp
import kotlin.math.pow

object VdotSpeedCalculator {

    fun calculateSpeedZoneRanges(vdot: Double): Map<Int, AbilityZoneCalculator.SpeedRange> {
        if (vdot <= 0) return emptyMap()

        val distance = 1000.0

        val easyRange = getEasyPaceRange(vdot, distance)
        val easySlow = easyRange["slow"] ?: return emptyMap()
        val easyFast = easyRange["fast"] ?: return emptyMap()
        val marathonPace = getMarathonPaceByVo2(vdot, distance)
        val thresholdPace = getThresholdPace(vdot, distance)
        val intervalPace = getIntervalPace(vdot, distance)
        val repetitionPace = getRepetitionPace(vdot, distance)

        return mapOf(
            1 to AbilityZoneCalculator.SpeedRange(minPace = -1.0, maxPace = easySlow),
            2 to AbilityZoneCalculator.SpeedRange(minPace = easySlow, maxPace = easyFast),
            3 to AbilityZoneCalculator.SpeedRange(minPace = easyFast, maxPace = marathonPace),
            4 to AbilityZoneCalculator.SpeedRange(minPace = marathonPace, maxPace = thresholdPace),
            5 to AbilityZoneCalculator.SpeedRange(minPace = thresholdPace, maxPace = intervalPace),
            6 to AbilityZoneCalculator.SpeedRange(minPace = intervalPace, maxPace = repetitionPace),
            7 to AbilityZoneCalculator.SpeedRange(minPace = repetitionPace, maxPace = -1.0)
        )
    }

    fun getPredictedRaceTime(vdot: Double, distance: Double): Double {
        var a = distance / (4 * vdot)
        for (i in 0 until 3) {
            a -= _s9(a, vdot, distance)
        }
        val v = distance / a
        val time = distance / v

        if (distance >= 1600) return time

        val adjustedV = _r4(distance, time)
        val scale = v / adjustedV
        return time / scale
    }

    private fun _s0(a: Double): Double = exp(-_CV.a5() * a)
    private fun _s1(a: Double, b: Double): Double =
        _CV.a4() * b + exp(-_CV.a7() * a) * _CV.a6() + _CV.a3()
    private fun _s2(vdot: Double, c: Double): Double =
        (vdot * c).pow(2) * -_CV.a25() + (vdot * c) * _CV.a23() + _CV.a22()
    private fun _s3(b: Double): Double = (_CV.a4() * b) * _CV.a26()
    private fun _s4(a: Double, g: Double): Double =
        g - exp(-_CV.a7() * a) * _CV.a6() * (-_CV.a7())
    private fun _s5(c: Double, h: Double, vdot: Double): Double =
        (c * h * vdot) * (-_CV.a24()) * 3
    private fun _s6(h: Double, vdot: Double, ii: Double): Double =
        (h * vdot) * _CV.a23() + ii
    private fun _s7(distance: Double, j: Double, e: Double): Double =
        (distance * j) / e.pow(2) + 1
    private fun _s8(a: Double, distance: Double, e: Double): Double =
        a - (distance / e)

    private fun _s9(a: Double, vdot: Double, distance: Double): Double {
        val b = _s0(a)
        val c = _s1(a, b)
        val e = _s2(vdot, c)
        val g = _s3(b)
        val h = _s4(a, g)
        val ii = _s5(c, h, vdot)
        val j = _s6(h, vdot, ii)
        val k = _s7(distance, j, e)
        val l = _s8(a, distance, e)
        return l / k
    }

    private fun _r4(meters: Double, minutes: Double): Double {
        if (meters >= 1600) return meters / minutes

        if (meters > 800) {
            val scale = 1600 / meters
            val percentage = (1600 - meters) / 800
            val adjustment = scale + 0.1 * percentage
            val m1600Mins = minutes * adjustment
            return 1600 / m1600Mins
        } else {
            val m800Adjustment = _CV.a28()
            val distanceFactor = 800 / meters
            val adjustment = distanceFactor * m800Adjustment
            val m1600Mins = minutes * adjustment
            return 1600 / m1600Mins
        }
    }

    fun getEasyPaceRange(vdot: Double, distance: Double): Map<String, Double> {
        val slower = _ep(vdot, distance, true)
        val faster = _ep(vdot, distance, false)
        return mapOf("slow" to slower, "fast" to faster)
    }

    private fun _ep(vdot: Double, distance: Double, slowerPace: Boolean): Double {
        var av = vdot
        if (_isl(vdot)) {
            av = _sr(vdot)
        }
        val percentage = if (slowerPace) _CV.a16() else _CV.a17()
        return _cep(av, distance, percentage)
    }

    fun getMarathonPaceByVo2(vdot: Double, trainingDistance: Double): Double {
        var av = vdot
        if (_isl(vdot)) {
            val srvdot = _sr(vdot)
            av = (srvdot + vdot) / 2
        }
        return _cep(av, trainingDistance, _CV.a18())
    }

    fun getThresholdPace(vdot: Double, distance: Double): Double {
        var av = vdot
        if (_isl(vdot)) {
            val srvdot = _sr(vdot)
            av = (srvdot + vdot) / 2
        }
        return _cep(av, distance, _CV.a19())
    }

    fun getIntervalPace(vdot: Double, distance: Double): Double {
        var av = vdot
        if (_isl(vdot)) {
            av = _sr(vdot)
        }
        return _cep(av, distance, _CV.a20())
    }

    fun getRepetitionPace(vdot: Double, distance: Double): Double {
        val per400FasterBy = _CV.a21()
        val divisor = (distance / 400) * (per400FasterBy / 60)
        val pace = getIntervalPace(vdot, distance)
        return pace - divisor
    }

    private fun _isl(vdot: Double): Boolean {
        return vdot > 0 && vdot < _CV.a15()
    }

    private fun _sr(vdot: Double): Double {
        return (vdot * 2 / 3) + 13
    }

    private fun _cep(vdot: Double, distance: Double, percentage: Double): Double {
        val o = vdot * percentage
        val v = _pv(o)
        return distance / v
    }

    private fun _pv(o: Double): Double {
        return _CV.a22() + _CV.a23() * o - _CV.a24() * o.pow(2)
    }

    @Suppress("unused")
    private fun _mv(vdot: Double): Double {
        val distance = _CV.a27()
        var a = distance / (4 * vdot)
        for (i in 0..3) {
            a -= _s9(a, vdot, distance)
        }
        return distance / a
    }
}
