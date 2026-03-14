package com.oterman.rundemo.domain.map

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import com.oterman.rundemo.domain.model.TrackPoint

/**
 * 地图轨迹渲染器策略接口
 * 每个地图供应商实现此接口，Composable层通过接口操作地图
 */
interface TrackMapRenderer {

    val provider: MapProvider

    /** 获取当前供应商可用的地图样式 */
    fun getAvailableStyles(isDarkTheme: Boolean): List<MapStyleInfo>

    /** 获取当前供应商的默认样式 */
    fun getDefaultStyle(isDarkTheme: Boolean): MapStyleInfo

    /** 创建地图视图 */
    fun createMapView(context: Context): View

    /** 生命周期管理 */
    fun onStart(mapView: View)
    fun onStop(mapView: View)
    fun onDestroy(mapView: View)

    /** 配置手势（禁用旋转/倾斜等） */
    fun configureGestures(mapView: View)

    /** 设置触摸事件拦截（处理嵌套滚动） */
    fun setupTouchInterception(mapView: View)

    /** 隐藏比例尺 */
    fun hideScaleBar(mapView: View)

    /** 加载地图样式 */
    fun loadStyle(mapView: View, styleUri: String, onReady: () -> Unit)

    /** 渲染轨迹线 + 起终点标记 */
    fun renderTrack(mapView: View, trackPoints: List<TrackPoint>, colors: TrackColorSet)

    /** 渲染公里标记 */
    fun renderKmMarkers(mapView: View, trackPoints: List<TrackPoint>, colors: TrackColorSet, interval: Int)

    /** 清除所有轨迹覆盖物 */
    fun clearOverlays(mapView: View)

    /** 将相机适配到轨迹边界 */
    fun fitTrackBounds(mapView: View, trackPoints: List<TrackPoint>, padding: EdgePadding)

    /** 设置相机状态 */
    fun setCamera(mapView: View, state: MapCameraState)

    /** 获取当前相机状态 */
    fun getCameraState(mapView: View): MapCameraState?

    /** 截图 */
    fun snapshot(mapView: View, callback: (Bitmap?) -> Unit)
}
