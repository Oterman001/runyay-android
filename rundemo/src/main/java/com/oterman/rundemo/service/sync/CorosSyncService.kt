package com.oterman.rundemo.service.sync

import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.data.local.dao.RunSamplePointDao
import com.oterman.rundemo.data.local.dao.RunSegmentDao
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.data.repository.HealthRepository
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.FileInfo
import com.oterman.rundemo.util.TimestampUtils

/**
 * 高驰数据同步服务
 * 负责从高驰平台同步运动数据
 *
 * 高驰的数据同步流程与佳明类似：
 * 1. 获取文件列表
 * 2. 下载FIT文件
 * 3. 解析并存储
 */
class CorosSyncService(
    dataSourceRepository: DataSourceRepository,
    runRecordDao: RunRecordDao,
    samplePointDao: RunSamplePointDao,
    segmentDao: RunSegmentDao,
    dataSourcePreferences: DataSourcePreferences,
    runDataRepository: RunDataRepository,
    healthRepository: HealthRepository? = null,
    preferencesManager: PreferencesManager? = null
) : BaseDataSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences, runDataRepository, healthRepository, preferencesManager) {

    override val platform: DataSourcePlatform = DataSourcePlatform.COROS

    override val logTag: String = "CorosSyncService"

    override fun getLastSyncTimestamp(): String {
        val timestamp = dataSourcePreferences.getCorosLastSyncTime()
        return TimestampUtils.normalizeTimestamp(timestamp)
    }

    override fun updateLastSyncTimestamp(timestamp: String) {
        val normalized = TimestampUtils.normalizeTimestamp(timestamp)
        dataSourcePreferences.setCorosLastSyncTime(normalized)
    }

    override fun clearSyncTimestampInternal() {
        dataSourcePreferences.clearCorosSyncTime()
    }

    override suspend fun fetchFileList(
        pageNum: Int,
        pageSize: Int,
        lastSyncTime: String
    ): Result<List<FileInfo>> {
        return dataSourceRepository.getCorosFileList(
            pageNum = pageNum,
            pageSize = pageSize,
            lastSyncTime = lastSyncTime
        )
    }

    override suspend fun downloadFile(fileInfo: FileInfo): Result<ByteArray> {
        return dataSourceRepository.downloadCorosFile(fileInfo)
    }
}
