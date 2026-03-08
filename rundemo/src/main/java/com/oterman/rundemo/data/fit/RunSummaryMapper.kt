package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.network.dto.RunSummaryBasicInfoDto
import com.oterman.rundemo.data.network.dto.request.RunRecordUploadItemDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * RunRecordEntity <-> RunSummaryBasicInfoDto 双向映射
 *
 * 单位转换规则：
 * | Entity字段 (客户端) | 单位 | DTO字段 (服务端) | 单位 | 转换 |
 * |---|---|---|---|---|
 * | duration | 分钟 | durationInSeconds | 秒 | ×60 / ÷60 |
 * | activeDuration | 分钟 | activeDuration | 秒 | ×60 / ÷60 |
 * | totalDistance | 公里 | distanceInMeters | 米 | ×1000 / ÷1000 |
 * | startTime | ms时间戳 | startTimeInSeconds | 秒时间戳 | ÷1000 / ×1000 |
 * | averageSpeed | min/km | averagePace | min/km | 直接映射 |
 * | averageHeartRate | bpm | averageHeartRate | bpm | 直接映射 |
 * | totalCalories | kcal | activeKilocalories | kcal | 直接映射 |
 * | elevationAscended | 米 | totalElevationGain | 米 | 直接映射 |
 * | outdoor | 0=户外,1=室内 | outdoor | boolean | 0↔true |
 */
object RunSummaryMapper {

    /**
     * Entity -> RunRecordUploadItemDto (用于 /api/rundata/upload)
     * 字段和类型完全对齐服务端 API (run_recordupload.txt)
     */
    fun toUploadItemDto(entity: RunRecordEntity): RunRecordUploadItemDto {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return RunRecordUploadItemDto(
            hkUUid = entity.workoutId,
            deviceVersion = entity.deviceVersion,
            deviceInfo = entity.deviceInfo,
            deviceName = entity.deviceInfo,
            timezone = TimeZone.getDefault().id,
            startTime = sdf.format(Date(entity.startTime)),
            endTime = sdf.format(Date(entity.endTime)),
            duration = entity.duration * 60.0,
            activeDuration = entity.activeDuration * 60.0,
            totalDistance = entity.totalDistance,
            averagePace = entity.averageSpeed.takeIf { it > 0 },
            averageHeartRate = entity.averageHeartRate.takeIf { it > 0 },
            maxHeartRate = entity.maxHeartRate.takeIf { it > 0 },
            minHeartRate = entity.minHeartRate.takeIf { it > 0 },
            totalStepCount = entity.totalStepCount.takeIf { it > 0 },
            totalActiveEnergy = entity.totalCalories.takeIf { it > 0 },
            originDistance = entity.originDistance.takeIf { it > 0 },
            maxPace = entity.maxSpeed.takeIf { it > 0 },
            address = entity.address,
            elevationAscended = entity.elevationAscended.takeIf { it > 0 },
            averageStrideLength = entity.averageStrideLength.takeIf { it > 0 },
            averageContactTime = entity.averageContactTime.takeIf { it > 0 },
            averageVerticalOscillation = entity.averageVerticalOscillation.takeIf { it > 0 },
            averagePower = entity.averagePower.takeIf { it > 0 },
            maxPower = entity.maxPower.takeIf { it > 0 },
            averageCadence = entity.averageCadence.takeIf { it > 0 },
            trainingLoad = entity.trainingLoad.takeIf { it > 0 },
            trainingEffect = entity.trainingEffect.takeIf { it > 0 },
            anaerobicTrainingEffect = entity.anaerobicTrainingEffect.takeIf { it > 0 },
            vdot = entity.vdot.takeIf { it > 0 },
            overallVdot = entity.overallVdot.takeIf { it > 0 },
            weatherHumidity = entity.weatherHumidity.takeIf { it != 0.0 },
            weatherTemperature = entity.weatherTemperature.takeIf { it != 0.0 },
            outdoor = entity.outdoor,
            feelingLevel = entity.feelingLevel.takeIf { it > 0 },
            inclusiveLevel = entity.inclusiveLevel,
            note = entity.note,
            eventStr = entity.eventStr,
            shoeId = entity.shoeId,
            trainPlanId = entity.trainPlanId,
            datasource = entity.datasource,
            originId = entity.originId,
            raceId = entity.linkedRaceRecordId
        )
    }

    /**
     * Entity -> DTO (用于上传)
     */
    fun toUploadDto(entity: RunRecordEntity): RunSummaryBasicInfoDto {
        return RunSummaryBasicInfoDto(
            summaryId = entity.originId,
            platformCode = entity.datasource,
            startTimeInSeconds = entity.startTime / 1000,
            durationInSeconds = entity.duration * 60.0,
            activeDuration = entity.activeDuration * 60.0,
            distanceInMeters = entity.totalDistance * 1000.0,
            averagePace = entity.averageSpeed.takeIf { it > 0 },
            maxPace = entity.maxSpeed.takeIf { it > 0 },
            averageHeartRate = entity.averageHeartRate.takeIf { it > 0 },
            maxHeartRate = entity.maxHeartRate.takeIf { it > 0 },
            minHeartRate = entity.minHeartRate.takeIf { it > 0 },
            averagePower = entity.averagePower.takeIf { it > 0 },
            maxPower = entity.maxPower.takeIf { it > 0 },
            averageCadence = entity.averageCadence.takeIf { it > 0 },
            averageStrideLength = entity.averageStrideLength.takeIf { it > 0 },
            averageVerticalOscillation = entity.averageVerticalOscillation.takeIf { it > 0 },
            averageContactTime = entity.averageContactTime.takeIf { it > 0 },
            activeKilocalories = entity.totalCalories.takeIf { it > 0 },
            totalStepCount = entity.totalStepCount.takeIf { it > 0 },
            totalElevationGain = entity.elevationAscended.takeIf { it > 0 },
            vdot = entity.vdot.takeIf { it > 0 },
            overallVdot = entity.overallVdot.takeIf { it > 0 },
            trainingEffect = entity.trainingEffect.takeIf { it > 0 },
            anaerobicTrainingEffect = entity.anaerobicTrainingEffect.takeIf { it > 0 },
            trainingLoad = entity.trainingLoad.takeIf { it > 0 },
            weatherTemperature = entity.weatherTemperature.takeIf { it != 0.0 },
            weatherHumidity = entity.weatherHumidity.takeIf { it != 0.0 },
            outdoor = entity.outdoor, // 0=户外→true
            deviceInfo = entity.deviceInfo,
            deviceVersion = entity.deviceVersion,
            datasource = entity.datasource,
            originId = entity.originId,
            inclusiveLevel = entity.inclusiveLevel,
            trajectoryStatus = entity.trajectoryStatus,
            note = entity.note,
            feelingLevel = entity.feelingLevel,
            address = entity.address,
            trainPlanId = entity.trainPlanId,
            shoeId = entity.shoeId,
            linkedRaceRecordId = entity.linkedRaceRecordId,
            userId = entity.userId
        )
    }

    /**
     * 从服务端DTO提取值，应用到Entity上（用于合并场景中提取服务端值）
     * 返回的Entity使用服务端值覆盖本地值（仅覆盖非null字段）
     */
    fun applyServerValues(local: RunRecordEntity, server: RunSummaryBasicInfoDto): RunRecordEntity {
        return local.copy(
            startTime = server.startTimeInSeconds?.let { it * 1000 } ?: local.startTime,
            endTime = server.durationInSeconds?.let { durationSec ->
                val startMs = server.startTimeInSeconds?.let { it * 1000 } ?: local.startTime
                startMs + (durationSec * 1000).toLong()
            } ?: local.endTime,
            duration = server.durationInSeconds?.let { it / 60.0 } ?: local.duration,
            activeDuration = server.activeDuration?.let { it / 60.0 } ?: local.activeDuration,
            totalDistance = server.distanceInMeters?.let { it / 1000.0 } ?: local.totalDistance,
            averageSpeed = server.averagePace ?: local.averageSpeed,
            maxSpeed = server.maxPace ?: local.maxSpeed,
            averageHeartRate = server.averageHeartRate ?: local.averageHeartRate,
            maxHeartRate = server.maxHeartRate ?: local.maxHeartRate,
            minHeartRate = server.minHeartRate ?: local.minHeartRate,
            averagePower = server.averagePower ?: local.averagePower,
            maxPower = server.maxPower ?: local.maxPower,
            averageCadence = server.averageCadence ?: local.averageCadence,
            averageStrideLength = server.averageStrideLength ?: local.averageStrideLength,
            averageVerticalOscillation = server.averageVerticalOscillation ?: local.averageVerticalOscillation,
            averageContactTime = server.averageContactTime ?: local.averageContactTime,
            totalCalories = server.activeKilocalories ?: local.totalCalories,
            totalStepCount = server.totalStepCount ?: local.totalStepCount,
            elevationAscended = server.totalElevationGain ?: local.elevationAscended,
            vdot = server.vdot ?: local.vdot,
            overallVdot = server.overallVdot ?: local.overallVdot,
            trainingEffect = server.trainingEffect ?: local.trainingEffect,
            anaerobicTrainingEffect = server.anaerobicTrainingEffect ?: local.anaerobicTrainingEffect,
            trainingLoad = server.trainingLoad ?: local.trainingLoad,
            weatherTemperature = server.weatherTemperature ?: local.weatherTemperature,
            weatherHumidity = server.weatherHumidity ?: local.weatherHumidity,
            outdoor = server.outdoor ?: local.outdoor,
            deviceInfo = server.deviceInfo ?: local.deviceInfo,
            deviceVersion = server.deviceVersion ?: local.deviceVersion,
            note = server.note ?: local.note,
            feelingLevel = server.feelingLevel ?: local.feelingLevel,
            address = server.address ?: local.address,
            trainPlanId = server.trainPlanId ?: local.trainPlanId,
            shoeId = server.shoeId ?: local.shoeId,
            linkedRaceRecordId = server.linkedRaceRecordId ?: local.linkedRaceRecordId
        )
    }
}
