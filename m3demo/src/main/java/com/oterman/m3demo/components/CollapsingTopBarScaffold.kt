package com.oterman.m3demo.components

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/**
 * 可复用的折叠标题脚手架组件
 * 
 * @param title 页面标题
 * @param modifier 修饰符
 * @param content 页面内容，接收 PaddingValues 参数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingTopBarScaffold(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current
    
    // 创建滚动行为，支持大标题到小标题的折叠效果
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )
    
    // 计算折叠进度 (0f = 完全展开, 1f = 完全折叠)
    val collapsedFraction = scrollBehavior.state.collapsedFraction
    
    // 计算标题透明度：展开时完全不透明，折叠过程中逐渐透明
    val titleAlpha = 1f - (collapsedFraction * 0.3f)
    
    // 计算标题缩放：展开时正常大小，折叠时稍微缩小
    val titleScale = 1f - (collapsedFraction * 0.1f)
    
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        modifier = Modifier.graphicsLayer {
                            alpha = titleAlpha
                            scaleX = titleScale
                            scaleY = titleScale
                        }
                    )
                },
                actions = {
                    // 右上角固定按钮
                    IconButton(
                        onClick = {
                            Toast.makeText(
                                context,
                                "点击了 $title 的操作按钮",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "更多操作"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            content(paddingValues)
        }
    }
}

