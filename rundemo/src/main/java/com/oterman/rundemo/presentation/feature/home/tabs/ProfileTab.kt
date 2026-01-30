package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User Profile Card
        UserProfileCard(
            isLoggedIn = isLoggedIn,
            userName = userName,
            phoneNumber = phoneNumber,
            onClick = {
                if (!isLoggedIn) {
                    onLoginClick()
                }
                // When logged in, clicking could navigate to profile edit page (empty for now)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Settings Group 1: Training Settings
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

        Spacer(modifier = Modifier.height(20.dp))

        // Settings Group 2: Support
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

        Spacer(modifier = Modifier.height(20.dp))

        // Debug Group (Show Welcome, Reset First Launch)
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

        Spacer(modifier = Modifier.height(24.dp))

        // Logout button (only when logged in)
        if (isLoggedIn) {
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

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Footer
        Footer()

        Spacer(modifier = Modifier.height(16.dp))
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
        Text(
            text = "跑鸭陪你一起跑呀！",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
