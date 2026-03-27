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
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 生成结构化诊断文件，随日志一同打包导出。
 *
 * 生成文件：
 * - settings.txt      生理设置与目标（key-value）
 * - vdot_history.csv  近60条跑力历史
 * - pb_records.csv    全部 PB 记录
 * - run_summaries.csv 最近30条跑步摘要，含 top5（按 vdot）标注
 *
 * 采样点数据（run_sample_point）不导出。
 */
object DiagnosticDataExporter {

    private val dtFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun generateDiagnosticFiles(context: Context): List<File> = withContext(Dispatchers.IO) {
        val outDir = File(context.cacheDir, "log_exports").also { it.mkdirs() }
        val prefs = PreferencesManager(context)
        val userId = prefs.getUserId() ?: ""
        val db = RunDatabase.getInstance(context)

        listOf(
            writeSettingsTxt(outDir, prefs, userId),
            writeVdotHistoryCsv(outDir, db, userId),
            writePbRecordsCsv(outDir, db, userId),
            writeRunSummariesCsv(outDir, db, userId)
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
            appendLine("user_id: ${if (userId.length > 8) "${userId.take(8)}..." else userId.ifEmpty { "(not logged in)" }}")
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

    private suspend fun writeVdotHistoryCsv(outDir: File, db: RunDatabase, userId: String): File {
        val rows = if (userId.isNotEmpty()) {
            db.overallVdotDao().getRecentVdotsForUser(userId, 60)
        } else emptyList()

        val content = buildString {
            appendLine("date,value,origin_value,confidence,workout_id")
            if (rows.isEmpty()) {
                appendLine("# no data")
            } else {
                rows.forEach { v ->
                    appendLine("${dateFmt.format(Date(v.date))},${v.value},${v.originValue},${v.confidence},${v.workoutId}")
                }
            }
        }
        return File(outDir, "vdot_history.csv").also { it.writeText(content) }
    }

    // ─── pb_records.csv ────────────────────────────────────────────────────────

    private suspend fun writePbRecordsCsv(outDir: File, db: RunDatabase, userId: String): File {
        val pbDao = db.pbRecordDao()
        var hasData = false

        val content = buildString {
            appendLine("type,sub_type,description,value,unit,complete_time")
            if (userId.isNotEmpty()) {
                PBSpeedKey.entries.forEach { key ->
                    val rec = pbDao.getBestRecordForUser(userId, "Speed", key.subType)
                    if (rec != null) {
                        hasData = true
                        appendLine("Speed,${key.subType},${key.description},${rec.value},min/km,${dtFmt.format(Date(rec.completeTime))}")
                    }
                }
                PBAbilityKey.entries.forEach { key ->
                    val rec = pbDao.getBestAbilityRecordForUser(userId, "Ability", key.subType)
                    if (rec != null) {
                        hasData = true
                        val unit = when (key) {
                            PBAbilityKey.MAX_VDOT -> "vdot"
                            PBAbilityKey.MAX_DISTANCE -> "km"
                            PBAbilityKey.MAX_DURATION -> "min"
                        }
                        appendLine("Ability,${key.subType},${key.description},${rec.value},$unit,${dtFmt.format(Date(rec.completeTime))}")
                    }
                }
            }
            if (!hasData) appendLine("# no data")
        }
        return File(outDir, "pb_records.csv").also { it.writeText(content) }
    }

    // ─── run_summaries.csv ─────────────────────────────────────────────────────

    private suspend fun writeRunSummariesCsv(outDir: File, db: RunDatabase, userId: String): File {
        val records = if (userId.isNotEmpty()) {
            db.runRecordDao().getRecentRecordsForUser(userId, 30)
        } else emptyList()

        val top5Ids = records
            .filter { it.vdot > 0 }
            .sortedByDescending { it.vdot }
            .take(5)
            .map { it.workoutId }
            .toSet()

        val content = buildString {
            appendLine("is_top5_vdot,start_time,total_distance_km,duration_min,avg_pace_min_per_km,avg_heart_rate,vdot,overall_vdot,training_effect,training_load,datasource")
            if (records.isEmpty()) {
                appendLine("# no data")
            } else {
                records.forEach { r ->
                    appendLine(
                        "${r.workoutId in top5Ids}," +
                        "${dtFmt.format(Date(r.startTime))}," +
                        "${"%.2f".format(r.totalDistance)}," +
                        "${"%.1f".format(r.duration)}," +
                        "${"%.2f".format(r.averageSpeed)}," +
                        "${r.averageHeartRate.roundToInt()}," +
                        "${"%.1f".format(r.vdot)}," +
                        "${"%.1f".format(r.overallVdot)}," +
                        "${"%.1f".format(r.trainingEffect)}," +
                        "${"%.0f".format(r.trainingLoad)}," +
                        "${r.datasource ?: ""}"
                    )
                }
            }
        }
        return File(outDir, "run_summaries.csv").also { it.writeText(content) }
    }
}
