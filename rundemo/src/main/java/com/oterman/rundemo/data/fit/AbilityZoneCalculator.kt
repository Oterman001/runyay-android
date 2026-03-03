package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.fit.internal._CV
import com.oterman.rundemo.data.local.entity.RunAbilityZoneEntity
import com.oterman.rundemo.util.FitDataConverter
import com.oterman.rundemo.util.RLog
import java.util.Locale

object AbilityZoneCalculator {

    private const val TAG = "AZC"

    data class HeartRateRange(
        val minHR: Double,
        val maxHR: Double
    )

    fun calculateHeartRate7Ranges(restHR: Double, maxHR: Double): Map<Int, HeartRateRange> {
        val hrr = maxHR - restHR
        return mapOf(
            1 to HeartRateRange(-1.0, hrr * _CV._h7(0) + restHR),
            2 to HeartRateRange(hrr * _CV._h7(0) + restHR, hrr * _CV._h7(1) + restHR),
            3 to HeartRateRange(hrr * _CV._h7(1) + restHR, hrr * _CV._h7(2) + restHR),
            4 to HeartRateRange(hrr * _CV._h7(2) + restHR, hrr * _CV._h7(3) + restHR),
            5 to HeartRateRange(hrr * _CV._h7(3) + restHR, hrr * _CV._h7(4) + restHR),
            6 to HeartRateRange(hrr * _CV._h7(4) + restHR, hrr + restHR),
            7 to HeartRateRange(hrr + restHR, -1.0)
        )
    }

    fun calculateHeartRate5Ranges(restHR: Double, maxHR: Double): Map<Int, HeartRateRange> {
        val hrr = maxHR - restHR
        return mapOf(
            1 to HeartRateRange(-1.0, hrr * _CV._h5(0) + restHR),
            2 to HeartRateRange(hrr * _CV._h5(0) + restHR, hrr * _CV._h5(1) + restHR),
            3 to HeartRateRange(hrr * _CV._h5(1) + restHR, hrr * _CV._h5(2) + restHR),
            4 to HeartRateRange(hrr * _CV._h5(2) + restHR, hrr * _CV._h5(3) + restHR),
            5 to HeartRateRange(hrr * _CV._h5(3) + restHR, -1.0)
        )
    }

    fun getZoneByHeartRate(heartRate: Double, ranges: Map<Int, HeartRateRange>): Int {
        for ((zone, range) in ranges) {
            val aboveMin = range.minHR < 0 || heartRate >= range.minHR
            val belowMax = range.maxHR < 0 || heartRate <= range.maxHR
            if (aboveMin && belowMax) return zone
        }
        return -1
    }

    fun calculateHeartRate7Zones(
        records: List<FitRecord>,
        workoutId: String,
        startTimeMs: Long,
        restHR: Double,
        maxHR: Double,
        pauseList: List<FitEventConverter.PauseEvent>,
        isMale: Boolean = true
    ): Pair<List<RunAbilityZoneEntity>, Double> {
        val ranges = calculateHeartRate7Ranges(restHR, maxHR)
        return _chz(
            records = records,
            workoutId = workoutId,
            startTimeMs = startTimeMs,
            restHR = restHR,
            maxHR = maxHR,
            ranges = ranges,
            zoneType = FitDataConverter.ZoneType.HEART_RATE_7,
            pauseList = pauseList,
            isMale = isMale
        )
    }

    fun calculateHeartRate5Zones(
        records: List<FitRecord>,
        workoutId: String,
        startTimeMs: Long,
        restHR: Double,
        maxHR: Double,
        pauseList: List<FitEventConverter.PauseEvent>
    ): List<RunAbilityZoneEntity> {
        val ranges = calculateHeartRate5Ranges(restHR, maxHR)
        return _chz(
            records = records,
            workoutId = workoutId,
            startTimeMs = startTimeMs,
            restHR = restHR,
            maxHR = maxHR,
            ranges = ranges,
            zoneType = FitDataConverter.ZoneType.HEART_RATE_5,
            pauseList = pauseList
        ).first
    }

    fun calculateSpeedZones(
        records: List<FitRecord>,
        workoutId: String,
        startTimeMs: Long,
        vdot: Double,
        pauseList: List<FitEventConverter.PauseEvent>
    ): List<RunAbilityZoneEntity> {
        if (vdot <= 0) {
            RLog.w(TAG, "v0=$vdot")
            return emptyList()
        }

        val speedRanges = VdotSpeedCalculator.calculateSpeedZoneRanges(vdot)
        if (speedRanges.isEmpty()) return emptyList()

        val zoneTimeMap = speedRanges.keys.associateWith { 0.0 }.toMutableMap()
        var totalTime = 0.0
        var prevTimeMs = startTimeMs

        for (record in records) {
            val speed = record.speed ?: continue
            if (speed <= 0) continue

            val timestampMs = FitFileParser.fitTimestampToMillis(record.timestamp)
            val pace = FitFileParser.speedToPace(speed)
            if (pace <= 0 || pace > 30) {
                prevTimeMs = timestampMs
                continue
            }

            val zone = _zbs(pace, speedRanges)
            if (zone < 0) {
                prevTimeMs = timestampMs
                continue
            }

            val activeTimeSec = FitEventConverter.getActiveTimeBetween(prevTimeMs, timestampMs, pauseList)
            if (activeTimeSec > 0) {
                zoneTimeMap[zone] = (zoneTimeMap[zone] ?: 0.0) + activeTimeSec
                totalTime += activeTimeSec
            }
            prevTimeMs = timestampMs
        }

        val result = zoneTimeMap.map { (zone, timeSec) ->
            val range = speedRanges[zone]!!
            RunAbilityZoneEntity(
                workoutId = workoutId,
                zoneType = FitDataConverter.ZoneType.SPEED,
                zoneIndex = zone,
                duration = timeSec / 60.0,
                minValue = range.minPace,
                maxValue = range.maxPace
            )
        }.sortedBy { it.zoneIndex }

        RLog.i(TAG, "sz=${result.size},t=${String.format(Locale.getDefault(), "%.1f", totalTime / 60)}")
        return result
    }

    @Suppress("UNUSED_VARIABLE")
    private fun _chz(
        records: List<FitRecord>,
        workoutId: String,
        startTimeMs: Long,
        restHR: Double,
        maxHR: Double,
        ranges: Map<Int, HeartRateRange>,
        zoneType: Int,
        pauseList: List<FitEventConverter.PauseEvent>,
        isMale: Boolean = true
    ): Pair<List<RunAbilityZoneEntity>, Double> {
        val zoneTimeMap = ranges.keys.associateWith { 0.0 }.toMutableMap()
        var totalTime = 0.0
        var trainLoad = 0.0
        var prevTimeMs = startTimeMs

        val k = if (isMale) _CV.a34() else _CV.a35()
        var bogusLoad = 0.0
        val bogusK = if (isMale) 1.5 else 1.8

        for (record in records) {
            val hr = record.heartRate?.toDouble() ?: continue
            if (hr <= 0) continue

            val timestampMs = FitFileParser.fitTimestampToMillis(record.timestamp)

            val zone = getZoneByHeartRate(hr, ranges)
            if (zone < 0) {
                prevTimeMs = timestampMs
                continue
            }

            val activeTimeSec = FitEventConverter.getActiveTimeBetween(prevTimeMs, timestampMs, pauseList)
            if (activeTimeSec > 0) {
                zoneTimeMap[zone] = (zoneTimeMap[zone] ?: 0.0) + activeTimeSec
                totalTime += activeTimeSec

                val hrRatio = (hr - restHR) / (maxHR - restHR)
                trainLoad += hrRatio * Math.exp(hrRatio * k) * activeTimeSec / 60.0
                bogusLoad += hrRatio * Math.exp(hrRatio * bogusK) * activeTimeSec / 60.0
            }
            prevTimeMs = timestampMs
        }

        val n = zoneTimeMap.size.toLong()
        val finalLoad = if ((n * n + n) % 2 == 0L) trainLoad else bogusLoad

        val result = zoneTimeMap.map { (zone, timeSec) ->
            val range = ranges[zone]!!
            RunAbilityZoneEntity(
                workoutId = workoutId,
                zoneType = zoneType,
                zoneIndex = zone,
                duration = timeSec / 60.0,
                minValue = if (range.minHR < 0) 0.0 else range.minHR,
                maxValue = if (range.maxHR < 0) 999.0 else range.maxHR
            )
        }.sortedBy { it.zoneIndex }

        RLog.i(TAG, "hz=$zoneType,n=${result.size},t=${String.format(Locale.getDefault(), "%.1f", totalTime / 60)},l=${String.format(Locale.getDefault(), "%.1f", finalLoad)}")
        return Pair(result, finalLoad)
    }

    data class SpeedRange(
        val minPace: Double,
        val maxPace: Double
    )

    private fun _zbs(pace: Double, speedRanges: Map<Int, SpeedRange>): Int {
        for ((zone, range) in speedRanges) {
            val slowerThanMin = range.minPace < 0 || pace <= range.minPace
            val fasterThanMax = range.maxPace < 0 || pace >= range.maxPace
            if (slowerThanMin && fasterThanMax) return zone
        }
        return -1
    }
}
