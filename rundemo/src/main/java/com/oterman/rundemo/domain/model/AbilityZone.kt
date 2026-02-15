package com.oterman.rundemo.domain.model

/**
 * 能力区间类型枚举
 */
enum class AbilityZoneType(val value: Int, val displayName: String) {
    HEART_RATE_7(1, "心率7区间"),
    HEART_RATE_5(2, "心率5区间"),
    SPEED(3, "配速区间");
    
    companion object {
        fun fromValue(value: Int): AbilityZoneType? {
            return entries.find { it.value == value }
        }
    }
}

/**
 * 能力区间领域模型
 */
data class AbilityZone(
    val zoneType: AbilityZoneType,
    val zoneIndex: Int,             // 区间序号(1-7或1-5)
    val duration: Double,           // 在该区间的时长(分钟)
    val minValue: Double,           // 区间下限
    val maxValue: Double,           // 区间上限
    val percentage: Double = 0.0    // 占总时长的百分比
) {
    /**
     * 获取区间名称
     */
    fun getZoneName(): String {
        return when (zoneType) {
            AbilityZoneType.HEART_RATE_7 -> "Z$zoneIndex"
            AbilityZoneType.HEART_RATE_5 -> "Z$zoneIndex"
            AbilityZoneType.SPEED -> "E${zoneIndex}"  // E/M/T/I/R
        }
    }

    /**
     * 获取配速区间描述（中文名称）
     * 对标iOS AbilityZoneItemView 的区间描述
     */
    fun getZoneDescription(): String {
        return when (zoneType) {
            AbilityZoneType.SPEED -> when (zoneIndex) {
                1 -> "恢复/热身"
                2 -> "轻松跑(E)"
                3 -> "马拉松配速(M)"
                4 -> "乳酸阈值(T)"
                5 -> "无氧耐力(A)"
                6 -> "最大摄氧(I)"
                7 -> "爆发力训练(R)"
                else -> "Z$zoneIndex"
            }
            AbilityZoneType.HEART_RATE_7 -> when (zoneIndex) {
                1 -> "热身放松"
                2 -> "轻松有氧"
                3 -> "有氧耐力"
                4 -> "马拉松配速"
                5 -> "乳酸阈值"
                6 -> "无氧耐力"
                7 -> "极限冲刺"
                else -> "Z$zoneIndex"
            }
            AbilityZoneType.HEART_RATE_5 -> when (zoneIndex) {
                1 -> "热身放松"
                2 -> "燃脂有氧"
                3 -> "有氧耐力"
                4 -> "无氧阈值"
                5 -> "极限冲刺"
                else -> "Z$zoneIndex"
            }
        }
    }

    /**
     * 格式化配速范围（如 "5:47~6:21"）
     * 将min/km的double值转为配速字符串
     */
    fun getFormattedSpeedRange(): String {
        if (zoneType != AbilityZoneType.SPEED) {
            // 心率区间显示心率范围
            return if (minValue > 0 && maxValue > 0) {
                "${minValue.toInt()}~${maxValue.toInt()}"
            } else if (minValue > 0) {
                ">=${minValue.toInt()}"
            } else if (maxValue > 0) {
                "<=${maxValue.toInt()}"
            } else ""
        }
        val minPace = formatPaceDouble(minValue)
        val maxPace = formatPaceDouble(maxValue)
        return when {
            minValue > 0 && maxValue > 0 -> "$maxPace~$minPace"
            minValue > 0 -> ">=$minPace"
            maxValue > 0 -> "<=$maxPace"
            else -> ""
        }
    }

    private fun formatPaceDouble(paceMinPerKm: Double): String {
        if (paceMinPerKm <= 0) return "-"
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    /**
     * 格式化时长显示
     */
    fun getFormattedDuration(): String {
        val minutes = duration.toInt()
        val seconds = ((duration - minutes) * 60).toInt()
        return String.format("%d:%02d", minutes, seconds)
    }

    /**
     * 格式化百分比显示
     */
    fun getFormattedPercentage(): String {
        return String.format("%.1f%%", percentage)
    }
}

/**
 * 心率区间汇总
 */
data class HeartRateZoneSummary(
    val zones: List<AbilityZone>,
    val totalDuration: Double,
    val is7Zone: Boolean = true     // true=7区间，false=5区间
)

/**
 * 配速区间汇总
 */
data class SpeedZoneSummary(
    val zones: List<AbilityZone>,
    val totalDuration: Double
)

