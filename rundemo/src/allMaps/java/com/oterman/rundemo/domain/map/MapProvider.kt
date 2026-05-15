package com.oterman.rundemo.domain.map

enum class MapProvider(val displayName: String) {
    MAPBOX("Mapbox"),
    AMAP("高德地图");

    companion object {
        fun default() = MAPBOX
    }
}
