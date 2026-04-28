package com.oterman.rundemo.data.gpx

import com.oterman.rundemo.data.fit.FitDataMapper
import com.oterman.rundemo.data.fit.FitDeviceInfo
import com.oterman.rundemo.data.fit.FitFileId
import com.oterman.rundemo.data.fit.FitParseResult
import com.oterman.rundemo.data.fit.FitRecord
import com.oterman.rundemo.data.fit.FitSession
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GpxDataMapper {

    private const val FIT_EPOCH_OFFSET_MS = 631065600000L
    private const val EARTH_RADIUS_M = 6371000.0

    /**
     * 生成确定性 originId。
     * 使用与 FitDataMapper.generateWorkoutId 相同的算法（SHA-256），
     * 时间戳向下取整到秒以匹配 FIT 时间戳精度。
     */
    fun generateOriginId(fileName: String, firstTrackpointTimeMs: Long): String {
        val roundedMs = (firstTrackpointTimeMs / 1000L) * 1000L
        return FitDataMapper.generateWorkoutId(fileName, roundedMs)
    }

    /**
     * 将 GpxParseResult 适配为 FitParseResult，供 FitRecordProcessor 统一处理。
     * laps 始终为空列表，FitDataMapper.toSegmentEntities 会返回空列表，
     * 由 GpxImportService 在 processFitData 之后调用 KilometerSegmentCalculator 补充公里分段。
     */
    fun toFitParseResult(gpxResult: GpxParseResult, fileName: String): FitParseResult {
        val points = gpxResult.trackPoints
        val cumulativeDistances = buildCumulativeDistances(points)
        val totalDistanceM = cumulativeDistances.lastOrNull() ?: 0.0

        val elapsedSeconds = ((gpxResult.endTime - gpxResult.startTime) / 1000.0).toFloat()
            .coerceAtLeast(0f)
        val avgSpeedMs = if (elapsedSeconds > 0) (totalDistanceM / elapsedSeconds).toFloat() else 0f

        val hrValues = points.mapNotNull { it.heartRate }
        val cadValues = points.mapNotNull { it.cadence }

        val altValues = points.mapNotNull { it.altitude }
        var totalAscent = 0
        for (i in 1 until altValues.size) {
            val diff = altValues[i] - altValues[i - 1]
            if (diff > 0) totalAscent += diff.toInt()
        }

        val startFitEpoch = millisToFitTimestamp(gpxResult.startTime)

        val session = FitSession(
            sport = "RUNNING",
            subSport = null,
            startTime = startFitEpoch,
            totalElapsedTime = elapsedSeconds,
            totalTimerTime = elapsedSeconds,
            totalDistance = totalDistanceM.toFloat(),
            totalCalories = null,
            avgSpeed = avgSpeedMs,
            maxSpeed = null,
            avgHeartRate = hrValues.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            maxHeartRate = hrValues.maxOrNull(),
            minHeartRate = hrValues.minOrNull(),
            avgCadence = cadValues.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            maxCadence = cadValues.maxOrNull(),
            avgPower = null,
            maxPower = null,
            totalAscent = if (totalAscent > 0) totalAscent else null,
            totalDescent = null,
            avgStepLength = null,
            avgVerticalOscillation = null,
            avgStanceTime = null,
            totalStrides = null,
            avgTemperature = null,
            trainingEffect = null,
            anaerobicTrainingEffect = null
        )

        val records = points.mapIndexed { i, pt ->
            val speedMs: Float? = if (i > 0) {
                val dt = (pt.timestamp - points[i - 1].timestamp) / 1000.0
                val dx = cumulativeDistances[i] - cumulativeDistances[i - 1]
                if (dt > 0) (dx / dt).toFloat() else null
            } else null

            FitRecord(
                timestamp = millisToFitTimestamp(pt.timestamp),
                positionLat = degreesToSemicircles(pt.latitude),
                positionLong = degreesToSemicircles(pt.longitude),
                altitude = pt.altitude?.toFloat(),
                heartRate = pt.heartRate,
                cadence = pt.cadence,   // 单脚 spm；FitDataMapper.toSamplePointEntities 会 ×2
                speed = speedMs,
                power = null,
                distance = cumulativeDistances[i].toFloat(),
                stepLength = null,
                verticalOscillation = null,
                stanceTime = null
            )
        }

        return FitParseResult(
            fileId = FitFileId(
                type = "ACTIVITY",
                manufacturer = gpxResult.creator,
                product = null,
                serialNumber = null,
                timeCreated = startFitEpoch
            ),
            session = session,
            laps = emptyList(),
            records = records,
            events = emptyList(),
            deviceInfo = null
        )
    }

    private fun buildCumulativeDistances(points: List<GpxTrackPoint>): List<Double> {
        val result = ArrayList<Double>(points.size)
        var cumDist = 0.0
        if (points.isNotEmpty()) result.add(0.0)
        for (i in 1 until points.size) {
            cumDist += haversine(
                points[i - 1].latitude, points[i - 1].longitude,
                points[i].latitude, points[i].longitude
            )
            result.add(cumDist)
        }
        return result
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun degreesToSemicircles(degrees: Double): Int {
        return (degrees * (Int.MAX_VALUE.toLong() + 1L) / 180.0).toInt()
    }

    // 对外暴露，GpxImportService 需要在 buildUserConfig 中转换日期
    fun millisToFitTimestamp(ms: Long): Long = (ms - FIT_EPOCH_OFFSET_MS) / 1000L
}
