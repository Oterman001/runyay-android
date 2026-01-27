package com.oterman.fitdemo.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.garmin.fit.Decode
import com.garmin.fit.DeviceInfoMesg
import com.garmin.fit.DeviceInfoMesgListener
import com.garmin.fit.Field
import com.garmin.fit.FileIdMesg
import com.garmin.fit.FileIdMesgListener
import com.garmin.fit.LapMesg
import com.garmin.fit.LapMesgListener
import com.garmin.fit.Manufacturer
import com.garmin.fit.MesgBroadcaster
import com.garmin.fit.RecordMesg
import com.garmin.fit.RecordMesgListener
import com.garmin.fit.SessionMesg
import com.garmin.fit.SessionMesgListener
import com.garmin.fit.Sport
import com.garmin.fit.SubSport
import com.oterman.fitdemo.data.model.DeviceInfo
import com.oterman.fitdemo.data.model.FileInfo
import com.oterman.fitdemo.data.model.FitSummaryData
import com.oterman.fitdemo.data.model.LapData
import com.oterman.fitdemo.data.model.SessionSummary
import com.oterman.fitdemo.data.model.TrackInfo
import com.oterman.fitdemo.data.model.TrackPoint
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * FIT文件解析仓库
 */
class FitFileRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "FitFileRepository"
    }

    /**
     * 解析FIT文件
     */
    fun parseFitFile(uri: Uri): Result<FitSummaryData> {
        Log.d(TAG, "开始解析FIT文件: $uri")
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "无法打开文件输入流")
                return Result.failure(Exception("无法打开文件"))
            }
            
            Log.d(TAG, "文件输入流打开成功，开始解析...")
            val data = parseInputStream(inputStream)
            inputStream.close()
            
            Log.d(TAG, "文件解析成功")
            Result.success(data)
        } catch (e: Exception) {
            Log.e(TAG, "解析FIT文件失败", e)
            Result.failure(Exception("解析FIT文件失败: ${e.message}", e))
        }
    }

    private fun parseInputStream(inputStream: InputStream): FitSummaryData {
        Log.d(TAG, "创建Decode和MesgBroadcaster")
        val decode = Decode()
        val mesgBroadcaster = MesgBroadcaster(decode)
        
        // 数据收集器
        var fileInfo: FileInfo? = null
        var sessionSummary: SessionSummary? = null
        var deviceInfo: DeviceInfo? = null
        val laps = mutableListOf<LapData>()
        val trackPoints = mutableListOf<TrackPoint>()  // GPS轨迹点列表
        var recordCount = 0
        var hasGps = false
        var hasHeartRate = false
        var hasCadence = false
        
        Log.d(TAG, "注册消息监听器")
        
        // 监听文件ID消息
        mesgBroadcaster.addListener(FileIdMesgListener { mesg ->
            Log.d(TAG, "收到FileIdMesg")
            fileInfo = FileInfo(
                type = mesg.type?.name,
                manufacturer = getManufacturerName(mesg.manufacturer),
                product = mesg.product?.toString(),
                serialNumber = mesg.serialNumber?.toString(),
                timeCreated = formatDate(mesg.timeCreated),
                number = mesg.number
            )
        })
        
        // 监听会话消息
        mesgBroadcaster.addListener(SessionMesgListener { mesg ->
            sessionSummary = SessionSummary(
                sport = getSportName(mesg.sport),
                subSport = getSubSportName(mesg.subSport),
                startTime = formatDate(mesg.startTime),
                totalElapsedTime = formatTime(mesg.totalElapsedTime),
                totalTimerTime = formatTime(mesg.totalTimerTime),
                totalDistance = formatDistance(mesg.totalDistance),
                totalCalories = mesg.totalCalories?.toString(),
                avgSpeed = formatSpeed(mesg.avgSpeed),
                maxSpeed = formatSpeed(mesg.maxSpeed),
                avgPace = formatPace(mesg.avgSpeed),
                maxPace = formatPace(mesg.maxSpeed),
                avgHeartRate = mesg.avgHeartRate?.toString(),
                maxHeartRate = mesg.maxHeartRate?.toString(),
                avgCadence = mesg.avgCadence?.toString(),
                maxCadence = mesg.maxCadence?.toString(),
                totalAscent = mesg.totalAscent?.toString()?.let { "$it 米" },
                totalDescent = mesg.totalDescent?.toString()?.let { "$it 米" },
                avgStride = null // SessionMesg没有avgStride属性
            )
        })
        
        // 监听区间消息
        mesgBroadcaster.addListener(LapMesgListener { mesg ->
            val lapNumber = laps.size + 1
            laps.add(
                LapData(
                    lapNumber = lapNumber,
                    startTime = formatDate(mesg.startTime),
                    totalElapsedTime = formatTime(mesg.totalElapsedTime),
                    totalTimerTime = formatTime(mesg.totalTimerTime),
                    totalDistance = formatDistance(mesg.totalDistance),
                    totalCalories = mesg.totalCalories?.toString(),
                    avgSpeed = formatSpeed(mesg.avgSpeed),
                    maxSpeed = formatSpeed(mesg.maxSpeed),
                    avgPace = formatPace(mesg.avgSpeed),
                    avgHeartRate = mesg.avgHeartRate?.toString(),
                    maxHeartRate = mesg.maxHeartRate?.toString(),
                    avgCadence = mesg.avgCadence?.toString(),
                    totalAscent = mesg.totalAscent?.toString()?.let { "$it 米" },
                    totalDescent = mesg.totalDescent?.toString()?.let { "$it 米" }
                )
            )
        })
        
        // 监听记录消息（轨迹点）
        mesgBroadcaster.addListener(RecordMesgListener { mesg ->
            recordCount++
            
            // 收集GPS坐标
            if (mesg.positionLat != null && mesg.positionLong != null) {
                hasGps = true
                
                // 转换坐标并添加到轨迹点列表
                val latitude = semicirclesToDegrees(mesg.positionLat)
                val longitude = semicirclesToDegrees(mesg.positionLong)
                
                trackPoints.add(
                    TrackPoint(
                        latitude = latitude,
                        longitude = longitude,
                        timestamp = mesg.timestamp?.timestamp,
                        altitude = mesg.altitude?.toDouble(),
                        heartRate = mesg.heartRate.toInt(),
                        speed = mesg.speed
                    )
                )
            }
            
            if (mesg.heartRate != null) {
                hasHeartRate = true
            }
            if (mesg.cadence != null) {
                hasCadence = true
            }
        })
        
        // 监听设备信息消息
        mesgBroadcaster.addListener(DeviceInfoMesgListener { mesg ->
            if (deviceInfo == null) { // 只保留第一个设备信息
                deviceInfo = DeviceInfo(
                    manufacturer = getManufacturerName(mesg.manufacturer),
                    product = mesg.product?.toString(),
                    serialNumber = mesg.serialNumber?.toString(),
                    deviceType = mesg.deviceType?.toString(),
                    hardwareVersion = mesg.hardwareVersion?.toString(),
                    softwareVersion = mesg.softwareVersion?.toString()
                )
            }
        })
        
        // 读取FIT文件
        Log.d(TAG, "开始读取FIT文件...")
        if (!decode.read(inputStream, mesgBroadcaster)) {
            Log.e(TAG, "Decode.read返回false")
            throw Exception("FIT文件解析失败")
        }
        
        Log.d(TAG, "FIT文件读取完成, 记录数: $recordCount, 区间数: ${laps.size}, 轨迹点: ${trackPoints.size}")
        
        val trackInfo = TrackInfo(
            totalRecords = recordCount,
            hasGpsData = hasGps,
            hasHeartRateData = hasHeartRate,
            hasCadenceData = hasCadence
        )
        
        Log.d(TAG, "构建FitSummaryData对象")
        return FitSummaryData(
            fileInfo = fileInfo,
            sessionSummary = sessionSummary,
            trackInfo = trackInfo,
            laps = laps,
            deviceInfo = deviceInfo,
            trackPoints = trackPoints  // 包含GPS轨迹点
        )
    }
    
    // 辅助方法：格式化日期
    private fun formatDate(date: com.garmin.fit.DateTime?): String? {
        if (date == null) return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(Date(date.timestamp * 1000L))
        } catch (e: Exception) {
            null
        }
    }
    
    // 辅助方法：格式化时间（秒转为时:分:秒）
    private fun formatTime(seconds: Float?): String? {
        if (seconds == null) return null
        val totalSeconds = seconds.toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
        }
    }
    
    // 辅助方法：格式化距离（米转为公里）
    private fun formatDistance(meters: Float?): String? {
        if (meters == null) return null
        val km = meters / 1000.0
        return String.format("%.2f 公里", km)
    }
    
    // 辅助方法：格式化速度（m/s转为km/h）
    private fun formatSpeed(metersPerSecond: Float?): String? {
        if (metersPerSecond == null || metersPerSecond == 0f) return null
        val kmPerHour = metersPerSecond * 3.6
        return String.format("%.2f 公里/小时", kmPerHour)
    }
    
    // 辅助方法：格式化配速（m/s转为分:秒/公里）
    private fun formatPace(metersPerSecond: Float?): String? {
        if (metersPerSecond == null || metersPerSecond == 0f) return null
        val secondsPerKm = 1000.0 / metersPerSecond
        val minutes = (secondsPerKm / 60).toInt()
        val seconds = (secondsPerKm % 60).roundToInt()
        return String.format("%d'%02d\" /公里", minutes, seconds)
    }
    
    // 辅助方法：获取制造商名称
    private fun getManufacturerName(manufacturer: Int?): String? {
        if (manufacturer == null) return null
        return try {
            Manufacturer.getStringFromValue(manufacturer)
        } catch (e: Exception) {
            manufacturer.toString()
        }
    }
    
    // 辅助方法：获取运动类型名称
    private fun getSportName(sport: Sport?): String? {
        return sport?.name
    }
    
    // 辅助方法：获取运动子类型名称
    private fun getSubSportName(subSport: SubSport?): String? {
        return subSport?.name
    }
    
    // 辅助方法：将semicircles转换为度数
    // FIT SDK使用semicircles单位存储坐标
    // 转换公式：degrees = semicircles * (180 / 2^31)
    private fun semicirclesToDegrees(semicircles: Int): Double {
        return semicircles * (180.0 / Math.pow(2.0, 31.0))
    }
}

