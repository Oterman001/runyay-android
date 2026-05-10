package com.oterman.rundemo.presentation.feature.home.tabs

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.core.content.FileProvider
import com.oterman.rundemo.util.RLog
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import com.oterman.rundemo.R
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.oterman.rundemo.BuildConfig
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.repository.AppUpdateInfo
import com.oterman.rundemo.data.repository.AppUpdateRepository
import com.oterman.rundemo.presentation.components.trajectory.TrajectoryColorMode
import com.oterman.rundemo.service.update.ApkDownloadService
import com.oterman.rundemo.service.update.ApkDownloadState
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.ThemeMode
import com.oterman.rundemo.presentation.components.settings.SettingsCard
import com.oterman.rundemo.presentation.components.settings.SettingsItem
import com.oterman.rundemo.presentation.components.settings.UserProfileCard
import com.oterman.rundemo.presentation.feature.mcp.AuthorizationSummaryViewModel
import com.oterman.rundemo.presentation.feature.mcp.AuthorizationSummaryViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Profile/Settings tab content with iOS-style NavigationTitle effect
 * Large title collapses to small title when scrolling
 * Corresponds to iOS SettingPage
 */
@Composable
fun ProfileTabContent(
    isLoggedIn: Boolean,
    userId: String? = null,
    userName: String?,
    phoneNumber: String? = null,
    avatarUrl: String? = null,
    isLoadingAvatar: Boolean = false,
    onLogoutClick: () -> Unit,
    onLoginClick: () -> Unit,
    onUserProfileClick: () -> Unit = {},
    onShowWelcomeClick: () -> Unit,
    onResetFirstLaunchClick: () -> Unit,
    onDataSourceManageClick: () -> Unit = {},
    onMcpConnectionManageClick: () -> Unit = {},
    onRunGoalClick: () -> Unit = {},
    onHearRateZoneClick: () -> Unit = {},
    onRunningShoesClick: () -> Unit = {},
    onDebugClick: () -> Unit = {},
    onContactUsClick: () -> Unit = {},
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    onStartApkDownload: (url: String, versionCode: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val backgroundColor = MaterialTheme.colorScheme.background
    val coroutineScope = rememberCoroutineScope()

    val preferencesManager = remember { PreferencesManager(context) }
    val authorizationSummaryViewModel: AuthorizationSummaryViewModel = viewModel(
        factory = AuthorizationSummaryViewModelFactory(context)
    )
    val authorizationSummary by authorizationSummaryViewModel.uiState.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) authorizationSummaryViewModel.load()
    }

    // App update state
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showWifiWarning by remember { mutableStateOf(false) }
    val downloadState by ApkDownloadService.downloadState.collectAsState()

    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is ApkDownloadState.Completed -> {
                triggerLocalInstall(context, state.apkFile.absolutePath)
            }
            is ApkDownloadState.Failed -> {
                Toast.makeText(context, "下载失败: ${state.message}", Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    // Trajectory color mode state
    var showTrajectoryColorSheet by remember { mutableStateOf(false) }
    var currentColorMode by remember { mutableStateOf(preferencesManager.getTrajectoryColorMode()) }

    // Appearance mode state
    var showAppearanceSheet by remember { mutableStateOf(false) }
    var currentThemeMode by remember { mutableStateOf(preferencesManager.getThemeMode()) }
    val currentColorModeLabel = when (currentColorMode) {
        TrajectoryColorMode.FIXED -> "固定配色"
        TrajectoryColorMode.DISTANCE_BASED -> "距离分色"
    }

    // Calculate collapse progress based on scroll offset
    val collapseProgress by remember {
        derivedStateOf {
            val firstItemIndex = lazyListState.firstVisibleItemIndex
            val firstItemOffset = lazyListState.firstVisibleItemScrollOffset

            if (firstItemIndex > 0) {
                1f
            } else {
                (firstItemOffset / 200f).coerceIn(0f, 1f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Collapsed header (small title) - appears when scrolled
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .zIndex(1f)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .alpha(collapseProgress)
        ) {
            Text(
                text = "我的",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Main content
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large title header (iOS NavigationTitle style)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, top = 48.dp, bottom = 16.dp)
                        .graphicsLayer {
                            val scale = 1f - (collapseProgress * 0.15f)
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - collapseProgress
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        }
                ) {
                    Text(
                        text = "我的",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 0.sp
                    )
                }
            }

            // User Profile Card
            item {
                UserProfileCard(
                    isLoggedIn = isLoggedIn,
                    userId = userId,
                    userName = userName,
                    phoneNumber = phoneNumber,
                    avatarUrl = avatarUrl,
                    isLoadingAvatar = isLoadingAvatar,
                    onClick = {
                        if (isLoggedIn) {
                            onUserProfileClick()
                        } else {
                            onLoginClick()
                        }
                    }
                )
            }

            // Debug Group (Show Welcome, Reset First Launch) - 仅Debug版本可见
            if (BuildConfig.DEBUG) {
                item { Spacer(modifier = Modifier.height(20.dp)) }
                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Outlined.Flag,
                            title = "调试工具",
                            subtitle = "缓存管理、开发调试",
                            iconTint = RunTheme.colorScheme.blue,
                            showDivider = false,
                            onClick = onDebugClick
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // Settings Group: Data Import & Sync
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Sync,
                        title = "数据源管理",
                        subtitle = "绑定佳明、高驰、手动导入等",
                        iconTint = RunTheme.colorScheme.blue,
                        showDivider = true,
                        onClick = onDataSourceManageClick
                    )
                    SettingsItem(
                        iconResId = R.drawable.svg_setting_shoes,
                        title = "跑鞋管理",
                        subtitle = "管理你的跑鞋装备",
                        iconTint = RunTheme.colorScheme.blue,
                        onClick = onRunningShoesClick,
                        showDivider = true
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Key,
                        title = "连接与授权管理",
                        subtitle = "AI 连接与合作方数据授权",
                        iconTint = RunTheme.colorScheme.blue,
                        onClick = onMcpConnectionManageClick,
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = authorizationSummary.mcpConnectionCount?.let { "$it 个连接" } ?: "MCP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RunTheme.colorScheme.blue,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = authorizationSummary.partnerAuthorizationCount?.let { "$it 个合作方" } ?: "合作方授权",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        showDivider = false
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // Settings Group 1: Training Settings
            item {
                SettingsCard {

                    SettingsItem(
                        icon = Icons.Outlined.Flag,
                        title = "跑步目标",
                        iconTint = RunTheme.colorScheme.blue,
                        onClick = onRunGoalClick
                    )
                    SettingsItem(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = "心率区间",
                        iconTint = RunTheme.colorScheme.blue,
                        onClick = onHearRateZoneClick
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Palette,
                        title = "外观设置",
                        subtitle = when (currentThemeMode) {
                            ThemeMode.AUTO -> "自动"
                            ThemeMode.LIGHT -> "亮色"
                            ThemeMode.DARK -> "暗色"
                        },
                        iconTint = RunTheme.colorScheme.blue,
                        showDivider = true,
                        onClick = { showAppearanceSheet = true }
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Route,
                        title = "轨迹配色",
                        subtitle = currentColorModeLabel,
                        iconTint = RunTheme.colorScheme.blue,
                        showDivider = false,
                        onClick = { showTrajectoryColorSheet = true }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // Settings Group 2: Support
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Email,
                        title = "联系我们",
                        iconTint = RunTheme.colorScheme.blue,
                        showDivider = true,
                        onClick = onContactUsClick
                    )
                    SettingsItem(
                        icon = Icons.Outlined.SystemUpdate,
                        title = "检查更新",
                        subtitle = if (isCheckingUpdate) "检查中..." else null,
                        iconTint = RunTheme.colorScheme.blue,
                        showDivider = false,
                        onClick = {
                            if (!isCheckingUpdate) {
                                coroutineScope.launch {
                                    isCheckingUpdate = true
                                    val result = withContext(Dispatchers.IO) {
                                        AppUpdateRepository.checkLatestVersion(context)
                                    }
                                    isCheckingUpdate = false
                                    result.onSuccess { info ->
                                        if (info != null) {
                                            updateInfo = info
                                            showUpdateDialog = true
                                        } else {
                                            Toast.makeText(context, "暂无新版本", Toast.LENGTH_SHORT).show()
                                        }
                                    }.onFailure {
                                        Toast.makeText(context, "检查更新失败，请稍后重试", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
//                    SettingsItem(
//                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
//                        title = "帮助与反馈",
//                        iconTint = RunTheme.colorScheme.blue,
//                        onClick = { /* TODO: Navigate to help page */ }
//                    )
//                    SettingsItem(
//                        icon = Icons.Outlined.Star,
//                        title = "给个好评",
//                        iconTint = RunTheme.colorScheme.blue,
//                        showDivider = false,
//                        onClick = { /* TODO: Open app store rating */ }
//                    )
                }
            }


            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Logout button (only when logged in)
//            if (isLoggedIn) {
//                item {
//                    OutlinedButton(
//                        onClick = onLogoutClick,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(48.dp),
//                        shape = RoundedCornerShape(12.dp),
//                        colors = ButtonDefaults.outlinedButtonColors(
//                            contentColor = MaterialTheme.colorScheme.error
//                        )
//                    ) {
//                        Text(
//                            text = "退出登录",
//                            style = MaterialTheme.typography.titleMedium
//                        )
//                    }
//                }
//
//                item { Spacer(modifier = Modifier.height(24.dp)) }
//            }

            // Footer
            item { Footer(onDebugClick = onDebugClick) }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // App update dialogs
        if (showUpdateDialog && updateInfo != null) {
            val info = updateInfo!!
            UpdateAvailableDialog(
                info = info.response,
                isAlreadyDownloaded = info.isAlreadyDownloaded,
                onUpdate = {
                    showUpdateDialog = false
                    if (info.isAlreadyDownloaded && info.localApkPath != null) {
                        // 已下载，直接触发安装
                        triggerLocalInstall(context, info.localApkPath)
                    } else {
                        // 需要下载
                        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
                        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                        if (isWifi) {
                            val url = info.response.downloadUrl ?: return@UpdateAvailableDialog
                            onStartApkDownload(url, info.response.versionCode ?: -1)
                        } else {
                            showWifiWarning = true
                        }
                    }
                },
                onDismiss = { showUpdateDialog = false }
            )
        }

        if (showWifiWarning) {
            WifiWarningDialog(
                onConfirm = {
                    showWifiWarning = false
                    val info = updateInfo ?: return@WifiWarningDialog
                    val url = info.response.downloadUrl ?: return@WifiWarningDialog
                    ApkDownloadService.start(context, url, info.response.versionCode ?: -1)
                },
                onDismiss = { showWifiWarning = false }
            )
        }

        // Trajectory color mode bottom sheet
        if (showTrajectoryColorSheet) {
            TrajectoryColorModeSheet(
                currentMode = currentColorMode,
                onModeSelected = { mode ->
                    currentColorMode = mode
                    preferencesManager.saveTrajectoryColorMode(mode)
                },
                onDismiss = { showTrajectoryColorSheet = false }
            )
        }

        // Appearance mode bottom sheet
        if (showAppearanceSheet) {
            AppearanceModeSheet(
                currentMode = currentThemeMode,
                onModeSelected = { mode ->
                    currentThemeMode = mode
                    preferencesManager.saveThemeMode(mode)
                    onThemeModeChanged(mode)
                },
                onDismiss = { showAppearanceSheet = false }
            )
        }

    }
}

/**
 * 直接触发本地已缓存 APK 的安装界面（兼容 Android 10+ 前台 Activity 启动）
 */
internal fun triggerLocalInstall(context: Context, apkPath: String) {
    try {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            Toast.makeText(context, "安装包文件不存在，请重新下载", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
    } catch (e: Exception) {
        RLog.e("ProfileTab", "触发本地安装失败", e)
        Toast.makeText(context, "启动安装失败，请重试", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Footer section with app version and tagline.
 * Tapping the version text 5 times in quick succession (within 2s) opens the debug screen.
 */
@Composable
private fun Footer(onDebugClick: () -> Unit = {}) {
    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "RunYay v${BuildConfig.VERSION_NAME}_${BuildConfig.GIT_HASH}_${BuildConfig.BUILD_DATE}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable {
                val now = System.currentTimeMillis()
                if (now - lastClickTime > 2000L) clickCount = 0
                lastClickTime = now
                clickCount++
                if (clickCount >= 5) {
                    clickCount = 0
                    onDebugClick()
                }
            }
        )
    }
}
