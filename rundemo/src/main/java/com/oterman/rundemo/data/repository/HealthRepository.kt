package com.oterman.rundemo.data.repository

import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.dao.DailyHealthDao
import com.oterman.rundemo.data.local.entity.DailyHealthEntity
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.util.RLog

/**
 * 健康数据仓库
 * 负责健康数据的缓存管理和对外提供静息心率、VO2Max等数据
 *
 * 缓存策略：per (userId, platformCode, calendarDate)
 * 如果缓存命中则跳过API调用，缓存未命中则调用DataSourceRepository获取并缓存
 */
class HealthRepository(
    private val dailyHealthDao: DailyHealthDao,
    private val dataSourceRepository: DataSourceRepository,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "HealthRepository"
    }

    /**
     * 获取指定日期的健康数据（如缓存未命中则从网络获取）
     * @param platform 数据源平台
     * @param calendarDateDash 日期，格式 "yyyy-MM-dd"
     */
    suspend fun fetchHealthDataIfNeeded(platform: DataSourcePlatform, calendarDateDash: String) {
        val userId = preferencesManager.getUserId() ?: return

        if (dailyHealthDao.exists(userId, platform.code, calendarDateDash)) {
            RLog.d(TAG, "健康数据缓存命中: platform=${platform.code}, date=$calendarDateDash")
            return
        }

        RLog.d(TAG, "健康数据缓存未命中，从网络获取: platform=${platform.code}, date=$calendarDateDash")

        // yyyy-MM-dd -> yyyyMMdd for API
        val dateYYYYMMDD = calendarDateDash.replace("-", "")

        try {
            val result = dataSourceRepository.queryHealth(platform, dateYYYYMMDD)
            result.onSuccess { dailyDataList ->
                for (data in dailyDataList) {
                    val entity = DailyHealthEntity(
                        userId = userId,
                        platformCode = platform.code,
                        calendarDate = data.calendarDate,
                        restingHeartRate = data.restingHeartRateInBeatsPerMinute,
                        vo2Max = data.vo2Max
                    )
                    dailyHealthDao.insertOrReplace(entity)
                    RLog.d(TAG, "已缓存健康数据: platform=${platform.code}, date=${data.calendarDate}, restHR=${data.restingHeartRateInBeatsPerMinute}, vo2Max=${data.vo2Max}")
                }
            }.onFailure { e ->
                RLog.w(TAG, "获取健康数据失败（非致命）: ${e.message}")
            }
        } catch (e: Exception) {
            RLog.w(TAG, "获取健康数据异常（非致命）: ${e.message}")
        }
    }

    /**
     * 获取指定日期的最佳静息心率（跨平台取最小值）
     * 优先精确匹配当日，无数据时 fallback 到该日期前最近的一条记录
     * @param calendarDateDash 日期，格式 "yyyy-MM-dd"
     * @return 静息心率，未获取到返回null
     */
    suspend fun getRestingHRForDate(calendarDateDash: String): Int? {
        val userId = preferencesManager.getUserId() ?: return null
        val exact = dailyHealthDao.getBestRestingHR(userId, calendarDateDash)
        if (exact != null && exact > 0) return exact
        val recent = dailyHealthDao.getMostRecentRestingHRBefore(userId, calendarDateDash)
        if (recent != null && recent > 0) {
            RLog.d(TAG, "当日[$calendarDateDash]无静息心率，使用最近一条记录: $recent")
        }
        return recent
    }

    /**
     * 获取健康数据并返回静息心率[]
     * 组合 fetchHealthDataIfNeeded + getRestingHRForDate
     * @param platform 数据源平台
     * @param calendarDateDash 日期，格式 "yyyy-MM-dd"
     * @return 静息心率，未获取到返回null
     */
    suspend fun fetchAndGetRestingHR(platform: DataSourcePlatform, calendarDateDash: String): Int? {
        fetchHealthDataIfNeeded(platform, calendarDateDash)
        return getRestingHRForDate(calendarDateDash)
    }

    /**
     * 从runSummary中直接存储健康数据到本地DB（不触发网络请求）
     * 用于同步流程中，服务端runSummary已包含健康数据的场景
     */
    suspend fun saveHealthDataFromSummary(
        platformCode: String,
        calendarDate: String,
        restingHeartRate: Int?,
        vo2Max: Double?
    ) {
        val userId = preferencesManager.getUserId() ?: return
        val entity = DailyHealthEntity(
            userId = userId,
            platformCode = platformCode,
            calendarDate = calendarDate,
            restingHeartRate = restingHeartRate,
            vo2Max = vo2Max
        )
        dailyHealthDao.insertOrReplace(entity)
        RLog.d(TAG, "从runSummary缓存健康数据: platform=$platformCode, date=$calendarDate, restHR=$restingHeartRate, vo2Max=$vo2Max")
    }

    /**
     * 获取最近一次静息心率（从DB取，不触发网络请求）
     */
    suspend fun getLatestRestingHR(): Int? {
        val userId = preferencesManager.getUserId() ?: return null
        return dailyHealthDao.getLatestRestingHR(userId)
    }

    /**
     * 获取指定日期、指定平台的静息心率
     */
    suspend fun getRestingHRForDateByPlatform(platform: DataSourcePlatform, calendarDateDash: String): Int? {
        val userId = preferencesManager.getUserId() ?: return null
        return dailyHealthDao.getByUserPlatformDate(userId, platform.code, calendarDateDash)?.restingHeartRate
    }

    /**
     * 获取最新VO2Max及变化量（用于详情页展示）
     * @return Pair(latestVo2Max, delta) delta为当前-上一次，无上一次时为null
     */
    suspend fun getLatestVo2MaxWithDelta(): Pair<Double, Double?>? {
        val userId = preferencesManager.getUserId() ?: return null
        val records = dailyHealthDao.getLatestTwoVo2Max(userId)
        if (records.isEmpty()) return null

        val latest = records[0].vo2Max ?: return null
        val delta = if (records.size >= 2) {
            val previous = records[1].vo2Max
            if (previous != null) latest - previous else null
        } else null

        return Pair(latest, delta)
    }

    /**
     * 获取指定日期的VO2Max（跨平台取最大值）
     * @param calendarDateDash 日期，格式 "yyyy-MM-dd"
     */
    suspend fun getVo2MaxForDate(calendarDateDash: String): Double? {
        val userId = preferencesManager.getUserId() ?: return null
        return dailyHealthDao.getVo2MaxForDate(userId, calendarDateDash)
    }

    /**
     * 获取指定日期之前最近的VO2Max（用于delta计算）
     * @param calendarDateDash 日期，格式 "yyyy-MM-dd"
     */
    suspend fun getPreviousVo2Max(calendarDateDash: String): Double? {
        val userId = preferencesManager.getUserId() ?: return null
        return dailyHealthDao.getPreviousVo2Max(userId, calendarDateDash)
    }
}
