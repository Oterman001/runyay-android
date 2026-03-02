package com.oterman.rundemo.service.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.oterman.rundemo.R
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 数据同步前台服务
 * 保持进程活跃，在通知栏显示同步状态
 */
class DataSyncForegroundService : Service() {

    companion object {
        private const val TAG = "DataSyncForegroundService"
        private const val CHANNEL_ID = "data_sync_channel"
        private const val NOTIFICATION_ID = 10001
        private const val EXTRA_PLATFORM = "extra_platform"

        /**
         * 启动统一同步服务（所有已授权平台，如果当前未在同步中）
         */
        fun start(context: Context) {
            val manager = UnifiedDataSyncManager.getInstance(context)
            if (manager.isAnySyncing()) {
                RLog.d(TAG, "已在同步中，不重复启动服务")
                return
            }

            val intent = Intent(context, DataSyncForegroundService::class.java)
            startServiceCompat(context, intent)
        }

        /**
         * 启动单平台手动同步服务（如果当前未在同步中）
         */
        fun startForPlatform(context: Context, platform: DataSourcePlatform) {
            val manager = UnifiedDataSyncManager.getInstance(context)
            if (manager.isAnySyncing()) {
                RLog.d(TAG, "已在同步中，不重复启动服务")
                return
            }

            val intent = Intent(context, DataSyncForegroundService::class.java).apply {
                putExtra(EXTRA_PLATFORM, platform.code)
            }
            startServiceCompat(context, intent)
        }

        private fun startServiceCompat(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val platformCode = intent?.getStringExtra(EXTRA_PLATFORM)
        val platform = platformCode?.let { DataSourcePlatform.fromCode(it) }

        RLog.i(TAG, "服务启动, platform=${platform?.displayName ?: "统一同步"}")

        startForeground(NOTIFICATION_ID, createNotification(platform))

        val manager = UnifiedDataSyncManager.getInstance(applicationContext)
        if (platform != null) {
            manager.launchManualSync(platform)
        } else {
//            manager.launchUnifiedSync()
        }

        // 观察同步状态，同步结束后停止服务
        serviceScope.launch {
            manager.syncUiState.collect { state ->
                when (state) {
                    is SyncUiState.Idle -> {
                        // 初始也是Idle，等待进入Syncing后再响应Completed
                    }
                    is SyncUiState.Syncing -> {
                        // 正在同步，保持服务运行
                    }
                    is SyncUiState.Completed -> {
                        RLog.i(TAG, "同步完成，准备停止服务")
                        stopSelf()
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        RLog.i(TAG, "服务销毁")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "数据同步",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示数据同步进度"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(platform: DataSourcePlatform? = null): Notification {
        val title = if (platform != null) {
            "正在同步 ${platform.displayName} 数据..."
        } else {
            "正在同步跑步数据..."
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
