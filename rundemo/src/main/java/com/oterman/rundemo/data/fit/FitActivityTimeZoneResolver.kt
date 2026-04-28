package com.oterman.rundemo.data.fit

import com.oterman.rundemo.util.RLog
import net.iakovlev.timeshape.TimeZoneEngine
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

/**
 * Resolves the timezone where a FIT activity happened.
 *
 * FIT timestamps are absolute UTC instants. Calendar-day ownership needs the
 * activity timezone, not the timezone of the device importing the file.
 */
object FitActivityTimeZoneResolver {
    private const val TAG = "FitActivityTimeZoneResolver"

    @Volatile
    private var engine: TimeZoneEngine? = null

    fun resolve(parseResult: FitParseResult): String {
        resolveFromGps(parseResult)?.let { return it }
        resolveFixedOffset(parseResult)?.let { return it }
        return TimeZone.getDefault().id
    }

    fun toTimeZone(zoneId: String?): TimeZone {
        if (zoneId.isNullOrBlank()) return TimeZone.getDefault()
        return try {
            ZoneId.of(zoneId)
            TimeZone.getTimeZone(zoneId)
        } catch (e: Exception) {
            TimeZone.getDefault()
        }
    }

    private fun resolveFromGps(parseResult: FitParseResult): String? {
        val point = parseResult.records.firstOrNull {
            it.positionLat != null && it.positionLong != null
        } ?: return null

        return try {
            val latitude = FitFileParser.semicirclesToDegrees(point.positionLat!!)
            val longitude = FitFileParser.semicirclesToDegrees(point.positionLong!!)
            getEngine().query(latitude, longitude).orElse(null)?.id?.also {
                RLog.i(TAG, "FIT活动时区由GPS推断: $it ($latitude,$longitude)")
            }
        } catch (e: Throwable) {
            RLog.w(TAG, "GPS推断FIT活动时区失败: ${e.message}")
            null
        }
    }

    private fun getEngine(): TimeZoneEngine {
        return engine ?: synchronized(this) {
            engine ?: TimeZoneEngine.initialize().also { engine = it }
        }
    }

    private fun resolveFixedOffset(parseResult: FitParseResult): String? {
        val activity = parseResult.activity ?: return null
        val timestamp = activity.timestamp ?: return null
        val localTimestamp = activity.localTimestamp ?: return null
        val offsetSeconds = (localTimestamp - timestamp).toInt()
        if (offsetSeconds !in -18 * 3600..18 * 3600) return null

        val sign = if (offsetSeconds >= 0) "+" else "-"
        val absSeconds = kotlin.math.abs(offsetSeconds)
        val hours = absSeconds / 3600
        val minutes = (absSeconds % 3600) / 60
        val zoneId = String.format(Locale.US, "GMT%s%02d:%02d", sign, hours, minutes)
        RLog.i(TAG, "FIT活动时区由local_timestamp推断为固定偏移: $zoneId")
        return zoneId
    }
}
