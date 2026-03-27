package com.oterman.rundemo.util

import android.content.Context
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.domain.model.PBAbilityKey
import com.oterman.rundemo.domain.model.PBSpeedKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 生成结构化诊断文件，随日志一同打包导出。
 *
 * 生成文件（写入 cacheDir/log_exports/diag/）：
 * - settings.txt       生理设置与目标（key-value）
 * - vdot_history.csv   近3个月跑力历史 + top10（有效/无效各组）
 * - pb_records.csv     每个 subType 的 top5（有效/无效各组）
 * - run_summaries.csv  近3个月跑步摘要 + top5（跑力/时间/距离）
 * - daily_health.csv   近3个月每日健康数据
 *
 * 采样点数据（run_sample_point）不导出。
 * 严格按 userId 过滤，不导出其他用户数据。
 */
object DiagnosticDataExporter {

    private val dtFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun generateDiagnosticFiles(context: Context): List<File> = withContext(Dispatchers.IO) {
        val outDir = File(context.cacheDir, "log_exports/diag").also { it.mkdirs() }
        val prefs = PreferencesManager(context)
        val userId = prefs.getUserId() ?: ""
        val db = RunDatabase.getInstance(context)

        // 近3个月时间边界
        val now = System.currentTimeMillis()
        val threeMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }.timeInMillis
        val threeMonthsAgoDate = dateFmt.format(Date(threeMonthsAgo))
        val todayDate = dateFmt.format(Date(now))

        listOf(
            writeSettingsTxt(outDir, prefs, userId),
            writeVdotHistoryCsv(outDir, db, userId, threeMonthsAgo, now),
            writePbRecordsCsv(outDir, db, userId),
            writeRunSummariesCsv(outDir, db, userId, threeMonthsAgo, now),
            writeDailyHealthCsv(outDir, db, userId, threeMonthsAgoDate, todayDate)
        )
    }

    // ─── settings.txt ──────────────────────────────────────────────────────────

    private fun writeSettingsTxt(outDir: File, prefs: PreferencesManager, userId: String): File {
        val hr = prefs.getHearRateZoneSettings()
        val goal = prefs.getGoalSettings()
        val age = if (hr.birthdayMillis > 0) {
            ((System.currentTimeMillis() - hr.birthdayMillis) / (365.25 * 24 * 3600 * 1000)).toInt()
        } else 0

        val content = buildString {
            appendLine("generated_at: ${dtFmt.format(Date())}")
            appendLine("user_id: ${userId.ifEmpty { "(not logged in)" }}")
            appendLine()
            appendLine("# Physiological Settings")
            appendLine("max_heart_rate: ${hr.maxHeartRate}")
            appendLine("resting_heart_rate: ${hr.restingHeartRate}")
            appendLine("age: ${if (age > 0) age else "not set"}")
            appendLine("gender: ${if (hr.isMale) "male" else "female"}")
            appendLine("physio_setup_completed: ${prefs.isPhysioSetupCompleted()}")
            appendLine("auto_sync_resting_hr: ${hr.isAutoSyncEnabled}")
            appendLine("preferred_platform: ${hr.preferredPlatform ?: "auto (cross-platform best)"}")
            appendLine()
            appendLine("# Goal Settings")
            appendLine("goal_enabled: ${goal.goalEnabled}")
            appendLine("goal_type: ${goal.goalType.name}")
            appendLine("year_distance_goal_km: ${goal.yearDistanceGoal}")
            appendLine("month_distance_goal_km: ${goal.monthDistanceGoal}")
            appendLine("year_duration_goal_min: ${goal.yearDurationGoal}")
            appendLine("month_duration_goal_min: ${goal.monthDurationGoal}")
        }
        return File(outDir, "settings.txt").also { it.writeText(content) }
    }

    // ─── vdot_history.csv ──────────────────────────────────────────────────────

    private suspend fun writeVdotHistoryCsv(
        outDir: File, db: RunDatabase, userId: String,
        start: Long, end: Long
    ): File {
        val dao = db.overallVdotDao()
        val recent3mo = if (userId.isNotEmpty()) dao.getAllVdotsInRangeForUser(userId, start, end) else emptyList()
        val top10Inc = if (userId.isNotEmpty()) dao.getTopVdotsInclusiveForUser(userId, 10) else emptyList()
        val top10Exc = if (userId.isNotEmpty()) dao.getTopVdotsExcludedForUser(userId, 10) else emptyList()

        val content = buildString {
            appendLine("section,date,value,origin_value,confidence,inclusive_level,workout_id")
            if (recent3mo.isEmpty() && top10Inc.isEmpty() && top10Exc.isEmpty()) {
                appendLine("# no data")
            } else {
                recent3mo.forEach { v ->
                    appendLine("recent_3mo,${dateFmt.format(Date(v.date))},${v.value},${v.originValue},${v.confidence},${v.inclusiveLevel},${v.workoutId}")
                }
                top10Inc.forEach { v ->
                    appendLine("top10_inclusive,${dateFmt.format(Date(v.date))},${v.value},${v.originValue},${v.confidence},${v.inclusiveLevel},${v.workoutId}")
                }
                top10Exc.forEach { v ->
                    appendLine("top10_excluded,${dateFmt.format(Date(v.date))},${v.value},${v.originValue},${v.confidence},${v.inclusiveLevel},${v.workoutId}")
                }
            }
        }
        return File(outDir, "vdot_history.csv").also { it.writeText(content) }
    }

    // ─── pb_records.csv ────────────────────────────────────────────────────────

    private suspend fun writePbRecordsCsv(outDir: File, db: RunDatabase, userId: String): File {
        val dao = db.pbRecordDao()

        val content = buildString {
            appendLine("section,type,sub_type,description,rank,value,unit,complete_time,inclusive_level,workout_id")
            var hasData = false

            if (userId.isNotEmpty()) {
                // ── Speed PBs ──
                PBSpeedKey.entries.forEach { key ->
                    val inclusive = dao.getTopSpeedRecordsForUser(userId, "Speed", key.subType, 5)
                    val excluded = dao.getTopExcludedSpeedRecordsForUser(userId, "Speed", key.subType, 5)
                    inclusive.forEachIndexed { i, rec ->
                        hasData = true
                        appendLine("top5_inclusive,Speed,${key.subType},${key.description},${i + 1},${rec.value},min/km,${dtFmt.format(Date(rec.completeTime))},${rec.inclusiveLevel},${rec.workoutId}")
                    }
                    excluded.forEachIndexed { i, rec ->
                        hasData = true
                        appendLine("top5_excluded,Speed,${key.subType},${key.description},${i + 1},${rec.value},min/km,${dtFmt.format(Date(rec.completeTime))},${rec.inclusiveLevel},${rec.workoutId}")
                    }
                }
                // ── Ability PBs ──
                PBAbilityKey.entries.forEach { key ->
                    val unit = when (key) {
                        PBAbilityKey.MAX_VDOT -> "vdot"
                        PBAbilityKey.MAX_DISTANCE -> "km"
                        PBAbilityKey.MAX_DURATION -> "min"
                    }
                    val inclusive = dao.getTopAbilityRecordsForUser(userId, "Ability", key.subType, 5)
                    val excluded = dao.getTopExcludedAbilityRecordsForUser(userId, "Ability", key.subType, 5)
                    inclusive.forEachIndexed { i, rec ->
                        hasData = true
                        appendLine("top5_inclusive,Ability,${key.subType},${key.description},${i + 1},${rec.value},$unit,${dtFmt.format(Date(rec.completeTime))},${rec.inclusiveLevel},${rec.workoutId}")
                    }
                    excluded.forEachIndexed { i, rec ->
                        hasData = true
                        appendLine("top5_excluded,Ability,${key.subType},${key.description},${i + 1},${rec.value},$unit,${dtFmt.format(Date(rec.completeTime))},${rec.inclusiveLevel},${rec.workoutId}")
                    }
                }
            }

            if (!hasData) appendLine("# no data")
        }
        return File(outDir, "pb_records.csv").also { it.writeText(content) }
    }

    // ─── run_summaries.csv ─────────────────────────────────────────────────────

    private suspend fun writeRunSummariesCsv(
        outDir: File, db: RunDatabase, userId: String,
        start: Long, end: Long
    ): File {
        val dao = db.runRecordDao()
        val recent3mo = if (userId.isNotEmpty()) dao.getByTimeRangeForUser(userId, start, end) else emptyList()
        val top5Vdot = if (userId.isNotEmpty()) dao.getTopByVdotForUser(userId, 5) else emptyList()
        val top5Duration = if (userId.isNotEmpty()) dao.getTopByDurationForUser(userId, 5) else emptyList()
        val top5Distance = if (userId.isNotEmpty()) dao.getTopByDistanceForUser(userId, 5) else emptyList()

        val header = "section,workout_id,start_time,total_distance_km,duration_min,avg_pace_min_per_km,avg_heart_rate,vdot,overall_vdot,training_effect,training_load,datasource,device_info,origin_id"

        fun rowOf(section: String, r: com.oterman.rundemo.data.local.entity.RunRecordEntity) =
            "$section,${r.workoutId},${dtFmt.format(Date(r.startTime))},${"%.2f".format(r.totalDistance)},${"%.1f".format(r.duration)},${"%.2f".format(r.averageSpeed)},${r.averageHeartRate.roundToInt()},${"%.1f".format(r.vdot)},${"%.1f".format(r.overallVdot)},${"%.1f".format(r.trainingEffect)},${"%.0f".format(r.trainingLoad)},${r.datasource ?: ""},${r.deviceInfo ?: ""},${r.originId ?: ""}"

        val content = buildString {
            appendLine(header)
            if (recent3mo.isEmpty() && top5Vdot.isEmpty() && top5Duration.isEmpty() && top5Distance.isEmpty()) {
                appendLine("# no data")
            } else {
                recent3mo.forEach { appendLine(rowOf("recent_3mo", it)) }
                top5Vdot.forEach { appendLine(rowOf("top5_vdot", it)) }
                top5Duration.forEach { appendLine(rowOf("top5_duration", it)) }
                top5Distance.forEach { appendLine(rowOf("top5_distance", it)) }
            }
        }
        return File(outDir, "run_summaries.csv").also { it.writeText(content) }
    }

    // ─── daily_health.csv ──────────────────────────────────────────────────────

    private suspend fun writeDailyHealthCsv(
        outDir: File, db: RunDatabase, userId: String,
        startDate: String, endDate: String
    ): File {
        val rows = if (userId.isNotEmpty()) {
            db.dailyHealthDao().getByDateRangeForUser(userId, startDate, endDate)
        } else emptyList()

        val content = buildString {
            appendLine("calendar_date,platform_code,resting_heart_rate,vo2_max,fetched_at")
            if (rows.isEmpty()) {
                appendLine("# no data")
            } else {
                rows.forEach { h ->
                    appendLine("${h.calendarDate},${h.platformCode},${h.restingHeartRate ?: ""},${h.vo2Max ?: ""},${dtFmt.format(Date(h.fetchedAt))}")
                }
            }
        }
        return File(outDir, "daily_health.csv").also { it.writeText(content) }
    }
}
