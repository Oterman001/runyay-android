package com.oterman.rundemo.data.network.interceptor

import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.Response

/**
 * 认证拦截器
 * 用于在请求头中添加token等认证信息
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenProvider()
        
        val builder = originalRequest.newBuilder()
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }
        // Don't set Content-Type for multipart requests — OkHttp derives it from the body
        if (originalRequest.body !is MultipartBody) {
            builder.header("Content-Type", "application/json")
        }
        val request = builder.build()
        
        return chain.proceed(request)
    }
}

