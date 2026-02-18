package com.oterman.rundemo.data.fit

import android.content.Context
import android.net.Uri
import com.garmin.fit.Decode
import com.garmin.fit.DeviceInfoMesgListener
import com.garmin.fit.EventMesgListener
import com.garmin.fit.FileIdMesgListener
import com.garmin.fit.LapMesgListener
import com.garmin.fit.Manufacturer
import com.garmin.fit.MesgBroadcaster
import com.garmin.fit.RecordMesgListener
import com.garmin.fit.SessionMesgListener
import com.oterman.rundemo.util.RLog
import java.io.InputStream

/**
 * FIT文件解析结果
 */
data class FitParseResult(
    val fileId: FitFileId?,
    val session: FitSession?,
    val laps: List<FitLap>,
    val records: List<FitRecord>,
    val events: List<FitEvent>,
    val deviceInfo: FitDeviceInfo?
)

/**
 * 文件ID信息
 */
data class FitFileId(
    val type: String?,
    val manufacturer: String?,
    val product: Int?,
    val serialNumber: Long?,
    val timeCreated: Long?
)

/**
 * 会话摘要
 */
data class FitSession(
    val sport: String?,
    val subSport: String?,
    val startTime: Long,
    val totalElapsedTime: Float,
    val totalTimerTime: Float,
    val totalDistance: Float,
    val totalCalories: Int?,
    val avgSpeed: Float?,
    val maxSpeed: Float?,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    val minHeartRate: Int?,
    val avgCadence: Int?,
    val maxCadence: Int?,
    val avgPower: Int?,
    val maxPower: Int?,
    val totalAscent: Int?,
    val totalDescent: Int?,
    val avgStepLength: Float?,
    val avgVerticalOscillation: Float?,
    val avgStanceTime: Float?,
    val totalStrides: Long?,
    val avgTemperature: Int?,
    val trainingEffect: Float?,
    val anaerobicTrainingEffect: Float?
)

/**
 * 分段数据
 */
data class FitLap(
    val startTime: Long,
    val totalElapsedTime: Float,
    val totalTimerTime: Float,
    val totalDistance: Float,
    val avgSpeed: Float?,
    val maxSpeed: Float?,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    val avgCadence: Int?,
    val avgPower: Int?,
    val avgStepLength: Float?,
    val avgVerticalOscillation: Float?,
    val avgStanceTime: Float?,
    val totalStrides: Long?,
    val intensity: Int?,
    val wktStepIndex: Int?
)

/**
 * 采样点记录
 */
data class FitRecord(
    val timestamp: Long,
    val positionLat: Int?,
    val positionLong: Int?,
    val altitude: Float?,
    val heartRate: Int?,
    val cadence: Int?,
    val speed: Float?,
    val power: Int?,
    val distance: Float?,
    val stepLength: Float?,
    val verticalOscillation: Float?,
    val stanceTime: Float?
)

/**
 * 事件数据（暂停/恢复等）
 */
data class FitEvent(
    val timestamp: Long,
    val event: String?,
    val eventType: String?
)

/**
 * 设备信息
 */
data class FitDeviceInfo(
    val manufacturer: String?,
    val product: Int?,
    val serialNumber: Long?,
    val softwareVersion: Float?,
    val deviceType: Int?
)

/**
 * FIT文件解析器
 * 使用Garmin FIT SDK解析FIT文件
 */
class FitFileParser(private val context: Context) {
    
    companion object {
        private const val TAG = "FitFileParser"
        
        /**
         * Semicircles坐标转换为度数
         */
        fun semicirclesToDegrees(semicircles: Int): Double {
            return semicircles * (180.0 / Math.pow(2.0, 31.0))
        }
        
        /**
         * 速度(m/s)转配速(min/km)
         */
        fun speedToPace(metersPerSecond: Float?): Double {
            if (metersPerSecond == null || metersPerSecond <= 0) return 0.0
            return (1000.0 / metersPerSecond) / 60.0
        }
        
        /**
         * FIT时间戳转毫秒
         * FIT时间戳是从1989-12-31 00:00:00 UTC开始的秒数
         */
        fun fitTimestampToMillis(fitTimestamp: Long): Long {
            // FIT epoch: 1989-12-31 00:00:00 UTC = 631065600000 ms from Unix epoch
            val fitEpochOffset = 631065600000L
            return fitTimestamp * 1000L + fitEpochOffset
        }
        
        /**
         * 从字节数组解析FIT文件
         * @param data FIT文件的字节数组
         * @return 解析结果，失败返回null
         */
        fun parseFromBytes(data: ByteArray): FitParseResult? {
            RLog.d(TAG, "开始从字节数组解析FIT文件, size=${data.size}")
            return try {
                val inputStream = java.io.ByteArrayInputStream(data)
                val result = parseInputStreamStatic(inputStream)
                inputStream.close()
                RLog.d(TAG, "FIT文件解析成功: records=${result.records.size}, laps=${result.laps.size}")
                result
            } catch (e: Exception) {
                RLog.e(TAG, "解析FIT文件失败", e)
                null
            }
        }
        
        /**
         * 静态方法解析输入流
         */
        private fun parseInputStreamStatic(inputStream: InputStream): FitParseResult {
            val decode = Decode()
            val mesgBroadcaster = MesgBroadcaster(decode)
            
            // 数据收集器
            var fileId: FitFileId? = null
            var session: FitSession? = null
            var deviceInfo: FitDeviceInfo? = null
            val laps = mutableListOf<FitLap>()
            val records = mutableListOf<FitRecord>()
            val events = mutableListOf<FitEvent>()
            
            // 监听文件ID消息
            mesgBroadcaster.addListener(FileIdMesgListener { mesg ->
                fileId = FitFileId(
                    type = mesg.type?.name,
                    manufacturer = getManufacturerNameStatic(mesg.manufacturer),
                    product = mesg.product,
                    serialNumber = mesg.serialNumber,
                    timeCreated = mesg.timeCreated?.timestamp
                )
            })
            
            // 监听会话消息
            mesgBroadcaster.addListener(SessionMesgListener { mesg ->
                session = FitSession(
                    sport = mesg.sport?.name,
                    subSport = mesg.subSport?.name,
                    startTime = mesg.startTime?.timestamp ?: 0L,
                    totalElapsedTime = mesg.totalElapsedTime ?: 0f,
                    totalTimerTime = mesg.totalTimerTime ?: 0f,
                    totalDistance = mesg.totalDistance ?: 0f,
                    totalCalories = mesg.totalCalories,
                    avgSpeed = mesg.avgSpeed,
                    maxSpeed = mesg.maxSpeed,
                    avgHeartRate = mesg.avgHeartRate?.toInt(),
                    maxHeartRate = mesg.maxHeartRate?.toInt(),
                    minHeartRate = mesg.minHeartRate?.toInt(),
                    avgCadence = mesg.avgCadence?.toInt(),
                    maxCadence = mesg.maxCadence?.toInt(),
                    avgPower = mesg.avgPower,
                    maxPower = mesg.maxPower,
                    totalAscent = mesg.totalAscent,
                    totalDescent = mesg.totalDescent,
                    avgStepLength = mesg.avgStepLength,
                    avgVerticalOscillation = mesg.avgVerticalOscillation,
                    avgStanceTime = mesg.avgStanceTime,
                    totalStrides = try { mesg.totalStrides } catch (e: Exception) { null },
                    avgTemperature = mesg.avgTemperature?.toInt(),
                    trainingEffect = mesg.totalTrainingEffect,
                    anaerobicTrainingEffect = mesg.totalAnaerobicTrainingEffect
                )
            })
            
            // 监听分段消息
            mesgBroadcaster.addListener(LapMesgListener { mesg ->
                laps.add(FitLap(
                    startTime = mesg.startTime?.timestamp ?: 0L,
                    totalElapsedTime = mesg.totalElapsedTime ?: 0f,
                    totalTimerTime = mesg.totalTimerTime ?: 0f,
                    totalDistance = mesg.totalDistance ?: 0f,
                    avgSpeed = mesg.avgSpeed,
                    maxSpeed = mesg.maxSpeed,
                    avgHeartRate = mesg.avgHeartRate?.toInt(),
                    maxHeartRate = mesg.maxHeartRate?.toInt(),
                    avgCadence = mesg.avgCadence?.toInt(),
                    avgPower = mesg.avgPower,
                    avgStepLength = mesg.avgStepLength,
                    avgVerticalOscillation = mesg.avgVerticalOscillation,
                    avgStanceTime = mesg.avgStanceTime,
                    totalStrides = try { mesg.totalStrides } catch (e: Exception) { null },
                    intensity = mesg.intensity?.value?.toInt(),
                    wktStepIndex = mesg.wktStepIndex
                ))
            })
            
            // 监听记录消息（采样点）
            mesgBroadcaster.addListener(RecordMesgListener { mesg ->
                records.add(FitRecord(
                    timestamp = mesg.timestamp?.timestamp ?: 0L,
                    positionLat = mesg.positionLat,
                    positionLong = mesg.positionLong,
                    altitude = mesg.enhancedAltitude ?: mesg.altitude,
                    heartRate = mesg.heartRate?.toInt(),
                    cadence = mesg.cadence?.toInt(),
                    speed = mesg.speed,
                    power = mesg.power,
                    distance = mesg.distance,
                    stepLength = mesg.stepLength,
                    verticalOscillation = mesg.verticalOscillation,
                    stanceTime = mesg.stanceTime
                ))
            })
            
            // 监听事件消息
            mesgBroadcaster.addListener(EventMesgListener { mesg ->
                events.add(FitEvent(
                    timestamp = mesg.timestamp?.timestamp ?: 0L,
                    event = mesg.event?.name,
                    eventType = mesg.eventType?.name
                ))
            })
            
            // 监听设备信息消息
            mesgBroadcaster.addListener(DeviceInfoMesgListener { mesg ->
                if (deviceInfo == null) {
                    deviceInfo = FitDeviceInfo(
                        manufacturer = getManufacturerNameStatic(mesg.manufacturer),
                        product = mesg.product,
                        serialNumber = mesg.serialNumber,
                        softwareVersion = mesg.softwareVersion,
                        deviceType = null
                    )
                }
            })
            
            // 读取FIT文件
            if (!decode.read(inputStream, mesgBroadcaster)) {
                throw Exception("FIT文件解析失败")
            }
            
            return FitParseResult(
                fileId = fileId,
                session = session,
                laps = laps,
                records = records,
                events = events,
                deviceInfo = deviceInfo
            )
        }
        
        private fun getManufacturerNameStatic(manufacturer: Int?): String? {
            if (manufacturer == null) return null
            return try {
                Manufacturer.getStringFromValue(manufacturer)
            } catch (e: Exception) {
                manufacturer.toString()
            }
        }
    }
    
    /**
     * 解析FIT文件
     * @param uri 文件Uri
     * @return 解析结果
     */
    fun parse(uri: Uri): Result<FitParseResult> {
        RLog.d(TAG, "开始解析FIT文件: $uri")
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                RLog.e(TAG, "无法打开文件输入流")
                return Result.failure(Exception("无法打开文件"))
            }
            
            val result = parseInputStream(inputStream)
            inputStream.close()
            
            RLog.d(TAG, "FIT文件解析成功: records=${result.records.size}, laps=${result.laps.size}")
            Result.success(result)
        } catch (e: Exception) {
            RLog.e(TAG, "解析FIT文件失败", e)
            Result.failure(Exception("解析FIT文件失败: ${e.message}", e))
        }
    }
    
    private fun parseInputStream(inputStream: InputStream): FitParseResult {
        val decode = Decode()
        val mesgBroadcaster = MesgBroadcaster(decode)
        
        // 数据收集器
        var fileId: FitFileId? = null
        var session: FitSession? = null
        var deviceInfo: FitDeviceInfo? = null
        val laps = mutableListOf<FitLap>()
        val records = mutableListOf<FitRecord>()
        val events = mutableListOf<FitEvent>()
        
        // 监听文件ID消息
        mesgBroadcaster.addListener(FileIdMesgListener { mesg ->
            fileId = FitFileId(
                type = mesg.type?.name,
                manufacturer = getManufacturerName(mesg.manufacturer),
                product = mesg.product,
                serialNumber = mesg.serialNumber,
                timeCreated = mesg.timeCreated?.timestamp
            )
        })
        
        // 监听会话消息
        mesgBroadcaster.addListener(SessionMesgListener { mesg ->
            session = FitSession(
                sport = mesg.sport?.name,
                subSport = mesg.subSport?.name,
                startTime = mesg.startTime?.timestamp ?: 0L,
                totalElapsedTime = mesg.totalElapsedTime ?: 0f,
                totalTimerTime = mesg.totalTimerTime ?: 0f,
                totalDistance = mesg.totalDistance ?: 0f,
                totalCalories = mesg.totalCalories,
                avgSpeed = mesg.avgSpeed,
                maxSpeed = mesg.maxSpeed,
                avgHeartRate = mesg.avgHeartRate?.toInt(),
                maxHeartRate = mesg.maxHeartRate?.toInt(),
                minHeartRate = mesg.minHeartRate?.toInt(),
                avgCadence = mesg.avgCadence?.toInt(),
                maxCadence = mesg.maxCadence?.toInt(),
                avgPower = mesg.avgPower,
                maxPower = mesg.maxPower,
                totalAscent = mesg.totalAscent,
                totalDescent = mesg.totalDescent,
                avgStepLength = mesg.avgStepLength,
                avgVerticalOscillation = mesg.avgVerticalOscillation,
                avgStanceTime = mesg.avgStanceTime,
                totalStrides = try { mesg.totalStrides } catch (e: Exception) { null },
                avgTemperature = mesg.avgTemperature?.toInt(),
                trainingEffect = mesg.totalTrainingEffect,
                anaerobicTrainingEffect = mesg.totalAnaerobicTrainingEffect
            )
        })
        
        // 监听分段消息
        mesgBroadcaster.addListener(LapMesgListener { mesg ->
            laps.add(FitLap(
                startTime = mesg.startTime?.timestamp ?: 0L,
                totalElapsedTime = mesg.totalElapsedTime ?: 0f,
                totalTimerTime = mesg.totalTimerTime ?: 0f,
                totalDistance = mesg.totalDistance ?: 0f,
                avgSpeed = mesg.avgSpeed,
                maxSpeed = mesg.maxSpeed,
                avgHeartRate = mesg.avgHeartRate?.toInt(),
                maxHeartRate = mesg.maxHeartRate?.toInt(),
                avgCadence = mesg.avgCadence?.toInt(),
                avgPower = mesg.avgPower,
                avgStepLength = mesg.avgStepLength,
                avgVerticalOscillation = mesg.avgVerticalOscillation,
                avgStanceTime = mesg.avgStanceTime,
                totalStrides = try { mesg.totalStrides } catch (e: Exception) { null },
                intensity = mesg.intensity?.value?.toInt(),
                wktStepIndex = mesg.wktStepIndex
            ))
        })
        
        // 监听记录消息（采样点）
        mesgBroadcaster.addListener(RecordMesgListener { mesg ->
            records.add(FitRecord(
                timestamp = mesg.timestamp?.timestamp ?: 0L,
                positionLat = mesg.positionLat,
                positionLong = mesg.positionLong,
                altitude = mesg.enhancedAltitude ?: mesg.altitude,
                heartRate = mesg.heartRate?.toInt(),
                cadence = mesg.cadence?.toInt(),
                speed = mesg.speed,
                power = mesg.power,
                distance = mesg.distance,
                stepLength = mesg.stepLength,
                verticalOscillation = mesg.verticalOscillation,
                stanceTime = mesg.stanceTime
            ))
        })
        
        // 监听事件消息
        mesgBroadcaster.addListener(EventMesgListener { mesg ->
            events.add(FitEvent(
                timestamp = mesg.timestamp?.timestamp ?: 0L,
                event = mesg.event?.name,
                eventType = mesg.eventType?.name
            ))
        })
        
        // 监听设备信息消息
        mesgBroadcaster.addListener(DeviceInfoMesgListener { mesg ->
            if (deviceInfo == null) {
                deviceInfo = FitDeviceInfo(
                    manufacturer = getManufacturerName(mesg.manufacturer),
                    product = mesg.product,
                    serialNumber = mesg.serialNumber,
                    softwareVersion = mesg.softwareVersion,
                    deviceType = null  // DeviceType枚举直接使用ordinal可能不准确，暂时设为null
                )
            }
        })
        
        // 读取FIT文件
        if (!decode.read(inputStream, mesgBroadcaster)) {
            throw Exception("FIT文件解析失败")
        }
        
        return FitParseResult(
            fileId = fileId,
            session = session,
            laps = laps,
            records = records,
            events = events,
            deviceInfo = deviceInfo
        )
    }
    
    private fun getManufacturerName(manufacturer: Int?): String? {
        if (manufacturer == null) return null
        return try {
            Manufacturer.getStringFromValue(manufacturer)
        } catch (e: Exception) {
            manufacturer.toString()
        }
    }
}

