package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.util.RLog

/**
 * VDOT级联重算服务
 *
 * 当用户toggle某次跑步的inclusiveLevel时，需要重新计算受影响窗口内所有记录的overallVdot。
 */
class VdotRecalculationService(
    private val repository: RunDataRepository
) {
    companion object {
        private const val TAG = "VdotRecalcService"
        private const val WINDOW_DAYS = 45L
        private const val MS_PER_DAY = 24L * 60 * 60 * 1000
    }

    /**
     * 当某条记录的inclusiveLevel被toggle后，级联重算受影响的overallVdot
     *
     * @param workoutId 被toggle的记录workoutId
     * @param newLevel 新的inclusiveLevel值
     */
    suspend fun onInclusiveLevelChanged(workoutId: String, newLevel: Int) {
        // 1. 更新该记录的inclusiveLevel
        repository.updateVdotInclusiveLevel(workoutId, newLevel)

        // 2. 获取该记录的日期
        val toggledRecord = repository.getVdotByWorkoutId(workoutId) ?: run {
            RLog.w(TAG, "未找到workoutId=$workoutId 的VDOT记录")
            return
        }

        // 3. 确定受影响窗口：该记录日期 → +45天
        val windowStart = toggledRecord.date
        val windowEnd = toggledRecord.date + WINDOW_DAYS * MS_PER_DAY

        // 4. 获取窗口内所有记录（不过滤inclusiveLevel）
        val affectedRecords = repository.getAllVdotsByDateRange(windowStart, windowEnd)

        if (affectedRecords.isEmpty()) {
            RLog.d(TAG, "无受影响的记录需要重算")
            return
        }

        RLog.i(TAG, "开始级联重算: ${affectedRecords.size}条记录, 窗口=$windowStart-$windowEnd")

        // 5. 对每条受影响记录重新计算overallVdot
        for (record in affectedRecords) {
            val historyStart = record.date - WINDOW_DAYS * MS_PER_DAY
            // 获取该记录之前的历史数据（按日期倒序，已过滤inclusiveLevel）
            val history = repository.getVdotsByDateRange(historyStart, record.date)
                .filter { it.workoutId != record.workoutId } // 排除自身

            // 获取上一次的综合VDOT
            val previousOverall = history.firstOrNull()?.value

            val newOverallVdot = if (record.inclusiveLevel != 1) {
                // 该记录被排除，overallVdot设为0（或保持原始值但不参与后续计算）
                record.originValue
            } else {
                VdotCalculator.calculateOverallVdot(
                    hisVdotList = history,
                    originVdot = record.originValue,
                    currentConfidence = if (record.confidence > 0) record.confidence else 0.5,
                    currentDateMs = record.date,
                    totalDistance = 10.0, // 已通过原始计算验证，不再需要门槛检查
                    activeDuration = 30.0,
                    previousOverallVdot = previousOverall
                ) ?: record.originValue
            }

            repository.updateOverallVdotValue(record.workoutId, newOverallVdot)
            repository.updateRunRecordOverallVdot(record.workoutId, newOverallVdot)
            RLog.d(TAG, "重算: workoutId=${record.workoutId}, old=${record.value}, new=$newOverallVdot")
        }

        RLog.i(TAG, "级联重算完成: ${affectedRecords.size}条记录已更新")
    }

    /**
     * 同步完成后批量重算overallVdot
     * 消除乱序依赖，保证所有历史数据完整后再计算
     *
     * @param earliestDateMs 本次同步中最早的记录startTime
     */
    suspend fun recalculateAfterSync(earliestDateMs: Long) {
        // 计算范围：[earliestDateMs - 45天, 最新记录]
        val rangeStart = earliestDateMs - WINDOW_DAYS * MS_PER_DAY
        val rangeEnd = Long.MAX_VALUE

        // 查询范围内所有overall_vdot记录（按date ASC，不过滤inclusiveLevel）
        val allRecords = repository.getAllVdotsByDateRange(rangeStart, rangeEnd)
        if (allRecords.isEmpty()) {
            RLog.d(TAG, "同步后重算: 无VDOT记录需要重算")
            return
        }

        // 按时间升序排列（getAllVdotsByDateRange返回的可能是降序）
        val sortedRecords = allRecords.sortedBy { it.date }

        RLog.i(TAG, "同步后批量重算: ${sortedRecords.size}条记录, 范围=$rangeStart-$rangeEnd")

        // 按时间顺序逐条重算overallVdot
        for (record in sortedRecords) {
            val historyStart = record.date - WINDOW_DAYS * MS_PER_DAY
            // 获取该记录之前的历史数据（按日期倒序，已过滤inclusiveLevel）
            val history = repository.getVdotsByDateRange(historyStart, record.date)
                .filter { it.workoutId != record.workoutId }

            val previousOverall = history.firstOrNull()?.value

            val newOverallVdot = if (record.inclusiveLevel != 1) {
                record.originValue
            } else {
                VdotCalculator.calculateOverallVdot(
                    hisVdotList = history,
                    originVdot = record.originValue,
                    currentConfidence = if (record.confidence > 0) record.confidence else 0.5,
                    currentDateMs = record.date,
                    totalDistance = 10.0,
                    activeDuration = 30.0,
                    previousOverallVdot = previousOverall
                ) ?: record.originValue
            }

            // 同时更新overall_vdot表和run_record表
            repository.updateOverallVdotValue(record.workoutId, newOverallVdot)
            repository.updateRunRecordOverallVdot(record.workoutId, newOverallVdot)
            RLog.d(TAG, "同步重算: workoutId=${record.workoutId}, old=${record.value}, new=$newOverallVdot")
        }

        RLog.i(TAG, "同步后批量重算完成: ${sortedRecords.size}条记录已更新")
    }
}
