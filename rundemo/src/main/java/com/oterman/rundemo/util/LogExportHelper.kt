package com.oterman.rundemo.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import kotlin.random.Random

object LogExportHelper {

    /**
     * 将所有 RLog 日志文件与诊断数据加密压缩，并返回可用于系统分享的 Intent。
     *
     * 打包结构：
     *   runyay_logs_NN.zip（外层）
     *     ├── 日志文件 × N
     *     └── runyay_data_MM.zip（内层，独立随机数 MM、独立密码）
     *           ├── settings.txt
     *           ├── vdot_history.csv
     *           ├── pb_records.csv
     *           ├── run_summaries.csv
     *           └── daily_health.csv
     *
     * 密码规则：SecurityProvider.generateTarget() + 两位随机数（00-99）
     * 文件名规则：runyay_logs_NN.zip / runyay_data_MM.zip（NN/MM 辅助解压时推导密码）
     *
     * @return 分享 Intent，若无日志文件则返回 null
     */
    suspend fun exportLogs(context: Context): Intent? = withContext(Dispatchers.IO) {
        val logFiles = RLog.getAllLogFiles()
        if (logFiles.isEmpty()) return@withContext null

        val outDir = File(context.cacheDir, "log_exports").also { it.mkdirs() }

        val encParams = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            encryptionMethod = EncryptionMethod.AES
            isEncryptFiles = true
        }

        // ── 1. 生成诊断文件 ──────────────────────────────────────────────────────
        val diagnosticFiles = DiagnosticDataExporter.generateDiagnosticFiles(context)

        // ── 2. 打内层诊断 ZIP（独立随机数 MM）──────────────────────────────────
        val mm = String.format("%02d", Random.nextInt(0, 100))
        val dataPassword = SecurityProvider.generateTarget() + mm
        val dataZipName = "runyay_data_$mm.zip"
        val dataZipFile = File(outDir, dataZipName)
        if (dataZipFile.exists()) dataZipFile.delete()

        ZipFile(dataZipFile, dataPassword.toCharArray()).use { dataZip ->
            diagnosticFiles.forEach { dataZip.addFile(it, encParams) }
        }
        // 诊断明文文件用完即删
        diagnosticFiles.forEach { it.delete() }
        diagnosticFiles.firstOrNull()?.parentFile?.let { diagDir ->
            if (diagDir.exists() && diagDir.list()?.isEmpty() == true) diagDir.delete()
        }

        // ── 3. 打外层日志 ZIP（原有逻辑，独立随机数 NN）────────────────────────
        val nn = String.format("%02d", Random.nextInt(0, 100))
        val logsPassword = SecurityProvider.generateTarget() + nn
        val logsZipName = "runyay_logs_$nn.zip"
        val logsZipFile = File(outDir, logsZipName)
        if (logsZipFile.exists()) logsZipFile.delete()

        ZipFile(logsZipFile, logsPassword.toCharArray()).use { logsZip ->
            logFiles.forEach { logsZip.addFile(it, encParams) }
            logsZip.addFile(dataZipFile, encParams)
        }
        // 内层 ZIP 打入外层后删除
        dataZipFile.delete()

        // ── 4. 返回分享 Intent ───────────────────────────────────────────────────
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            logsZipFile
        )
        Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
