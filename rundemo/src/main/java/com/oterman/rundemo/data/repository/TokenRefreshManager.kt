package com.oterman.rundemo.data.repository

import android.content.Context
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.RequestBuilder
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.network.api.UserApi
import com.oterman.rundemo.data.network.dto.request.TokenRefreshRequest
import com.oterman.rundemo.util.RLog
import com.oterman.rundemo.util.SecurityUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Token刷新管理器（单例）
 *
 * 解决iOS存在的两个并发问题：
 * 1. 刷新请求与其他请求并发 → 通过 Mutex + Deferred 确保刷新期间其他请求等待
 * 2. 刷新完成后短时间内仍有请求携带旧 token → AuthInterceptor 每次实时读取 tokenProvider()
 *
 * 约束：
 * - 每天最多刷新一次
 * - 登录当天不刷新
 * - 刷新成功才记录日期（失败下次启动重试）
 */
class TokenRefreshManager private constructor(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val userApi: UserApi
) {
    companion object {
        private const val TAG = "TokenRefreshManager"

        @Volatile
        private var instance: TokenRefreshManager? = null

        fun getInstance(context: Context): TokenRefreshManager {
            return instance ?: synchronized(this) {
                instance ?: TokenRefreshManager(
                    context = context.applicationContext,
                    preferencesManager = PreferencesManager(context.applicationContext),
                    userApi = RetrofitClient.userApi
                ).also { instance = it }
            }
        }
    }

    private val mutex = Mutex()

    @Volatile
    private var refreshDeferred: Deferred<Boolean>? = null

    /**
     * token过期强制登出事件
     * HomeViewModel 监听此 flow 触发退出登录导航
     */
    val tokenExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * 每日刷新入口（在 HomeViewModel.init 中调用）
     *
     * 跳过条件：
     * - 今天已经刷新过
     * - 今天刚登录（登录时已获得最新 token）
     * - 用户未登录
     */
    suspend fun performDailyTokenRefreshIfNeeded() {
        if (!preferencesManager.isUserLoggedIn()) {
            RLog.d(TAG, "用户未登录，跳过每日刷新")
            return
        }

        val today = LocalDate.now().toString()

        val lastRefreshDate = preferencesManager.getLastDailyRefreshDate()
        if (lastRefreshDate == today) {
            RLog.d(TAG, "今日已刷新过 Token，跳过 ($today)")
            return
        }

        val lastLoginDate = preferencesManager.getLastLoginDate()
        if (lastLoginDate == today) {
            RLog.d(TAG, "今日登录，Token 为最新，跳过每日刷新")
            return
        }

        RLog.i(TAG, "触发每日 Token 刷新")
        val success = refreshTokenIfNeeded()
        if (success) {
            preferencesManager.saveLastDailyRefreshDate(today)
            RLog.i(TAG, "每日 Token 刷新成功，已记录日期: $today")
        } else {
            RLog.w(TAG, "每日 Token 刷新失败，下次启动将重试")
        }
    }

    /**
     * 并发安全的 Token 刷新核心逻辑
     *
     * - 若已有刷新 Deferred 正在进行 → 直接 await 复用结果（不重复发请求）
     * - 否则通过 Mutex 创建新 Deferred 执行刷新
     */
    suspend fun refreshTokenIfNeeded(): Boolean {
        refreshDeferred?.let { return it.await() }
        return mutex.withLock {
            // double-check inside lock
            refreshDeferred?.let { return it.await() }

            val deferred = CompletableDeferred<Boolean>()
            refreshDeferred = deferred
            try {
                val result = callRefreshApi()
                deferred.complete(result)
                result
            } catch (e: Exception) {
                RLog.e(TAG, "Token 刷新异常: ${e.message}")
                deferred.complete(false)
                false
            } finally {
                refreshDeferred = null
            }
        }
    }

    /**
     * 等待正在进行的刷新完成（供 AuthInterceptor 调用）
     * 若无刷新进行中则立即返回
     */
    suspend fun waitForOngoingRefresh() {
        refreshDeferred?.await()
    }

    /**
     * 调用刷新 Token API
     */
    private suspend fun callRefreshApi(): Boolean {
        return try {
            val deviceId = SecurityUtils.getDeviceId(context)
            val requestDto = TokenRefreshRequest(deviceId = deviceId)
            val request = RequestBuilder.createRequest(
                dtoName = "TokenRefreshRequestDto",
                data = requestDto,
                preferencesManager = preferencesManager
            )

            val response = userApi.refreshToken(request)

            if (response.isSuccess()) {
                val refreshResponse = response.data?.tokenRefreshResponseDto?.firstOrNull()
                if (refreshResponse != null && refreshResponse.isSuccess) {
                    val newToken = refreshResponse.token!!
                    preferencesManager.saveUserToken(newToken)
                    refreshResponse.expireDays?.let { preferencesManager.saveTokenExpireDate(it) }
                    RLog.i(TAG, "Token 刷新成功")
                    true
                } else {
                    RLog.w(TAG, "Token 刷新返回空 token，视为 token 已过期")
                    handleTokenExpired()
                    false
                }
            } else {
                RLog.w(TAG, "Token 刷新 API 失败: ${response.msg}")
                if (response.code == "0022" || preferencesManager.isTokenExpired()) {
                    handleTokenExpired()
                }
                false
            }
        } catch (e: Exception) {
            RLog.e(TAG, "Token 刷新请求异常: ${e.message}")
            false
        }
    }

    /**
     * 处理 token 真正过期的情况：清除本地数据并发送过期事件
     */
    private fun handleTokenExpired() {
        RLog.w(TAG, "Token 已过期，清除用户数据并触发退出登录")
        preferencesManager.clearUserData()
        tokenExpiredEvent.tryEmit(Unit)
    }
}
