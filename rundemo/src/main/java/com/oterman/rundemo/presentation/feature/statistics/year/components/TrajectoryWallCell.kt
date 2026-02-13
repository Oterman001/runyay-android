package com.oterman.rundemo.presentation.feature.statistics.year.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.data.repository.RunDataRepository
import com.oterman.rundemo.domain.trajectory.TrajectoryThumbnailManager

/**
 * Individual cell for the trajectory wall grid.
 * Lazily loads track points and generates thumbnail via TrajectoryThumbnailManager.
 */
@Composable
fun TrajectoryWallCell(
    workoutId: String,
    repository: RunDataRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val thumbnailManager = remember { TrajectoryThumbnailManager.getInstance(context) }

    var bitmap by remember(workoutId, isDark) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(workoutId) { mutableStateOf(true) }
    var sizePx by remember { mutableStateOf(0) }

    LaunchedEffect(workoutId, isDark, sizePx) {
        if (sizePx <= 0) return@LaunchedEffect
        isLoading = true
        try {
            val trackPoints = repository.getTrackPoints(workoutId)
            val state = thumbnailManager.getThumbnail(
                workoutId = workoutId,
                trackPoints = trackPoints,
                sizePx = sizePx,
                isDark = isDark
            )
            bitmap = when (state) {
                is com.oterman.rundemo.domain.trajectory.ThumbnailState.Cached -> state.bitmap
                else -> null
            }
        } catch (_: Exception) {
            bitmap = null
        }
        isLoading = false
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .onSizeChanged { sizePx = it.width }
    ) {
        when {
            isLoading || (bitmap == null && sizePx > 0) -> {
                ShimmerBox(modifier = Modifier.fillMaxSize())
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wall_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wall_shimmer_translate"
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

    Box(modifier = modifier.background(brush))
}
