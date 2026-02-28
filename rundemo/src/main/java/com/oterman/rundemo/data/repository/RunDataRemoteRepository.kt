package com.oterman.rundemo.data.repository

import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.RequestBuilder
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.network.api.RunDataApi
import com.oterman.rundemo.data.network.dto.RunSummaryBasicInfoDto
import com.oterman.rundemo.data.network.dto.request.ActivityFileListRequest
import com.oterman.rundemo.data.network.dto.request.RunRecordUploadRequest
import com.oterman.rundemo.data.network.dto.request.RunSummaryDeleteRequest
import com.oterman.rundemo.data.network.dto.request.RunSummaryQueryRequest
import com.oterman.rundemo.data.network.dto.request.RunSummaryUpdateRequest
import com.oterman.rundemo.data.network.dto.response.RunDataUploadResponse
import com.oterman.rundemo.data.network.dto.response.RunSummaryDeleteResponse
import com.oterman.rundemo.data.network.dto.response.RunSummaryUpdateResponse
import com.oterman.rundemo.data.network.dto.response.UnifiedFileInfoDto
import com.oterman.rundemo.domain.model.UnifiedFileInfo
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 跑步数据远程仓库
 * 封装RunDataApi调用，遵循DataSourceRepository的模式
 */
class RunDataRemoteRepository(
    private val preferencesManager: PreferencesManager,
    private val api: RunDataApi = RetrofitClient.runDataApi
) {
    companion object {
        private const val TAG = "RunDataRemoteRepo"
    }

    /**
     * 获取统一文件列表（含内嵌runSummary）
     */
    suspend fun getActivityFileList(
        platformCode: String,
        pageNum: Int = 1,
        pageSize: Int = 15,
        lastSyncTime: String
    ): Result<List<UnifiedFileInfo>> = withContext(Dispatchers.IO) {
        try {
            val userId = preferencesManager.getUserId()
                ?: return@withContext Result.failure(Exception("用户未登录"))

            val request = createBaseRequest(
                dtoName = "ActivityFileListRequestDto",
                data = ActivityFileListRequest(
                    userId = userId,
                    platformCode = platformCode,
                    lastSyncTime = lastSyncTime,
                    pageNum = pageNum,
                    pageSize = pageSize
                )
            )

            val response = api.getActivityFileList(request)

            if (response.isSuccess()) {
                val files = response.data?.activityFileListResponse
                    ?.firstOrNull()?.files ?: emptyList()
                val unifiedFiles = files.map { it.toUnifiedFileInfo() }
                RLog.i(TAG, "获取文件列表成功: ${unifiedFiles.size}个文件, platform=$platformCode")
                Result.success(unifiedFiles)
            } else {
                RLog.w(TAG, "获取文件列表失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "获取文件列表异常", e)
            Result.failure(e)
        }
    }

    /**
     * 批量上传跑步基础数据
     */
    suspend fun uploadRunRecords(
        records: List<RunSummaryBasicInfoDto>
    ): Result<RunDataUploadResponse> = withContext(Dispatchers.IO) {
        try {
            val request = createBaseRequest(
                dtoName = "RunRecordUploadRequestDto",
                data = RunRecordUploadRequest(records = records)
            )

            val response = api.uploadRunRecords(request)

            if (response.isSuccess()) {
                val uploadResponse = response.data?.runRecordUploadResponseDto?.firstOrNull()
                    ?: RunDataUploadResponse(totalCount = records.size, successCount = records.size)
                RLog.i(TAG, "上传成功: total=${uploadResponse.totalCount}, success=${uploadResponse.successCount}, failed=${uploadResponse.failedCount}")
                Result.success(uploadResponse)
            } else {
                RLog.w(TAG, "上传失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "上传异常", e)
            Result.failure(e)
        }
    }

    /**
     * 更新跑步摘要
     */
    suspend fun updateRunSummary(
        summaryId: String,
        activityName: String? = null,
        note: String? = null,
        feelingLevel: Int? = null,
        shoeId: String? = null,
        raceId: String? = null
    ): Result<RunSummaryUpdateResponse> = withContext(Dispatchers.IO) {
        try {
            val request = createBaseRequest(
                dtoName = "RunSummaryUpdateRequestDto",
                data = RunSummaryUpdateRequest(
                    summaryId = summaryId,
                    activityName = activityName,
                    note = note,
                    feelingLevel = feelingLevel,
                    shoeId = shoeId,
                    raceId = raceId
                )
            )

            val response = api.updateRunSummary(request)

            if (response.isSuccess()) {
                val updateResponse = response.data?.runSummaryUpdateResponseDto?.firstOrNull()
                    ?: RunSummaryUpdateResponse(summaryId = summaryId, success = true)
                RLog.i(TAG, "更新成功: summaryId=$summaryId")
                Result.success(updateResponse)
            } else {
                RLog.w(TAG, "更新失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "更新异常", e)
            Result.failure(e)
        }
    }

    /**
     * 删除跑步记录
     */
    suspend fun deleteRunSummary(
        summaryId: String
    ): Result<RunSummaryDeleteResponse> = withContext(Dispatchers.IO) {
        try {
            val request = createBaseRequest(
                dtoName = "RunSummaryDeleteRequestDto",
                data = RunSummaryDeleteRequest(summaryId = summaryId)
            )

            val response = api.deleteRunSummary(request)

            if (response.isSuccess()) {
                val deleteResponse = response.data?.runSummaryDeleteResponseDto?.firstOrNull()
                    ?: RunSummaryDeleteResponse(summaryId = summaryId, success = true)
                RLog.i(TAG, "删除成功: summaryId=$summaryId")
                Result.success(deleteResponse)
            } else {
                RLog.w(TAG, "删除失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "删除异常", e)
            Result.failure(e)
        }
    }

    /**
     * 查询跑步摘要列表
     */
    suspend fun queryRunSummaryList(
        startDate: String,
        endDate: String,
        platformCode: String? = null,
        activityType: String? = null,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): Result<List<RunSummaryBasicInfoDto>> = withContext(Dispatchers.IO) {
        try {
            val request = createBaseRequest(
                dtoName = "RunSummaryQueryRequestDto",
                data = RunSummaryQueryRequest(
                    startDate = startDate,
                    endDate = endDate,
                    platformCode = platformCode,
                    activityType = activityType,
                    pageNum = pageNum,
                    pageSize = pageSize
                )
            )

            val response = api.queryRunSummaryList(request)

            if (response.isSuccess()) {
                val records = response.data?.runSummaryQueryResponseDto
                    ?.firstOrNull()?.records ?: emptyList()
                RLog.i(TAG, "查询成功: ${records.size}条记录")
                Result.success(records)
            } else {
                RLog.w(TAG, "查询失败: ${response.msg}")
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            RLog.e(TAG, "查询异常", e)
            Result.failure(e)
        }
    }

    private fun <T> createBaseRequest(dtoName: String, data: T) =
        RequestBuilder.createRequest(dtoName, data, preferencesManager)

    private fun UnifiedFileInfoDto.toUnifiedFileInfo(): UnifiedFileInfo = UnifiedFileInfo(
        id = id,
        platformCode = platformCode,
        summaryId = summaryId,
        dataDate = dataDate,
        deviceName = deviceName,
        ossUrl = ossUrl,
        fitUrl = fitUrl,
        runSummary = runSummary
    )
}
