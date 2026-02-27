package com.oterman.rundemo.service.sync

import com.oterman.rundemo.data.local.DataSourcePreferences
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
 * 佳明中国数据同步服务
 * 负责从佳明中国平台同步FIT文件并解析存储
 */
open class GarminChinaSyncService(
    dataSourceRepository: DataSourceRepository,
    runRecordDao: RunRecordDao,
    samplePointDao: RunSamplePointDao,
    segmentDao: RunSegmentDao,
    dataSourcePreferences: DataSourcePreferences,
    runDataRepository: RunDataRepository,
    healthRepository: HealthRepository? = null
) : BaseDataSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences, runDataRepository, healthRepository) {

    override val platform: DataSourcePlatform = DataSourcePlatform.GARMIN_CHINA

    override val logTag: String = "GarminChinaSync"

    override fun getLastSyncTimestamp(): String {
        val timestamp = dataSourcePreferences.getGarminChinaLastSyncTime()
        return TimestampUtils.normalizeTimestamp(timestamp)
    }

    override fun updateLastSyncTimestamp(timestamp: String) {
        val normalized = TimestampUtils.normalizeTimestamp(timestamp)
        dataSourcePreferences.setGarminChinaLastSyncTime(normalized)
    }

    override fun clearSyncTimestampInternal() {
        dataSourcePreferences.clearGarminChinaSyncTime()
    }

    override suspend fun fetchFileList(
        pageNum: Int,
        pageSize: Int,
        lastSyncTime: String
    ): Result<List<FileInfo>> {
        return dataSourceRepository.getGarminFileList(
            platform = DataSourcePlatform.GARMIN_CHINA,
            pageNum = pageNum,
            pageSize = pageSize,
            lastSyncTime = lastSyncTime
        )
    }

    override suspend fun downloadFile(fileInfo: FileInfo): Result<ByteArray> {
        return dataSourceRepository.downloadGarminFile(fileInfo)
    }
}
