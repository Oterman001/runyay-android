package com.oterman.rundemo.service.sync

import android.content.Context
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.data.local.dao.RunSamplePointDao
import com.oterman.rundemo.data.local.dao.RunSegmentDao
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.domain.model.SyncResult
import com.oterman.rundemo.domain.model.SyncTimeRange
import com.oterman.rundemo.service.sync.model.SyncConstants
import com.oterman.rundemo.service.sync.model.SyncNotification
import com.oterman.rundemo.service.sync.model.UnifiedSyncResult
import com.oterman.rundemo.util.RLog
import com.oterman.rundemo.util.TimestampUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 统一数据同步管理器
 * 单例模式，协调所有平台的数据同步
 *
 * 主要功能：
 * - 统一管理所有同步服务
 * - 提供统一的同步入口
 * - 授权状态缓存（60秒有效期）
 * - 手动同步频率限制（10秒冷却）
 * - 同步通知Flow
 */
class UnifiedDataSyncManager private constructor(
    private val context: Context,
    private val dataSourceRepository: DataSourceRepository,
    private val dataSourcePreferences: DataSourcePreferences,
    private val runRecordDao: RunRecordDao,
    private val samplePointDao: RunSamplePointDao,
    private val segmentDao: RunSegmentDao,
    private val runDataRepository: RunDataRepository
) {
    companion object {
        private const val TAG = "UnifiedDataSyncManager"

        @Volatile
        private var instance: UnifiedDataSyncManager? = null

        /**
         * 获取单例实例
         */
        fun getInstance(context: Context): UnifiedDataSyncManager {
            return instance ?: synchronized(this) {
                instance ?: createInstance(context.applicationContext).also { instance = it }
            }
        }

        private fun createInstance(context: Context): UnifiedDataSyncManager {
            val dataSourcePreferences = DataSourcePreferences(context)
            val preferencesManager = PreferencesManager(context)
            val dataSourceRepository = DataSourceRepository(
                dataSourcePreferences = dataSourcePreferences,
                preferencesManager = preferencesManager
            )
            val database = RunDatabase.getInstance(context)
            val runRecordDao = database.runRecordDao()
            val samplePointDao = database.runSamplePointDao()
            val segmentDao = database.runSegmentDao()
            val runDataRepository = RunDataRepositoryImpl.getInstance(database)

            return UnifiedDataSyncManager(
                context = context,
                dataSourceRepository = dataSourceRepository,
                dataSourcePreferences = dataSourcePreferences,
                runRecordDao = runRecordDao,
                samplePointDao = samplePointDao,
                segmentDao = segmentDao,
                runDataRepository = runDataRepository
            )
        }

        /**
         * 使用自定义依赖创建实例（用于测试或DI）
         */
        fun createWithDependencies(
            context: Context,
            dataSourceRepository: DataSourceRepository,
            dataSourcePreferences: DataSourcePreferences,
            runRecordDao: RunRecordDao,
            samplePointDao: RunSamplePointDao,
            segmentDao: RunSegmentDao,
            runDataRepository: RunDataRepository
        ): UnifiedDataSyncManager {
            return UnifiedDataSyncManager(
                context = context,
                dataSourceRepository = dataSourceRepository,
                dataSourcePreferences = dataSourcePreferences,
                runRecordDao = runRecordDao,
                samplePointDao = samplePointDao,
                segmentDao = segmentDao,
                runDataRepository = runDataRepository
            )
        }
    }

    // 协程作用域
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 同步服务实例
    private val garminChinaSyncService: GarminChinaSyncService by lazy {
        GarminChinaSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences, runDataRepository)
    }

    private val garminGlobalSyncService: GarminGlobalSyncService by lazy {
        GarminGlobalSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences, runDataRepository)
    }

    private val corosSyncService: CorosSyncService by lazy {
        CorosSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences, runDataRepository)
    }

    // 同步状态
    private val isUnifiedSyncing = AtomicBoolean(false)
    private val platformSyncMutex = Mutex()

    // 授权状态缓存
    private val authStatusCache = ConcurrentHashMap<DataSourcePlatform, AuthCacheEntry>()

    // 手动同步时间记录
    private val lastManualSyncTime = ConcurrentHashMap<DataSourcePlatform, Long>()

    // 同步通知Flow
    private val _syncNotifications = MutableSharedFlow<SyncNotification>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val syncNotifications: SharedFlow<SyncNotification> = _syncNotifications.asSharedFlow()

    /**
     * 授权状态缓存条目
     */
    private data class AuthCacheEntry(
        val isAuthorized: Boolean,
        val timestamp: Long
    ) {
        fun isValid(): Boolean {
            val elapsed = System.currentTimeMillis() - timestamp
            return elapsed < SyncConstants.AUTH_STATUS_CACHE_TTL_SECONDS * 1000
        }
    }

    // ============ 同步服务获取 ============

    /**
     * 获取指定平台的同步服务
     */
    fun getSyncService(platform: DataSourcePlatform): DataSyncService? {
        return when (platform) {
            DataSourcePlatform.GARMIN_CHINA -> garminChinaSyncService
            DataSourcePlatform.GARMIN_GLOBAL -> garminGlobalSyncService
            DataSourcePlatform.COROS -> corosSyncService
            else -> null
        }
    }

    // ============ 统一同步 ============

    /**
     * 执行统一同步（所有已授权平台）
     * @param forceRefresh 是否强制刷新授权状态
     * @return 统一同步结果Flow
     */
    fun executeUnifiedSync(forceRefresh: Boolean = false): Flow<SyncNotification> = flow {
        if (!isUnifiedSyncing.compareAndSet(false, true)) {
            emit(SyncNotification.UnifiedFailed("正在同步中，请勿重复操作"))
            return@flow
        }

        val startTime = System.currentTimeMillis()
        val results = mutableMapOf<DataSourcePlatform, SyncResult>()

        try {
           RLog.i(TAG, "开始统一同步, forceRefresh=$forceRefresh")

            // 获取所有需要同步的平台
            val platformsToSync = getAuthorizedPlatforms(forceRefresh)
           RLog.i(TAG, "需要同步的平台: ${platformsToSync.map { it.displayName }}")

            if (platformsToSync.isEmpty()) {
               RLog.i(TAG, "没有已授权的平台需要同步")
                emit(SyncNotification.UnifiedCompleted(UnifiedSyncResult.empty()))
                return@flow
            }

            // 按顺序同步每个平台
            for (platform in platformsToSync) {
                val service = getSyncService(platform) ?: continue
                val timeRange = SyncConstants.getDefaultTimeRange(platform)

               RLog.i(TAG, "开始同步平台: ${platform.displayName}")
                emit(SyncNotification.PlatformStarted(platform))

                try {
                    service.executeSync(timeRange).collect { event ->
                        when (event) {
                            is SyncEvent.Started -> {
                                // 已在上面发送PlatformStarted
                            }
                            is SyncEvent.Progress -> {
                                emit(SyncNotification.PlatformProgress(
                                    platform = platform,
                                    current = event.current,
                                    total = event.total,
                                    message = event.message
                                ))
                            }
                            is SyncEvent.RecordImported -> {
                                emit(SyncNotification.RecordImported(
                                    platform = platform,
                                    originId = event.summary.originId,
                                    displayText = event.summary.displayText
                                ))
                            }
                            is SyncEvent.Completed -> {
                                results[platform] = event.result
                                emit(SyncNotification.PlatformCompleted(platform, event.result))
                               RLog.i(TAG, "平台 ${platform.displayName} 同步完成, 导入: ${event.result.importedCount}")
                            }
                            is SyncEvent.Failed -> {
                                val failResult = SyncResult(
                                    success = false,
                                    importedCount = 0,
                                    error = event.error,
                                    platform = platform
                                )
                                results[platform] = failResult
                                emit(SyncNotification.PlatformFailed(platform, event.error))
                               RLog.e(TAG, "平台 ${platform.displayName} 同步失败: ${event.error}")
                            }
                        }
                    }
                } catch (e: Exception) {
                   RLog.e(TAG, "同步平台 ${platform.displayName} 异常", e)
                    val failResult = SyncResult(
                        success = false,
                        importedCount = 0,
                        error = e.message,
                        platform = platform
                    )
                    results[platform] = failResult
                    emit(SyncNotification.PlatformFailed(platform, e.message ?: "同步异常"))
                }
            }

            // 生成统一结果
            val unifiedResult = UnifiedSyncResult.fromPlatformResults(results, startTime)
            emit(SyncNotification.UnifiedCompleted(unifiedResult))
           RLog.i(TAG, "统一同步完成, 总导入: ${unifiedResult.totalImportedCount}")

        } catch (e: Exception) {
           RLog.e(TAG, "统一同步异常", e)
            emit(SyncNotification.UnifiedFailed(e.message ?: "同步异常"))
        } finally {
            isUnifiedSyncing.set(false)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 执行单平台同步
     * @param platform 目标平台
     * @param timeRange 时间范围
     * @return 同步结果Flow
     */
    fun executeSinglePlatformSync(
        platform: DataSourcePlatform,
        timeRange: SyncTimeRange = SyncConstants.getDefaultTimeRange(platform)
    ): Flow<SyncNotification> = flow {
        val service = getSyncService(platform)
        if (service == null) {
            emit(SyncNotification.PlatformFailed(platform, "不支持的平台"))
            return@flow
        }

        if (service.isCurrentlySyncing()) {
            emit(SyncNotification.PlatformFailed(platform, "正在同步中，请勿重复操作"))
            return@flow
        }

       RLog.i(TAG, "开始单平台同步: ${platform.displayName}, timeRange=${timeRange.displayName}")
        emit(SyncNotification.PlatformStarted(platform))

        try {
            service.executeSync(timeRange).collect { event ->
                when (event) {
                    is SyncEvent.Started -> {
                        // 已发送PlatformStarted
                    }
                    is SyncEvent.Progress -> {
                        emit(SyncNotification.PlatformProgress(
                            platform = platform,
                            current = event.current,
                            total = event.total,
                            message = event.message
                        ))
                    }
                    is SyncEvent.RecordImported -> {
                        emit(SyncNotification.RecordImported(
                            platform = platform,
                            originId = event.summary.originId,
                            displayText = event.summary.displayText
                        ))
                    }
                    is SyncEvent.Completed -> {
                        emit(SyncNotification.PlatformCompleted(platform, event.result))
                    }
                    is SyncEvent.Failed -> {
                        emit(SyncNotification.PlatformFailed(platform, event.error))
                    }
                }
            }
        } catch (e: Exception) {
           RLog.e(TAG, "单平台同步异常: ${platform.displayName}", e)
            emit(SyncNotification.PlatformFailed(platform, e.message ?: "同步异常"))
        }
    }.flowOn(Dispatchers.IO)

    // ============ 手动同步 ============

    /**
     * 触发手动同步
     * 包含回填请求 + 延迟后启动同步
     * @param platform 目标平台
     * @param timeRange 时间范围
     * @return 同步结果Flow
     */
    fun triggerManualSync(
        platform: DataSourcePlatform,
        timeRange: SyncTimeRange = SyncConstants.getDefaultTimeRange(platform)
    ): Flow<SyncNotification> = flow {
        // 检查频率限制
        if (!checkManualSyncCooldown(platform)) {
            emit(SyncNotification.PlatformFailed(platform, "请勿频繁触发同步，请稍后再试"))
            return@flow
        }

        // 检查是否正在同步
        val service = getSyncService(platform)
        if (service?.isCurrentlySyncing() == true) {
            emit(SyncNotification.PlatformFailed(platform, "正在同步中，请勿重复操作"))
            return@flow
        }

       RLog.i(TAG, "触发手动同步: ${platform.displayName}, timeRange=${timeRange.displayName}")

        // 触发回填请求
        val backfillResult = triggerBackfill(platform, timeRange)
        if (backfillResult.isFailure) {
           RLog.w(TAG, "回填请求失败: ${backfillResult.exceptionOrNull()?.message}")
            // 回填失败不影响同步，继续执行
        } else {
           RLog.i(TAG, "回填请求成功，${SyncConstants.BACKFILL_DELAY_MS}ms后开始同步")
            // 延迟等待回填数据
            delay(SyncConstants.BACKFILL_DELAY_MS)
        }

        // 记录手动同步时间
        lastManualSyncTime[platform] = System.currentTimeMillis()

        // 执行同步
        executeSinglePlatformSync(platform, timeRange).collect { event ->
            emit(event)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 检查手动同步冷却时间
     */
    private fun checkManualSyncCooldown(platform: DataSourcePlatform): Boolean {
        val lastTime = lastManualSyncTime[platform] ?: return true
        val elapsed = System.currentTimeMillis() - lastTime
        return elapsed >= SyncConstants.MANUAL_SYNC_COOLDOWN_SECONDS * 1000
    }

    /**
     * 触发回填请求
     */
    private suspend fun triggerBackfill(
        platform: DataSourcePlatform,
        timeRange: SyncTimeRange
    ): Result<Boolean> {
        val startTime = TimestampUtils.getApiTimestampDaysAgo(timeRange.days)
        val endTime = TimestampUtils.getCurrentApiTimestamp()

        return when (platform) {
            DataSourcePlatform.GARMIN_CHINA, DataSourcePlatform.GARMIN_GLOBAL -> {
                dataSourceRepository.triggerGarminBackfill(platform, startTime, endTime)
            }
            DataSourcePlatform.COROS -> {
                dataSourceRepository.triggerCorosBackfill(startTime, endTime)
            }
            else -> Result.failure(Exception("不支持的平台"))
        }
    }

    // ============ 状态查询 ============

    /**
     * 检查是否正在统一同步
     */
    fun isUnifiedSyncing(): Boolean = isUnifiedSyncing.get()

    /**
     * 检查指定平台是否正在同步
     */
    fun isPlatformSyncing(platform: DataSourcePlatform): Boolean {
        return getSyncService(platform)?.isCurrentlySyncing() ?: false
    }

    /**
     * 检查是否有任何平台正在同步
     */
    fun isAnySyncing(): Boolean {
        return isUnifiedSyncing.get() ||
                garminChinaSyncService.isCurrentlySyncing() ||
                garminGlobalSyncService.isCurrentlySyncing() ||
                corosSyncService.isCurrentlySyncing()
    }

    /**
     * 取消指定平台的同步
     */
    fun cancelPlatformSync(platform: DataSourcePlatform) {
        getSyncService(platform)?.cancelSync()
    }

    /**
     * 取消所有同步
     */
    fun cancelAllSync() {
        garminChinaSyncService.cancelSync()
        garminGlobalSyncService.cancelSync()
        corosSyncService.cancelSync()
        isUnifiedSyncing.set(false)
    }

    // ============ 授权状态 ============

    /**
     * 获取所有已授权的平台
     * @param forceRefresh 是否强制刷新
     */
    private suspend fun getAuthorizedPlatforms(forceRefresh: Boolean): List<DataSourcePlatform> {
        val platforms = listOf(
            DataSourcePlatform.GARMIN_CHINA,
            DataSourcePlatform.GARMIN_GLOBAL,
            DataSourcePlatform.COROS
        )

        return platforms.filter { platform ->
            isAuthorized(platform, forceRefresh)
        }
    }

    /**
     * 检查平台是否已授权
     */
    private suspend fun isAuthorized(platform: DataSourcePlatform, forceRefresh: Boolean): Boolean {
        // 检查缓存
        if (!forceRefresh) {
            authStatusCache[platform]?.let { cache ->
                if (cache.isValid()) {
                    return cache.isAuthorized
                }
            }
        }

        // 从本地preferences获取
        val isAuthorized = dataSourcePreferences.isPlatformBound(platform)

        // 更新缓存
        authStatusCache[platform] = AuthCacheEntry(
            isAuthorized = isAuthorized,
            timestamp = System.currentTimeMillis()
        )

        return isAuthorized
    }

    /**
     * 刷新授权状态（从服务器）
     */
    suspend fun refreshAuthorizationStatus(): Result<Unit> {
        return try {
            val result = dataSourceRepository.queryPlatformStatus()
            result.fold(
                onSuccess = {
                    // 清除缓存，下次获取时会从preferences读取最新状态
                    authStatusCache.clear()
                    Result.success(Unit)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
           RLog.e(TAG, "刷新授权状态失败", e)
            Result.failure(e)
        }
    }

    /**
     * 清除授权状态缓存
     */
    fun clearAuthStatusCache() {
        authStatusCache.clear()
    }

    // ============ 时间戳管理 ============

    /**
     * 清除指定平台的同步时间戳
     */
    fun clearSyncTimestamp(platform: DataSourcePlatform) {
        getSyncService(platform)?.clearSyncTimestamp()
    }

    /**
     * 清除所有平台的同步时间戳
     */
    fun clearAllSyncTimestamps() {
        garminChinaSyncService.clearSyncTimestamp()
        garminGlobalSyncService.clearSyncTimestamp()
        corosSyncService.clearSyncTimestamp()
    }

    /**
     * 获取指定平台的上次同步时间戳
     */
    fun getLastSyncTimestamp(platform: DataSourcePlatform): String {
        return dataSourcePreferences.getLastSyncTime(platform)
    }
}
