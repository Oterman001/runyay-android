package com.oterman.rundemo.presentation.components.trajectory

/**
 * 轨迹缩略图颜色模式
 */
enum class TrajectoryColorMode {
    FIXED,          // 固定色（亮色橙 / 暗色黄绿）
    DISTANCE_BASED  // 基于距离分色
}

/**
 * 距离档位（5 档 × 2 主题）
 *
 * 色相从冷到暖：青 → 绿 → 橙 → 红 → 紫
 * 亮色取 Material 400 色阶，暗色取 200-300 色阶
 */
enum class DistanceTier(
    val lightTrackColor: Int,
    val darkTrackColor: Int,
    val maxKm: Double,       // 该档上限（含），Double.MAX_VALUE 表示无上限
    val suffix: String       // 缓存 key 后缀
) {
    T1_RECOVERY(
        lightTrackColor = 0xFF26A69A.toInt(),  // Teal 400
        darkTrackColor  = 0xFF4DD0E1.toInt(),  // Cyan 300
        maxKm  = 3.0,
        suffix = "_t1"
    ),
    T2_DAILY(
        lightTrackColor = 0xFF66BB6A.toInt(),  // Green 400
        darkTrackColor  = 0xFF81C784.toInt(),  // Green 300
        maxKm  = 5.0,
        suffix = "_t2"
    ),
    T3_LONG(
        lightTrackColor = 0xFFFFA726.toInt(),  // Orange 400
        darkTrackColor  = 0xFFFFB74D.toInt(),  // Orange 300
        maxKm  = 10.0,
        suffix = "_t3"
    ),
    T4_HALF_MARATHON(
        lightTrackColor = 0xFFEF5350.toInt(),  // Red 400
        darkTrackColor  = 0xFFFF8A65.toInt(),  // DeepOrange 300
        maxKm  = 21.0,
        suffix = "_t4"
    ),
    T5_MARATHON(
        lightTrackColor = 0xFFAB47BC.toInt(),  // Purple 400
        darkTrackColor  = 0xFFCE93D8.toInt(),  // Purple 200
        maxKm  = Double.MAX_VALUE,
        suffix = "_t5"
    );

    fun trackColor(isDark: Boolean): Int = if (isDark) darkTrackColor else lightTrackColor
}

/** 根据总距离（km）返回所属档位 */
fun getDistanceTier(distanceKm: Double): DistanceTier {
    return when {
        distanceKm < 3.0  -> DistanceTier.T1_RECOVERY
        distanceKm < 5.0  -> DistanceTier.T2_DAILY
        distanceKm < 10.0 -> DistanceTier.T3_LONG
        distanceKm < 21.0 -> DistanceTier.T4_HALF_MARATHON
        else               -> DistanceTier.T5_MARATHON
    }
}

/**
 * 获取轨迹线颜色覆盖值
 * @return DISTANCE_BASED 模式返回 Android Color int，FIXED 模式返回 null（走原逻辑）
 */
fun getTrackColor(
    distanceKm: Double,
    isDark: Boolean,
    mode: TrajectoryColorMode = TrajectoryColorMode.DISTANCE_BASED
): Int? {
    if (mode != TrajectoryColorMode.DISTANCE_BASED) return null
    return getDistanceTier(distanceKm).trackColor(isDark)
}

/**
 * 获取缓存 key 后缀
 * DISTANCE_BASED 返回 "_t1"~"_t5"，FIXED 返回 ""
 */
fun getCacheKeySuffix(
    distanceKm: Double,
    mode: TrajectoryColorMode = TrajectoryColorMode.DISTANCE_BASED
): String {
    if (mode != TrajectoryColorMode.DISTANCE_BASED) return ""
    return getDistanceTier(distanceKm).suffix
}
