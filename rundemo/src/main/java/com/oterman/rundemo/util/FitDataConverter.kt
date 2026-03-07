package com.oterman.rundemo.util

import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.local.entity.RunSamplePointEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import java.security.MessageDigest
import java.util.UUID

/**
 * FIT数据转换器
 * 用于将FIT解析数据转换为Room Entity
 */
object FitDataConverter {
    
    private const val TAG = "FitDataConverter"
    
    /**
     * 基于文件名和开始时间生成确定性UUID
     * 确保同一个FIT文件多次导入生成相同的UUID
     */
    fun generateWorkoutId(summaryId: String, startTime: Long): String {
        val input = "$summaryId-$startTime"
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(input.toByteArray())
        
        // 将前16字节转换为UUID格式
        val uuid = UUID.nameUUIDFromBytes(hashBytes.copyOf(16))
        return uuid.toString()
    }
    
    /**
     * Semicircles坐标转换为度数
     * FIT SDK使用semicircles单位存储坐标
     * 转换公式：degrees = semicircles * (180 / 2^31)
     */
    fun semicirclesToDegrees(semicircles: Int): Double {
        return semicircles * (180.0 / Math.pow(2.0, 31.0))
    }
    
    /**
     * 速度转配速
     * @param metersPerSecond 速度(m/s)
     * @return 配速(min/km)
     */
    fun speedToPace(metersPerSecond: Double): Double {
        if (metersPerSecond <= 0) return 0.0
        return (1000.0 / metersPerSecond) / 60.0
    }
    
    /**
     * 配速转速度
     * @param paceMinPerKm 配速(min/km)
     * @return 速度(m/s)
     */
    fun paceToSpeed(paceMinPerKm: Double): Double {
        if (paceMinPerKm <= 0) return 0.0
        return 1000.0 / (paceMinPerKm * 60.0)
    }
    
    /**
     * 格式化配速显示
     * @param paceMinPerKm 配速(min/km)
     * @return 格式化字符串，如 "5'30""
     */
    fun formatPace(paceMinPerKm: Double): String {
        if (paceMinPerKm <= 0 || paceMinPerKm > 60) return "-"
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return "${minutes}'${seconds.toString().padStart(2, '0')}\""
    }
    
    /**
     * 格式化时长显示
     * @param durationMinutes 时长(分钟)
     * @return 格式化字符串，如 "1:05:30"
     */
    fun formatDuration(durationMinutes: Double): String {
        val totalSeconds = (durationMinutes * 60).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    /**
     * 格式化距离显示
     * @param distanceKm 距离(公里)
     * @return 格式化字符串，如 "10.5"
     */
    fun formatDistance(distanceKm: Double): String {
        return String.format("%.2f", distanceKm)
    }
    
    // ==================== Entity构建辅助方法 ====================
    
    /**
     * 创建空的采样点Entity（用于后续填充数据）
     */
    fun createSamplePoint(
        workoutId: String,
        sequence: Int,
        timestamp: Long,
        startTime: Long
    ): RunSamplePointEntity {
        return RunSamplePointEntity(
            workoutId = workoutId,
            sequence = sequence,
            timestamp = timestamp,
            timeOffset = ((timestamp - startTime) / 1000).toInt()
        )
    }
    
    /**
     * 创建分段Entity
     */
    fun createSegment(
        workoutId: String,
        seq: Int,
        segmentType: Int,
        beginTime: Long,
        endTime: Long,
        distance: Double,
        activeDuration: Double
    ): RunSegmentEntity {
        return RunSegmentEntity(
            workoutId = workoutId,
            seq = seq,
            segmentType = segmentType,
            beginTime = beginTime,
            endTime = endTime,
            duration = ((endTime - beginTime) / 1000.0 / 60.0),
            activeDuration = activeDuration,
            distance = distance
        )
    }
    
    // ==================== 数据源常量 ====================
    
    object Datasource {
        const val GARMIN_CHINA = "GCN"      // 佳明中国区
        const val GARMIN_GLOBAL = "GGB"     // 佳明国际区
        const val COROS = "COROS"           // 高驰
        const val SUUNTO = "SUUNTO"         // 颂拓
        const val POLAR = "POLAR"           // 博能
        const val APPLE_WATCH = "APPLE"     // Apple Watch
        const val LOCAL_FIT = "LOCAL"       // 本地FIT文件（已废弃，改用 MANUAL）
        const val MANUAL = "MANUAL"         // 用户手动导入
    }
    
    // ==================== 轨迹状态常量 ====================
    
    object TrajectoryStatus {
        const val UNKNOWN = 0       // 未知
        const val EXISTS = 1        // 存在轨迹
        const val NOT_EXISTS = 2    // 不存在轨迹
    }
    
    // ==================== 分段类型常量 ====================
    
    object SegmentType {
        const val KILOMETER = 1     // 公里分段
        const val TRAINING = 2      // 训练分段
    }
    
    // ==================== 区间类型常量 ====================
    
    object ZoneType {
        const val HEART_RATE_7 = 1  // 心率7区间
        const val HEART_RATE_5 = 2  // 心率5区间
        const val SPEED = 3         // 配速区间
    }
}

