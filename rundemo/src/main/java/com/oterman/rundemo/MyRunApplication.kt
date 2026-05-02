package com.oterman.rundemo

import android.app.Application
import android.content.Context
import coil.Coil
import com.oterman.rundemo.data.local.PreferencesManager
import java.io.File
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.oterman.rundemo.BuildConfig
import com.oterman.rundemo.util.RLog
import com.tencent.bugly.crashreport.CrashReport
import com.umeng.commonsdk.UMConfigure

class MyRunApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        RLog.i("MyRunApplication","onCreate")

        initCoilImageLoader()

        /**
         * 注意: 即使您已经在AndroidManifest.xml中配置过appkey和channel值，也需要在App代码中调
         * 用初始化接口（如需要使用AndroidManifest.xml中配置好的appkey和channel值，
         * UMConfigure.init调用中appkey和channel参数请置为null）。
         */
        UMConfigure.preInit(this,"69a930fe6f259537c76ddab0", BuildConfig.UMENG_CHANNEL)

        // 已同意隐私政策的后续冷启动，立即完成初始化；首次启动由 PrivacyConsentScreen 在用户同意后调用
        if (PreferencesManager(this).isPrivacyConsentAccepted()) {
            initPrivacyRequiredSDKs(this)
        }

        cleanupOldDownloadedApk()
    }

    companion object {
        /**
         * 需隐私同意后方可调用，由 Application.onCreate（已同意）和 PrivacyConsentScreen（首次同意）分别触发。
         */
        fun initPrivacyRequiredSDKs(context: Context) {
            UMConfigure.init(context, "69a930fe6f259537c76ddab0", BuildConfig.UMENG_CHANNEL, UMConfigure.DEVICE_TYPE_PHONE, "")
            CrashReport.initCrashReport(context.applicationContext, "ea1ea72caa", BuildConfig.DEBUG)
            CrashReport.setAppChannel(context, BuildConfig.UMENG_CHANNEL)
        }
    }

    /**
     * 若当前版本已 >= 已缓存的下载版本，说明更新已完成，清理旧 APK 文件和缓存记录
     */
    private fun cleanupOldDownloadedApk() {
        val prefs = PreferencesManager(this)
        val storedVersionCode = prefs.getDownloadedApkVersionCode()
        if (storedVersionCode > 0 && BuildConfig.VERSION_CODE >= storedVersionCode) {
            prefs.getDownloadedApkPath()?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
            prefs.clearDownloadedApkInfo()
        }
    }

    private fun initCoilImageLoader() {
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("shoe_images"))
                    .maxSizeBytes(50 * 1024 * 1024L)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        Coil.setImageLoader(imageLoader)
    }
}