package com.oterman.rundemo.presentation.components.trajectory

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.oterman.rundemo.domain.model.TrackPoint
import kotlin.math.max
import kotlin.math.min

/**
 * 轨迹缩略图渲染器
 * 将GPS轨迹点渲染为Bitmap
 * 参考iOS TrajectoryDisplayManager的渲染逻辑
 */
object TrajectoryRenderer {

    /**
     * 轨迹渲染颜色配置
     */
    private data class ColorScheme(
        val track: Int,
        val start: Int,
        val end: Int,
        val background: Int
    )

    // 亮色主题
    private val LightColors = ColorScheme(
        track = 0xFFFB7B26.toInt(),      // 橙色轨迹
        start = 0xFF008F00.toInt(),      // 绿色起点
        end = 0xFF941652.toInt(),        // 深红色终点
        background = 0xFFF5F5F5.toInt()  // 浅灰背景
    )

    // 暗色主题
    private val DarkColors = ColorScheme(
        track = 0xFFDDFF04.toInt(),      // 黄绿色轨迹
        start = 0xFF73FA79.toInt(),      // 亮绿色起点
        end = 0xFFFF2F92.toInt(),        // 洋红色终点
        background = 0xFF2A2A2E.toInt()  // 深灰背景
    )

    // 渲染参数
    private const val TRACK_WIDTH_RATIO = 0.03f      // 轨迹线宽度占画布的比例
    private const val MARKER_RADIUS_RATIO = 0.04f    // 起终点标记半径占画布的比例
    private const val PADDING_RATIO = 0.12f          // 内边距占画布的比例

    /**
     * 渲染轨迹缩略图
     *
     * @param trackPoints 轨迹点列表
     * @param size 输出图片尺寸（像素，正方形）
     * @param isDark 是否为暗色主题
     * @return 渲染后的Bitmap，如果轨迹点不足则返回null
     */
    fun render(
        trackPoints: List<TrackPoint>,
        size: Int,
        isDark: Boolean,
        trackColorOverride: Int? = null
    ): Bitmap? {
        // 过滤有效坐标点
        val validPoints = trackPoints.filter { it.isValidCoordinate() }
        if (validPoints.size < 2) return null

        // 创建Bitmap和Canvas
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 获取主题颜色
        val colors = if (isDark) DarkColors else LightColors

        // 1. 绘制背景
        canvas.drawColor(colors.background)

        // 2. 计算边界和变换参数
        val bounds = calculateBounds(validPoints)
        val padding = size * PADDING_RATIO
        val drawableSize = size - 2 * padding

        // 计算缩放比例（保持宽高比）
        val latRange = bounds.maxLat - bounds.minLat
        val lonRange = bounds.maxLon - bounds.minLon
        val scale = if (latRange > 0 || lonRange > 0) {
            drawableSize / max(latRange, lonRange)
        } else {
            1.0
        }

        // 计算偏移使轨迹居中
        val centerLat = (bounds.minLat + bounds.maxLat) / 2
        val centerLon = (bounds.minLon + bounds.maxLon) / 2

        // 转换函数：将经纬度转换为画布坐标
        fun toCanvasX(lon: Double): Float {
            return (padding + drawableSize / 2 + (lon - centerLon) * scale).toFloat()
        }

        fun toCanvasY(lat: Double): Float {
            // 注意：纬度向上增大，但画布Y轴向下增大，所以要反转
            return (padding + drawableSize / 2 - (lat - centerLat) * scale).toFloat()
        }

        // 3. 绘制轨迹线
        val trackPaint = Paint().apply {
            color = trackColorOverride ?: colors.track
            style = Paint.Style.STROKE
            strokeWidth = size * TRACK_WIDTH_RATIO
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        val path = Path()
        validPoints.forEachIndexed { index, point ->
            val x = toCanvasX(point.longitude)
            val y = toCanvasY(point.latitude)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, trackPaint)

        // 4. 绘制起点（绿色）
        val markerRadius = size * MARKER_RADIUS_RATIO
        val startPoint = validPoints.first()
        val startPaint = Paint().apply {
            color = colors.start
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(
            toCanvasX(startPoint.longitude),
            toCanvasY(startPoint.latitude),
            markerRadius,
            startPaint
        )

        // 5. 绘制终点（红色）
        if (validPoints.size > 1) {
            val endPoint = validPoints.last()
            val endPaint = Paint().apply {
                color = colors.end
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(
                toCanvasX(endPoint.longitude),
                toCanvasY(endPoint.latitude),
                markerRadius,
                endPaint
            )
        }

        return bitmap
    }

    /**
     * 渲染透明背景的融合轨迹缩略图，用于与卡片背景融合显示。
     * 轨迹线和标记使用降低的 alpha，无背景色填充。
     *
     * @param trackPoints 轨迹点列表
     * @param width 输出图片宽度（像素）
     * @param height 输出图片高度（像素）
     * @param isDark 是否为暗色主题
     * @param trackAlpha 轨迹线透明度 0.0~1.0
     * @param markerAlpha 起终点标记透明度 0.0~1.0
     * @return 渲染后的透明背景 Bitmap，轨迹点不足则返回 null
     */
    fun renderBlended(
        trackPoints: List<TrackPoint>,
        width: Int,
        height: Int,
        isDark: Boolean,
        trackAlpha: Float = 0.45f,
        markerAlpha: Float = 0.65f,
        trackColorOverride: Int? = null
    ): Bitmap? {
        val validPoints = trackPoints.filter { it.isValidCoordinate() }
        if (validPoints.size < 2) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val colors = if (isDark) DarkColors else LightColors
        val minDim = min(width, height)

        val bounds = calculateBounds(validPoints)
        val padding = minDim * PADDING_RATIO
        val drawableWidth = width - 2 * padding
        val drawableHeight = height - 2 * padding

        val latRange = bounds.maxLat - bounds.minLat
        val lonRange = bounds.maxLon - bounds.minLon
        val scale = if (latRange > 0 || lonRange > 0) {
            min(drawableWidth, drawableHeight) / max(latRange, lonRange)
        } else {
            1.0
        }

        val centerLat = (bounds.minLat + bounds.maxLat) / 2
        val centerLon = (bounds.minLon + bounds.maxLon) / 2

        fun toCanvasX(lon: Double): Float {
            return (width / 2f + (lon - centerLon) * scale).toFloat()
        }

        fun toCanvasY(lat: Double): Float {
            return (height / 2f - (lat - centerLat) * scale).toFloat()
        }

        fun applyAlpha(color: Int, alpha: Float): Int {
            val a = (alpha * 255).toInt().coerceIn(0, 255)
            return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
        }

        val trackPaint = Paint().apply {
            color = applyAlpha(trackColorOverride ?: colors.track, trackAlpha)
            style = Paint.Style.STROKE
            strokeWidth = minDim * TRACK_WIDTH_RATIO
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        val path = Path()
        validPoints.forEachIndexed { index, point ->
            val x = toCanvasX(point.longitude)
            val y = toCanvasY(point.latitude)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, trackPaint)

        val markerRadius = minDim * 0.035f
        val startPoint = validPoints.first()
        canvas.drawCircle(
            toCanvasX(startPoint.longitude),
            toCanvasY(startPoint.latitude),
            markerRadius,
            Paint().apply {
                color = applyAlpha(colors.start, markerAlpha)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
        )

        if (validPoints.size > 1) {
            val endPoint = validPoints.last()
            canvas.drawCircle(
                toCanvasX(endPoint.longitude),
                toCanvasY(endPoint.latitude),
                markerRadius,
                Paint().apply {
                    color = applyAlpha(colors.end, markerAlpha)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
            )
        }

        return bitmap
    }

    /**
     * 计算轨迹边界
     */
    private fun calculateBounds(points: List<TrackPoint>): Bounds {
        var minLat = Double.MAX_VALUE
        var maxLat = Double.MIN_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = Double.MIN_VALUE

        points.forEach { point ->
            minLat = min(minLat, point.latitude)
            maxLat = max(maxLat, point.latitude)
            minLon = min(minLon, point.longitude)
            maxLon = max(maxLon, point.longitude)
        }

        return Bounds(minLat, maxLat, minLon, maxLon)
    }

    /**
     * 轨迹边界数据类
     */
    private data class Bounds(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    )
}
