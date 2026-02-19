package com.oterman.rundemo.presentation.feature.debug

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.presentation.components.settings.SettingsCard
import com.oterman.rundemo.presentation.components.settings.SettingsItem
import com.oterman.rundemo.presentation.feature.rundetail.components.RunMapPreferences

/**
 * 调试页面
 * 提供开发调试相关功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAllRunRecords: () -> Unit = {},
    viewModel: DebugViewModel = viewModel(
        factory = DebugViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("调试") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 缓存管理
            item {
                Text(
                    text = "缓存管理",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Delete,
                        title = "清除轨迹缩略图缓存",
                        subtitle = if (uiState.isClearingThumbnailCache) "清除中..." else "清除所有轨迹缩略图缓存",
                        showDivider = false,
                        onClick = {
                            if (!uiState.isClearingThumbnailCache) {
                                viewModel.clearThumbnailCache {
                                    Toast.makeText(context, "缩略图缓存已清除", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // 数据管理
            item {
                Text(
                    text = "数据管理",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Outlined.List,
                        title = "查看所有跑步记录",
                        subtitle = "按数据源筛选、多选删除",
                        showDivider = false,
                        onClick = onNavigateToAllRunRecords
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // 地图设置
            item {
                Text(
                    text = "地图设置",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Delete,
                        title = "清除地图样式与公里点设置",
                        subtitle = "恢复地图风格、公里点开关及间隔为默认值",
                        showDivider = false,
                        onClick = {
                            RunMapPreferences.clearAll(context)
                            Toast.makeText(context, "地图设置已清除", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
