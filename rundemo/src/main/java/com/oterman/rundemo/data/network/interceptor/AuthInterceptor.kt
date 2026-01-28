package com.oterman.rundemo.data.network.interceptor

import okhttp3.Interceptor
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
        
        val request = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
        } else {
            originalRequest.newBuilder()
                .header("Content-Type", "application/json")
                .build()
        }
        
        return chain.proceed(request)
    }
}

