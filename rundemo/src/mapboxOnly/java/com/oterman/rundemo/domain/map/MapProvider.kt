package com.oterman.rundemo.domain.map

enum class MapProvider(val displayName: String) {
    MAPBOX("Mapbox");

    companion object {
        fun default() = MAPBOX
    }
}
