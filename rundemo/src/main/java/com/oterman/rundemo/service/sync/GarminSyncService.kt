package com.oterman.rundemo.service.sync

import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.data.local.dao.RunSamplePointDao
import com.oterman.rundemo.data.local.dao.RunSegmentDao
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.domain.model.DataSourcePlatform

/**
 * 佳明数据同步服务（兼容旧版）
 *
 * @deprecated 使用 GarminChinaSyncService 或 GarminGlobalSyncService
 * 此类保留用于向后兼容，默认行为等同于 GarminChinaSyncService
 */
@Deprecated(
    message = "Use GarminChinaSyncService or GarminGlobalSyncService instead",
    replaceWith = ReplaceWith("GarminChinaSyncService")
)
class GarminSyncService(
    dataSourceRepository: DataSourceRepository,
    runRecordDao: RunRecordDao,
    samplePointDao: RunSamplePointDao,
    segmentDao: RunSegmentDao,
    dataSourcePreferences: DataSourcePreferences
) : GarminChinaSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences) {

    override val platform: DataSourcePlatform = DataSourcePlatform.GARMIN_CHINA

    override val logTag: String = "GarminSyncService"
}
