package com.oterman.rundemo.presentation.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.presentation.feature.home.tabs.DataTabContent
import com.oterman.rundemo.presentation.feature.home.tabs.HomeTabContent
import com.oterman.rundemo.presentation.feature.home.tabs.ProfileTabContent

/**
 * Main Home screen with bottom navigation
 * Corresponds to iOS MainTabView
 */
@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToWelcome: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

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
                HomeTab.HOME -> HomeTabContent()
                HomeTab.DATA -> DataTabContent()
                HomeTab.PROFILE -> ProfileTabContent(
                    isLoggedIn = uiState.isLoggedIn,
                    userName = uiState.userName,
                    onLogoutClick = viewModel::showLogoutConfirmation,
                    onLoginClick = viewModel::navigateToLogin,
                    onShowWelcomeClick = viewModel::navigateToWelcome,
                    onResetFirstLaunchClick = viewModel::resetFirstLaunch
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
    NavigationBar {
        // Tab 1: 首页 (Home)
        NavigationBarItem(
            selected = selectedTab == HomeTab.HOME,
            onClick = { onTabSelected(HomeTab.HOME) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == HomeTab.HOME) {
                        Icons.Filled.Home
                    } else {
                        Icons.Outlined.Home
                    },
                    contentDescription = "首页"
                )
            },
            label = { Text("首页") }
        )

        // Tab 2: 数据 (Data/Chart)
        NavigationBarItem(
            selected = selectedTab == HomeTab.DATA,
            onClick = { onTabSelected(HomeTab.DATA) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == HomeTab.DATA) {
                        Icons.Filled.BarChart
                    } else {
                        Icons.Outlined.BarChart
                    },
                    contentDescription = "数据"
                )
            },
            label = { Text("数据") }
        )

        // Tab 3: 我的 (Profile)
        NavigationBarItem(
            selected = selectedTab == HomeTab.PROFILE,
            onClick = { onTabSelected(HomeTab.PROFILE) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == HomeTab.PROFILE) {
                        Icons.Filled.Person
                    } else {
                        Icons.Outlined.Person
                    },
                    contentDescription = "我的"
                )
            },
            label = { Text("我的") }
        )
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
