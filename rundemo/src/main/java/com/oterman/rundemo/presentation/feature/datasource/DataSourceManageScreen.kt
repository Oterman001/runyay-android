package com.oterman.rundemo.presentation.feature.datasource

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.presentation.feature.datasource.components.DataSourceItem

/**
 * 数据源管理页面
 * 对应iOS的DataSourceManagePage
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSourceManageScreen(
    viewModel: DataSourceManageViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (DataSourcePlatform) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()

    // 每次屏幕显示时刷新数据源状态（处理从详情页授权返回的情况）
    LaunchedEffect(Unit) {
        viewModel.refreshDataSources()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "数据源管理",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isEditingOrder) {
                            viewModel.cancelEditingOrder()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (uiState.isEditingOrder) {
                        TextButton(onClick = { viewModel.saveOrder() }) {
                            Text(
                                text = "保存",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 加载状态
            if (uiState.isLoading && uiState.dataSources.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 数据源列表
                    val displayList = if (uiState.isEditingOrder) {
                        uiState.sortableDataSources
                    } else {
                        uiState.displayDataSources
                    }
                    
                    itemsIndexed(
                        items = displayList,
                        key = { _, item -> item.platform.code }
                    ) { _, dataSourceInfo ->
                        DataSourceItem(
                            dataSourceInfo = dataSourceInfo,
                            isEditMode = uiState.isEditingOrder,
                            showPriority = dataSourceInfo.platform.supportsSorting,
                            onClick = {
                                if (!uiState.isEditingOrder) {
                                    if (viewModel.isComingSoonDataSource(dataSourceInfo.platform)) {
                                        viewModel.showComingSoonDialog()
                                    } else {
                                        onNavigateToDetail(dataSourceInfo.platform)
                                    }
                                }
                            }
                        )
                    }
                    
                    // 编辑模式说明
                    if (uiState.isEditingOrder) {
                        item {
                            PriorityExplanationCard()
                        }
                    }
                }
                
                // 底部按钮区域（非编辑模式）
                AnimatedVisibility(
                    visible = !uiState.isEditingOrder,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    BottomButtonSection(
                        onEditOrderClick = { viewModel.startEditingOrder() }
                    )
                }
            }
        }
    }
    
    // 即将支持弹窗
    if (uiState.showComingSoonDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissComingSoonDialog() },
            title = { Text("即将支持") },
            text = { Text("该功能即将上线，敬请期待") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissComingSoonDialog() }) {
                    Text("确定")
                }
            }
        )
    }
    
    // 需要登录弹窗
    if (uiState.showLoginRequiredDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLoginRequiredDialog() },
            title = { Text("需要登录") },
            text = { Text("该功能需要登录后才能使用，请先登录您的账户") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissLoginRequiredDialog()
                    onNavigateToLogin()
                }) {
                    Text("去登录")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLoginRequiredDialog() }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 优先级说明卡片
 */
@Composable
private fun PriorityExplanationCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "数据源优先级",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "长按拖动调整顺序",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "排在前面的数据源优先级更高。当多个数据源有冲突的运动数据时，会优先采用排序靠前的数据源。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 底部按钮区域
 */
@Composable
private fun BottomButtonSection(
    onEditOrderClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onEditOrderClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "调整数据源优先级",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }
        
        Text(
            text = "碰到问题？联系我们",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(0.7f)
        )
    }
}

