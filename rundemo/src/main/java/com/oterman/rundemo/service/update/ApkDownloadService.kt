package com.oterman.rundemo.service.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.oterman.rundemo.R
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

sealed class ApkDownloadState {
    object Idle : ApkDownloadState()
    data class Downloading(val progress: Float) : ApkDownloadState()
    data class Completed(val apkFile: File) : ApkDownloadState()
    data class Failed(val message: String) : ApkDownloadState()
}

/**
 * APK 下载前台服务
 * 进度通过前台通知展示，下载完成后以可点击通知触发安装（兼容 Android 10+ 后台无法启动 Activity）
 */
class ApkDownloadService : Service() {

    companion object {
        private const val TAG = "ApkDownloadService"
        private const val CHANNEL_ID = "apk_download_channel"
        private const val NOTIFICATION_ID = 10002
        private const val EXTRA_DOWNLOAD_URL = "extra_download_url"
        private const val EXTRA_FILE_NAME = "extra_file_name"
        private const val EXTRA_VERSION_CODE = "extra_version_code"

        private val _downloadState = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Idle)
        val downloadState: StateFlow<ApkDownloadState> = _downloadState

        fun start(
            context: Context,
            downloadUrl: String,
            versionCode: Int,
            fileName: String = "yayarun_update.apk"
        ) {
            _downloadState.value = ApkDownloadState.Idle
            val intent = Intent(context, ApkDownloadService::class.java).apply {
                putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
                putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_VERSION_CODE, versionCode)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val okHttpClient = OkHttpClient()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val downloadUrl = intent?.getStringExtra(EXTRA_DOWNLOAD_URL) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "yayarun_update.apk"
        val versionCode = intent.getIntExtra(EXTRA_VERSION_CODE, -1)

        startForeground(NOTIFICATION_ID, createProgressNotification(0f))
        _downloadState.value = ApkDownloadState.Downloading(0f)

        serviceScope.launch {
            downloadApk(downloadUrl, fileName, versionCode)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private suspend fun downloadApk(url: String, fileName: String, versionCode: Int) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val msg = "下载失败: HTTP ${response.code}"
                RLog.e(TAG, msg)
                _downloadState.value = ApkDownloadState.Failed(msg)
                stopSelf()
                return
            }

            val body = response.body ?: run {
                val msg = "下载失败: 响应体为空"
                RLog.e(TAG, msg)
                _downloadState.value = ApkDownloadState.Failed(msg)
                stopSelf()
                return
            }

            val totalBytes = body.contentLength()
            val downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: cacheDir
            val downloadedFile = File(downloadsDir, fileName)

            body.byteStream().use { inputStream ->
                FileOutputStream(downloadedFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L
                    var bytesRead: Int
                    var lastNotifyProgress = -1f

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) {
                            (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                        _downloadState.value = ApkDownloadState.Downloading(progress)

                        // 每 2% 更新一次通知，避免频繁刷新
                        if (progress - lastNotifyProgress >= 0.02f) {
                            lastNotifyProgress = progress
                            updateProgressNotification(progress)
                        }
                    }
                }
            }

            RLog.i(TAG, "下载完成: ${downloadedFile.absolutePath}")

            // 若文件是 ZIP 格式（包含 APK 本身，因为 APK 也基于 ZIP），尝试从内部提取 .apk
            // 若内部没有 .apk 条目，说明该文件本身就是 APK（只是扩展名被改成了 zip），直接当 APK 使用
            val apkFile = if (isZipFile(downloadedFile)) {
                RLog.i(TAG, "检测到 ZIP 格式文件，尝试提取内部 APK")
                val extracted = extractApkFromZip(downloadedFile, downloadsDir)
                if (extracted != null) {
                    // 真正的 zip 包，提取成功
                    downloadedFile.delete()
                    extracted
                } else {
                    // 内部无 .apk 条目，文件本身即为 APK（扩展名为 zip）
                    RLog.i(TAG, "ZIP 内无 APK 条目，将文件本身作为 APK 处理")
                    val renamedApk = File(downloadsDir, downloadedFile.nameWithoutExtension + ".apk")
                    downloadedFile.renameTo(renamedApk)
                    renamedApk
                }
            } else {
                downloadedFile
            }

            // 保存到 SharedPreferences，供下次检查更新时判断是否已下载
            if (versionCode > 0) {
                PreferencesManager(this).saveDownloadedApkInfo(versionCode, apkFile.absolutePath)
            }

            _downloadState.value = ApkDownloadState.Completed(apkFile)
            showInstallNotification(apkFile)
            stopSelf()

        } catch (e: Exception) {
            RLog.e(TAG, "下载异常", e)
            _downloadState.value = ApkDownloadState.Failed(e.message ?: "未知错误")
            stopSelf()
        }
    }

    /**
     * 判断文件是否为 zip（通过 magic bytes PK\x03\x04）
     */
    private fun isZipFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            file.inputStream().use { stream ->
                val magic = ByteArray(4)
                stream.read(magic)
                magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte() &&
                        magic[2] == 0x03.toByte() && magic[3] == 0x04.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从 zip 中提取第一个 .apk 文件
     */
    private fun extractApkFromZip(zipFile: File, destDir: File): File? {
        return try {
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                        val apkName = File(entry.name).name
                        val outFile = File(destDir, apkName)
                        FileOutputStream(outFile).use { out ->
                            zis.copyTo(out)
                        }
                        RLog.i(TAG, "解压 APK: ${outFile.absolutePath}")
                        return@use outFile
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                null
            }
        } catch (e: Exception) {
            RLog.e(TAG, "解压异常", e)
            null
        }
    }

    /**
     * 下载完成后，显示"点击安装"通知（PendingIntent）
     * Android 10+ 不允许从后台 Service 直接 startActivity，改用通知点击触发
     */
    private fun showInstallNotification(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("下载完成")
                .setContentText("点击安装新版本")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            RLog.e(TAG, "显示安装通知失败", e)
        }
    }

    private fun updateProgressNotification(progress: Float) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createProgressNotification(progress))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "APK 下载",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示 APK 下载进度"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createProgressNotification(progress: Float): Notification {
        val percent = (progress * 100).toInt()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在下载新版本")
            .setContentText("$percent%")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
