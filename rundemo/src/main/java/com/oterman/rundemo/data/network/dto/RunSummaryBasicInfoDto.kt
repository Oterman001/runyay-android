package com.oterman.rundemo.data.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 跑步摘要基础信息DTO
 * 共用DTO：既用于文件列表响应中内嵌的runSummary，也用于上传请求的payload
 * 所有字段nullable，服务端可能只返回部分字段
 */
data class RunSummaryBasicInfoDto(
    // 基本标识
    @SerializedName("summaryId")
    val summaryId: String? = null,

    @SerializedName("platformCode")
    val platformCode: String? = null,

    @SerializedName("activityType")
    val activityType: String? = null,

    @SerializedName("activityName")
    val activityName: String? = null,

    // 时间
    @SerializedName("startTimeInSeconds")
    val startTimeInSeconds: Long? = null,

    @SerializedName("startTimeOffsetInSeconds")
    val startTimeOffsetInSeconds: Int? = null,

    @SerializedName("durationInSeconds")
    val durationInSeconds: Double? = null,

    @SerializedName("activeDuration")
    val activeDuration: Double? = null,

    // 距离
    @SerializedName("distanceInMeters")
    val distanceInMeters: Double? = null,

    // 配速
    @SerializedName("averagePace")
    val averagePace: Double? = null,

    @SerializedName("maxPace")
    val maxPace: Double? = null,

    // 心率
    @SerializedName("averageHeartRate")
    val averageHeartRate: Double? = null,

    @SerializedName("maxHeartRate")
    val maxHeartRate: Double? = null,

    @SerializedName("minHeartRate")
    val minHeartRate: Double? = null,

    // 生理指标（来自服务端runSummary）
    @SerializedName("vo2Max")
    val vo2Max: Double? = null,

    @SerializedName("restingHeartRate")
    val restingHeartRate: Int? = null,

    // 功率
    @SerializedName("averagePower")
    val averagePower: Double? = null,

    @SerializedName("maxPower")
    val maxPower: Double? = null,

    // 步频步幅
    @SerializedName("averageCadence")
    val averageCadence: Double? = null,

    @SerializedName("averageStrideLength")
    val averageStrideLength: Double? = null,

    // 跑步动态
    @SerializedName("averageVerticalOscillation")
    val averageVerticalOscillation: Double? = null,

    @SerializedName("averageContactTime")
    val averageContactTime: Double? = null,

    // 消耗
    @SerializedName("activeKilocalories")
    val activeKilocalories: Double? = null,

    @SerializedName("totalStepCount")
    val totalStepCount: Double? = null,

    @SerializedName("totalElevationGain")
    val totalElevationGain: Double? = null,

    @SerializedName("totalElevationLoss")
    val totalElevationLoss: Double? = null,

    // VDOT与训练效果
    @SerializedName("vdot")
    val vdot: Double? = null,

    @SerializedName("overallVdot")
    val overallVdot: Double? = null,

    @SerializedName("trainingEffect")
    val trainingEffect: Double? = null,

    @SerializedName("anaerobicTrainingEffect")
    val anaerobicTrainingEffect: Double? = null,

    @SerializedName("trainingLoad")
    val trainingLoad: Double? = null,

    // 环境信息
    @SerializedName("weatherTemperature")
    val weatherTemperature: Double? = null,

    @SerializedName("weatherHumidity")
    val weatherHumidity: Double? = null,

    // 0户外 1室内
    @SerializedName("outdoor")
    val outdoor: Int? = null,

    // 设备信息
    @SerializedName("deviceInfo")
    val deviceInfo: String? = null,

    @SerializedName("deviceVersion")
    val deviceVersion: String? = null,

    // 数据来源
    @SerializedName("datasource")
    val datasource: String? = null,

    @SerializedName("originId")
    val originId: String? = null,

    // 状态标记
    @SerializedName("inclusiveLevel")
    val inclusiveLevel: Int? = null,

    @SerializedName("trajectoryStatus")
    val trajectoryStatus: Int? = null,

    @SerializedName("uploadStatus")
    val uploadStatus: Int? = null,

    // 用户信息
    @SerializedName("note")
    val note: String? = null,

    @SerializedName("feelingLevel")
    val feelingLevel: Int? = null,

    @SerializedName("address")
    val address: String? = null,

    // 关联ID
    @SerializedName("trainPlanId")
    val trainPlanId: String? = null,

    @SerializedName("shoeId")
    val shoeId: String? = null,

    @SerializedName("linkedRaceRecordId")
    val linkedRaceRecordId: String? = null,

    // 用户
    @SerializedName("userId")
    val userId: String? = null
)
