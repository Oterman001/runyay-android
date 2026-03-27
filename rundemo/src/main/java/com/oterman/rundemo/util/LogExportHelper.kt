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
     * 将所有 RLog 日志文件加密压缩，并返回可用于系统分享的 Intent。
     *
     * 密码规则：SecurityProvider.generateTarget() + 两位随机数（00-99）
     * 文件名规则：rundemo_logs_NN.zip（NN 即两位随机数，用于辅助解压时推导密码）
     *
     * @return 分享 Intent，若无日志文件则返回 null
     */
    suspend fun exportLogs(context: Context): Intent? = withContext(Dispatchers.IO) {
        val logFiles = RLog.getAllLogFiles()
        if (logFiles.isEmpty()) return@withContext null

        val nn = String.format("%02d", Random.nextInt(0, 100))
        val password = SecurityProvider.generateTarget() + nn
        val zipFileName = "runyay_logs_$nn.zip"

        val outDir = File(context.cacheDir, "log_exports").also { it.mkdirs() }
        val destZip = File(outDir, zipFileName)
        if (destZip.exists()) destZip.delete()

        val zipFile = ZipFile(destZip, password.toCharArray())
        val params = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            encryptionMethod = EncryptionMethod.AES
            isEncryptFiles = true
        }
        logFiles.forEach { zipFile.addFile(it, params) }

        val diagnosticFiles = DiagnosticDataExporter.generateDiagnosticFiles(context)
        diagnosticFiles.forEach { zipFile.addFile(it, params) }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            destZip
        )
        Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
