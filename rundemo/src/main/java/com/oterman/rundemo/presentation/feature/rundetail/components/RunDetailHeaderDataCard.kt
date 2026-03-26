package com.oterman.rundemo.presentation.feature.rundetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.presentation.components.InclusiveLevelIndicator
import com.oterman.rundemo.presentation.components.MetricTagChip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.oterman.rundemo.presentation.feature.rundetail.PerformTagType
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailLayoutConstants
import com.oterman.rundemo.presentation.feature.rundetail.RunMetricItem
import com.oterman.rundemo.presentation.feature.rundetail.RunPerformanceTag
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.RunYayFontFamily
import com.oterman.rundemo.ui.theme.RunYayFontFamily4
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 合并的 Header + DataGrid 卡片
 * 对标 iOS V3HeaderCardView，将距离/时间信息和数据网格合并在一张卡片内
 */
@Composable
fun RunDetailHeaderDataCard(
    distance: Double,
    startTime: Long,
    endTime: Long,
    duration: Double,
    deviceName: String?,
    isOutdoor: Boolean,
    metrics: List<RunMetricItem>,
    avatarUrl: String? = null,
    isLoadingAvatar: Boolean = false,
    userName: String? = null,
    inclusiveLevel: Int = 1,
    onInclusiveLevelClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Tag dialog state
    var showTagDialog by remember { mutableStateOf(false) }
    var dialogTag by remember { mutableStateOf<RunPerformanceTag?>(null) }

    AppCard(
        modifier = modifier
            .padding(horizontal = RunDetailLayoutConstants.HeaderCardMargin.dp)
    ) {
        Column(
            modifier = Modifier.padding(RunDetailLayoutConstants.HeaderCardPadding.dp)
        ) {
                // ========== Header 部分（对标 iOS：距离 → 日期 → 设备）==========
                val avatarSize = RunDetailLayoutConstants.AvatarSize.dp

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：距离 + 日期 + 设备
                    Column(modifier = Modifier.weight(1f)) {
                        // 第一行：距离（大字）
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = if (onInclusiveLevelClick != null)
                                Modifier.clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = onInclusiveLevelClick
                                )
                            else Modifier
                        ) {
                            Text(
                                text = String.format("%.2f", distance),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = RunYayFontFamily
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "km",
                                fontSize = RunDetailLayoutConstants.DistanceUnitFontSize.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            InclusiveLevelIndicator(
                                inclusiveLevel = inclusiveLevel,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 第二行：日期（含周几）+ 时间段
                        Text(
                            text = formatHeaderStartEndTime(startTime, endTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 第三行：设备信息（可选）
                        deviceName?.let { device ->
                            if (device.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Watch,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = device,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 右侧：头像（垂直居中于header三行）
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        HeaderAvatar(
                            avatarUrl = avatarUrl,
                            isLoading = isLoadingAvatar
                        )
                        if (!userName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(min = avatarSize, max = 96.dp)
                            )
                        }
                    }
                }

                // ========== 分隔线 ==========
                if (metrics.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // ========== DataGrid 部分 ==========
                    val chunkedMetrics = metrics.chunked(3)
                    chunkedMetrics.forEachIndexed { index, rowMetrics ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowMetrics.forEach { metric ->
                                MergedMetricCell(
                                    metric = metric,
                                    modifier = Modifier.weight(1f),
                                    onTagClick = { tag ->
                                        dialogTag = tag
                                        showTagDialog = true
                                    }
                                )
                            }
                            repeat(3 - rowMetrics.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        if (index < chunkedMetrics.size - 1) {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }

    // Tag 介绍 Dialog
    if (showTagDialog && dialogTag != null) {
        TagInfoDialog(
            tag = dialogTag!!,
            onDismiss = {
                showTagDialog = false
                dialogTag = null
            }
        )
    }
}

/**
 * 单个指标单元格
 */
@Composable
private fun MergedMetricCell(
    metric: RunMetricItem,
    modifier: Modifier = Modifier,
    onTagClick: (RunPerformanceTag) -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            if (metric.isVdot) {
                Text(
                    text = metric.value,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = RunTheme.colorScheme.blue,
                    fontFamily = RunYayFontFamily
                )
            } else {
                Text(
                    text = metric.value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = RunYayFontFamily4
                )
            }
            metric.unit?.let { unit ->
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            // Tag 标签（紧跟在 value+unit 后面）
            metric.tag?.let { tag ->
                Spacer(modifier = Modifier.width(4.dp))
                MetricTagChip(
                    text = tag.tagName,
                    color = Color(tag.tagColor),
                    onClick = { onTagClick(tag) }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = metric.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Tag 介绍弹窗
 */
@Composable
private fun TagInfoDialog(
    tag: RunPerformanceTag,
    onDismiss: () -> Unit
) {
    val title = when (tag.tagType) {
        PerformTagType.STRIDE_RATIO -> "垂直步幅比"
        PerformTagType.TRAINING_LOAD -> "运动负荷"
    }

    val description = when (tag.tagType) {
        PerformTagType.STRIDE_RATIO ->
            "垂直步幅比反映每一步中垂直方向位移占步幅的比例，比值越低说明跑步效率越高。"
        PerformTagType.TRAINING_LOAD ->
            "运动负荷综合评估本次运动对身体的影响程度，数值越高表示训练强度越大。"
    }

    data class LevelInfo(val range: String, val name: String, val color: Long)

    val levels = when (tag.tagType) {
        PerformTagType.STRIDE_RATIO -> listOf(
            LevelInfo("<6%", "凌波鸭", 0xFF90CAF9),
            LevelInfo("6%-8%", "踏浪鸭", 0xFF4CAF50),
            LevelInfo("8%-10%", "轻羽鸭", 0xFFFFC107),
            LevelInfo("10%-12%", "稳健鸭", 0xFFFF9800),
            LevelInfo(">=12%", "蓄力鸭", 0xFFF44336)
        )
        PerformTagType.TRAINING_LOAD -> listOf(
            LevelInfo("0-50", "很低", 0xFF90CAF9),
            LevelInfo("50-120", "较低", 0xFF4CAF50),
            LevelInfo("120-250", "中等", 0xFFFFC107),
            LevelInfo("250-400", "高", 0xFFFF9800),
            LevelInfo(">=400", "很高", 0xFFF44336)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                levels.forEach { level ->
                    val isCurrent = level.name == tag.tagName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(level.color))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildAnnotatedString {
                                append("${level.range}  ${level.name}")
                                if (isCurrent) {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(level.color))) {
                                        append("  <-- 当前")
                                    }
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 头像组件：有URL时加载网络图片，否则显示"跑"占位符
 */
@Composable
private fun HeaderAvatar(
    avatarUrl: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val size = RunDetailLayoutConstants.AvatarSize.dp

    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(
                width = RunDetailLayoutConstants.AvatarBorderWidth.dp,
                color = Color.White,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.4f),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            avatarUrl.isNullOrBlank() -> {
                // 无头像URL - 显示"跑"占位符
                Text(
                    text = "跑",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            else -> {
                SubcomposeAsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .allowHardware(false)
                        .build(),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(size * 0.4f),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    error = {
                        Text(
                            text = "跑",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                )
            }
        }
    }
}

/**
 * 格式化日期时间: "12.28(六) 14:30-15:45"
 */
private fun formatHeaderStartEndTime(startTime: Long, endTime: Long): String {
    val dateFormat = SimpleDateFormat("MM.dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startDate = Date(startTime)
    val endDate = Date(endTime)
    val dayOfWeek = arrayOf("日", "一", "二", "三", "四", "五", "六")
    val cal = Calendar.getInstance().apply { time = startDate }
    val weekDay = dayOfWeek[cal.get(Calendar.DAY_OF_WEEK) - 1]
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val recordYear = cal.get(Calendar.YEAR)
    val yearPrefix = if (recordYear != currentYear) "$recordYear." else ""
    return "${yearPrefix}${dateFormat.format(startDate)}(周$weekDay) ${timeFormat.format(startDate)}-${timeFormat.format(endDate)}"
}
