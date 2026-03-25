package com.oterman.rundemo.service.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.oterman.rundemo.R
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

sealed class ApkDownloadState {
    object Idle : ApkDownloadState()
    data class Downloading(val progress: Float) : ApkDownloadState()
    data class Completed(val apkFile: File) : ApkDownloadState()
    data class Failed(val message: String) : ApkDownloadState()
}

/**
 * APK 下载前台服务
 */
class ApkDownloadService : Service() {

    companion object {
        private const val TAG = "ApkDownloadService"
        private const val CHANNEL_ID = "apk_download_channel"
        private const val NOTIFICATION_ID = 10002
        private const val EXTRA_DOWNLOAD_URL = "extra_download_url"
        private const val EXTRA_FILE_NAME = "extra_file_name"

        private val _downloadState = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Idle)
        val downloadState: StateFlow<ApkDownloadState> = _downloadState

        fun start(context: Context, downloadUrl: String, fileName: String = "yayarun_update.apk") {
            _downloadState.value = ApkDownloadState.Idle
            val intent = Intent(context, ApkDownloadService::class.java).apply {
                putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
                putExtra(EXTRA_FILE_NAME, fileName)
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

        startForeground(NOTIFICATION_ID, createNotification(0f))
        _downloadState.value = ApkDownloadState.Downloading(0f)

        serviceScope.launch {
            downloadApk(downloadUrl, fileName)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private suspend fun downloadApk(url: String, fileName: String) {
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
            val downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: cacheDir
            val apkFile = File(downloadsDir, fileName)

            body.byteStream().use { inputStream ->
                FileOutputStream(apkFile).use { outputStream ->
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
                            updateNotification(progress)
                        }
                    }
                }
            }

            RLog.i(TAG, "下载完成: ${apkFile.absolutePath}")
            _downloadState.value = ApkDownloadState.Completed(apkFile)
            triggerInstall(apkFile)
            stopSelf()

        } catch (e: Exception) {
            RLog.e(TAG, "下载异常", e)
            _downloadState.value = ApkDownloadState.Failed(e.message ?: "未知错误")
            stopSelf()
        }
    }

    private fun triggerInstall(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(installIntent)
        } catch (e: Exception) {
            RLog.e(TAG, "触发安装失败", e)
        }
    }

    private fun updateNotification(progress: Float) {
        val percent = (progress * 100).toInt()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在下载新版本")
            .setContentText("$percent%")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
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

    private fun createNotification(progress: Float): Notification {
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
