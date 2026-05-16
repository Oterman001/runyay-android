package com.oterman.rundemo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 跑步记录主表Entity
 * 存储跑步活动的摘要信息
 */
@Entity(
    tableName = "run_record",
    indices = [Index(value = ["userId"])]
)
data class RunRecordEntity(
    @PrimaryKey
    val workoutId: String,              // UUID字符串，主键
    
    // 基本信息
    val startTime: Long,                // 开始时间戳(ms)
    val endTime: Long,                  // 结束时间戳(ms)
    val activityTimeZone: String? = null, // 运动发生地时区(IANA ID, e.g. Europe/Belgrade)
    val duration: Double = 0.0,         // 总时长(分钟)
    val activeDuration: Double = 0.0,   // 运动时长(分钟)
    val totalDistance: Double = 0.0,    // 总距离(公里)
    val originDistance: Double = 0.0,   // 原始距离(公里)
    
    // 速度配速
    val averageSpeed: Double = 0.0,     // 平均配速(min/km)
    val maxSpeed: Double = 0.0,         // 最快配速(min/km)
    
    // 心率
    val averageHeartRate: Double = 0.0,
    val maxHeartRate: Double = 0.0,
    val minHeartRate: Double = 0.0,
    
    // 功率
    val averagePower: Double = 0.0,
    val maxPower: Double = 0.0,
    
    // 步频步幅
    val averageCadence: Double = 0.0,       // 平均步频(spm)
    val averageStrideLength: Double = 0.0,  // 平均步幅(cm)
    
    // 跑步动态
    val averageVerticalOscillation: Double = 0.0, // 垂直振幅(cm)
    val averageContactTime: Double = 0.0,   // 触地时间(ms)
    
    // 消耗
    val totalCalories: Double = 0.0,        // 总卡路里
    val totalStepCount: Double = 0.0,       // 总步数
    val elevationAscended: Double = 0.0,    // 累计爬升(米)
    val elevationDescended: Double = 0.0,  // 累计下降(米)
    
    // VDOT与训练效果
    val vdot: Double = 0.0,
    val overallVdot: Double = 0.0,
    val trainingEffect: Double = 0.0,           // 有氧训练效果
    val anaerobicTrainingEffect: Double = 0.0,  // 无氧训练效果
    val trainingLoad: Double = 0.0,
    
    // 环境信息
    val weatherTemperature: Double = 0.0,   // 温度
    val weatherHumidity: Double = 0.0,      // 湿度
    val outdoor: Int = 0,                   // 0=室外，1=室内
    
    // 设备信息
    val deviceInfo: String? = null,         // 设备品牌
    val deviceVersion: String? = null,      // 设备型号
    
    // 数据来源
    val datasource: String? = null,         // 平台编码(GCN/GGB/COROS/HK等)
    val originId: String? = null,           // 原始活动ID
    
    // 状态标记
    val inclusiveLevel: Int = 1,            // 数据优先级(0/1/2)
    val trajectoryStatus: Int = 0,          // 轨迹状态(0未知/1存在/2不存在)
    val uploadStatus: Int = 0,              // 上传状态(0未上传/1上传中/2成功/3失败)
    
    // 用户信息
    val note: String? = null,               // 备注
    val feelingLevel: Int = 0,              // 感受等级
    val address: String? = null,            // 地理位置
    
    // 暂停事件JSON
    val eventStr: String? = null,           // 暂停/恢复事件JSON
    
    // 关联ID
    val trainPlanId: String? = null,        // 关联训练计划
    val shoeId: String? = null,             // 关联跑鞋
    val linkedRaceRecordId: String? = null, // 关联赛事记录

    // 用户隔离
    val userId: String = ""                 // 所属用户ID
)
