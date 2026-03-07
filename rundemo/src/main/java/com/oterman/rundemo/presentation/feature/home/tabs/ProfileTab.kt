package com.oterman.rundemo.presentation.feature.home.tabs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.oterman.rundemo.presentation.components.trajectory.TrajectoryColorMode
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.ui.theme.ThemeMode
import com.oterman.rundemo.presentation.components.settings.SettingsCard
import com.oterman.rundemo.presentation.components.settings.SettingsItem
import com.oterman.rundemo.presentation.components.settings.UserProfileCard

/**
 * Profile/Settings tab content with iOS-style NavigationTitle effect
 * Large title collapses to small title when scrolling
 * Corresponds to iOS SettingPage
 */
@Composable
fun ProfileTabContent(
    isLoggedIn: Boolean,
    userName: String?,
    phoneNumber: String? = null,
    avatarUrl: String? = null,
    isLoadingAvatar: Boolean = false,
    isImportingFit: Boolean = false,
    onLogoutClick: () -> Unit,
    onLoginClick: () -> Unit,
    onUserProfileClick: () -> Unit = {},
    onShowWelcomeClick: () -> Unit,
    onResetFirstLaunchClick: () -> Unit,
    onImportFitFile: (Uri) -> Unit = {},
    onDataSourceManageClick: () -> Unit = {},
    onRunGoalClick: () -> Unit = {},
    onHearRateZoneClick: () -> Unit = {},
    onDebugClick: () -> Unit = {},
    onContactUsClick: () -> Unit = {},
    onThemeModeChanged: (ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val backgroundColor = MaterialTheme.colorScheme.background

    val preferencesManager = remember { PreferencesManager(context) }

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

    // FIT import disclaimer dialog state
    var showFitImportDisclaimer by remember { mutableStateOf(false) }

    // FIT文件选择器
    val fitFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportFitFile(it) }
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

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // Settings Group: Data Import & Sync
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Sync,
                        title = "数据源管理",
                        subtitle = "佳明、高驰等平台数据同步",
                        iconTint = RunTheme.colorScheme.blue,
                        onClick = onDataSourceManageClick
                    )
                    SettingsItem(
                        icon = Icons.Outlined.FileUpload,
                        title = "导入FIT文件",
                        subtitle = if (isImportingFit) "导入中..." else "从本地导入运动数据",
                        iconTint = RunTheme.colorScheme.blue,
                        showDivider = false,
                        onClick = {
                            if (!isImportingFit) {
                                if (preferencesManager.getFitImportDisclaimerDismissed()) {
                                    fitFileLauncher.launch(arrayOf("*/*"))
                                } else {
                                    showFitImportDisclaimer = true
                                }
                            }
                        }
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
                        onClick = onContactUsClick
                    )
                    SettingsItem(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        title = "帮助与反馈",
                        iconTint = RunTheme.colorScheme.blue,
                        onClick = { /* TODO: Navigate to help page */ }
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Star,
                        title = "给个好评",
                        iconTint = RunTheme.colorScheme.blue,
                        showDivider = false,
                        onClick = { /* TODO: Open app store rating */ }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // Debug Group (Show Welcome, Reset First Launch) - 仅Debug版本可见
            if (BuildConfig.DEBUG) {
                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Outlined.Flag,
                            title = "显示欢迎页",
                            iconTint = RunTheme.colorScheme.blue,
                            showDivider = true,
                            onClick = onShowWelcomeClick
                        )
                        SettingsItem(
                            icon = Icons.Outlined.Flag,
                            title = "重置首次启动",
                            iconTint = RunTheme.colorScheme.blue,
                            showDivider = true,
                            onClick = onResetFirstLaunchClick
                        )
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

        if (showFitImportDisclaimer) {
            FitImportDisclaimerDialog(
                onConfirm = { dontShowAgain ->
                    if (dontShowAgain) {
                        preferencesManager.saveFitImportDisclaimerDismissed(true)
                    }
                    showFitImportDisclaimer = false
                    fitFileLauncher.launch(arrayOf("*/*"))
                },
                onDismiss = { showFitImportDisclaimer = false }
            )
        }
    }
}

@Composable
private fun FitImportDisclaimerDialog(
    onConfirm: (dontShowAgain: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var dontShowAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("实验功能提示") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("FIT 文件导入为实验测试功能，数据仅保存在本地设备。\n\n卸载重装后，已导入的数据将会丢失。后续版本会支持云端同步，敬请期待。")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { dontShowAgain = !dontShowAgain }
                ) {
                    Checkbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
                    Text("不再提醒", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(dontShowAgain) }) { Text("继续导入") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
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
