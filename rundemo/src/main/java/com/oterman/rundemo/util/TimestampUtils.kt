package com.oterman.rundemo.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 时间戳工具类
 * 统一管理同步时间戳的格式转换
 *
 * 时间戳格式规范：
 * - 本地存储/同步时间戳：17位 yyyyMMddHHmmssSSS
 * - API回填请求参数：14位 yyyyMMddHHmmss
 * - 服务器返回dataDate：14位 yyyyMMddHHmmss
 */
object TimestampUtils {

    /** 标准时间戳长度（17位） */
    const val STANDARD_LENGTH = 17

    /** API时间戳长度（14位） */
    const val API_LENGTH = 14

    /** 默认同步开始时间（17位格式） */
    const val DEFAULT_SYNC_START_TIME = "20251001000000000"

    /** 17位时间戳格式 */
    private const val FORMAT_17 = "yyyyMMddHHmmssSSS"

    /** 14位时间戳格式 */
    private const val FORMAT_14 = "yyyyMMddHHmmss"

    /** 8位日期格式 */
    private const val FORMAT_8 = "yyyyMMdd"

    private val dateFormat17 = SimpleDateFormat(FORMAT_17, Locale.getDefault())
    private val dateFormat14 = SimpleDateFormat(FORMAT_14, Locale.getDefault())
    private val dateFormat8 = SimpleDateFormat(FORMAT_8, Locale.getDefault())

    /**
     * 规范化时间戳为17位格式
     * @param timestamp 输入时间戳（任意长度）
     * @return 17位标准格式时间戳
     */
    fun normalizeTimestamp(timestamp: String): String {
        val clean = timestamp.trim()
        return when {
            clean.length == STANDARD_LENGTH -> clean
            clean.length < STANDARD_LENGTH -> clean.padEnd(STANDARD_LENGTH, '0')
            else -> clean.take(STANDARD_LENGTH)
        }
    }

    /**
     * 迁移旧格式时间戳到17位格式
     * 支持8位(yyyyMMdd)和14位(yyyyMMddHHmmss)格式
     * @param oldTimestamp 旧格式时间戳
     * @return 17位标准格式时间戳
     */
    fun migrateOldTimestamp(oldTimestamp: String): String {
        val clean = oldTimestamp.trim()
        return when (clean.length) {
            8 -> clean + "000000000"   // yyyyMMdd → yyyyMMdd000000000
            14 -> clean + "000"        // yyyyMMddHHmmss → yyyyMMddHHmmss000
            else -> normalizeTimestamp(clean)
        }
    }

    /**
     * 将17位时间戳转换为API请求所需的14位格式
     * @param timestamp17 17位时间戳
     * @return 14位API格式时间戳
     */
    fun toApiFormat(timestamp17: String): String {
        val normalized = normalizeTimestamp(timestamp17)
        return normalized.take(API_LENGTH)
    }

    /**
     * 将时间戳转换为8位日期格式（yyyyMMdd），用于高驰回填请求
     * @param timestamp 任意长度时间戳
     * @return 8位日期格式
     */
    fun toDateFormat(timestamp: String): String {
        val normalized = normalizeTimestamp(timestamp)
        return normalized.take(8)
    }

    /**
     * 将14位服务器返回的dataDate规范化为17位存储格式
     * @param dataDate 服务器返回的14位dataDate
     * @return 17位标准格式时间戳
     */
    fun fromServerDataDate(dataDate: String): String {
        return migrateOldTimestamp(dataDate)
    }

    /**
     * 获取当前时间的17位时间戳
     * @return 当前时间的17位时间戳
     */
    @Synchronized
    fun getCurrentTimestamp(): String {
        return dateFormat17.format(Date())
    }

    /**
     * 获取指定天数前的17位时间戳
     * @param days 天数
     * @return 指定天数前的17位时间戳
     */
    @Synchronized
    fun getTimestampDaysAgo(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return dateFormat17.format(calendar.time)
    }

    /**
     * 获取指定天数前的14位时间戳（用于API请求）
     * @param days 天数
     * @return 指定天数前的14位时间戳
     */
    @Synchronized
    fun getApiTimestampDaysAgo(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return dateFormat14.format(calendar.time)
    }

    /**
     * 获取当前时间的14位时间戳（用于API请求）
     * @return 当前时间的14位时间戳
     */
    @Synchronized
    fun getCurrentApiTimestamp(): String {
        return dateFormat14.format(Date())
    }

    /**
     * 比较两个17位时间戳
     * @return 正数表示timestamp1更晚，负数表示timestamp1更早，0表示相等
     */
    fun compareTimestamps(timestamp1: String, timestamp2: String): Int {
        val t1 = normalizeTimestamp(timestamp1)
        val t2 = normalizeTimestamp(timestamp2)
        return t1.compareTo(t2)
    }

    /**
     * 获取较大（较晚）的时间戳
     */
    fun maxTimestamp(timestamp1: String, timestamp2: String): String {
        return if (compareTimestamps(timestamp1, timestamp2) >= 0) {
            normalizeTimestamp(timestamp1)
        } else {
            normalizeTimestamp(timestamp2)
        }
    }

    /**
     * 获取较小（较早）的时间戳
     */
    fun minTimestamp(timestamp1: String, timestamp2: String): String {
        return if (compareTimestamps(timestamp1, timestamp2) <= 0) {
            normalizeTimestamp(timestamp1)
        } else {
            normalizeTimestamp(timestamp2)
        }
    }

    /**
     * 验证时间戳格式是否有效
     * @param timestamp 待验证的时间戳
     * @return true如果是有效的数字字符串
     */
    fun isValidTimestamp(timestamp: String): Boolean {
        val clean = timestamp.trim()
        return clean.isNotEmpty() && clean.all { it.isDigit() } && clean.length in 8..STANDARD_LENGTH
    }

    /**
     * 将17位时间戳解析为Date对象
     * @param timestamp17 17位时间戳
     * @return Date对象，解析失败返回null
     */
    @Synchronized
    fun parseToDate(timestamp17: String): Date? {
        return try {
            val normalized = normalizeTimestamp(timestamp17)
            dateFormat17.parse(normalized)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 将Date对象格式化为17位时间戳
     * @param date Date对象
     * @return 17位时间戳
     */
    @Synchronized
    fun formatFromDate(date: Date): String {
        return dateFormat17.format(date)
    }
}
