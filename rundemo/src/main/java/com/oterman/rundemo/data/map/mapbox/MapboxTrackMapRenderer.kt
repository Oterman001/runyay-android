package com.oterman.rundemo.data.map.mapbox

import android.content.Context
import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Terrain
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.scalebar.scalebar
import com.oterman.rundemo.domain.map.EdgePadding
import com.oterman.rundemo.domain.map.MapCameraState
import com.oterman.rundemo.domain.map.MapProvider
import com.oterman.rundemo.domain.map.MapStyleInfo
import com.oterman.rundemo.domain.map.TrackColorSet
import com.oterman.rundemo.domain.map.TrackMapRenderer
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.presentation.feature.rundetail.components.MapTrackColors
import com.oterman.rundemo.presentation.feature.rundetail.components.calculateKilometerPositions
import com.oterman.rundemo.util.RLog

private const val TAG = "MapboxRenderer"

class MapboxTrackMapRenderer : TrackMapRenderer {

    override val provider = MapProvider.MAPBOX

    override fun getAvailableStyles(isDarkTheme: Boolean): List<MapStyleInfo> = ALL_STYLES

    override fun getDefaultStyle(isDarkTheme: Boolean): MapStyleInfo {
        return if (isDarkTheme) STYLE_DARK else STYLE_STANDARD
    }

    override fun createMapView(context: Context): View {
        return MapView(context)
    }

    override fun onStart(mapView: View) {
        (mapView as MapView).onStart()
    }

    override fun onStop(mapView: View) {
        (mapView as MapView).onStop()
    }

    override fun onDestroy(mapView: View) {
        (mapView as MapView).onDestroy()
    }

    override fun configureGestures(mapView: View) {
        val mv = mapView as MapView
        mv.gestures.rotateEnabled = false
        mv.gestures.pitchEnabled = false
        mv.gestures.scrollEnabled = true
        mv.gestures.doubleTapToZoomInEnabled = true
        mv.gestures.pinchToZoomEnabled = true
    }

    override fun setupTouchInterception(mapView: View) {
        mapView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    (view.parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (view.parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }

    override fun hideScaleBar(mapView: View) {
        (mapView as MapView).scalebar.enabled = false
    }

    override fun loadStyle(mapView: View, styleUri: String, onReady: () -> Unit) {
        (mapView as MapView).mapboxMap.loadStyle(styleUri) { onReady() }
    }

    override fun renderTrack(mapView: View, trackPoints: List<TrackPoint>, colors: TrackColorSet) {
        val mv = mapView as MapView
        val style = mv.mapboxMap.style ?: return

        try {
            val validPoints = trackPoints.filter { it.isValidCoordinate() }
            if (validPoints.isEmpty()) {
                RLog.w(TAG, "没有有效的轨迹点")
                return
            }

            val points = validPoints.map { Point.fromLngLat(it.longitude, it.latitude) }
            val lineString = LineString.fromLngLats(points)

            style.addSource(geoJsonSource("track-source") { geometry(lineString) })
            style.addLayer(lineLayer("track-layer", "track-source") {
                lineColor(colors.track)
                lineWidth(MapTrackColors.TRACK_WIDTH)
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
            })

            // 起点标记
            val startPoint = validPoints.first()
            style.addSource(geoJsonSource("start-point-source") {
                geometry(Point.fromLngLat(startPoint.longitude, startPoint.latitude))
            })
            style.addLayer(circleLayer("start-point-layer", "start-point-source") {
                circleRadius(MapTrackColors.START_RADIUS)
                circleColor(colors.start)
                circleStrokeWidth(MapTrackColors.MARKER_STROKE_WIDTH)
                circleStrokeColor(colors.stroke)
            })

            // 终点标记
            if (validPoints.size > 1) {
                val endPoint = validPoints.last()
                style.addSource(geoJsonSource("end-point-source") {
                    geometry(Point.fromLngLat(endPoint.longitude, endPoint.latitude))
                })
                style.addLayer(circleLayer("end-point-layer", "end-point-source") {
                    circleRadius(MapTrackColors.END_RADIUS)
                    circleColor(colors.end)
                    circleStrokeWidth(MapTrackColors.MARKER_STROKE_WIDTH)
                    circleStrokeColor(colors.stroke)
                })
            }

            RLog.d(TAG, "轨迹添加成功, 点数: ${points.size}")
        } catch (e: Exception) {
            RLog.e(TAG, "添加轨迹失败", e)
        }
    }

    override fun renderKmMarkers(
        mapView: View,
        trackPoints: List<TrackPoint>,
        colors: TrackColorSet,
        interval: Int
    ) {
        val mv = mapView as MapView
        val style = mv.mapboxMap.style ?: return

        try {
            val kmPositions = calculateKilometerPositions(trackPoints, interval)
            RLog.d(TAG, "公里标记点数: ${kmPositions.size}, 间隔: ${interval}km")

            kmPositions.forEachIndexed { index, point ->
                val kmNumber = (index + 1) * interval
                val sourceId = "km-marker-source-$kmNumber"
                val bgLayerId = "km-marker-bg-layer-$kmNumber"
                val textLayerId = "km-marker-text-layer-$kmNumber"

                style.addSource(geoJsonSource(sourceId) {
                    geometry(Point.fromLngLat(point.longitude, point.latitude))
                })
                style.addLayer(circleLayer(bgLayerId, sourceId) {
                    circleRadius(MapTrackColors.KM_BADGE_RADIUS)
                    circleColor(colors.kmBadgeBg)
                    circleStrokeWidth(MapTrackColors.KM_BADGE_STROKE_WIDTH)
                    circleStrokeColor(colors.stroke)
                })
                style.addLayer(symbolLayer(textLayerId, sourceId) {
                    textField("$kmNumber")
                    textSize(9.0)
                    textColor(colors.kmBadgeText)
                    textAllowOverlap(true)
                    textIgnorePlacement(true)
                })
            }
        } catch (e: Exception) {
            RLog.e(TAG, "添加公里标记失败", e)
        }
    }

    override fun clearOverlays(mapView: View) {
        // Mapbox 通过 loadStyle 重新加载来清除，无需单独操作
    }

    override fun fitTrackBounds(mapView: View, trackPoints: List<TrackPoint>, padding: EdgePadding) {
        val mv = mapView as MapView
        val validPoints = trackPoints.filter { it.isValidCoordinate() }
        if (validPoints.isEmpty()) return

        try {
            var minLat = validPoints.first().latitude
            var maxLat = validPoints.first().latitude
            var minLon = validPoints.first().longitude
            var maxLon = validPoints.first().longitude

            validPoints.forEach { point ->
                if (point.latitude < minLat) minLat = point.latitude
                if (point.latitude > maxLat) maxLat = point.latitude
                if (point.longitude < minLon) minLon = point.longitude
                if (point.longitude > maxLon) maxLon = point.longitude
            }

            val bounds = CoordinateBounds(
                Point.fromLngLat(minLon, minLat),
                Point.fromLngLat(maxLon, maxLat)
            )
            val edgeInsets = EdgeInsets(padding.top, padding.left, padding.bottom, padding.right)
            val cameraOptions = mv.mapboxMap.cameraForCoordinateBounds(bounds, edgeInsets, null, 0.0)
            mv.mapboxMap.setCamera(cameraOptions)
        } catch (e: Exception) {
            RLog.e(TAG, "居中地图失败", e)
        }
    }

    override fun setCamera(mapView: View, state: MapCameraState) {
        val mv = mapView as MapView
        mv.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(state.centerLongitude, state.centerLatitude))
                .zoom(state.zoom)
                .bearing(state.bearing)
                .pitch(state.pitch)
                .build()
        )
    }

    override fun getCameraState(mapView: View): MapCameraState? {
        return try {
            val mv = mapView as MapView
            val state = mv.mapboxMap.cameraState
            val center = state.center
            MapCameraState(
                centerLatitude = center.latitude(),
                centerLongitude = center.longitude(),
                zoom = state.zoom,
                bearing = state.bearing,
                pitch = state.pitch
            )
        } catch (e: Exception) {
            RLog.e(TAG, "获取相机状态失败", e)
            null
        }
    }

    override fun snapshot(mapView: View, callback: (Bitmap?) -> Unit) {
        (mapView as MapView).snapshot(callback)
    }

    companion object {
        private val STYLE_STANDARD = MapStyleInfo("standard", "标准", "通用地图风格", Style.STANDARD, Icons.Default.Map)
        private val STYLE_OUTDOORS = MapStyleInfo("outdoors", "户外", "适合户外跑步和越野", Style.OUTDOORS, Icons.Default.Terrain)
        private val STYLE_LIGHT = MapStyleInfo("light", "浅色", "简洁的浅色主题", Style.LIGHT, Icons.Default.LightMode)
        private val STYLE_DARK = MapStyleInfo("dark", "深色", "夜间模式深色主题", Style.DARK, Icons.Default.DarkMode)
        private val STYLE_SATELLITE = MapStyleInfo("satellite", "卫星", "真实卫星影像", Style.SATELLITE, Icons.Default.Satellite)
        private val STYLE_SATELLITE_STREETS = MapStyleInfo("satellite_streets", "卫星街道", "卫星影像叠加街道信息", Style.SATELLITE_STREETS, Icons.Default.SatelliteAlt)

        val ALL_STYLES = listOf(STYLE_STANDARD, STYLE_OUTDOORS, STYLE_LIGHT, STYLE_DARK, STYLE_SATELLITE, STYLE_SATELLITE_STREETS)
    }
}
