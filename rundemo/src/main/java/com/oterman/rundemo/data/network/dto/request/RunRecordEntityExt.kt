package com.oterman.rundemo.data.network.dto.request

import com.oterman.rundemo.data.local.entity.RunRecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun RunRecordEntity.toUpdateRequest(): RunSummaryUpdateRequest {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    return RunSummaryUpdateRequest(
        summaryId = originId ?: "",
        distanceInMeters = totalDistance * 1000.0,
        averageHeartRate = averageHeartRate.takeIf { it > 0 }?.toInt(),
        maxHeartRate = maxHeartRate.takeIf { it > 0 },
        minHeartRate = minHeartRate.takeIf { it > 0 }?.toInt(),
        averagePace = averageSpeed.takeIf { it > 0 },
        maxSpeed = maxSpeed.takeIf { it > 0 },
        averagePower = averagePower.takeIf { it > 0 },
        maxPower = maxPower.takeIf { it > 0 },
        averageRunCadence = averageCadence.takeIf { it > 0 },
        averageStrideLength = averageStrideLength.takeIf { it > 0 },
        averageVerticalOscillation = averageVerticalOscillation.takeIf { it > 0 },
        averageContactTime = averageContactTime.takeIf { it > 0 },
        activeKilocalories = totalCalories.takeIf { it > 0 },
        steps = totalStepCount.takeIf { it > 0 }?.toInt(),
        totalElevationGain = elevationAscended.takeIf { it > 0 },
        vdot = vdot.takeIf { it > 0 },
        overallVdot = overallVdot.takeIf { it > 0 },
        trainingEffect = trainingEffect.takeIf { it > 0 },
        trainingLoad = trainingLoad.takeIf { it > 0 },
        weatherTemperature = weatherTemperature.takeIf { it != 0.0 },
        weatherHumidity = weatherHumidity.takeIf { it != 0.0 },
        outdoor = outdoor,
        feelingLevel = feelingLevel,
        inclusiveLevel = inclusiveLevel,
        note = note,
        address = address,
        deviceName = deviceInfo,
        deviceVersion = deviceVersion,
        dataSource = datasource,
        originDistance = originDistance.takeIf { it > 0 },
        shoeId = shoeId,
        trainPlanId = trainPlanId,
        originId = originId,
        startTime = sdf.format(Date(startTime)),
        endTime = sdf.format(Date(endTime)),
        activeDuration = (activeDuration * 60).toInt(),
        durationInSeconds = (duration * 60).toInt()
    )
}
