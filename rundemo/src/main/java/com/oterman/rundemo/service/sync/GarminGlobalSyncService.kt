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
import com.oterman.rundemo.domain.model.ImportedRunSummary
import com.oterman.rundemo.domain.model.SyncTimeRange
import com.oterman.rundemo.service.sync.model.SyncConstants
import com.oterman.rundemo.util.TimestampUtils

/**
 * 佳明国际数据同步服务
 * 负责从佳明国际平台同步FIT文件并解析存储
 *
 * 注意：佳明国际版有时间限制，最多只能同步近1个月的数据
 */
class GarminGlobalSyncService(
    dataSourceRepository: DataSourceRepository,
    runRecordDao: RunRecordDao,
    samplePointDao: RunSamplePointDao,
    segmentDao: RunSegmentDao,
    dataSourcePreferences: DataSourcePreferences,
    runDataRepository: RunDataRepository,
    healthRepository: HealthRepository? = null,
    preferencesManager: PreferencesManager? = null
) : BaseDataSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences, runDataRepository, healthRepository, preferencesManager) {

    override val platform: DataSourcePlatform = DataSourcePlatform.GARMIN_GLOBAL

    override val logTag: String = "GarminGlobalSync"

    override fun getLastSyncTimestamp(): String {
        val timestamp = dataSourcePreferences.getGarminGlobalLastSyncTime()
        return TimestampUtils.normalizeTimestamp(timestamp)
    }

    override fun updateLastSyncTimestamp(timestamp: String) {
        val normalized = TimestampUtils.normalizeTimestamp(timestamp)
        dataSourcePreferences.setGarminGlobalLastSyncTime(normalized)
    }

    override fun clearSyncTimestampInternal() {
        dataSourcePreferences.clearGarminGlobalSyncTime()
    }

    override suspend fun fetchFileList(
        pageNum: Int,
        pageSize: Int,
        lastSyncTime: String
    ): Result<List<FileInfo>> {
        return dataSourceRepository.getGarminFileList(
            platform = DataSourcePlatform.GARMIN_GLOBAL,
            pageNum = pageNum,
            pageSize = pageSize,
            lastSyncTime = lastSyncTime
        )
    }

    override suspend fun processFileData(
        fileInfo: FileInfo,
        fileData: ByteArray
    ): ImportedRunSummary? {
        return super.processFileData(fileInfo, fileData)
    }

    override suspend fun downloadFile(fileInfo: FileInfo): Result<ByteArray> {
        return dataSourceRepository.downloadGarminFile(fileInfo)
    }

    /**
     * 获取默认同步时间范围
     * 佳明国际版限制为1个月
     */
    fun getDefaultTimeRange(): SyncTimeRange {
        return SyncConstants.getDefaultTimeRange(platform)
    }

    /**
     * 获取最大同步天数
     * 佳明国际版限制为30天
     */
    fun getMaxSyncDays(): Int {
        return SyncConstants.getMaxDays(platform)
    }
}
