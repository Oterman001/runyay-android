package com.oterman.rundemo.presentation.feature.datasource

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.R

/**
 * 数据源详情页面
 * 对应iOS的DataSourceDetailPage
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSourceDetailScreen(
    viewModel: DataSourceDetailViewModel,
    onNavigateBack: () -> Unit,
    onOpenOAuthWebView: (String) -> Unit,
    onNavigateToDebug: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 刷新授权状态
    LaunchedEffect(Unit) {
        viewModel.refreshAuthStatus()
    }
    
    // 处理OAuth WebView打开
    LaunchedEffect(uiState.showOAuthWebView, uiState.authUrl) {
        if (uiState.showOAuthWebView && uiState.authUrl != null) {
            onOpenOAuthWebView(uiState.authUrl!!)
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = uiState.platform.displayName,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部图标区域
                item {
                    TopIconsSection(
                        platform = uiState.platform,
                        onQuickTap = { onNavigateToDebug?.invoke() }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
                
                // 说明区域
                item {
                    DescriptionSection(
                        descriptions = uiState.dataSourceInfo?.descriptions ?: emptyList()
                    )
                }
                
                // 同步状态区域
                if (uiState.isSyncing || uiState.importedRecords.isNotEmpty()) {
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                    
                    item {
                        SyncStatusSection(
                            isSyncing = uiState.isSyncing,
                            isSyncFinished = uiState.isSyncFinished,
                            importedRecords = uiState.importedRecords
                        )
                    }
                    item { Spacer(modifier = Modifier.height(60.dp)) }
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
            
            // 底部按钮区域
            BottomButtonsSection(
                isAuthorized = uiState.isAuthorized,
                isLoading = uiState.isLoading,
                isUnbinding = uiState.isUnbinding,
                isSyncing = uiState.isSyncing,
                onAuthClick = { viewModel.startAuthorization() },
                onSyncClick = { viewModel.manualSync() },
                onUnbindClick = { viewModel.showUnbindConfirmDialog() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
    
    // 解绑确认弹窗
    if (uiState.showUnbindConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUnbindConfirmDialog() },
            title = { Text("取消授权") },
            text = { 
                Text("确定要取消${uiState.platform.displayName}的授权吗？取消后将无法同步该平台的运动数据。") 
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmUnbind() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确认取消")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUnbindConfirmDialog() }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 提示消息弹窗
    uiState.alertMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearAlertMessage() },
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAlertMessage() }) {
                    Text("确定")
                }
            }
        )
    }
}

/**
 * 顶部图标区域
 * 支持快速点击5次进入调试界面
 */
@Composable
private fun TopIconsSection(
    platform: com.oterman.rundemo.domain.model.DataSourcePlatform,
    onQuickTap: () -> Unit
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp)
            .clickable {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < 500) {
                    tapCount++
                    if (tapCount >= 5) {
                        onQuickTap()
                        tapCount = 0
                    }
                } else {
                    tapCount = 1
                }
                lastTapTime = currentTime
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // RunDemo图标
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.run_duck),
                contentDescription = "RunDemo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "RunDemo",
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        // 链接图标
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(24.dp))
        
        // 平台图标
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = platform.iconResId),
                contentDescription = platform.displayName,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = platform.appBrandName,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * 说明区域
 */
@Composable
private fun DescriptionSection(descriptions: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "说明",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        descriptions.forEach { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

/**
 * 同步状态区域
 */
@Composable
private fun SyncStatusSection(
    isSyncing: Boolean,
    isSyncFinished: Boolean,
    importedRecords: List<com.oterman.rundemo.domain.model.ImportedRunSummary>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isSyncFinished) "本次同步完成" else "数据同步中",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            if (!isSyncFinished && isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (importedRecords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                modifier = Modifier.height(120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(importedRecords) { record ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = record.displayText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
//        Spacer(modifier = Modifier.height(8.dp))
        
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.spacedBy(4.dp)
//        ) {
//            Icon(
//                imageVector = Icons.Default.Info,
//                contentDescription = null,
//                tint = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.size(12.dp)
//            )
//            Text(
//                text = "实时显示最新导入的运动记录（最新50条）",
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
    }
}

/**
 * 底部按钮区域
 */
@Composable
private fun BottomButtonsSection(
    isAuthorized: Boolean,
    isLoading: Boolean,
    isUnbinding: Boolean,
    isSyncing: Boolean,
    onAuthClick: () -> Unit,
    onSyncClick: () -> Unit,
    onUnbindClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 主按钮
        Button(
            onClick = {
                if (isAuthorized) {
                    onSyncClick()
                } else {
                    onAuthClick()
                }
            },
            enabled = !isLoading && !isUnbinding && !isSyncing,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isLoading || isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = if (isAuthorized) "手动同步" else "授权链接",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        // 取消授权按钮（仅在已授权时显示）
        if (isAuthorized) {
            TextButton(
                onClick = onUnbindClick,
                enabled = !isUnbinding && !isLoading && !isSyncing
            ) {
                if (isUnbinding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "取消授权",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

