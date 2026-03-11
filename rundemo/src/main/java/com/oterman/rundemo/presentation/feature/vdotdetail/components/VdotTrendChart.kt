package com.oterman.rundemo.presentation.feature.vdotdetail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.presentation.feature.vdotdetail.VdotTrendPoint
import com.oterman.rundemo.ui.theme.RunTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VdotTrendChart(
    points: List<VdotTrendPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无数据",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val blueColor = RunTheme.colorScheme.blue
    val orangeColor = RunTheme.colorScheme.orange
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val avgLineColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(blueColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "综合跑力",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(orangeColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "动态跑力",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Chart canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            drawTrendChart(
                points = points,
                blueColor = blueColor,
                orangeColor = orangeColor,
                gridColor = gridColor,
                avgLineColor = avgLineColor
            )
        }

        // X-axis date labels
        Spacer(modifier = Modifier.height(4.dp))
        val dateLabels = getDateLabels(points)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dateLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun getDateLabels(points: List<VdotTrendPoint>): List<String> {
    if (points.isEmpty()) return emptyList()
    val sdf = SimpleDateFormat("M/d", Locale.getDefault())
    val count = minOf(5, points.size)
    if (count <= 1) return listOf(sdf.format(Date(points.first().dateMillis)))

    val indices = (0 until count).map { i ->
        (i * (points.size - 1).toLong() / (count - 1)).toInt()
    }
    return indices.map { sdf.format(Date(points[it].dateMillis)) }
}

private fun DrawScope.drawTrendChart(
    points: List<VdotTrendPoint>,
    blueColor: Color,
    orangeColor: Color,
    gridColor: Color,
    avgLineColor: Color
) {
    if (points.size < 2) {
        // Single point - draw dots
        if (points.size == 1) {
            val cx = size.width / 2
            val cy = size.height / 2
            drawCircle(blueColor, 4f, Offset(cx, cy))
            drawCircle(orangeColor, 4f, Offset(cx, cy - 20f))
        }
        return
    }

    val chartWidth = size.width
    val chartHeight = size.height

    // Calculate Y range with 5% margin
    val allValues = points.flatMap { listOf(it.smoothedValue, it.rawValue) }
    val dataMin = allValues.min()
    val dataMax = allValues.max()
    val range = dataMax - dataMin
    val margin = if (range == 0.0) 2.0 else range * 0.05
    val yMin = dataMin - margin
    val yMax = dataMax + margin
    val yRange = yMax - yMin

    // X range
    val xMin = points.first().dateMillis.toFloat()
    val xMax = points.last().dateMillis.toFloat()
    val xRange = (xMax - xMin).let { if (it == 0f) 1f else it }

    fun getX(dateMillis: Long): Float = ((dateMillis - xMin) / xRange) * chartWidth
    fun getY(value: Double): Float = ((1.0 - (value - yMin) / yRange) * chartHeight).toFloat()

    // Grid lines (3 horizontal)
    for (i in 0..3) {
        val y = chartHeight * (i / 3f)
        drawLine(gridColor, Offset(0f, y), Offset(chartWidth, y), 1f)
    }

    // Average dashed line (smoothed values)
    val avgSmoothed = points.map { it.smoothedValue }.average()
    if (avgSmoothed in yMin..yMax) {
        val avgY = getY(avgSmoothed)
        drawLine(
            color = avgLineColor.copy(alpha = 0.6f),
            start = Offset(0f, avgY),
            end = Offset(chartWidth, avgY),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
        )
    }

    // Build paths using Catmull-Rom spline
    val bluePath = Path()
    val blueFillPath = Path()
    val orangePath = Path()

    val bluePts = points.map { Offset(getX(it.dateMillis), getY(it.smoothedValue)) }
    val orangePts = points.map { Offset(getX(it.dateMillis), getY(it.rawValue)) }

    fun buildSmoothPath(path: Path, pts: List<Offset>) {
        path.moveTo(pts[0].x, pts[0].y)
        val n = pts.size
        if (n < 3) {
            for (i in 1 until n) path.lineTo(pts[i].x, pts[i].y)
        } else {
            for (i in 1 until n) {
                val p0 = if (i == 1) pts[0] else pts[i - 2]
                val p1 = pts[i - 1]
                val p2 = pts[i]
                val p3 = if (i == n - 1) pts[n - 1] else pts[i + 1]
                val cp1x = p1.x + (p2.x - p0.x) / 6f
                val cp1y = p1.y + (p2.y - p0.y) / 6f
                val cp2x = p2.x - (p3.x - p1.x) / 6f
                val cp2y = p2.y - (p3.y - p1.y) / 6f
                path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
            }
        }
    }

    buildSmoothPath(bluePath, bluePts)
    buildSmoothPath(orangePath, orangePts)

    // Blue fill path
    blueFillPath.moveTo(bluePts[0].x, chartHeight)
    blueFillPath.lineTo(bluePts[0].x, bluePts[0].y)
    val n = bluePts.size
    if (n < 3) {
        for (i in 1 until n) blueFillPath.lineTo(bluePts[i].x, bluePts[i].y)
    } else {
        for (i in 1 until n) {
            val p0 = if (i == 1) bluePts[0] else bluePts[i - 2]
            val p1 = bluePts[i - 1]
            val p2 = bluePts[i]
            val p3 = if (i == n - 1) bluePts[n - 1] else bluePts[i + 1]
            val cp1x = p1.x + (p2.x - p0.x) / 6f
            val cp1y = p1.y + (p2.y - p0.y) / 6f
            val cp2x = p2.x - (p3.x - p1.x) / 6f
            val cp2y = p2.y - (p3.y - p1.y) / 6f
            blueFillPath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
        }
    }
    blueFillPath.lineTo(bluePts.last().x, chartHeight)
    blueFillPath.close()

    // Draw blue gradient fill
    drawPath(
        path = blueFillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                blueColor.copy(alpha = 0.3f),
                blueColor.copy(alpha = 0.05f)
            )
        )
    )

    // Draw blue line
    drawPath(
        path = bluePath,
        color = blueColor,
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )

    // Draw orange line (no fill)
    drawPath(
        path = orangePath,
        color = orangeColor,
        style = Stroke(width = 2f, cap = StrokeCap.Round)
    )
}
