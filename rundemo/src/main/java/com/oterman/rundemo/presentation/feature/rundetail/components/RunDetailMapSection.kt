package com.oterman.rundemo.presentation.feature.rundetail.components

import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
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
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants
import com.oterman.rundemo.util.RLog
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "RunDetailMapSection"

// ==================== 颜色常量 (匹配iOS) ====================

/**
 * 地图轨迹颜色常量
 * 支持亮色/暗色主题
 */
object MapTrackColors {
    // 轨迹线宽度
    const val TRACK_WIDTH = 4.0

    // 起点/终点标记大小
    const val START_RADIUS = 8.0
    const val END_RADIUS = 8.0
    const val MARKER_STROKE_WIDTH = 2.0

    // 公里标记大小
    const val KM_BADGE_RADIUS = 12.0
    const val KM_BADGE_STROKE_WIDTH = 1.5

    // 亮色主题颜色 (匹配iOS)
    object Light {
        const val TRACK = "#FB7B26"           // 橙色轨迹
        const val START = "#008F00"           // 绿色起点
        const val END = "#941652"             // 深红色终点
        const val MARKER_STROKE = "#FFFFFF"   // 白色边框
        const val KM_BADGE_BG = "#222638"     // 深蓝灰色背景
        const val KM_BADGE_TEXT = "#FFFFFF"   // 白色文字
    }

    // 暗色主题颜色 (匹配iOS)
    object Dark {
        const val TRACK = "#DDFF04"           // 黄绿色轨迹
        const val START = "#73FA79"           // 亮绿色起点
        const val END = "#FF2F92"             // 洋红色终点
        const val MARKER_STROKE = "#FFFFFF"   // 白色边框
        const val KM_BADGE_BG = "#222638"     // 深蓝灰色背景
        const val KM_BADGE_TEXT = "#FFFFFF"   // 白色文字
    }
}

/**
 * 轨迹颜色集合
 */
data class TrackColorSet(
    val track: String,
    val start: String,
    val end: String,
    val stroke: String,
    val kmBadgeBg: String,
    val kmBadgeText: String
)

/**
 * 根据主题获取轨迹颜色
 */
@Composable
fun getTrackColors(): TrackColorSet {
    val isDarkTheme = isSystemInDarkTheme()
    return if (isDarkTheme) {
        TrackColorSet(
            track = MapTrackColors.Dark.TRACK,
            start = MapTrackColors.Dark.START,
            end = MapTrackColors.Dark.END,
            stroke = MapTrackColors.Dark.MARKER_STROKE,
            kmBadgeBg = MapTrackColors.Dark.KM_BADGE_BG,
            kmBadgeText = MapTrackColors.Dark.KM_BADGE_TEXT
        )
    } else {
        TrackColorSet(
            track = MapTrackColors.Light.TRACK,
            start = MapTrackColors.Light.START,
            end = MapTrackColors.Light.END,
            stroke = MapTrackColors.Light.MARKER_STROKE,
            kmBadgeBg = MapTrackColors.Light.KM_BADGE_BG,
            kmBadgeText = MapTrackColors.Light.KM_BADGE_TEXT
        )
    }
}

// ==================== 主组件 ====================

/**
 * 跑步详情页地图区域
 * 显示GPS轨迹，带底部渐变遮罩和地图风格切换
 */
@Composable
fun RunDetailMapSection(
    trackPoints: List<TrackPoint>,
    isOutdoor: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val mapHeight = screenHeight * RunDetailLayoutConstants.MapHeightRatio

    // 地图风格状态
    var currentStyle by remember { mutableStateOf(RunMapPreferences.getMapStyle(context)) }
    var showStyleSelector by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight)
    ) {
        if (isOutdoor && trackPoints.isNotEmpty()) {
            // 户外跑 - 显示地图
            MapViewComposable(
                trackPoints = trackPoints,
                context = context,
                styleUri = currentStyle
            )

            // 底部渐变遮罩
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(RunDetailLayoutConstants.MapGradientHeight.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            // 地图风格切换按钮 (小按钮，右下角，避开底部渐变)
            SmallFloatingActionButton(
                onClick = { showStyleSelector = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 80.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "切换地图风格",
                    modifier = Modifier.size(18.dp)
                )
            }

            // 地图风格选择器
            if (showStyleSelector) {
                RunMapStyleBottomSheet(
                    currentStyleUri = currentStyle,
                    onStyleSelected = { styleUri ->
                        currentStyle = styleUri
                        RunMapPreferences.saveMapStyle(context, styleUri)
                        showStyleSelector = false
                    },
                    onDismiss = { showStyleSelector = false }
                )
            }
        } else {
            // 室内跑 - 显示占位符
            IndoorRunPlaceholder()
        }
    }
}

/**
 * 室内跑占位符
 */
@Composable
private fun IndoorRunPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.DirectionsRun,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "室内跑",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Mapbox MapView Composable包装
 */
@Composable
private fun MapViewComposable(
    trackPoints: List<TrackPoint>,
    context: Context,
    styleUri: String
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDarkTheme = isSystemInDarkTheme()

    // 使用rememberUpdatedState确保颜色始终最新
    val trackColors by rememberUpdatedState(getTrackColors())

    // 创建MapView
    val mapView = remember {
        RLog.d(TAG, "创建MapView, 轨迹点数: ${trackPoints.size}")
        MapView(context).apply {
            // 启用所有需要的手势
            gestures.rotateEnabled = false       // 禁用旋转
            gestures.pitchEnabled = false        // 禁用倾斜
            gestures.scrollEnabled = true        // 启用平移
            gestures.doubleTapToZoomInEnabled = true  // 启用双击缩放
            gestures.pinchToZoomEnabled = true   // 启用捏合缩放

            // 隐藏比例尺
            scalebar.enabled = false

            // 触摸时通知父容器不要拦截，让地图处理手势
            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 触摸开始，告诉父容器不要拦截
                        (view.parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // 触摸结束，恢复父容器拦截
                        (view.parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false // 返回false让MapView继续处理触摸事件
            }
        }
    }

    // 生命周期管理 - 使用标志防止重复销毁
    DisposableEffect(lifecycleOwner) {
        var isDestroyed = false
        RLog.d("RunDetailPerf", "MapView DisposableEffect setup")

        val observer = LifecycleEventObserver { _, event ->
            val startTime = System.currentTimeMillis()
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mapView.onStart()
                    RLog.d("RunDetailPerf", "MapView.onStart() cost=${System.currentTimeMillis() - startTime}ms")
                }
                Lifecycle.Event.ON_STOP -> {
                    mapView.onStop()
                    RLog.d("RunDetailPerf", "MapView.onStop() cost=${System.currentTimeMillis() - startTime}ms")
                }
                Lifecycle.Event.ON_DESTROY -> {
                    if (!isDestroyed) {
                        isDestroyed = true
                        mapView.onDestroy()
                        RLog.d("RunDetailPerf", "MapView.onDestroy(lifecycle) cost=${System.currentTimeMillis() - startTime}ms")
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            val startTime = System.currentTimeMillis()
            RLog.d("RunDetailPerf", "MapView onDispose START")
            lifecycleOwner.lifecycle.removeObserver(observer)
            // 仅在生命周期观察者未触发销毁时（如配置更改）才在这里销毁
            if (!isDestroyed) {
                isDestroyed = true
                mapView.onDestroy()
                RLog.d("RunDetailPerf", "MapView.onDestroy(dispose) cost=${System.currentTimeMillis() - startTime}ms")
            }
            RLog.d("RunDetailPerf", "MapView onDispose END, total=${System.currentTimeMillis() - startTime}ms")
        }
    }

    // 当styleUri变化时重新加载地图
    AndroidView(
        factory = {
            mapView.apply {
                RLog.d(TAG, "初始化地图样式: $styleUri")
                mapboxMap.loadStyle(styleUri) { style ->
                    if (trackPoints.isNotEmpty()) {
                        RLog.d(TAG, "添加轨迹到地图")
                        addTrackToMap(style, trackPoints, trackColors)
                        addKilometerMarkers(style, trackPoints, trackColors)
                        centerMapOnTrack(this, trackPoints)
                    } else {
                        RLog.w(TAG, "轨迹点为空")
                    }
                }
            }
        },
        update = { view ->
            RLog.d(TAG, "更新地图样式: $styleUri, isDark: $isDarkTheme")
            view.mapboxMap.loadStyle(styleUri) { style ->
                if (trackPoints.isNotEmpty()) {
                    addTrackToMap(style, trackPoints, trackColors)
                    addKilometerMarkers(style, trackPoints, trackColors)
                    centerMapOnTrack(view, trackPoints)
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * 添加轨迹到地图
 */
private fun addTrackToMap(
    style: Style,
    trackPoints: List<TrackPoint>,
    colors: TrackColorSet
) {
    try {
        // 过滤有效坐标点
        val validPoints = trackPoints.filter { it.isValidCoordinate() }
        if (validPoints.isEmpty()) {
            RLog.w(TAG, "没有有效的轨迹点")
            return
        }

        // 创建线条的坐标点列表
        val points = validPoints.map { point ->
            Point.fromLngLat(point.longitude, point.latitude)
        }

        RLog.d(TAG, "创建轨迹线，点数: ${points.size}")

        // 创建LineString
        val lineString = LineString.fromLngLats(points)

        // 添加轨迹线数据源
        style.addSource(
            geoJsonSource("track-source") {
                geometry(lineString)
            }
        )

        // 添加轨迹线图层 (使用主题颜色)
        style.addLayer(
            lineLayer("track-layer", "track-source") {
                lineColor(colors.track)  // 亮色:#FB7B26(橙) / 暗色:#DDFF04(黄绿)
                lineWidth(MapTrackColors.TRACK_WIDTH)
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
            }
        )

        // 添加起点标记 (使用主题颜色)
        val startPoint = validPoints.first()
        style.addSource(
            geoJsonSource("start-point-source") {
                geometry(Point.fromLngLat(startPoint.longitude, startPoint.latitude))
            }
        )

        style.addLayer(
            circleLayer("start-point-layer", "start-point-source") {
                circleRadius(MapTrackColors.START_RADIUS)
                circleColor(colors.start)  // 亮色:#008F00(绿) / 暗色:#73FA79(亮绿)
                circleStrokeWidth(MapTrackColors.MARKER_STROKE_WIDTH)
                circleStrokeColor(colors.stroke)
            }
        )

        // 添加终点标记 (使用主题颜色)
        if (validPoints.size > 1) {
            val endPoint = validPoints.last()
            style.addSource(
                geoJsonSource("end-point-source") {
                    geometry(Point.fromLngLat(endPoint.longitude, endPoint.latitude))
                }
            )

            style.addLayer(
                circleLayer("end-point-layer", "end-point-source") {
                    circleRadius(MapTrackColors.END_RADIUS)
                    circleColor(colors.end)  // 亮色:#941652(深红) / 暗色:#FF2F92(洋红)
                    circleStrokeWidth(MapTrackColors.MARKER_STROKE_WIDTH)
                    circleStrokeColor(colors.stroke)
                }
            )
        }

        RLog.d(TAG, "轨迹添加成功")
    } catch (e: Exception) {
        RLog.e(TAG, "添加轨迹失败", e)
    }
}

/**
 * 添加公里标记
 */
private fun addKilometerMarkers(
    style: Style,
    trackPoints: List<TrackPoint>,
    colors: TrackColorSet
) {
    try {
        val kmPositions = calculateKilometerPositions(trackPoints)
        RLog.d(TAG, "公里标记点数: ${kmPositions.size}")

        kmPositions.forEachIndexed { index, point ->
            val kmNumber = index + 1
            val sourceId = "km-marker-source-$kmNumber"
            val bgLayerId = "km-marker-bg-layer-$kmNumber"
            val textLayerId = "km-marker-text-layer-$kmNumber"

            // 添加公里标记数据源
            style.addSource(
                geoJsonSource(sourceId) {
                    geometry(Point.fromLngLat(point.longitude, point.latitude))
                }
            )

            // 圆形背景
            style.addLayer(
                circleLayer(bgLayerId, sourceId) {
                    circleRadius(MapTrackColors.KM_BADGE_RADIUS)
                    circleColor(colors.kmBadgeBg)
                    circleStrokeWidth(MapTrackColors.KM_BADGE_STROKE_WIDTH)
                    circleStrokeColor(colors.stroke)
                }
            )

            // 公里数字文本
            style.addLayer(
                symbolLayer(textLayerId, sourceId) {
                    textField("$kmNumber")
                    textSize(11.0)
                    textColor(colors.kmBadgeText)
                    textAllowOverlap(true)
                    textIgnorePlacement(true)
                }
            )
        }

        RLog.d(TAG, "公里标记添加成功")
    } catch (e: Exception) {
        RLog.e(TAG, "添加公里标记失败", e)
    }
}

/**
 * 计算每公里位置点
 * 使用线性插值计算精确的公里位置
 */
private fun calculateKilometerPositions(trackPoints: List<TrackPoint>): List<TrackPoint> {
    val validPoints = trackPoints.filter { it.isValidCoordinate() }
    if (validPoints.size < 2) return emptyList()

    val kmPositions = mutableListOf<TrackPoint>()
    var accumulatedDistance = 0.0
    var nextKmThreshold = 1.0  // 第一个公里标记在1km处

    for (i in 1 until validPoints.size) {
        val prev = validPoints[i - 1]
        val curr = validPoints[i]

        val segmentDistance = haversineDistance(
            prev.latitude, prev.longitude,
            curr.latitude, curr.longitude
        )

        val prevAccumulated = accumulatedDistance
        accumulatedDistance += segmentDistance

        // 检查是否跨过公里阈值
        while (accumulatedDistance >= nextKmThreshold && segmentDistance > 0) {
            // 计算插值比例
            val distanceToThreshold = nextKmThreshold - prevAccumulated
            val ratio = distanceToThreshold / segmentDistance

            // 线性插值计算精确位置
            val kmPoint = TrackPoint(
                latitude = prev.latitude + ratio * (curr.latitude - prev.latitude),
                longitude = prev.longitude + ratio * (curr.longitude - prev.longitude)
            )
            kmPositions.add(kmPoint)
            nextKmThreshold += 1.0
        }
    }

    return kmPositions
}

/**
 * Haversine公式计算两点间距离
 * @return 距离(公里)
 */
private fun haversineDistance(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val R = 6371.0 // 地球半径(公里)
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

/**
 * 将地图居中到轨迹
 */
private fun centerMapOnTrack(mapView: MapView, trackPoints: List<TrackPoint>) {
    val validPoints = trackPoints.filter { it.isValidCoordinate() }
    if (validPoints.isEmpty()) return

    try {
        // 计算边界
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

        // 计算中心点
        val centerLat = (minLat + maxLat) / 2
        val centerLon = (minLon + maxLon) / 2

        // 计算合适的缩放级别
        val latDiff = maxLat - minLat
        val lonDiff = maxLon - minLon
        val maxDiff = maxOf(latDiff, lonDiff)

        // 根据跨度计算zoom level
        val zoom = when {
            maxDiff > 0.1 -> 11.0
            maxDiff > 0.05 -> 12.0
            maxDiff > 0.01 -> 13.0
            maxDiff > 0.005 -> 14.0
            else -> 15.0
        }

        RLog.d(TAG, "居中地图: lat=$centerLat, lon=$centerLon, zoom=$zoom")

        // 设置相机位置
        val cameraOptions = cameraOptions {
            center(Point.fromLngLat(centerLon, centerLat))
            zoom(zoom)
            pitch(0.0)
        }

        mapView.mapboxMap.setCamera(cameraOptions)
    } catch (e: Exception) {
        RLog.e(TAG, "居中地图失败", e)
    }
}
