package com.oterman.rundemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 抖动修饰符
 * 用于实现协议勾选框未勾选时的抖动效果
 */
@Composable
fun Modifier.shake(shouldShake: Boolean): Modifier {
    val offsetX = remember { Animatable(0f) }
    
    LaunchedEffect(shouldShake) {
        if (shouldShake) {
            // 抖动动画：左右移动
            for (i in 0..3) {
                offsetX.animateTo(10f, animationSpec = tween(50))
                offsetX.animateTo(-10f, animationSpec = tween(50))
            }
            offsetX.animateTo(0f, animationSpec = tween(50))
        }
    }
    
    return this.offset(x = offsetX.value.dp)
}

/**
 * 抖动容器组件
 */
@Composable
fun ShakeBox(
    shouldShake: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.shake(shouldShake)
    ) {
        content()
    }
}

