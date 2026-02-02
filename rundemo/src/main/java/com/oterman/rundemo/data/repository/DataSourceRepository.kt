package com.oterman.rundemo.data.repository

import android.util.Log
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.RequestBuilder
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.network.api.DataSourceApi
import com.oterman.rundemo.data.network.dto.request.BackfillRequest
import com.oterman.rundemo.data.network.dto.request.CorosCallbackRequest
import com.oterman.rundemo.data.network.dto.request.CorosSportDetailRequest
import com.oterman.rundemo.data.network.dto.request.FileDownloadRequest
import com.oterman.rundemo.data.network.dto.request.FileListRequest
import com.oterman.rundemo.data.network.dto.request.GarminCallbackRequest
import com.oterman.rundemo.data.network.dto.request.PlatformBindRequest
import com.oterman.rundemo.data.network.dto.request.PlatformStatusRequest
import com.oterman.rundemo.data.network.dto.request.PlatformUnbindRequest
import com.oterman.rundemo.service.sync.model.SyncConstants
import com.oterman.rundemo.util.TimestampUtils
import com.oterman.rundemo.data.network.dto.response.FileInfoDto
import com.oterman.rundemo.data.network.dto.response.PlatformStatusResponse
import com.oterman.rundemo.domain.model.DataSourceInfo
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.FileInfo
import com.oterman.rundemo.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 数据源仓库
 * 处理数据源授权、解绑和数据同步相关的业务逻辑
 */
class DataSourceRepository(
    private val dataSourcePreferences: DataSourcePreferences,
    private val preferencesManager: PreferencesManager,
    private val api: DataSourceApi = RetrofitClient.dataSourceApi
) {
    companion object {
        private const val TAG = "DataSourceRepository"
    }
    
    // ============ 平台状态 ============
    
    /**
     * 查询所有平台的绑定状态
     */
    suspend fun queryPlatformStatus(): Result<List<PlatformStatusResponse>> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId() 
                ?: return@withContext Result.failure(Exception("用户未登录"))
            
            Logger.i(TAG, "开始查询平台绑定状态, userId=$userId")
            
            val request = createBaseRequest(
                dtoName = "PlatformStatusRequest",
                data = PlatformStatusRequest(userId = userId)
            )
            
            val response = api.queryPlatformStatus(request)
            
            Logger.d(TAG, "平台状态响应: code=${response.code}, msg=${response.msg}")
            Logger.d(TAG, "响应data是否为空: ${response.data == null}")
            Logger.d(TAG, "platformStatusResponseDto是否为空: ${response.data?.platformStatusResponseDto == null}")
            
            if (response.isSuccess()) {
                val statuses = response.data?.platformStatusResponseDto ?: emptyList()
                
                Logger.i(TAG, "解析到平台状态数量: ${statuses.size}")
                
                // 更新本地缓存
                val statusMap = mutableMapOf<DataSourcePlatform, Boolean>()
                statuses.forEach { status ->
                    Logger.d(TAG, "平台: ${status.platformCode}, bindStatusCode=${status.bindStatusCode}, bindStatus=${status.bindStatus}, isBound=${status.isBound}")
                    DataSourcePlatform.fromCode(status.platformCode)?.let { platform ->
                        statusMap[platform] = status.isBound
                        Logger.i(TAG, "更新缓存: ${platform.displayName} -> ${if (status.isBound) "已授权" else "未授权"}")
                    }
                }
                dataSourcePreferences.updatePlatformStatus(statusMap)
                
                Logger.i(TAG, "平台状态查询成功, 共更新${statusMap.size}个平台状态")
                Result.success(statuses)
            } else {
                Logger.w(TAG, "平台状态查询失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "查询平台状态异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取本地缓存的平台绑定状态
     */
    fun isPlatformBound(platform: DataSourcePlatform): Boolean {
        return dataSourcePreferences.isPlatformBound(platform)
    }
    
    // ============ 绑定/授权 ============
    
    /**
     * 获取授权URL
     */
    suspend fun getBindUrl(platform: DataSourcePlatform): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId()
                ?: return@withContext Result.failure(Exception("用户未登录"))

            Logger.i(TAG, "开始获取授权URL, platform=${platform.code}, userId=$userId")

            val dtoName = when (platform) {
                DataSourcePlatform.GARMIN_CHINA, DataSourcePlatform.GARMIN_GLOBAL -> "GarminBindRequest"
                DataSourcePlatform.COROS -> "CorosBindRequest"
                else -> return@withContext Result.failure(Exception("不支持的平台"))
            }

            val request = createBaseRequest(
                dtoName = dtoName,
                data = PlatformBindRequest(userId = userId)
            )

            Logger.d(TAG, "发送绑定请求, dtoName=$dtoName")

            val response = when (platform) {
                DataSourcePlatform.GARMIN_CHINA -> api.bindGarminChina(request)
                DataSourcePlatform.GARMIN_GLOBAL -> api.bindGarminGlobal(request)
                DataSourcePlatform.COROS -> api.bindCoros(request)
                else -> return@withContext Result.failure(Exception("不支持的平台"))
            }

            Logger.d(TAG, "收到绑定响应, code=${response.code}, msg=${response.msg}")

            if (response.isSuccess()) {
                // 根据平台解析不同的响应字段
                val authUrl = when (platform) {
                    DataSourcePlatform.GARMIN_CHINA, DataSourcePlatform.GARMIN_GLOBAL -> {
                        val url = response.data?.garminBindResponse?.firstOrNull()?.redirectUrl
                        Logger.d(TAG, "佳明授权URL: $url")
                        url
                    }
                    DataSourcePlatform.COROS -> {
                        val url = response.data?.corosBindResponse?.firstOrNull()?.authUrl
                        Logger.d(TAG, "高驰授权URL: $url")
                        url
                    }
                    else -> null
                }

                if (authUrl != null) {
                    Logger.i(TAG, "获取授权URL成功, platform=${platform.code}")
                    Result.success(authUrl)
                } else {
                    Logger.w(TAG, "授权URL为空, platform=${platform.code}")
                    Result.failure(Exception("授权URL为空"))
                }
            } else {
                Logger.w(TAG, "获取授权URL失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "获取授权URL异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 处理佳明OAuth回调（OAuth 1.0a）
     * @param platform 平台（GARMIN_CHINA或GARMIN_GLOBAL）
     * @param oauthToken OAuth令牌
     * @param oauthVerifier OAuth验证码
     */
    suspend fun handleGarminOAuthCallback(
        platform: DataSourcePlatform,
        oauthToken: String,
        oauthVerifier: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId()
                ?: return@withContext Result.failure(Exception("用户未登录"))

            Logger.i(TAG, "处理佳明OAuth回调: platform=${platform.code}, token=$oauthToken, verifier=$oauthVerifier")

            val request = createBaseRequest(
                dtoName = "GarminCallBackDto",  // 与iOS保持一致
                data = GarminCallbackRequest(
                    oauthToken = oauthToken,
                    oauthVerifier = oauthVerifier,
                    userId = userId
                )
            )

            Logger.d(TAG, "发送佳明回调请求")

            val response = when (platform) {
                DataSourcePlatform.GARMIN_CHINA -> api.callbackGarminChina(request)
                DataSourcePlatform.GARMIN_GLOBAL -> api.callbackGarminGlobal(request)
                else -> return@withContext Result.failure(Exception("不支持的平台"))
            }

            Logger.d(TAG, "佳明回调响应: code=${response.code}, msg=${response.msg}")

            if (response.isSuccess()) {
                dataSourcePreferences.setPlatformBound(platform, true)
                Logger.i(TAG, "佳明授权成功，已更新本地绑定状态")
                Result.success(true)
            } else {
                Logger.w(TAG, "佳明回调失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "处理佳明OAuth回调异常", e)
            Result.failure(e)
        }
    }

    /**
     * 处理高驰OAuth回调（OAuth 2.0）
     * @param code 授权码
     * @param state 状态参数
     */
    suspend fun handleCorosOAuthCallback(
        code: String,
        state: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId()
                ?: return@withContext Result.failure(Exception("用户未登录"))

            Logger.i(TAG, "处理高驰OAuth回调: code=$code, state=$state")

            val request = createBaseRequest(
                dtoName = "CorosCallbackRequest",  // 与iOS保持一致
                data = CorosCallbackRequest(
                    code = code,
                    state = state,
                    userId = userId
                )
            )

            Logger.d(TAG, "发送高驰回调请求")

            val response = api.callbackCoros(request)

            Logger.d(TAG, "高驰回调响应: code=${response.code}, msg=${response.msg}")

            if (response.isSuccess()) {
                dataSourcePreferences.setPlatformBound(DataSourcePlatform.COROS, true)
                Logger.i(TAG, "高驰授权成功，已更新本地绑定状态")
                Result.success(true)
            } else {
                Logger.w(TAG, "高驰回调失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "处理高驰OAuth回调异常", e)
            Result.failure(e)
        }
    }
    
    // ============ 解绑 ============
    
    /**
     * 解绑平台
     */
    suspend fun unbindPlatform(platform: DataSourcePlatform): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId() 
                ?: return@withContext Result.failure(Exception("用户未登录"))
            
            val request = createBaseRequest(
                dtoName = when (platform) {
                    DataSourcePlatform.GARMIN_CHINA, DataSourcePlatform.GARMIN_GLOBAL -> "GarminUnbindRequest"
                    DataSourcePlatform.COROS -> "CorosUnbindRequest"
                    else -> return@withContext Result.failure(Exception("不支持的平台"))
                },
                data = PlatformUnbindRequest(userId = userId)
            )
            
            val response = when (platform) {
                DataSourcePlatform.GARMIN_CHINA -> api.unbindGarminChina(request)
                DataSourcePlatform.GARMIN_GLOBAL -> api.unbindGarminGlobal(request)
                DataSourcePlatform.COROS -> api.unbindCoros(request)
                else -> return@withContext Result.failure(Exception("不支持的平台"))
            }
            
            if (response.isSuccess()) {
                // 更新本地绑定状态
                dataSourcePreferences.setPlatformBound(platform, false)
                // 清除同步时间戳
                when (platform) {
                    DataSourcePlatform.GARMIN_CHINA, DataSourcePlatform.GARMIN_GLOBAL -> 
                        dataSourcePreferences.clearGarminSyncTime()
                    DataSourcePlatform.COROS -> 
                        dataSourcePreferences.clearCorosSyncTime()
                    else -> {}
                }
                Result.success(true)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "解绑平台失败", e)
            Result.failure(e)
        }
    }
    
    // ============ 文件同步 ============

    /**
     * 获取待同步文件列表（佳明）
     * @param platform 平台（GARMIN_CHINA或GARMIN_GLOBAL）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param lastSyncTime 上次同步时间（17位格式）
     */
    suspend fun getGarminFileList(
        platform: DataSourcePlatform = DataSourcePlatform.GARMIN_CHINA,
        pageNum: Int = 1,
        pageSize: Int = SyncConstants.DEFAULT_PAGE_SIZE,
        lastSyncTime: String? = null
    ): Result<List<FileInfo>> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId()
                ?: return@withContext Result.failure(Exception("用户未登录"))

            // 使用传入的时间戳或从preferences获取
            val syncTime = lastSyncTime ?: dataSourcePreferences.getLastSyncTime(platform)

            Logger.d(TAG, "获取佳明文件列表, platform=${platform.code}, pageNum=$pageNum, lastSyncTime=$syncTime")

            val request = createBaseRequest(
                dtoName = SyncConstants.DtoNames.GARMIN_FILE_LIST_REQUEST,
                data = FileListRequest(
                    pageNum = pageNum,
                    pageSize = pageSize,
                    userId = userId,
                    lastSyncTime = syncTime
                )
            )

            val response = api.getGarminFileList(request)

            if (response.isSuccess()) {
                val files = response.data?.garminFileListResponse?.firstOrNull()?.files
                    ?.map { it.toFileInfo() }
                    ?: emptyList()
                Logger.d(TAG, "获取佳明文件列表成功, 文件数: ${files.size}")
                Result.success(files)
            } else {
                Logger.w(TAG, "获取佳明文件列表失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "获取文件列表异常", e)
            Result.failure(e)
        }
    }

    /**
     * 下载FIT文件（佳明）
     */
    suspend fun downloadGarminFile(fileInfo: FileInfo): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId()
                ?: return@withContext Result.failure(Exception("用户未登录"))

            val request = createBaseRequest(
                dtoName = SyncConstants.DtoNames.GARMIN_FILE_DOWNLOAD_REQUEST,
                data = FileDownloadRequest(
                    userId = userId,
                    id = fileInfo.id,
                    platformCode = fileInfo.platformCode,
                    summaryId = fileInfo.summaryId
                )
            )

            val responseBody = api.downloadGarminFile(request)
            val bytes = responseBody.bytes()

            Result.success(bytes)
        } catch (e: Exception) {
            Logger.e(TAG, "下载佳明文件失败", e)
            Result.failure(e)
        }
    }

    /**
     * 触发数据回填（佳明）
     * @param platform 平台
     * @param startTime 开始时间（14位格式 yyyyMMddHHmmss）
     * @param endTime 结束时间（14位格式 yyyyMMddHHmmss）
     */
    suspend fun triggerGarminBackfill(
        platform: DataSourcePlatform,
        startTime: String,
        endTime: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId()
                ?: return@withContext Result.failure(Exception("用户未登录"))

            // 确保使用14位格式
            val apiStartTime = TimestampUtils.toApiFormat(startTime)
            val apiEndTime = TimestampUtils.toApiFormat(endTime)

            Logger.i(TAG, "触发佳明回填, platform=${platform.code}, startTime=$apiStartTime, endTime=$apiEndTime")

            val request = createBaseRequest(
                dtoName = SyncConstants.DtoNames.GARMIN_BACKFILL_REQUEST,
                data = BackfillRequest(
                    userId = userId,
                    startTime = apiStartTime,
                    endTime = apiEndTime
                )
            )

            val response = when (platform) {
                DataSourcePlatform.GARMIN_CHINA -> api.backfillGarminChina(request)
                DataSourcePlatform.GARMIN_GLOBAL -> api.backfillGarminGlobal(request)
                else -> return@withContext Result.failure(Exception("不支持的平台"))
            }

            if (response.isSuccess()) {
                val backfillResult = response.data?.garminBackfillResponse?.firstOrNull()
                if (backfillResult?.success == true) {
                    Logger.i(TAG, "佳明回填请求成功, totalRequests=${backfillResult.totalRequests}")
                    Result.success(true)
                } else {
                    Logger.w(TAG, "佳明回填请求失败: ${backfillResult?.message}")
                    Result.failure(Exception(backfillResult?.message ?: "回填失败"))
                }
            } else {
                Logger.w(TAG, "佳明回填响应失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "触发佳明回填异常", e)
            Result.failure(e)
        }
    }

    // ============ 高驰文件同步 ============

    /**
     * 获取待同步文件列表（高驰）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param lastSyncTime 上次同步时间（17位格式）
     */
    suspend fun getCorosFileList(
        pageNum: Int = 1,
        pageSize: Int = SyncConstants.DEFAULT_PAGE_SIZE,
        lastSyncTime: String? = null
    ): Result<List<FileInfo>> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId()
                ?: return@withContext Result.failure(Exception("用户未登录"))

            // 使用传入的时间戳或从preferences获取
            val syncTime = lastSyncTime ?: dataSourcePreferences.getCorosLastSyncTime()

            Logger.d(TAG, "获取高驰文件列表, pageNum=$pageNum, lastSyncTime=$syncTime")

            val request = createBaseRequest(
                dtoName = SyncConstants.DtoNames.COROS_FILE_LIST_REQUEST,
                data = FileListRequest(
                    pageNum = pageNum,
                    pageSize = pageSize,
                    userId = userId,
                    lastSyncTime = syncTime
                )
            )

            val response = api.getCorosFileList(request)

            if (response.isSuccess()) {
                val files = response.data?.corosFileListResponse?.firstOrNull()?.files
                    ?.map { it.toFileInfo() }
                    ?: emptyList()
                Logger.d(TAG, "获取高驰文件列表成功, 文件数: ${files.size}")
                Result.success(files)
            } else {
                Logger.w(TAG, "获取高驰文件列表失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "获取高驰文件列表异常", e)
            Result.failure(e)
        }
    }

    /**
     * 下载高驰运动详情/FIT文件
     * 采用两级下载策略：
     * Level 1: ossUrl优先（如果有且有效）
     * Level 2: fitUrl下载（ossUrl失败或不存在时）
     *
     * @param fileInfo 文件信息
     */
    suspend fun downloadCorosFile(fileInfo: FileInfo): Result<ByteArray> = withContext(Dispatchers.IO) {
        // Level 1: 尝试ossUrl下载
        if (fileInfo.hasOssUrl) {
            val ossResult = downloadFromUrl(fileInfo.ossUrl!!, fileInfo.summaryId)
            if (ossResult.isSuccess) {
                Logger.i(TAG, "ossUrl下载成功: ${fileInfo.summaryId}")
                return@withContext ossResult
            }
            Logger.w(TAG, "ossUrl下载失败，尝试fitUrl: ${fileInfo.summaryId}")
        }

        // Level 2: 尝试fitUrl下载
        if (fileInfo.hasFitUrl) {
            val fitResult = downloadFromUrl(fileInfo.fitUrl!!, fileInfo.summaryId)
            if (fitResult.isSuccess) {
                Logger.i(TAG, "fitUrl下载成功: ${fileInfo.summaryId}")
                return@withContext fitResult
            }
            Logger.e(TAG, "fitUrl下载也失败: ${fileInfo.summaryId}")
        }

        // 两级都失败或不存在
        Logger.e(TAG, "下载失败: ossUrl和fitUrl均不可用, summaryId=${fileInfo.summaryId}")
        Result.failure(Exception("下载失败: ossUrl和fitUrl均不可用"))
    }

    /**
     * 通用URL下载方法
     * @param url 下载URL
     * @param fileName 文件名（用于日志）
     */
    private fun downloadFromUrl(url: String, fileName: String): Result<ByteArray> {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                    ?: return Result.failure(Exception("响应体为空"))
                Result.success(bytes)
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "URL下载异常: $url", e)
            Result.failure(e)
        }
    }

    /**
     * 触发数据回填（高驰）
     * @param startTime 开始时间（14位格式 yyyyMMddHHmmss）
     * @param endTime 结束时间（14位格式 yyyyMMddHHmmss）
     */
    suspend fun triggerCorosBackfill(
        startTime: String,
        endTime: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId()
                ?: return@withContext Result.failure(Exception("用户未登录"))

            // 确保使用14位格式
            val apiStartTime = TimestampUtils.toApiFormat(startTime)
            val apiEndTime = TimestampUtils.toApiFormat(endTime)

            Logger.i(TAG, "触发高驰回填, startTime=$apiStartTime, endTime=$apiEndTime")

            val request = createBaseRequest(
                dtoName = SyncConstants.DtoNames.COROS_BACKFILL_REQUEST,
                data = BackfillRequest(
                    userId = userId,
                    startTime = apiStartTime,
                    endTime = apiEndTime
                )
            )

            val response = api.backfillCoros(request)

            if (response.isSuccess()) {
                val backfillResult = response.data?.corosBackfillResponse?.firstOrNull()
                if (backfillResult?.success == true) {
                    Logger.i(TAG, "高驰回填请求成功, totalRequests=${backfillResult.totalRequests}")
                    Result.success(true)
                } else {
                    Logger.w(TAG, "高驰回填请求失败: ${backfillResult?.message}")
                    Result.failure(Exception(backfillResult?.message ?: "回填失败"))
                }
            } else {
                Logger.w(TAG, "高驰回填响应失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "触发高驰回填异常", e)
            Result.failure(e)
        }
    }
    
    // ============ 数据源信息 ============
    
    /**
     * 获取所有数据源信息（包含授权状态和优先级）
     */
    fun getAllDataSourceInfos(): List<DataSourceInfo> {
        val order = dataSourcePreferences.getDataSourceOrder()
        
        return DataSourceInfo.getAllDataSources().map { info ->
            info.copy(
                isAuthorized = dataSourcePreferences.isPlatformBound(info.platform),
                priority = order[info.platform.code] ?: 999
            )
        }.sortedBy { it.priority }
    }
    
    /**
     * 获取可排序的数据源（按优先级排序）
     */
    fun getSortableDataSourceInfos(): List<DataSourceInfo> {
        return getAllDataSourceInfos().filter { it.platform.supportsSorting }
    }
    
    /**
     * 保存数据源优先级
     */
    fun saveDataSourceOrder(order: Map<String, Int>) {
        dataSourcePreferences.saveDataSourceOrder(order)
    }
    
    /**
     * 重置为默认优先级
     */
    fun resetToDefaultOrder() {
        dataSourcePreferences.resetToDefaultOrder()
    }
    
    // ============ 同步时间戳 ============
    
    /**
     * 更新佳明同步时间戳
     */
    fun updateGarminSyncTime(timestamp: String) {
        dataSourcePreferences.setGarminLastSyncTime(timestamp)
    }
    
    /**
     * 更新高驰同步时间戳
     */
    fun updateCorosSyncTime(timestamp: String) {
        dataSourcePreferences.setCorosLastSyncTime(timestamp)
    }
    
    // ============ 辅助方法 ============
    
    private fun <T> createBaseRequest(dtoName: String, data: T) = 
        RequestBuilder.createRequest(dtoName, data, preferencesManager)
    
    private fun FileInfoDto.toFileInfo(): FileInfo {
        return FileInfo(
            id = id,
            platformCode = platformCode,
            summaryId = summaryId,
            dataDate = dataDate,
            deviceName = deviceName,
            ossUrl = ossUrl,
            fitUrl = fitUrl
        )
    }
}

