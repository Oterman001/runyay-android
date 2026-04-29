package com.oterman.rundemo.data.network.interceptor

import com.google.gson.JsonParser
import com.oterman.rundemo.data.repository.TokenRefreshManager
import com.oterman.rundemo.util.RLog
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Token 失效拦截器
 *
 * 服务端返回以下 code 时，直接触发登出跳转登录：
 * 0002, 0006, 0007, 0020, 0021, 0022, 002B
 *
 * 不再尝试刷新 token，直接清除用户数据并 emit tokenExpiredEvent，
 * NavGraph 中的 LaunchedEffect 收到事件后跳转登录界面。
 *
 * 拦截器链位置：应置于 AuthInterceptor 之前。
 */
class TokenRefreshRetryInterceptor(
    private val tokenRefreshManagerProvider: () -> TokenRefreshManager?
) : Interceptor {

    companion object {
        private const val TAG = "TokenRefreshRetry"
        private val CODES_FORCE_LOGOUT = setOf(
            "0002", "0006", "0007",
            "0020", "0021", "0022",
            "002B"
        )
        private const val PEEK_SIZE = 512L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val code = extractBizCode(response) ?: return response
        if (code !in CODES_FORCE_LOGOUT) return response

        RLog.w(TAG, "收到 $code 响应，触发登出: ${request.url}")

        val manager = tokenRefreshManagerProvider()
        if (manager == null) {
            RLog.w(TAG, "TokenRefreshManager 未初始化，无法触发登出")
            return response
        }

        manager.handleTokenExpired()

        return response
    }

    private fun extractBizCode(response: Response): String? {
        return try {
            val bodyString = response.peekBody(PEEK_SIZE).string()
            val json = JsonParser.parseString(bodyString).asJsonObject
            json.get("code")?.asString
        } catch (e: Exception) {
            null
        }
    }
}
