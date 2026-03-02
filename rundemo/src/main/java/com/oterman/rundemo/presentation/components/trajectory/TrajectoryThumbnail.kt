package com.oterman.rundemo.presentation.components.trajectory

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import com.oterman.rundemo.ui.theme.RunTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.domain.trajectory.ThumbnailState
import com.oterman.rundemo.domain.trajectory.TrajectoryThumbnailManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 轨迹缩略图组件
 * 用于在列表中显示跑步轨迹的预览图
 *
 * 特性：
 * - 自动根据主题切换颜色
 * - 加载时显示Shimmer动画
 * - 室内跑显示跑步机图标
 * - 支持缓存
 *
 * @param workoutId 运动记录ID
 * @param trackPoints 轨迹点列表，null表示无轨迹（室内跑）
 * @param isLoading 外部传入的加载状态，true表示轨迹点正在加载中
 * @param isOutdoor 是否为户外跑（用于判断是否应该有轨迹）
 * @param modifier Modifier
 * @param size 缩略图尺寸
 * @param cornerRadius 圆角大小
 */
@Composable
fun TrajectoryThumbnail(
    workoutId: String,
    trackPoints: List<TrackPoint>?,
    isLoading: Boolean = false,
    isOutdoor: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    cornerRadius: Dp = 8.dp
) {
    val context = LocalContext.current
    val isDark = RunTheme.isDark
    val density = LocalDensity.current

    // 将dp转换为px
    val sizePx = with(density) { size.toPx().toInt() }

    // 缩略图状态
    var thumbnailState by remember(workoutId, isDark) {
        mutableStateOf<ThumbnailState>(ThumbnailState.NotStarted)
    }

    // 获取缩略图管理器
    val thumbnailManager = remember { TrajectoryThumbnailManager.getInstance(context) }
    val colorMode = remember { PreferencesManager(context).getTrajectoryColorMode() }

    // 加载缩略图
    LaunchedEffect(workoutId, trackPoints, isLoading, isDark, sizePx, colorMode) {
        if (!isOutdoor) {
            thumbnailState = ThumbnailState.NoTrajectory
            return@LaunchedEffect
        }

        // 如果外部正在加载轨迹点，显示 Generating 状态
        if (isLoading || trackPoints == null) {
            thumbnailState = ThumbnailState.Generating
            return@LaunchedEffect
        }

        // 轨迹点已加载，生成缩略图
        thumbnailState = ThumbnailState.Generating
        thumbnailState = thumbnailManager.getThumbnail(
            workoutId = workoutId,
            trackPoints = trackPoints,
            sizePx = sizePx,
            isDark = isDark,
            colorMode = colorMode
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        when (val state = thumbnailState) {
            is ThumbnailState.Cached -> {
                // 显示缩略图
                Image(
                    bitmap = state.bitmap.asImageBitmap(),
                    contentDescription = "轨迹预览",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            }

            is ThumbnailState.Generating -> {
                // 加载中，显示Shimmer动画
                ShimmerPlaceholder(
                    modifier = Modifier.matchParentSize()
                )
            }

            is ThumbnailState.NotStarted -> {
                // 未开始，显示占位背景
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            is ThumbnailState.NoTrajectory -> {
                // 室内跑，显示跑步机图标
                IndoorRunIcon(
                    modifier = Modifier.matchParentSize()
                )
            }

            is ThumbnailState.Failed -> {
                // 加载失败，显示占位符
                NoTrajectoryPlaceholder(
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}

/**
 * 融合式轨迹缩略图，透明背景，用于与卡片背景无缝融合。
 * 轨迹以低透明度装饰性地绘制在卡片右侧区域。
 */
@Composable
fun BlendedTrajectoryThumbnail(
    trackPoints: List<TrackPoint>?,
    isLoading: Boolean = false,
    isOutdoor: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 100.dp,
    trackAlpha: Float = 0.9f,
    markerAlpha: Float = 1.0f
) {
    val isDark = RunTheme.isDark
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx().toInt() }
    val heightPx = with(density) { height.toPx().toInt() }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(trackPoints, isLoading, isDark, widthPx, heightPx) {
        if (!isOutdoor || isLoading || trackPoints.isNullOrEmpty()) {
            bitmap = null
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.Default) {
            TrajectoryRenderer.renderBlended(
                trackPoints = trackPoints,
                width = widthPx,
                height = heightPx,
                isDark = isDark,
                trackAlpha = trackAlpha,
                markerAlpha = markerAlpha
            )
        }
    }

    Box(
        modifier = modifier.size(width, height),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * Shimmer加载动画占位符
 */
@Composable
private fun ShimmerPlaceholder(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceVariant
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier.background(brush)
    )
}

/**
 * 室内跑图标占位符
 */
@Composable
private fun IndoorRunIcon(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.FitnessCenter,
            contentDescription = "室内跑",
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

/**
 * 无轨迹占位符
 */
@Composable
private fun NoTrajectoryPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.FitnessCenter,
            contentDescription = "无轨迹",
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}
