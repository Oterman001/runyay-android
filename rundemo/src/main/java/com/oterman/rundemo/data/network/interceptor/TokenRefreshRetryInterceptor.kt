package com.oterman.rundemo.data.network.interceptor

import com.google.gson.JsonParser
import com.oterman.rundemo.data.repository.TokenRefreshManager
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Token 无效自动刷新重试拦截器
 *
 * 服务端返回 code=0022 表示当前 token 存在问题，此时：
 * 1. 触发一次 Token 刷新（复用 TokenRefreshManager 的并发安全机制）
 * 2. 刷新成功后，使用新 token 重试原始请求
 * 3. 刷新失败，返回原始 0022 响应，由上层业务处理
 *
 * 并发安全：多个请求同时收到 0022 时，均调用 refreshTokenIfNeeded()，
 * TokenRefreshManager 的 Mutex + CompletableDeferred 确保只发一次刷新请求，
 * 其余请求 await 同一结果。
 *
 * 拦截器链位置：应置于 AuthInterceptor 之前，以便重试时 AuthInterceptor
 * 能自动将新 token 注入 header 和 body。
 */
class TokenRefreshRetryInterceptor(
    private val tokenRefreshManagerProvider: () -> TokenRefreshManager?
) : Interceptor {

    companion object {
        private const val TAG = "TokenRefreshRetry"
        private const val CODE_TOKEN_INVALID = "0022"
        private const val PEEK_SIZE = 512L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // refreshToken 接口自身不处理，避免形成刷新→刷新的死锁
        if (request.url.encodedPath.contains("refreshToken")) {
            return chain.proceed(request)
        }

        val response = chain.proceed(request)

        if (!isTokenInvalidResponse(response)) {
            return response
        }

        RLog.w(TAG, "收到 0022 响应，触发 Token 刷新: ${request.url}")

        val manager = tokenRefreshManagerProvider()
        if (manager == null) {
            RLog.w(TAG, "TokenRefreshManager 未初始化，返回原始响应")
            return response
        }

        val refreshSuccess = runBlocking { manager.refreshTokenIfNeeded() }

        if (!refreshSuccess) {
            RLog.w(TAG, "Token 刷新失败，返回原始 0022 响应")
            return response
        }

        // 关闭旧响应，用新 token 重试（AuthInterceptor 会自动注入新 token）
        response.close()
        RLog.i(TAG, "Token 刷新成功，重试请求: ${request.url}")
        return chain.proceed(request)
    }

    private fun isTokenInvalidResponse(response: Response): Boolean {
        return try {
            val bodyString = response.peekBody(PEEK_SIZE).string()
            if (!bodyString.contains(CODE_TOKEN_INVALID)) return false
            val json = JsonParser.parseString(bodyString).asJsonObject
            json.get("code")?.asString == CODE_TOKEN_INVALID
        } catch (e: Exception) {
            false
        }
    }
}
