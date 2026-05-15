package com.oterman.rundemo.data.map

import com.oterman.rundemo.data.map.amap.AMapTrackMapRenderer
import com.oterman.rundemo.data.map.mapbox.MapboxTrackMapRenderer
import com.oterman.rundemo.domain.map.MapProvider
import com.oterman.rundemo.domain.map.TrackMapRenderer

object MapRendererFactory {

    private val renderers = mutableMapOf<MapProvider, TrackMapRenderer>()

    fun getRenderer(provider: MapProvider): TrackMapRenderer {
        return renderers.getOrPut(provider) {
            when (provider) {
                MapProvider.MAPBOX -> MapboxTrackMapRenderer()
                MapProvider.AMAP -> AMapTrackMapRenderer()
            }
        }
    }
}
