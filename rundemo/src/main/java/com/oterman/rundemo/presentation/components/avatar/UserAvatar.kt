package com.oterman.rundemo.presentation.components.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlin.math.abs

/**
 * 预设的渐变色对，每组包含起始色和结束色（左上→右下方向）
 * 根据 userId 哈希值确定性地选取，保证同一用户始终显示相同颜色
 */
private val GRADIENT_PAIRS: List<Pair<Color, Color>> = listOf(
    Color(0xFFE91E63) to Color(0xFFFF5722),  // 玫红→深橙
    Color(0xFF3F51B5) to Color(0xFF03A9F4),  // 靛蓝→天蓝
    Color(0xFF4CAF50) to Color(0xFF26C6DA),  // 绿→青
    Color(0xFF9C27B0) to Color(0xFFE91E63),  // 紫→玫红
    Color(0xFFFF9800) to Color(0xFFFFEB3B),  // 橙→黄
    Color(0xFF009688) to Color(0xFF4CAF50),  // 青绿→绿
    Color(0xFF673AB7) to Color(0xFF3F51B5),  // 深紫→靛蓝
    Color(0xFFFF5722) to Color(0xFFFF9800),  // 深橙→橙
    Color(0xFF2196F3) to Color(0xFF9C27B0),  // 蓝→紫
    Color(0xFF00BCD4) to Color(0xFF3F51B5),  // 青→靛蓝
)

/**
 * 根据 seed（userId）确定性地获取渐变色对
 */
private fun getGradientForSeed(seed: String): Pair<Color, Color> {
    val index = abs(seed.hashCode()) % GRADIENT_PAIRS.size
    return GRADIENT_PAIRS[index]
}

/**
 * 从显示名称中提取头像首字符
 * 优先取第一个 Unicode 字符（支持中文、emoji 等）
 */
private fun getInitial(displayName: String?): String {
    if (displayName.isNullOrBlank()) return "?"
    val trimmed = displayName.trim()
    val codePoint = trimmed.codePointAt(0)
    return String(Character.toChars(codePoint)).uppercase()
}

/**
 * 默认头像内容：基于 seed 生成渐变背景 + 首字母
 * 无需外部库，纯 Compose 实现
 *
 * @param seed 生成颜色的种子（通常为 userId），相同 seed 永远渲染相同颜色
 * @param displayName 用于提取首字母的显示名称（userName）
 * @param size 头像尺寸
 */
@Composable
fun DefaultAvatarContent(
    seed: String,
    displayName: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val (startColor, endColor) = remember(seed) { getGradientForSeed(seed) }
    val initial = remember(displayName) { getInitial(displayName) }
    val fontSize = remember(size) { (size.value * 0.38f).sp }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(startColor, endColor),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
    }
}

/**
 * 统一用户头像组件
 *
 * 处理三种状态：
 * 1. 加载中（isLoading=true）：显示圆形进度指示器
 * 2. 无头像 URL：显示基于 userId 生成的渐变默认头像
 * 3. 有头像 URL：通过 Coil 加载网络图片，加载/失败时回退到默认头像
 *
 * @param avatarUrl 头像网络 URL（签名临时 URL）
 * @param isLoading 是否正在获取 avatarUrl（外部加载状态）
 * @param userId 用户 ID，用作生成默认头像颜色的种子
 * @param userName 用户名，用于提取默认头像的首字母
 * @param size 头像尺寸
 * @param modifier Modifier
 */
@Composable
fun UserAvatar(
    avatarUrl: String?,
    isLoading: Boolean,
    userId: String?,
    userName: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val seed = userId ?: userName ?: "default"

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size * 0.4f),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            avatarUrl.isNullOrBlank() -> {
                DefaultAvatarContent(
                    seed = seed,
                    displayName = userName,
                    size = size,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .allowHardware(false)
                        .build(),
                    contentDescription = "头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                        DefaultAvatarContent(
                            seed = seed,
                            displayName = userName,
                            size = size,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
            }
        }
    }
}
