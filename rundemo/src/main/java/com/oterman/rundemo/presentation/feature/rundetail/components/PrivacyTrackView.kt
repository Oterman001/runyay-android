package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.ui.theme.RunTheme
import kotlin.math.max
import kotlin.math.min

private const val PADDING_RATIO = 0.12f
private const val TRACK_WIDTH_RATIO = 0.008f
private const val MARKER_RADIUS_RATIO = 0.015f
private const val KM_BADGE_RADIUS_RATIO = 0.02f

@Composable
fun PrivacyTrackView(
    trackPoints: List<TrackPoint>,
    showKmMarkers: Boolean = true,
    kmMarkerInterval: Int = 1,
    modifier: Modifier = Modifier
) {
    val isDark = RunTheme.isDark
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val trackColor = if (isDark) Color(0xFFDDFF04) else Color(0xFFFB7B26)
    val startColor = if (isDark) Color(0xFF4CAF50) else Color(0xFF34A853)
    val endColor = if (isDark) Color(0xFFF44336) else Color(0xFFEA4335)
    val kmBadgeBg = Color(0xFF222638)
    val kmBadgeText = Color.White
    val markerStroke = Color.White

    val kmPositions = if (showKmMarkers) {
        calculateKilometerPositions(trackPoints, kmMarkerInterval)
    } else {
        emptyList()
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val validPoints = trackPoints.filter { it.isValidCoordinate() }
        if (validPoints.size < 2) return@Canvas

        val bounds = calcBounds(validPoints)
        val paddingPx = min(size.width, size.height) * PADDING_RATIO
        val drawableWidth = size.width - 2 * paddingPx
        val drawableHeight = size.height - 2 * paddingPx

        val latRange = bounds.maxLat - bounds.minLat
        val lonRange = bounds.maxLon - bounds.minLon
        val scale = if (latRange > 0 || lonRange > 0) {
            min(drawableWidth, drawableHeight) / max(latRange, lonRange).toFloat()
        } else {
            1f
        }

        val centerLat = (bounds.minLat + bounds.maxLat) / 2
        val centerLon = (bounds.minLon + bounds.maxLon) / 2

        fun toCanvas(point: TrackPoint): Offset {
            val x = size.width / 2f + ((point.longitude - centerLon) * scale).toFloat()
            val y = size.height / 2f - ((point.latitude - centerLat) * scale).toFloat()
            return Offset(x, y)
        }

        // Draw track
        val trackWidth = min(size.width, size.height) * TRACK_WIDTH_RATIO
        val path = Path()
        validPoints.forEachIndexed { index, point ->
            val offset = toCanvas(point)
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        drawPath(
            path = path,
            color = trackColor,
            style = Stroke(width = trackWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )

        val markerRadius = min(size.width, size.height) * MARKER_RADIUS_RATIO
        val strokeWidth = markerRadius * 0.4f

        // Draw km markers
        if (kmPositions.isNotEmpty()) {
            val kmRadius = min(size.width, size.height) * KM_BADGE_RADIUS_RATIO
            kmPositions.forEachIndexed { index, kmPoint ->
                val offset = toCanvas(kmPoint)
                drawKmBadge(
                    center = offset,
                    radius = kmRadius,
                    kmNumber = (index + 1) * kmMarkerInterval,
                    bgColor = kmBadgeBg,
                    textColor = kmBadgeText,
                    strokeColor = markerStroke,
                    textMeasurer = textMeasurer,
                    density = density
                )
            }
        }

        // Draw start point
        val startOffset = toCanvas(validPoints.first())
        drawCircle(color = startColor, radius = markerRadius, center = startOffset)
        drawCircle(color = markerStroke, radius = markerRadius, center = startOffset, style = Stroke(width = strokeWidth))

        // Draw end point
        val endOffset = toCanvas(validPoints.last())
        drawCircle(color = endColor, radius = markerRadius, center = endOffset)
        drawCircle(color = markerStroke, radius = markerRadius, center = endOffset, style = Stroke(width = strokeWidth))
    }
}

private fun DrawScope.drawKmBadge(
    center: Offset,
    radius: Float,
    kmNumber: Int,
    bgColor: Color,
    textColor: Color,
    strokeColor: Color,
    textMeasurer: TextMeasurer,
    density: Density
) {
    drawCircle(color = bgColor, radius = radius, center = center)
    drawCircle(color = strokeColor, radius = radius, center = center, style = Stroke(width = 1.5f))

    val fontSizeSp = (radius * 0.9f / density.density / density.fontScale)
    val textStyle = TextStyle(color = textColor, fontSize = fontSizeSp.sp)
    val textLayout = textMeasurer.measure("$kmNumber", textStyle)
    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(
            center.x - textLayout.size.width / 2f,
            center.y - textLayout.size.height / 2f
        )
    )
}

private data class TrackBounds(
    val minLat: Double, val maxLat: Double,
    val minLon: Double, val maxLon: Double
)

private fun calcBounds(points: List<TrackPoint>): TrackBounds {
    var minLat = Double.MAX_VALUE
    var maxLat = -Double.MAX_VALUE
    var minLon = Double.MAX_VALUE
    var maxLon = -Double.MAX_VALUE
    points.forEach { p ->
        minLat = min(minLat, p.latitude)
        maxLat = max(maxLat, p.latitude)
        minLon = min(minLon, p.longitude)
        maxLon = max(maxLon, p.longitude)
    }
    return TrackBounds(minLat, maxLat, minLon, maxLon)
}
