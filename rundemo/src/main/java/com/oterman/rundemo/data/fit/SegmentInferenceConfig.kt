package com.oterman.rundemo.data.fit

/**
 * 训练分段类型推断配置
 * 对齐iOS SegmentInferenceConfig
 * 用于调整推断算法的参数阈值
 */
object SegmentInferenceConfig {

    // ==================== 心率阈值 ====================

    /** 热身心率阈值（HRR百分比），低于此值倾向于判断为热身 */
    var warmupHRThreshold: Double = 0.65

    /** 恢复心率阈值（HRR百分比），低于此值倾向于判断为恢复 */
    var recoveryHRThreshold: Double = 0.70

    /** 训练心率阈值（HRR百分比），高于此值倾向于判断为训练 */
    var workHRThreshold: Double = 0.75

    /** 心率上升判断阈值（bpm），相对前一Lap上升超过此值视为显著上升 */
    var hrRisingThreshold: Double = 10.0

    /** 心率下降判断阈值（bpm），相对前一Lap下降超过此值视为显著下降 */
    var hrFallingThreshold: Double = 10.0

    // ==================== 配速阈值 ====================

    /** 慢配速阈值（相对平均值的比例） */
    var slowPaceThreshold: Double = 1.2

    /** 快配速阈值（相对平均值的比例） */
    var fastPaceThreshold: Double = 0.8

    /** 非常慢配速阈值（相对平均值的比例） */
    var verySlowPaceThreshold: Double = 1.3

    /** 非常快配速阈值（相对平均值的比例） */
    var veryFastPaceThreshold: Double = 0.7

    // ==================== 波动检测 ====================

    /** 波动检测阈值（标准差倍数） */
    var fluctuationThreshold: Double = 1.0

    /** 间歇训练判定阈值 */
    var intervalTrainingThreshold: Double = 0.4

    // ==================== 评分权重 ====================

    var positionWeight: Double = 0.25
    var heartRateWeight: Double = 0.35
    var paceWeight: Double = 0.30
    var durationWeight: Double = 0.10

    // ==================== 时长阈值 ====================

    /** 短Lap时长阈值（秒） */
    var shortLapDuration: Double = 120.0

    /** 长Lap时长阈值（秒） */
    var longLapDuration: Double = 600.0

    // ==================== 决策阈值 ====================

    var warmupCooldownScoreThreshold: Double = 0.7
    var highConfidenceThreshold: Double = 0.8

    // ==================== 相似度分组阈值 ====================

    /** 时间相似度阈值（秒） */
    var timeSimilarityThreshold: Double = 5.0

    /** 距离相似度阈值（米） */
    var distanceSimilarityThreshold: Double = 10.0

    /** 分组一致性最低样本数 */
    var minGroupSizeForConsistency: Int = 2

    /** 分组一致性占比阈值 */
    var groupConsistencyThreshold: Double = 0.6
}

