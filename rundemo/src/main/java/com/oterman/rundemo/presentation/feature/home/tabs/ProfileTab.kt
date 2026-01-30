package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
    onLogoutClick: () -> Unit,
    onLoginClick: () -> Unit,
    onShowWelcomeClick: () -> Unit,
    onResetFirstLaunchClick: () -> Unit
) {
    val lazyListState = rememberLazyListState()
    val backgroundColor = MaterialTheme.colorScheme.background

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
