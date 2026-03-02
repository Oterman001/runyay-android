package com.oterman.rundemo.presentation.feature.statistics.components

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.domain.model.TrackPoint
import com.oterman.rundemo.R
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.domain.trajectory.ThumbnailState
import com.oterman.rundemo.domain.trajectory.TrajectoryThumbnailManager
import com.oterman.rundemo.ui.theme.NoDataBg
import com.oterman.rundemo.ui.theme.NoDataBgDark
import com.oterman.rundemo.ui.theme.RunTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 通用轨迹缩略图单元格组件
 * 对应iOS的 DayTrajectoryThumbnailCell.swift
 *
 * 功能：
 * - 显示轨迹缩略图或占位状态
 * - 支持多状态渲染：缓存/加载中/失败/无轨迹
 * - 自动请求缩略图生成
 *
 * @param workoutId 运动记录ID
 * @param trackPoints 轨迹点列表（可为null）
 * @param size 单元格尺寸（默认48dp）
 * @param isFuture 是否为未来日期
 * @param modifier Modifier
 */
@Composable
fun DayTrajectoryCell(
    workoutId: String?,
    trackPoints: List<TrackPoint>?,
    size: Dp = 48.dp,
    isFuture: Boolean = false,
    isIndoor: Boolean = false,
    totalDistanceKm: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current
    
    // 转换dp到px
    val sizePx = with(density) { size.roundToPx() }
    
    // 本地状态：缩略图状态
    var thumbnailState by remember(workoutId, isDark) {
        mutableStateOf<ThumbnailState>(ThumbnailState.NotStarted)
    }
    
    // 淡入动画
    val alpha by animateFloatAsState(
        targetValue = if (thumbnailState is ThumbnailState.Cached) 1f else 0.7f,
        animationSpec = tween(durationMillis = 300),
        label = "thumbnail_alpha"
    )
    
    val cellShape = RoundedCornerShape(8.dp)
    val futureBorderColor = RunTheme.colorScheme.blue.copy(alpha = 0.4f)
    val bgColor = if (isDark) NoDataBgDark else NoDataBg
    val colorMode = remember { PreferencesManager(context).getTrajectoryColorMode() }

    // 请求缩略图
    LaunchedEffect(workoutId, trackPoints, isDark, isIndoor, colorMode) {
        if (isIndoor) {
            thumbnailState = ThumbnailState.NoTrajectory
            return@LaunchedEffect
        }
        if (workoutId != null && trackPoints != null) {
            // 在后台线程请求缩略图
            thumbnailState = ThumbnailState.Generating
            val result = withContext(Dispatchers.IO) {
                val manager = TrajectoryThumbnailManager.getInstance(context)
                manager.getThumbnail(
                    workoutId = workoutId,
                    trackPoints = trackPoints,
                    sizePx = sizePx,
                    isDark = isDark,
                    totalDistanceKm = totalDistanceKm,
                    colorMode = colorMode
                )
            }
            thumbnailState = result
        }
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(cellShape)
            .background(getBackgroundColor(thumbnailState, isDark, isFuture, bgColor))
            .alpha(alpha)
            .then(
                if (isFuture) {
                    Modifier.drawBehind {
                        val strokeWidth = 1.5.dp.toPx()
                        val dashLength = 4.dp.toPx()
                        val gapLength = 3.dp.toPx()
                        drawRoundRect(
                            color = futureBorderColor,
                            cornerRadius = CornerRadius(8.dp.toPx()),
                            style = Stroke(
                                width = strokeWidth,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(dashLength, gapLength),
                                    phase = 0f
                                )
                            )
                        )
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (val state = thumbnailState) {
            is ThumbnailState.Cached -> {
                // 显示缓存的缩略图
                TrajectoryImage(bitmap = state.bitmap)
            }
            
            is ThumbnailState.Generating -> {
                // 加载中
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            is ThumbnailState.NoTrajectory -> {
                // 室内跑/无轨迹 - 只显示跑步机图标，淡灰色
                Icon(
                    painter = painterResource(id = R.drawable.figure_run_treadmill),
                    contentDescription = "室内跑",
                    tint = Color.Gray.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            is ThumbnailState.Failed -> {
                // 加载失败 - 红色背景和图标
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "加载失败",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "失败",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
            
            is ThumbnailState.NotStarted -> {
                // 未开始（无数据）- 显示小圆点

//                Icon(
//                    imageVector = Icons.Default.FiberManualRecord,
//                    contentDescription = "无数据",
//                    tint = Color.Gray.copy(alpha = 0.3f),
//                    modifier = Modifier.size(8.dp)
//                )
            }
        }
    }
}

/**
 * 显示轨迹Bitmap图片
 */
@Composable
private fun TrajectoryImage(bitmap: Bitmap) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "轨迹缩略图",
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
    )
}

/**
 * 根据状态获取背景色
 */
private fun getBackgroundColor(
    state: ThumbnailState,
    isDark: Boolean,
    isFuture: Boolean,
    defaultBgColor: Color
): Color {
    return when (state) {
        is ThumbnailState.NoTrajectory -> {
            // 室内跑/无轨迹 - 淡灰色背景
            defaultBgColor.copy(alpha = 0.3f)
        }
        is ThumbnailState.Failed -> {
            // 失败 - 淡红色背景
            if (isDark) {
                Color(0xFFB71C1C).copy(alpha = 0.25f)
            } else {
                Color(0xFFFFEBEE)
            }
        }
        is ThumbnailState.Generating -> {
            // 加载中 - 半透明默认背景
            defaultBgColor.copy(alpha = 0.5f)
        }
        is ThumbnailState.NotStarted -> {
            // 未开始 - 透明背景（无数据）
            if (isFuture) {
                Color.Transparent
            } else {
                defaultBgColor.copy(alpha = 0.3f)
            }
        }
        is ThumbnailState.Cached -> {
            // 有缩略图 - 不显示背景
            Color.Transparent
        }
    }
}

