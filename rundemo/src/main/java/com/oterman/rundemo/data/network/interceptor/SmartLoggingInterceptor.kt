package com.oterman.rundemo.data.network.interceptor

import com.oterman.rundemo.util.RLog
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * 智能日志拦截器
 * 根据 Content-Type 自动判断是否为二进制内容，
 * 非二进制正常打印 body，二进制只打印摘要信息。
 */
class SmartLoggingInterceptor : Interceptor {

    companion object {
        private const val TAG = "OkHttp"
        private val UTF8 = Charset.forName("UTF-8")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // --- 请求日志 ---
        RLog.d(TAG, "--> ${request.method} ${request.url}")
        logHeaders(request.headers)

        val requestBody = request.body
        if (requestBody != null) {
            requestBody.contentType()?.let {
                RLog.d(TAG, "Content-Type: $it")
            }
            when {
                requestBody is MultipartBody -> logMultipartBody(requestBody)
                isBinaryContent(requestBody.contentType()) -> {
                    RLog.d(TAG, "[binary ${requestBody.contentLength()} bytes]")
                }
                else -> {
                    val buffer = Buffer()
                    requestBody.writeTo(buffer)
                    RLog.d(TAG, buffer.readString(UTF8))
                }
            }
        }
        RLog.d(TAG, "--> END ${request.method}")

        // --- 响应日志 ---
        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            RLog.d(TAG, "<-- HTTP FAILED: $e")
            throw e
        }
        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body
        RLog.d(TAG, "<-- ${response.code} ${response.message} ${response.request.url} (${tookMs}ms)")
        logHeaders(response.headers)

        if (responseBody != null) {
            val contentType = responseBody.contentType()
            val contentLength = responseBody.contentLength()
            if (isBinaryContent(contentType)) {
                RLog.d(TAG, "[binary body omitted, ${contentLength} bytes]")
            } else {
                val peekBody = response.peekBody(1024 * 1024) // peek up to 1MB
                RLog.d(TAG, peekBody.string())
            }
        }
        RLog.d(TAG, "<-- END HTTP")

        return response
    }

    private fun logHeaders(headers: Headers) {
        for (i in 0 until headers.size) {
            RLog.d(TAG, "${headers.name(i)}: ${headers.value(i)}")
        }
    }

    private fun logMultipartBody(body: MultipartBody) {
        body.parts.forEachIndexed { index, part ->
            val partContentType = part.body.contentType()
            val partSize = part.body.contentLength()
            val disposition = part.headers?.get("Content-Disposition") ?: ""
            if (isBinaryContent(partContentType)) {
                RLog.d(TAG, "part[$index] $disposition [binary $partSize bytes]")
            } else {
                val buffer = Buffer()
                part.body.writeTo(buffer)
                RLog.d(TAG, "part[$index] $disposition: ${buffer.readString(UTF8)}")
            }
        }
    }

    private fun isBinaryContent(contentType: MediaType?): Boolean {
        if (contentType == null) return false
        return contentType.type in listOf("image", "audio", "video") ||
                contentType.subtype in listOf("octet-stream", "protobuf", "zip", "gzip")
    }
}
