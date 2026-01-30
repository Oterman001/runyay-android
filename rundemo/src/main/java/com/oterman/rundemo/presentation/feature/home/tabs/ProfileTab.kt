package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Profile/Settings tab content
 * Corresponds to iOS Tab3Page
 * Shows login state and provides logout/login actions
 */
@Composable
fun ProfileTabContent(
    isLoggedIn: Boolean,
    userName: String?,
    onLogoutClick: () -> Unit,
    onLoginClick: () -> Unit,
    onShowWelcomeClick: () -> Unit,
    onResetFirstLaunchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "我的",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoggedIn) {
            LoggedInContent(
                userName = userName,
                onLogoutClick = onLogoutClick
            )
        } else {
            NotLoggedInContent(
                onLoginClick = onLoginClick,
                onShowWelcomeClick = onShowWelcomeClick,
                onResetFirstLaunchClick = onResetFirstLaunchClick
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Content shown when user is logged in
 */
@Composable
private fun LoggedInContent(
    userName: String?,
    onLogoutClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Login status indicator
        Text(
            text = "已登录",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4CAF50) // Green color
        )

        // User name (if available)
        userName?.let { name ->
            Text(
                text = "欢迎，$name",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logout button
        OutlinedButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth(0.6f)
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
}

/**
 * Content shown when user is not logged in
 */
@Composable
private fun NotLoggedInContent(
    onLoginClick: () -> Unit,
    onShowWelcomeClick: () -> Unit,
    onResetFirstLaunchClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Not logged in status
        Text(
            text = "未登录",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Login button (primary action)
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "登录",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Show welcome page button
        OutlinedButton(
            onClick = onShowWelcomeClick,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "显示欢迎页",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Reset first launch button (for debugging)
        OutlinedButton(
            onClick = onResetFirstLaunchClick,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFFF9800) // Orange color
            )
        ) {
            Text(
                text = "重置首次启动",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
