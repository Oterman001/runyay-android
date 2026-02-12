package com.oterman.rundemo.data.fit

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 暂停事件转换器
 * 将FIT文件中的Event消息转换为eventStr JSON字符串
 * 对齐iOS GarminFitEventConverter
 */
object FitEventConverter {

    private const val TAG = "FitEventConverter"
    private val gson = Gson()

    /**
     * 暂停事件数据模型
     */
    data class RunEvent(
        var beginTime: String = "",
        var endTime: String = "",
        var eventType: Int = 0  // 1=暂停事件
    )

    /**
     * 暂停区间（用于区间计算时排除暂停时间）
     */
    data class PauseEvent(
        val beginTimeMs: Long,
        val endTimeMs: Long
    )

    // 北京时间格式化器
    private val beijingDateFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }
    }

    /**
     * 将FitEvent列表转换为eventStr JSON字符串
     * 逻辑：匹配timer事件的stop/start对，生成暂停区间
     *
     * @param events FIT解析出的事件列表
     * @return eventStr JSON字符串
     */
    fun convertEventsToJson(events: List<FitEvent>): String {
        val runEvents = mutableListOf<RunEvent>()
        var pauseStartMs: Long? = null

        Log.i(TAG, "开始解析Event，共${events.size}个事件")

        for ((index, event) in events.withIndex()) {
            val eventName = event.event ?: continue
            val eventType = event.eventType ?: continue
            val timestampMs = FitFileParser.fitTimestampToMillis(event.timestamp)

            Log.d(TAG, "Event[$index]: event=$eventName, eventType=$eventType, timestamp=$timestampMs")

            // 只处理timer类型的事件（对齐iOS: eventEnum == .timer）
            if (eventName.lowercase() != "timer") continue

            when (eventType.lowercase()) {
                "stop", "stop_all" -> {
                    // 暂停开始
                    pauseStartMs = timestampMs
                    Log.i(TAG, "检测到暂停开始: $timestampMs")
                }
                "start" -> {
                    // 恢复（暂停结束）
                    val startMs = pauseStartMs
                    if (startMs != null) {
                        val runEvent = RunEvent(
                            beginTime = formatToBeijingTime(startMs),
                            endTime = formatToBeijingTime(timestampMs),
                            eventType = 1 // 暂停事件
                        )
                        runEvents.add(runEvent)
                        Log.i(TAG, "记录暂停事件: ${runEvent.beginTime} -> ${runEvent.endTime}")
                        pauseStartMs = null
                    }
                }
            }
        }

        return try {
            val jsonString = gson.toJson(runEvents)
            Log.i(TAG, "成功转换${runEvents.size}个暂停事件")
            jsonString
        } catch (e: Exception) {
            Log.e(TAG, "转换事件为JSON失败: ${e.message}")
            ""
        }
    }

    /**
     * 将eventStr JSON转换为PauseEvent列表
     * 用于区间计算时排除暂停时间
     *
     * @param eventStr eventStr JSON字符串
     * @return PauseEvent列表
     */
    fun convertEventStrToPauseList(eventStr: String?): List<PauseEvent> {
        if (eventStr.isNullOrEmpty()) return emptyList()

        return try {
            val type = object : TypeToken<List<RunEvent>>() {}.type
            val runEvents: List<RunEvent> = gson.fromJson(eventStr, type)
            runEvents
                .filter { it.eventType == 1 }
                .mapNotNull { event ->
                    val beginMs = parseBeijingTimeToMs(event.beginTime) ?: return@mapNotNull null
                    val endMs = parseBeijingTimeToMs(event.endTime) ?: return@mapNotNull null
                    PauseEvent(beginTimeMs = beginMs, endTimeMs = endMs)
                }
                .also { Log.i(TAG, "转换了${it.size}个暂停事件") }
        } catch (e: Exception) {
            Log.e(TAG, "解析eventStr失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 计算两个时间点之间的活跃时间（排除暂停）
     *
     * @param beginTimeMs 开始时间戳(ms)
     * @param endTimeMs 结束时间戳(ms)
     * @param pauseList 暂停事件列表
     * @return 活跃时间(秒)
     */
    fun getActiveTimeBetween(
        beginTimeMs: Long,
        endTimeMs: Long,
        pauseList: List<PauseEvent>
    ): Double {
        var totalPause = 0.0
        for (pause in pauseList) {
            // 计算暂停区间与查询区间的交集
            val overlapStart = maxOf(pause.beginTimeMs, beginTimeMs)
            val overlapEnd = minOf(pause.endTimeMs, endTimeMs)
            if (overlapStart < overlapEnd) {
                totalPause += (overlapEnd - overlapStart) / 1000.0
            }
        }
        return (endTimeMs - beginTimeMs) / 1000.0 - totalPause
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 毫秒时间戳转北京时间字符串
     */
    private fun formatToBeijingTime(timestampMs: Long): String {
        return beijingDateFormat.format(Date(timestampMs))
    }

    /**
     * 北京时间字符串转毫秒时间戳
     */
    private fun parseBeijingTimeToMs(dateStr: String): Long? {
        return try {
            beijingDateFormat.parse(dateStr)?.time
        } catch (e: Exception) {
            Log.w(TAG, "解析时间字符串失败: $dateStr")
            null
        }
    }
}

