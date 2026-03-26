package com.oterman.rundemo.data.repository

import android.content.Context
import com.oterman.rundemo.BuildConfig
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.RequestBuilder
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.network.dto.request.GetLatestVersionRequest
import com.oterman.rundemo.data.network.dto.response.GetLatestVersionResponse
import com.oterman.rundemo.util.RLog
import java.io.File

data class AppUpdateInfo(
    val response: GetLatestVersionResponse,
    val isAlreadyDownloaded: Boolean,
    val localApkPath: String?
)

/**
 * 应用版本检测仓库
 */
object AppUpdateRepository {

    private const val TAG = "AppUpdateRepository"

    /**
     * 检查是否有新版本
     * @return Result<AppUpdateInfo?> — null 表示已是最新版，有值表示有新版本
     */
    suspend fun checkLatestVersion(context: Context): Result<AppUpdateInfo?> {
        return try {
            val request = RequestBuilder.createRequest(
                dtoName = "GetLatestVersionRequest",
                data = GetLatestVersionRequest(),
                token = "",
                userId = ""
            )
            val response = RetrofitClient.versionApi.getLatestVersion(request)
            if (response.isSuccess()) {
                val versionResponse = response.data
                    ?.getLatestVersionResponse
                    ?.firstOrNull()
                    ?.let { resp ->
                        val cleanUrl = resp.downloadUrl?.let { url ->
                            val secondIdx = url.indexOf("http", 1)
                            if (secondIdx > 0) url.substring(secondIdx) else url
                        }
                        resp.copy(downloadUrl = cleanUrl)
                    }
                RLog.d(TAG, "Latest version: ${versionResponse?.versionCode}, current: ${BuildConfig.VERSION_CODE}")
                if (versionResponse != null && (versionResponse.versionCode ?: 0) > BuildConfig.VERSION_CODE) {
                    val prefs = PreferencesManager(context)
                    val cachedVersionCode = prefs.getDownloadedApkVersionCode()
                    val cachedPath = prefs.getDownloadedApkPath()
                    val isAlreadyDownloaded = cachedVersionCode == versionResponse.versionCode &&
                            cachedPath != null && File(cachedPath).exists()
                    Result.success(
                        AppUpdateInfo(
                            response = versionResponse,
                            isAlreadyDownloaded = isAlreadyDownloaded,
                            localApkPath = if (isAlreadyDownloaded) cachedPath else null
                        )
                    )
                } else {
                    Result.success(null)
                }
            } else {
                Result.success(null)   // 业务层无可用版本，等同于"已是最新"
            }
        } catch (e: Exception) {
            RLog.e(TAG, "检查更新失败", e)
            Result.failure(e)
        }
    }
}
