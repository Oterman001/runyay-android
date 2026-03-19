package com.oterman.rundemo.data.map.amap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Satellite
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.oterman.rundemo.domain.map.EdgePadding
import com.oterman.rundemo.domain.map.MapCameraState
import com.oterman.rundemo.domain.map.MapProvider
import com.oterman.rundemo.domain.map.MapStyleInfo
import com.oterman.rundemo.domain.map.TrackColorSet
import com.oterman.rundemo.domain.map.TrackMapRenderer
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.presentation.feature.rundetail.components.MapTrackColors
import com.oterman.rundemo.presentation.feature.rundetail.components.calculateKilometerPositions
import com.oterman.rundemo.util.CoordinateConverter
import com.oterman.rundemo.util.RLog

private const val TAG = "AMapRenderer"

class AMapTrackMapRenderer : TrackMapRenderer {

    override val provider = MapProvider.AMAP

    override fun getAvailableStyles(isDarkTheme: Boolean): List<MapStyleInfo> = ALL_STYLES

    override fun getDefaultStyle(isDarkTheme: Boolean): MapStyleInfo {
        return if (isDarkTheme) STYLE_NIGHT else STYLE_NORMAL
    }

    override fun createMapView(context: Context): View {
        // 高德 SDK 隐私合规：必须在创建 MapView 前调用，否则报错 555570
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        return MapView(context).also { it.onCreate(null) }
    }

    override fun onStart(mapView: View) {
        RLog.d(TAG, "onStart → mapView.onResume() 调用")
        (mapView as MapView).onResume()
    }

    override fun onStop(mapView: View) {
        (mapView as MapView).onPause()
    }

    override fun onDestroy(mapView: View) {
        (mapView as MapView).onDestroy()
    }

    override fun configureGestures(mapView: View) {
        val aMap = getAMap(mapView) ?: return
        aMap.uiSettings.isRotateGesturesEnabled = false
        aMap.uiSettings.isTiltGesturesEnabled = false
        aMap.uiSettings.isScrollGesturesEnabled = true
        aMap.uiSettings.isZoomGesturesEnabled = true
    }

    override fun setupTouchInterception(mapView: View) {
        val aMap = getAMap(mapView) ?: return
        aMap.setOnMapTouchListener { event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN ->
                    (mapView.parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    (mapView.parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(false)
            }
        }
    }

    override fun hideScaleBar(mapView: View) {
        val aMap = getAMap(mapView) ?: return
        aMap.uiSettings.isScaleControlsEnabled = false
        aMap.uiSettings.isZoomControlsEnabled = false
    }

    override fun loadStyle(mapView: View, styleUri: String, onReady: () -> Unit) {
        val aMap = getAMap(mapView) ?: run {
            RLog.e(TAG, "loadStyle: getAMap 返回 null!")
            return
        }
        val newMapType = when (styleUri) {
            STYLE_URI_NORMAL -> AMap.MAP_TYPE_NORMAL
            STYLE_URI_SATELLITE -> AMap.MAP_TYPE_SATELLITE
            STYLE_URI_NIGHT -> AMap.MAP_TYPE_NIGHT
            STYLE_URI_NAVI -> AMap.MAP_TYPE_NAVI
            else -> AMap.MAP_TYPE_NORMAL
        }

        // 若 mapType 未变化，地图已就绪，直接回调，无需等待 OnMapLoadedListener
        if (aMap.mapType == newMapType) {
            RLog.d(TAG, "loadStyle: mapType 未变($newMapType)，直接调用 onReady()")
            onReady()
            return
        }

        RLog.d(TAG, "loadStyle: mapType 变化 ${aMap.mapType} -> $newMapType，注册 OnMapLoadedListener")
        aMap.mapType = newMapType

        var readyCalled = false
        aMap.setOnMapLoadedListener {
            if (!readyCalled) {
                readyCalled = true
                RLog.d(TAG, "OnMapLoadedListener 触发! 调用 onReady()")
                onReady()
            }
        }
        // 延迟后备：若 listener 不触发，主动调用 onReady
        (mapView as? MapView)?.postDelayed({
            if (!readyCalled) {
                readyCalled = true
                RLog.d(TAG, "OnMapLoadedListener 未触发，postDelayed 后备调用 onReady()")
                onReady()
            }
        }, 1000L)

        RLog.d(TAG, "loadStyle: cameraPosition=${aMap.cameraPosition}")
    }

    override fun renderTrack(mapView: View, trackPoints: List<TrackPoint>, colors: TrackColorSet) {
        val aMap = getAMap(mapView) ?: run {
            RLog.e(TAG, "renderTrack: getAMap 返回 null!")
            return
        }

        try {
            val validPoints = trackPoints.filter { it.isValidCoordinate() }
            RLog.d(TAG, "renderTrack: 开始, validPoints=${validPoints.size}")
            if (validPoints.isEmpty()) return

            // 转换坐标到 GCJ-02
            val gcjPoints = validPoints.map { pt ->
                val (lat, lon) = CoordinateConverter.wgs84ToGcj02(pt.latitude, pt.longitude)
                LatLng(lat, lon)
            }

            // 绘制轨迹线
            val polylineOptions = PolylineOptions()
                .addAll(gcjPoints)
                .width(MapTrackColors.TRACK_WIDTH.toFloat() * 2.5f)  // 高德线宽单位不同，需调整
                .color(android.graphics.Color.parseColor(colors.track))

            aMap.addPolyline(polylineOptions)

            // 起点标记
            val startBitmap = createCircleMarkerBitmap(
                android.graphics.Color.parseColor(colors.start),
                android.graphics.Color.parseColor(colors.stroke),
                (MapTrackColors.START_RADIUS * 3).toFloat(),
                (MapTrackColors.MARKER_STROKE_WIDTH * 2).toFloat()
            )
            aMap.addMarker(
                MarkerOptions()
                    .position(gcjPoints.first())
                    .icon(BitmapDescriptorFactory.fromBitmap(startBitmap))
                    .anchor(0.5f, 0.5f)
            )

            // 终点标记
            if (gcjPoints.size > 1) {
                val endBitmap = createCircleMarkerBitmap(
                    android.graphics.Color.parseColor(colors.end),
                    android.graphics.Color.parseColor(colors.stroke),
                    (MapTrackColors.END_RADIUS * 3).toFloat(),
                    (MapTrackColors.MARKER_STROKE_WIDTH * 2).toFloat()
                )
                aMap.addMarker(
                    MarkerOptions()
                        .position(gcjPoints.last())
                        .icon(BitmapDescriptorFactory.fromBitmap(endBitmap))
                        .anchor(0.5f, 0.5f)
                )
            }

            RLog.d(TAG, "轨迹添加成功, 点数: ${gcjPoints.size}")
        } catch (e: Exception) {
            RLog.e(TAG, "添加轨迹失败", e)
        }
    }

    override fun renderKmMarkers(
        mapView: View,
        trackPoints: List<TrackPoint>,
        colors: TrackColorSet,
        interval: Int,
        maxDistanceKm: Double?
    ) {
        val aMap = getAMap(mapView) ?: return

        try {
            val kmPositions = calculateKilometerPositions(
                trackPoints = trackPoints,
                interval = interval,
                maxDistanceKm = maxDistanceKm
            )
            RLog.d(TAG, "公里标记点数: ${kmPositions.size}, 间隔: ${interval}km")

            kmPositions.forEachIndexed { index, point ->
                val kmNumber = (index + 1) * interval
                val (lat, lon) = CoordinateConverter.wgs84ToGcj02(point.latitude, point.longitude)

                val bitmap = createKmBadgeBitmap(
                    kmNumber,
                    android.graphics.Color.parseColor(colors.kmBadgeBg),
                    android.graphics.Color.parseColor(colors.kmBadgeText),
                    android.graphics.Color.parseColor(colors.stroke)
                )
                aMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(lat, lon))
                        .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                        .anchor(0.5f, 0.5f)
                )
            }
        } catch (e: Exception) {
            RLog.e(TAG, "添加公里标记失败", e)
        }
    }

    override fun clearOverlays(mapView: View) {
        getAMap(mapView)?.clear()
    }

    override fun fitTrackBounds(mapView: View, trackPoints: List<TrackPoint>, padding: EdgePadding) {
        val aMap = getAMap(mapView) ?: run {
            RLog.e(TAG, "fitTrackBounds: getAMap 返回 null!")
            return
        }
        val validPoints = trackPoints.filter { it.isValidCoordinate() }
        RLog.d(TAG, "fitTrackBounds: validPoints=${validPoints.size}")
        if (validPoints.isEmpty()) return

        try {
            val boundsBuilder = LatLngBounds.Builder()
            validPoints.forEach { pt ->
                val (lat, lon) = CoordinateConverter.wgs84ToGcj02(pt.latitude, pt.longitude)
                boundsBuilder.include(LatLng(lat, lon))
            }
            val bounds = boundsBuilder.build()
            val cameraUpdate = CameraUpdateFactory.newLatLngBoundsRect(
                bounds,
                padding.left.toInt(),
                padding.right.toInt(),
                padding.top.toInt(),
                padding.bottom.toInt()
            )
            aMap.moveCamera(cameraUpdate)
        } catch (e: Exception) {
            RLog.e(TAG, "居中地图失败", e)
        }
    }

    override fun setCamera(mapView: View, state: MapCameraState) {
        val aMap = getAMap(mapView) ?: return
        // 注意：传入的是 WGS-84 坐标，需转换为 GCJ-02
        val (lat, lon) = CoordinateConverter.wgs84ToGcj02(state.centerLatitude, state.centerLongitude)
        aMap.moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition(LatLng(lat, lon), state.zoom.toFloat(), state.pitch.toFloat(), state.bearing.toFloat())
            )
        )
    }

    override fun getCameraState(mapView: View): MapCameraState? {
        return try {
            val aMap = getAMap(mapView) ?: return null
            val pos = aMap.cameraPosition
            // 高德返回的是 GCJ-02，转回 WGS-84 存储，避免 save/restore 循环中二次偏移
            val (wgsLat, wgsLon) = CoordinateConverter.gcj02ToWgs84(
                pos.target.latitude, pos.target.longitude
            )
            MapCameraState(
                centerLatitude = wgsLat,
                centerLongitude = wgsLon,
                zoom = pos.zoom.toDouble(),
                bearing = pos.bearing.toDouble(),
                pitch = pos.tilt.toDouble()
            )
        } catch (e: Exception) {
            RLog.e(TAG, "获取相机状态失败", e)
            null
        }
    }

    override fun snapshot(mapView: View, callback: (Bitmap?) -> Unit) {
        val aMap = getAMap(mapView) ?: run {
            callback(null)
            return
        }
        aMap.getMapScreenShot(object : AMap.OnMapScreenShotListener {
            override fun onMapScreenShot(bitmap: Bitmap?) {
                callback(bitmap)
            }

            override fun onMapScreenShot(bitmap: Bitmap?, status: Int) {
                callback(bitmap)
            }
        })
    }

    // ==================== 私有工具方法 ====================

    private fun getAMap(mapView: View): AMap? {
        return (mapView as? MapView)?.map
    }

    /** 创建圆形标记 Bitmap（起点/终点） */
    private fun createCircleMarkerBitmap(
        fillColor: Int,
        strokeColor: Int,
        radius: Float,
        strokeWidth: Float
    ): Bitmap {
        val size = ((radius + strokeWidth) * 2).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, radius + strokeWidth, strokePaint)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, radius, fillPaint)

        return bitmap
    }

    /** 创建公里标记 Bitmap（圆形背景 + 数字） */
    private fun createKmBadgeBitmap(
        kmNumber: Int,
        bgColor: Int,
        textColor: Int,
        strokeColor: Int
    ): Bitmap {
        val radius = (MapTrackColors.KM_BADGE_RADIUS * 3).toFloat()
        val strokeWidth = (MapTrackColors.KM_BADGE_STROKE_WIDTH * 2).toFloat()
        val size = ((radius + strokeWidth) * 2).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f

        // 边框
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, radius + strokeWidth, strokePaint)

        // 背景
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, radius, bgPaint)

        // 文字
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = radius * 0.85f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val textY = center - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText("$kmNumber", center, textY, textPaint)

        return bitmap
    }

    companion object {
        const val STYLE_URI_NORMAL = "amap://normal"
        const val STYLE_URI_SATELLITE = "amap://satellite"
        const val STYLE_URI_NIGHT = "amap://night"
        const val STYLE_URI_NAVI = "amap://navi"

        private val STYLE_NORMAL = MapStyleInfo("amap_normal", "普通", "标准地图", STYLE_URI_NORMAL, Icons.Default.Map)
        private val STYLE_SATELLITE = MapStyleInfo("amap_satellite", "卫星", "卫星影像", STYLE_URI_SATELLITE, Icons.Default.Satellite)
        private val STYLE_NIGHT = MapStyleInfo("amap_night", "夜间", "夜间模式", STYLE_URI_NIGHT, Icons.Default.DarkMode)
        private val STYLE_NAVI = MapStyleInfo("amap_navi", "导航", "导航风格", STYLE_URI_NAVI, Icons.Default.Navigation)

        val ALL_STYLES = listOf(STYLE_NORMAL, STYLE_SATELLITE, STYLE_NIGHT, STYLE_NAVI)
    }
}
