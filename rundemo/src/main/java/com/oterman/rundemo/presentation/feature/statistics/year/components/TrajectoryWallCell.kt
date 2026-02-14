package com.oterman.rundemo.presentation.feature.statistics.year.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val thumbnailManager = remember { TrajectoryThumbnailManager.getInstance(context) }

    var bitmap by remember(workoutId, isDark) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(workoutId) { mutableStateOf(true) }
    var sizePx by remember { mutableStateOf(0) }

    val borderColor = MaterialTheme.colorScheme.outlineVariant

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
            .clickable { onClick() }
            .onSizeChanged { sizePx = it.width }
            .then(
                if (bitmap == null) {
                    Modifier.drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val dashLength = 4.dp.toPx()
                        val gapLength = 4.dp.toPx()
                        val cornerRadius = 6.dp.toPx()
                        drawRoundRect(
                            color = borderColor,
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                            style = Stroke(
                                width = strokeWidth,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(dashLength, gapLength),
                                    0f
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
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
