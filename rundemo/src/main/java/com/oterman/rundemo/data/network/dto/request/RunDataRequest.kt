package com.oterman.rundemo.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * 单条跑步记录上传项
 * 对应 /api/rundata/upload Body DtoClassName: RunRecordUploadRequestDto
 * 字段完全对齐 rundemo/docs/run_recordupload.txt
 */
data class RunRecordUploadItemDto(
    @SerializedName("hkUUid") val hkUUid: String,
    @SerializedName("deviceVersion") val deviceVersion: String? = null,
    @SerializedName("deviceInfo") val deviceInfo: String? = null,
    @SerializedName("heartRateDevice") val heartRateDevice: String? = null,
    @SerializedName("deviceName") val deviceName: String? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("activeDuration") val activeDuration: Double? = null,
    @SerializedName("totalDistance") val totalDistance: Double? = null,
    @SerializedName("averagePace") val averagePace: Double? = null,
    @SerializedName("averageHeartRate") val averageHeartRate: Double? = null,
    @SerializedName("maxHeartRate") val maxHeartRate: Double? = null,
    @SerializedName("minHeartRate") val minHeartRate: Double? = null,
    @SerializedName("totalStepCount") val totalStepCount: Double? = null,
    @SerializedName("totalActiveEnergy") val totalActiveEnergy: Double? = null,
    @SerializedName("restingHeartRate") val restingHeartRate: Int? = null,
    @SerializedName("userMaxHeartRate") val userMaxHeartRate: Int? = null,

    // 距离和速度
    @SerializedName("originDistance") val originDistance: Double? = null,
    @SerializedName("maxPace") val maxPace: Double? = null,

    // 地理
    @SerializedName("address") val address: String? = null,
    @SerializedName("elevationAscended") val elevationAscended: Double? = null,

    // 步幅触地垂直振幅
    @SerializedName("averageStrideLength") val averageStrideLength: Double? = null,
    @SerializedName("averageContactTime") val averageContactTime: Double? = null,
    @SerializedName("averageVerticalOscillation") val averageVerticalOscillation: Double? = null,

    // 功率
    @SerializedName("averagePower") val averagePower: Double? = null,
    @SerializedName("maxPower") val maxPower: Double? = null,

    // 步频
    @SerializedName("averageCadence") val averageCadence: Double? = null,

    // 训练指标（注意服务端字段名拼写错误，SerializedName 必须与服务端一致）
    @SerializedName("trainingLoad") val trainingLoad: Double? = null,
    @SerializedName("tainingEffect") val trainingEffect: Double? = null,           // 服务端拼写错误
    @SerializedName("anaerobicTrainingEffect") val anaerobicTrainingEffect: Double? = null,

    // 能力评估
    @SerializedName("vdot") val vdot: Double? = null,
    @SerializedName("overallVdot") val overallVdot: Double? = null,

    // 环境
    @SerializedName("weatherHumidity") val weatherHumidity: Double? = null,
    @SerializedName("weatherTemprature") val weatherTemperature: Double? = null,   // 服务端拼写错误
    @SerializedName("outdoor") val outdoor: Int? = null,

    // 用户主观
    @SerializedName("feelingLevel") val feelingLevel: Int? = null,
    @SerializedName("inclusiveLevel") val inclusiveLevel: Int? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("eventStr") val eventStr: String? = null,

    // 关联
    @SerializedName("shoeId") val shoeId: String? = null,
    @SerializedName("trainPlanId") val trainPlanId: String? = null,
    @SerializedName("datasource") val datasource: String? = null,
    @SerializedName("originId") val originId: String? = null,
    @SerializedName("raceId") val raceId: String? = null
)

/**
 * 统一文件列表请求
 * POST /api/activityfile/list
 */
data class ActivityFileListRequest(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("platformCode")
    val platformCode: String,

    @SerializedName("lastSyncTime")
    val lastSyncTime: String,

    @SerializedName("pageNum")
    val pageNum: Int = 1,

    @SerializedName("pageSize")
    val pageSize: Int = 15,

    @SerializedName("queryManual")
    val queryManual: String = "Y"
)

/**
 * 跑步记录上传请求
 * POST /api/rundata/upload
 */
data class RunRecordUploadRequest(
    @SerializedName("records")
    val records: List<RunRecordUploadItemDto>
)

/**
 * 跑步摘要更新请求
 * POST /api/rundata/summary/update
 * 字段对齐 rundemo/docs/runRecordupdateBean.txt
 */
data class RunSummaryUpdateRequest(
    // 必填
    @SerializedName("summaryId")
    val summaryId: String,

    // 基础信息
    @SerializedName("platformCode")
    val platformCode: String? = null,
    @SerializedName("activityId")
    val activityId: String? = null,
    @SerializedName("activityName")
    val activityName: String? = null,
    @SerializedName("startTimeInSeconds")
    val startTimeInSeconds: Long? = null,
    @SerializedName("startTimeOffsetInSeconds")
    val startTimeOffsetInSeconds: Int? = null,
    @SerializedName("activityType")
    val activityType: String? = null,
    @SerializedName("durationInSeconds")
    val durationInSeconds: Int? = null,

    // 生理指标
    @SerializedName("averageHeartRate")
    val averageHeartRate: Int? = null,
    @SerializedName("averageRunCadence")
    val averageRunCadence: Double? = null,
    @SerializedName("averagePushCadence")
    val averagePushCadence: Double? = null,
    @SerializedName("averagePace")
    val averagePace: Double? = null,
    @SerializedName("activeKilocalories")
    val activeKilocalories: Double? = null,

    // 设备信息
    @SerializedName("deviceName")
    val deviceName: String? = null,

    // 运动指标
    @SerializedName("distanceInMeters")
    val distanceInMeters: Double? = null,
    @SerializedName("maxHeartRate")
    val maxHeartRate: Double? = null,
    @SerializedName("maxPace")
    val maxPace: Double? = null,
    @SerializedName("maxRunCadence")
    val maxRunCadence: Double? = null,
    @SerializedName("maxPushCadence")
    val maxPushCadence: Double? = null,

    // 运动数据
    @SerializedName("steps")
    val steps: Int? = null,
    @SerializedName("pushes")
    val pushes: Int? = null,
    @SerializedName("totalElevationGain")
    val totalElevationGain: Double? = null,
    @SerializedName("totalElevationLoss")
    val totalElevationLoss: Double? = null,

    // 活动关系
    @SerializedName("isParent")
    val isParent: Boolean? = null,
    @SerializedName("parentSummaryId")
    val parentSummaryId: String? = null,
    @SerializedName("isManual")
    val isManual: Boolean? = null,

    // 数据日期
    @SerializedName("dataDate")
    val dataDate: String? = null,

    // 生理指标详细
    @SerializedName("minHeartRate")
    val minHeartRate: Int? = null,
    @SerializedName("averageContactTime")
    val averageContactTime: Double? = null,
    @SerializedName("maxContactTime")
    val maxContactTime: Double? = null,
    @SerializedName("minContactTime")
    val minContactTime: Double? = null,
    @SerializedName("averagePower")
    val averagePower: Double? = null,
    @SerializedName("maxPower")
    val maxPower: Double? = null,
    @SerializedName("minPower")
    val minPower: Double? = null,
    @SerializedName("averageStrideLength")
    val averageStrideLength: Double? = null,
    @SerializedName("maxStrideLength")
    val maxStrideLength: Double? = null,
    @SerializedName("minStrideLength")
    val minStrideLength: Double? = null,
    @SerializedName("averageVerticalOscillation")
    val averageVerticalOscillation: Double? = null,
    @SerializedName("maxVerticalOscillation")
    val maxVerticalOscillation: Double? = null,
    @SerializedName("minVerticalOscillation")
    val minVerticalOscillation: Double? = null,
    @SerializedName("minPace")
    val minPace: Double? = null,
    @SerializedName("maxSpeed")
    val maxSpeed: Double? = null,
    @SerializedName("minSpeed")
    val minSpeed: Double? = null,
    @SerializedName("averageMets")
    val averageMets: Double? = null,

    // 环境与状态
    @SerializedName("weatherTemperature")
    val weatherTemperature: Double? = null,
    @SerializedName("weatherHumidity")
    val weatherHumidity: Double? = null,
    @SerializedName("aqi")
    val aqi: Double? = null,
    @SerializedName("altitude")
    val altitude: Double? = null,
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("timezone")
    val timezone: String? = null,
    @SerializedName("outdoor")
    val outdoor: Int? = null,
    @SerializedName("feelingLevel")
    val feelingLevel: Int? = null,

    // 训练分析
    @SerializedName("vdot")
    val vdot: Double? = null,
    @SerializedName("overallVdot")
    val overallVdot: Double? = null,
    @SerializedName("vo2Max")
    val vo2Max: Double? = null,
    @SerializedName("maf")
    val maf: Double? = null,
    @SerializedName("trainingLoad")
    val trainingLoad: Double? = null,
    @SerializedName("trainingPerformance")
    val trainingPerformance: Double? = null,
    @SerializedName("trainingEffect")
    val trainingEffect: Double? = null,
    @SerializedName("equivalentSpeed")
    val equivalentSpeed: Double? = null,
    @SerializedName("originDistance")
    val originDistance: Double? = null,

    // 其他
    @SerializedName("dataSource")
    val dataSource: String? = null,
    @SerializedName("inclusiveLevel")
    val inclusiveLevel: Int? = null,
    @SerializedName("runId")
    val runId: Int? = null,
    @SerializedName("runType")
    val runType: Int? = null,
    @SerializedName("pb")
    val pb: String? = null,
    @SerializedName("note")
    val note: String? = null,
    @SerializedName("runDescription")
    val runDescription: String? = null,
    @SerializedName("totalBasalEnergy")
    val totalBasalEnergy: Double? = null,
    @SerializedName("deviceVersion")
    val deviceVersion: String? = null,
    @SerializedName("heartRateDevice")
    val heartRateDevice: String? = null,
    @SerializedName("shoeId")
    val shoeId: String? = null,
    @SerializedName("trainPlanId")
    val trainPlanId: String? = null,

    // 新增字段
    @SerializedName("originId")
    val originId: String? = null,
    @SerializedName("startLatitude")
    val startLatitude: Double? = null,
    @SerializedName("startLongitude")
    val startLongitude: Double? = null,
    @SerializedName("mapCoordinateSystem")
    val mapCoordinateSystem: Int? = null,
    @SerializedName("raceId")
    val raceId: String? = null,

    // HealthKit客户端字段
    @SerializedName("startTime")
    val startTime: String? = null,
    @SerializedName("endTime")
    val endTime: String? = null,
    @SerializedName("activeDuration")
    val activeDuration: Int? = null,
    @SerializedName("restingHeartRate")
    val restingHeartRate: Int? = null,
    @SerializedName("userMaxHeartRate")
    val userMaxHeartRate: Int? = null
)

/**
 * 跑步记录删除请求
 * POST /api/rundata/summary/delete
 */
data class RunSummaryDeleteRequest(
    @SerializedName("summaryId")
    val summaryId: String
)

/**
 * 跑步摘要查询请求
 * POST /api/rundata/summary/list
 */
data class RunSummaryQueryRequest(
    @SerializedName("startDate")
    val startDate: String,

    @SerializedName("endDate")
    val endDate: String,

    @SerializedName("platformCode")
    val platformCode: String? = null,

    @SerializedName("activityType")
    val activityType: String? = null,

    @SerializedName("pageNum")
    val pageNum: Int = 1,

    @SerializedName("pageSize")
    val pageSize: Int = 20
)
