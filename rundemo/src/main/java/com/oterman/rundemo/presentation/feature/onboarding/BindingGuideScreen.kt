package com.oterman.rundemo.presentation.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.R
import com.oterman.rundemo.domain.model.DataSourceInfo
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.presentation.components.GradientButton
import com.oterman.rundemo.ui.theme.RunTheme
import kotlinx.coroutines.delay

/**
 * 绑定引导页
 * 引导用户绑定运动手表平台（佳明/高驰）
 */
@Composable
fun BindingGuideScreen(
    viewModel: BindingGuideViewModel,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    onNavigateToDetail: (DataSourcePlatform) -> Unit,
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // 每次屏幕显示时刷新绑定状态（处理从详情页授权返回的情况）
    LaunchedEffect(Unit) {
        viewModel.refreshAfterBinding()
    }

    // 监听自动跳转事件
    LaunchedEffect(uiState.shouldNavigateToHome) {
        if (uiState.shouldNavigateToHome) {
            onNavigateToHome()
        }
    }

    // 加载中或检查中，显示空白（避免闪烁）
    if (uiState.isLoading || uiState.isCheckingComplete) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // 如果 shouldNavigateToHome 已设置，不渲染内容（等待导航）
    if (uiState.shouldNavigateToHome) return

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // 顶部跳过按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = "跳过",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 图示区域
            IllustrationSection()

            Spacer(modifier = Modifier.height(24.dp))

            // 标题
            Text(
                text = "连接你的运动手表",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 副标题
            Text(
                text = "绑定佳明或高驰账号，自动同步跑步数据，训练记录、GPS轨迹、配速分析，一键掌握。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 平台卡片列表（带级联动画）
            uiState.platforms.forEachIndexed { index, platform ->
                var visible by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(index * 100L)
                    visible = true
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(300)) +
                            slideInVertically(
                                animationSpec = tween(300),
                                initialOffsetY = { it / 4 }
                            )
                ) {
                    OnboardingPlatformCard(
                        dataSourceInfo = platform,
                        onClick = { onNavigateToDetail(platform.platform) }
                    )
                }

                if (index < uiState.platforms.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 开始使用按钮（至少绑定一个时显示）
            AnimatedVisibility(
                visible = uiState.hasAnyBound,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
            ) {
                GradientButton(
                    text = "开始使用",
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 底部提示
            Text(
                text = "你也可以稍后在「我的」-「数据源管理」中绑定",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )
        }
    }
}

/**
 * 图示区域：App图标 - 虚线动画 - 手表图标
 */
@Composable
private fun IllustrationSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App图标
        Image(
            painter = painterResource(id = R.drawable.run_demo),
            contentDescription = "App",
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(13.dp))
            ,
        )

        // 连接动画指示
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .alpha(alpha),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 2.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }

        // 手表图标
        Icon(
            painter = painterResource(R.drawable.watch_analog),
//            imageVector = Icons.Outlined.Watch,
            contentDescription = "手表",
            modifier = Modifier.size(60.dp)
//            tint = RunTheme.colorScheme.blue
        )
    }
}

/**
 * 引导页平台卡片
 */
@Composable
private fun OnboardingPlatformCard(
    dataSourceInfo: DataSourceInfo,
    onClick: () -> Unit
) {
    val isBound = dataSourceInfo.isAuthorized
    val backgroundColor = if (isBound) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 平台图标
        Image(
            painter = painterResource(id = dataSourceInfo.platform.iconResId),
            contentDescription = dataSourceInfo.platform.displayName,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 平台名称和状态
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dataSourceInfo.platform.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isBound) "已绑定" else "点击绑定",
                style = MaterialTheme.typography.bodySmall,
                color = if (isBound) {
                    RunTheme.colorScheme.success
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // 右侧图标
        if (isBound) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "已绑定",
                tint = RunTheme.colorScheme.success,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "前往绑定",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
