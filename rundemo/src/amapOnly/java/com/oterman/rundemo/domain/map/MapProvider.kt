package com.oterman.rundemo.domain.map

enum class MapProvider(val displayName: String) {
    AMAP("高德地图");

    companion object {
        fun default() = AMAP
    }
}
