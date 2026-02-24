package com.oterman.rundemo.domain.model

/**
 * PB Speed distance type enum
 * Corresponds to iOS PBSpeedItemKey
 */
enum class PBSpeedKey(val distance: Double, val description: String, val subType: String) {
    KM_1(1.0, "1km", "1k"),
    KM_3(3.0, "3km", "3k"),
    KM_5(5.0, "5km", "5k"),
    KM_10(10.0, "10km", "10k"),
    KM_HALF_MARATHON(21.0975, "半马(21.0975km)", "21k"),
    KM_MARATHON(42.195, "全马(42.195km)", "42k")
}
