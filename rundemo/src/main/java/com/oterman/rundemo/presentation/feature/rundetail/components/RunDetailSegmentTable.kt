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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.RunSegment
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.ui.theme.SecondaryTextColor

// 柱状图第三列可切换指标
private enum class SegmentMetric(val label: String) {
    CADENCE("步频"),
    HEART_RATE("心率"),
    STRIDE_LENGTH("步幅"),
    POWER("功率");

    fun next(): SegmentMetric = entries[(ordinal + 1) % entries.size]
}

private fun RunSegment.metricValue(metric: SegmentMetric): String = when (metric) {
    SegmentMetric.CADENCE -> if (averageCadence > 0) "${averageCadence.toInt()}" else "-"
    SegmentMetric.HEART_RATE -> if (averageHeartRate > 0) "${averageHeartRate.toInt()}" else "-"
    SegmentMetric.STRIDE_LENGTH -> if (averageStrideLength > 0) String.format("%.1f", averageStrideLength) else "-"
    SegmentMetric.POWER -> if (averagePower > 0) "${averagePower.toInt()}" else "-"
}

// 左列固定宽度
private val LeftColWidth = 40.dp
// 右列固定宽度
private val RightColWidth = 56.dp

/**
 * 公里分段卡片
 * 支持两种视图模式：
 *   - TABLE: 6列表格（序号、距离、配速、心率、步幅、步频）
 *   - BAR_CHART: Strava风格横向柱状图，配速叠加在柱子内，第三列指标可切换
 */
@Composable
fun RunDetailSegmentTable(
    segments: List<RunSegment>,
    initialBarChartMode: Boolean = false,
    onBarChartModeChange: ((Boolean) -> Unit)? = null,
    initialMetricIndex: Int = 0,
    onMetricIndexChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (segments.isEmpty()) return

    var isBarChartMode by remember { mutableStateOf(initialBarChartMode) }
    var selectedMetric by remember {
        mutableStateOf(SegmentMetric.entries[initialMetricIndex.coerceIn(0, SegmentMetric.entries.lastIndex)])
    }

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

                // 视图模式切换按钮
                IconButton(
                    onClick = {
                        val newMode = !isBarChartMode
                        isBarChartMode = newMode
                        onBarChartModeChange?.invoke(newMode)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isBarChartMode) Icons.Filled.TableChart else Icons.Filled.BarChart,
                        contentDescription = if (isBarChartMode) "切换到表格视图" else "切换到柱状图视图",
                        tint = RunBlue,
                        modifier = Modifier
                            .size(20.dp)
                            // BarChart icon 默认竖向，旋转90°变为横向柱状图样式
                            .graphicsLayer { rotationZ = if (isBarChartMode) 0f else 90f }
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
                        selectedMetric = selectedMetric,
                        onMetricChange = {
                            selectedMetric = selectedMetric.next()
                            onMetricIndexChange?.invoke(selectedMetric.ordinal)
                        }
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
        Color.LightGray.copy(alpha = 0.22f)
    } else {
        Color.Transparent
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
 * Strava 风格横向柱状图（升级版）
 *
 * 每行布局：
 *   [40dp 左标签: 序号/最快/零头距离]  [weight 1f: 柱+配速叠加]  [56dp 指标值]
 *
 * 归一化规则：
 *   longerMeansSlow = true  → 柱长 ∝ (pace - minPace) / range，最慢满宽，最快最短
 *   longerMeansSlow = false → 柱长 ∝ 1 - 上述，最快满宽，最慢最短
 *   保留 28% 最小可见宽度，确保配速文字始终在柱内可见
 *
 * 不足1公里段：第一列显示零头距离（如"0.85"），其余与普通行一致
 */
@Composable
private fun SegmentBarChart(
    segments: List<RunSegment>,
    selectedMetric: SegmentMetric,
    onMetricChange: () -> Unit
) {
    val validPaces = segments.map { it.averageSpeed }.filter { it > 0.0 }
    if (validPaces.isEmpty()) return

    val minPace = validPaces.min()
    val maxPace = validPaces.max()
    val paceRange = (maxPace - minPace).coerceAtLeast(0.001)

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        // 表头
        SegmentBarChartHeader(
            selectedMetric = selectedMetric,
            onMetricChange = onMetricChange
        )
        Spacer(modifier = Modifier.height(6.dp))

        segments.forEachIndexed { index, segment ->
            val pace = segment.averageSpeed
            // slowRatio: 0 = 最快，1 = 最慢；固定快→长模式
            val slowRatio = if (pace > 0) ((pace - minPace) / paceRange).toFloat() else 0f
            // 快→长：越快柱越长
            val targetFraction = 0.28f + (1f - slowRatio) * 0.72f

            val animatedFraction by animateFloatAsState(
                targetValue = targetFraction,
                animationSpec = tween(durationMillis = 400, delayMillis = index * 30),
                label = "bar_$index"
            )

            SegmentBarRow(
                segment = segment,
                barFraction = animatedFraction,
                selectedMetric = selectedMetric
            )

            if (index < segments.lastIndex) {
                Spacer(modifier = Modifier.height(3.dp))
            }
        }
    }
}

/**
 * 三列表头：公里 | 配速 | [指标名▼]（第三列可点击切换）
 */
@Composable
private fun SegmentBarChartHeader(
    selectedMetric: SegmentMetric,
    onMetricChange: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 第一列：公里
        Text(
            text = "公里",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(LeftColWidth),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 第二列：配速
        Text(
            text = "配速",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        // 第三列：可切换指标
        Row(
            modifier = Modifier
                .width(RightColWidth)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onMetricChange)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = selectedMetric.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = RunBlue
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "切换指标",
                tint = RunBlue,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun SegmentBarRow(
    segment: RunSegment,
    barFraction: Float,
    selectedMetric: SegmentMetric
) {
    val isFastest = segment.isFastest
    val isPartial = segment.distance < 0.95
    val barColor = if (isFastest) RunBlue else RunBlue.copy(alpha = 0.65f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── 第一列：公里号 / 最快 / 零头距离（固定 40dp）──
        Box(
            modifier = Modifier
                .width(LeftColWidth)
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isFastest) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RunBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "最快",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RunBlue
                    )
                }
            } else if (isPartial) {
                // 不足1公里：显示零头距离，如"0.85"
                Text(
                    text = segment.getFormattedDistance(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = SecondaryTextColor,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "${segment.seq + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SecondaryTextColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // ── 第二列：柱状条（配速叠加，weight 1f）──
        Box(
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // 轨道背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            )
            // 实心柱 + 配速文字叠加
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .height(22.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(barColor),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 6.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = segment.getFormattedSpeed(),
                        fontSize = 11.sp,
                        fontWeight = if (isFastest) FontWeight.SemiBold else FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    if (isFastest) {
                        Text(
                            text = "最快",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }

        // ── 第三列：指标值（固定 56dp，右对齐）──
        Text(
            text = segment.metricValue(selectedMetric),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isFastest) RunBlue else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(RightColWidth)
                .padding(start = 6.dp)
        )
    }
}

