package com.oterman.rundemo.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.oterman.rundemo.presentation.feature.auth.login.LoginScreen

/**
 * 应用导航图
 * 定义应用的导航结构和页面跳转逻辑
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 登录页面
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // 登录成功后导航到主页面
                    navController.navigate(Screen.Home.route) {
                        // 清除登录页面，防止返回
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    // TODO: 导航到注册页面（暂未实现）
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    // TODO: 导航到忘记密码页面（暂未实现）
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }
        
        // 主页面（登录成功后的页面）
        composable(Screen.Home.route) {
            HomeScreen()
        }
        
        // 注册页面（占位符）
        composable(Screen.Register.route) {
            PlaceholderScreen(title = "注册页面")
        }
        
        // 忘记密码页面（占位符）
        composable(Screen.ForgotPassword.route) {
            PlaceholderScreen(title = "忘记密码页面")
        }
        
        // 用户协议页面（占位符）
        composable(Screen.UserTerms.route) {
            PlaceholderScreen(title = "用户协议")
        }
        
        // 隐私政策页面（占位符）
        composable(Screen.PrivacyPolicy.route) {
            PlaceholderScreen(title = "隐私政策")
        }
    }
}

/**
 * 主页面
 * 登录成功后显示的页面
 */
@Composable
private fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "登录成功！\n欢迎使用demorun",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

/**
 * 占位符页面
 * 用于暂未实现的页面
 */
@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title\n（待实现）",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

