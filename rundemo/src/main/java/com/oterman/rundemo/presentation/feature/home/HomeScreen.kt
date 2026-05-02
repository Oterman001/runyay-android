package com.oterman.rundemo.presentation.feature.home

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.res.painterResource
import com.oterman.rundemo.R
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.Manifest
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.ThemeMode
import com.oterman.rundemo.presentation.feature.home.components.FitImportConflictDialog
import com.oterman.rundemo.presentation.feature.home.tabs.DataTabContent
import com.oterman.rundemo.presentation.feature.home.tabs.DashboardTabContent
import com.oterman.rundemo.presentation.feature.home.tabs.ProfileTabContent
import com.oterman.rundemo.presentation.feature.home.tabs.UpdateAvailableDialog
import com.oterman.rundemo.presentation.feature.home.tabs.WifiWarningDialog
import com.oterman.rundemo.presentation.feature.home.tabs.triggerLocalInstall
import com.oterman.rundemo.service.update.ApkDownloadService
import com.oterman.rundemo.service.update.ApkDownloadState

/**
 * Main Home screen with bottom navigation
 * Corresponds to iOS MainTabView
 */
@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToWelcome: () -> Unit = {},
    onNavigateToRunDetail: (workoutId: String) -> Unit = {},
    onNavigateToRunDetailDebug: (workoutId: String) -> Unit = {},
    onNavigateToDataSourceManage: () -> Unit = {},
    onNavigateToUserProfile: () -> Unit = {},
    onNavigateToRunGoalSet: () -> Unit = {},
    onNavigateToHearRateZoneSet: () -> Unit = {},
    onNavigateToRunStatistics: (tab: String) -> Unit = {},
    onNavigateToDebug: () -> Unit = {},
    onNavigateToContactUs: () -> Unit = {},
    onNavigateToSyncStatus: () -> Unit = {},
    onNavigateToVdotDetail: () -> Unit = {},
    onNavigateToRunningShoes: () -> Unit = {},
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 通知权限请求 launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // 同步已在进行，关闭弹窗；若用户拒绝，记录今日日期
        viewModel.dismissNotificationPermissionRequest(saveDenial = !granted)
    }

    // APK 下载通知权限请求 launcher
    val downloadNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.dismissDownloadPermissionRequest()
    }

    // 进入首页时自动触发同步 + 检查强制更新
    LaunchedEffect(Unit) {
        viewModel.startSyncIfNeeded()
        viewModel.checkUpdateOnLaunch()
    }

    // ON_RESUME 重检版本（用户从应用市场返回后触发）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.checkUpdateOnLaunch()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 同步成功消息 Snackbar（展示 1 秒后自动关闭）
    LaunchedEffect(uiState.syncSuccessMessage) {
        uiState.syncSuccessMessage?.let { message ->
            val job = launch { snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short) }
            delay(2000L)
            job.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()
            viewModel.dismissSyncSuccess()
        }
    }

    // APK 下载开始提示 Snackbar
    LaunchedEffect(uiState.apkDownloadStartedMessage) {
        uiState.apkDownloadStartedMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.dismissApkDownloadStartedMessage()
        }
    }

    // APK 下载通知权限申请（仅 Android 13+）
    LaunchedEffect(uiState.needsNotificationPermissionForDownload) {
        if (uiState.needsNotificationPermissionForDownload &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            downloadNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // APK 下载完成后自动触发安装（用户切换 Tab 后 ProfileTab 不再 compose，需在此层监听）
    val downloadState by ApkDownloadService.downloadState.collectAsState()
    LaunchedEffect(downloadState) {
        if (downloadState is ApkDownloadState.Completed) {
            val apkFile = (downloadState as ApkDownloadState.Completed).apkFile
            triggerLocalInstall(context, apkFile.absolutePath)
        }
    }

    // Handle navigation events
    LaunchedEffect(uiState.navigateToLogin) {
        if (uiState.navigateToLogin) {
            onNavigateToLogin()
            viewModel.resetNavigateToLogin()
        }
    }

    LaunchedEffect(uiState.navigateToWelcome) {
        if (uiState.navigateToWelcome) {
            onNavigateToWelcome()
            viewModel.resetNavigateToWelcome()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            HomeBottomNavigationBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = viewModel::selectTab
            )
        }
    ) { paddingValues ->
        // Tab content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.selectedTab) {
                HomeTab.DASHBOARD -> DashboardTabContent(
                    showSyncIcon = uiState.showSyncIcon,
                    isSyncing = uiState.isSyncing,
                    isRefreshing = uiState.isSyncing && !uiState.showSyncIcon,
                    onPullToRefresh = viewModel::manualSync,
                    onSyncIconClick = onNavigateToSyncStatus,
                    onSetGoalClick = onNavigateToRunGoalSet,
                    onNavigateToRunDetail = onNavigateToRunDetail,
                    onNavigateToRunStatistics = onNavigateToRunStatistics,
                    onNavigateToVdotDetail = {
                        // TODO: 跑力详情页面待实现
                        android.widget.Toast.makeText(
                            context,
                            "跑力详情页面即将上线",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onSwitchToDataTab = { viewModel.selectTab(HomeTab.DATA) }
                )
                HomeTab.DATA -> DataTabContent(
                    onRecordClick = { workoutId -> onNavigateToRunDetail(workoutId) },
                    onRecordLongClick = { workoutId -> onNavigateToRunDetailDebug(workoutId) },
                    onNavigateToDataSourceManage = onNavigateToDataSourceManage
                )
                HomeTab.PROFILE -> ProfileTabContent(
                    isLoggedIn = uiState.isLoggedIn,
                    userId = uiState.userId,
                    userName = uiState.userName,
                    phoneNumber = uiState.phoneNumber,
                    avatarUrl = uiState.avatarUrl,
                    isLoadingAvatar = uiState.isLoadingAvatar,
                    onLogoutClick = viewModel::showLogoutConfirmation,
                    onLoginClick = viewModel::navigateToLogin,
                    onUserProfileClick = onNavigateToUserProfile,
                    onShowWelcomeClick = viewModel::navigateToWelcome,
                    onResetFirstLaunchClick = viewModel::resetFirstLaunch,
                    onDataSourceManageClick = onNavigateToDataSourceManage,
                    onRunGoalClick = onNavigateToRunGoalSet,
                    onHearRateZoneClick = onNavigateToHearRateZoneSet,
                    onRunningShoesClick = onNavigateToRunningShoes,
                    onDebugClick = onNavigateToDebug,
                    onContactUsClick = onNavigateToContactUs,
                    onThemeModeChanged = onThemeModeChanged,
                    onStartApkDownload = { url, versionCode ->
                        viewModel.startApkDownload(url, versionCode)
                    }
                )
            }
        }
    }

    // Logout confirmation dialog
    if (uiState.showLogoutConfirmDialog) {
        LogoutConfirmationDialog(
            onConfirm = viewModel::logout,
            onDismiss = viewModel::dismissLogoutConfirmation
        )
    }
    
    // FIT文件导入结果对话框
    if (uiState.showImportResultDialog) {
        FitImportResultDialog(
            result = uiState.fitImportResult,
            onDismiss = viewModel::dismissImportResultDialog
        )
    }

    // FIT时间冲突对话框
    if (uiState.showConflictDialog) {
        FitImportConflictDialog(
            conflictingRecords = uiState.conflictingRecords,
            onConfirm = viewModel::confirmConflictImport,
            onSkip = viewModel::skipConflictImport,
            onViewDetail = onNavigateToRunDetail
        )
    }

    // 通知权限引导弹窗
    if (uiState.needsNotificationPermission) {
        NotificationPermissionDialog(
            onConfirm = {
                viewModel.dismissNotificationPermissionRequest(saveDenial = false) // 结果由系统弹窗回调决定
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = {
                viewModel.dismissNotificationPermissionRequest(saveDenial = true) // 用户明确拒绝
            }
        )
    }

    // 强制更新弹窗（不可关闭）
    val forceUpdateInfo = uiState.forceUpdateInfo
    if (uiState.showForceUpdateDialog && forceUpdateInfo != null) {
        var showForceWifiWarning by remember { mutableStateOf(false) }

        if (showForceWifiWarning) {
            WifiWarningDialog(
                onConfirm = {
                    showForceWifiWarning = false
                    val url = forceUpdateInfo.response.downloadUrl ?: return@WifiWarningDialog
                    viewModel.startApkDownload(url, forceUpdateInfo.response.versionCode ?: -1)
                    viewModel.dismissForceUpdateDialog()
                },
                onDismiss = { showForceWifiWarning = false }
            )
        }

        UpdateAvailableDialog(
            info = forceUpdateInfo.response,
            isAlreadyDownloaded = forceUpdateInfo.isAlreadyDownloaded,
            resolvedMarket = uiState.resolvedMarket,
            onUpdate = {
                val resolved = uiState.resolvedMarket
                if (resolved != null) {
                    viewModel.openMarketForUpdate()
                    // 强制更新：对话框保持，等用户从市场回来后 ON_RESUME 重新验证版本
                    // 非强制更新：dismiss
                    if (forceUpdateInfo.response.forceUpgrade != true) {
                        viewModel.dismissForceUpdateDialog()
                    }
                } else if (forceUpdateInfo.isAlreadyDownloaded && forceUpdateInfo.localApkPath != null) {
                    triggerInstall(context, forceUpdateInfo.localApkPath)
                    viewModel.dismissForceUpdateDialog()
                } else {
                    val url = forceUpdateInfo.response.downloadUrl ?: return@UpdateAvailableDialog
                    val cm = context.getSystemService(ConnectivityManager::class.java)
                    val isWifi = cm.getNetworkCapabilities(cm.activeNetwork)
                        ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                    if (isWifi) {
                        viewModel.startApkDownload(url, forceUpdateInfo.response.versionCode ?: -1)
                        viewModel.dismissForceUpdateDialog()
                    } else {
                        showForceWifiWarning = true
                    }
                }
            },
            onDismiss = { /* forceUpgrade=true 时 dialog 内部不会调用此回调 */ }
        )
    }
}

private fun triggerInstall(context: android.content.Context, apkPath: String) {
    try {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            Toast.makeText(context, "安装包文件不存在，请重新下载", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "启动安装失败，请重试", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Bottom navigation bar with 3 tabs
 * Uses Material 3 NavigationBar
 */
@Composable
private fun HomeBottomNavigationBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    val tabColors = NavigationBarItemDefaults.colors(
        selectedIconColor = RunTheme.colorScheme.blue,
        selectedTextColor = RunTheme.colorScheme.blue,
        indicatorColor = Color.Transparent,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalDivider(
            color = RunTheme.colorScheme.divider,
            thickness = 0.5.dp
        )
        NavigationBar(
            modifier = Modifier.height(56.dp),
            containerColor = MaterialTheme.colorScheme.background,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
        NavigationBarItem(
            selected = selectedTab == HomeTab.DASHBOARD,
            onClick = { onTabSelected(HomeTab.DASHBOARD) },
            colors = tabColors,
            icon = {
                Icon(
                    imageVector = if (selectedTab == HomeTab.DASHBOARD) {
                        Icons.Filled.Home
                    } else {
                        Icons.Outlined.Home
                    },
                    contentDescription = "仪表盘"
                )
            }
        )

        NavigationBarItem(
            selected = selectedTab == HomeTab.DATA,
            onClick = { onTabSelected(HomeTab.DATA) },
            colors = tabColors,
            icon = {
                Icon(
                    imageVector = if (selectedTab == HomeTab.DATA) {
                        Icons.Filled.BarChart
                    } else {
                        Icons.Outlined.BarChart
                    },
                    contentDescription = "跑步记录"
                )
            }
        )

        NavigationBarItem(
            selected = selectedTab == HomeTab.PROFILE,
            onClick = { onTabSelected(HomeTab.PROFILE) },
            colors = tabColors,
            icon = {
                Icon(
                    imageVector = if (selectedTab == HomeTab.PROFILE) {
                        Icons.Filled.Person
                    } else {
                        Icons.Outlined.Person
                    },
                    contentDescription = "我的"
                )
            }
        )
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

/**
 * Logout confirmation dialog
 */
@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("退出登录") },
        text = { Text("确定要退出登录吗？") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * FIT文件导入结果对话框
 */
@Composable
private fun FitImportResultDialog(
    result: FitImportResult?,
    onDismiss: () -> Unit
) {
    val (title, message) = when (result) {
        is FitImportResult.Success -> {
            "导入成功" to "已成功导入跑步记录\n距离：${String.format("%.2f", result.distance)} 公里\n时长：${String.format("%.1f", result.duration)} 分钟"
        }
        is FitImportResult.AlreadyExists -> {
            "文件已存在" to "该FIT文件之前已导入过，无需重复导入"
        }
        is FitImportResult.Error -> {
            "导入失败" to result.message
        }
        is FitImportResult.UploadFailed -> {
            "上传失败" to result.message
        }
        is FitImportResult.ConflictFound -> return
        null -> return
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

/**
 * 通知权限引导弹窗
 */
@Composable
private fun NotificationPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("开启通知权限") },
        text = { Text("数据同步时需要在通知栏显示同步进度，帮助您了解同步状态。请开启通知权限以获得更好的体验。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("去开启")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("暂不开启")
            }
        }
    )
}
