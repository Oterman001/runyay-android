package com.oterman.rundemo.service.sync

import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.ImportedRunSummary
import com.oterman.rundemo.domain.model.SyncResult
import com.oterman.rundemo.domain.model.SyncTimeRange
import kotlinx.coroutines.flow.Flow

/**
 * 数据同步服务接口
 * 定义数据源同步的通用行为
 */
interface DataSyncService {
    
    /**
     * 获取数据源平台
     */
    val platform: DataSourcePlatform
    
    /**
     * 检查是否正在同步
     */
    fun isCurrentlySyncing(): Boolean
    
    /**
     * 执行数据同步
     * @return 同步结果流，包含导入进度和最终结果
     */
    fun executeSync(): Flow<SyncEvent>
    
    /**
     * 执行指定时间范围的同步
     * @param timeRange 同步时间范围
     * @return 同步结果流
     */
    fun executeSync(timeRange: SyncTimeRange): Flow<SyncEvent>
    
    /**
     * 取消同步
     */
    fun cancelSync()
    
    /**
     * 清除同步时间戳（用于重新同步）
     */
    fun clearSyncTimestamp()
}

/**
 * 同步事件
 */
sealed class SyncEvent {
    /**
     * 同步开始
     */
    data object Started : SyncEvent()
    
    /**
     * 记录导入成功
     */
    data class RecordImported(val summary: ImportedRunSummary) : SyncEvent()
    
    /**
     * 同步进度
     */
    data class Progress(
        val current: Int,
        val total: Int,
        val message: String = ""
    ) : SyncEvent()
    
    /**
     * 同步完成
     */
    data class Completed(val result: SyncResult) : SyncEvent()
    
    /**
     * 同步失败
     */
    data class Failed(val error: String) : SyncEvent()
}

