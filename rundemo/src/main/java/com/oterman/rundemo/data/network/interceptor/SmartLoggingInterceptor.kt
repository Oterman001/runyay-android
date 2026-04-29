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
import java.util.concurrent.atomic.AtomicInteger

/**
 * 智能日志拦截器
 * 根据 Content-Type 自动判断是否为二进制内容，
 * 非二进制正常打印 body，二进制只打印摘要信息。
 * 每个请求分配唯一 ID（#001 格式），并发时日志不会交叉混乱。
 */
class SmartLoggingInterceptor : Interceptor {

    companion object {
        private const val TAG = "OkHttp"
        private val UTF8 = Charset.forName("UTF-8")
        private val requestCounter = AtomicInteger(0)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val reqId = "#%03d".format(requestCounter.getAndIncrement())
        val request = chain.request()

        // --- 请求日志 ---
        RLog.d(TAG, "$reqId --> ${request.method} ${request.url}")
        logHeaders(reqId, request.headers)

        val requestBody = request.body
        if (requestBody != null) {
            requestBody.contentType()?.let {
                RLog.d(TAG, "$reqId Content-Type: $it")
            }
            when {
                requestBody is MultipartBody -> logMultipartBody(reqId, requestBody)
                isBinaryContent(requestBody.contentType()) -> {
                    RLog.d(TAG, "$reqId [binary ${requestBody.contentLength()} bytes]")
                }
                else -> {
                    val buffer = Buffer()
                    requestBody.writeTo(buffer)
                    RLog.d(TAG, "$reqId ${buffer.readString(UTF8)}")
                }
            }
        }
        RLog.d(TAG, "$reqId --> END ${request.method}")

        // --- 响应日志 ---
        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            RLog.d(TAG, "$reqId <-- HTTP FAILED: $e")
            throw e
        }
        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body
        RLog.d(TAG, "$reqId <-- ${response.code} ${response.message} ${response.request.url} (${tookMs}ms)")
        logHeaders(reqId, response.headers)

        if (responseBody != null) {
            val contentType = responseBody.contentType()
            val contentLength = responseBody.contentLength()
            if (isBinaryContent(contentType)) {
                RLog.d(TAG, "$reqId [binary body omitted, ${contentLength} bytes]")
            } else {
                val peekBody = response.peekBody(1024 * 1024) // peek up to 1MB
                RLog.d(TAG, "$reqId ${peekBody.string()}")
            }
        }
        RLog.d(TAG, "$reqId <-- END HTTP")

        return response
    }

    private fun logHeaders(reqId: String, headers: Headers) {
        for (i in 0 until headers.size) {
            RLog.d(TAG, "$reqId ${headers.name(i)}: ${headers.value(i)}")
        }
    }

    private fun logMultipartBody(reqId: String, body: MultipartBody) {
        body.parts.forEachIndexed { index, part ->
            val partContentType = part.body.contentType()
            val partSize = part.body.contentLength()
            val disposition = part.headers?.get("Content-Disposition") ?: ""
            if (isBinaryContent(partContentType)) {
                RLog.d(TAG, "$reqId part[$index] $disposition [binary $partSize bytes]")
            } else {
                val buffer = Buffer()
                part.body.writeTo(buffer)
                RLog.d(TAG, "$reqId part[$index] $disposition: ${buffer.readString(UTF8)}")
            }
        }
    }

    private fun isBinaryContent(contentType: MediaType?): Boolean {
        if (contentType == null) return false
        return contentType.type in listOf("image", "audio", "video") ||
                contentType.subtype in listOf("octet-stream", "protobuf", "zip", "gzip")
    }
}
