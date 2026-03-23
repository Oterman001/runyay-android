package com.oterman.rundemo.presentation.feature.rundetail.components

import android.content.Context
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.oterman.rundemo.R
import com.oterman.rundemo.data.map.MapRendererFactory
import com.oterman.rundemo.domain.map.EdgePadding
import com.oterman.rundemo.domain.map.MapCameraState
import com.oterman.rundemo.domain.map.MapProvider
import com.oterman.rundemo.domain.map.TrackColorSet
import com.oterman.rundemo.domain.map.TrackMapRenderer
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.util.RLog
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
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
    const val START_RADIUS = 5.0
    const val END_RADIUS = 5.0
    const val MARKER_STROKE_WIDTH = 1.5

    // 公里标记大小
    const val KM_BADGE_RADIUS = 8.0
    const val KM_BADGE_STROKE_WIDTH = 1.0

    // 亮色主题颜色 (匹配iOS)
    object Light {
        const val TRACK = "#FB7B26"           // 橙色轨迹
        const val START = "#34A853"           // 绿色起点
        const val END = "#EA4335"             // 红色终点
        const val MARKER_STROKE = "#FFFFFF"   // 白色边框
        const val KM_BADGE_BG = "#222638"     // 深蓝灰色背景
        const val KM_BADGE_TEXT = "#FFFFFF"   // 白色文字
    }

    // 暗色主题颜色 (匹配iOS)
    object Dark {
        const val TRACK = "#DDFF04"           // 黄绿色轨迹
        const val START = "#4CAF50"           // 绿色起点
        const val END = "#F44336"             // 红色终点
        const val MARKER_STROKE = "#FFFFFF"   // 白色边框
        const val KM_BADGE_BG = "#222638"     // 深蓝灰色背景
        const val KM_BADGE_TEXT = "#FFFFFF"   // 白色文字
    }
}

/**
 * 根据主题获取轨迹颜色
 */
@Composable
fun getTrackColors(): TrackColorSet {
    val isDarkTheme = RunTheme.isDark
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
 * 支持 Mapbox 和高德地图供应商切换
 */
@Composable
fun RunDetailMapSection(
    trackPoints: List<TrackPoint>,
    isOutdoor: Boolean,
    actualDistanceKm: Double? = null,
    modifier: Modifier = Modifier,
    savedCameraState: MapCameraState? = null,
    onCameraChanged: (MapCameraState) -> Unit = {},
    onMapViewReady: (View) -> Unit = {}
) {
    val context = LocalContext.current
    val isDarkTheme = RunTheme.isDark
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val mapHeight = screenHeight * RunDetailLayoutConstants.MapHeightRatio

    var currentProvider by remember { mutableStateOf(RunMapPreferences.getMapProvider(context)) }
    val renderer = remember(currentProvider) { MapRendererFactory.getRenderer(currentProvider) }
    var currentStyle by remember(currentProvider) {
        mutableStateOf(RunMapPreferences.getMapStyle(context, isDarkTheme, renderer))
    }
    var showKmMarkers by remember { mutableStateOf(RunMapPreferences.getShowKmMarkers(context)) }
    var kmMarkerInterval by remember { mutableIntStateOf(RunMapPreferences.getKmMarkerInterval(context)) }
    var privacyMode by remember { mutableStateOf(RunMapPreferences.getPrivacyMode(context)) }
    var showSettingSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight)
    ) {
        if (isOutdoor && trackPoints.isNotEmpty()) {
            if (privacyMode) {
                PrivacyTrackView(
                    trackPoints = trackPoints,
                    showKmMarkers = showKmMarkers,
                    kmMarkerInterval = kmMarkerInterval,
                    actualDistanceKm = actualDistanceKm
                )
            } else {
                // key(currentProvider) 确保切换供应商时重建地图
                androidx.compose.runtime.key(currentProvider) {
                    MapViewComposable(
                        trackPoints = trackPoints,
                        context = context,
                        renderer = renderer,
                        styleUri = currentStyle,
                        showKmMarkers = showKmMarkers,
                        kmMarkerInterval = kmMarkerInterval,
                        actualDistanceKm = actualDistanceKm,
                        savedCameraState = savedCameraState,
                        onCameraChanged = onCameraChanged,
                        onMapViewReady = onMapViewReady
                    )
                }
            }

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

            Surface(
                onClick = { showSettingSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 40.dp)
                    .size(36.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.40f),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.22f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "地图设置",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
            }

            if (showSettingSheet) {
                RunMapSettingBottomSheet(
                    currentProvider = currentProvider,
                    currentStyleUri = currentStyle,
                    showKmMarkers = showKmMarkers,
                    kmMarkerInterval = kmMarkerInterval,
                    privacyMode = privacyMode,
                    renderer = renderer,
                    onProviderChanged = { newProvider ->
                        currentProvider = newProvider
                        RunMapPreferences.saveMapProvider(context, newProvider)
                        val newRenderer = MapRendererFactory.getRenderer(newProvider)
                        currentStyle = newRenderer.getDefaultStyle(isDarkTheme).styleUri
                        RunMapPreferences.saveMapStyle(context, isDarkTheme, currentStyle)
                    },
                    onStyleSelected = { styleUri ->
                        currentStyle = styleUri
                        RunMapPreferences.saveMapStyle(context, isDarkTheme, styleUri)
                    },
                    onKmMarkersToggled = { show ->
                        showKmMarkers = show
                        RunMapPreferences.saveShowKmMarkers(context, show)
                    },
                    onKmIntervalChanged = { interval ->
                        kmMarkerInterval = interval
                        RunMapPreferences.saveKmMarkerInterval(context, interval)
                    },
                    onPrivacyModeToggled = { enabled ->
                        privacyMode = enabled
                        RunMapPreferences.savePrivacyMode(context, enabled)
                    },
                    onDismiss = { showSettingSheet = false }
                )
            }
        } else {
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
               painter = painterResource(id = R.drawable.figure_run_treadmill),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
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
 * 地图视图 Composable 包装（供应商无关）
 */
@Composable
private fun MapViewComposable(
    trackPoints: List<TrackPoint>,
    context: Context,
    renderer: TrackMapRenderer,
    styleUri: String,
    showKmMarkers: Boolean,
    kmMarkerInterval: Int,
    actualDistanceKm: Double? = null,
    savedCameraState: MapCameraState? = null,
    onCameraChanged: (MapCameraState) -> Unit = {},
    onMapViewReady: (View) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val trackColors by rememberUpdatedState(getTrackColors())

    val mapView = remember {
        RLog.d(TAG, "创建MapView(${renderer.provider}), 轨迹点数: ${trackPoints.size}")
        renderer.createMapView(context).also { view ->
            renderer.configureGestures(view)
            renderer.setupTouchInterception(view)
            renderer.hideScaleBar(view)
        }
    }

    // 将 MapView 引用传递给调用方
    onMapViewReady(mapView)

    // 追踪参数变化，避免滚动recomposition时无条件重置地图
    var lastStyleUri by remember { mutableStateOf(styleUri) }
    var lastShowKmMarkers by remember { mutableStateOf(showKmMarkers) }
    var lastKmMarkerInterval by remember { mutableIntStateOf(kmMarkerInterval) }

    DisposableEffect(lifecycleOwner) {
        var isDestroyed = false
        RLog.d("RunDetailPerf", "MapView DisposableEffect setup (${renderer.provider})")

        val observer = LifecycleEventObserver { _, event ->
            val startTime = System.currentTimeMillis()
            when (event) {
                Lifecycle.Event.ON_START -> {
                    renderer.onStart(mapView)
                    RLog.d("RunDetailPerf", "MapView.onStart() cost=${System.currentTimeMillis() - startTime}ms")
                }
                Lifecycle.Event.ON_STOP -> {
                    renderer.onStop(mapView)
                    RLog.d("RunDetailPerf", "MapView.onStop() cost=${System.currentTimeMillis() - startTime}ms")
                }
                Lifecycle.Event.ON_DESTROY -> {
                    if (!isDestroyed) {
                        isDestroyed = true
                        renderer.onDestroy(mapView)
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
            // 保存相机状态，以便地图重建时恢复
            try {
                renderer.getCameraState(mapView)?.let { state ->
                    onCameraChanged(state)
                }
            } catch (e: Exception) {
                RLog.e("RunDetailPerf", "保存相机状态失败", e)
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!isDestroyed) {
                isDestroyed = true
                renderer.onDestroy(mapView)
                RLog.d("RunDetailPerf", "MapView.onDestroy(dispose) cost=${System.currentTimeMillis() - startTime}ms")
            }
            RLog.d("RunDetailPerf", "MapView onDispose END, total=${System.currentTimeMillis() - startTime}ms")
        }
    }

    val defaultPadding = EdgePadding(180.0, 100.0, 200.0, 100.0)

    AndroidView(
        factory = {
            mapView.apply {
                RLog.d(TAG, "初始化地图样式: $styleUri, 有保存的相机状态: ${savedCameraState != null}")
                renderer.loadStyle(this, styleUri) {
                    if (trackPoints.isNotEmpty()) {
                        RLog.d(TAG, "添加轨迹到地图")
                        renderer.renderTrack(this, trackPoints, trackColors)
                        if (showKmMarkers) {
                            renderer.renderKmMarkers(
                                this,
                                trackPoints,
                                trackColors,
                                kmMarkerInterval,
                                actualDistanceKm
                            )
                        }
                        if (savedCameraState != null) {
                            renderer.setCamera(this, savedCameraState)
                        } else {
                            renderer.fitTrackBounds(this, trackPoints, defaultPadding)
                        }
                    } else {
                        RLog.w(TAG, "轨迹点为空")
                    }
                }
            }
        },
        update = { view ->
            val styleChanged = styleUri != lastStyleUri
            val kmChanged = showKmMarkers != lastShowKmMarkers || kmMarkerInterval != lastKmMarkerInterval

            if (styleChanged) {
                // 样式变化：全量重加载，重置镜头
                RLog.d(TAG, "地图样式变化, 全量重加载")
                lastStyleUri = styleUri
                lastShowKmMarkers = showKmMarkers
                lastKmMarkerInterval = kmMarkerInterval

                renderer.clearOverlays(view)
                renderer.loadStyle(view, styleUri) {
                    if (trackPoints.isNotEmpty()) {
                        renderer.renderTrack(view, trackPoints, trackColors)
                        if (showKmMarkers) {
                            renderer.renderKmMarkers(
                                view,
                                trackPoints,
                                trackColors,
                                kmMarkerInterval,
                                actualDistanceKm
                            )
                        }
                        renderer.fitTrackBounds(view, trackPoints, defaultPadding)
                    }
                }
            } else if (kmChanged) {
                // 仅公里标记变化：直接清除并重渲染，不走 loadStyle 回调链，保留当前镜头位置
                RLog.d(TAG, "公里标记参数变化, 直接重渲染, showKmMarkers=$showKmMarkers, interval=$kmMarkerInterval")
                lastShowKmMarkers = showKmMarkers
                lastKmMarkerInterval = kmMarkerInterval

                renderer.clearOverlays(view)
                if (trackPoints.isNotEmpty()) {
                    renderer.renderTrack(view, trackPoints, trackColors)
                    if (showKmMarkers) {
                        renderer.renderKmMarkers(
                            view,
                            trackPoints,
                            trackColors,
                            kmMarkerInterval,
                            actualDistanceKm
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ==================== 工具函数 ====================

/**
 * 计算公里标记位置点
 * @param interval 公里间隔（例如 2 表示每 2km 一个标记）
 */
fun calculateKilometerPositions(
    trackPoints: List<TrackPoint>,
    interval: Int = 1,
    maxDistanceKm: Double? = null
): List<TrackPoint> {
    val validPoints = trackPoints.filter { it.isValidCoordinate() }
    if (validPoints.size < 2) return emptyList()

    val intervalKm = interval.coerceIn(1, 10)
    val step = intervalKm.toDouble()
    val kmPositions = mutableListOf<TrackPoint>()
    var accumulatedDistance = 0.0
    var nextKmThreshold = step

    for (i in 1 until validPoints.size) {
        val prev = validPoints[i - 1]
        val curr = validPoints[i]

        val segmentDistance = haversineDistance(
            prev.latitude, prev.longitude,
            curr.latitude, curr.longitude
        )

        if (segmentDistance > 0.5) {
            RLog.w(TAG, "跳过异常大的轨迹段[i=$i]: prev=(${prev.latitude},${prev.longitude}), curr=(${curr.latitude},${curr.longitude}), dist=${String.format("%.3f", segmentDistance)}km, accumulated=${String.format("%.3f", accumulatedDistance)}km, 不计入累积距离")
            continue
        }

        val prevAccumulated = accumulatedDistance
        accumulatedDistance += segmentDistance

        while (accumulatedDistance >= nextKmThreshold && segmentDistance > 0) {
            val distanceToThreshold = nextKmThreshold - prevAccumulated
            val ratio = distanceToThreshold / segmentDistance

            val kmPoint = TrackPoint(
                latitude = prev.latitude + ratio * (curr.latitude - prev.latitude),
                longitude = prev.longitude + ratio * (curr.longitude - prev.longitude)
            )
            kmPositions.add(kmPoint)
            nextKmThreshold += step
        }
    }

    val filteredPositions = if (maxDistanceKm != null) {
        val maxKmNumber = floor(maxDistanceKm.coerceAtLeast(0.0)).toInt()
        kmPositions.filterIndexed { index, _ ->
            val kmNumber = (index + 1) * intervalKm
            kmNumber <= maxKmNumber
        }.also { filtered ->
            RLog.d(
                TAG,
                "公里点上限过滤: actualDistanceKm=${String.format("%.3f", maxDistanceKm)}, maxKmNumber=$maxKmNumber, before=${kmPositions.size}, after=${filtered.size}, interval=$intervalKm"
            )
        }
    } else {
        kmPositions
    }

    RLog.d(
        TAG,
        "calculateKmPositions完成: totalDist=${String.format("%.3f", accumulatedDistance)}km, positions=${filteredPositions.size}, interval=$intervalKm"
    )
    return filteredPositions
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
