package com.oterman.rundemo.service.sync

import android.content.Context
import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.data.local.dao.RunSamplePointDao
import com.oterman.rundemo.data.local.dao.RunSegmentDao
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.fit.RunSummaryMapper
import com.oterman.rundemo.data.repository.DataSourceRepository
import com.oterman.rundemo.data.repository.HealthRepository
import com.oterman.rundemo.data.repository.RunDataRemoteRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 同步UI状态，供UI层观察
 */
sealed class SyncUiState {
    object Idle : SyncUiState()
    data class Syncing(val startTime: Long = System.currentTimeMillis()) : SyncUiState()
    data class Completed(val result: UnifiedSyncResult) : SyncUiState()
}

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
    private val runDataRepository: RunDataRepository,
    private val healthRepository: HealthRepository? = null
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
            val healthRepository = HealthRepository(
                dailyHealthDao = database.dailyHealthDao(),
                dataSourceRepository = dataSourceRepository,
                preferencesManager = preferencesManager
            )

            return UnifiedDataSyncManager(
                context = context,
                dataSourceRepository = dataSourceRepository,
                dataSourcePreferences = dataSourcePreferences,
                runRecordDao = runRecordDao,
                samplePointDao = samplePointDao,
                segmentDao = segmentDao,
                runDataRepository = runDataRepository,
                healthRepository = healthRepository
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

    // 用户偏好设置管理器
    private val preferencesManager: PreferencesManager by lazy { PreferencesManager(context) }

    // 跑步数据远程仓库（lazy初始化）
    private val runDataRemoteRepository: RunDataRemoteRepository by lazy {
        RunDataRemoteRepository(preferencesManager)
    }

    // 同步服务实例
    private val garminChinaSyncService: GarminChinaSyncService by lazy {
        GarminChinaSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences, runDataRepository, healthRepository, preferencesManager)
    }

    private val garminGlobalSyncService: GarminGlobalSyncService by lazy {
        GarminGlobalSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences, runDataRepository, healthRepository, preferencesManager)
    }

    private val corosSyncService: CorosSyncService by lazy {
        CorosSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences, runDataRepository, healthRepository, preferencesManager)
    }

    // 统一同步服务实例（每个平台一个）
    private val unifiedGarminChinaSyncService: UnifiedSyncService by lazy {
        UnifiedSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences, runDataRepository, healthRepository, runDataRemoteRepository, DataSourcePlatform.GARMIN_CHINA, preferencesManager)
    }

    private val unifiedGarminGlobalSyncService: UnifiedSyncService by lazy {
        UnifiedSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences, runDataRepository, healthRepository, runDataRemoteRepository, DataSourcePlatform.GARMIN_GLOBAL, preferencesManager)
    }

    private val unifiedCorosSyncService: UnifiedSyncService by lazy {
        UnifiedSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences, runDataRepository, healthRepository, runDataRemoteRepository, DataSourcePlatform.COROS, preferencesManager)
    }

    private val unifiedAppleHealthSyncService: UnifiedSyncService by lazy {
        UnifiedSyncService(dataSourceRepository, runRecordDao, samplePointDao, segmentDao, dataSourcePreferences, runDataRepository, healthRepository, runDataRemoteRepository, DataSourcePlatform.APPLE_HEALTH, preferencesManager)
    }

    private val unifiedManualSyncService: UnifiedSyncService by lazy {
        UnifiedSyncService(
            dataSourceRepository, runRecordDao, samplePointDao, segmentDao,
            dataSourcePreferences, runDataRepository, healthRepository,
            runDataRemoteRepository, DataSourcePlatform.MANUAL, preferencesManager
        )
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

    // 同步UI状态
    private val _syncUiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncUiState: StateFlow<SyncUiState> = _syncUiState.asStateFlow()

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
     * 启动单平台手动同步（fire-and-forget）
     * 在manager自己的scope中执行，退出界面后仍持续运行
     * 更新syncUiState供HomeViewModel的同步图标感知
     * 如果已在同步中则忽略
     */
    fun launchManualSync(platform: DataSourcePlatform, timeRange: SyncTimeRange? = null) {
        if (isAnySyncing()) {
            RLog.d(TAG, "launchManualSync: 已在同步中，忽略")
            return
        }

        scope.launch {
            _syncUiState.value = SyncUiState.Syncing()
            try {
                var syncResult: SyncResult? = null
                val effectiveTimeRange = timeRange ?: SyncConstants.getDefaultTimeRange(platform)
                triggerManualSync(platform, effectiveTimeRange).collect { notification ->
                    _syncNotifications.tryEmit(notification)
                    when (notification) {
                        is SyncNotification.PlatformCompleted -> {
                            syncResult = notification.result
                        }
                        is SyncNotification.PlatformFailed -> {
                            syncResult = SyncResult(
                                success = false,
                                importedCount = 0,
                                platform = platform,
                                error = notification.error
                            )
                        }
                        else -> { /* progress events */ }
                    }
                }
                val result = syncResult
                    ?.let { UnifiedSyncResult.fromSinglePlatform(it) }
                    ?: UnifiedSyncResult.empty()
                _syncUiState.value = SyncUiState.Completed(result)
                delay(3000)
                _syncUiState.value = SyncUiState.Idle
            } catch (e: Exception) {
                RLog.e(TAG, "launchManualSync异常", e)
                _syncUiState.value = SyncUiState.Completed(UnifiedSyncResult.empty())
                delay(3000)
                _syncUiState.value = SyncUiState.Idle
            }
        }
    }

    /**
     * 通过前台服务启动单平台手动同步
     * ViewModel无Context，由manager代理启动Service
     */
    fun startManualSyncViaService(platform: DataSourcePlatform, timeRange: SyncTimeRange? = null) {
        DataSyncForegroundService.startForPlatform(context, platform, timeRange)
    }

    /**
     * 启动统一同步（fire-and-forget）
     * 在manager自己的scope中执行，更新syncUiState供UI观察
     * 如果已在同步中则忽略
     */
    fun launchUnifiedSync() {
        if (isUnifiedSyncing.get()) {
            RLog.d(TAG, "launchUnifiedSync: 已在同步中，忽略")
            return
        }

        scope.launch {
            _syncUiState.value = SyncUiState.Syncing()
            try {
                var finalResult: UnifiedSyncResult? = null
                executeUnifiedSync().collect { notification ->
                    _syncNotifications.tryEmit(notification)
                    when (notification) {
                        is SyncNotification.UnifiedCompleted -> {
                            finalResult = notification.result
                        }
                        is SyncNotification.UnifiedFailed -> {
                            finalResult = UnifiedSyncResult.empty()
                        }
                        else -> { /* progress events */ }
                    }
                }
                val result = finalResult ?: UnifiedSyncResult.empty()
                _syncUiState.value = SyncUiState.Completed(result)
                delay(3000)
                _syncUiState.value = SyncUiState.Idle
            } catch (e: Exception) {
                RLog.e(TAG, "launchUnifiedSync异常", e)
                _syncUiState.value = SyncUiState.Completed(UnifiedSyncResult.empty())
                delay(3000)
                _syncUiState.value = SyncUiState.Idle
            }
        }
    }

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

            // 读取用户在数据源管理页面设置的平台排序，所有支持排序的平台均按用户设置顺序执行
            val savedOrder = dataSourcePreferences.getDataSourceOrder()
            val platformsToSync = DataSourcePlatform.getSortablePlatforms()
                .filter { it.isEnabled }
                .sortedBy { savedOrder[it.code] ?: Int.MAX_VALUE }
           RLog.i(TAG, "需要同步的平台(按用户排序): ${platformsToSync.map { it.displayName }}")

            // 按顺序同步每个平台
            for (platform in platformsToSync) {
                val service = getUnifiedSyncService(platform) ?: continue
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
//        val service = getSyncService(platform)
        val service = getUnifiedSyncService(platform)
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
            emit(SyncNotification.BackfillCompleted(platform))
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
                corosSyncService.isCurrentlySyncing() ||
                unifiedAppleHealthSyncService.isCurrentlySyncing() ||
                unifiedManualSyncService.isCurrentlySyncing()
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
        unifiedAppleHealthSyncService.cancelSync()
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
        unifiedAppleHealthSyncService.clearSyncTimestamp()
    }

    /**
     * 获取指定平台的上次同步时间戳
     */
    fun getLastSyncTimestamp(platform: DataSourcePlatform): String {
        return dataSourcePreferences.getLastSyncTime(platform)
    }

    // ============ 统一同步服务 ============

    /**
     * 获取指定平台的统一同步服务
     */
    fun getUnifiedSyncService(platform: DataSourcePlatform): UnifiedSyncService? {
        return when (platform) {
            DataSourcePlatform.MANUAL -> unifiedManualSyncService
            DataSourcePlatform.APPLE_HEALTH -> unifiedAppleHealthSyncService
            DataSourcePlatform.GARMIN_CHINA -> unifiedGarminChinaSyncService
            DataSourcePlatform.GARMIN_GLOBAL -> unifiedGarminGlobalSyncService
            DataSourcePlatform.COROS -> unifiedCorosSyncService
            else -> null
        }
    }

    // ============ 跑步数据管理 ============

    /**
     * 更新跑步摘要（note/feeling/shoe等）
     */
    suspend fun updateRunSummary(
        summaryId: String,
        activityName: String? = null,
        note: String? = null,
        feelingLevel: Int? = null,
        shoeId: String? = null,
        raceId: String? = null
    ): Result<Boolean> {
        return try {
            val result = runDataRemoteRepository.updateRunSummary(
                summaryId = summaryId,
                activityName = activityName,
                note = note,
                feelingLevel = feelingLevel,
                shoeId = shoeId,
                raceId = raceId
            )
            result.map { it.success }
        } catch (e: Exception) {
            RLog.e(TAG, "更新跑步摘要失败", e)
            Result.failure(e)
        }
    }

    /**
     * 删除跑步记录
     * 先调API删除服务端，成功后删本地
     */
    suspend fun deleteRunSummary(summaryId: String, workoutId: String): Result<Boolean> {
        return try {
            val result = runDataRemoteRepository.deleteRunSummary(summaryId)
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        // 服务端删除成功，删除本地记录
                        runDataRepository.deleteRunRecord(workoutId)
                        RLog.i(TAG, "删除成功: summaryId=$summaryId, workoutId=$workoutId")
                        Result.success(true)
                    } else {
                        Result.failure(Exception(response.message ?: "服务端删除失败"))
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            RLog.e(TAG, "删除跑步记录失败", e)
            Result.failure(e)
        }
    }

    /**
     * 重试待上传的记录
     * 查询uploadStatus=0/3的记录，批量上传
     */
    suspend fun retryPendingUploads(): Result<Int> {
        return try {
            val pendingRecords = runDataRepository.getRecordsNeedingUpload()
            if (pendingRecords.isEmpty()) {
                RLog.d(TAG, "没有待上传的记录")
                return Result.success(0)
            }

            RLog.i(TAG, "发现${pendingRecords.size}条待上传记录")

            // 标记为上传中
            pendingRecords.forEach { record ->
                runDataRepository.updateUploadStatus(record.workoutId, 1)
            }

            // 转换为上传DTO
            val uploadDtos = pendingRecords.map { RunSummaryMapper.toUploadItemDto(it) }
            val result = runDataRemoteRepository.uploadRunRecords(uploadDtos)

            result.fold(
                onSuccess = { response ->
                    // 全部标记为成功
                    pendingRecords.forEach { record ->
                        runDataRepository.updateUploadStatus(record.workoutId, 2)
                    }
                    RLog.i(TAG, "批量上传成功: ${response.successCount}/${response.totalCount}")
                    Result.success(response.successCount)
                },
                onFailure = { error ->
                    // 全部标记为失败
                    pendingRecords.forEach { record ->
                        runDataRepository.updateUploadStatus(record.workoutId, 3)
                    }
                    RLog.w(TAG, "批量上传失败: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            RLog.e(TAG, "重试上传异常", e)
            Result.failure(e)
        }
    }
}
