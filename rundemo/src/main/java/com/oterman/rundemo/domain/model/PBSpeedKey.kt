package com.oterman.rundemo.domain.model

/**
 * PB Speed distance type enum
 * Corresponds to iOS PBSpeedItemKey
 */
enum class PBSpeedKey(val distance: Double, val description: String) {
    KM_1(1.0, "1km"),
    KM_3(3.0, "3km"),
    KM_5(5.0, "5km"),
    KM_10(10.0, "10km"),
    KM_HALF_MARATHON(21.0975, "半马(21.0975km)"),
    KM_MARATHON(42.195, "全马(42.195km)")
}
