package com.oterman.fitdemo.ui.screen

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.gestures.gestures
import com.oterman.fitdemo.data.model.TrackPoint

private const val TAG = "MapScreen"

/**
 * 地图界面 - 显示跑步轨迹
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    trackPoints: List<TrackPoint>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("跑步轨迹") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MapViewComposable(
                trackPoints = trackPoints,
                context = context
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
    context: Context
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 创建MapView（Access Token从资源文件中自动读取）
    val mapView = remember {
        Log.d(TAG, "创建MapView, 轨迹点数: ${trackPoints.size}")
        MapView(context).apply {
            // 启用手势控制
            gestures.rotateEnabled = true
            gestures.pitchEnabled = true
            gestures.scrollEnabled = true
            gestures.doubleTapToZoomInEnabled = true
            gestures.pinchToZoomEnabled = true
        }
    }
    
    // 生命周期管理
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    Log.d(TAG, "MapView onStart")
                    mapView.onStart()
                }
                Lifecycle.Event.ON_STOP -> {
                    Log.d(TAG, "MapView onStop")
                    mapView.onStop()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    Log.d(TAG, "MapView onDestroy")
                    mapView.onDestroy()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    
    AndroidView(
        factory = {
            mapView.apply {
                Log.d(TAG, "初始化地图样式")
                mapboxMap.loadStyle(Style.STANDARD) { style ->
                    if (trackPoints.isNotEmpty()) {
                        Log.d(TAG, "添加轨迹到地图")
                        addTrackToMap(style, trackPoints)
                        centerMapOnTrack(this, trackPoints)
                    } else {
                        Log.w(TAG, "轨迹点为空")
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * 添加轨迹到地图
 */
private fun addTrackToMap(style: Style, trackPoints: List<TrackPoint>) {
    try {
        // 创建线条的坐标点列表
        val points = trackPoints.map { point ->
            Point.fromLngLat(point.longitude, point.latitude)
        }
        
        Log.d(TAG, "创建轨迹线，点数: ${points.size}")
        
        // 创建LineString
        val lineString = LineString.fromLngLats(points)
        
        // 添加轨迹线数据源
        style.addSource(
            geoJsonSource("track-source") {
                geometry(lineString)
            }
        )
        
        // 添加轨迹线图层
        style.addLayer(
            lineLayer("track-layer", "track-source") {
                lineColor("#0080FF")  // 蓝色
                lineWidth(5.0)
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
            }
        )
        
        // 添加起点标记
        if (trackPoints.isNotEmpty()) {
            val startPoint = trackPoints.first()
            style.addSource(
                geoJsonSource("start-point-source") {
                    geometry(Point.fromLngLat(startPoint.longitude, startPoint.latitude))
                }
            )
            
            style.addLayer(
                circleLayer("start-point-layer", "start-point-source") {
                    circleRadius(10.0)
                    circleColor("#00FF00")  // 绿色
                    circleStrokeWidth(3.0)
                    circleStrokeColor("#FFFFFF")
                }
            )
        }
        
        // 添加终点标记
        if (trackPoints.size > 1) {
            val endPoint = trackPoints.last()
            style.addSource(
                geoJsonSource("end-point-source") {
                    geometry(Point.fromLngLat(endPoint.longitude, endPoint.latitude))
                }
            )
            
            style.addLayer(
                circleLayer("end-point-layer", "end-point-source") {
                    circleRadius(10.0)
                    circleColor("#FF0000")  // 红色
                    circleStrokeWidth(3.0)
                    circleStrokeColor("#FFFFFF")
                }
            )
        }
        
        Log.d(TAG, "轨迹添加成功")
    } catch (e: Exception) {
        Log.e(TAG, "添加轨迹失败", e)
    }
}

/**
 * 将地图居中到轨迹
 */
private fun centerMapOnTrack(mapView: MapView, trackPoints: List<TrackPoint>) {
    if (trackPoints.isEmpty()) return
    
    try {
        // 计算边界
        var minLat = trackPoints.first().latitude
        var maxLat = trackPoints.first().latitude
        var minLon = trackPoints.first().longitude
        var maxLon = trackPoints.first().longitude
        
        trackPoints.forEach { point ->
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
        
        // 根据跨度计算zoom level（粗略估算）
        val zoom = when {
            maxDiff > 0.1 -> 11.0
            maxDiff > 0.05 -> 12.0
            maxDiff > 0.01 -> 13.0
            maxDiff > 0.005 -> 14.0
            else -> 15.0
        }
        
        Log.d(TAG, "居中地图: lat=$centerLat, lon=$centerLon, zoom=$zoom")
        
        // 设置相机位置
        val cameraOptions = cameraOptions {
            center(Point.fromLngLat(centerLon, centerLat))
            zoom(zoom)
            pitch(0.0)
        }
        
        mapView.mapboxMap.setCamera(cameraOptions)
    } catch (e: Exception) {
        Log.e(TAG, "居中地图失败", e)
    }
}

