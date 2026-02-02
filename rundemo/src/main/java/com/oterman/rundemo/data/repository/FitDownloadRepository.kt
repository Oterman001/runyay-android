package com.oterman.rundemo.data.repository

import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.RequestBuilder
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.network.api.DataSourceApi
import com.oterman.rundemo.data.network.dto.request.FitFileDetailRequest
import com.oterman.rundemo.data.network.dto.response.FitFileDetailResponse
import com.oterman.rundemo.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

/**
 * FIT文件下载仓库
 * 负责获取FIT文件下载URL并下载文件
 */
class FitDownloadRepository(
    private val preferencesManager: PreferencesManager,
    private val api: DataSourceApi = RetrofitClient.dataSourceApi
) {
    companion object {
        private const val TAG = "FitDownloadRepository"
    }

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * 获取FIT文件下载URL
     */
    suspend fun getFitFileUrls(
        summaryId: String,
        platformCode: String
    ): Result<FitFileDetailResponse> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId()
                ?: return@withContext Result.failure(Exception("用户未登录"))

            Logger.d(TAG, "获取FIT文件URL: summaryId=$summaryId, platformCode=$platformCode")

            val request = RequestBuilder.createRequest(
                dtoName = "FitFileDetailRequest",
                data = FitFileDetailRequest(
                    userId = userId,
                    summaryId = summaryId,
                    platformCode = platformCode
                ),
                preferencesManager = preferencesManager
            )

            val response = api.getFitFileDetail(request)

            if (response.isSuccess()) {
                val fitDetail = response.data?.fitFileDetailResponse?.firstOrNull()
                if (fitDetail != null) {
                    Logger.i(TAG, "获取FIT文件URL成功: ossUrl=${fitDetail.ossUrl}, fitUrl=${fitDetail.fitUrl}")
                    Result.success(fitDetail)
                } else {
                    Logger.w(TAG, "未找到FIT文件信息")
                    Result.failure(Exception("未找到FIT文件信息"))
                }
            } else {
                Logger.w(TAG, "获取FIT信息失败: ${response.msg}")
                Result.failure(Exception(response.msg ?: "获取FIT信息失败"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "获取FIT文件URL异常", e)
            Result.failure(e)
        }
    }

    /**
     * 下载FIT文件数据
     * 优先使用ossUrl（需解压），失败则降级到fitUrl
     */
    suspend fun downloadFitData(urlInfo: FitFileDetailResponse): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            // 1. 优先尝试ossUrl（通常是压缩文件）
            if (!urlInfo.ossUrl.isNullOrEmpty()) {
                Logger.d(TAG, "尝试从ossUrl下载: ${urlInfo.ossUrl}")
                val ossResult = downloadAndDecompress(urlInfo.ossUrl)
                if (ossResult.isSuccess) {
                    Logger.i(TAG, "ossUrl下载成功")
                    return@withContext ossResult
                }
                Logger.w(TAG, "ossUrl下载失败，尝试fitUrl")
            }

            // 2. 降级使用fitUrl
            Logger.d(TAG, "从fitUrl下载: ${urlInfo.fitUrl}")
            downloadFromUrl(urlInfo.fitUrl)
        }

    /**
     * 从URL下载并解压
     */
    private fun downloadAndDecompress(url: String): Result<ByteArray> {
        return try {
            val request = Request.Builder().url(url).build()
            val response = downloadClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return Result.failure(Exception("下载失败: ${response.code}"))
            }

            val data = response.body?.bytes() ?: return Result.failure(Exception("响应体为空"))

            // 尝试解压（根据文件头判断格式）
            val decompressed = when {
                isGzip(data) -> {
                    Logger.d(TAG, "检测到GZIP格式，开始解压")
                    decompressGzip(data)
                }
                isZip(data) -> {
                    Logger.d(TAG, "检测到ZIP格式，开始解压")
                    decompressZip(data)
                }
                else -> {
                    Logger.d(TAG, "非压缩格式，直接使用")
                    data // 可能已经是FIT文件
                }
            }

            Result.success(decompressed)
        } catch (e: Exception) {
            Logger.e(TAG, "下载并解压失败", e)
            Result.failure(e)
        }
    }

    /**
     * 直接从URL下载
     */
    private fun downloadFromUrl(url: String): Result<ByteArray> {
        return try {
            val request = Request.Builder().url(url).build()
            val response = downloadClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return Result.failure(Exception("下载失败: ${response.code}"))
            }

            val data = response.body?.bytes() ?: return Result.failure(Exception("响应体为空"))
            Logger.i(TAG, "fitUrl下载成功，大小: ${data.size} bytes")
            Result.success(data)
        } catch (e: Exception) {
            Logger.e(TAG, "URL下载异常", e)
            Result.failure(e)
        }
    }

    private fun isGzip(data: ByteArray): Boolean {
        return data.size >= 2 && data[0] == 0x1f.toByte() && data[1] == 0x8b.toByte()
    }

    private fun isZip(data: ByteArray): Boolean {
        return data.size >= 4 && data[0] == 0x50.toByte() && data[1] == 0x4b.toByte()
    }

    private fun decompressGzip(data: ByteArray): ByteArray {
        return GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
    }

    private fun decompressZip(data: ByteArray): ByteArray {
        ZipInputStream(ByteArrayInputStream(data)).use { zis ->
            val entry = zis.nextEntry
            if (entry != null) {
                val output = ByteArrayOutputStream()
                zis.copyTo(output)
                return output.toByteArray()
            }
        }
        return data
    }
}
