package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.RunSegment
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.ui.theme.SecondaryTextColor

/**
 * 公里分段卡片
 * 支持两种视图模式：
 *   - TABLE: 6列表格（序号、距离、配速、心率、步幅、步频）
 *   - BAR_CHART: Strava风格横向柱状图，以配速为长度基准
 *
 * 标题右侧有两个控件：
 *   1. 视图切换图标按钮（表格 ↔ 柱状图）
 *   2. 柱状图模式下，柱子方向切换 Toggle（慢→长 / 快→长）
 */
@Composable
fun RunDetailSegmentTable(
    segments: List<RunSegment>,
    modifier: Modifier = Modifier
) {
    if (segments.isEmpty()) return

    var isBarChartMode by remember { mutableStateOf(false) }
    // true = 越慢柱越长（默认，与 Strava 一致）；false = 越快柱越长
    var barLongerMeansSlow by remember { mutableStateOf(true) }

    AppCard(
        modifier = modifier
            .padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RunDetailLayoutConstants.HeaderCardPadding.dp)
        ) {
            // ===== 标题行 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "公里分段",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // 柱状图方向切换（仅柱状图模式显示）
                if (isBarChartMode) {
                    BarChartDirectionToggle(
                        longerMeansSlow = barLongerMeansSlow,
                        onToggle = { barLongerMeansSlow = !barLongerMeansSlow }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // 视图模式切换按钮
                IconButton(
                    onClick = { isBarChartMode = !isBarChartMode },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isBarChartMode) Icons.Filled.TableChart else Icons.Filled.BarChart,
                        contentDescription = if (isBarChartMode) "切换到表格视图" else "切换到柱状图视图",
                        tint = RunBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ===== 内容区域（淡入淡出切换） =====
            AnimatedContent(
                targetState = isBarChartMode,
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                },
                label = "segment_view_mode"
            ) { barMode ->
                if (barMode) {
                    SegmentBarChart(
                        segments = segments,
                        longerMeansSlow = barLongerMeansSlow
                    )
                } else {
                    SegmentTable(segments = segments)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 表格视图
// ─────────────────────────────────────────────────────────────

@Composable
private fun SegmentTable(segments: List<RunSegment>) {
    Column {
        SegmentTableHeader(
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        segments.forEachIndexed { index, segment ->
            SegmentTableRow(
                segment = segment,
                index = index,
                isFastest = segment.isFastest
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun SegmentTableHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        val headers = listOf("序号", "距离", "配速", "心率", "步幅", "步频")
        headers.forEach { header ->
            Text(
                text = header,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SegmentTableRow(
    segment: RunSegment,
    index: Int,
    isFastest: Boolean
) {
    val bgColor = if (index % 2 == 0) {
        Color.LightGray.copy(alpha = 0.1f)
    } else {
        Color.Gray.copy(alpha = 0.01f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 12.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 序号 / 最快 badge
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (isFastest) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E88E5).copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "最快",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E88E5)
                    )
                }
            } else {
                Text(
                    text = "${segment.seq + 1}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 距离
        Text(
            text = segment.getFormattedDistance(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 配速
        Text(
            text = segment.getFormattedSpeed(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 心率
        Text(
            text = if (segment.averageHeartRate > 0) "${segment.averageHeartRate.toInt()}" else "-",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 步幅
        Text(
            text = if (segment.averageStrideLength > 0) {
                String.format("%.1f", segment.averageStrideLength)
            } else "-",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // 步频
        Text(
            text = if (segment.averageCadence > 0) "${segment.averageCadence.toInt()}" else "-",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 柱状图视图
// ─────────────────────────────────────────────────────────────

/**
 * Strava 风格横向柱状图
 *
 * 每行布局：
 *   [72dp 左标签: 序号 + 配速]  [10dp 间距]  [剩余宽度: 轨道 + 实心柱]
 *
 * 归一化规则：
 *   longerMeansSlow = true  → 柱长 ∝ (pace - minPace) / range，最慢满宽，最快最短
 *   longerMeansSlow = false → 柱长 ∝ 1 - 上述，最快满宽，最慢最短
 *   保留 15% 最小可见宽度，避免最短柱消失
 */
@Composable
private fun SegmentBarChart(
    segments: List<RunSegment>,
    longerMeansSlow: Boolean
) {
    val validPaces = segments.map { it.averageSpeed }.filter { it > 0.0 }
    if (validPaces.isEmpty()) return

    val minPace = validPaces.min()
    val maxPace = validPaces.max()
    val paceRange = (maxPace - minPace).coerceAtLeast(0.001)

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        segments.forEachIndexed { index, segment ->
            val pace = segment.averageSpeed
            // slowRatio: 0 = 最快，1 = 最慢
            val slowRatio = if (pace > 0) ((pace - minPace) / paceRange).toFloat() else 0f

            val targetFraction = if (longerMeansSlow) {
                0.15f + slowRatio * 0.85f
            } else {
                0.15f + (1f - slowRatio) * 0.85f
            }

            val animatedFraction by animateFloatAsState(
                targetValue = targetFraction,
                animationSpec = tween(durationMillis = 400, delayMillis = index * 30),
                label = "bar_$index"
            )

            SegmentBarRow(
                segment = segment,
                barFraction = animatedFraction
            )

            if (index < segments.lastIndex) {
                Spacer(modifier = Modifier.height(5.dp))
            }
        }
    }
}

@Composable
private fun SegmentBarRow(
    segment: RunSegment,
    barFraction: Float
) {
    val isFastest = segment.isFastest
    val barColor = if (isFastest) RunBlue else RunBlue.copy(alpha = 0.30f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── 左侧标签（固定 72dp）──
        Row(
            modifier = Modifier.width(72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isFastest) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RunBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "最快",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RunBlue
                    )
                }
            } else {
                Text(
                    text = "${segment.seq + 1}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SecondaryTextColor
                )
            }

            Text(
                text = segment.getFormattedSpeed(),
                fontSize = 12.sp,
                fontWeight = if (isFastest) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isFastest) RunBlue else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // ── 柱状条 ──
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // 轨道背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
            // 实心柱
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 柱状图方向 Toggle
// ─────────────────────────────────────────────────────────────

@Composable
private fun BarChartDirectionToggle(
    longerMeansSlow: Boolean,
    onToggle: () -> Unit
) {
    val activeBg = RunBlue.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 慢→长
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (longerMeansSlow) activeBg else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "慢→长",
                    fontSize = 11.sp,
                    fontWeight = if (longerMeansSlow) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (longerMeansSlow) RunBlue else SecondaryTextColor
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // 快→长
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (!longerMeansSlow) activeBg else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "快→长",
                    fontSize = 11.sp,
                    fontWeight = if (!longerMeansSlow) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (!longerMeansSlow) RunBlue else SecondaryTextColor
                )
            }
        }
    }
}
