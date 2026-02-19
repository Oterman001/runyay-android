package com.oterman.rundemo.data.repository

import android.content.Context
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.RequestBuilder
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.network.api.UserApi
import com.oterman.rundemo.data.network.dto.request.GetAvatarUrlRequest
import com.oterman.rundemo.util.RLog

/**
 * 头像URL统一管理器
 * 负责头像临时URL的请求、缓存（内存+磁盘）、过期自动刷新
 */
class AvatarManager private constructor(
    private val context: Context
) {
    private val userApi: UserApi = RetrofitClient.userApi
    private val preferencesManager = PreferencesManager(context)

    // 内存缓存
    @Volatile
    private var cachedUrl: String? = null
    @Volatile
    private var cacheExpiration: Long = 0L

    companion object {
        private const val TAG = "AvatarManager"
        private const val EARLY_REFRESH_MS = 5 * 60 * 1000L // 提前5分钟刷新

        @Volatile
        private var instance: AvatarManager? = null

        fun getInstance(context: Context): AvatarManager {
            return instance ?: synchronized(this) {
                instance ?: AvatarManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 获取头像URL，优先使用缓存
     * @param userId 用户ID
     * @param forceRefresh 强制刷新（忽略缓存）
     */
    suspend fun getAvatarUrl(userId: String, forceRefresh: Boolean = false): Result<String?> {
        if (!forceRefresh) {
            // 1. 内存缓存命中
            cachedUrl?.let { url ->
                if (isCacheValid(cacheExpiration)) {
                    RLog.d(TAG, "内存缓存命中: $url")
                    return Result.success(url)
                }
            }

            // 2. 磁盘缓存命中
            val diskUrl = preferencesManager.getCachedAvatarUrl()
            val diskExpiration = preferencesManager.getCachedAvatarExpiration()
            if (diskUrl != null && isCacheValid(diskExpiration)) {
                RLog.d(TAG, "磁盘缓存命中: $diskUrl")
                cachedUrl = diskUrl
                cacheExpiration = diskExpiration
                return Result.success(diskUrl)
            }
        }

        // 3. 缓存失效，调用 API 获取
        return fetchAndCache(userId)
    }

    private fun isCacheValid(expiration: Long): Boolean {
        return System.currentTimeMillis() < (expiration - EARLY_REFRESH_MS)
    }

    private suspend fun fetchAndCache(userId: String): Result<String?> {
        return try {
            RLog.d(TAG, "从API获取头像URL: userId=$userId")

            val requestDto = GetAvatarUrlRequest(userId = userId)
            val request = RequestBuilder.createRequest(
                dtoName = "GetAvatarUrlRequestDto",
                data = requestDto,
                preferencesManager = preferencesManager
            )

            val response = userApi.getAvatarUrl(request)

            if (response.isSuccess()) {
                val avatarData = response.data?.avatarUrlResponseDto?.firstOrNull()
                val url = avatarData?.avatarUrl
                val expiration = avatarData?.expirationTime ?: 0L

                if (url != null && expiration > 0) {
                    // 更新内存缓存
                    cachedUrl = url
                    cacheExpiration = expiration
                    // 更新磁盘缓存
                    preferencesManager.saveCachedAvatarUrl(url, expiration)
                    RLog.d(TAG, "头像URL已缓存, 过期时间: $expiration")
                }

                Result.success(url)
            } else {
                RLog.e(TAG, "获取头像URL失败: ${response.msg}")
                Result.failure(Exception(response.msg ?: "获取头像URL失败"))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "获取头像URL异常: ${e.message}")
            Result.failure(e)
        }
    }

    fun clearCache() {
        cachedUrl = null
        cacheExpiration = 0L
        preferencesManager.clearCachedAvatar()
        RLog.d(TAG, "头像缓存已清除")
    }
}
