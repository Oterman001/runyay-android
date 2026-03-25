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
    private val ossImageCache = OssImageCache(context, "oss_cache_avatar", 24 * 60 * 60 * 1000L)

    companion object {
        private const val TAG = "AvatarManager"

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
            ossImageCache.getCachedUrl(userId)?.let { url ->
                RLog.d(TAG, "缓存命中: $url")
                return Result.success(url)
            }
        }

        // 缓存失效，调用 API 获取
        return fetchAndCache(userId)
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

                if (url != null) {
                    ossImageCache.setCachedUrl(userId, url)
                    RLog.d(TAG, "头像URL已缓存: $url")
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
        ossImageCache.clearAll()
        RLog.d(TAG, "头像缓存已清除")
    }
}
