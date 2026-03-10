package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.local.DataSourcePreferences
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.util.RLog

/**
 * inclusiveLevel 冲突解决结果
 */
data class InclusiveLevelResult(
    val adjustedRecord: RunRecordEntity,
    val conflictsUpdated: Int = 0
)

/**
 * inclusiveLevel 冲突解决器
 * 确保同一时间段内只有一条记录被纳入统计（inclusiveLevel=1）
 *
 * 优先级规则：
 * 1. 服务端下发非1的 inclusiveLevel → 直接使用，不检查冲突
 * 2. inclusiveLevel=1 时检查时间冲突：
 *    - 不同数据源 → 按 DataSourcePreferences 优先级（数字小=优先级高）
 *    - 同数据源非HK → 当前设为0
 *    - 同数据源都是HK → Apple Watch 优先
 * 3. 任何 inclusiveLevel 变更的记录同时设 uploadStatus=0
 */
class InclusiveLevelResolver(
    private val repository: RunDataRepository,
    private val dataSourcePreferences: DataSourcePreferences
) {
    companion object {
        private const val TAG = "InclusiveLevelResolver"
        private const val MANUAL_PRIORITY = 5
        private const val APPLE_WATCH_DEVICE = "Apple Watch"
    }

    /**
     * 解决 inclusiveLevel 冲突
     * @param record 待处理的记录
     * @param serverInclusiveLevel 服务端下发的 inclusiveLevel（null 表示服务端未指定）
     */
    suspend fun resolve(
        record: RunRecordEntity,
        serverInclusiveLevel: Int? = null
    ): InclusiveLevelResult {
        // 1. 服务端指定了非1的 inclusiveLevel → 直接使用，不检查冲突
        if (serverInclusiveLevel != null && serverInclusiveLevel != 1) {
            RLog.i(TAG, "服务端指定 inclusiveLevel=$serverInclusiveLevel，直接使用")
            val adjusted = if (record.inclusiveLevel != serverInclusiveLevel) {
                record.copy(inclusiveLevel = serverInclusiveLevel, uploadStatus = 0)
            } else {
                record
            }
            return InclusiveLevelResult(adjusted)
        }

        // 2. inclusiveLevel != 1 → 不参与冲突检测
        if (record.inclusiveLevel != 1) {
            return InclusiveLevelResult(record)
        }

        // 3. 查找时间冲突的记录
        val conflicts = repository.getConflictingRecords(record.startTime, record.endTime)
            .filter { it.workoutId != record.workoutId } // 排除自身
        if (conflicts.isEmpty()) {
            RLog.d(TAG, "无冲突记录，保持 inclusiveLevel=1")
            return InclusiveLevelResult(record)
        }

        RLog.i(TAG, "发现${conflicts.size}条冲突记录，开始解决")

        val currentDatasource = record.datasource ?: ""
        val priorityMap = dataSourcePreferences.getDataSourceOrder()
        val currentPriority = priorityMap[currentDatasource] ?: MANUAL_PRIORITY

        var conflictsUpdated = 0

        for (conflict in conflicts) {
            val conflictDatasource = conflict.datasource ?: ""

            if (currentDatasource != conflictDatasource) {
                // 不同数据源 → 比较优先级
                val conflictPriority = priorityMap[conflictDatasource] ?: MANUAL_PRIORITY

                if (currentPriority < conflictPriority) {
                    // 当前优先级更高 → 冲突记录设为0
                    RLog.i(TAG, "当前[$currentDatasource]优先级($currentPriority)高于冲突[$conflictDatasource]($conflictPriority)，冲突记录设为0")
                    repository.updateRunRecord(
                        conflict.copy(inclusiveLevel = 0, uploadStatus = 0)
                    )
                    conflictsUpdated++
                } else {
                    // 当前优先级更低 → 当前设为0
                    RLog.i(TAG, "当前[$currentDatasource]优先级($currentPriority)低于冲突[$conflictDatasource]($conflictPriority)，当前设为0")
                    return InclusiveLevelResult(
                        adjustedRecord = record.copy(inclusiveLevel = 0, uploadStatus = 0),
                        conflictsUpdated = conflictsUpdated
                    )
                }
            } else {
                // 同数据源
                if (currentDatasource == "HK") {
                    // 都是HK → Apple Watch优先
                    val currentIsWatch = record.deviceInfo == APPLE_WATCH_DEVICE
                    val conflictIsWatch = conflict.deviceInfo == APPLE_WATCH_DEVICE

                    if (currentIsWatch && !conflictIsWatch) {
                        // 当前是Apple Watch → 冲突的非Watch记录设为0
                        RLog.i(TAG, "同为HK，当前Apple Watch优先，冲突记录设为0")
                        repository.updateRunRecord(
                            conflict.copy(inclusiveLevel = 0, uploadStatus = 0)
                        )
                        conflictsUpdated++
                    } else {
                        // 当前不是Apple Watch → 当前设为0
                        RLog.i(TAG, "同为HK，当前非Apple Watch，设为0")
                        return InclusiveLevelResult(
                            adjustedRecord = record.copy(inclusiveLevel = 0, uploadStatus = 0),
                            conflictsUpdated = conflictsUpdated
                        )
                    }
                } else {
                    // 同数据源非HK → 当前设为0
                    RLog.i(TAG, "同数据源[$currentDatasource]非HK，当前设为0")
                    return InclusiveLevelResult(
                        adjustedRecord = record.copy(inclusiveLevel = 0, uploadStatus = 0),
                        conflictsUpdated = conflictsUpdated
                    )
                }
            }
        }

        // 所有冲突都已处理，当前记录保持 inclusiveLevel=1
        RLog.i(TAG, "冲突解决完成，当前保持 inclusiveLevel=1，更新了${conflictsUpdated}条冲突记录")
        return InclusiveLevelResult(record, conflictsUpdated)
    }
}
