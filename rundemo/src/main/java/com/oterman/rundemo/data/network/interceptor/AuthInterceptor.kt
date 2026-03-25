package com.oterman.rundemo.data.network.interceptor

import com.google.gson.JsonParser
import com.oterman.rundemo.data.repository.TokenRefreshManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer

/**
 * 认证拦截器
 * 用于在请求头中添加token等认证信息
 *
 * 并发安全：发请求前先等待进行中的 Token 刷新完成，
 * 确保所有请求拿到最新 token 之后再发出。
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val tokenRefreshManagerProvider: (() -> TokenRefreshManager?)? = null
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // 刷新 Token 接口本身不等待，避免死锁
        val isRefreshTokenRequest = chain.request().url.encodedPath.contains("refreshToken")

        // 等待进行中的 Token 刷新完成，确保拿到最新 token
        if (!isRefreshTokenRequest) {
            val manager = tokenRefreshManagerProvider?.invoke()
            manager?.let { runBlocking { it.waitForOngoingRefresh() } }
        }

        val originalRequest = chain.request()
        val token = tokenProvider()

        val builder = originalRequest.newBuilder()
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
            // 同步更新 body 中的 head.token，防止 body 携带旧 token
            if (!isRefreshTokenRequest && originalRequest.body !is MultipartBody) {
                val newBody = rebuildBodyWithNewToken(originalRequest.body, token)
                if (newBody != null) {
                    builder.method(originalRequest.method, newBody)
                }
            }
        }
        // Don't set Content-Type for multipart requests — OkHttp derives it from the body
        if (originalRequest.body !is MultipartBody) {
            builder.header("Content-Type", "application/json")
        }
        val request = builder.build()

        return chain.proceed(request)
    }

    private fun rebuildBodyWithNewToken(originalBody: RequestBody?, newToken: String): RequestBody? {
        if (originalBody == null || newToken.isEmpty()) return null
        val buffer = Buffer()
        originalBody.writeTo(buffer)
        val bodyString = buffer.readUtf8()
        return try {
            val jsonObj = JsonParser.parseString(bodyString).asJsonObject
            jsonObj.getAsJsonObject("head")?.addProperty("token", newToken)
            jsonObj.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())
        } catch (e: Exception) {
            null  // 解析失败就不修改，保持原样
        }
    }
}
