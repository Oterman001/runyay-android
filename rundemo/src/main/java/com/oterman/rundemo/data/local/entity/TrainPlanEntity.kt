package com.oterman.rundemo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 训练计划本地缓存表
 *
 * 摘要字段用于日历页快速查询（不加载 detailJson）；
 * detailJson 存储完整 TrainPlan 的 JSON，仅在编辑页加载时读取。
 *
 * version 字段来自服务端，用于判断 detailJson 是否过期：
 * 当列表返回的 version > 本地 version 时，清空 detailJson 强制重新拉取。
 */
@Entity(
    tableName = "train_plan",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["scheduledDate"])
    ]
)
data class TrainPlanEntity(
    @PrimaryKey
    val planId: String,
    val userId: String,
    val name: String,
    val description: String? = null,
    /** 存原始字符串值，与 API 一致（如 selfDefine / distance / time / calories / pacer） */
    val trainWholeType: String = "selfDefine",
    /** 统一存 yyyy-MM-dd 格式，保证 SQL BETWEEN 字符串比较正确 */
    val scheduledDate: String? = null,
    val hardLevel: Int? = null,
    /** 完成标志：Y / N */
    val finishFlag: String? = "N",
    /** 存原始字符串值，与 API 一致（如 INDOOR / OUTDOOR） */
    val locationType: String? = null,
    val workoutId: String? = null,
    /** 训练来源类型，如 MCP */
    val sourceType: String? = null,
    /** 训练来源名称，如 claude ai */
    val sourceName: String? = null,
    /** 已成功推送的平台代码，逗号分隔，如 GGB,COROS */
    val sentPlatformCodes: String? = null,
    /** 第三方平台返回的 workoutId 映射 JSON，如 {"GGB":"xxx"} */
    val sentPlatformExtWorkoutIds: String? = null,
    /** 服务端版本号，每次 save 后递增，用于检测 detailJson 是否需要更新 */
    val version: Int? = null,
    /**
     * 完整 TrainPlan 的 JSON（由 TrainPlanDetailResponseData 序列化而来），
     * 包含 templateId、planIdOfAW、warmupBlock、blockList、cooldownBlock 及四个 goalStep。
     * 为 null 表示仅有摘要，尚未缓存详情。
     */
    val detailJson: String? = null,
    /** 最后一次从服务端成功同步的时间戳（毫秒） */
    val lastSyncAt: Long = 0L,
    /** 本地有未上传到服务端的修改 */
    val isDirty: Boolean = false
)
