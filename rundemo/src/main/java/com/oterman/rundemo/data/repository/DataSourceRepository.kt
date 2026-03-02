package com.oterman.rundemo.data.repository

import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.RequestBuilder
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.network.api.DataSourceApi
import com.oterman.rundemo.data.network.dto.request.BackfillRequest
import com.oterman.rundemo.data.network.dto.request.CorosCallbackRequest
import com.oterman.rundemo.data.network.dto.request.FileDownloadRequest
import com.oterman.rundemo.data.network.dto.request.FileListRequest
import com.oterman.rundemo.data.network.dto.request.GarminCallbackRequest
import com.oterman.rundemo.data.network.dto.request.HealthQueryRequest
import com.oterman.rundemo.data.network.dto.request.PlatformBindRequest
import com.oterman.rundemo.data.network.dto.request.PlatformStatusRequest
import com.oterman.rundemo.data.network.dto.request.PlatformUnbindRequest
import com.oterman.rundemo.service.sync.model.SyncConstants
import com.oterman.rundemo.util.TimestampUtils
import com.oterman.rundemo.data.network.dto.response.DailyHealthData
import com.oterman.rundemo.data.network.dto.response.FileInfoDto
import com.oterman.rundemo.data.network.dto.response.PlatformStatusResponse
import com.oterman.rundemo.domain.model.DataSourceInfo
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.FileInfo
import com.oterman.rundemo.util.RLog
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
            
            RLog.i(TAG, "开始查询平台绑定状态, userId=$userId")
            
            val request = createBaseRequest(
                dtoName = "PlatformStatusRequest",
                data = PlatformStatusRequest(userId = userId)
            )
            
            val response = api.queryPlatformStatus(request)
            
            RLog.d(TAG, "平台状态响应: code=${response.code}, msg=${response.msg}")
            RLog.d(TAG, "响应data是否为空: ${response.data == null}")
            RLog.d(TAG, "platformStatusResponseDto是否为空: ${response.data?.platformStatusResponseDto == null}")
            
            if (response.isSuccess()) {
                val statuses = response.data?.platformStatusResponseDto ?: emptyList()
                
                RLog.i(TAG, "解析到平台状态数量: ${statuses.size}")
                
                // 更新本地缓存
                val statusMap = mutableMapOf<DataSourcePlatform, Boolean>()
                statuses.forEach { status ->
                    RLog.d(TAG, "平台: ${status.platformCode}, bindStatusCode=${status.bindStatusCode}, bindStatus=${status.bindStatus}, isBound=${status.isBound}")
                    DataSourcePlatform.fromCode(status.platformCode)?.let { platform ->
                        statusMap[platform] = status.isBound
                        RLog.i(TAG, "更新缓存: ${platform.displayName} -> ${if (status.isBound) "已授权" else "未授权"}")
                    }
                }
                dataSourcePreferences.updatePlatformStatus(statusMap)
                
                RLog.i(TAG, "平台状态查询成功, 共更新${statusMap.size}个平台状态")
                Result.success(statuses)
            } else {
                RLog.w(TAG, "平台状态查询失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "查询平台状态异常", e)
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

            RLog.i(TAG, "开始获取授权URL, platform=${platform.code}, userId=$userId")

            val dtoName = when (platform) {
                DataSourcePlatform.GARMIN_CHINA, DataSourcePlatform.GARMIN_GLOBAL -> "GarminBindRequest"
                DataSourcePlatform.COROS -> "CorosBindRequest"
                else -> return@withContext Result.failure(Exception("不支持的平台"))
            }

            val request = createBaseRequest(
                dtoName = dtoName,
                data = PlatformBindRequest(userId = userId)
            )

            RLog.d(TAG, "发送绑定请求, dtoName=$dtoName")

            val response = when (platform) {
                DataSourcePlatform.GARMIN_CHINA -> api.bindGarminChina(request)
                DataSourcePlatform.GARMIN_GLOBAL -> api.bindGarminGlobal(request)
                DataSourcePlatform.COROS -> api.bindCoros(request)
                else -> return@withContext Result.failure(Exception("不支持的平台"))
            }

            RLog.d(TAG, "收到绑定响应, code=${response.code}, msg=${response.msg}")

            if (response.isSuccess()) {
                // 根据平台解析不同的响应字段
                val authUrl = when (platform) {
                    DataSourcePlatform.GARMIN_CHINA, DataSourcePlatform.GARMIN_GLOBAL -> {
                        val url = response.data?.garminBindResponse?.firstOrNull()?.redirectUrl
                        RLog.d(TAG, "佳明授权URL: $url")
                        url
                    }
                    DataSourcePlatform.COROS -> {
                        val url = response.data?.corosBindResponse?.firstOrNull()?.authUrl
                        RLog.d(TAG, "高驰授权URL: $url")
                        url
                    }
                    else -> null
                }

                if (authUrl != null) {
                    RLog.i(TAG, "获取授权URL成功, platform=${platform.code}")
                    Result.success(authUrl)
                } else {
                    RLog.w(TAG, "授权URL为空, platformøø=${platform.code}")
                    Result.failure(Exception("授权URL为空"))
                }
            } else {
                RLog.w(TAG, "获取授权URL失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "获取授权URL异常", e)
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

            RLog.i(TAG, "处理佳明OAuth回调: platform=${platform.code}, token=$oauthToken, verifier=$oauthVerifier")

            val request = createBaseRequest(
                dtoName = "GarminCallBackDto",  // 与iOS保持一致
                data = GarminCallbackRequest(
                    oauthToken = oauthToken,
                    oauthVerifier = oauthVerifier,
                    userId = userId
                )
            )

            RLog.d(TAG, "发送佳明回调请求")

            val response = when (platform) {
                DataSourcePlatform.GARMIN_CHINA -> api.callbackGarminChina(request)
                DataSourcePlatform.GARMIN_GLOBAL -> api.callbackGarminGlobal(request)
                else -> return@withContext Result.failure(Exception("不支持的平台"))
            }

            RLog.d(TAG, "佳明回调响应: code=${response.code}, msg=${response.msg}")

            if (response.isSuccess()) {
                dataSourcePreferences.setPlatformBound(platform, true)
                RLog.i(TAG, "佳明授权成功，已更新本地绑定状态")
                Result.success(true)
            } else {
                RLog.w(TAG, "佳明回调失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "处理佳明OAuth回调异常", e)
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

            RLog.i(TAG, "处理高驰OAuth回调: code=$code, state=$state")

            val request = createBaseRequest(
                dtoName = "CorosCallbackRequest",  // 与iOS保持一致
                data = CorosCallbackRequest(
                    code = code,
                    state = state,
                    userId = userId
                )
            )

            RLog.d(TAG, "发送高驰回调请求")

            val response = api.callbackCoros(request)

            RLog.d(TAG, "高驰回调响应: code=${response.code}, msg=${response.msg}")

            if (response.isSuccess()) {
                dataSourcePreferences.setPlatformBound(DataSourcePlatform.COROS, true)
                RLog.i(TAG, "高驰授权成功，已更新本地绑定状态")
                Result.success(true)
            } else {
                RLog.w(TAG, "高驰回调失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "处理高驰OAuth回调异常", e)
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
            RLog.e(TAG, "解绑平台失败", e)
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

            RLog.d(TAG, "获取佳明文件列表, platform=${platform.code}, pageNum=$pageNum, lastSyncTime=$syncTime")

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
                RLog.d(TAG, "获取佳明文件列表成功, 文件数: ${files.size}")
                Result.success(files)
            } else {
                RLog.w(TAG, "获取佳明文件列表失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "获取文件列表异常", e)
            Result.failure(e)
        }
    }

    /**
     * 下载FIT文件（佳明）
     * 三级下载策略：
     * Level 1: ossUrl优先（如果有且有效）
     * Level 2: fitUrl下载（ossUrl失败或不存在时）
     * Level 3: 后端API下载（兜底方案）
     */
    suspend fun downloadGarminFile(fileInfo: FileInfo): Result<ByteArray> = withContext(Dispatchers.IO) {
        // Level 1: 尝试ossUrl下载
        if (fileInfo.hasOssUrl) {
            RLog.d(TAG, "佳明: 尝试ossUrl下载: ${fileInfo.summaryId}")
            val ossResult = downloadFromUrl(fileInfo.ossUrl!!, fileInfo.summaryId)
            if (ossResult.isSuccess) {
                RLog.i(TAG, "佳明: ossUrl下载成功: ${fileInfo.summaryId}")
                return@withContext ossResult
            }
            RLog.w(TAG, "佳明: ossUrl下载失败，尝试下一级: ${fileInfo.summaryId}")
        }

        // Level 2: 尝试fitUrl下载
        if (fileInfo.hasFitUrl) {
            RLog.d(TAG, "佳明: 尝试fitUrl下载: ${fileInfo.summaryId}")
            val fitResult = downloadFromUrl(fileInfo.fitUrl!!, fileInfo.summaryId)
            if (fitResult.isSuccess) {
                RLog.i(TAG, "佳明: fitUrl下载成功: ${fileInfo.summaryId}")
                return@withContext fitResult
            }
            RLog.w(TAG, "佳明: fitUrl下载失败，尝试后端API: ${fileInfo.summaryId}")
        }

        // Level 3: 后端API下载（兜底）
        try {
            val userId = preferencesManager.getUserId()
                ?: return@withContext Result.failure(Exception("用户未登录"))

            RLog.d(TAG, "佳明: 尝试后端API下载: ${fileInfo.summaryId}")

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
            RLog.i(TAG, "佳明: 后端API下载成功: ${fileInfo.summaryId}")
            Result.success(bytes)
        } catch (e: Exception) {
            RLog.e(TAG, "佳明: 所有下载方式均失败: ${fileInfo.summaryId}", e)
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

            RLog.i(TAG, "触发佳明回填, platform=${platform.code}, startTime=$apiStartTime, endTime=$apiEndTime")

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
                    RLog.i(TAG, "佳明回填请求成功, totalRequests=${backfillResult.totalRequests}")
                    Result.success(true)
                } else {
                    RLog.w(TAG, "佳明回填请求失败: ${backfillResult?.message}")
                    Result.failure(Exception(backfillResult?.message ?: "回填失败"))
                }
            } else {
                RLog.w(TAG, "佳明回填响应失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "触发佳明回填异常", e)
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

            RLog.d(TAG, "获取高驰文件列表, pageNum=$pageNum, lastSyncTime=$syncTime")

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
                RLog.d(TAG, "获取高驰文件列表成功, 文件数: ${files.size}")
                Result.success(files)
            } else {
                RLog.w(TAG, "获取高驰文件列表失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "获取高驰文件列表异常", e)
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
                RLog.i(TAG, "ossUrl下载成功: ${fileInfo.summaryId}")
                return@withContext ossResult
            }
            RLog.w(TAG, "ossUrl下载失败，尝试fitUrl: ${fileInfo.summaryId}")
        }

        // Level 2: 尝试fitUrl下载
        if (fileInfo.hasFitUrl) {
            val fitResult = downloadFromUrl(fileInfo.fitUrl!!, fileInfo.summaryId)
            if (fitResult.isSuccess) {
                RLog.i(TAG, "fitUrl下载成功: ${fileInfo.summaryId}")
                return@withContext fitResult
            }
            RLog.e(TAG, "fitUrl下载也失败: ${fileInfo.summaryId}")
        }

        // 两级都失败或不存在
        RLog.e(TAG, "下载失败: ossUrl和fitUrl均不可用, summaryId=${fileInfo.summaryId}")
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
            RLog.e(TAG, "URL下载异常: $url", e)
            Result.failure(e)
        }
    }

    /**
     * 触发数据回填（高驰）
     * @param startTime 开始时间（8位格式 yyyyMMdd）
     * @param endTime 结束时间（8位格式 yyyyMMdd）
     */
    suspend fun triggerCorosBackfill(
        startTime: String,
        endTime: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId()
                ?: return@withContext Result.failure(Exception("用户未登录"))

            // 确保使用8位日期格式（高驰要求yyyyMMdd）
            val apiStartTime = TimestampUtils.toDateFormat(startTime)
            val apiEndTime = TimestampUtils.toDateFormat(endTime)

            RLog.i(TAG, "触发高驰回填, startTime=$apiStartTime, endTime=$apiEndTime")

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
                    RLog.i(TAG, "高驰回填请求成功, totalRequests=${backfillResult.totalRequests}")
                    Result.success(true)
                } else {
                    RLog.w(TAG, "高驰回填请求失败: ${backfillResult?.message}")
                    Result.failure(Exception(backfillResult?.message ?: "回填失败"))
                }
            } else {
                RLog.w(TAG, "高驰回填响应失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "触发高驰回填异常", e)
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
    
    // ============ 健康数据 ============

    /**
     * 查询健康数据（静息心率、VO2Max等）
     * @param platform 数据源平台
     * @param dateYYYYMMDD 日期，格式 "yyyyMMdd"
     * @return 每日健康数据列表
     */
    suspend fun queryHealth(
        platform: DataSourcePlatform,
        dateYYYYMMDD: String
    ): Result<List<DailyHealthData>> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId()
                ?: return@withContext Result.failure(Exception("用户未登录"))

            RLog.d(TAG, "查询健康数据, platform=${platform.code}, date=$dateYYYYMMDD")

            val request = createBaseRequest(
                dtoName = SyncConstants.DtoNames.HEALTH_QUERY_REQUEST,
                data = HealthQueryRequest(
                    userId = userId,
                    platformCode = platform.code,
                    startDate = dateYYYYMMDD,
                    endDate = dateYYYYMMDD
                )
            )

            val response = api.queryHealth(request)

            if (response.isSuccess()) {
                val dailyData = response.data?.garminHealthQueryResponse
                    ?.firstOrNull()?.dailyData ?: emptyList()
                RLog.d(TAG, "健康数据查询成功, 记录数: ${dailyData.size}")
                Result.success(dailyData)
            } else {
                RLog.w(TAG, "健康数据查询失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "查询健康数据异常", e)
            Result.failure(e)
        }
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

