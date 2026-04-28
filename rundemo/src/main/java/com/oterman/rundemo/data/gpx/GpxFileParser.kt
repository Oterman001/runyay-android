package com.oterman.rundemo.data.gpx

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.oterman.rundemo.util.RLog
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class GpxParseResult(
    val trackName: String?,
    val activityType: String?,
    val startTime: Long,
    val endTime: Long,
    val trackPoints: List<GpxTrackPoint>,
    val creator: String? = null
)

data class GpxTrackPoint(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val heartRate: Int?,
    val cadence: Int?   // 单脚 spm（与 FIT 格式一致），GpxDataMapper 存入 FitRecord.cadence 原值，FitDataMapper.toSamplePointEntities ×2
)

class GpxFileParser(private val context: Context) {

    private enum class TextTarget {
        NONE, TRACK_NAME, TRACK_TYPE, TRKPT_TIME, TRKPT_ELE, TRKPT_HR, TRKPT_CAD
    }

    companion object {
        private const val TAG = "GpxFileParser"

        fun parseIso8601(text: String): Long {
            return try {
                val t = text.trim()
                when {
                    t.endsWith("Z") -> {
                        val base = t.substringBefore('.').dropLast(1)  // remove ms and 'Z'
                        sdfUtc().parse(base)?.time ?: 0L
                    }
                    t.length > 19 && (t[19] == '+' || t[19] == '-') -> {
                        // Has timezone offset like +08:00
                        val base = t.substringBefore('.')  // remove ms
                        val tzStart = base.length - 6      // "+08:00" is 6 chars
                        val dateTime = base.substring(0, tzStart)
                        val tz = base.substring(tzStart).replace(":", "")
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
                            .parse("$dateTime$tz")?.time ?: 0L
                    }
                    else -> {
                        sdfUtc().parse(t.take(19))?.time ?: 0L
                    }
                }
            } catch (e: Exception) {
                RLog.w(TAG, "GPX时间解析失败: $text")
                0L
            }
        }

        private fun sdfUtc() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun parse(uri: Uri): Result<GpxParseResult> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("无法打开文件"))
            val result = inputStream.use { parseStream(it) }
            when {
                result.trackPoints.isEmpty() ->
                    Result.failure(Exception("无效的GPX文件：未找到轨迹点"))
                result.trackPoints.size < 2 ->
                    Result.failure(Exception("无效的GPX文件：轨迹点不足（${result.trackPoints.size}个）"))
                else -> Result.success(result)
            }
        } catch (e: Exception) {
            RLog.e(TAG, "解析GPX文件失败", e)
            Result.failure(Exception("解析GPX文件失败: ${e.message}", e))
        }
    }

    private fun parseStream(input: InputStream): GpxParseResult {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var trackName: String? = null
        var activityType: String? = null
        var creator: String? = null
        val trackPoints = mutableListOf<GpxTrackPoint>()

        var inTrk = false
        var inTrkpt = false
        var inExtensions = false
        var inTrackPointExtension = false
        var textTarget = TextTarget.NONE

        var trkptLat = 0.0
        var trkptLon = 0.0
        var trkptTime = 0L
        var trkptAlt: Double? = null
        var trkptHr: Int? = null
        var trkptCad: Int? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val local = parser.name.substringAfterLast(':')
                    textTarget = TextTarget.NONE
                    when {
                        local == "gpx" ->
                            creator = parser.getAttributeValue(null, "creator")
                        local == "trk" && !inTrkpt ->
                            inTrk = true
                        local == "name" && inTrk && !inTrkpt ->
                            textTarget = TextTarget.TRACK_NAME
                        local == "type" && inTrk && !inTrkpt ->
                            textTarget = TextTarget.TRACK_TYPE
                        local == "trkpt" -> {
                            inTrkpt = true
                            trkptLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                            trkptLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                            trkptTime = 0L; trkptAlt = null; trkptHr = null; trkptCad = null
                        }
                        local == "time" && inTrkpt && !inExtensions ->
                            textTarget = TextTarget.TRKPT_TIME
                        local == "ele" && inTrkpt ->
                            textTarget = TextTarget.TRKPT_ELE
                        local == "extensions" && inTrkpt ->
                            inExtensions = true
                        local == "TrackPointExtension" && inExtensions ->
                            inTrackPointExtension = true
                        local == "hr" && inTrackPointExtension ->
                            textTarget = TextTarget.TRKPT_HR
                        local == "cad" && inTrackPointExtension ->
                            textTarget = TextTarget.TRKPT_CAD
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        when (textTarget) {
                            TextTarget.TRACK_NAME -> { trackName = text; textTarget = TextTarget.NONE }
                            TextTarget.TRACK_TYPE -> { activityType = text; textTarget = TextTarget.NONE }
                            TextTarget.TRKPT_TIME -> { trkptTime = parseIso8601(text); textTarget = TextTarget.NONE }
                            TextTarget.TRKPT_ELE -> { trkptAlt = text.toDoubleOrNull(); textTarget = TextTarget.NONE }
                            TextTarget.TRKPT_HR -> { trkptHr = text.toIntOrNull(); textTarget = TextTarget.NONE }
                            TextTarget.TRKPT_CAD -> { trkptCad = text.toIntOrNull(); textTarget = TextTarget.NONE }
                            TextTarget.NONE -> {}
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val local = parser.name.substringAfterLast(':')
                    textTarget = TextTarget.NONE
                    when {
                        local == "trkpt" -> {
                            if (trkptTime > 0) {
                                trackPoints.add(GpxTrackPoint(
                                    timestamp = trkptTime,
                                    latitude = trkptLat,
                                    longitude = trkptLon,
                                    altitude = trkptAlt,
                                    heartRate = trkptHr,
                                    cadence = trkptCad
                                ))
                            }
                            inTrkpt = false
                            inExtensions = false
                            inTrackPointExtension = false
                        }
                        local == "extensions" -> { inExtensions = false; inTrackPointExtension = false }
                        local == "TrackPointExtension" -> inTrackPointExtension = false
                        local == "trk" -> inTrk = false
                    }
                }
            }
            eventType = parser.next()
        }

        return GpxParseResult(
            trackName = trackName,
            activityType = activityType,
            startTime = trackPoints.firstOrNull()?.timestamp ?: 0L,
            endTime = trackPoints.lastOrNull()?.timestamp ?: 0L,
            trackPoints = trackPoints,
            creator = creator
        )
    }
}
