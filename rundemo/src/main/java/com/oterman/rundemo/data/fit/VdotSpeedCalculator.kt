package com.oterman.rundemo.data.fit

import kotlin.math.exp
import kotlin.math.pow

/**
 * VDOT配速计算器
 * 对齐iOS VdotSpeedCalculater
 * 基于VDOT计算各训练配速区间
 */
object VdotSpeedCalculator {

    private const val SLOW_VDOT_LIMIT = 39.0

    /**
     * 计算7个配速区间范围
     * 对齐iOS AbilityZoneManager.calculateSpeedZone (Int版本)
     *
     * @param vdot VDOT值
     * @return Map<区间编号, SpeedRange(min/km配速)>
     */
    fun calculateSpeedZoneRanges(vdot: Double): Map<Int, AbilityZoneCalculator.SpeedRange> {
        if (vdot <= 0) return emptyMap()

        val distance = 1000.0

        // E区: 轻松跑
        val easyRange = getEasyPaceRange(vdot, distance)
        val easySlow = easyRange["slow"] ?: return emptyMap()
        val easyFast = easyRange["fast"] ?: return emptyMap()

        // M区: 马拉松配速
        val marathonPace = getMarathonPaceByVo2(vdot, distance)

        // T区: 乳酸阈值
        val thresholdPace = getThresholdPace(vdot, distance)

        // I区: 间歇
        val intervalPace = getIntervalPace(vdot, distance)

        // R区: 冲刺
        val repetitionPace = getRepetitionPace(vdot, distance)

        return mapOf(
            // Zone 1: 恢复 (比E区慢的)
            1 to AbilityZoneCalculator.SpeedRange(minPace = -1.0, maxPace = easySlow),
            // Zone 2: 轻松跑 E
            2 to AbilityZoneCalculator.SpeedRange(minPace = easySlow, maxPace = easyFast),
            // Zone 3: 马拉松 M
            3 to AbilityZoneCalculator.SpeedRange(minPace = easyFast, maxPace = marathonPace),
            // Zone 4: 乳酸阈值 T
            4 to AbilityZoneCalculator.SpeedRange(minPace = marathonPace, maxPace = thresholdPace),
            // Zone 5: 无氧耐力 A
            5 to AbilityZoneCalculator.SpeedRange(minPace = thresholdPace, maxPace = intervalPace),
            // Zone 6: 间歇 I
            6 to AbilityZoneCalculator.SpeedRange(minPace = intervalPace, maxPace = repetitionPace),
            // Zone 7: 冲刺 R (比R区快的)
            7 to AbilityZoneCalculator.SpeedRange(minPace = repetitionPace, maxPace = -1.0)
        )
    }

    // ==================== 配速计算方法 ====================

    /**
     * 预测比赛时间
     */
    fun getPredictedRaceTime(vdot: Double, distance: Double): Double {
        var a = distance / (4 * vdot)
        for (i in 0 until 3) {
            val b = exp(-0.193261 * a)
            val c = 0.298956 * b + exp(-0.012778 * a) * 0.189439 + 0.8
            val e = (vdot * c).pow(2) * -0.0075 + (vdot * c) * 5.000663 + 29.54
            val g = (0.298956 * b) * 0.19326
            val h = g - exp(-0.012778 * a) * 0.189439 * (-0.012778)
            val ii = (c * h * vdot) * (-0.007546) * 3
            val j = (h * vdot) * 5.000663 + ii
            val k = (distance * j) / e.pow(2) + 1
            val l = a - (distance / e)
            val p = l / k
            a -= p
        }
        val v = distance / a
        val time = distance / v

        if (distance >= 1600) return time

        val adjustedV = getVDOTSpeedParam(distance, time)
        val scale = v / adjustedV
        return time / scale
    }

    private fun getVDOTSpeedParam(meters: Double, minutes: Double): Double {
        if (meters >= 1600) return meters / minutes

        if (meters > 800) {
            val scale = 1600 / meters
            val percentage = (1600 - meters) / 800
            val adjustment = scale + 0.1 * percentage
            val m1600Mins = minutes * adjustment
            return 1600 / m1600Mins
        } else {
            val m800Adjustment = 2.1
            val distanceFactor = 800 / meters
            val adjustment = distanceFactor * m800Adjustment
            val m1600Mins = minutes * adjustment
            return 1600 / m1600Mins
        }
    }

    /**
     * 轻松跑配速范围
     */
    fun getEasyPaceRange(vdot: Double, distance: Double): Map<String, Double> {
        val slower = getEasyPace(vdot, distance, slowerPace = true)
        val faster = getEasyPace(vdot, distance, slowerPace = false)
        return mapOf("slow" to slower, "fast" to faster)
    }

    private fun getEasyPace(vdot: Double, distance: Double, slowerPace: Boolean): Double {
        var adjustedVdot = vdot
        if (isSlowVdot(vdot)) {
            adjustedVdot = getSRVDOT(vdot)
        }
        val percentage = if (slowerPace) 0.62 else 0.70
        return getCustomEffortPace(adjustedVdot, distance, percentage)
    }

    /**
     * 马拉松配速
     */
    fun getMarathonPaceByVo2(vdot: Double, trainingDistance: Double): Double {
        var adjustedVdot = vdot
        if (isSlowVdot(vdot)) {
            val srvdot = getSRVDOT(vdot)
            adjustedVdot = (srvdot + vdot) / 2
        }
        return getCustomEffortPace(adjustedVdot, trainingDistance, 0.84)
    }

    /**
     * 乳酸阈值配速
     */
    fun getThresholdPace(vdot: Double, distance: Double): Double {
        var adjustedVdot = vdot
        if (isSlowVdot(vdot)) {
            val srvdot = getSRVDOT(vdot)
            adjustedVdot = (srvdot + vdot) / 2
        }
        return getCustomEffortPace(adjustedVdot, distance, 0.88)
    }

    /**
     * 间歇配速
     */
    fun getIntervalPace(vdot: Double, distance: Double): Double {
        var adjustedVdot = vdot
        if (isSlowVdot(vdot)) {
            adjustedVdot = getSRVDOT(vdot)
        }
        return getCustomEffortPace(adjustedVdot, distance, 0.975)
    }

    /**
     * 冲刺配速
     */
    fun getRepetitionPace(vdot: Double, distance: Double): Double {
        val per400FasterBy = 6.0
        val divisor = (distance / 400) * (per400FasterBy / 60)
        val pace = getIntervalPace(vdot, distance)
        return pace - divisor
    }

    // ==================== 私有辅助方法 ====================

    private fun isSlowVdot(vdot: Double): Boolean {
        return vdot > 0 && vdot < SLOW_VDOT_LIMIT
    }

    private fun getSRVDOT(vdot: Double): Double {
        return (vdot * 2 / 3) + 13
    }

    private fun getCustomEffortPace(vdot: Double, distance: Double, percentage: Double): Double {
        val o = vdot * percentage
        val v = getPaceVelocity(o)
        return distance / v
    }

    private fun getPaceVelocity(o: Double): Double {
        return 29.54 + 5.000663 * o - 0.007546 * o.pow(2)
    }

    private fun getMarathonVelocity(vdot: Double): Double {
        val distance = 42195.0
        var a = distance / (4 * vdot)
        for (i in 0..3) {
            val b = exp(-0.193261 * a)
            val c = 0.298956 * b + exp(-0.012778 * a) * 0.189439 + 0.8
            val e = (vdot * c).pow(2) * -0.0075 + (vdot * c) * 5.000663 + 29.54
            val g = (0.298956 * b) * 0.19326
            val h = g - exp(-0.012778 * a) * 0.189439 * (-0.012778)
            val ii = (c * h * vdot) * (-0.007546) * 3
            val j = (h * vdot) * 5.000663 + ii
            val k = (distance * j) / e.pow(2) + 1
            val l = a - (distance / e)
            val p = l / k
            a -= p
        }
        return distance / a
    }
}

