package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.presentation.components.settings.SettingsCard
import com.oterman.rundemo.presentation.components.settings.SettingsItem
import com.oterman.rundemo.presentation.components.settings.UserProfileCard

/**
 * Profile/Settings tab content
 * Corresponds to iOS SettingPage
 * Shows user profile, settings groups, and footer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTabContent(
    isLoggedIn: Boolean,
    userName: String?,
    phoneNumber: String? = null,
    onLogoutClick: () -> Unit,
    onLoginClick: () -> Unit,
    onShowWelcomeClick: () -> Unit,
    onResetFirstLaunchClick: () -> Unit
) {
    Scaffold(
        topBar = {

            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "我的",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User Profile Card
            item {
                UserProfileCard(
                    isLoggedIn = isLoggedIn,
                    userName = userName,
                    phoneNumber = phoneNumber,
                    onClick = {
                        if (!isLoggedIn) {
                            onLoginClick()
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // Settings Group 1: Training Settings
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Flag,
                        title = "跑步目标",
                        onClick = { /* TODO: Navigate to running goal page */ }
                    )
                    SettingsItem(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = "心率区间",
                        onClick = { /* TODO: Navigate to heart rate zone page */ }
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Palette,
                        title = "外观设置",
                        showDivider = false,
                        onClick = { /* TODO: Navigate to appearance settings page */ }
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
                        onClick = { /* TODO: Navigate to contact us page */ }
                    )
                    SettingsItem(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        title = "帮助与反馈",
                        onClick = { /* TODO: Navigate to help page */ }
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Star,
                        title = "给个好评",
                        showDivider = false,
                        onClick = { /* TODO: Open app store rating */ }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // Debug Group (Show Welcome, Reset First Launch)
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Flag,
                        title = "显示欢迎页",
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        showDivider = true,
                        onClick = onShowWelcomeClick
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Flag,
                        title = "重置首次启动",
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        showDivider = false,
                        onClick = onResetFirstLaunchClick
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Logout button (only when logged in)
            if (isLoggedIn) {
                item {
                    OutlinedButton(
                        onClick = onLogoutClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = "退出登录",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // Footer
            item { Footer() }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * Footer section with app version and tagline
 */
@Composable
private fun Footer() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "DemoRun V1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
